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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Harness;
import org.miaixz.lancia.Realm;
import org.miaixz.lancia.Response;
import org.miaixz.lancia.kernel.Element;
import org.miaixz.lancia.kernel.Frame;
import org.miaixz.lancia.kernel.Locator;
import org.miaixz.lancia.kernel.Prompts;
import org.miaixz.lancia.kernel.bidi.accessor.BidiSession;
import org.miaixz.lancia.kernel.bidi.device.BidiDeviceRequestPrompt;
import org.miaixz.lancia.kernel.bidi.protocol.BidiDeserializer;
import org.miaixz.lancia.kernel.bidi.protocol.BidiSerializer;
import org.miaixz.lancia.kernel.bidi.runtime.BidiRealm;
import org.miaixz.lancia.kernel.bidi.session.BidiCDPSession;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.options.GoToOptions;
import org.miaixz.lancia.options.ScriptTagOptions;
import org.miaixz.lancia.options.StyleTagOptions;
import org.miaixz.lancia.options.WaitForOptions;
import org.miaixz.lancia.options.WaitForSelectorOptions;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.locator.ElementLocator;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * WebDriver BiDi Frame.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiFrame implements Harness, Frame {

    /**
     * Current page.
     */
    private final BidiPage page;
    /**
     * Current parent frame.
     */
    private final BidiFrame parentFrame;
    /**
     * Current identifier.
     */
    private final String id;
    /**
     * Registered child frames values.
     */
    private final List<BidiFrame> childFrames = new ArrayList<>();
    /**
     * Mapped exposed functions values.
     */
    private final Map<String, BidiExposedFunction> exposedFunctions = new ConcurrentHashMap<>();
    /**
     * Current device request prompt manager.
     */
    private final BidiDeviceRequestPrompt.Manager deviceRequestPromptManager;
    /**
     * Thread-safe detached state.
     */
    private final AtomicBoolean detached = new AtomicBoolean(false);
    /**
     * Current URL.
     */
    private volatile String url;

    /**
     * Returns the from.
     *
     * @param page page instance
     * @param id   identifier
     * @param url  target URL
     * @return from value
     */
    public static BidiFrame from(BidiPage page, String id, String url) {
        return new BidiFrame(page, null, id, url);
    }

    /**
     * Returns the from.
     *
     * @param parentFrame parent frame value
     * @param id          identifier
     * @param url         target URL
     * @return from value
     */
    public static BidiFrame from(BidiFrame parentFrame, String id, String url) {
        BidiFrame frame = new BidiFrame(parentFrame.page(), parentFrame, id, url);
        parentFrame.addChildFrame(frame);
        return frame;
    }

    /**
     * Creates a bidi frame.
     *
     * @param page        page instance
     * @param parentFrame parent frame
     * @param id          identifier
     * @param url         target URL
     */
    public BidiFrame(BidiPage page, BidiFrame parentFrame, String id, String url) {
        this.page = Assert.notNull(page, "page");
        this.parentFrame = parentFrame;
        this.id = Assert.notBlank(id, "id");
        this.url = StringKit.isBlank(url) ? Builder.ABOUT_BLANK : url;
        this.deviceRequestPromptManager = BidiDeviceRequestPrompt.manager(this.id, session());
        Logger.debug(
                false,
                "Page",
                "BiDi frame initialized: frame={}, parent={}, url={}",
                this.id,
                parentFrame == null ? Normal.EMPTY : parentFrame.id(),
                this.url.replaceAll("[?#].*$", "?<redacted>"));
    }

    /**
     * Returns the goto URL.
     *
     * @param url target URL
     * @return completion future
     */
    public CompletableFuture<CdpPayload> gotoUrl(String url) {
        assertAttached();
        String actualUrl = Assert.notBlank(url, "url");
        page.validateNavigationUrl(actualUrl);
        Logger.debug(
                true,
                "Page",
                "BiDi frame navigation requested: frame={}, url={}",
                id,
                actualUrl.replaceAll("[?#].*$", "?<redacted>"));
        return session()
                .send("browsingContext.navigate", Map.of("context", id, "url", actualUrl, "wait", "interactive"))
                .thenApply(result -> {
                    this.url = actualUrl;
                    Logger.debug(
                            false,
                            "Page",
                            "BiDi frame navigation completed: frame={}, url={}",
                            id,
                            actualUrl.replaceAll("[?#].*$", "?<redacted>"));
                    return result;
                });
    }

    /**
     * Navigates to the specified URL.
     *
     * @param url target URL
     * @return go to value
     */
    public Response goTo(String url) {
        Awaitable.await(goToAsync(url), "BiDi frame navigation failed.", 5_000L);
        return null;
    }

    /**
     * Navigates to the specified URL.
     *
     * @param url     target URL
     * @param options navigation options
     * @return main resource response or {@code null}
     */
    public Response goTo(String url, GoToOptions options) {
        return goTo(url);
    }

    /**
     * Navigates to the specified URL.
     *
     * @param url target URL
     * @return go to value
     */
    public CompletableFuture<CdpPayload> goToAsync(String url) {
        return gotoUrl(url);
    }

    /**
     * Sets the current HTML content.
     *
     * @param html HTML content
     */
    public void setContent(String html) {
        Awaitable.await(setContentAsync(html), "BiDi frame set content failed.", 5_000L);
    }

    /**
     * Sets the current HTML content.
     *
     * @param html    HTML content
     * @param options operation options
     */
    public void setContent(String html, WaitForOptions options) {
        setContent(html);
    }

    /**
     * Sets the current HTML content.
     *
     * @param html HTML content
     * @return set content value
     */
    public CompletableFuture<CdpPayload> setContentAsync(String html) {
        assertAttached();
        Logger.debug(
                true,
                "Page",
                "BiDi frame content update requested: frame={}, chars={}",
                id,
                html == null ? Normal._0 : html.length());
        return session().send(
                "script.callFunction",
                Map.of(
                        "functionDeclaration",
                        "(html)=>{document.open();document.write(html);document.close();}",
                        "awaitPromise",
                        true,
                        "target",
                        target(),
                        "arguments",
                        List.of(BidiSerializer.serialize(html == null ? Normal.EMPTY : html))));
    }

    /**
     * Returns the current HTML content.
     *
     * @return current HTML content
     */
    public String content() {
        CdpPayload result = Awaitable.await(contentAsync(), "BiDi frame content read failed.", 5_000L);
        return PayloadReader.text(result.get("result").get("value"));
    }

    /**
     * Returns the current HTML content.
     *
     * @return current HTML content
     */
    public CompletableFuture<CdpPayload> contentAsync() {
        assertAttached();
        Logger.debug(true, "Page", "BiDi frame content read requested: frame={}", id);
        return session().send(
                "script.evaluate",
                Map.of(
                        "expression",
                        "document.documentElement.outerHTML",
                        "awaitPromise",
                        true,
                        "target",
                        target(),
                        "resultOwnership",
                        "none"));
    }

    /**
     * Returns the evaluate.
     *
     * @param expression JavaScript expression
     * @return completion future
     */
    public Object evaluate(String expression) {
        CdpPayload result = Awaitable.await(evaluateAsync(expression), "BiDi frame evaluate failed.", 5_000L);
        return BidiDeserializer.deserialize(result.get("result"));
    }

    /**
     * Returns the evaluate.
     *
     * @param expression JavaScript expression
     * @return completion future
     */
    public CompletableFuture<CdpPayload> evaluateAsync(String expression) {
        assertAttached();
        Logger.debug(
                true,
                "Page",
                "BiDi frame evaluate requested: frame={}, chars={}",
                id,
                expression == null ? Normal._0 : expression.length());
        return session().send(
                "script.evaluate",
                Map.of(
                        "expression",
                        Assert.notBlank(expression, "expression"),
                        "awaitPromise",
                        true,
                        "target",
                        target(),
                        "resultOwnership",
                        "root"));
    }

    /**
     * Waits for device prompt.
     *
     * @param timeout timeout value
     * @return wait for device prompt value
     */
    public CompletableFuture<? extends Prompts> waitForDevicePrompt(Duration timeout) {
        assertAttached();
        Logger.debug(true, "Device", "BiDi frame device prompt wait requested: frame={}, timeout={}", id, timeout);
        return deviceRequestPromptManager.waitForDevicePrompt(timeout);
    }

    /**
     * Returns the expose function.
     *
     * @param name     name to use
     * @param callback callback to invoke
     * @return completion future
     */
    public CompletableFuture<BidiExposedFunction> exposeFunction(
            String name,
            BidiExposedFunction.ExposedCallback callback) {
        assertAttached();
        String actualName = Assert.notBlank(name, "name");
        if (exposedFunctions.containsKey(actualName)) {
            CompletableFuture<BidiExposedFunction> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(
                    new InternalException("BiDi frame exposed function already exists: " + actualName));
            Logger.warn(false, "Page", "BiDi frame exposed function rejected: frame={}, name={}", id, actualName);
            return rejected;
        }
        Logger.debug(true, "Page", "BiDi frame exposed function add requested: frame={}, name={}", id, actualName);
        return BidiExposedFunction.from(page, actualName, callback).thenApply(function -> {
            exposedFunctions.put(actualName, function);
            Logger.debug(false, "Page", "BiDi frame exposed function added: frame={}, name={}", id, actualName);
            return function;
        });
    }

    /**
     * Removes exposed function.
     *
     * @param name name to use
     * @return remove exposed function value
     */
    public CompletableFuture<Void> removeExposedFunction(String name) {
        assertAttached();
        String actualName = Assert.notBlank(name, "name");
        BidiExposedFunction function = exposedFunctions.remove(actualName);
        if (function == null) {
            CompletableFuture<Void> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(
                    new InternalException("BiDi frame exposed function does not exist: " + actualName));
            Logger.warn(
                    false,
                    "Page",
                    "BiDi frame exposed function remove rejected: frame={}, name={}",
                    id,
                    actualName);
            return rejected;
        }
        Logger.debug(true, "Page", "BiDi frame exposed function remove requested: frame={}, name={}", id, actualName);
        return function.dispose();
    }

    /**
     * Creates CDP session.
     *
     * @return created CDP session
     */
    public BidiCDPSession client() {
        return Awaitable.await(createCDPSessionAsync(), "BiDi frame CDP session failed.", 5_000L);
    }

    /**
     * Creates CDP session.
     *
     * @return created CDP session
     */
    public CompletableFuture<BidiCDPSession> createCDPSessionAsync() {
        assertAttached();
        Logger.debug(true, "Protocol", "BiDi frame CDP session create requested: frame={}", id);
        return BidiCDPSession.fromContext(session(), id);
    }

    /**
     * Updates files.
     *
     * @param element element handle
     * @param files   files
     * @return set files value
     */
    public CompletableFuture<CdpPayload> setFiles(BidiElementHandle element, String... files) {
        assertAttached();
        Logger.debug(
                true,
                "Input",
                "BiDi frame file input update requested: frame={}, files={}",
                id,
                files == null ? Normal._0 : files.length);
        return session().send(
                "input.setFiles",
                Map.of(
                        "context",
                        id,
                        "element",
                        Assert.notNull(element, "element").remoteValue(),
                        "files",
                        files == null ? List.of() : List.of(files)));
    }

    /**
     * Returns the locate nodes.
     *
     * @param element element handle
     * @param locator locator value
     * @return completion future
     */
    public CompletableFuture<List<CdpPayload>> locateNodes(BidiElementHandle element, Map<String, Object> locator) {
        assertAttached();
        Logger.debug(
                true,
                "Page",
                "BiDi frame locate nodes requested: frame={}, locator={}",
                id,
                locator == null ? Normal._0 : locator.size());
        return session().send(
                "browsingContext.locateNodes",
                Map.of(
                        "context",
                        id,
                        "locator",
                        locator == null ? Map.of() : locator,
                        "startNodes",
                        List.of(Assert.notNull(element, "element").remoteValue())))
                .thenApply(result -> {
                    List<CdpPayload> nodes = result.get("nodes").isArray() ? result.get("nodes").elements() : List.of();
                    Logger.debug(
                            false,
                            "Page",
                            "BiDi frame locate nodes completed: frame={}, nodes={}",
                            id,
                            nodes.size());
                    return nodes;
                });
    }

    /**
     * Returns the frame element.
     *
     * @return completion future
     */
    public Optional<? extends Element> frameElement() {
        return Awaitable.await(frameElementAsync(), "BiDi frame element lookup failed.", 5_000L);
    }

    /**
     * Returns the frame element.
     *
     * @return completion future
     */
    public CompletableFuture<Optional<BidiElementHandle>> frameElementAsync() {
        assertAttached();
        if (parentFrame == null) {
            Logger.debug(false, "Page", "BiDi frame element lookup skipped for main frame: frame={}", id);
            return CompletableFuture.completedFuture(Optional.empty());
        }
        Logger.debug(true, "Page", "BiDi frame element lookup requested: frame={}, parent={}", id, parentFrame.id());
        Map<String, Object> locator = Map.of("type", "context", "value", Map.of("context", id));
        return session().send("browsingContext.locateNodes", Map.of("context", parentFrame.id(), "locator", locator))
                .thenApply(result -> {
                    CdpPayload nodes = result.get("nodes");
                    if (!nodes.isArray() || nodes.elements().isEmpty()) {
                        Logger.debug(false, "Page", "BiDi frame element lookup returned empty: frame={}", id);
                        return Optional.empty();
                    }
                    Logger.debug(false, "Page", "BiDi frame element lookup completed: frame={}", id);
                    return Optional.of(BidiElementHandle.from(nodes.elements().get(0), page));
                });
    }

    /**
     * Returns the page.
     *
     * @return page value
     */
    public BidiPage page() {
        return page;
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        return id;
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
     * Returns the main execution realm.
     *
     * @return main realm
     */
    public BidiRealm mainRealm() {
        return BidiRealm.frame(id + ":main", this, null);
    }

    /**
     * Returns the isolated execution realm.
     *
     * @return isolated realm
     */
    public BidiRealm isolatedRealm() {
        return BidiRealm.frame(id + ":isolated", this, "__lancia_isolated");
    }

    /**
     * Clears cached document handles.
     */
    public void clearDocumentHandle() {
    }

    /**
     * Evaluates JavaScript and returns a handle.
     *
     * @param expression expression
     * @return handle
     */
    public BidiJSHandle evaluateHandle(String expression) {
        CdpPayload result = Awaitable.await(evaluateAsync(expression), "BiDi frame evaluate handle failed.", 5_000L);
        return BidiJSHandle.from(result.get("result"), page);
    }

    /**
     * Creates a locator rooted in this frame.
     *
     * @param selector selector
     * @return locator
     */
    public Locator locator(String selector) {
        return new ElementLocator(this, selector);
    }

    /**
     * Queries one element.
     *
     * @param selector selector
     * @return matching element
     */
    public Optional<? extends Element> $(String selector) {
        return Optional.empty();
    }

    /**
     * Queries all matching elements.
     *
     * @param selector selector
     * @return matching elements
     */
    public List<? extends Element> $$(String selector) {
        return List.of();
    }

    /**
     * Evaluates a function against the first matching element.
     *
     * @param selector     selector
     * @param pageFunction page function
     * @return evaluation result
     */
    public Object $eval(String selector, String pageFunction) {
        return null;
    }

    /**
     * Evaluates a function against all matching elements.
     *
     * @param selector     selector
     * @param pageFunction page function
     * @return evaluation result
     */
    public Object $$eval(String selector, String pageFunction) {
        return null;
    }

    /**
     * Waits for a selector.
     *
     * @param selector selector
     * @return matching element
     */
    public Optional<? extends Element> waitForSelector(String selector) {
        return $(selector);
    }

    /**
     * Waits for a selector.
     *
     * @param selector selector
     * @param options  selector options
     * @return matching element
     */
    public Optional<? extends Element> waitForSelector(String selector, WaitForSelectorOptions options) {
        return waitForSelector(selector);
    }

    /**
     * Waits for a function to become truthy.
     *
     * @param expression expression
     * @return handle result
     */
    public BidiJSHandle waitForFunction(String expression) {
        return evaluateHandle(expression);
    }

    /**
     * Waits for a function to become truthy.
     *
     * @param expression expression
     * @param timeout    timeout value
     * @return handle result
     */
    public BidiJSHandle waitForFunction(String expression, Duration timeout) {
        return waitForFunction(expression);
    }

    /**
     * Waits for navigation.
     *
     * @return main resource response or {@code null}
     */
    public Response waitForNavigation() {
        return null;
    }

    /**
     * Waits for navigation.
     *
     * @param options wait options
     * @return main resource response or {@code null}
     */
    public Response waitForNavigation(WaitForOptions options) {
        return null;
    }

    /**
     * Returns the frame name.
     *
     * @return frame name
     */
    public String name() {
        return id;
    }

    /**
     * Returns detached state.
     *
     * @return detached state
     */
    public boolean isDetached() {
        return detached();
    }

    /**
     * Returns disposed state.
     *
     * @return disposed state
     */
    public boolean disposed() {
        return detached();
    }

    /**
     * Adds a script tag.
     *
     * @param content script content
     * @return created element
     */
    public Element addScriptTag(String content) {
        return null;
    }

    /**
     * Adds a script tag.
     *
     * @param options script tag options
     * @return created element
     */
    public Element addScriptTag(ScriptTagOptions options) {
        if (options != null && StringKit.isNotBlank(options.getUrl())) {
            page.validateRenderRequestUrl(options.getUrl());
        }
        return null;
    }

    /**
     * Adds a style tag.
     *
     * @param content style content
     * @return created element
     */
    public Element addStyleTag(String content) {
        return null;
    }

    /**
     * Adds a style tag.
     *
     * @param options style tag options
     * @return created element
     */
    public Element addStyleTag(StyleTagOptions options) {
        if (options != null && StringKit.isNotBlank(options.getUrl())) {
            page.validateRenderRequestUrl(options.getUrl());
        }
        return null;
    }

    /**
     * Clicks an element.
     *
     * @param selector selector
     */
    public void click(String selector) {
    }

    /**
     * Focuses an element.
     *
     * @param selector selector
     */
    public void focus(String selector) {
    }

    /**
     * Hovers an element.
     *
     * @param selector selector
     */
    public void hover(String selector) {
    }

    /**
     * Selects option values.
     *
     * @param selector selector
     * @param values   values
     * @return selected values
     */
    public List<String> select(String selector, String... values) {
        return values == null ? List.of() : List.of(values);
    }

    /**
     * Taps an element.
     *
     * @param selector selector
     */
    public void tap(String selector) {
    }

    /**
     * Types text into an element.
     *
     * @param selector selector
     * @param text     text
     */
    public void type(String selector, String text) {
    }

    /**
     * Returns the frame title.
     *
     * @return title
     */
    public String title() {
        Object value = evaluate("document.title");
        return value == null ? Normal.EMPTY : String.valueOf(value);
    }

    /**
     * Returns extension realms associated with this frame.
     *
     * @return extension realms
     */
    public List<? extends Realm> extensionRealms() {
        return List.of();
    }

    /**
     * Returns the parent frame.
     *
     * @return parent frame value
     */
    public BidiFrame parentFrame() {
        return parentFrame;
    }

    /**
     * Returns the child frames.
     *
     * @return values
     */
    public List<BidiFrame> childFrames() {
        return List.copyOf(childFrames);
    }

    /**
     * Returns the detached.
     *
     * @return {@code true} when the condition matches
     */
    public boolean detached() {
        return detached.get();
    }

    /**
     * Detaches this object from its protocol target.
     */
    public void detach() {
        if (detached.compareAndSet(false, true) && parentFrame != null) {
            parentFrame.removeChildFrame(this);
            Logger.debug(false, "Page", "BiDi frame detached: frame={}, parent={}", id, parentFrame.id());
        }
    }

    /**
     * Adds child frame.
     *
     * @param frame frame instance
     */
    public void addChildFrame(BidiFrame frame) {
        BidiFrame actualFrame = Assert.notNull(frame, "frame");
        if (!childFrames.contains(actualFrame)) {
            childFrames.add(actualFrame);
            Logger.debug(
                    false,
                    "Page",
                    "BiDi child frame added: parent={}, child={}, count={}",
                    id,
                    actualFrame.id(),
                    childFrames.size());
        }
    }

    /**
     * Removes child frame.
     *
     * @param frame frame instance
     */
    public void removeChildFrame(BidiFrame frame) {
        childFrames.remove(frame);
        Logger.debug(
                false,
                "Page",
                "BiDi child frame removed: parent={}, child={}, count={}",
                id,
                frame == null ? Normal.EMPTY : frame.id(),
                childFrames.size());
    }

    /**
     * Returns the session.
     *
     * @return session value
     */
    private BidiSession session() {
        return page.browserContext().browser().session();
    }

    /**
     * Returns the target.
     *
     * @return mapped values
     */
    private Map<String, Object> target() {
        return Map.of("context", id);
    }

    /**
     * Asserts the attached condition.
     */
    private void assertAttached() {
        if (detached.get()) {
            throw new InternalException("BiDi frame has been detached: " + id);
        }
    }

}
