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
package org.miaixz.lancia.shared.payload;

import org.miaixz.lancia.Payload;
import org.miaixz.lancia.nimble.emulation.ScreenOrientation;
import org.miaixz.lancia.nimble.screen.ScreenInfo;
import org.miaixz.lancia.nimble.screen.WorkAreaInsets;

/**
 * Parses screen information from protocol neutral payloads.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class PayloadScreenInfo {

    /**
     * Creates a payload screen info.
     */
    private PayloadScreenInfo() {
        // No initialization required.
    }

    /**
     * Parses screen information from a payload.
     *
     * @param payload payload
     * @return converted value
     */
    public static ScreenInfo from(Payload payload) {
        Payload actualPayload = payload == null ? NullPayload.INSTANCE : payload;
        return new ScreenInfo(PayloadReader.number(actualPayload.get("left")),
                PayloadReader.number(actualPayload.get("top")), PayloadReader.number(actualPayload.get("width")),
                PayloadReader.number(actualPayload.get("height")), PayloadReader.number(actualPayload.get("availLeft")),
                PayloadReader.number(actualPayload.get("availTop")),
                PayloadReader.number(actualPayload.get("availWidth")),
                PayloadReader.number(actualPayload.get("availHeight")),
                PayloadReader.decimal(actualPayload.get("devicePixelRatio")),
                PayloadReader.number(actualPayload.get("colorDepth")), orientation(actualPayload.get("orientation")),
                PayloadReader.bool(actualPayload.get("isExtended")),
                PayloadReader.bool(actualPayload.get("isInternal")), PayloadReader.bool(actualPayload.get("isPrimary")),
                PayloadReader.text(actualPayload.get("label")), PayloadReader.text(actualPayload.get("id")));
    }

    /**
     * Parses work area insets from a payload.
     *
     * @param payload payload
     * @return work area insets value
     */
    public static WorkAreaInsets workAreaInsets(Payload payload) {
        if (payload == null || payload.isNull()) {
            return new WorkAreaInsets();
        }
        return new WorkAreaInsets(PayloadReader.number(payload.get("top")), PayloadReader.number(payload.get("left")),
                PayloadReader.number(payload.get("bottom")), PayloadReader.number(payload.get("right")));
    }

    /**
     * Parses screen orientation from a payload.
     *
     * @param payload payload
     * @return orientation value
     */
    private static ScreenOrientation orientation(Payload payload) {
        if (payload == null || payload.isNull()) {
            return new ScreenOrientation(0, "landscapePrimary");
        }
        return new ScreenOrientation(PayloadReader.number(payload.get("angle")),
                PayloadReader.text(payload.get("type")));
    }

}
