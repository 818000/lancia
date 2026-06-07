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
package org.miaixz.lancia.kernel.cdp.runtime;

import org.miaixz.lancia.kernel.cdp.session.CDPSession;

/**
 * Provides DOM-focused evaluation helpers for a frame.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpDomWorld {

    /**
     * Current session.
     */
    private final CDPSession session;
    /**
     * Current execution context.
     */
    private final CdpExecutionContext executionContext;

    /**
     * Creates a DOM world.
     *
     * @param session protocol session
     */
    public CdpDomWorld(CDPSession session) {
        this.session = session;
        this.executionContext = new CdpExecutionContext(session);
    }

    /**
     * Returns the protocol session.
     *
     * @return protocol session
     */
    public CDPSession session() {
        return session;
    }

    /**
     * Returns the execution context.
     *
     * @return execution context value
     */
    public CdpExecutionContext executionContext() {
        return executionContext;
    }

}
