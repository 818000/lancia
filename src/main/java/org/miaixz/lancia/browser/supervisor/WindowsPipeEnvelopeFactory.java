/*
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
 ~                                                                           ~
 ~ Copyright (c) 2015-2026 miaixz.org and other contributors.                ~
 ~                                                                           ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");           ~
 ~ you may not use this file except in compliance with the License.          ~
 ~ You may obtain a copy of the License at                                   ~
 ~                                                                           ~
 ~      https://www.apache.org/licenses/LICENSE-2.0                          ~
 ~                                                                           ~
 ~ Unless required by applicable law or agreed to in writing, software       ~
 ~ distributed under the License is distributed on an "AS IS" BASIS,         ~
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  ~
 ~ See the License for the specific language governing permissions and       ~
 ~ limitations under the License.                                            ~
 ~                                                                           ~
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
*/
package org.miaixz.lancia.browser.supervisor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;

/**
 * Creates pipe envelopes for browser processes on Windows.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class WindowsPipeEnvelopeFactory implements PipeEnvelopeFactory {

    /**
     * CRT file descriptor count passed through STARTUPINFO.lpReserved2.
     */
    private static final int CRT_FD_COUNT = 5;

    /**
     * CRT fd flag for an open pipe descriptor.
     */
    private static final byte CRT_FOPEN_PIPE = 0x09;

    /**
     * Windows invalid handle value.
     */
    private static final long INVALID_HANDLE_VALUE = -1L;

    /**
     * Handle inheritance flag.
     */
    private static final int HANDLE_FLAG_INHERIT = 0x00000001;

    /**
     * Startup flag for std handles.
     */
    private static final int STARTF_USESTDHANDLES = 0x00000100;

    /**
     * Process is still active.
     */
    private static final int STILL_ACTIVE = 259;

    /**
     * Infinite wait timeout.
     */
    private static final int INFINITE = 0xffffffff;

    /**
     * Wait object signaled result.
     */
    private static final int WAIT_OBJECT_0 = 0;

    /**
     * Broken pipe error.
     */
    private static final int ERROR_BROKEN_PIPE = 109;

    /**
     * Creates a Windows pipe envelope factory.
     */
    public WindowsPipeEnvelopeFactory() {
        // No initialization required.
    }

    /**
     * Creates a pipe envelope backed by inherited Windows handles.
     *
     * @param command command name
     * @return pipe envelope
     * @throws IOException if the operation fails
     */
    @Override
    public PipeEnvelope create(List<String> command) throws IOException {
        validatePipeCommand(command);
        PipeHandles handles = null;
        WinBase.PROCESS_INFORMATION processInformation = null;
        try {
            handles = createPipeHandles();
            WinBase.STARTUPINFO startupInfo = startupInfo(handles);
            processInformation = createProcess(command, startupInfo);

            closeQuietly(handles.childRead);
            handles.childRead = null;
            closeQuietly(handles.childWrite);
            handles.childWrite = null;
            closeQuietly(processInformation.hThread);

            WindowsProcess process = new WindowsProcess(processInformation.hProcess,
                    processInformation.dwProcessId.longValue());
            return new PipeEnvelope(process, new WindowsHandleInputStream(handles.parentRead),
                    new WindowsHandleOutputStream(handles.parentWrite));
        } catch (IOException | RuntimeException ex) {
            if (processInformation != null && processInformation.hProcess != null) {
                Kernel32.INSTANCE.TerminateProcess(processInformation.hProcess, 1);
                closeQuietly(processInformation.hProcess);
            }
            if (processInformation != null && processInformation.hThread != null) {
                closeQuietly(processInformation.hThread);
            }
            if (handles != null) {
                handles.close();
            }
            if (ex instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Failed to launch Windows pipe.", ex);
        }
    }

    /**
     * Validates pipe command.
     *
     * @param command command name
     */
    public void validatePipeCommand(List<String> command) {
        PipeEnvelopeFactory.validatePipeCommand(command);
    }

    /**
     * Creates the two pipe pairs used by Chromium fd 3 and fd 4.
     *
     * @return pipe handles
     * @throws IOException if the handles cannot be created
     */
    private PipeHandles createPipeHandles() throws IOException {
        WinBase.SECURITY_ATTRIBUTES attributes = new WinBase.SECURITY_ATTRIBUTES();
        attributes.dwLength = new WinDef.DWORD(attributes.size());
        attributes.bInheritHandle = true;
        attributes.write();

        PipeHandles handles = new PipeHandles();
        try {
            WinNT.HANDLEByReference childRead = new WinNT.HANDLEByReference();
            WinNT.HANDLEByReference parentWrite = new WinNT.HANDLEByReference();
            if (!Kernel32.INSTANCE.CreatePipe(childRead, parentWrite, attributes, 0)) {
                throw lastError("Failed to create Java -> Chromium pipe.");
            }

            WinNT.HANDLEByReference parentRead = new WinNT.HANDLEByReference();
            WinNT.HANDLEByReference childWrite = new WinNT.HANDLEByReference();
            if (!Kernel32.INSTANCE.CreatePipe(parentRead, childWrite, attributes, 0)) {
                throw lastError("Failed to create Chromium -> Java pipe.");
            }

            handles.childRead = childRead.getValue();
            handles.parentWrite = parentWrite.getValue();
            handles.parentRead = parentRead.getValue();
            handles.childWrite = childWrite.getValue();
            makeNonInheritable(handles.parentRead, "Failed to mark parent read handle as non-inheritable.");
            makeNonInheritable(handles.parentWrite, "Failed to mark parent write handle as non-inheritable.");
            return handles;
        } catch (IOException | RuntimeException ex) {
            handles.close();
            if (ex instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Failed to create Windows pipe handles.", ex);
        }
    }

    /**
     * Builds STARTUPINFO with the Windows CRT fd table required by Chromium remote-debugging-pipe.
     *
     * @param handles pipe handles
     * @return startup info
     */
    private WinBase.STARTUPINFO startupInfo(PipeHandles handles) {
        WinBase.STARTUPINFO startupInfo = new WinBase.STARTUPINFO();
        startupInfo.cb = new WinDef.DWORD(startupInfo.size());
        startupInfo.dwFlags = STARTF_USESTDHANDLES;
        startupInfo.hStdInput = invalidHandle();
        startupInfo.hStdOutput = invalidHandle();
        startupInfo.hStdError = invalidHandle();

        Memory reserved = crtReservedData(handles.childRead, handles.childWrite);
        ByteByReference reservedPointer = new ByteByReference();
        reservedPointer.setPointer(reserved);
        startupInfo.cbReserved2 = new WinDef.WORD((short) reserved.size());
        startupInfo.lpReserved2 = reservedPointer;
        startupInfo.write();
        return startupInfo;
    }

    /**
     * Starts Chromium with inheritable fd 3 and fd 4 handles.
     *
     * @param command     command
     * @param startupInfo startup info
     * @return process information
     * @throws IOException if process startup fails
     */
    private WinBase.PROCESS_INFORMATION createProcess(List<String> command, WinBase.STARTUPINFO startupInfo)
            throws IOException {
        WinBase.PROCESS_INFORMATION processInformation = new WinBase.PROCESS_INFORMATION();
        char[] commandLine = Native.toCharArray(commandLine(command));
        boolean created = Kernel32.INSTANCE.CreateProcessW(
                null,
                commandLine,
                null,
                null,
                true,
                new WinDef.DWORD(0),
                null,
                null,
                startupInfo,
                processInformation);
        if (!created) {
            throw lastError("Failed to start Windows pipe envelope.");
        }
        return processInformation;
    }

    /**
     * Creates the lpReserved2 CRT fd table.
     *
     * @param fd3 fd 3 child read handle
     * @param fd4 fd 4 child write handle
     * @return memory block
     */
    private Memory crtReservedData(WinNT.HANDLE fd3, WinNT.HANDLE fd4) {
        int size = Integer.BYTES + CRT_FD_COUNT + Native.POINTER_SIZE * CRT_FD_COUNT;
        Memory memory = new Memory(size);
        memory.clear(size);
        memory.setInt(0, CRT_FD_COUNT);
        memory.setByte(Integer.BYTES + 3L, CRT_FOPEN_PIPE);
        memory.setByte(Integer.BYTES + 4L, CRT_FOPEN_PIPE);

        long handleOffset = Integer.BYTES + CRT_FD_COUNT;
        for (int index = 0; index < CRT_FD_COUNT; index++) {
            setPointerValue(memory, handleOffset + (long) index * Native.POINTER_SIZE, INVALID_HANDLE_VALUE);
        }
        setPointerValue(memory, handleOffset + 3L * Native.POINTER_SIZE, pointerValue(fd3));
        setPointerValue(memory, handleOffset + 4L * Native.POINTER_SIZE, pointerValue(fd4));
        return memory;
    }

    /**
     * Converts command arguments to a Windows command line.
     *
     * @param command arguments
     * @return command line
     */
    private String commandLine(List<String> command) {
        StringBuilder builder = new StringBuilder();
        for (String argument : command) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(quote(argument));
        }
        return builder.toString();
    }

    /**
     * Quotes a single Windows command-line argument.
     *
     * @param argument argument
     * @return quoted argument
     */
    private String quote(String argument) {
        if (argument == null || argument.isEmpty()) {
            return "\"\"";
        }
        boolean needsQuotes = false;
        for (int index = 0; index < argument.length(); index++) {
            char current = argument.charAt(index);
            if (Character.isWhitespace(current) || current == '"') {
                needsQuotes = true;
                break;
            }
        }
        if (!needsQuotes) {
            return argument;
        }
        StringBuilder quoted = new StringBuilder(argument.length() + 2);
        quoted.append('"');
        int backslashes = 0;
        for (int index = 0; index < argument.length(); index++) {
            char current = argument.charAt(index);
            if (current == '\\') {
                backslashes++;
                continue;
            }
            if (current == '"') {
                quoted.append("\\".repeat(backslashes * 2 + 1));
                quoted.append('"');
                backslashes = 0;
                continue;
            }
            quoted.append("\\".repeat(backslashes));
            quoted.append(current);
            backslashes = 0;
        }
        quoted.append("\\".repeat(backslashes * 2));
        quoted.append('"');
        return quoted.toString();
    }

    /**
     * Makes a handle non-inheritable.
     *
     * @param handle  handle
     * @param message failure message
     * @throws IOException if the operation fails
     */
    private void makeNonInheritable(WinNT.HANDLE handle, String message) throws IOException {
        if (!Kernel32.INSTANCE.SetHandleInformation(handle, HANDLE_FLAG_INHERIT, 0)) {
            throw lastError(message);
        }
    }

    /**
     * Returns the Windows invalid handle value.
     *
     * @return invalid handle
     */
    private WinNT.HANDLE invalidHandle() {
        return new WinNT.HANDLE(Pointer.createConstant(INVALID_HANDLE_VALUE));
    }

    /**
     * Returns the native pointer value for a handle.
     *
     * @param handle handle
     * @return pointer value
     */
    private long pointerValue(WinNT.HANDLE handle) {
        return Pointer.nativeValue(handle.getPointer());
    }

    /**
     * Writes a pointer-sized value into memory.
     *
     * @param memory memory
     * @param offset offset
     * @param value  value
     */
    private void setPointerValue(Memory memory, long offset, long value) {
        if (Native.POINTER_SIZE == Long.BYTES) {
            memory.setLong(offset, value);
        } else {
            memory.setInt(offset, (int) value);
        }
    }

    /**
     * Creates an IOException with the current Windows error code.
     *
     * @param message message
     * @return exception
     */
    private IOException lastError(String message) {
        return new IOException(message + " Windows error=" + Kernel32.INSTANCE.GetLastError());
    }

    /**
     * Closes a handle quietly.
     *
     * @param handle handle
     */
    private static void closeQuietly(WinNT.HANDLE handle) {
        if (handle != null && handle.getPointer() != null) {
            Kernel32.INSTANCE.CloseHandle(handle);
        }
    }

    /**
     * Stores Windows pipe handles used by the browser transport.
     */
    private static final class PipeHandles {

        /**
         * Child fd 3 read handle.
         */
        private WinNT.HANDLE childRead;

        /**
         * Parent CDP writer handle.
         */
        private WinNT.HANDLE parentWrite;

        /**
         * Parent CDP reader handle.
         */
        private WinNT.HANDLE parentRead;

        /**
         * Child fd 4 write handle.
         */
        private WinNT.HANDLE childWrite;

        /**
         * Closes all handles.
         */
        private void close() {
            closeQuietly(childRead);
            closeQuietly(parentWrite);
            closeQuietly(parentRead);
            closeQuietly(childWrite);
        }
    }

    /**
     * Process wrapper backed by a Windows process handle.
     */
    private static final class WindowsProcess extends Process {

        /**
         * Process handle.
         */
        private WinNT.HANDLE process;

        /**
         * Process id.
         */
        private final long pid;

        /**
         * Exit code.
         */
        private volatile Integer exitCode;

        /**
         * Creates a Windows process wrapper.
         *
         * @param process process handle
         * @param pid     process id
         */
        private WindowsProcess(WinNT.HANDLE process, long pid) {
            this.process = process;
            this.pid = pid;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream getInputStream() {
            return InputStream.nullInputStream();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int waitFor() throws InterruptedException {
            WinNT.HANDLE current = process;
            if (current == null) {
                return exitCode == null ? 0 : exitCode;
            }
            int wait = Kernel32.INSTANCE.WaitForSingleObject(current, INFINITE);
            if (wait != WAIT_OBJECT_0) {
                throw new IllegalStateException("Failed to wait for Windows process exit, result: " + wait);
            }
            return markExited();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean waitFor(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException {
            WinNT.HANDLE current = process;
            if (current == null) {
                return true;
            }
            int millis = Math.toIntExact(Math.min(Integer.MAX_VALUE, unit.toMillis(timeout)));
            int wait = Kernel32.INSTANCE.WaitForSingleObject(current, millis);
            if (wait == WAIT_OBJECT_0) {
                markExited();
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int exitValue() {
            if (isAlive()) {
                throw new IllegalThreadStateException("process is still alive");
            }
            return exitCode == null ? 0 : exitCode;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void destroy() {
            WinNT.HANDLE current = process;
            if (current != null && isAlive()) {
                Kernel32.INSTANCE.TerminateProcess(current, 1);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Process destroyForcibly() {
            destroy();
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isAlive() {
            WinNT.HANDLE current = process;
            if (current == null) {
                return false;
            }
            IntByReference code = new IntByReference();
            if (!Kernel32.INSTANCE.GetExitCodeProcess(current, code)) {
                markExited(0);
                return false;
            }
            if (code.getValue() == STILL_ACTIVE) {
                return true;
            }
            markExited(code.getValue());
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long pid() {
            return pid;
        }

        /**
         * Marks the process as exited.
         *
         * @return exit code
         */
        private int markExited() {
            IntByReference code = new IntByReference();
            int value = Kernel32.INSTANCE.GetExitCodeProcess(process, code) ? code.getValue() : 0;
            return markExited(value);
        }

        /**
         * Marks the process as exited.
         *
         * @param value exit code
         * @return exit code
         */
        private synchronized int markExited(int value) {
            if (exitCode == null) {
                exitCode = value;
            }
            if (process != null) {
                closeQuietly(process);
                process = null;
            }
            return exitCode;
        }
    }

    /**
     * Input stream backed by a Windows handle.
     */
    private static final class WindowsHandleInputStream extends InputStream {

        /**
         * Read handle.
         */
        private WinNT.HANDLE handle;

        /**
         * Creates an input stream.
         *
         * @param handle read handle
         */
        private WindowsHandleInputStream(WinNT.HANDLE handle) {
            this.handle = handle;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int read = read(one, 0, one.length);
            return read < 0 ? -1 : one[0] & 0xff;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (handle == null) {
                return -1;
            }
            if (buffer == null) {
                throw new NullPointerException("buffer");
            }
            if (offset < 0 || length < 0 || length > buffer.length - offset) {
                throw new IndexOutOfBoundsException();
            }
            if (length == 0) {
                return 0;
            }
            byte[] target = offset == 0 && length == buffer.length ? buffer : new byte[length];
            IntByReference bytesRead = new IntByReference();
            boolean ok = Kernel32.INSTANCE.ReadFile(handle, target, length, bytesRead, null);
            if (!ok) {
                int error = Kernel32.INSTANCE.GetLastError();
                if (error == ERROR_BROKEN_PIPE) {
                    return -1;
                }
                throw new IOException("Failed to read from Windows pipe, Windows error=" + error);
            }
            int count = bytesRead.getValue();
            if (count == 0) {
                return -1;
            }
            if (target != buffer) {
                System.arraycopy(target, 0, buffer, offset, count);
            }
            return count;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
            WinNT.HANDLE current = handle;
            handle = null;
            closeQuietly(current);
        }
    }

    /**
     * Output stream backed by a Windows handle.
     */
    private static final class WindowsHandleOutputStream extends OutputStream {

        /**
         * Write handle.
         */
        private WinNT.HANDLE handle;

        /**
         * Creates an output stream.
         *
         * @param handle write handle
         */
        private WindowsHandleOutputStream(WinNT.HANDLE handle) {
            this.handle = handle;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(int value) throws IOException {
            write(new byte[] { (byte) value });
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            if (handle == null) {
                throw new IOException("Windows pipe has been closed.");
            }
            if (buffer == null) {
                throw new NullPointerException("buffer");
            }
            if (offset < 0 || length < 0 || length > buffer.length - offset) {
                throw new IndexOutOfBoundsException();
            }
            if (length == 0) {
                return;
            }
            byte[] source = offset == 0 && length == buffer.length ? buffer
                    : java.util.Arrays.copyOfRange(buffer, offset, offset + length);
            IntByReference written = new IntByReference();
            boolean ok = Kernel32.INSTANCE.WriteFile(handle, source, length, written, null);
            if (!ok || written.getValue() != length) {
                int error = Kernel32.INSTANCE.GetLastError();
                throw new IOException("Failed to write to Windows pipe, Windows error=" + error);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() {
            WinNT.HANDLE current = handle;
            handle = null;
            closeQuietly(current);
        }
    }

}
