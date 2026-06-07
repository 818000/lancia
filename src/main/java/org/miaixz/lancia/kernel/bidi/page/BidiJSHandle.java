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
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.kernel.Handle;
import org.miaixz.lancia.kernel.bidi.accessor.BidiSession;
import org.miaixz.lancia.kernel.bidi.protocol.BidiDeserializer;
import org.miaixz.lancia.kernel.bidi.protocol.BidiSerializer;
import org.miaixz.lancia.kernel.bidi.protocol.BidiValue;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents BiDi JS handle.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiJSHandle implements Handle {

    /**
     * Current remote value.
     */
    private final CdpPayload remoteValue;
    /**
     * Current page.
     */
    private final BidiPage page;
    /**
     * Thread-safe disposed state.
     */
    private final AtomicBoolean disposed = new AtomicBoolean(false);

    /**
     * Returns the from.
     *
     * @param value to use
     * @param page  page instance
     * @return from value
     */
    public static BidiJSHandle from(CdpPayload value, BidiPage page) {
        return new BidiJSHandle(value, page);
    }

    /**
     * Creates a bidi JS handle.
     *
     * @param remoteValue remote value
     * @param page        page instance
     */
    public BidiJSHandle(CdpPayload remoteValue, BidiPage page) {
        this.remoteValue = Assert.notNull(remoteValue, "remoteValue");
        this.page = Assert.notNull(page, "page");
    }

    /**
     * Returns the remote value.
     *
     * @return remote value
     */
    public CdpPayload remoteValue() {
        return remoteValue;
    }

    /**
     * Returns the page.
     *
     * @return page value
     */
    public BidiPage page() {
        return page;
    }

    /**
     * Returns the ID.
     *
     * @return optional value
     */
    public String id() {
        return optionalId().orElse(Normal.EMPTY);
    }

    /**
     * Returns the optional ID.
     *
     * @return optional value
     */
    public Optional<String> optionalId() {
        String handle = PayloadReader.text(remoteValue.get("handle"));
        return StringKit.isBlank(handle) ? Optional.empty() : Optional.of(handle);
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
     * Returns whether primitive value is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isPrimitiveValue() {
        return BidiValue.primitive(remoteValue);
    }

    /**
     * Returns the json value.
     *
     * @return JSON value
     */
    public Object jsonValue() {
        return BidiDeserializer.deserialize(remoteValue);
    }

    /**
     * Returns the as element.
     *
     * @return optional value
     */
    public BidiElementHandle asElement() {
        return null;
    }

    /**
     * Returns the as element.
     *
     * @return optional value
     */
    public Optional<BidiElementHandle> optionalElement() {
        return Optional.empty();
    }

    /**
     * Releases resources held by this object.
     *
     * @return disposal result
     */
    public CompletableFuture<CdpPayload> dispose() {
        if (!disposed.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        Optional<String> handle = optionalId();
        if (handle.isEmpty()) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        return session().send("script.disown", Map.of("handles", List.of(handle.getOrThrow()), "target", target()));
    }

    /**
     * Returns the evaluate.
     *
     * @param functionDeclaration function declaration value
     * @param arguments           arguments value
     * @return completion future
     */
    public CompletableFuture<Object> evaluate(String functionDeclaration, Object... arguments) {
        return callFunction(functionDeclaration, "none", arguments)
                .thenApply(result -> BidiDeserializer.deserialize(result.get("result")));
    }

    /**
     * Returns the evaluate.
     *
     * @param functionDeclaration function declaration value
     * @return result
     */
    public Object evaluate(String functionDeclaration) {
        return Awaitable.await(evaluate(functionDeclaration, new Object[0]), "BiDi handle evaluate failed.", 5_000L);
    }

    /**
     * Returns the evaluate handle.
     *
     * @param functionDeclaration function declaration value
     * @param arguments           arguments value
     * @return completion future
     */
    public CompletableFuture<BidiJSHandle> evaluateHandle(String functionDeclaration, Object... arguments) {
        return callFunction(functionDeclaration, "root", arguments)
                .thenApply(result -> new BidiJSHandle(result.get("result"), page));
    }

    /**
     * Returns the evaluate handle.
     *
     * @param functionDeclaration function declaration value
     * @return handle
     */
    public BidiJSHandle evaluateHandle(String functionDeclaration) {
        return Awaitable
                .await(evaluateHandle(functionDeclaration, new Object[0]), "BiDi handle evaluate failed.", 5_000L);
    }

    /**
     * Returns a property by name.
     *
     * @param name property name
     * @return property handle
     */
    public Optional<BidiJSHandle> getProperty(String name) {
        return Optional.ofNullable(getProperties().get(name));
    }

    /**
     * Returns the properties.
     *
     * @return mapped values
     */
    public Map<String, BidiJSHandle> getProperties() {
        CdpPayload value = remoteValue.get("value");
        if (!value.isArray()) {
            return Map.of();
        }
        Map<String, BidiJSHandle> result = new LinkedHashMap<>();
        for (CdpPayload tuple : value.elements()) {
            List<CdpPayload> elements = tuple.elements();
            if (elements.size() >= 2) {
                result.put(PayloadReader.text(elements.get(0)), new BidiJSHandle(elements.get(1), page));
            }
        }
        return Map.copyOf(result);
    }

    /**
     * Returns the remote object.
     *
     * @return remote object value
     */
    public CdpPayload remoteObject() {
        Map<String, Object> object = new LinkedHashMap<>();
        String type = PayloadReader.text(remoteValue.get("type"));
        Optional<String> handle = optionalId();
        switch (type) {
            case "undefined" -> object.put("type", "undefined");
            case "null" -> {
                object.put("type", "object");
                object.put("subtype", "null");
                object.put("value", null);
            }
            case "boolean", "string" -> {
                object.put("type", type);
                object.put("value", remoteValue.get("value").raw());
            }
            case "number" -> {
                object.put("type", "number");
                putNumberValue(object, remoteValue.get("value"));
            }
            case "bigint" -> {
                object.put("type", "bigint");
                object.put("unserializableValue", bigintValue(PayloadReader.text(remoteValue.get("value"))));
            }
            case "function" -> object.put("type", "function");
            case "array" -> {
                object.put("type", "object");
                object.put("subtype", "array");
                object.put("description", arrayDescription(remoteValue.get("value")));
            }
            case "map", "set", "date", "regexp", "promise", "node" -> {
                object.put("type", "object");
                object.put("subtype", type);
                object.put("description", description(type));
            }
            default -> {
                object.put("type", "object");
                if (StringKit.isNotBlank(type)) {
                    object.put("subtype", type);
                }
                object.put("description", description(type));
            }
        }
        handle.ifPresent(value -> object.put("objectId", value));
        String description = PayloadReader.text(remoteValue.get("description"));
        if (StringKit.isNotBlank(description)) {
            object.put("description", description);
        }
        String className = PayloadReader.text(remoteValue.get("className"));
        if (StringKit.isNotBlank(className)) {
            object.put("className", className);
        }
        if (!handle.isPresent() && !remoteValue.get("value").isNull() && !object.containsKey("value")
                && !object.containsKey("unserializableValue")) {
            object.put("value", BidiDeserializer.deserialize(remoteValue));
        }
        return CdpPayload.of(object);
    }

    /**
     * Returns the call function.
     *
     * @param functionDeclaration function declaration value
     * @param resultOwnership     result ownership value
     * @param arguments           arguments value
     * @return completion future
     */
    private CompletableFuture<CdpPayload> callFunction(
            String functionDeclaration,
            String resultOwnership,
            Object... arguments) {
        List<Object> args = new ArrayList<>();
        args.add(remoteArgument());
        if (arguments != null) {
            for (Object argument : arguments) {
                args.add(remoteValue(argument));
            }
        }
        return session().send(
                "script.callFunction",
                Map.of(
                        "functionDeclaration",
                        Assert.notBlank(functionDeclaration, "functionDeclaration"),
                        "awaitPromise",
                        true,
                        "target",
                        target(),
                        "resultOwnership",
                        resultOwnership,
                        "arguments",
                        args));
    }

    /**
     * Returns the remote argument.
     *
     * @return remote argument value
     */
    private Object remoteArgument() {
        Optional<String> handle = optionalId();
        return handle.<Object>map(value -> Map.of("handle", value)).orElseGet(() -> remoteValue(jsonValue()));
    }

    /**
     * Returns the remote value.
     *
     * @param value to use
     * @return remote value
     */
    private Object remoteValue(Object value) {
        return BidiSerializer.serialize(value);
    }

    /**
     * Handles put number value.
     *
     * @param object object to inspect
     * @param value  value to use
     */
    private static void putNumberValue(Map<String, Object> object, CdpPayload value) {
        Object raw = value.raw();
        if (raw instanceof Number number) {
            object.put("value", number);
            return;
        }
        String text = PayloadReader.text(value);
        if ("NaN".equals(text) || "Infinity".equals(text) || "-Infinity".equals(text) || "-0".equals(text)) {
            object.put("unserializableValue", text);
        } else {
            object.put("value", text);
        }
    }

    /**
     * Returns the bigint value.
     *
     * @param value to use
     * @return BigInt value
     */
    private static String bigintValue(String value) {
        return value.endsWith("n") ? value : value + "n";
    }

    /**
     * Returns the array description.
     *
     * @param value to use
     * @return array description value
     */
    private static String arrayDescription(CdpPayload value) {
        return value != null && value.isArray() ? "Array(" + value.elements().size() + Symbol.PARENTHESE_RIGHT
                : "Array";
    }

    /**
     * Returns the description.
     *
     * @param type type to use
     * @return description value
     */
    private static String description(String type) {
        return StringKit.isBlank(type) ? "Object" : type;
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
     * Returns the target.
     *
     * @return mapped values
     */
    private Map<String, Object> target() {
        return Map.of("context", page.contextId());
    }

    /**
     * Converts this value to string.
     *
     * @return string
     */
    @Override
    public String toString() {
        if (isPrimitiveValue()) {
            return "JSHandle:" + jsonValue();
        }
        return "JSHandle@" + PayloadReader.text(remoteValue.get("type"))
                + optionalId().map(value -> Symbol.AT + value).orElse(Normal.EMPTY);
    }

}
