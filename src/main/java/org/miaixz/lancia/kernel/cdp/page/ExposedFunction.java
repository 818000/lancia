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
package org.miaixz.lancia.kernel.cdp.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.lancia.kernel.cdp.runtime.CdpExecutionContext;
import org.miaixz.lancia.kernel.cdp.runtime.CdpJSHandle;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Bridges a page-exposed Java callback to the Runtime binding installed in a frame.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class ExposedFunction {

    /**
     * Current name.
     */
    private final String name;

    /**
     * Current function.
     */
    private final Callback function;

    /**
     * Current init source.
     */
    private final String initSource;

    /**
     * Creates an exposed function bridge.
     *
     * @param name       exposed function name
     * @param function   Java callback
     * @param initSource JavaScript installed before the binding is used
     */
    public ExposedFunction(String name, Callback function, String initSource) {
        this.name = Assert.notBlank(name, "name");
        this.function = Assert.notNull(function, "function");
        this.initSource = initSource == null ? Normal.EMPTY : initSource;
    }

    /**
     * Invokes the Java callback for a Runtime binding call and resolves or rejects the page-side promise.
     *
     * @param context   execution context that received the binding call
     * @param id        page-side callback id
     * @param args      serialized callback arguments
     * @param isTrivial whether all arguments were sent by value
     */
    public void run(CdpExecutionContext context, int id, List<Object> args, boolean isTrivial) {
        CdpExecutionContext actualContext = Assert.notNull(context, "context");
        List<Object> actualArgs = new ArrayList<>(args == null ? List.of() : args);
        List<CdpJSHandle> disposableHandles = new ArrayList<>();
        try {
            if (!isTrivial) {
                fillNonTrivialArguments(actualContext, id, actualArgs, disposableHandles);
            }
            Object result = awaitResult(function.apply(List.copyOf(actualArgs)));
            resolve(actualContext, id, result);
            for (Object arg : actualArgs) {
                if (arg instanceof CdpJSHandle handle) {
                    disposableHandles.add(handle);
                }
            }
        } catch (Throwable throwable) {
            reject(actualContext, id, throwable);
        } finally {
            for (CdpJSHandle handle : disposableHandles) {
                handle.dispose();
            }
        }
    }

    /**
     * Awaits an asynchronous binding result when needed.
     *
     * @param result result
     * @return resolved result
     * @throws Exception if the result fails
     */
    private Object awaitResult(Object result) throws Exception {
        if (!(result instanceof CompletionStage<?> stage)) {
            return result;
        }
        try {
            return stage.toCompletableFuture().get();
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw ex;
        }
    }

    /**
     * Replaces non-trivial node arguments with temporary handles from the page-side argument store.
     *
     * @param context           execution context
     * @param id                page-side callback id
     * @param args              argument list to update
     * @param disposableHandles handles that must be disposed after callback execution
     */
    private void fillNonTrivialArguments(
            CdpExecutionContext context,
            int id,
            List<Object> args,
            List<CdpJSHandle> disposableHandles) {
        CdpJSHandle handles = context
                .evaluateHandle("globalThis[" + context.literal(name) + "].args.get(" + id + Symbol.PARENTHESE_RIGHT);
        Map<String, CdpJSHandle> properties = handles.getProperties();
        for (Map.Entry<String, CdpJSHandle> entry : properties.entrySet()) {
            int index = parseIndex(entry.getKey());
            CdpJSHandle handle = entry.getValue();
            if (index >= 0 && index < args.size()
                    && "node".equals(PayloadReader.text(handle.remoteObject().get("subtype")))) {
                args.set(index, handle);
            } else {
                disposableHandles.add(handle);
            }
        }
        disposableHandles.add(handles);
    }

    /**
     * Resolves the page-side callback promise.
     *
     * @param context execution context
     * @param id      page-side callback id
     * @param result  Java callback result
     */
    private void resolve(CdpExecutionContext context, int id, Object result) {
        context.evaluate(
                "(()=>{const callbacks=globalThis[" + context.literal(name) + "].callbacks;" + "callbacks.get(" + id
                        + ").resolve(" + context.literal(result) + ");" + "callbacks.delete(" + id + ");})()");
    }

    /**
     * Rejects the page-side callback promise.
     *
     * @param context   execution context
     * @param id        page-side callback id
     * @param throwable Java callback failure
     */
    private void reject(CdpExecutionContext context, int id, Throwable throwable) {
        String message = throwable == null ? Normal.EMPTY : throwable.getMessage();
        String stack = throwable == null ? Normal.EMPTY : String.valueOf(throwable);
        try {
            context.evaluate(
                    "(()=>{const error=new Error(" + context.literal(message) + ");" + "error.stack="
                            + context.literal(stack) + Symbol.SEMICOLON + "const callbacks=globalThis["
                            + context.literal(name) + "].callbacks;" + "callbacks.get(" + id
                            + ").reject(error);callbacks.delete(" + id + ");})()");
        } catch (RuntimeException ex) {
            throw new InternalException("Failed to report Binding exception.", ex);
        }
    }

    /**
     * Parses an array index from a property name.
     *
     * @return index or {@code -1} when the name is not numeric
     */
    private int parseIndex(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /**
     * Returns the exposed function name.
     *
     * @return function name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the JavaScript source installed for this binding.
     *
     * @return initialization source
     */
    public String initSource() {
        return initSource;
    }

    /**
     * Receives arguments from the page and returns the value exposed to JavaScript.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    @FunctionalInterface
    public interface Callback {

        /**
         * Returns the apply.
         *
         * @param args arguments to pass
         * @return apply value
         * @throws Exception if the operation fails
         */
        Object apply(List<Object> args) throws Exception;
    }

}
