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
package org.miaixz.lancia.nimble.network;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.miaixz.bus.core.lang.Normal;

/**
 * Holds protocol-neutral response security details.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class SecurityDetails {

    /**
     * Current protocol.
     */
    private String protocol;
    /**
     * Current issuer.
     */
    private String issuer;
    /**
     * Current subject name.
     */
    private String subjectName;
    /**
     * Current valid from.
     */
    private double validFrom;
    /**
     * Current valid to.
     */
    private double validTo;
    /**
     * Registered subject alternative names values.
     */
    private List<String> subjectAlternativeNames = List.of();

    /**
     * Creates empty security details.
     */
    public SecurityDetails() {
        // No initialization required.
    }

    /**
     * Creates security details from certificate metadata.
     *
     * @param protocol                protocol name
     * @param issuer                  certificate issuer
     * @param subjectName             certificate subject
     * @param validFrom               not-before timestamp
     * @param validTo                 not-after timestamp
     * @param subjectAlternativeNames certificate subject alternative names
     */
    public SecurityDetails(String protocol, String issuer, String subjectName, double validFrom, double validTo,
            List<String> subjectAlternativeNames) {
        this.protocol = protocol == null ? Normal.EMPTY : protocol;
        this.issuer = issuer == null ? Normal.EMPTY : issuer;
        this.subjectName = subjectName == null ? Normal.EMPTY : subjectName;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.subjectAlternativeNames = subjectAlternativeNames == null ? List.of()
                : List.copyOf(subjectAlternativeNames);
    }

    /**
     * Returns the protocol.
     *
     * @return protocol value
     */
    public String protocol() {
        return protocol;
    }

    /**
     * Returns the protocol.
     *
     * @return protocol
     */
    public String getProtocol() {
        return protocol();
    }

    /**
     * Returns the certificate issuer.
     *
     * @return certificate issuer
     */
    public String issuer() {
        return issuer;
    }

    /**
     * Returns the issuer.
     *
     * @return issuer
     */
    public String getIssuer() {
        return issuer();
    }

    /**
     * Returns the subject name.
     *
     * @return subject name value
     */
    public String subjectName() {
        return subjectName;
    }

    /**
     * Returns the subject name.
     *
     * @return subject name
     */
    public String getSubjectName() {
        return subjectName();
    }

    /**
     * Returns the valid from.
     *
     * @return valid from value
     */
    public double validFrom() {
        return validFrom;
    }

    /**
     * Returns the valid from.
     *
     * @return valid from
     */
    public double getValidFrom() {
        return validFrom();
    }

    /**
     * Returns the valid to.
     *
     * @return valid to value
     */
    public double validTo() {
        return validTo;
    }

    /**
     * Returns the valid to.
     *
     * @return valid to
     */
    public double getValidTo() {
        return validTo();
    }

    /**
     * Returns the subject alternative names.
     *
     * @return values
     */
    public List<String> subjectAlternativeNames() {
        return List.copyOf(subjectAlternativeNames);
    }

    /**
     * Returns the subject alternative names.
     *
     * @return values
     */
    public List<String> getSubjectAlternativeNames() {
        return subjectAlternativeNames();
    }

    /**
     * Converts this instance to a map.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subjectName", subjectName == null ? Normal.EMPTY : subjectName);
        result.put("issuer", issuer == null ? Normal.EMPTY : issuer);
        result.put("validFrom", validFrom);
        result.put("validTo", validTo);
        result.put("protocol", protocol == null ? Normal.EMPTY : protocol);
        result.put("sanList", subjectAlternativeNames());
        return Map.copyOf(result);
    }

}
