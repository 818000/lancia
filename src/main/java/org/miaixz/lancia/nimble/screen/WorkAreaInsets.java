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
package org.miaixz.lancia.nimble.screen;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a work area insets value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class WorkAreaInsets {

    /**
     * Current top.
     */
    private int top;
    /**
     * Current left.
     */
    private int left;
    /**
     * Current bottom.
     */
    private int bottom;
    /**
     * Current right.
     */
    private int right;

    /**
     * Creates a work area insets.
     */
    public WorkAreaInsets() {
        // No initialization required.
    }

    /**
     * Creates a work area insets.
     *
     * @param top    top
     * @param left   left
     * @param bottom bottom
     * @param right  right
     */
    public WorkAreaInsets(int top, int left, int bottom, int right) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    /**
     * Returns the top.
     *
     * @return top
     */
    public int getTop() {
        return top;
    }

    /**
     * Updates top.
     *
     * @param top top value
     */
    public void setTop(int top) {
        this.top = top;
    }

    /**
     * Returns the left.
     *
     * @return left
     */
    public int getLeft() {
        return left;
    }

    /**
     * Updates left.
     *
     * @param left left value
     */
    public void setLeft(int left) {
        this.left = left;
    }

    /**
     * Returns the bottom.
     *
     * @return bottom
     */
    public int getBottom() {
        return bottom;
    }

    /**
     * Updates bottom.
     *
     * @param bottom bottom value
     */
    public void setBottom(int bottom) {
        this.bottom = bottom;
    }

    /**
     * Returns the right.
     *
     * @return right
     */
    public int getRight() {
        return right;
    }

    /**
     * Updates right.
     *
     * @param right right value
     */
    public void setRight(int right) {
        this.right = right;
    }

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("top", top);
        value.put("left", left);
        value.put("bottom", bottom);
        value.put("right", right);
        return value;
    }

}
