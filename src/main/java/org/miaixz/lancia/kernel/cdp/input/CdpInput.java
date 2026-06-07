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
package org.miaixz.lancia.kernel.cdp.input;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.nimble.input.DragData;

/**
 * Sends input input actions.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpInput {

    /**
     * Current session.
     */
    private CDPSession session;
    /**
     * Current modifiers.
     */
    private int modifiers;

    /**
     * Creates an CdpInput instance.
     *
     * @param session session
     */
    public CdpInput(CDPSession session) {
        this.session = session;
    }

    /**
     * Updates client.
     *
     * @param session protocol session
     */
    public void updateClient(CDPSession session) {
        this.session = session;
        Logger.debug(
                false,
                "CdpInput",
                "CdpInput client updated: session={}",
                session == null ? Normal.EMPTY : session.id());
    }

    /**
     * Returns the client.
     *
     * @return client value
     */
    public CDPSession client() {
        return session;
    }

    /**
     * Updates modifiers.
     *
     * @param modifiers modifiers value
     */
    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }

    /**
     * Returns the modifiers.
     *
     * @return modifiers value
     */
    public int modifiers() {
        return modifiers;
    }

    /**
     * Returns the dispatch key event.
     *
     * @param type type to use
     * @param key  key value
     * @param text text to use
     * @return completion future
     */
    public CompletableFuture<CdpPayload> dispatchKeyEvent(String type, String key, String text) {
        return dispatchKeyEvent(type, key, text, 0, Normal.EMPTY, 0, false, List.of());
    }

    /**
     * Returns the dispatch key event.
     *
     * @param type                  type to use
     * @param key                   key value
     * @param text                  text to use
     * @param windowsVirtualKeyCode windows virtual key code value
     * @param code                  code value
     * @param location              location value
     * @param autoRepeat            auto repeat value
     * @param commands              commands to send
     * @return completion future
     */
    public CompletableFuture<CdpPayload> dispatchKeyEvent(
            String type,
            String key,
            String text,
            int windowsVirtualKeyCode,
            String code,
            int location,
            boolean autoRepeat,
            List<String> commands) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", type);
        params.put("key", key);
        params.put("modifiers", modifiers);
        params.put("windowsVirtualKeyCode", windowsVirtualKeyCode);
        params.put("code", code);
        params.put("autoRepeat", autoRepeat);
        params.put("location", location);
        params.put("isKeypad", location == 3);
        if (text != null && !text.isEmpty()) {
            params.put("text", text);
            params.put("unmodifiedText", text);
        }
        if (commands != null && !commands.isEmpty()) {
            params.put("commands", commands);
        }
        return send("CdpInput.dispatchKeyEvent", params);
    }

    /**
     * Returns the insert text.
     *
     * @param text text to use
     * @return completion future
     */
    public CompletableFuture<CdpPayload> insertText(String text) {
        return send("CdpInput.insertText", Map.of("text", text == null ? Normal.EMPTY : text));
    }

    /**
     * Returns the dispatch mouse event.
     *
     * @param type    type to use
     * @param x       x value
     * @param y       y value
     * @param button  button value
     * @param buttons buttons value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> dispatchMouseEvent(
            String type,
            double x,
            double y,
            String button,
            int buttons) {
        return dispatchMouseEvent(type, x, y, button, buttons, 0, 0, 1, "mouse");
    }

    /**
     * Returns the dispatch mouse event.
     *
     * @param type        type to use
     * @param x           x value
     * @param y           y value
     * @param button      button value
     * @param buttons     buttons value
     * @param deltaX      delta x value
     * @param deltaY      delta y value
     * @param clickCount  click count value
     * @param pointerType pointer type value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> dispatchMouseEvent(
            String type,
            double x,
            double y,
            String button,
            int buttons,
            double deltaX,
            double deltaY,
            int clickCount,
            String pointerType) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", type);
        params.put("x", x);
        params.put("y", y);
        params.put("button", button);
        params.put("buttons", buttons);
        params.put("modifiers", modifiers);
        params.put("clickCount", clickCount);
        if ("mouseWheel".equals(type)) {
            params.put("deltaX", deltaX);
            params.put("deltaY", deltaY);
        }
        if (pointerType != null) {
            params.put("pointerType", pointerType);
        }
        return send("CdpInput.dispatchMouseEvent", params);
    }

    /**
     * Returns the dispatch wheel event.
     *
     * @param x      x value
     * @param y      y value
     * @param deltaX delta x value
     * @param deltaY delta y value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> dispatchWheelEvent(double x, double y, double deltaX, double deltaY) {
        return dispatchMouseEvent("mouseWheel", x, y, "none", 0, deltaX, deltaY, 0, "mouse");
    }

    /**
     * Returns the dispatch touch event.
     *
     * @param type        type to use
     * @param touchPoints touch points value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> dispatchTouchEvent(String type, List<Map<String, Object>> touchPoints) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", type);
        params.put("touchPoints", touchPoints == null ? List.of() : touchPoints);
        params.put("modifiers", modifiers);
        return send("CdpInput.dispatchTouchEvent", params);
    }

    /**
     * Returns the dispatch drag event.
     *
     * @param type type to use
     * @param x    x value
     * @param y    y value
     * @param data data to use
     * @return completion future
     */
    public CompletableFuture<CdpPayload> dispatchDragEvent(String type, double x, double y, DragData data) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", type);
        params.put("x", x);
        params.put("y", y);
        params.put("modifiers", modifiers);
        params.put("data", data == null ? Map.of("items", List.of(), "dragOperationsMask", 1) : data.toMap());
        return send("CdpInput.dispatchDragEvent", params);
    }

    /**
     * Sends a protocol command.
     *
     * @param method protocol method
     * @param params protocol parameters
     * @return completion future
     */
    private CompletableFuture<CdpPayload> send(String method, Map<String, Object> params) {
        if (session == null) {
            Logger.trace(false, "CdpInput", "CdpInput command skipped without session: method={}", method);
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        Logger.trace(
                true,
                "CdpInput",
                "CdpInput command requested: method={}, params={}",
                method,
                params == null ? Normal._0 : params.size());
        CompletableFuture<CdpPayload> result = session.send(method, params);
        result.whenComplete((payload, error) -> {
            if (error == null) {
                Logger.trace(false, "CdpInput", "CdpInput command completed: method={}", method);
            } else {
                Logger.warn(false, "CdpInput", error, "CdpInput command failed: method={}", method);
            }
        });
        return result;
    }

}
