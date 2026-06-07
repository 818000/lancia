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
package org.miaixz.lancia.kernel.cdp.runtime;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.kernel.cdp.page.ExposedFunction;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.shared.payload.PayloadReader;
import org.miaixz.lancia.shared.protocol.TextWriter;
import org.miaixz.lancia.shared.runtime.LazyArg;

/**
 * CDP execution context for evaluating JavaScript and managing exposed functions.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpExecutionContext extends EventEmitter<String> {

    /**
     * Event name emitted when the context is disposed.
     */
    public static final String DISPOSED = "disposed";

    /**
     * Event name emitted when console API is called.
     */
    public static final String CONSOLE_API_CALLED = "consoleapicalled";

    /**
     * Event name emitted when an exposed binding is called.
     */
    public static final String BINDING_CALLED = "bindingcalled";
    /**
     * Shared constant for internal source URL.
     */
    private static final String INTERNAL_SOURCE_URL = "\n//# sourceURL=__lancia_evaluation_script__\n";
    /**
     * Current session.
     */
    private final CDPSession session;
    /**
     * Current identifier.
     */
    private final int id;
    /**
     * Current name.
     */
    private final String name;
    /**
     * Current text writer.
     */
    private final TextWriter textWriter = new TextWriter();
    /**
     * Mapped exposed functions values.
     */
    private final Map<String, ExposedFunction> exposedFunctions = new java.util.concurrent.ConcurrentHashMap<>();
    /**
     * Thread-safe disposed state.
     */
    private final AtomicBoolean disposed = new AtomicBoolean();

    /**
     * Creates a CDP execution context.
     *
     * @param session session
     */
    public CdpExecutionContext(CDPSession session) {
        this(session, 0, Normal.EMPTY);
    }

    /**
     * Creates a CDP execution context.
     *
     * @param session session
     * @param id      id
     * @param name    name
     */
    public CdpExecutionContext(CDPSession session, int id, String name) {
        this.session = Assert.notNull(session, "session");
        this.id = id;
        this.name = name == null ? Normal.EMPTY : name;
        installEventListeners();
        Logger.debug(false, "Page", "Execution context initialized: contextId={}, name={}", this.id, this.name);
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public int id() {
        return id;
    }

    /**
     * Returns the name.
     *
     * @return name value
     */
    public String name() {
        return name;
    }

    /**
     * Returns the disposed.
     *
     * @return {@code true} when the condition matches
     */
    public boolean disposed() {
        return disposed.get();
    }

    /**
     * Returns the evaluate remote object.
     *
     * @param expression JavaScript expression
     * @return evaluate remote object value
     */
    public CdpPayload evaluateRemoteObject(String expression) {
        return evaluateRemoteObject(expression, true);
    }

    /**
     * Returns the evaluate remote object.
     *
     * @param expression    JavaScript expression
     * @param returnByValue whether the result should be returned by value
     * @return evaluate remote object value
     */
    public CdpPayload evaluateRemoteObject(String expression, boolean returnByValue) {
        Logger.debug(
                true,
                "Page",
                "Execution context evaluate requested: contextId={}, returnByValue={}, expressionChars={}",
                id,
                returnByValue,
                expression == null ? Normal._0 : expression.length());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("expression", expressionWithSourceUrl(expression));
        if (id > Normal._0) {
            params.put("contextId", id);
        }
        params.put("returnByValue", returnByValue);
        params.put("awaitPromise", true);
        params.put("userGesture", true);
        CdpPayload response = rewriteEvaluationResponse(
                await(session.send("Runtime.evaluate", params), "Runtime.evaluate failed."));
        throwIfEvaluationFailed(response);
        Logger.debug(false, "Page", "Execution context evaluate completed: contextId={}", id);
        return response.get("result");
    }

    /**
     * Returns the evaluate.
     *
     * @param expression JavaScript expression
     * @return evaluate value
     */
    public Object evaluate(String expression) {
        return valueFromRemoteObject(evaluateRemoteObject(expression));
    }

    /**
     * Returns the evaluate handle.
     *
     * @param expression JavaScript expression
     * @return evaluate handle value
     */
    public CdpJSHandle evaluateHandle(String expression) {
        return createHandle(evaluateRemoteObject(expression, false));
    }

    /**
     * Returns the call function on.
     *
     * @param functionDeclaration function declaration value
     * @return call function on value
     */
    public Object callFunctionOn(String functionDeclaration) {
        return callFunctionOn(functionDeclaration, new Object[0]);
    }

    /**
     * Returns the call function on.
     *
     * @param functionDeclaration function declaration value
     * @param args                arguments to pass
     * @return call function on value
     */
    public Object callFunctionOn(String functionDeclaration, Object... args) {
        return valueFromRemoteObject(callFunctionOnRemoteObject(functionDeclaration, true, args));
    }

    /**
     * Returns the call function on handle.
     *
     * @param functionDeclaration function declaration value
     * @param args                arguments to pass
     * @return call function on handle value
     */
    public CdpJSHandle callFunctionOnHandle(String functionDeclaration, Object... args) {
        return createHandle(callFunctionOnRemoteObject(functionDeclaration, false, args));
    }

    /**
     * Returns the call function on remote object.
     *
     * @param functionDeclaration function declaration value
     * @param returnByValue       whether the result should be returned by value
     * @param args                arguments to pass
     * @return call function on remote object value
     */
    public CdpPayload callFunctionOnRemoteObject(String functionDeclaration, boolean returnByValue, Object... args) {
        Logger.debug(
                true,
                "Page",
                "Execution context function call requested: contextId={}, returnByValue={}, functionChars={}, argCount={}",
                id,
                returnByValue,
                functionDeclaration == null ? Normal._0 : functionDeclaration.length(),
                args == null ? Normal._0 : args.length);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("functionDeclaration", expressionWithSourceUrl(functionDeclaration));
        if (id > Normal._0) {
            params.put("executionContextId", id);
        }
        params.put("arguments", convertArguments(args));
        params.put("returnByValue", returnByValue);
        params.put("awaitPromise", true);
        params.put("userGesture", true);
        CdpPayload response = rewriteEvaluationResponse(
                await(session.send("Runtime.callFunctionOn", params), "Runtime.callFunctionOn failed."));
        throwIfEvaluationFailed(response);
        Logger.debug(false, "Page", "Execution context function call completed: contextId={}", id);
        return response.get("result");
    }

    /**
     * Adds exposed function.
     *
     * @param function exposed function
     */
    public void addExposedFunction(ExposedFunction function) {
        ExposedFunction actualFunction = Assert.notNull(function, "function");
        if (exposedFunctions.containsKey(actualFunction.name())) {
            Logger.debug(
                    false,
                    "Page",
                    "Execution context exposed function already exists: contextId={}, name={}",
                    id,
                    actualFunction.name());
            return;
        }
        Logger.debug(
                true,
                "Page",
                "Execution context exposed function add requested: contextId={}, name={}",
                id,
                actualFunction.name());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", actualFunction.name());
        if (StringKit.isNotBlank(name)) {
            params.put("executionContextName", name);
        } else if (id > Normal._0) {
            params.put("executionContextId", id);
        }
        try {
            await(session.send("Runtime.addBinding", params), "Failed to install Runtime binding.");
            if (StringKit.isNotBlank(actualFunction.initSource())) {
                evaluate(actualFunction.initSource());
            }
            exposedFunctions.put(actualFunction.name(), actualFunction);
            Logger.debug(
                    false,
                    "Page",
                    "Execution context exposed function added: contextId={}, name={}",
                    id,
                    actualFunction.name());
        } catch (InternalException ex) {
            String message = String.valueOf(ex.getMessage());
            if (message.contains("Execution context was destroyed")
                    || message.contains("Cannot find context with specified id")) {
                Logger.warn(
                        false,
                        "Page",
                        "Execution context exposed function add ignored after context destroy: contextId={}, name={}",
                        id,
                        actualFunction.name());
                return;
            }
            Logger.error(
                    false,
                    "Page",
                    "Execution context exposed function add failed: contextId={}, name={}, message={}",
                    id,
                    actualFunction.name(),
                    ex.getMessage());
            throw ex;
        }
    }

    /**
     * Returns the exposed function.
     *
     * @param name name to use
     * @return exposed function value
     */
    public ExposedFunction exposedFunction(String name) {
        return exposedFunctions.get(name);
    }

    /**
     * Removes an exposed function from the execution context.
     *
     * @param name exposed function name
     */
    public void removeExposedFunction(String name) {
        String actualName = Assert.notBlank(name, "name");
        exposedFunctions.remove(actualName);
        await(session.send("Runtime.removeBinding", Map.of("name", actualName)), "Failed to remove Runtime binding.");
        evaluate("globalThis[" + literal(actualName) + "]=undefined");
        Logger.debug(
                false,
                "Page",
                "Execution context exposed function removed: contextId={}, name={}",
                id,
                actualName);
    }

    /**
     * Returns the literal.
     *
     * @param value to use
     * @return literal value
     */
    public String literal(Object value) {
        return textWriter.writeValue(value);
    }

    /**
     * Disposes context state and emits the disposed event.
     */
    public void dispose() {
        if (disposed.compareAndSet(false, true)) {
            Logger.debug(true, "Page", "Execution context dispose requested: contextId={}", id);
            emit(DISPOSED, CdpPayload.NULL);
            Logger.debug(false, "Page", "Execution context disposed: contextId={}", id);
        }
    }

    /**
     * Closes this context by disposing it.
     */
    @Override
    public void close() {
        dispose();
    }

    /**
     * Handles install event listeners.
     */
    private void installEventListeners() {
        session.on("Runtime.bindingCalled", this::onBindingCalled);
        session.on("Runtime.executionContextDestroyed", this::onExecutionContextDestroyed);
        session.on("Runtime.executionContextsCleared", payload -> dispose());
        session.on("Runtime.consoleAPICalled", this::onConsoleApiCalled);
        session.once(CDPSession.Events.DISCONNECTED, payload -> dispose());
        Logger.debug(false, "Page", "Execution context protocol listeners installed: contextId={}", id);
    }

    /**
     * Handles on binding called.
     *
     * @param event event type
     */
    private void onBindingCalled(CdpPayload event) {
        if (!matchesContext(event.get("executionContextId"))) {
            return;
        }
        CdpPayload payload;
        try {
            payload = CdpPayload.parse(PayloadReader.text(event.get("payload")));
        } catch (RuntimeException ex) {
            emit(BINDING_CALLED, event);
            return;
        }
        String type = PayloadReader.text(payload.get("type"));
        String bindingName = PayloadReader.text(payload.get("name"));
        ExposedFunction function = exposedFunctions.get(bindingName);
        if (!"internal".equals(type) || function == null) {
            emit(BINDING_CALLED, event);
            Logger.debug(
                    false,
                    "Page",
                    "Execution context binding event emitted: contextId={}, name={}",
                    id,
                    bindingName);
            return;
        }
        boolean isTrivial = payload.get("isTrivial").isNull() || payload.get("isTrivial").asBoolean();
        Logger.debug(
                false,
                "Page",
                "Execution context internal binding called: contextId={}, name={}, trivial={}",
                id,
                bindingName,
                isTrivial);
        function.run(this, payload.get("seq").asInt(), PayloadReader.array(payload.get("args")), isTrivial);
    }

    /**
     * Handles on execution context destroyed.
     *
     * @param event event type
     */
    private void onExecutionContextDestroyed(CdpPayload event) {
        if (matchesContext(event.get("executionContextId"))) {
            Logger.debug(false, "Page", "Execution context destroyed by protocol: contextId={}", id);
            dispose();
        }
    }

    /**
     * Handles on console api called.
     *
     * @param event event type
     */
    private void onConsoleApiCalled(CdpPayload event) {
        if (matchesContext(event.get("executionContextId"))) {
            emit(CONSOLE_API_CALLED, event);
            Logger.debug(false, "Page", "Execution context console event emitted: contextId={}", id);
        }
    }

    /**
     * Returns the matches context.
     *
     * @param contextId context ID value
     * @return {@code true} when the condition matches
     */
    private boolean matchesContext(CdpPayload contextId) {
        return id <= Normal._0 || (!contextId.isNull() && contextId.asInt() == id);
    }

    /**
     * Creates handle.
     *
     * @param remoteObject remote object payload
     * @return create handle value
     */
    private CdpJSHandle createHandle(CdpPayload remoteObject) {
        CdpJSHandle handle = new CdpJSHandle(remoteObject, session);
        CdpElementHandle element = handle.asElement();
        return element == null ? handle : element;
    }

    /**
     * Returns the convert arguments.
     *
     * @param args arguments to pass
     * @return values
     */
    private List<Map<String, Object>> convertArguments(Object... args) {
        if (args == null || args.length == Normal._0) {
            return List.of();
        }
        List<Map<String, Object>> arguments = new ArrayList<>();
        for (Object arg : args) {
            arguments.add(convertArgument(arg));
        }
        return List.copyOf(arguments);
    }

    /**
     * Returns the convert argument.
     *
     * @param arg arg value
     * @return mapped values
     */
    private Map<String, Object> convertArgument(Object arg) {
        if (arg instanceof LazyArg<?, ?> lazyArg) {
            return convertArgument(resolveLazyArg(lazyArg));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        if (arg instanceof CdpJSHandle handle) {
            assertHandleUsable(handle);
            CdpPayload remoteObject = handle.remoteObject();
            if (!remoteObject.get("unserializableValue").isNull()) {
                result.put("unserializableValue", remoteObject.get("unserializableValue").raw());
            } else if (StringKit.isNotBlank(handle.id())) {
                result.put("objectId", handle.id());
            } else {
                result.put("value", remoteObject.get("value").raw());
            }
            return result;
        }
        if (arg instanceof BigInteger value) {
            result.put("unserializableValue", value + "n");
            return result;
        }
        if (arg instanceof Double value) {
            putDoubleArgument(result, value);
            return result;
        }
        if (arg instanceof Float value) {
            putDoubleArgument(result, value.doubleValue());
            return result;
        }
        result.put("value", arg);
        return result;
    }

    /**
     * Returns the resolve lazy arg.
     *
     * @param lazyArg lazy arg value
     * @return resolve lazy arg value
     */
    private Object resolveLazyArg(LazyArg<?, ?> lazyArg) {
        return ((LazyArg<Object, LazyArg.InjectedRuntime<CdpExecutionContext, CdpJSHandle>>) lazyArg)
                .get(LazyArg.context(this));
    }

    /**
     * Handles put double argument.
     *
     * @param result result value
     * @param value  value to use
     */
    private void putDoubleArgument(Map<String, Object> result, double value) {
        if (Double.isNaN(value)) {
            result.put("unserializableValue", "NaN");
        } else if (value == Double.POSITIVE_INFINITY) {
            result.put("unserializableValue", "Infinity");
        } else if (value == Double.NEGATIVE_INFINITY) {
            result.put("unserializableValue", "-Infinity");
        } else if (Double.doubleToRawLongBits(value) == Double.doubleToRawLongBits(-0.0d)) {
            result.put("unserializableValue", "-0");
        } else {
            result.put("value", value);
        }
    }

    /**
     * Asserts the handle usable condition.
     *
     * @param handle handle
     */
    private void assertHandleUsable(CdpJSHandle handle) {
        if (handle.disposed()) {
            throw new InternalException("CdpJSHandle is disposed!");
        }
        handle.session().ifPresent(handleSession -> {
            if (handleSession != session) {
                throw new InternalException("JSHandles can be evaluated only in the context they were created!");
            }
        });
    }

    /**
     * Returns the value from remote object.
     *
     * @param remoteObject remote object payload
     * @return value from remote object value
     */
    private Object valueFromRemoteObject(CdpPayload remoteObject) {
        CdpPayload unserializableValue = remoteObject.get("unserializableValue");
        if (!unserializableValue.isNull()) {
            return unserializableValue(PayloadReader.text(unserializableValue));
        }
        CdpPayload value = remoteObject.get("value");
        if (!value.isNull()) {
            return PayloadReader.value(value);
        }
        return remoteObject.get("description").isNull() ? null : remoteObject.get("description").raw();
    }

    /**
     * Returns the unserializable value.
     *
     * @param value to use
     * @return unserializable value
     */
    private Object unserializableValue(String value) {
        return switch (value) {
            case "NaN" -> Double.NaN;
            case "Infinity" -> Double.POSITIVE_INFINITY;
            case "-Infinity" -> Double.NEGATIVE_INFINITY;
            case "-0" -> -0.0d;
            default -> value.endsWith("n") ? new BigInteger(value.substring(0, value.length() - 1)) : value;
        };
    }

    /**
     * Returns the expression with source URL.
     *
     * @param expression JavaScript expression
     * @return expression with source URL value
     */
    private String expressionWithSourceUrl(String expression) {
        String actual = expression == null ? Normal.EMPTY : expression;
        return actual.contains("sourceURL=") ? actual : actual + INTERNAL_SOURCE_URL;
    }

    /**
     * Returns the rewrite evaluation response.
     *
     * @param response response object
     * @return rewrite evaluation response value
     */
    private CdpPayload rewriteEvaluationResponse(CdpPayload response) {
        return response == null ? CdpPayload.NULL : response;
    }

    /**
     * Handles throw if evaluation failed.
     *
     * @param response response object
     */
    private void throwIfEvaluationFailed(CdpPayload response) {
        CdpPayload exceptionDetails = response.get("exceptionDetails");
        if (!exceptionDetails.isNull()) {
            String text = PayloadReader.text(exceptionDetails.get("text"));
            String description = PayloadReader.text(exceptionDetails.get("exception").get("description"));
            Logger.error(
                    false,
                    "Page",
                    "Execution context evaluation failed: contextId={}, message={}",
                    id,
                    StringKit.isBlank(description) ? text : description);
            throw new InternalException(StringKit.isBlank(description) ? text : description);
        }
    }

    /**
     * Returns the await.
     *
     * @param future  future value
     * @param message message text
     * @return await value
     */
    private CdpPayload await(java.util.concurrent.CompletableFuture<CdpPayload> future, String message) {
        try {
            return future.get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            Logger.error(
                    false,
                    "Page",
                    "Execution context protocol await interrupted: contextId={}, message={}",
                    id,
                    ex.getMessage());
            throw new InternalException(message, ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            if (isUndefinedEvaluationError(cause)) {
                Logger.warn(
                        false,
                        "Page",
                        "Execution context evaluation returned undefined fallback: contextId={}",
                        id);
                return CdpPayload.of(Map.of("result", Map.of("type", "undefined")));
            }
            Logger.error(
                    false,
                    "Page",
                    "Execution context protocol await failed: contextId={}, message={}",
                    id,
                    cause.getMessage());
            throw rewriteError(cause, message);
        } catch (Exception ex) {
            Logger.error(
                    false,
                    "Page",
                    "Execution context protocol await failed: contextId={}, message={}",
                    id,
                    ex.getMessage());
            throw new InternalException(message, ex);
        }
    }

    /**
     * Returns whether undefined evaluation error is enabled.
     *
     * @param error error to propagate
     * @return {@code true} when the condition matches
     */
    private boolean isUndefinedEvaluationError(Throwable error) {
        String errorMessage = String.valueOf(error == null ? null : error.getMessage());
        return errorMessage.contains("Object reference chain is too long")
                || errorMessage.contains("Object couldn't be returned by value");
    }

    /**
     * Returns the rewrite error.
     *
     * @param error   error to propagate
     * @param message message text
     * @return rewrite error value
     */
    private InternalException rewriteError(Throwable error, String message) {
        String errorMessage = String.valueOf(error == null ? null : error.getMessage());
        if (errorMessage.endsWith("Cannot find context with specified id")
                || errorMessage.endsWith("Inspected target navigated or closed")) {
            return new InternalException("Execution context was destroyed, most likely because of a navigation.",
                    error);
        }
        return new InternalException(message, error);
    }

}
