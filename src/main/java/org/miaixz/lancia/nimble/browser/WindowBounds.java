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
package org.miaixz.lancia.nimble.browser;

import java.util.LinkedHashMap;
import java.util.Map;

import org.miaixz.lancia.Payload;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents a window bounds value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class WindowBounds {

    /**
     * Current left.
     */
    private Integer left;
    /**
     * Current top.
     */
    private Integer top;
    /**
     * Current width.
     */
    private Integer width;
    /**
     * Current height.
     */
    private Integer height;
    /**
     * Current window state.
     */
    private WindowState windowState;

    /**
     * Creates a window bounds.
     */
    public WindowBounds() {
        // No initialization required.
    }

    /**
     * Returns the from.
     *
     * @param payload protocol payload
     * @return from value
     */
    public static WindowBounds from(Payload payload) {
        WindowBounds bounds = new WindowBounds();
        if (payload == null || payload.isNull()) {
            return bounds;
        }
        bounds.left = PayloadReader.numberObject(payload.get("left"));
        bounds.top = PayloadReader.numberObject(payload.get("top"));
        bounds.width = PayloadReader.numberObject(payload.get("width"));
        bounds.height = PayloadReader.numberObject(payload.get("height"));
        Payload state = payload.get("windowState");
        if (!state.isNull()) {
            bounds.windowState = WindowState.from(state.asText());
        }
        return bounds;
    }

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        put(result, "left", left);
        put(result, "top", top);
        put(result, "width", width);
        put(result, "height", height);
        if (windowState != null) {
            result.put("windowState", windowState.value());
        }
        return result;
    }

    /**
     * Converts this value to create target map.
     *
     * @return mapped values
     */
    public Map<String, Object> toCreateTargetMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        put(result, "left", left);
        put(result, "top", top);
        put(result, "width", width);
        put(result, "height", height);
        return result;
    }

    /**
     * Returns the left.
     *
     * @return left
     */
    public Integer getLeft() {
        return left;
    }

    /**
     * Updates left.
     *
     * @param left left value
     */
    public void setLeft(Integer left) {
        this.left = left;
    }

    /**
     * Returns the top.
     *
     * @return top
     */
    public Integer getTop() {
        return top;
    }

    /**
     * Updates top.
     *
     * @param top top value
     */
    public void setTop(Integer top) {
        this.top = top;
    }

    /**
     * Returns the width.
     *
     * @return width
     */
    public Integer getWidth() {
        return width;
    }

    /**
     * Updates width.
     *
     * @param width width value
     */
    public void setWidth(Integer width) {
        this.width = width;
    }

    /**
     * Returns the height.
     *
     * @return height
     */
    public Integer getHeight() {
        return height;
    }

    /**
     * Updates height.
     *
     * @param height height value
     */
    public void setHeight(Integer height) {
        this.height = height;
    }

    /**
     * Returns the window state.
     *
     * @return window state
     */
    public WindowState getWindowState() {
        return windowState;
    }

    /**
     * Updates window state.
     *
     * @param windowState window state value
     */
    public void setWindowState(WindowState windowState) {
        this.windowState = windowState;
    }

    /**
     * Handles put.
     *
     * @param target target object
     * @param name   name to use
     * @param value  value to use
     */
    private static void put(Map<String, Object> target, String name, Object value) {
        if (value != null) {
            target.put(name, value);
        }
    }

}
