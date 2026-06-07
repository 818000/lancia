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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.miaixz.bus.core.xyz.ThreadKit;
import org.miaixz.bus.health.Executor;

/**
 * Creates pipe envelopes for browser processes on Unix-like platforms.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class UnixPipeEnvelopeFactory implements PipeEnvelopeFactory {

    /**
     * Creates a Unix pipe envelope factory.
     */
    public UnixPipeEnvelopeFactory() {
        // No initialization required.
    }

    /**
     * Maximum time to wait for both pipe streams to open.
     */
    private static final Duration OPEN_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Creates a pipe envelope backed by Unix named pipes.
     *
     * @param command command name
     * @return pipe envelope
     * @throws IOException if the operation fails
     */
    @Override
    public PipeEnvelope create(List<String> command) throws IOException {
        validatePipeCommand(command);
        Path temporaryDirectory = Files.createTempDirectory("lancia-pipe-");
        Path javaToChrome = temporaryDirectory.resolve("java-to-chrome.pipe");
        Path chromeToJava = temporaryDirectory.resolve("chrome-to-java.pipe");
        createFifo(javaToChrome);
        createFifo(chromeToJava);

        Process process = startProcess(command, javaToChrome, chromeToJava);
        try {
            CompletableFuture<OutputStream> writer = CompletableFuture.supplyAsync(() -> openOutput(javaToChrome));
            CompletableFuture<InputStream> reader = CompletableFuture.supplyAsync(() -> openInput(chromeToJava));
            OutputStream cdpWriter = writer.get(OPEN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            InputStream cdpReader = reader.get(OPEN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            ensureProcessAlive(process);
            return new PipeEnvelope(process, cdpReader, cdpWriter, temporaryDirectory);
        } catch (Exception ex) {
            process.destroyForcibly();
            throw new IOException("Failed to open Chromium pipe.", ex);
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
     * Creates a named pipe.
     *
     * @param path file path
     * @throws IOException if the operation fails
     */
    private void createFifo(Path path) throws IOException {
        Executor.runNative(new String[] { "mkfifo", "-m", "600", path.toString() });
        if (!Files.exists(path)) {
            throw new IOException("Failed to create named pipe: " + path);
        }
    }

    /**
     * Starts the browser with file descriptors 3 and 4 mapped to named pipes.
     *
     * @param command      command name
     * @param javaToChrome java to chrome value
     * @param chromeToJava chrome to java value
     * @return start process value
     * @throws IOException if the operation fails
     */
    private Process startProcess(List<String> command, Path javaToChrome, Path chromeToJava) throws IOException {
        List<String> shellCommand = new ArrayList<>();
        shellCommand.add("/bin/sh");
        shellCommand.add("-c");
        shellCommand.add("exec 3<\"$1\"; exec 4>\"$2\"; shift 2; exec \"$@\"");
        shellCommand.add("lancia-pipe");
        shellCommand.add(javaToChrome.toString());
        shellCommand.add(chromeToJava.toString());
        shellCommand.addAll(command);
        return new ProcessBuilder(shellCommand).start();
    }

    /**
     * Opens the Java-to-browser pipe writer.
     *
     * @param path file path
     * @return open output value
     */
    private OutputStream openOutput(Path path) {
        try {
            return new FileOutputStream(path.toFile());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to open pipe output stream: " + path, ex);
        }
    }

    /**
     * Opens the browser-to-Java pipe reader.
     *
     * @param path file path
     * @return open input value
     */
    private InputStream openInput(Path path) {
        try {
            return new FileInputStream(path.toFile());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to open pipe input stream: " + path, ex);
        }
    }

    /**
     * Handles ensure process alive.
     *
     * @param process process value
     * @throws IOException if the operation fails
     */
    private void ensureProcessAlive(Process process) throws IOException {
        if (!ThreadKit.sleep(100)) {
            throw new IOException("Interrupted while waiting for Chromium pipe envelope state.");
        }
        if (!process.isAlive()) {
            throw new IOException(
                    "Chromium pipe envelope exited immediately after launch, exit code: " + process.exitValue());
        }
    }

}
