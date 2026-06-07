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
package org.miaixz.lancia.options;

/**
 * Public element click options matching Puppeteer's ClickOptions name.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class ClickOptions extends MouseClickOptions {

    /**
     * Horizontal offset relative to the element border rectangle.
     */
    private Double offsetX;

    /**
     * Vertical offset relative to the element border rectangle.
     */
    private Double offsetY;

    /**
     * Creates click options.
     */
    public ClickOptions() {
        // No initialization required.
    }

    /**
     * Returns horizontal offset.
     *
     * @return horizontal offset
     */
    public Double getOffsetX() {
        return offsetX;
    }

    /**
     * Updates offset x.
     *
     * @param offsetX horizontal offset
     */
    public void setOffsetX(Double offsetX) {
        this.offsetX = offsetX;
    }

    /**
     * Returns vertical offset.
     *
     * @return vertical offset
     */
    public Double getOffsetY() {
        return offsetY;
    }

    /**
     * Updates offset y.
     *
     * @param offsetY vertical offset
     */
    public void setOffsetY(Double offsetY) {
        this.offsetY = offsetY;
    }

    /**
     * Returns whether offset is available.
     *
     * @return offset state
     */
    public boolean hasOffset() {
        return offsetX != null || offsetY != null;
    }

}
