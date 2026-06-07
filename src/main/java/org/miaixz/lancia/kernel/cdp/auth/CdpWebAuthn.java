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
package org.miaixz.lancia.kernel.cdp.auth;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.miaixz.lancia.kernel.Authenticator;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;

/**
 * Controls CdpWebAuthn virtual authenticators for a page.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpWebAuthn implements Authenticator {

    /**
     * Current session.
     */
    private final CDPSession session;

    /**
     * Creates a web authn.
     *
     * @param session protocol session
     */
    public CdpWebAuthn(CDPSession session) {
        this.session = session;
    }

    /**
     * Returns the enable.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> enable() {
        return session.send("CdpWebAuthn.enable");
    }

    /**
     * Returns the disable.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> disable() {
        return session.send("CdpWebAuthn.disable");
    }

    /**
     * Adds virtual authenticator.
     *
     * @param options operation options
     * @return add virtual authenticator value
     */
    public CompletableFuture<CdpPayload> addVirtualAuthenticator(Map<String, Object> options) {
        return session.send("CdpWebAuthn.addVirtualAuthenticator", Map.of("options", options));
    }

    /**
     * Removes virtual authenticator.
     *
     * @param authenticatorId authenticator id
     * @return remove virtual authenticator value
     */
    public CompletableFuture<CdpPayload> removeVirtualAuthenticator(String authenticatorId) {
        return session.send("CdpWebAuthn.removeVirtualAuthenticator", Map.of("authenticatorId", authenticatorId));
    }

}
