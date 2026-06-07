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
package org.miaixz.lancia.nimble.emulation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a viewport value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class Viewport {

    /**
     * Default width.
     */
    public static final int DEFAULT_WIDTH = 800;

    /**
     * Default height.
     */
    public static final int DEFAULT_HEIGHT = 600;

    /**
     * Default device scale factor.
     */
    public static final double DEFAULT_DEVICE_SCALE_FACTOR = 1.0D;
    /**
     * Current width.
     */
    private int width = DEFAULT_WIDTH;
    /**
     * Current height.
     */
    private int height = DEFAULT_HEIGHT;
    /**
     * Current device scale factor.
     */
    private double deviceScaleFactor = DEFAULT_DEVICE_SCALE_FACTOR;
    /**
     * Whether mobile is enabled.
     */
    private boolean mobile;
    /**
     * Whether has touch is enabled.
     */
    private boolean hasTouch;
    /**
     * Whether landscape is enabled.
     */
    private boolean landscape;

    /**
     * Creates a viewport.
     */
    public Viewport() {
        // No initialization required.
    }

    /**
     * Creates a viewport.
     *
     * @param width  width
     * @param height height
     */
    public Viewport(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Creates a viewport.
     *
     * @param width             width
     * @param height            height
     * @param deviceScaleFactor device scale factor
     * @param mobile            mobile
     * @param landscape         landscape
     * @param hasTouch          has touch
     */
    public Viewport(int width, int height, double deviceScaleFactor, boolean mobile, boolean landscape,
            boolean hasTouch) {
        this.width = width;
        this.height = height;
        this.deviceScaleFactor = deviceScaleFactor;
        this.mobile = mobile;
        this.landscape = landscape;
        this.hasTouch = hasTouch;
    }

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("width", width);
        result.put("height", height);
        result.put("deviceScaleFactor", deviceScaleFactor);
        result.put("mobile", mobile);
        result.put(
                "screenOrientation",
                Map.of("type", landscape ? "landscapePrimary" : "portraitPrimary", "angle", landscape ? 90 : 0));
        return result;
    }

    /**
     * Converts this value to puppeteer map.
     *
     * @return mapped values
     */
    public Map<String, Object> toPuppeteerMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("width", width);
        result.put("height", height);
        result.put("deviceScaleFactor", deviceScaleFactor);
        result.put("isMobile", mobile);
        result.put("isLandscape", landscape);
        result.put("hasTouch", hasTouch);
        return result;
    }

    /**
     * Returns the copy.
     *
     * @return copy value
     */
    public Viewport copy() {
        return new Viewport(width, height, deviceScaleFactor, mobile, landscape, hasTouch);
    }

    /**
     * Returns the width.
     *
     * @return width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Updates width.
     *
     * @param width width value
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Returns the height.
     *
     * @return height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Updates height.
     *
     * @param height height value
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Returns the device scale factor.
     *
     * @return device scale factor
     */
    public double getDeviceScaleFactor() {
        return deviceScaleFactor;
    }

    /**
     * Updates device scale factor.
     *
     * @param deviceScaleFactor device scale factor value
     */
    public void setDeviceScaleFactor(double deviceScaleFactor) {
        this.deviceScaleFactor = deviceScaleFactor;
    }

    /**
     * Returns whether mobile is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isMobile() {
        return mobile;
    }

    /**
     * Returns the is mobile.
     *
     * @return {@code true} when the condition matches
     */
    public boolean getIsMobile() {
        return isMobile();
    }

    /**
     * Updates mobile.
     *
     * @param mobile mobile value
     */
    public void setMobile(boolean mobile) {
        this.mobile = mobile;
    }

    /**
     * Updates is mobile.
     *
     * @param mobile mobile value
     */
    public void setIsMobile(boolean mobile) {
        setMobile(mobile);
    }

    /**
     * Returns whether has touch is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isHasTouch() {
        return hasTouch;
    }

    /**
     * Returns whether touch is available.
     *
     * @return {@code true} when the condition matches
     */
    public boolean hasTouch() {
        return hasTouch;
    }

    /**
     * Returns the has touch.
     *
     * @return {@code true} when the condition matches
     */
    public boolean getHasTouch() {
        return hasTouch;
    }

    /**
     * Updates has touch.
     *
     * @param hasTouch has touch value
     */
    public void setHasTouch(boolean hasTouch) {
        this.hasTouch = hasTouch;
    }

    /**
     * Returns whether landscape is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isLandscape() {
        return landscape;
    }

    /**
     * Returns the is landscape.
     *
     * @return {@code true} when the condition matches
     */
    public boolean getIsLandscape() {
        return isLandscape();
    }

    /**
     * Updates landscape.
     *
     * @param landscape landscape value
     */
    public void setLandscape(boolean landscape) {
        this.landscape = landscape;
    }

    /**
     * Updates is landscape.
     *
     * @param landscape landscape value
     */
    public void setIsLandscape(boolean landscape) {
        setLandscape(landscape);
    }

    /**
     * Returns the equals.
     *
     * @param object object to inspect
     * @return {@code true} when the condition matches
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Viewport viewport)) {
            return false;
        }
        return width == viewport.width && height == viewport.height
                && Double.compare(deviceScaleFactor, viewport.deviceScaleFactor) == 0 && mobile == viewport.mobile
                && hasTouch == viewport.hasTouch && landscape == viewport.landscape;
    }

    /**
     * Returns whether h code is available.
     *
     * @return {@code true} when the condition matches
     */
    @Override
    public int hashCode() {
        return Objects.hash(width, height, deviceScaleFactor, mobile, hasTouch, landscape);
    }

}
