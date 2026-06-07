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

import java.util.Locale;

/**
 * public screencast video format.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public enum VideoFormat {

    /**
     * WebM video.
     */
    WEBM("webm"),

    /**
     * GIF animation.
     */
    GIF("gif"),

    /**
     * MP4 video.
     */
    MP4("mp4");

    /**
     * Format id.
     */
    private final String id;

    /**
     * Creates a video format.
     *
     * @param id format id
     */
    VideoFormat(String id) {
        this.id = id;
    }

    /**
     * Returns the format id.
     *
     * @return format id
     */
    public String id() {
        return id;
    }

    /**
     * Resolves a video format from text.
     *
     * @param value format value
     * @return video format
     */
    public static VideoFormat fromValue(String value) {
        String actualValue = value == null ? WEBM.id : value.toLowerCase(Locale.ROOT);
        for (VideoFormat format : values()) {
            if (format.id.equals(actualValue)) {
                return format;
            }
        }
        throw new IllegalArgumentException("Invalid video format: " + value);
    }

}
