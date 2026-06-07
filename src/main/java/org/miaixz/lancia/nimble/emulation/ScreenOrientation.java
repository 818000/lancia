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

import org.miaixz.bus.core.xyz.StringKit;

/**
 * Represents a screen orientation value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class ScreenOrientation {

    /**
     * Current angle.
     */
    private final int angle;
    /**
     * Current type.
     */
    private final String type;

    /**
     * Creates a screen orientation.
     *
     * @param angle angle
     * @param type  type name
     */
    public ScreenOrientation(int angle, String type) {
        this.angle = angle;
        this.type = StringKit.isBlank(type) ? "landscapePrimary" : type;
    }

    /**
     * Returns the angle.
     *
     * @return angle value
     */
    public int angle() {
        return angle;
    }

    /**
     * Returns the type.
     *
     * @return type value
     */
    public String type() {
        return type;
    }

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("angle", angle);
        value.put("type", type);
        return value;
    }

}
