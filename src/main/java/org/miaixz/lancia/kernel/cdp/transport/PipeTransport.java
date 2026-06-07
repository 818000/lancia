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
package org.miaixz.lancia.kernel.cdp.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.xyz.ByteKit;
import org.miaixz.bus.core.xyz.IoKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Transport;
import org.miaixz.lancia.kernel.cdp.session.Connection;
import org.miaixz.lancia.runtime.ResourceLimits;

/**
 * CDP transport backed by Chromium remote-debugging-pipe.
 *
 * <p>
 * Pipe messages are separated by a NUL byte. The reader accumulates fragments until a delimiter appears and then
 * dispatches complete UTF-8 messages.
 * </p>
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class PipeTransport implements Transport {

    /**
     * CDP pipe message delimiter.
     */
    private static final int MESSAGE_DELIMITER = 0;

    /**
     * Pipe read stream.
     */
    private final InputStream reader;

    /**
     * Pipe write stream.
     */
    private final OutputStream writer;

    /**
     * Connection reference used by legacy direct callbacks.
     */
    private final AtomicReference<Connection> connectionRef = new AtomicReference<>();

    /**
     * Message handler reference.
     */
    private final AtomicReference<Consumer<String>> messageHandlerRef = new AtomicReference<>();

    /**
     * Close handler reference.
     */
    private final AtomicReference<Runnable> closeHandlerRef = new AtomicReference<>();

    /**
     * Whether the transport is closed.
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Pipe reader thread.
     */
    private final Thread readerThread;
    /**
     * Resource limits.
     */
    private final ResourceLimits resourceLimits;

    /**
     * Creates a pipe transport.
     *
     * @param reader pipe read stream
     * @param writer pipe write stream
     */
    public PipeTransport(InputStream reader, OutputStream writer) {
        this(reader, writer, ResourceLimits.defaults());
    }

    /**
     * Creates a pipe transport.
     *
     * @param reader         pipe read stream
     * @param writer         pipe write stream
     * @param resourceLimits resource limits
     */
    public PipeTransport(InputStream reader, OutputStream writer, ResourceLimits resourceLimits) {
        this.reader = Assert.notNull(reader, "reader");
        this.writer = Assert.notNull(writer, "writer");
        this.resourceLimits = resourceLimits == null ? ResourceLimits.defaults() : resourceLimits;
        this.readerThread = new Thread(this::readLoop, "lancia-pipe-transport-reader");
        this.readerThread.setDaemon(true);
        this.readerThread.start();
        Logger.debug(false, "Protocol", "CDP pipe transport initialized.");
    }

    /**
     * Sends one CDP text message.
     *
     * @param message CDP text message
     */
    @Override
    public synchronized void send(String message) {
        Assert.notNull(message, "message");
        if (closed.get()) {
            throw new IllegalStateException("PipeTransport is closed.");
        }
        Logger.trace(true, "Protocol", "CDP pipe message send requested: chars={}", message.length());
        try {
            writer.write(ByteKit.toBytes(message, Charset.UTF_8));
            writer.write(MESSAGE_DELIMITER);
            writer.flush();
            Logger.trace(false, "Protocol", "CDP pipe message sent: chars={}", message.length());
        } catch (IOException ex) {
            Logger.error(false, "Protocol", ex, "CDP pipe message send failed: chars={}", message.length());
            throw new IllegalStateException("Failed to send pipe message.", ex);
        }
    }

    /**
     * Updates connection.
     *
     * @param connection connection callback target
     */
    @Override
    public void setConnection(Object connection) {
        connectionRef.set((Connection) Assert.notNull(connection, "connection"));
    }

    /**
     * Updates message handler.
     *
     * @param handler message handler
     */
    @Override
    public void setMessageHandler(Consumer<String> handler) {
        messageHandlerRef.set(handler);
    }

    /**
     * Updates close handler.
     *
     * @param handler close handler
     */
    @Override
    public void setCloseHandler(Runnable handler) {
        closeHandlerRef.set(handler);
    }

    /**
     * Closes the pipe transport.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            Logger.debug(true, "Protocol", "CDP pipe transport close requested.");
            IoKit.closeQuietly(reader, writer);
            readerThread.interrupt();
            notifyClosed();
            Logger.debug(false, "Protocol", "CDP pipe transport closed.");
        }
    }

    /**
     * Returns whether this object is closed.
     *
     * @return {@code true} when closed
     */
    public boolean closed() {
        return closed.get();
    }

    /**
     * Reads and dispatches pipe messages.
     */
    private void readLoop() {
        Logger.debug(true, "Protocol", "CDP pipe reader started.");
        ByteArrayOutputStream message = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        try {
            while (!closed.get()) {
                int read = reader.read(buffer);
                if (read < 0) {
                    Logger.debug(false, "Protocol", "CDP pipe reader reached EOF.");
                    close();
                    return;
                }
                for (int i = 0; i < read; i++) {
                    int current = buffer[i] & 0xff;
                    if (current == MESSAGE_DELIMITER) {
                        emitMessage(message);
                    } else {
                        message.write(current);
                        resourceLimits.validateProtocolMessageBytes(message.size());
                    }
                }
            }
        } catch (IOException ex) {
            if (!closed.get()) {
                Logger.warn(false, "Protocol", ex, "CDP pipe reader failed.");
                Connection connection = connectionRef.get();
                if (connection != null) {
                    connection.onError(ex);
                }
                close();
            }
        } catch (RuntimeException ex) {
            if (!closed.get()) {
                Logger.warn(false, "Protocol", ex, "CDP pipe reader rejected message.");
                Connection connection = connectionRef.get();
                if (connection != null) {
                    connection.onError(ex);
                }
                close();
            }
        }
    }

    /**
     * Dispatches one complete message.
     *
     * @param message message buffer
     */
    private void emitMessage(ByteArrayOutputStream message) {
        if (message.size() == 0) {
            return;
        }
        String text = StringKit.toString(message.toByteArray(), Charset.UTF_8);
        Logger.trace(false, "Protocol", "CDP pipe message received: chars={}", text.length());
        Consumer<String> handler = messageHandlerRef.get();
        if (handler != null) {
            handler.accept(text);
        } else {
            Connection connection = connectionRef.get();
            if (connection != null) {
                connection.onMessage(text);
            }
        }
        message.reset();
    }

    /**
     * Dispatches a close event.
     */
    private void notifyClosed() {
        Connection connection = connectionRef.get();
        if (connection != null) {
            connection.onClosed(SocketTransport.NORMAL_CLOSE, "pipe close");
        }
        Runnable handler = closeHandlerRef.get();
        if (handler != null) {
            handler.run();
        }
    }

}
