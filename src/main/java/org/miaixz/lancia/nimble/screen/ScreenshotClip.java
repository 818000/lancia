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

/**
 * Public screenshot clip matching Puppeteer's ScreenshotClip name.
 */
@Getter
@Setter
/**
 * Represents a screenshot clip value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class ScreenshotClip {

    /**
     * Clip x coordinate.
     */
    private double x;

    /**
     * Clip y coordinate.
     */
    private double y;

    /**
     * Clip width.
     */
    private double width;

    /**
     * Clip height.
     */
    private double height;

    /**
     * Creates a screenshot clip.
     */
    public ScreenshotClip() {
        // No initialization required.
    }

    /**
     * Creates a screenshot clip.
     *
     * @param x      x coordinate
     * @param y      y coordinate
     * @param width  clip width
     * @param height clip height
     */
    public ScreenshotClip(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Converts this clip to a protocol map.
     *
     * @return protocol map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("x", x);
        value.put("y", y);
        value.put("width", width);
        value.put("height", height);
        return value;
    }

}
