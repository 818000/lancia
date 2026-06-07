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
package org.miaixz.lancia;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.miaixz.bus.core.lang.Optional;
import org.miaixz.lancia.events.PageEvent;
import org.miaixz.lancia.kernel.Accessibility;
import org.miaixz.lancia.kernel.Bluetooth;
import org.miaixz.lancia.kernel.Coverage;
import org.miaixz.lancia.kernel.Element;
import org.miaixz.lancia.kernel.FileChooser;
import org.miaixz.lancia.kernel.Frame;
import org.miaixz.lancia.kernel.Handle;
import org.miaixz.lancia.kernel.Keyboard;
import org.miaixz.lancia.kernel.Locator;
import org.miaixz.lancia.kernel.Mcp;
import org.miaixz.lancia.kernel.Mouse;
import org.miaixz.lancia.kernel.Prompts;
import org.miaixz.lancia.kernel.Recorder;
import org.miaixz.lancia.kernel.Touch;
import org.miaixz.lancia.nimble.emulation.Device;
import org.miaixz.lancia.nimble.emulation.Geolocation;
import org.miaixz.lancia.nimble.emulation.IdleState;
import org.miaixz.lancia.nimble.emulation.MediaFeature;
import org.miaixz.lancia.nimble.emulation.Viewport;
import org.miaixz.lancia.nimble.network.Cookie;
import org.miaixz.lancia.nimble.network.CookieParam;
import org.miaixz.lancia.nimble.network.Credentials;
import org.miaixz.lancia.nimble.network.DeleteCookiesParameters;
import org.miaixz.lancia.nimble.network.NetworkConditions;
import org.miaixz.lancia.options.ClickOptions;
import org.miaixz.lancia.options.GoToOptions;
import org.miaixz.lancia.options.KeyboardTypeOptions;
import org.miaixz.lancia.options.PDFOptions;
import org.miaixz.lancia.options.PageCloseOptions;
import org.miaixz.lancia.options.ScreencastOptions;
import org.miaixz.lancia.options.ScreenshotOptions;
import org.miaixz.lancia.options.ScriptTagOptions;
import org.miaixz.lancia.options.StyleTagOptions;
import org.miaixz.lancia.options.UserAgentOptions;
import org.miaixz.lancia.options.WaitForOptions;
import org.miaixz.lancia.options.WaitForSelectorOptions;

/**
 * Public page API for navigation, execution, input, network, and page-level browser capabilities.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Page extends Emitter<PageEvent>, AutoCloseable {

    /**
     * Returns the page target.
     *
     * @return page target
     */
    Target target();

    /**
     * Returns the owning browser.
     *
     * @return browser
     */
    Browser browser();

    /**
     * Returns the browser context.
     *
     * @return browser context
     */
    Context browserContext();

    /**
     * Creates a CDP-compatible protocol session.
     *
     * @return CDP session
     */
    Optional<? extends Session> createCDPSession();

    /**
     * Returns the page window id.
     *
     * @return window id
     */
    String windowId();

    /**
     * Resizes the page contents.
     *
     * @param contentWidth  content width
     * @param contentHeight content height
     * @return command future
     */
    CompletableFuture<? extends Payload> resize(int contentWidth, int contentHeight);

    /**
     * Updates default navigation timeout.
     *
     * @param timeout timeout value
     */
    void setDefaultNavigationTimeout(Duration timeout);

    /**
     * Updates default timeout.
     *
     * @param timeout timeout value
     */
    void setDefaultTimeout(Duration timeout);

    /**
     * Returns default timeout.
     *
     * @return timeout
     */
    Duration getDefaultTimeout();

    /**
     * Returns default navigation timeout.
     *
     * @return timeout
     */
    Duration getDefaultNavigationTimeout();

    /**
     * Returns whether this object is closed.
     *
     * @return {@code true} when the condition matches
     */
    boolean isClosed();

    /**
     * Returns current page URL.
     *
     * @return URL
     */
    String url();

    /**
     * Navigates to the URL.
     *
     * @param url target URL
     * @return main resource response or {@code null}
     */
    Response goTo(String url);

    /**
     * Navigates to the URL.
     *
     * @param url     target URL
     * @param options navigation options
     * @return main resource response or {@code null}
     */
    Response goTo(String url, GoToOptions options);

    /**
     * Waits for navigation.
     *
     * @return main resource response or {@code null}
     */
    Response waitForNavigation();

    /**
     * Waits for navigation.
     *
     * @param options wait options
     * @return main resource response or {@code null}
     */
    Response waitForNavigation(WaitForOptions options);

    /**
     * Waits for navigation.
     *
     * @param timeout   timeout value
     * @param waitUntil lifecycle markers
     * @return main resource response or {@code null}
     */
    Response waitForNavigation(Duration timeout, String... waitUntil);

    /**
     * Waits for network idle state.
     */
    void waitForNetworkIdle();

    /**
     * Waits for network idle state.
     *
     * @param timeout             timeout value
     * @param idleTime            idle time
     * @param maxInflightRequests max in-flight request count
     */
    void waitForNetworkIdle(Duration timeout, Duration idleTime, int maxInflightRequests);

    /**
     * Waits for network idle state using the Puppeteer observable-style entrypoint name.
     */
    void waitForNetworkIdle$();

    /**
     * Waits for a request.
     *
     * @param predicate request predicate
     * @return request
     */
    Request waitForRequest(Predicate<Request> predicate);

    /**
     * Waits for a request matching the URL.
     *
     * @param url request URL
     * @return request
     */
    Request waitForRequest(String url);

    /**
     * Waits for a request matching the URL.
     *
     * @param url     request URL
     * @param options wait options
     * @return request
     */
    Request waitForRequest(String url, WaitForOptions options);

    /**
     * Waits for a request matching the URL pattern.
     *
     * @param pattern request URL pattern
     * @return request
     */
    Request waitForRequest(Pattern pattern);

    /**
     * Waits for a request matching the URL pattern.
     *
     * @param pattern request URL pattern
     * @param options wait options
     * @return request
     */
    Request waitForRequest(Pattern pattern, WaitForOptions options);

    /**
     * Waits for a request.
     *
     * @param predicate request predicate
     * @param timeout   timeout value
     * @return request
     */
    Request waitForRequest(Predicate<Request> predicate, Duration timeout);

    /**
     * Waits for a response.
     *
     * @param predicate response predicate
     * @return response
     */
    Response waitForResponse(Predicate<Response> predicate);

    /**
     * Waits for a response matching the URL.
     *
     * @param url response URL
     * @return response
     */
    Response waitForResponse(String url);

    /**
     * Waits for a response matching the URL.
     *
     * @param url     response URL
     * @param options wait options
     * @return response
     */
    Response waitForResponse(String url, WaitForOptions options);

    /**
     * Waits for a response matching the URL pattern.
     *
     * @param pattern response URL pattern
     * @return response
     */
    Response waitForResponse(Pattern pattern);

    /**
     * Waits for a response matching the URL pattern.
     *
     * @param pattern response URL pattern
     * @param options wait options
     * @return response
     */
    Response waitForResponse(Pattern pattern, WaitForOptions options);

    /**
     * Waits for a response.
     *
     * @param predicate response predicate
     * @param timeout   timeout value
     * @return response
     */
    Response waitForResponse(Predicate<Response> predicate, Duration timeout);

    /**
     * Reloads the page.
     *
     * @return main resource response or {@code null}
     */
    Response reload();

    /**
     * Reloads the page.
     *
     * @param options reload wait options
     * @return main resource response or {@code null}
     */
    Response reload(WaitForOptions options);

    /**
     * Goes back in history.
     *
     * @return main resource response or {@code null}
     */
    Response goBack();

    /**
     * Goes back in history.
     *
     * @param options navigation wait options
     * @return main resource response or {@code null}
     */
    Response goBack(WaitForOptions options);

    /**
     * Goes forward in history.
     *
     * @return main resource response or {@code null}
     */
    Response goForward();

    /**
     * Goes forward in history.
     *
     * @param options navigation wait options
     * @return main resource response or {@code null}
     */
    Response goForward(WaitForOptions options);

    /**
     * Brings the page to front.
     *
     * @return command future
     */
    CompletableFuture<? extends Payload> bringToFront();

    /**
     * Returns page frames.
     *
     * @return frame list
     */
    List<? extends Frame> frames();

    /**
     * Waits for a frame.
     *
     * @param predicate frame predicate
     * @param timeout   timeout value
     * @return frame
     */
    Frame waitForFrame(Predicate predicate, Duration timeout);

    /**
     * Waits for a frame.
     *
     * @param predicate frame predicate
     * @return frame
     */
    Frame waitForFrame(Predicate predicate);

    /**
     * Enables request interception.
     *
     * @param enabled enabled state
     * @return command future
     */
    CompletableFuture<? extends Payload> setRequestInterception(boolean enabled);

    /**
     * Updates bypass service worker.
     *
     * @param bypass bypass value
     * @return completion future
     */
    CompletableFuture<? extends Payload> setBypassServiceWorker(boolean bypass);

    /**
     * Returns service worker bypass state.
     *
     * @return bypass state
     */
    boolean isServiceWorkerBypassed();

    /**
     * Updates drag interception.
     *
     * @param enabled whether the feature should be enabled
     * @return completion future
     */
    CompletableFuture<? extends Payload> setDragInterception(boolean enabled);

    /**
     * Returns drag interception state.
     *
     * @return interception state
     */
    boolean isDragInterceptionEnabled();

    /**
     * Updates offline mode.
     *
     * @param enabled whether the feature should be enabled
     * @return completion future
     */
    CompletableFuture<? extends Payload> setOfflineMode(boolean enabled);

    /**
     * Emulates network conditions.
     *
     * @param conditions network conditions
     * @return command future
     */
    CompletableFuture<? extends Payload> emulateNetworkConditions(NetworkConditions conditions);

    /**
     * Updates extra HTTP headers.
     *
     * @param headers HTTP headers
     * @return completion future
     */
    CompletableFuture<? extends Payload> setExtraHTTPHeaders(Map<String, String> headers);

    /**
     * Updates user agent.
     *
     * @param userAgent user agent value
     * @return completion future
     */
    CompletableFuture<? extends Payload> setUserAgent(String userAgent);

    /**
     * Updates user agent.
     *
     * @param options operation options
     * @return completion future
     */
    CompletableFuture<? extends Payload> setUserAgent(UserAgentOptions options);

    /**
     * Authenticates the page.
     *
     * @param credentials credentials
     * @return command future
     */
    CompletableFuture<? extends Payload> authenticate(Credentials credentials);

    /**
     * Authenticates the page.
     *
     * @param username username
     * @param password password
     * @return command future
     */
    CompletableFuture<? extends Payload> authenticate(String username, String password);

    /**
     * Updates cache enabled.
     *
     * @param enabled whether the feature should be enabled
     * @return completion future
     */
    CompletableFuture<? extends Payload> setCacheEnabled(boolean enabled);

    /**
     * Returns page content.
     *
     * @return content
     */
    String content();

    /**
     * Updates content.
     *
     * @param html HTML content
     */
    void setContent(String html);

    /**
     * Adds a script tag.
     *
     * @param content script content
     * @return created script element
     */
    Element addScriptTag(String content);

    /**
     * Adds a script tag.
     *
     * @param options script tag options
     * @return created script element
     */
    Element addScriptTag(ScriptTagOptions options);

    /**
     * Adds a style tag.
     *
     * @param content style content
     * @return created style element
     */
    Element addStyleTag(String content);

    /**
     * Adds a style tag.
     *
     * @param options style tag options
     * @return created style/link element
     */
    Element addStyleTag(StyleTagOptions options);

    /**
     * Returns page title.
     *
     * @return title
     */
    String title();

    /**
     * Evaluates source on new document.
     *
     * @param source source value
     * @return identifier
     */
    String evaluateOnNewDocument(String source);

    /**
     * Removes source from new document evaluation.
     *
     * @param identifier script identifier
     * @return command future
     */
    CompletableFuture<? extends Payload> removeScriptToEvaluateOnNewDocument(String identifier);

    /**
     * Returns cookies.
     *
     * @param urls URL values
     * @return cookies
     */
    List<Cookie> cookies(String... urls);

    /**
     * Updates cookie.
     *
     * @param cookies cookies to use
     * @return completion future
     */
    CompletableFuture<? extends Payload> setCookie(CookieParam... cookies);

    /**
     * Deletes cookies.
     *
     * @param cookies cookies
     * @return command future
     */
    CompletableFuture<Void> deleteCookie(DeleteCookiesParameters... cookies);

    /**
     * Waits for file chooser.
     *
     * @return file chooser
     */
    FileChooser waitForFileChooser();

    /**
     * Waits for file chooser.
     *
     * @param timeout timeout value
     * @return file chooser
     */
    FileChooser waitForFileChooser(Duration timeout);

    /**
     * Starts screencast recording with the supplied options.
     *
     * @param options screencast options
     * @return active recorder
     */
    Recorder screencast(ScreencastOptions options);

    /**
     * Starts screencast.
     *
     * @return command future
     */
    CompletableFuture<Void> startScreencast();

    /**
     * Stops screencast.
     *
     * @return command future
     */
    CompletableFuture<Void> stopScreencast();

    /**
     * Takes a screenshot.
     *
     * @return screenshot bytes
     */
    byte[] screenshot();

    /**
     * Takes a screenshot.
     *
     * @param options screenshot options
     * @return screenshot bytes
     */
    byte[] screenshot(ScreenshotOptions options);

    /**
     * Creates a PDF.
     *
     * @return PDF bytes
     */
    byte[] pdf();

    /**
     * Creates a PDF.
     *
     * @param options PDF options
     * @return PDF bytes
     */
    byte[] pdf(PDFOptions options);

    /**
     * Evaluates an expression.
     *
     * @param expression expression
     * @return evaluation result
     */
    Object evaluate(String expression);

    /**
     * Runs a function against the first element matching a selector.
     *
     * @param selector     selector
     * @param pageFunction page function
     * @return evaluation result
     */
    Object $eval(String selector, String pageFunction);

    /**
     * Runs a function against all elements matching a selector.
     *
     * @param selector     selector
     * @param pageFunction page function
     * @return evaluation result
     */
    Object $$eval(String selector, String pageFunction);

    /**
     * Evaluates an expression and returns a handle.
     *
     * @param expression expression
     * @return handle
     */
    Handle evaluateHandle(String expression);

    /**
     * Queries objects by prototype handle.
     *
     * @param prototypeHandle prototype handle
     * @return handle
     */
    Handle queryObjects(Handle prototypeHandle);

    /**
     * Waits for a function.
     *
     * @param expression expression
     * @return wait result
     */
    Object waitForFunction(String expression);

    /**
     * Waits for a function.
     *
     * @param expression expression
     * @param timeout    timeout value
     * @return wait result
     */
    Object waitForFunction(String expression, Duration timeout);

    /**
     * Queries one element.
     *
     * @param selector selector
     * @return element handle
     */
    Optional<? extends Element> $(String selector);

    /**
     * Queries all elements.
     *
     * @param selector selector
     * @return element handles
     */
    List<? extends Element> $$(String selector);

    /**
     * Waits for selector.
     *
     * @param selector selector
     * @return element handle
     */
    Optional<? extends Element> waitForSelector(String selector);

    /**
     * Waits for selector.
     *
     * @param selector selector
     * @param options  selector options
     * @return element handle
     */
    Optional<? extends Element> waitForSelector(String selector, WaitForSelectorOptions options);

    /**
     * Clicks an element.
     *
     * @param selector selector
     */
    void click(String selector);

    /**
     * Clicks an element.
     *
     * @param selector selector
     * @param options  click options
     */
    void click(String selector, ClickOptions options);

    /**
     * Focuses an element.
     *
     * @param selector selector
     */
    void focus(String selector);

    /**
     * Hovers an element.
     *
     * @param selector selector
     */
    void hover(String selector);

    /**
     * Taps an element.
     *
     * @param selector selector
     */
    void tap(String selector);

    /**
     * Types text into an element.
     *
     * @param selector selector
     * @param text     text
     */
    void type(String selector, String text);

    /**
     * Types text into an element.
     *
     * @param selector selector
     * @param text     text
     * @param options  keyboard options
     */
    void type(String selector, String text, KeyboardTypeOptions options);

    /**
     * Selects option values.
     *
     * @param selector selector
     * @param values   values
     * @return selected values
     */
    List<String> select(String selector, String... values);

    /**
     * Creates a locator.
     *
     * @param selector selector
     * @return locator
     */
    Locator locator(String selector);

    /**
     * Creates a locator that races the supplied locators.
     *
     * @param locators locators
     * @return racing locator
     */
    Locator locatorRace(List<? extends Locator> locators);

    /**
     * Returns main frame.
     *
     * @return frame
     */
    Frame mainFrame();

    /**
     * Returns coverage.
     *
     * @return coverage
     */
    Coverage coverage();

    /**
     * Returns tracing.
     *
     * @return tracing
     */
    Tracing tracing();

    /**
     * Returns accessibility.
     *
     * @return accessibility
     */
    Accessibility accessibility();

    /**
     * Waits for a device request prompt.
     *
     * @param timeout timeout value
     * @return prompt future
     */
    CompletableFuture<? extends Prompts> waitForDevicePrompt(Duration timeout);

    /**
     * Returns Bluetooth emulation.
     *
     * @return Bluetooth emulation
     */
    Bluetooth bluetooth();

    /**
     * Returns the Lancia-only WebMCP extension controller.
     *
     * @return WebMCP extension controller
     */
    Mcp webmcp();

    /**
     * Returns extension realms.
     *
     * @return realms
     */
    List<? extends Realm> extensionRealms();

    /**
     * Opens DevTools.
     *
     * @return DevTools page
     */
    Page openDevTools();

    /**
     * Returns whether dev tools is available.
     *
     * @return {@code true} when the condition matches
     */
    boolean hasDevTools();

    /**
     * Triggers an extension action.
     *
     * @param extension extension
     * @return completion future
     */
    CompletableFuture<Void> triggerExtensionAction(Extension extension);

    /**
     * Returns workers.
     *
     * @return workers
     */
    List<? extends Worker> workers();

    /**
     * Returns keyboard.
     *
     * @return keyboard
     */
    Keyboard keyboard();

    /**
     * Returns mouse.
     *
     * @return mouse
     */
    Mouse mouse();

    /**
     * Returns touch input.
     *
     * @return touch input
     */
    Touch touchscreen();

    /**
     * Updates viewport.
     *
     * @param viewport viewport value
     * @return completion future
     */
    CompletableFuture<Void> setViewport(Viewport viewport);

    /**
     * Returns viewport.
     *
     * @return viewport
     */
    Viewport viewport();

    /**
     * Emulates a device.
     *
     * @param device device
     * @return completion future
     */
    CompletableFuture<Void> emulate(Device device);

    /**
     * Emulates media type.
     *
     * @param mediaType media type
     * @return completion future
     */
    CompletableFuture<Void> emulateMediaType(String mediaType);

    /**
     * Emulates media features.
     *
     * @param mediaFeatures media features
     * @return completion future
     */
    CompletableFuture<Void> emulateMediaFeatures(List<MediaFeature> mediaFeatures);

    /**
     * Emulates CPU throttling.
     *
     * @param rate rate
     * @return command future
     */
    CompletableFuture<? extends Payload> emulateCPUThrottling(double rate);

    /**
     * Emulates idle state.
     *
     * @param idleState idle state
     * @return command future
     */
    CompletableFuture<? extends Payload> emulateIdleState(IdleState idleState);

    /**
     * Emulates timezone.
     *
     * @param timezone timezone
     * @return command future
     */
    CompletableFuture<? extends Payload> emulateTimezone(String timezone);

    /**
     * Emulates vision deficiency.
     *
     * @param visionDeficiency vision deficiency
     * @return command future
     */
    CompletableFuture<? extends Payload> emulateVisionDeficiency(String visionDeficiency);

    /**
     * Updates geolocation.
     *
     * @param geolocation geolocation value
     * @return completion future
     */
    CompletableFuture<? extends Payload> setGeolocation(Geolocation geolocation);

    /**
     * Updates java script enabled.
     *
     * @param enabled whether the feature should be enabled
     * @return completion future
     */
    CompletableFuture<? extends Payload> setJavaScriptEnabled(boolean enabled);

    /**
     * Returns JavaScript state.
     *
     * @return JavaScript state
     */
    boolean isJavaScriptEnabled();

    /**
     * Updates bypass CSP.
     *
     * @param enabled whether the feature should be enabled
     * @return completion future
     */
    CompletableFuture<? extends Payload> setBypassCSP(boolean enabled);

    /**
     * Emulates focused page state.
     *
     * @param enabled enabled state
     * @return command future
     */
    CompletableFuture<? extends Payload> emulateFocusedPage(boolean enabled);

    /**
     * Returns metrics.
     *
     * @return metrics
     */
    Map<String, Number> metrics();

    /**
     * Captures a heap snapshot.
     *
     * @param path target path
     */
    void captureHeapSnapshot(Path path);

    /**
     * Creates a PDF stream.
     *
     * @param options PDF options
     * @return PDF stream
     */
    InputStream createPDFStream(PDFOptions options);

    /**
     * Exposes a function.
     *
     * @param name function name
     * @return command future
     */
    CompletableFuture<? extends Payload> exposeFunction(String name);

    /**
     * Exposes a Java callback as a page function.
     *
     * @param name     function name
     * @param callback callback
     * @return command future
     */
    CompletableFuture<? extends Payload> exposeFunction(String name, Function<List<Object>, Object> callback);

    /**
     * Removes an exposed function.
     *
     * @param name function name
     * @return command future
     */
    CompletableFuture<? extends Payload> removeExposedFunction(String name);

    /**
     * Closes the page.
     */
    @Override
    void close();

    /**
     * Closes the page.
     *
     * @param options close options
     */
    void close(PageCloseOptions options);

}
