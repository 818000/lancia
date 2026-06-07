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

import java.util.List;

import org.miaixz.lancia.Payload;
import org.miaixz.lancia.nimble.network.SecurityDetails;

/**
 * Parses security details from protocol neutral payloads.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class PayloadSecurityDetails {

    /**
     * Creates a payload security details.
     */
    private PayloadSecurityDetails() {
        // No initialization required.
    }

    /**
     * Parses security details from a payload.
     *
     * @param payload payload
     * @return converted value
     */
    public static SecurityDetails from(Payload payload) {
        return payload == null || payload.isNull() ? null : fromPresent(payload);
    }

    /**
     * Parses security details from a non-null payload.
     *
     * @param payload payload
     * @return converted value
     */
    private static SecurityDetails fromPresent(Payload payload) {
        Payload sanList = payload.get("sanList");
        if (sanList.isNull()) {
            sanList = payload.get("subjectAlternativeNames");
        }
        List<String> subjectAlternativeNames = PayloadReader.elements(sanList).stream().map(PayloadReader::text)
                .toList();
        return new SecurityDetails(PayloadReader.text(payload.get("protocol")),
                PayloadReader.text(payload.get("issuer")), PayloadReader.text(payload.get("subjectName")),
                PayloadReader.decimal(payload.get("validFrom")), PayloadReader.decimal(payload.get("validTo")),
                subjectAlternativeNames);
    }

}
