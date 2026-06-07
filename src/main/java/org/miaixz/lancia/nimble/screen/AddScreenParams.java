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

import lombok.Getter;
import lombok.Setter;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.xyz.StringKit;

/**
 * Public screen creation parameters matching Puppeteer's AddScreenParams name.
 */
@Getter
@Setter
/**
 * Represents an add screen params value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class AddScreenParams {

    /**
     * Left coordinate.
     */
    private int left;

    /**
     * Top coordinate.
     */
    private int top;

    /**
     * Screen width.
     */
    private int width;

    /**
     * Screen height.
     */
    private int height;

    /**
     * Work area insets.
     */
    private WorkAreaInsets workAreaInsets;

    /**
     * Device pixel ratio.
     */
    private double devicePixelRatio = 1;

    /**
     * Screen rotation.
     */
    private int rotation;

    /**
     * Color depth.
     */
    private int colorDepth = 24;

    /**
     * Screen label.
     */
    private String label = Normal.EMPTY;

    /**
     * Whether this screen is internal.
     */
    private boolean internal;

    /**
     * Creates screen parameters.
     */
    public AddScreenParams() {
        // No initialization required.
    }

    /**
     * Creates screen parameters.
     *
     * @param left   left coordinate
     * @param top    top coordinate
     * @param width  screen width
     * @param height screen height
     */
    public AddScreenParams(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }

    /**
     * Converts these parameters to a protocol map.
     *
     * @return protocol map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("left", left);
        value.put("top", top);
        value.put("width", width);
        value.put("height", height);
        if (workAreaInsets != null) {
            value.put("workAreaInsets", workAreaInsets.toMap());
        }
        value.put("devicePixelRatio", devicePixelRatio);
        value.put("rotation", rotation);
        value.put("colorDepth", colorDepth);
        if (StringKit.isNotBlank(label)) {
            value.put("label", label);
        }
        value.put("isInternal", internal);
        return value;
    }

}
