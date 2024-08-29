/*
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
 ~                                                                               ~
 ~ The MIT License (MIT)                                                         ~
 ~                                                                               ~
 ~ Copyright (c) 2015-2024 miaixz.org and other contributors.                    ~
 ~                                                                               ~
 ~ Permission is hereby granted, free of charge, to any person obtaining a copy  ~
 ~ of this software and associated documentation files (the "Software"), to deal ~
 ~ in the Software without restriction, including without limitation the rights  ~
 ~ to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     ~
 ~ copies of the Software, and to permit persons to whom the Software is         ~
 ~ furnished to do so, subject to the following conditions:                      ~
 ~                                                                               ~
 ~ The above copyright notice and this permission notice shall be included in    ~
 ~ all copies or substantial portions of the Software.                           ~
 ~                                                                               ~
 ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    ~
 ~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      ~
 ~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   ~
 ~ AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        ~
 ~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, ~
 ~ OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     ~
 ~ THE SOFTWARE.                                                                 ~
 ~                                                                               ~
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
*/
package org.miaixz.lancia.nimble.network;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Security details about a request.
 * @author Kimi Liu
 * @since Java 17+
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityDetailsPayload {

    /**
     * Protocol name (e.g. "TLS 1.2" or "QUIC").
     */
    private String protocol;
    /**
     * Key Exchange used by the connection, or the empty string if not applicable.
     */
    private String keyExchange;
    /**
     * (EC)DH group used by the connection, if applicable.
     */
    private String keyExchangeGroup;
    /**
     * Cipher name.
     */
    private String cipher;
    /**
     * TLS MAC. Note that AEAD ciphers do not have separate MACs.
     */
    private String mac;
    /**
     * Certificate ID value.
     */
    private int certificateId;
    /**
     * Certificate subject name.
     */
    private String subjectName;
    /**
     * Subject Alternative Name (SAN) DNS names and IP addresses.
     */
    private List<String> sanList;
    /**
     * Name of the issuing CA.
     */
    private String issuer;
    /**
     * Certificate valid from date.
     */
    private Double validFrom;
    /**
     * Certificate valid to (expiration) date
     */
    private Double validTo;
    /**
     * List of signed certificate timestamps (SCTs).
     */
    private List<SignedCertificateTimestamp> signedCertificateTimestampList;
    /**
     * Whether the request complied with Certificate Transparency policy
     */
    private String certificateTransparencyCompliance;

}
