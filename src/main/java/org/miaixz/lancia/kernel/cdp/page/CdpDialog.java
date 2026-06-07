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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;

/**
 * Represents a browser dialog shown by the page.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpDialog {

    /**
     * Current session.
     */
    private final CDPSession session;
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
    private final AtomicBoolean handled = new AtomicBoolean();

    /**
     * Creates a dialog.
     *
     * @param session      protocol session
     * @param type         type name
     * @param message      message text
     * @param defaultValue default value
     */
    public CdpDialog(CDPSession session, String type, String message, String defaultValue) {
        this.session = Assert.notNull(session, "session");
        this.type = type == null ? Normal.EMPTY : type;
        this.message = message == null ? Normal.EMPTY : message;
        this.defaultValue = defaultValue == null ? Normal.EMPTY : defaultValue;
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
     * Returns the handled.
     *
     * @return {@code true} when the condition matches
     */
    boolean handled() {
        return handled.get();
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

    /**
     * Returns the handle.
     *
     * @param accept     accept value
     * @param promptText prompt text value
     * @return completion future
     */
    private CompletableFuture<CdpPayload> handle(boolean accept, String promptText) {
        if (!handled.compareAndSet(false, true)) {
            CompletableFuture<CdpPayload> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(new InternalException("CdpDialog has already been handled."));
            return rejected;
        }
        if (!accept) {
            return session.send("Page.handleJavaScriptDialog", Map.of("accept", false));
        }
        if (promptText == null) {
            return session.send("Page.handleJavaScriptDialog", Map.of("accept", true));
        }
        return session.send("Page.handleJavaScriptDialog", Map.of("accept", true, "promptText", promptText));
    }

}
