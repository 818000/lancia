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

import java.util.Map;

/**
 * Represents a geolocation value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class Geolocation {

    /**
     * Current longitude.
     */
    private final double longitude;
    /**
     * Current latitude.
     */
    private final double latitude;
    /**
     * Current accuracy.
     */
    private final double accuracy;

    /**
     * Creates a geolocation.
     *
     * @param longitude longitude
     * @param latitude  latitude
     * @param accuracy  accuracy
     */
    public Geolocation(double longitude, double latitude, double accuracy) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.accuracy = accuracy;
    }

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        return Map.of("longitude", longitude, "latitude", latitude, "accuracy", accuracy);
    }

    /**
     * Returns the longitude.
     *
     * @return longitude value
     */
    public double longitude() {
        return longitude;
    }

    /**
     * Returns the latitude.
     *
     * @return latitude value
     */
    public double latitude() {
        return latitude;
    }

    /**
     * Returns the accuracy.
     *
     * @return accuracy value
     */
    public double accuracy() {
        return accuracy;
    }

}
