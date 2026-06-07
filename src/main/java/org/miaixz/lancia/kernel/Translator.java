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
package org.miaixz.lancia.kernel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.kernel.bidi.accessor.BidiSession;
import org.miaixz.lancia.kernel.bidi.page.BidiFrame;
import org.miaixz.lancia.kernel.bidi.runtime.BidiRealm;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Translates the CDP commands used by Lancia into WebDriver BiDi commands.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class Translator {

    /**
     * BiDi session used to send translated commands.
     */
    private final BidiSession session;
    /**
     * Default browsing context ID for context-scoped commands.
     */
    private final String contextId;
    /**
     * Default realm ID for realm-scoped commands.
     */
    private final String realmId;

    /**
     * Creates a translator.
     *
     * @param session   protocol session
     * @param contextId context id
     * @param realmId   realm id
     */
    public Translator(BidiSession session, String contextId, String realmId) {
        this.session = Assert.notNull(session, "session");
        this.contextId = StringKit.isBlank(contextId) ? null : contextId;
        this.realmId = StringKit.isBlank(realmId) ? null : realmId;
    }

    /**
     * Translates a CDP command to the closest WebDriver BiDi operation.
     *
     * @param method protocol method
     * @param params protocol parameters
     * @param realm  realm value
     * @param frame  frame instance
     * @return completion future
     */
    public CompletableFuture<CdpPayload> translate(
            String method,
            Map<String, Object> params,
            BidiRealm realm,
            BidiFrame frame) {
        String actualMethod = Assert.notBlank(method, "method");
        Map<String, Object> actualParams = params == null ? Map.of() : params;
        return switch (actualMethod) {
            case "Browser.getVersion" -> CompletableFuture.completedFuture(browserVersion());
            case "Runtime.evaluate" -> evaluate(actualParams, realm, frame);
            case "Runtime.callFunctionOn" -> callFunction(actualParams, realm, frame);
            case "Runtime.getProperties" -> CompletableFuture
                    .completedFuture(CdpPayload.of(Map.of("result", List.of())));
            case "Runtime.releaseObject" -> releaseObject(actualParams, realm, frame);
            case "DOM.resolveNode" -> resolveNode(actualParams);
            case "Page.captureScreenshot" -> captureScreenshot(actualParams, frame);
            case "Page.printToPDF" -> printToPdf(actualParams, frame);
            case "Network.getResponseBody" -> CompletableFuture
                    .completedFuture(CdpPayload.of(Map.of("body", Normal.EMPTY, "base64Encoded", false)));
            case "Network.setBypassServiceWorker" -> CompletableFuture.completedFuture(CdpPayload.of(Map.of()));
            case "Input.dispatchMouseEvent", "Input.dispatchKeyEvent" -> dispatchInput(
                    actualMethod,
                    actualParams,
                    frame);
            default -> translateByDomain(actualMethod, actualParams, realm, frame);
        };
    }

    /**
     * Builds a CDP-compatible browser version response from BiDi capabilities.
     *
     * @return browser version value
     */
    private CdpPayload browserVersion() {
        CdpPayload capabilities = session.capabilities();
        String browserName = PayloadReader.text(capabilities.get("browserName"));
        String browserVersion = PayloadReader.text(capabilities.get("browserVersion"));
        String product = StringKit.isBlank(browserName) && StringKit.isBlank(browserVersion) ? "WebDriver BiDi"
                : browserName + (StringKit.isBlank(browserVersion) ? Normal.EMPTY : Symbol.SLASH + browserVersion);
        return CdpPayload.of(
                Map.of(
                        "protocolVersion",
                        "WebDriver BiDi",
                        "product",
                        product,
                        "revision",
                        Normal.EMPTY,
                        "userAgent",
                        PayloadReader.text(capabilities.get("userAgent")),
                        "jsVersion",
                        Normal.EMPTY));
    }

    /**
     * Translates Runtime.evaluate to script.evaluate.
     *
     * @param params protocol parameters
     * @param realm  realm value
     * @param frame  frame instance
     * @return completion future
     */
    private CompletableFuture<CdpPayload> evaluate(Map<String, Object> params, BidiRealm realm, BidiFrame frame) {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("expression", stringParam(params, "expression"));
        command.put("awaitPromise", booleanParam(params, "awaitPromise", true));
        command.put("target", target(realm, frame));
        command.put("resultOwnership", booleanParam(params, "returnByValue", false) ? "none" : "root");
        return session.send("script.evaluate", command)
                .thenApply(result -> CdpPayload.of(Map.of("result", result.get("result"))));
    }

    /**
     * Translates Runtime.callFunctionOn to script.callFunction.
     *
     * @param params protocol parameters
     * @param realm  realm value
     * @param frame  frame instance
     * @return completion future
     */
    private CompletableFuture<CdpPayload> callFunction(Map<String, Object> params, BidiRealm realm, BidiFrame frame) {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("functionDeclaration", stringParam(params, "functionDeclaration"));
        command.put("awaitPromise", booleanParam(params, "awaitPromise", true));
        command.put("target", target(realm, frame));
        command.put("resultOwnership", booleanParam(params, "returnByValue", false) ? "none" : "root");
        command.put("arguments", remoteArguments(params.get("arguments")));
        return session.send("script.callFunction", command)
                .thenApply(result -> CdpPayload.of(Map.of("result", result.get("result"))));
    }

    /**
     * Translates Runtime.releaseObject to script.disown.
     *
     * @param params protocol parameters
     * @param realm  realm value
     * @param frame  frame instance
     * @return completion future
     */
    private CompletableFuture<CdpPayload> releaseObject(Map<String, Object> params, BidiRealm realm, BidiFrame frame) {
        String objectId = stringParam(params, "objectId");
        if (StringKit.isBlank(objectId)) {
            return CompletableFuture.completedFuture(CdpPayload.of(Map.of()));
        }
        return session.send("script.disown", Map.of("handles", List.of(objectId), "target", target(realm, frame)))
                .thenApply(result -> CdpPayload.of(Map.of()));
    }

    /**
     * Builds a CDP-compatible DOM.resolveNode response.
     *
     * @param params protocol parameters
     * @return completion future
     */
    private CompletableFuture<CdpPayload> resolveNode(Map<String, Object> params) {
        String objectId = stringParam(params, "objectId");
        if (StringKit.isBlank(objectId)) {
            objectId = "backendNode:" + stringParam(params, "backendNodeId");
        }
        return CompletableFuture.completedFuture(
                CdpPayload.of(Map.of("object", Map.of("type", "object", "subtype", "node", "objectId", objectId))));
    }

    /**
     * Translates Page.captureScreenshot to browsingContext.captureScreenshot.
     *
     * @param params protocol parameters
     * @param frame  frame instance
     * @return completion future
     */
    private CompletableFuture<CdpPayload> captureScreenshot(Map<String, Object> params, BidiFrame frame) {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("context", context(frame));
        if (params.containsKey("clip")) {
            command.put("clip", params.get("clip"));
        }
        return session.send("browsingContext.captureScreenshot", command);
    }

    /**
     * Translates Page.printToPDF to browsingContext.print.
     *
     * @param params protocol parameters
     * @param frame  frame instance
     * @return completion future
     */
    private CompletableFuture<CdpPayload> printToPdf(Map<String, Object> params, BidiFrame frame) {
        Map<String, Object> command = new LinkedHashMap<>(params);
        command.put("context", context(frame));
        return session.send("browsingContext.print", command);
    }

    /**
     * Translates basic CDP input dispatch commands to BiDi actions.
     *
     * @param method protocol method
     * @param params protocol parameters
     * @param frame  frame instance
     * @return completion future
     */
    private CompletableFuture<CdpPayload> dispatchInput(String method, Map<String, Object> params, BidiFrame frame) {
        Map<String, Object> action = new LinkedHashMap<>();
        action.put("type", method.endsWith("MouseEvent") ? "pointer" : "key");
        action.put("id", method.endsWith("MouseEvent") ? "lancia-mouse" : "lancia-keyboard");
        action.put("actions", List.of(new LinkedHashMap<>(params)));
        return session.send("input.performActions", Map.of("context", context(frame), "actions", List.of(action)))
                .thenApply(result -> CdpPayload.of(Map.of()));
    }

    /**
     * Translates supported CDP command families by domain.
     *
     * @param method protocol method
     * @param params protocol parameters
     * @param realm  realm value
     * @param frame  frame instance
     * @return completion future
     */
    private CompletableFuture<CdpPayload> translateByDomain(
            String method,
            Map<String, Object> params,
            BidiRealm realm,
            BidiFrame frame) {
        if (method.startsWith("Emulation.")) {
            if ("Emulation.setDeviceMetricsOverride".equals(method)) {
                return session.send(
                        "browsingContext.setViewport",
                        Map.of(
                                "context",
                                context(frame),
                                "viewport",
                                Map.of("width", intParam(params, "width", 0), "height", intParam(params, "height", 0))))
                        .thenApply(result -> CdpPayload.of(Map.of()));
            }
            return CompletableFuture.completedFuture(CdpPayload.of(Map.of()));
        }
        if (method.startsWith("Runtime.")) {
            return CompletableFuture.completedFuture(CdpPayload.of(Map.of()));
        }
        if (method.startsWith("HeapProfiler.") || method.startsWith("Extensions.")) {
            return CompletableFuture.completedFuture(CdpPayload.of(Map.of()));
        }
        CompletableFuture<CdpPayload> rejected = new CompletableFuture<>();
        rejected.completeExceptionally(new InternalException("Invalid CDP bridge command: " + method));
        return rejected;
    }

    /**
     * Builds a BiDi script target from realm or frame context.
     *
     * @param realm realm value
     * @param frame frame instance
     * @return mapped values
     */
    private Map<String, Object> target(BidiRealm realm, BidiFrame frame) {
        if (realm != null) {
            return Map.of("realm", realm.id());
        }
        if (StringKit.isNotBlank(realmId)) {
            return Map.of("realm", realmId);
        }
        return Map.of("context", context(frame));
    }

    /**
     * Resolves the browsing context used by context-scoped BiDi commands.
     *
     * @param frame frame instance
     * @return context value
     */
    private String context(BidiFrame frame) {
        if (frame != null) {
            return frame.id();
        }
        if (StringKit.isNotBlank(contextId)) {
            return contextId;
        }
        return Normal.DEFAULT;
    }

    /**
     * Copies CDP call arguments into BiDi remote arguments.
     *
     * @param value to use
     * @return values
     */
    private List<Object> remoteArguments(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<Object> result = new ArrayList<>();
        for (Object argument : iterable) {
            result.add(argument);
        }
        return result;
    }

    /**
     * Reads a string parameter.
     *
     * @param params protocol parameters
     * @param name   name to use
     * @return string param value
     */
    private String stringParam(Map<String, Object> params, String name) {
        Object value = params.get(name);
        return value == null ? Normal.EMPTY : String.valueOf(value);
    }

    /**
     * Reads a boolean parameter with a default.
     *
     * @param params       protocol parameters
     * @param name         name to use
     * @param defaultValue default value
     * @return {@code true} when the condition matches
     */
    private boolean booleanParam(Map<String, Object> params, String name, boolean defaultValue) {
        Object value = params.get(name);
        return value instanceof Boolean bool ? bool : defaultValue;
    }

    /**
     * Reads an integer parameter with a default.
     *
     * @param params       protocol parameters
     * @param name         name to use
     * @param defaultValue default value
     * @return int param value
     */
    private int intParam(Map<String, Object> params, String name, int defaultValue) {
        Object value = params.get(name);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

}
