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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.kernel.Handle;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents a handle to a JavaScript value in a page execution context.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpJSHandle implements Handle {

    /**
     * Current remote object.
     */
    private final CdpPayload remoteObject;

    /**
     * CDP session.
     */
    private final CDPSession session;
    /**
     * Current realm.
     */
    private final CdpIsolatedWorld realm;
    /**
     * Thread-safe disposed state.
     */
    private final AtomicBoolean disposed = new AtomicBoolean();

    /**
     * Creates a JS handle.
     *
     * @param remoteObject remote object payload
     */
    public CdpJSHandle(CdpPayload remoteObject) {
        this(remoteObject, null);
    }

    /**
     * Creates a JS handle.
     *
     * @param remoteObject remote object payload
     * @param session      protocol session
     */
    public CdpJSHandle(CdpPayload remoteObject, CDPSession session) {
        this(remoteObject, session, null);
    }

    /**
     * Creates a JS handle.
     *
     * @param realm        realm
     * @param remoteObject remote object payload
     */
    public CdpJSHandle(CdpIsolatedWorld realm, CdpPayload remoteObject) {
        this(remoteObject, realm == null ? null : realm.client(), realm);
    }

    /**
     * Creates a JS handle.
     *
     * @param remoteObject remote object payload
     * @param session      protocol session
     * @param realm        realm
     */
    private CdpJSHandle(CdpPayload remoteObject, CDPSession session, CdpIsolatedWorld realm) {
        this.remoteObject = remoteObject == null ? CdpPayload.NULL : remoteObject;
        this.session = session;
        this.realm = realm;
    }

    /**
     * Returns the remote object.
     *
     * @return remote object value
     */
    public CdpPayload remoteObject() {
        return remoteObject;
    }

    /**
     * Returns the session.
     *
     * @return optional value
     */
    Optional<CDPSession> session() {
        return Optional.ofNullable(session);
    }

    /**
     * Returns the client.
     *
     * @return client value
     */
    CDPSession client() {
        return session;
    }

    /**
     * Returns the realm.
     *
     * @return optional value
     */
    public Optional<CdpIsolatedWorld> realm() {
        return Optional.ofNullable(realm);
    }

    /**
     * Returns the description.
     *
     * @return description value
     */
    public String description() {
        CdpPayload description = remoteObject.get("description");
        return description.isNull() ? Normal.EMPTY : description.asText();
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        CdpPayload objectId = remoteObject.get("objectId");
        return objectId.isNull() ? null : objectId.asText();
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
     * Returns the json value.
     *
     * @return JSON value
     */
    public Object jsonValue() {
        if (StringKit.isBlank(id())) {
            return valueFromPrimitiveRemoteObject(remoteObject);
        }
        CdpPayload result = callFunctionOn("function(object){return object;}", true);
        if ("undefined".equals(PayloadReader.text(result.get("type"))) && result.get("value").isNull()) {
            throw new InternalException("Could not serialize referenced object");
        }
        return valueFromPrimitiveRemoteObject(result);
    }

    /**
     * Returns the as element.
     *
     * @return as element value
     */
    public CdpElementHandle asElement() {
        String subtype = remoteObject.get("subtype").isNull() ? Normal.EMPTY : remoteObject.get("subtype").asText();
        if ("node".equals(subtype) || "element".equals(subtype)) {
            return new CdpElementHandle(remoteObject, session);
        }
        return null;
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
        if (session == null || StringKit.isBlank(id())) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        return releaseObject(session, remoteObject);
    }

    /**
     * Returns the property.
     *
     * @param name name to use
     * @return property
     */
    public Optional<CdpJSHandle> getProperty(String name) {
        return Optional.ofNullable(getProperties().get(name));
    }

    /**
     * Returns the properties.
     *
     * @return mapped values
     */
    public Map<String, CdpJSHandle> getProperties() {
        if (session == null || StringKit.isBlank(id())) {
            return Map.of();
        }
        CdpPayload result = Awaitable.await(
                session.send("Runtime.getProperties", Map.of("objectId", id(), "ownProperties", true)),
                "Failed to read CdpJSHandle properties.");
        Map<String, CdpJSHandle> properties = new LinkedHashMap<>();
        if (result.get("result").isArray()) {
            for (CdpPayload descriptor : result.get("result").elements()) {
                String name = descriptor.get("name").isNull() ? Normal.EMPTY : descriptor.get("name").asText();
                CdpPayload value = descriptor.get("value");
                boolean enumerable = descriptor.get("enumerable").isNull() || descriptor.get("enumerable").asBoolean();
                if (enumerable && StringKit.isNotBlank(name) && !value.isNull()) {
                    properties.put(name, createHandle(value));
                }
            }
        }
        return Map.copyOf(properties);
    }

    /**
     * Returns the evaluate.
     *
     * @param functionDeclaration function declaration value
     * @return evaluate value
     */
    public Object evaluate(String functionDeclaration) {
        CdpPayload result = callFunctionOn(functionDeclaration, true);
        return valueFromPrimitiveRemoteObject(result);
    }

    /**
     * Returns the evaluate handle.
     *
     * @param functionDeclaration function declaration value
     * @return evaluate handle value
     */
    public CdpJSHandle evaluateHandle(String functionDeclaration) {
        return createHandle(callFunctionOn(functionDeclaration, false));
    }

    /**
     * Returns the call function on.
     *
     * @param functionDeclaration function declaration value
     * @param returnByValue       whether the result should be returned by value
     * @return call function on value
     */
    private CdpPayload callFunctionOn(String functionDeclaration, boolean returnByValue) {
        ensureUsable();
        if (session == null || StringKit.isBlank(id())) {
            return CdpPayload.NULL;
        }
        return Awaitable.await(
                session.send(
                        "Runtime.callFunctionOn",
                        Map.of(
                                "objectId",
                                id(),
                                "functionDeclaration",
                                functionDeclaration,
                                "returnByValue",
                                returnByValue,
                                "awaitPromise",
                                true)),
                "Failed to call CdpJSHandle function.").get("result");
    }

    /**
     * Creates handle.
     *
     * @param remoteObject remote object payload
     * @return create handle value
     */
    private CdpJSHandle createHandle(CdpPayload remoteObject) {
        if (realm != null) {
            return realm.createCdpHandle(remoteObject);
        }
        CdpJSHandle handle = new CdpJSHandle(remoteObject, session);
        CdpElementHandle element = handle.asElement();
        return element == null ? handle : element;
    }

    /**
     * Handles ensure usable.
     */
    private void ensureUsable() {
        if (disposed()) {
            throw new InternalException("CdpJSHandle is disposed!");
        }
    }

    /**
     * Returns the release object.
     *
     * @param client       protocol client
     * @param remoteObject remote object payload
     * @return completion future
     */
    public static CompletableFuture<CdpPayload> releaseObject(CDPSession client, CdpPayload remoteObject) {
        if (client == null || remoteObject == null
                || StringKit.isBlank(PayloadReader.text(remoteObject.get("objectId")))) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        return client
                .send("Runtime.releaseObject", Map.of("objectId", PayloadReader.text(remoteObject.get("objectId"))))
                .exceptionally(error -> CdpPayload.NULL);
    }

    /**
     * Returns the value from primitive remote object.
     *
     * @param remoteObject remote object payload
     * @return value from primitive remote object value
     */
    static Object valueFromPrimitiveRemoteObject(CdpPayload remoteObject) {
        if (remoteObject == null || remoteObject.isNull()) {
            return null;
        }
        CdpPayload unserializable = remoteObject.get("unserializableValue");
        if (!unserializable.isNull()) {
            return unserializableValue(PayloadReader.text(unserializable));
        }
        CdpPayload value = remoteObject.get("value");
        if (!value.isNull()) {
            return value.raw();
        }
        if ("undefined".equals(PayloadReader.text(remoteObject.get("type")))) {
            return null;
        }
        return remoteObject.get("description").isNull() ? null : remoteObject.get("description").raw();
    }

    /**
     * Returns the unserializable value.
     *
     * @param value to use
     * @return unserializable value
     */
    private static Object unserializableValue(String value) {
        return switch (value) {
            case "NaN" -> Double.NaN;
            case "Infinity" -> Double.POSITIVE_INFINITY;
            case "-Infinity" -> Double.NEGATIVE_INFINITY;
            case "-0" -> -0.0d;
            default -> value != null && value.endsWith("n")
                    ? new java.math.BigInteger(value.substring(0, value.length() - 1))
                    : value;
        };
    }

    /**
     * Converts this value to string.
     *
     * @return string
     */
    @Override
    public String toString() {
        if (StringKit.isBlank(id())) {
            return "CdpJSHandle:" + valueFromPrimitiveRemoteObject(remoteObject);
        }
        String type = StringKit.isNotBlank(PayloadReader.text(remoteObject.get("subtype")))
                ? PayloadReader.text(remoteObject.get("subtype"))
                : PayloadReader.text(remoteObject.get("type"));
        return "CdpJSHandle@" + (StringKit.isBlank(type) ? id() : type);
    }

}
