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
package org.miaixz.lancia.kernel.bidi.page;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.lancia.kernel.bidi.accessor.BidiSession;
import org.miaixz.lancia.kernel.bidi.session.BidiProtocolSession;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents BiDi dialog.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiDialog {

    /**
     * Shared constant for handle user prompt.
     */
    private static final String HANDLE_USER_PROMPT = "browsingContext.handleUserPrompt";

    /**
     * BiDi session.
     */
    private final BidiSession session;
    /**
     * Current context ID.
     */
    private final String contextId;
    /**
     * Current type.
     */
    private final String type;
    /**
     * Current message.
     */
    private final String message;

    /**
     * Current default value.
     */
    private final String defaultValue;
    /**
     * Thread-safe handled state.
     */
    private final AtomicBoolean handled;

    /**
     * Returns the from.
     *
     * @param session protocol session
     * @param params  protocol parameters
     * @return from value
     */
    public static BidiDialog from(BidiSession session, CdpPayload params) {
        return new BidiDialog(session, PayloadReader.text(params.get("context")),
                PayloadReader.text(params.get("type")), PayloadReader.text(params.get("message")),
                PayloadReader.text(params.get("defaultValue")), false);
    }

    /**
     * Returns the from.
     *
     * @param session protocol session
     * @param params  protocol parameters
     * @return from value
     */
    public static BidiDialog from(BidiProtocolSession session, CdpPayload params) {
        return from(BidiSession.wrap(session), params);
    }

    /**
     * Creates a bidi dialog.
     *
     * @param session      protocol session
     * @param contextId    context id
     * @param type         type name
     * @param message      message text
     * @param defaultValue default value
     */
    public BidiDialog(BidiSession session, String contextId, String type, String message, String defaultValue) {
        this(session, contextId, type, message, defaultValue, false);
    }

    /**
     * Creates a bidi dialog.
     *
     * @param session      protocol session
     * @param contextId    context id
     * @param type         type name
     * @param message      message text
     * @param defaultValue default value
     */
    public BidiDialog(BidiProtocolSession session, String contextId, String type, String message, String defaultValue) {
        this(BidiSession.wrap(session), contextId, type, message, defaultValue);
    }

    /**
     * Creates a bidi dialog.
     *
     * @param session      protocol session
     * @param contextId    context id
     * @param type         type name
     * @param message      message text
     * @param defaultValue default value
     * @param handled      handled
     */
    public BidiDialog(BidiSession session, String contextId, String type, String message, String defaultValue,
            boolean handled) {
        this.session = Assert.notNull(session, "session");
        this.contextId = Assert.notBlank(contextId, "contextId");
        this.type = type == null ? Normal.EMPTY : type;
        this.message = message == null ? Normal.EMPTY : message;
        this.defaultValue = defaultValue == null ? Normal.EMPTY : defaultValue;
        this.handled = new AtomicBoolean(handled);
    }

    /**
     * Returns the accept.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> accept() {
        return accept(null);
    }

    /**
     * Returns the accept.
     *
     * @param promptText prompt text value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> accept(String promptText) {
        return handle(true, promptText);
    }

    /**
     * Returns the dismiss.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> dismiss() {
        return handle(false, null);
    }

    /**
     * Returns the handle.
     *
     * @param accept     accept value
     * @param promptText prompt text value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> handle(boolean accept, String promptText) {
        if (!handled.compareAndSet(false, true)) {
            CompletableFuture<CdpPayload> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(new InternalException("BiDi dialog has already been handled."));
            return rejected;
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("context", contextId);
        params.put("accept", accept);
        if (accept && promptText != null) {
            params.put("userText", promptText);
        }
        return session.send(HANDLE_USER_PROMPT, params);
    }

    /**
     * Handles d.
     *
     * @return handled state
     */
    public boolean handled() {
        return handled.get();
    }

    /**
     * Returns the context ID.
     *
     * @return context ID value
     */
    public String contextId() {
        return contextId;
    }

    /**
     * Returns the type.
     *
     * @return type value
     */
    public String type() {
        return type;
    }

    /**
     * Returns the message.
     *
     * @return message value
     */
    public String message() {
        return message;
    }

    /**
     * Returns the default value.
     *
     * @return default value
     */
    public String defaultValue() {
        return defaultValue;
    }

}
