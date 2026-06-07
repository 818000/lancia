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
package org.miaixz.lancia.kernel.bidi.target;

import java.util.concurrent.CompletableFuture;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.Session;
import org.miaixz.lancia.Target;
import org.miaixz.lancia.Worker;
import org.miaixz.lancia.kernel.bidi.browser.BidiBrowser;
import org.miaixz.lancia.kernel.bidi.browser.BidiBrowserContext;
import org.miaixz.lancia.kernel.bidi.page.BidiFrame;
import org.miaixz.lancia.kernel.bidi.page.BidiPage;
import org.miaixz.lancia.kernel.bidi.session.BidiCDPSession;
import org.miaixz.lancia.kernel.bidi.worker.BidiWorker;
import org.miaixz.lancia.nimble.browser.TargetType;
import org.miaixz.lancia.shared.async.Awaitable;

/**
 * Represents a BiDi target.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public abstract class BidiTarget implements Target {

    /**
     * Returns the browser.
     *
     * @param browser browser instance
     * @return browser value
     */
    public static BidiTarget browser(BidiBrowser browser) {
        return new BrowserTarget(browser);
    }

    /**
     * Returns the page.
     *
     * @param page page instance
     * @return page value
     */
    public static BidiTarget page(BidiPage page) {
        return new PageTarget(page);
    }

    /**
     * Returns the frame.
     *
     * @param frame frame instance
     * @return frame value
     */
    public static BidiTarget frame(BidiFrame frame) {
        return new FrameTarget(frame);
    }

    /**
     * Returns the worker.
     *
     * @param frame frame instance
     * @param url   target URL
     * @return worker value
     */
    public static BidiTarget worker(BidiFrame frame, String url) {
        return new WorkerTarget(frame, url);
    }

    /**
     * Returns the worker.
     *
     * @param worker worker value
     * @return worker value
     */
    public static BidiTarget worker(BidiWorker worker) {
        return new WorkerTarget(worker);
    }

    /**
     * Returns the page.
     *
     * @return completion future
     */
    public CompletableFuture<BidiPage> pageAsync() {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Returns the as page.
     *
     * @return completion future
     */
    public CompletableFuture<BidiPage> asPageAsync() {
        return pageAsync();
    }

    /**
     * Creates CDP session.
     *
     * @return created CDP session
     */
    public CompletableFuture<BidiCDPSession> createCDPSessionAsync() {
        Logger.debug(true, "Protocol", "BiDi target CDP session requested: type={}", type());
        return CompletableFuture.completedFuture(BidiCDPSession.compatible(browser().session(), type().name()));
    }

    /**
     * Returns the target page.
     *
     * @return page
     */
    public org.miaixz.bus.core.lang.Optional<Page> page() {
        return org.miaixz.bus.core.lang.Optional
                .ofNullable(Awaitable.await(pageAsync(), "BiDi target page resolution failed.", 5_000L));
    }

    /**
     * Returns the target page.
     *
     * @return page
     */
    public Page asPage() {
        return Awaitable.await(asPageAsync(), "BiDi target page resolution failed.", 5_000L);
    }

    /**
     * Returns the target worker.
     *
     * @return worker
     */
    public org.miaixz.bus.core.lang.Optional<Worker> worker() {
        return org.miaixz.bus.core.lang.Optional.empty();
    }

    /**
     * Creates a CDP-compatible protocol session.
     *
     * @return session
     */
    public org.miaixz.bus.core.lang.Optional<? extends Session> createCDPSession() {
        return org.miaixz.bus.core.lang.Optional.ofNullable(
                Awaitable.await(createCDPSessionAsync(), "BiDi target CDP session creation failed.", 5_000L));
    }

    /**
     * Returns the URL.
     *
     * @return URL value
     */
    public abstract String url();

    /**
     * Returns the type.
     *
     * @return type value
     */
    public abstract TargetType type();

    /**
     * Returns the browser.
     *
     * @return browser value
     */
    public abstract BidiBrowser browser();

    /**
     * Returns the browser context.
     *
     * @return browser context value
     */
    public abstract BidiBrowserContext browserContext();

    /**
     * Returns the opener.
     *
     * @return optional value
     */
    public Optional<BidiTarget> opener() {
        return Optional.empty();
    }

    /**
     * Represents a browser target.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class BrowserTarget extends BidiTarget {

        /**
         * Current browser.
         */
        private final BidiBrowser browser;

        /**
         * Creates an instance.
         *
         * @param browser browser instance
         */
        private BrowserTarget(BidiBrowser browser) {
            this.browser = Assert.notNull(browser, "browser");
        }

        /**
         * Returns the URL.
         *
         * @return URL value
         */
        @Override
        public String id() {
            return Normal.EMPTY;
        }

        /**
         * Returns the URL.
         *
         * @return URL value
         */
        @Override
        public String url() {
            return Normal.EMPTY;
        }

        /**
         * Returns the type.
         *
         * @return type value
         */
        @Override
        public TargetType type() {
            return TargetType.BROWSER;
        }

        /**
         * Returns the browser.
         *
         * @return browser value
         */
        @Override
        public BidiBrowser browser() {
            return browser;
        }

        /**
         * Returns the browser context.
         *
         * @return browser context value
         */
        @Override
        public BidiBrowserContext browserContext() {
            return browser.defaultBrowserContext();
        }

        /**
         * Returns the page.
         *
         * @return completion future
         */
        @Override
        public CompletableFuture<BidiPage> pageAsync() {
            return CompletableFuture.completedFuture(browser.defaultBrowserContext().newPage());
        }

        /**
         * Creates CDP session.
         *
         * @return created CDP session
         */
        @Override
        public CompletableFuture<BidiCDPSession> createCDPSessionAsync() {
            Logger.debug(true, "Protocol", "BiDi browser target CDP session requested.");
            return CompletableFuture.completedFuture(BidiCDPSession.compatible(browser.session(), "browser"));
        }
    }

    /**
     * Represents a page target.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class PageTarget extends BidiTarget {

        /**
         * Current page.
         */
        private final BidiPage page;

        /**
         * Creates an instance.
         *
         * @param page page instance
         */
        private PageTarget(BidiPage page) {
            this.page = Assert.notNull(page, "page");
        }

        /**
         * Returns the page.
         *
         * @return completion future
         */
        @Override
        public CompletableFuture<BidiPage> pageAsync() {
            return CompletableFuture.completedFuture(page);
        }

        /**
         * Creates CDP session.
         *
         * @return created CDP session
         */
        @Override
        public CompletableFuture<BidiCDPSession> createCDPSessionAsync() {
            Logger.debug(
                    true,
                    "Protocol",
                    "BiDi page target CDP session requested: url={}",
                    page.url() == null ? Normal.EMPTY : page.url().replaceAll("[?#].*$", "?<redacted>"));
            return page.createCDPSessionAsync();
        }

        /**
         * Returns the URL.
         *
         * @return URL value
         */
        @Override
        public String id() {
            return page.mainFrame().id();
        }

        /**
         * Returns the URL.
         *
         * @return URL value
         */
        @Override
        public String url() {
            return page.url();
        }

        /**
         * Returns the type.
         *
         * @return type value
         */
        @Override
        public TargetType type() {
            return TargetType.PAGE;
        }

        /**
         * Returns the browser.
         *
         * @return browser value
         */
        @Override
        public BidiBrowser browser() {
            return page.browser();
        }

        /**
         * Returns the browser context.
         *
         * @return browser context value
         */
        @Override
        public BidiBrowserContext browserContext() {
            return page.browserContext();
        }
    }

    /**
     * Represents a frame target.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class FrameTarget extends BidiTarget {

        /**
         * Current frame.
         */
        private final BidiFrame frame;

        /**
         * Creates an instance.
         *
         * @param frame frame instance
         */
        private FrameTarget(BidiFrame frame) {
            this.frame = Assert.notNull(frame, "frame");
        }

        /**
         * Returns the page.
         *
         * @return completion future
         */
        @Override
        public CompletableFuture<BidiPage> pageAsync() {
            return CompletableFuture.completedFuture(frame.page());
        }

        /**
         * Creates CDP session.
         *
         * @return created CDP session
         */
        @Override
        public CompletableFuture<BidiCDPSession> createCDPSessionAsync() {
            Logger.debug(true, "Protocol", "BiDi frame target CDP session requested: frame={}", frame.id());
            return frame.createCDPSessionAsync();
        }

        /**
         * Returns the URL.
         *
         * @return URL value
         */
        @Override
        public String id() {
            return frame.id();
        }

        /**
         * Returns the URL.
         *
         * @return URL value
         */
        @Override
        public String url() {
            return frame.url();
        }

        /**
         * Returns the type.
         *
         * @return type value
         */
        @Override
        public TargetType type() {
            return TargetType.PAGE;
        }

        /**
         * Returns the browser.
         *
         * @return browser value
         */
        @Override
        public BidiBrowser browser() {
            return frame.page().browser();
        }

        /**
         * Returns the browser context.
         *
         * @return browser context value
         */
        @Override
        public BidiBrowserContext browserContext() {
            return frame.page().browserContext();
        }

    }

    /**
     * Represents a worker target.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class WorkerTarget extends BidiTarget {

        /**
         * Current frame.
         */
        private final BidiFrame frame;
        /**
         * Current URL.
         */
        private final String url;
        /**
         * Current worker.
         */
        private final BidiWorker worker;

        /**
         * Creates an instance.
         *
         * @param frame frame instance
         * @param url   target URL
         */
        private WorkerTarget(BidiFrame frame, String url) {
            this.frame = Assert.notNull(frame, "frame");
            this.url = url == null ? Normal.EMPTY : url;
            this.worker = null;
        }

        /**
         * Creates an instance.
         *
         * @param worker worker value
         */
        private WorkerTarget(BidiWorker worker) {
            this.worker = Assert.notNull(worker, "worker");
            this.frame = worker.frame();
            this.url = worker.url();
        }

        /**
         * Returns the URL.
         *
         * @return URL value
         */
        @Override
        public String id() {
            return worker == null ? url : worker.url();
        }

        /**
         * Returns the URL.
         *
         * @return URL value
         */
        @Override
        public String url() {
            return worker == null ? url : worker.url();
        }

        /**
         * Returns the type.
         *
         * @return type value
         */
        @Override
        public TargetType type() {
            return TargetType.OTHER;
        }

        /**
         * Returns the page.
         *
         * @return completion future
         */
        @Override
        public CompletableFuture<BidiPage> pageAsync() {
            return CompletableFuture.completedFuture(frame.page());
        }

        /**
         * Creates CDP session.
         *
         * @return created CDP session
         */
        @Override
        public CompletableFuture<BidiCDPSession> createCDPSessionAsync() {
            Logger.debug(
                    true,
                    "Protocol",
                    "BiDi worker target CDP session requested: url={}",
                    url().replaceAll("[?#].*$", "?<redacted>"));
            if (worker != null) {
                return CompletableFuture.completedFuture(worker.client());
            }
            return CompletableFuture.completedFuture(BidiCDPSession.fromFrame(frame.page().browser().session(), frame));
        }

        /**
         * Returns the browser.
         *
         * @return browser value
         */
        @Override
        public BidiBrowser browser() {
            return frame.page().browser();
        }

        /**
         * Returns the browser context.
         *
         * @return browser context value
         */
        @Override
        public BidiBrowserContext browserContext() {
            return frame.page().browserContext();
        }

        /**
         * Returns the target worker.
         *
         * @return worker
         */
        @Override
        public org.miaixz.bus.core.lang.Optional<Worker> worker() {
            return org.miaixz.bus.core.lang.Optional.ofNullable(worker);
        }
    }

}
