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
package org.miaixz.lancia.kernel.cdp.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;

/**
 * CDP command callback registry.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CallbackRegistry {

    /**
     * Callback timeout scheduler.
     */
    private static final ScheduledExecutorService CALLBACK_TIMER = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "lancia-callback-registry-timer");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Creates a CDP command callback registry.
     */
    public CallbackRegistry() {
        this(IdGenerator.intRange()::nextInt);
    }

    /**
     * Creates a CDP command callback registry.
     *
     * @param idGenerator command ID generator
     */
    public CallbackRegistry(IntSupplier idGenerator) {
        this.idGenerator = idGenerator == null ? IdGenerator.intRange()::nextInt : idGenerator;
    }

    /**
     * Command ID generator.
     */
    private final IntSupplier idGenerator;

    /**
     * Pending command callbacks.
     */
    private final Map<Integer, Callback> callbacks = new ConcurrentHashMap<>();

    /**
     * Returns the next command ID.
     *
     * @return command ID
     */
    public int nextId() {
        return idGenerator.getAsInt();
    }

    /**
     * Creates a command callback.
     *
     * @param id     command ID
     * @param method command method
     * @return command result
     */
    public CompletableFuture<CdpPayload> create(int id, String method) {
        return create(id, method, Normal._0);
    }

    /**
     * Creates a command callback.
     *
     * @param id            command ID
     * @param method        command method
     * @param timeoutMillis timeout in milliseconds
     * @return command result
     */
    public CompletableFuture<CdpPayload> create(int id, String method, long timeoutMillis) {
        Callback callback = new Callback(id, method, timeoutMillis);
        callbacks.put(id, callback);
        callback.future.whenComplete((value, error) -> callbacks.remove(id, callback));
        return callback.future;
    }

    /**
     * Creates a callback and invokes the protocol send request.
     *
     * @param method        command method
     * @param timeoutMillis timeout in milliseconds
     * @param request       protocol send request
     * @return command result
     */
    public CompletableFuture<CdpPayload> create(String method, long timeoutMillis, IntConsumer request) {
        int id = nextId();
        CompletableFuture<CdpPayload> future = create(id, method, timeoutMillis);
        try {
            request.accept(id);
        } catch (RuntimeException ex) {
            rejectRaw(id, ex);
            throw ex;
        }
        return future;
    }

    /**
     * Resolves a command callback.
     *
     * @param id     command ID
     * @param result command result
     * @return {@code true} when a callback was found and resolved
     */
    public boolean resolve(int id, CdpPayload result) {
        Callback callback = callbacks.remove(id);
        if (callback == null) {
            return false;
        }
        callback.resolve(result);
        return true;
    }

    /**
     * Rejects a command callback.
     *
     * @param id      command ID
     * @param message error message
     * @return {@code true} when a callback was found and rejected
     */
    public boolean reject(int id, String message) {
        return reject(id, message, null);
    }

    /**
     * Rejects a command callback.
     *
     * @param id              command ID
     * @param message         error message
     * @param originalMessage original error message
     * @return {@code true} when a callback was found and rejected
     */
    public boolean reject(int id, String message, String originalMessage) {
        Callback callback = callbacks.remove(id);
        if (callback == null) {
            return false;
        }
        String actualMessage = "Protocol error (" + callback.method + "): " + message;
        if (originalMessage != null && !originalMessage.isEmpty()) {
            actualMessage += Symbol.SPACE + originalMessage;
        }
        callback.reject(new InternalException(actualMessage));
        return true;
    }

    /**
     * Rejects a command callback with a raw exception.
     *
     * @param id    command ID
     * @param error raw exception
     * @return {@code true} when a callback was found and rejected
     */
    public boolean rejectRaw(int id, Throwable error) {
        Callback callback = callbacks.remove(id);
        if (callback == null) {
            return false;
        }
        callback.reject(error == null ? new InternalException("Protocol callback rejected.") : error);
        return true;
    }

    /**
     * Clears all pending commands.
     */
    public void clear() {
        clear(new InternalException("Target closed"));
    }

    /**
     * Clears all pending commands.
     *
     * @param throwable clear reason
     */
    public void clear(Throwable throwable) {
        for (Callback callback : callbacks.values()) {
            callback.reject(throwable == null ? new InternalException("Target closed") : throwable);
        }
        callbacks.clear();
    }

    /**
     * Returns pending protocol error descriptions.
     *
     * @return pending protocol error descriptions
     */
    public List<String> getPendingProtocolErrors() {
        List<String> errors = new ArrayList<>();
        for (Callback callback : callbacks.values()) {
            errors.add(
                    "Protocol call " + callback.method + " with id " + callback.id + " is pending. Trace: "
                            + callback.creationTrace);
        }
        return errors;
    }

    /**
     * Returns the contains.
     *
     * @param id identifier
     * @return {@code true} when the condition matches
     */
    public boolean contains(int id) {
        return callbacks.containsKey(id);
    }

    /**
     * Command callback.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class Callback {

        /**
         * Command ID.
         */
        private final int id;

        /**
         * Command method.
         */
        private final String method;

        /**
         * Asynchronous command result.
         */
        private final CompletableFuture<CdpPayload> future = new CompletableFuture<>();

        /**
         * Creation stack trace.
         */
        private final String creationTrace;

        /**
         * Timeout task.
         */
        private final ScheduledFuture<?> timeoutTask;

        /**
         * Creates a command callback.
         *
         * @param id     command ID
         * @param method command method
         */
        private Callback(int id, String method, long timeoutMillis) {
            this.id = id;
            this.method = method;
            this.creationTrace = creationTrace();
            this.timeoutTask = timeoutMillis <= Normal._0 ? null
                    : CALLBACK_TIMER.schedule(
                            () -> reject(
                                    new InternalException(method
                                            + " timed out. Increase the 'protocolTimeout' setting in launch/connect calls for a higher timeout if needed.")),
                            timeoutMillis,
                            TimeUnit.MILLISECONDS);
        }

        /**
         * Resolves the command callback.
         *
         * @param result command result
         */
        private void resolve(CdpPayload result) {
            cancelTimeout();
            future.complete(result);
        }

        /**
         * Rejects the command callback.
         *
         * @param throwable exception
         */
        private void reject(Throwable throwable) {
            cancelTimeout();
            future.completeExceptionally(throwable);
        }

        /**
         * Cancels the timeout task.
         */
        private void cancelTimeout() {
            if (timeoutTask != null) {
                timeoutTask.cancel(false);
            }
        }

        /**
         * Creates the current call stack text.
         *
         * @return call stack text
         */
        private String creationTrace() {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            List<String> lines = new ArrayList<>();
            for (int index = Math.min(3, stack.length); index < stack.length; index++) {
                lines.add(stack[index].toString());
            }
            return String.join(Symbol.LF, lines);
        }
    }

}
