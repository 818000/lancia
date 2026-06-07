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
package org.miaixz.lancia.kernel.cdp.network;

import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.nimble.network.Cookie;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Parses CDP cookie payloads.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class CdpCookie {

    /**
     * Creates a CDP cookie.
     */
    private CdpCookie() {
        // No initialization required.
    }

    /**
     * Parses a cookie from a CDP payload.
     *
     * @param payload payload
     * @return converted value
     */
    public static Cookie from(CdpPayload payload) {
        Cookie cookie = new Cookie();
        CdpPayload actualPayload = payload == null ? CdpPayload.NULL : payload;
        cookie.setName(PayloadReader.text(actualPayload.get("name")));
        cookie.setValue(PayloadReader.text(actualPayload.get("value")));
        cookie.setDomain(PayloadReader.text(actualPayload.get("domain")));
        cookie.setPath(PayloadReader.text(actualPayload.get("path")));
        cookie.setExpires(PayloadReader.decimal(actualPayload.get("expires")));
        cookie.setSize(PayloadReader.number(actualPayload.get("size")));
        cookie.setHttpOnly(PayloadReader.bool(actualPayload.get("httpOnly")));
        cookie.setSecure(PayloadReader.bool(actualPayload.get("secure")));
        cookie.setSession(PayloadReader.bool(actualPayload.get("session")));
        cookie.setSameSite(PayloadReader.text(actualPayload.get("sameSite")));
        cookie.setPriority(PayloadReader.text(actualPayload.get("priority")));
        cookie.setSourceScheme(PayloadReader.text(actualPayload.get("sourceScheme")));
        cookie.setPartitionKey(partitionKey(actualPayload.get("partitionKey")));
        cookie.setPartitionKeyOpaque(PayloadReader.boolObject(actualPayload.get("partitionKeyOpaque")));
        return cookie;
    }

    /**
     * Parses a partition key from a CDP payload.
     *
     * @param payload payload
     * @return partition key value
     */
    private static Object partitionKey(CdpPayload payload) {
        if (payload == null || payload.isNull()) {
            return null;
        }
        if (payload.isObject()) {
            Cookie.CookiePartitionKey key = new Cookie.CookiePartitionKey();
            key.setSourceOrigin(PayloadReader.text(payload.get("sourceOrigin")));
            key.setHasCrossSiteAncestor(PayloadReader.boolObject(payload.get("hasCrossSiteAncestor")));
            return key;
        }
        return PayloadReader.text(payload);
    }

}
