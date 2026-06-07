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

import java.util.LinkedHashMap;
import java.util.Map;

import org.miaixz.lancia.Builder;
import org.miaixz.lancia.nimble.browser.PageCreateType;
import org.miaixz.lancia.nimble.browser.WindowBounds;

/**
 * Defines options for create page operations.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class CreatePageOptions {

    /**
     * Current type.
     */
    private PageCreateType type = PageCreateType.TAB;
    /**
     * Whether background is enabled.
     */
    private Boolean background;
    /**
     * Current window bounds.
     */
    private WindowBounds windowBounds;

    /**
     * Creates create page options.
     */
    public CreatePageOptions() {
        // No initialization required.
    }

    /**
     * Converts this value to target create map.
     *
     * @return mapped values
     */
    public Map<String, Object> toTargetCreateMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("url", Builder.ABOUT_BLANK);
        if (type == PageCreateType.WINDOW) {
            result.put("newWindow", true);
        }
        if (background != null) {
            result.put("background", background);
        }
        if (windowBounds != null) {
            result.putAll(windowBounds.toCreateTargetMap());
        }
        return result;
    }

    /**
     * Returns the type.
     *
     * @return type
     */
    public PageCreateType getType() {
        return type;
    }

    /**
     * Updates type.
     *
     * @param type type to use
     */
    public void setType(PageCreateType type) {
        this.type = type == null ? PageCreateType.TAB : type;
    }

    /**
     * Returns the background.
     *
     * @return {@code true} when the condition matches
     */
    public Boolean getBackground() {
        return background;
    }

    /**
     * Updates background.
     *
     * @param background background value
     */
    public void setBackground(Boolean background) {
        this.background = background;
    }

    /**
     * Returns the window bounds.
     *
     * @return window bounds
     */
    public WindowBounds getWindowBounds() {
        return windowBounds;
    }

    /**
     * Updates window bounds.
     *
     * @param windowBounds window bounds value
     */
    public void setWindowBounds(WindowBounds windowBounds) {
        this.windowBounds = windowBounds;
    }

}
