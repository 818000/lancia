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
package org.miaixz.lancia.kernel.bidi.protocol.message;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents a BiDi browsing context info protocol message.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BidiBrowsingContextInfoMessage {

    /**
     * Current context.
     */
    private final String context;
    /**
     * Current URL.
     */
    private final String url;
    /**
     * Current user context.
     */
    private final String userContext;

    /**
     * Creates a bidi browsing context info message.
     *
     * @param context     browser context
     * @param url         target URL
     * @param userContext user context
     */
    public BidiBrowsingContextInfoMessage(String context, String url, String userContext) {
        this.context = context;
        this.url = StringKit.isBlank(url) ? Builder.ABOUT_BLANK : url;
        this.userContext = StringKit.isBlank(userContext) ? Normal.DEFAULT : userContext;
    }

    /**
     * Returns the from.
     *
     * @param payload protocol payload
     * @return from value
     */
    public static BidiBrowsingContextInfoMessage from(CdpPayload payload) {
        return new BidiBrowsingContextInfoMessage(PayloadReader.text(payload.get("context")),
                PayloadReader.text(payload.get("url")), PayloadReader.text(payload.get("userContext")));
    }

    /**
     * Returns the context.
     *
     * @return context value
     */
    public String context() {
        return context;
    }

    /**
     * Returns the URL.
     *
     * @return URL value
     */
    public String url() {
        return url;
    }

    /**
     * Returns the user context.
     *
     * @return user context value
     */
    public String userContext() {
        return userContext;
    }

}
