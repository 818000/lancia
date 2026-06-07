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
package org.miaixz.lancia.kernel.bidi.accessor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.events.EventHooks;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents a BiDi user prompt.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BidiUserPrompt implements AutoCloseable {

    /**
     * Shared constant for handled.
     */
    public static final String HANDLED = "handled";

    /**
     * Shared constant for closed.
     */
    public static final String CLOSED = "closed";
    /**
     * Current browsing context.
     */
    private final BidiBrowsingContext browsingContext;
    /**
     * Current info.
     */
    private final CdpPayload info;
    /**
     * Current emitter.
     */
    private final EventEmitter<String> emitter = new EventEmitter<>();
    /**
     * Current binding.
     */
    private Binding binding = new EventBinding();

    /**
     * Current result.
     */
    private CdpPayload result;
    /**
     * Current reason.
     */
    private String reason;

    /**
     * Returns the from.
     *
     * @param browsingContext browsing context value
     * @param info            info value
     * @return from value
     */
    public static BidiUserPrompt from(BidiBrowsingContext browsingContext, CdpPayload info) {
        BidiUserPrompt prompt = new BidiUserPrompt(browsingContext, info);
        prompt.initialize();
        return prompt;
    }

    /**
     * Creates a BiDi user prompt.
     *
     * @param browsingContext browsing context
     * @param info            info
     */
    private BidiUserPrompt(BidiBrowsingContext browsingContext, CdpPayload info) {
        this.browsingContext = Assert.notNull(browsingContext, "browsingContext");
        this.info = Assert.notNull(info, "info");
    }

    /**
     * Handles initialize.
     */
    private void initialize() {
        binding = binding.combine(
                browsingContext
                        .once(BidiBrowsingContext.CLOSED, value -> dispose("User prompt already closed: " + value)));
        binding = binding.combine(
                browsingContext.browser().session().connection()
                        .on("browsingContext.userPromptClosed", this::onUserPromptClosed));
    }

    /**
     * Returns the handle.
     *
     * @param options operation options
     * @return completion future
     */
    public CompletableFuture<CdpPayload> handle(Map<String, Object> options) {
        if (closed()) {
            return Awaitable.failed(reason());
        }
        Map<String, Object> params = new LinkedHashMap<>(options == null ? Map.of() : options);
        params.put("context", PayloadReader.text(info.get("context")));
        return browsingContext.browser().session().send("browsingContext.handleUserPrompt", params)
                .thenApply(value -> result == null ? CdpPayload.NULL : result);
    }

    /**
     * Registers an event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return listener binding
     */
    public Binding on(String event, Consumer<Object> listener) {
        return EventHooks.onNamed(emitter, event, listener);
    }

    /**
     * Registers a one-shot event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return listener binding
     */
    public Binding once(String event, Consumer<Object> listener) {
        return EventHooks.onceNamed(emitter, event, listener);
    }

    /**
     * Releases resources held by this object.
     *
     * @param reason reason
     */
    public void dispose(String reason) {
        if (this.reason != null) {
            return;
        }
        this.reason = reason == null
                ? "User prompt already closed, probably because the associated browsing context was destroyed."
                : reason;
        binding.unbind();
        binding = new EventBinding();
        emitter.emit(CLOSED, this.reason);
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        dispose(null);
    }

    /**
     * Returns the browsing context.
     *
     * @return browsing context value
     */
    public BidiBrowsingContext browsingContext() {
        return browsingContext;
    }

    /**
     * Returns the info.
     *
     * @return info value
     */
    public CdpPayload info() {
        return info;
    }

    /**
     * Returns whether this object is closed.
     *
     * @return whether this object is closed
     */
    public boolean closed() {
        return reason != null;
    }

    /**
     * Returns the disposed.
     *
     * @return {@code true} when the condition matches
     */
    public boolean disposed() {
        return closed();
    }

    /**
     * Handles d.
     *
     * @return handled state
     */
    public boolean handled() {
        String handler = PayloadReader.text(info.get("handler"));
        return "accept".equals(handler) || "dismiss".equals(handler) || result != null;
    }

    /**
     * Returns the result.
     *
     * @return optional value
     */
    public Optional<CdpPayload> result() {
        return Optional.ofNullable(result);
    }

    /**
     * Returns the reason.
     *
     * @return reason value
     */
    public String reason() {
        return reason == null ? Normal.EMPTY : reason;
    }

    /**
     * Handles on user prompt closed.
     *
     * @param parameters parameters value
     */
    private void onUserPromptClosed(CdpPayload parameters) {
        if (!browsingContext.id().equals(PayloadReader.text(parameters.get("context")))) {
            return;
        }
        result = parameters;
        emitter.emit(HANDLED, parameters);
        dispose("User prompt already handled.");
    }

}
