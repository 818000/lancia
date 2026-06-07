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

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.events.EventHooks;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents a BiDi navigation object.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BidiNavigation implements AutoCloseable {

    /**
     * Shared constant for request.
     */
    public static final String REQUEST = "request";

    /**
     * Shared constant for fragment.
     */
    public static final String FRAGMENT = "fragment";

    /**
     * Shared constant for failed.
     */
    public static final String FAILED = "failed";

    /**
     * Shared constant for aborted.
     */
    public static final String ABORTED = "aborted";
    /**
     * Current context.
     */
    private final BidiBrowsingContext context;
    /**
     * Current emitter.
     */
    private final EventEmitter<String> emitter = new EventEmitter<>();
    /**
     * Current binding.
     */
    private Binding binding = new EventBinding();
    /**
     * Current request.
     */
    private CdpPayload request;
    /**
     * Current navigation.
     */
    private BidiNavigation navigation;
    /**
     * Current identifier.
     */
    private String id;
    /**
     * Whether ID bound is enabled.
     */
    private boolean idBound;
    /**
     * Thread-safe disposed state.
     */
    private final AtomicBoolean disposed = new AtomicBoolean(false);

    /**
     * Returns the from.
     *
     * @param context browser context
     * @return from value
     */
    public static BidiNavigation from(BidiBrowsingContext context) {
        BidiNavigation navigation = new BidiNavigation(context);
        navigation.initialize();
        return navigation;
    }

    /**
     * Creates a BiDi navigation.
     *
     * @param context browser context
     */
    private BidiNavigation(BidiBrowsingContext context) {
        this.context = Assert.notNull(context, "context");
    }

    /**
     * Handles initialize.
     */
    private void initialize() {
        binding = binding.combine(context.once(BidiBrowsingContext.CLOSED, value -> {
            emitter.emit(FAILED, new NavigationInfo(context.url(), Instant.now()));
            dispose();
        }));
        binding = binding.combine(context.on(BidiBrowsingContext.REQUEST, this::onRequest));
        binding = binding.combine(sessionOn("browsingContext.navigationStarted", this::onNavigationStarted));
        binding = binding.combine(sessionOn("browsingContext.domContentLoaded", this::onTerminalNavigationEvent));
        binding = binding.combine(sessionOn("browsingContext.load", this::onTerminalNavigationEvent));
        binding = binding.combine(sessionOn("browsingContext.navigationCommitted", this::onTerminalNavigationEvent));
        binding = binding
                .combine(sessionOn("browsingContext.fragmentNavigated", info -> onNavigationOutcome(FRAGMENT, info)));
        binding = binding
                .combine(sessionOn("browsingContext.navigationFailed", info -> onNavigationOutcome(FAILED, info)));
        binding = binding
                .combine(sessionOn("browsingContext.navigationAborted", info -> onNavigationOutcome(ABORTED, info)));
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
     * Returns the request.
     *
     * @return optional value
     */
    public Optional<CdpPayload> request() {
        return Optional.ofNullable(request);
    }

    /**
     * Returns the navigation.
     *
     * @return optional value
     */
    public Optional<BidiNavigation> navigation() {
        return Optional.ofNullable(navigation);
    }

    /**
     * Returns the disposed.
     *
     * @return {@code true} when the condition matches
     */
    public boolean disposed() {
        return disposed.get();
    }

    /**
     * Releases resources held by this object.
     */
    public void dispose() {
        binding.unbind(disposed);
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        dispose();
    }

    /**
     * Handles on request.
     *
     * @param payload protocol payload
     */
    private void onRequest(Object payload) {
        CdpPayload value = (CdpPayload) payload;
        String navigationId = PayloadReader.nullableText(value.get("navigation"));
        if (navigationId == null || !matches(navigationId)) {
            return;
        }
        request = value;
        emitter.emit(REQUEST, value);
    }

    /**
     * Handles on navigation started.
     *
     * @param info info value
     */
    private void onNavigationStarted(CdpPayload info) {
        if (!context.id().equals(PayloadReader.text(info.get("context"))) || navigation != null) {
            return;
        }
        navigation = BidiNavigation.from(context);
    }

    /**
     * Handles on terminal navigation event.
     *
     * @param info info value
     */
    private void onTerminalNavigationEvent(CdpPayload info) {
        if (!context.id().equals(PayloadReader.text(info.get("context")))) {
            return;
        }
        String navigationId = PayloadReader.nullableText(info.get("navigation"));
        if (navigationId == null || !matches(navigationId)) {
            return;
        }
        dispose();
    }

    /**
     * Handles on navigation outcome.
     *
     * @param event event type
     * @param info  info value
     */
    private void onNavigationOutcome(String event, CdpPayload info) {
        if (!context.id().equals(PayloadReader.text(info.get("context")))
                || !matches(PayloadReader.nullableText(info.get("navigation")))) {
            return;
        }
        emitter.emit(event, new NavigationInfo(PayloadReader.text(info.get("url")), timestamp(info.get("timestamp"))));
        dispose();
    }

    /**
     * Returns the matches.
     *
     * @param navigationId navigation ID value
     * @return {@code true} when the condition matches
     */
    private boolean matches(String navigationId) {
        if (navigation != null && !navigation.disposed()) {
            return false;
        }
        if (!idBound) {
            id = navigationId;
            idBound = true;
            return true;
        }
        return Objects.equals(id, navigationId);
    }

    /**
     * Returns the session on.
     *
     * @param method   protocol method
     * @param listener event listener
     * @return session on value
     */
    private Binding sessionOn(String method, Consumer<CdpPayload> listener) {
        return context.browser().session().connection().on(method, listener);
    }

    /**
     * Returns the timestamp.
     *
     * @param payload protocol payload
     * @return timestamp value
     */
    private static Instant timestamp(CdpPayload payload) {
        if (payload == null || payload.isNull()) {
            return Instant.now();
        }
        Object raw = payload.raw();
        if (raw instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }
        try {
            return Instant.parse(String.valueOf(raw));
        } catch (RuntimeException ignored) {
            return Instant.now();
        }
    }

    /**
     * Represents a BiDi navigation info object.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class NavigationInfo {

        /**
         * Current URL.
         */
        private final String url;
        /**
         * Current timestamp.
         */
        private final Instant timestamp;

        /**
         * Creates an instance.
         *
         * @param url       target URL
         * @param timestamp timestamp value
         */
        public NavigationInfo(String url, Instant timestamp) {
            this.url = url == null ? Normal.EMPTY : url;
            this.timestamp = timestamp == null ? Instant.now() : timestamp;
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
         * Returns the timestamp.
         *
         * @return timestamp value
         */
        public Instant timestamp() {
            return timestamp;
        }
    }

}
