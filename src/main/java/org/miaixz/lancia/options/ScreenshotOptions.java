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

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.miaixz.bus.core.io.file.FileType;

/**
 * Defines options for screenshot operations.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class ScreenshotOptions {

    /**
     * Creates screenshot options.
     */
    public ScreenshotOptions() {
        // No initialization required.
    }

    /**
     * Current type.
     */
    private String type = FileType.TYPE_PNG;
    /**
     * Current quality.
     */
    private Integer quality;
    /**
     * Whether full page is enabled.
     */
    private boolean fullPage;

    /**
     * Whether from surface is enabled.
     */
    private Boolean fromSurface;
    /**
     * Current encoding.
     */
    private String encoding = "binary";

    /**
     * Whether capture beyond viewport is enabled.
     */
    private Boolean captureBeyondViewport;

    /**
     * Whether scroll into view is enabled.
     */
    private boolean scrollIntoView = true;
    /**
     * Whether omit background is enabled.
     */
    private boolean omitBackground;
    /**
     * Whether optimize for speed is enabled.
     */
    private boolean optimizeForSpeed;
    /**
     * Mapped clip values.
     */
    private Map<String, Object> clip;
    /**
     * Current path.
     */
    private Path path;

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("format", type);
        if (quality != null) {
            result.put("quality", quality);
        }
        if (clip != null) {
            result.put("clip", clip);
        }
        if (fromSurface != null) {
            result.put("fromSurface", fromSurface);
        }
        result.put("captureBeyondViewport", captureBeyondViewport == null ? fullPage : captureBeyondViewport);
        result.put("optimizeForSpeed", optimizeForSpeed);
        return result;
    }

    /**
     * Returns the type.
     *
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * Updates type.
     *
     * @param type type to use
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the quality.
     *
     * @return quality
     */
    public Integer getQuality() {
        return quality;
    }

    /**
     * Updates quality.
     *
     * @param quality quality value
     */
    public void setQuality(Integer quality) {
        this.quality = quality;
    }

    /**
     * Returns whether full page is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isFullPage() {
        return fullPage;
    }

    /**
     * Updates full page.
     *
     * @param fullPage full page value
     */
    public void setFullPage(boolean fullPage) {
        this.fullPage = fullPage;
    }

    /**
     * Returns the from surface.
     *
     * @return {@code true} when the condition matches
     */
    public Boolean getFromSurface() {
        return fromSurface;
    }

    /**
     * Updates from surface.
     *
     * @param fromSurface from surface value
     */
    public void setFromSurface(Boolean fromSurface) {
        this.fromSurface = fromSurface;
    }

    /**
     * Returns the encoding.
     *
     * @return encoding
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Updates encoding.
     *
     * @param encoding encoding value
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Returns the capture beyond viewport.
     *
     * @return {@code true} when the condition matches
     */
    public Boolean getCaptureBeyondViewport() {
        return captureBeyondViewport;
    }

    /**
     * Updates capture beyond viewport.
     *
     * @param captureBeyondViewport capture beyond viewport value
     */
    public void setCaptureBeyondViewport(Boolean captureBeyondViewport) {
        this.captureBeyondViewport = captureBeyondViewport;
    }

    /**
     * Returns whether scroll into view is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isScrollIntoView() {
        return scrollIntoView;
    }

    /**
     * Updates scroll into view.
     *
     * @param scrollIntoView scroll into view value
     */
    public void setScrollIntoView(boolean scrollIntoView) {
        this.scrollIntoView = scrollIntoView;
    }

    /**
     * Returns whether omit background is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isOmitBackground() {
        return omitBackground;
    }

    /**
     * Updates omit background.
     *
     * @param omitBackground omit background value
     */
    public void setOmitBackground(boolean omitBackground) {
        this.omitBackground = omitBackground;
    }

    /**
     * Returns whether optimize for speed is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isOptimizeForSpeed() {
        return optimizeForSpeed;
    }

    /**
     * Updates optimize for speed.
     *
     * @param optimizeForSpeed optimize for speed value
     */
    public void setOptimizeForSpeed(boolean optimizeForSpeed) {
        this.optimizeForSpeed = optimizeForSpeed;
    }

    /**
     * Returns the clip.
     *
     * @return mapped values
     */
    public Map<String, Object> getClip() {
        return clip;
    }

    /**
     * Updates clip.
     *
     * @param clip clip value
     */
    public void setClip(Map<String, Object> clip) {
        this.clip = clip;
    }

    /**
     * Returns the path.
     *
     * @return path
     */
    public Path getPath() {
        return path;
    }

    /**
     * Updates path.
     *
     * @param path file path
     */
    public void setPath(Path path) {
        this.path = path;
    }

}
