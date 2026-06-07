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
package org.miaixz.lancia.shared.page;

import java.util.LinkedHashMap;

import org.miaixz.lancia.options.ScreenshotOptions;

/**
 * Represents page defaults.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class PageDefaults {

    /**
     * Current default screenshot options.
     */
    private static volatile ScreenshotOptions defaultScreenshotOptions = new ScreenshotOptions();

    /**
     * Creates no PageDefaults instance.
     */
    private PageDefaults() {
        // No initialization required.
    }

    /**
     * Updates default screenshot options.
     *
     * @param options screenshot options
     */
    public static void setDefaultScreenshotOptions(ScreenshotOptions options) {
        defaultScreenshotOptions = copyScreenshotOptions(options);
    }

    /**
     * Returns copied default screenshot options.
     *
     * @return default screenshot options
     */
    public static ScreenshotOptions defaultScreenshotOptions() {
        return copyScreenshotOptions(defaultScreenshotOptions);
    }

    /**
     * Copies screenshot options.
     *
     * @param options source options
     * @return copied options
     */
    public static ScreenshotOptions copyScreenshotOptions(ScreenshotOptions options) {
        ScreenshotOptions copy = new ScreenshotOptions();
        if (options == null) {
            return copy;
        }
        copy.setType(options.getType());
        copy.setQuality(options.getQuality());
        copy.setPath(options.getPath());
        copy.setFullPage(options.isFullPage());
        copy.setFromSurface(options.getFromSurface());
        copy.setEncoding(options.getEncoding());
        copy.setCaptureBeyondViewport(options.getCaptureBeyondViewport());
        copy.setScrollIntoView(options.isScrollIntoView());
        copy.setOmitBackground(options.isOmitBackground());
        copy.setOptimizeForSpeed(options.isOptimizeForSpeed());
        if (options.getClip() != null) {
            copy.setClip(new LinkedHashMap<>(options.getClip()));
        }
        return copy;
    }

}
