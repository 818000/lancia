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
package org.miaixz.lancia.kernel.bidi.page;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.kernel.bidi.accessor.BidiSession;
import org.miaixz.lancia.kernel.bidi.protocol.BidiDeserializer;
import org.miaixz.lancia.kernel.bidi.protocol.BidiSerializer;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Defines the BidiExposedFunction class.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiExposedFunction implements AutoCloseable {

    /**
     * Defines the script message constant.
     */
    private static final String SCRIPT_MESSAGE = "script.message";

    /**
     * Defines the add preload script constant.
     */
    private static final String ADD_PRELOAD_SCRIPT = "script.addPreloadScript";

    /**
     * Defines the remove preload script constant.
     */
    private static final String REMOVE_PRELOAD_SCRIPT = "script.removePreloadScript";

    /**
     * Defines the call function constant.
     */
    private static final String CALL_FUNCTION = "script.callFunction";
    /**
     * Current page.
     */
    private final BidiPage page;
    /**
     * Current name.
     */
    private final String name;
    /**
     * Current callback.
     */
    private final ExposedCallback callback;
    /**
     * Whether isolate is enabled.
     */
    private final boolean isolate;
    /**
     * Current channel.
     */
    private final String channel;
    /**
     * Registered scripts values.
     */
    private final List<String> scripts = new ArrayList<>();
    /**
     * Current binding.
     */
    private Binding binding = new EventBinding();
    /**
     * Thread-safe disposed state.
     */
    private final AtomicBoolean disposed = new AtomicBoolean(false);

    /**
     * Returns the from.
     *
     * @param page     page instance
     * @param name     name to use
     * @param callback callback to invoke
     * @return completion future
     */
    public static CompletableFuture<BidiExposedFunction> from(BidiPage page, String name, ExposedCallback callback) {
        return from(page, name, callback, false);
    }

    /**
     * Returns the from.
     *
     * @param page     page instance
     * @param name     name to use
     * @param callback callback to invoke
     * @param isolate  isolate value
     * @return completion future
     */
    public static CompletableFuture<BidiExposedFunction> from(
            BidiPage page,
            String name,
            ExposedCallback callback,
            boolean isolate) {
        BidiExposedFunction function = new BidiExposedFunction(page, name, callback, isolate);
        return function.initialize().thenApply(value -> function);
    }

    /**
     * Creates a bidi exposed function.
     *
     * @param page     the page value
     * @param name     the name value
     * @param callback the callback value
     * @param isolate  the isolate value
     */
    public BidiExposedFunction(BidiPage page, String name, ExposedCallback callback, boolean isolate) {
        this.page = Assert.notNull(page, "page");
        this.name = Assert.notBlank(name, "name");
        this.callback = Assert.notNull(callback, "callback");
        this.isolate = isolate;
        this.channel = "__lancia__" + page.contextId() + "_page_exposeFunction_" + this.name;
    }

    /**
     * Initializes protocol state for this object.
     *
     * @return the result
     */
    public CompletableFuture<CdpPayload> initialize() {
        binding = binding.combine(session().connection().on(SCRIPT_MESSAGE, this::handleMessage));
        return session().send(ADD_PRELOAD_SCRIPT, preloadParams()).thenCompose(result -> {
            String script = PayloadReader.text(result.get("script"));
            if (StringKit.isNotBlank(script)) {
                scripts.add(script);
            }
            return session().send(CALL_FUNCTION, callInstallParams());
        });
    }

    /**
     * Releases resources held by this object.
     *
     * @return the result
     */
    public CompletableFuture<Void> dispose() {
        if (!disposed.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(null);
        }
        binding.unbind();
        CompletableFuture<?>[] removals = scripts.stream()
                .map(script -> session().send(REMOVE_PRELOAD_SCRIPT, Map.of("script", script)))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(removals).thenCompose(value -> session().send(CALL_FUNCTION, callDeleteParams()))
                .thenApply(value -> null);
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        try {
            dispose().get(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new InternalException("Failed to release BiDi exposed function: " + name, ex);
        }
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
     * Returns the channel.
     *
     * @return channel value
     */
    public String channel() {
        return channel;
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
     * Returns the scripts.
     *
     * @return values
     */
    public List<String> scripts() {
        return List.copyOf(scripts);
    }

    /**
     * Handles handle message.
     *
     * @param params protocol parameters
     */
    private void handleMessage(CdpPayload params) {
        if (disposed.get() || !channel.equals(PayloadReader.text(params.get("channel")))) {
            return;
        }
        if (!belongsToPage(params.get("source"))) {
            return;
        }
        try {
            Object result = callback.apply(arguments(params.get("data")));
            resolve(params, result);
        } catch (Throwable throwable) {
            reject(params, throwable);
        }
    }

    /**
     * Handles resolve.
     *
     * @param params protocol parameters
     * @param result result value
     */
    private void resolve(CdpPayload params, Object result) {
        session().send(
                CALL_FUNCTION,
                replyParams(params, "(resolve,result)=>resolve(result)", 0, List.of(BidiSerializer.serialize(result))));
    }

    /**
     * Handles reject.
     *
     * @param params    protocol parameters
     * @param throwable throwable value
     */
    private void reject(CdpPayload params, Throwable throwable) {
        session().send(
                CALL_FUNCTION,
                replyParams(
                        params,
                        "(reject,name,message)=>{const error=new Error(message);error.name=name;reject(error);}",
                        1,
                        List.of(
                                BidiSerializer.serialize(throwable.getClass().getSimpleName()),
                                BidiSerializer.serialize(throwable.getMessage()))));
    }

    /**
     * Returns the belongs to page.
     *
     * @param source source value
     * @return {@code true} when the condition matches
     */
    private boolean belongsToPage(CdpPayload source) {
        String context = PayloadReader.text(source.get("context"));
        return StringKit.isBlank(context) || page.contextId().equals(context);
    }

    /**
     * Returns the arguments.
     *
     * @param data data to use
     * @return values
     */
    private List<Object> arguments(CdpPayload data) {
        CdpPayload values = data.get("value");
        if (!values.isArray()) {
            return List.of();
        }
        List<CdpPayload> elements = values.elements();
        CdpPayload args = elements.size() >= 3 ? elements.get(2).get("value") : values;
        if (!args.isArray()) {
            return List.of();
        }
        List<Object> result = new ArrayList<>();
        for (CdpPayload arg : args.elements()) {
            result.add(BidiDeserializer.deserialize(arg));
        }
        return List.copyOf(result);
    }

    /**
     * Returns the preload params.
     *
     * @return mapped values
     */
    private Map<String, Object> preloadParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("functionDeclaration", installFunctionDeclaration());
        params.put("arguments", List.of(channelArgument()));
        params.put("contexts", List.of(page.contextId()));
        sandbox().ifPresent(value -> params.put("sandbox", value));
        return params;
    }

    /**
     * Returns the call install params.
     *
     * @return mapped values
     */
    private Map<String, Object> callInstallParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("functionDeclaration", installFunctionDeclaration());
        params.put("awaitPromise", true);
        params.put("arguments", List.of(channelArgument()));
        params.put("target", target());
        return params;
    }

    /**
     * Returns the call delete params.
     *
     * @return mapped values
     */
    private Map<String, Object> callDeleteParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("functionDeclaration", "(name)=>{delete globalThis[name];}");
        params.put("awaitPromise", true);
        params.put("arguments", List.of(BidiSerializer.serialize(name)));
        params.put("target", target());
        return params;
    }

    /**
     * Returns the reply params.
     *
     * @param params              protocol parameters
     * @param functionDeclaration function declaration value
     * @param callbackIndex       callback index value
     * @param extraArguments      extra arguments value
     * @return mapped values
     */
    private Map<String, Object> replyParams(
            CdpPayload params,
            String functionDeclaration,
            int callbackIndex,
            List<Object> extraArguments) {
        Map<String, Object> call = new LinkedHashMap<>();
        call.put("functionDeclaration", functionDeclaration);
        call.put("awaitPromise", true);
        call.put("target", target(params.get("source")));
        call.put("arguments", replyArguments(params.get("data"), callbackIndex, extraArguments));
        return call;
    }

    /**
     * Returns the reply arguments.
     *
     * @param data           data to use
     * @param callbackIndex  callback index value
     * @param extraArguments extra arguments value
     * @return values
     */
    private List<Object> replyArguments(CdpPayload data, int callbackIndex, List<Object> extraArguments) {
        List<Object> result = new ArrayList<>();
        CdpPayload values = data.get("value");
        if (values.isArray() && values.elements().size() > callbackIndex) {
            result.add(values.elements().get(callbackIndex).raw());
        }
        result.addAll(extraArguments);
        return List.copyOf(result);
    }

    /**
     * Returns the install function declaration.
     *
     * @return install function declaration value
     */
    private String installFunctionDeclaration() {
        return "(callback)=>{globalThis[" + quote(name)
                + "]=function(...args){return new Promise((resolve,reject)=>callback([resolve,reject,args]));};}";
    }

    /**
     * Returns the channel argument.
     *
     * @return mapped values
     */
    private Map<String, Object> channelArgument() {
        return Map.of("type", "channel", "value", Map.of("channel", channel, "ownership", "root"));
    }

    /**
     * Returns the target.
     *
     * @return mapped values
     */
    private Map<String, Object> target() {
        return target(CdpPayload.NULL);
    }

    /**
     * Returns the target.
     *
     * @param source source value
     * @return mapped values
     */
    private Map<String, Object> target(CdpPayload source) {
        Map<String, Object> target = new LinkedHashMap<>();
        String realm = PayloadReader.text(source.get("realm"));
        if (StringKit.isNotBlank(realm)) {
            target.put("realm", realm);
            return target;
        }
        target.put(
                "context",
                StringKit.isBlank(PayloadReader.text(source.get("context"))) ? page.contextId()
                        : PayloadReader.text(source.get("context")));
        sandbox().ifPresent(value -> target.put("sandbox", value));
        return target;
    }

    /**
     * Returns the sandbox.
     *
     * @return optional value
     */
    private java.util.Optional<String> sandbox() {
        return isolate ? java.util.Optional.of("__lancia_isolated__") : java.util.Optional.empty();
    }

    /**
     * Returns the quote.
     *
     * @param value to use
     * @return quote value
     */
    private String quote(String value) {
        return String.valueOf(Symbol.C_DOUBLE_QUOTES)
                + value.replace(Symbol.BACKSLASH, "\\\\").replace(Symbol.DOUBLE_QUOTES, "\\\"")
                + Symbol.C_DOUBLE_QUOTES;
    }

    /**
     * Returns the session.
     *
     * @return session value
     */
    private BidiSession session() {
        return page.browserContext().browser().session();
    }

    /**
     * Defines the ExposedCallback interface.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    @FunctionalInterface
    public interface ExposedCallback {

        /**
         * Returns the apply.
         *
         * @param arguments arguments value
         * @return apply value
         * @throws Exception if the operation fails
         */
        Object apply(List<Object> arguments) throws Exception;
    }

}
