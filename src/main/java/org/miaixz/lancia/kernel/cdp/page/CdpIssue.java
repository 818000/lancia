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
package org.miaixz.lancia.kernel.cdp.page;

import java.util.Map;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Issue;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents CDP issue.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpIssue extends Issue {

    /**
     * Current code.
     */
    private final String code;
    /**
     * Mapped details values.
     */
    private final Map<String, Object> details;

    /**
     * Creates a CDP issue.
     *
     * @param code    code
     * @param details exception details
     */
    public CdpIssue(String code, Map<String, Object> details) {
        this.code = StringKit.isBlank(code) ? Normal.EMPTY : code;
        this.details = Map.copyOf(details == null ? Map.of() : details);
    }

    /**
     * Returns the from.
     *
     * @param payload protocol payload
     * @return from value
     */
    public static CdpIssue from(CdpPayload payload) {
        if (payload == null || payload.isNull()) {
            return new CdpIssue(Normal.EMPTY, Map.of());
        }
        return new CdpIssue(PayloadReader.text(payload.get("code")), PayloadReader.object(payload.get("details")));
    }

    /**
     * Returns the code.
     *
     * @return code value
     */
    public String code() {
        return code;
    }

    /**
     * Returns the code.
     *
     * @return code
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the details.
     *
     * @return mapped values
     */
    public Map<String, Object> details() {
        return details;
    }

    /**
     * Returns the details.
     *
     * @return mapped values
     */
    public Map<String, Object> getDetails() {
        return details;
    }

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        return Map.of("code", code, "details", details);
    }

}
