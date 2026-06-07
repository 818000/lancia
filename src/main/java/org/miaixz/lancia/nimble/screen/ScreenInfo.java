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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.nimble.emulation.ScreenOrientation;

/**
 * Represents a screen info value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class ScreenInfo {

    /**
     * Current left.
     */
    private final int left;
    /**
     * Current top.
     */
    private final int top;
    /**
     * Current width.
     */
    private final int width;
    /**
     * Current height.
     */
    private final int height;
    /**
     * Current avail left.
     */
    private final int availLeft;
    /**
     * Current avail top.
     */
    private final int availTop;
    /**
     * Current avail width.
     */
    private final int availWidth;
    /**
     * Current avail height.
     */
    private final int availHeight;
    /**
     * Current device pixel ratio.
     */
    private final double devicePixelRatio;
    /**
     * Current color depth.
     */
    private final int colorDepth;
    /**
     * Current orientation.
     */
    private final ScreenOrientation orientation;
    /**
     * Whether extended is enabled.
     */
    private final boolean extended;
    /**
     * Whether internal is enabled.
     */
    private final boolean internal;
    /**
     * Whether primary is enabled.
     */
    private final boolean primary;
    /**
     * Current label.
     */
    private final String label;
    /**
     * Current identifier.
     */
    private final String id;

    /**
     * Creates a screen info.
     *
     * @param left             left
     * @param top              top
     * @param width            width
     * @param height           height
     * @param availLeft        avail left
     * @param availTop         avail top
     * @param availWidth       avail width
     * @param availHeight      avail height
     * @param devicePixelRatio device pixel ratio
     * @param colorDepth       color depth
     * @param orientation      orientation
     * @param extended         extended
     * @param internal         internal
     * @param primary          primary
     * @param label            label
     * @param id               identifier
     */
    public ScreenInfo(int left, int top, int width, int height, int availLeft, int availTop, int availWidth,
            int availHeight, double devicePixelRatio, int colorDepth, ScreenOrientation orientation, boolean extended,
            boolean internal, boolean primary, String label, String id) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.availLeft = availLeft;
        this.availTop = availTop;
        this.availWidth = availWidth;
        this.availHeight = availHeight;
        this.devicePixelRatio = devicePixelRatio;
        this.colorDepth = colorDepth;
        this.orientation = orientation == null ? new ScreenOrientation(0, "landscapePrimary") : orientation;
        this.extended = extended;
        this.internal = internal;
        this.primary = primary;
        this.label = label == null ? Normal.EMPTY : label;
        this.id = StringKit.isBlank(id) ? "screen" : id;
    }

    /**
     * Returns the from.
     *
     * @param params  protocol parameters
     * @param id      identifier
     * @param primary primary value
     * @return from value
     */
    public static ScreenInfo from(AddScreenParams params, String id, boolean primary) {
        WorkAreaInsets insets = params.getWorkAreaInsets() == null ? new WorkAreaInsets() : params.getWorkAreaInsets();
        int availLeft = params.getLeft() + insets.getLeft();
        int availTop = params.getTop() + insets.getTop();
        int availWidth = Math.max(0, params.getWidth() - insets.getLeft() - insets.getRight());
        int availHeight = Math.max(0, params.getHeight() - insets.getTop() - insets.getBottom());
        return new ScreenInfo(params.getLeft(), params.getTop(), params.getWidth(), params.getHeight(), availLeft,
                availTop, availWidth, availHeight, params.getDevicePixelRatio(), params.getColorDepth(),
                new ScreenOrientation(params.getRotation(), orientationType(params.getRotation())), !primary,
                params.isInternal(), primary, params.getLabel(), id);
    }

    /**
     * Registers a local screen.
     *
     * @param params       add screen params
     * @param screenIndex  screen index
     * @param localScreens local screens
     * @return register local value
     */
    public static ScreenInfo registerLocal(
            AddScreenParams params,
            AtomicInteger screenIndex,
            List<ScreenInfo> localScreens) {
        ScreenInfo screen = ScreenInfo.from(params, "screen-" + screenIndex.incrementAndGet(), localScreens.isEmpty());
        registerLocal(screen, localScreens);
        return screen;
    }

    /**
     * Registers or replaces a local screen.
     *
     * @param screen       screen
     * @param localScreens local screens
     */
    public static void registerLocal(ScreenInfo screen, List<ScreenInfo> localScreens) {
        unregisterLocal(screen.id(), localScreens);
        localScreens.add(screen);
    }

    /**
     * Unregisters a local screen.
     *
     * @param screenId     screen id
     * @param localScreens local screens
     */
    public static void unregisterLocal(String screenId, List<ScreenInfo> localScreens) {
        localScreens.removeIf(screen -> screen.id().equals(screenId));
    }

    /**
     * Returns the left.
     *
     * @return left value
     */
    public int left() {
        return left;
    }

    /**
     * Converts this value to p.
     *
     * @return p
     */
    public int top() {
        return top;
    }

    /**
     * Returns the width.
     *
     * @return width value
     */
    public int width() {
        return width;
    }

    /**
     * Returns the height.
     *
     * @return height value
     */
    public int height() {
        return height;
    }

    /**
     * Returns the avail left.
     *
     * @return avail left value
     */
    public int availLeft() {
        return availLeft;
    }

    /**
     * Returns the avail top.
     *
     * @return avail top value
     */
    public int availTop() {
        return availTop;
    }

    /**
     * Returns the avail width.
     *
     * @return avail width value
     */
    public int availWidth() {
        return availWidth;
    }

    /**
     * Returns the avail height.
     *
     * @return avail height value
     */
    public int availHeight() {
        return availHeight;
    }

    /**
     * Returns the device pixel ratio.
     *
     * @return device pixel ratio value
     */
    public double devicePixelRatio() {
        return devicePixelRatio;
    }

    /**
     * Returns the color depth.
     *
     * @return color depth value
     */
    public int colorDepth() {
        return colorDepth;
    }

    /**
     * Returns the orientation.
     *
     * @return orientation value
     */
    public ScreenOrientation orientation() {
        return orientation;
    }

    /**
     * Returns whether the screen is extended.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isExtended() {
        return extended;
    }

    /**
     * Returns whether the screen is internal.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isInternal() {
        return internal;
    }

    /**
     * Returns whether the screen is primary.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isPrimary() {
        return primary;
    }

    /**
     * Returns the label.
     *
     * @return label value
     */
    public String label() {
        return label;
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        return id;
    }

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("left", left);
        value.put("top", top);
        value.put("width", width);
        value.put("height", height);
        value.put("availLeft", availLeft);
        value.put("availTop", availTop);
        value.put("availWidth", availWidth);
        value.put("availHeight", availHeight);
        value.put("devicePixelRatio", devicePixelRatio);
        value.put("colorDepth", colorDepth);
        value.put("orientation", orientation.toMap());
        value.put("isExtended", extended);
        value.put("isInternal", internal);
        value.put("isPrimary", primary);
        value.put("label", label);
        value.put("id", id);
        return value;
    }

    /**
     * Returns the orientation type.
     *
     * @param rotation rotation value
     * @return orientation type value
     */
    private static String orientationType(int rotation) {
        int normalized = Math.floorMod(rotation, 360);
        return normalized == 90 || normalized == 270 ? "portraitPrimary" : "landscapePrimary";
    }

}
