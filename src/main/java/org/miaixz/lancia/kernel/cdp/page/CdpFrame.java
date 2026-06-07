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

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.core.xyz.ThreadKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Harness;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.Response;
import org.miaixz.lancia.kernel.Frame;
import org.miaixz.lancia.kernel.Locator;
import org.miaixz.lancia.kernel.cdp.accessibility.CdpAccessibility;
import org.miaixz.lancia.kernel.cdp.device.CdpDevicePrompt;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.runtime.CdpDomWorld;
import org.miaixz.lancia.kernel.cdp.runtime.CdpElementHandle;
import org.miaixz.lancia.kernel.cdp.runtime.CdpJSHandle;
import org.miaixz.lancia.kernel.cdp.runtime.CdpRealm;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.options.ClickOptions;
import org.miaixz.lancia.options.GoToOptions;
import org.miaixz.lancia.options.KeyboardTypeOptions;
import org.miaixz.lancia.options.ScriptTagOptions;
import org.miaixz.lancia.options.StyleTagOptions;
import org.miaixz.lancia.options.WaitForOptions;
import org.miaixz.lancia.options.WaitForSelectorOptions;
import org.miaixz.lancia.runtime.Scripts;
import org.miaixz.lancia.runtime.SecurityPolicy;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.locator.ElementLocator;
import org.miaixz.lancia.shared.page.TagInjection;
import org.miaixz.lancia.shared.payload.PayloadReader;
import org.miaixz.lancia.shared.query.QueryHandler;
import org.miaixz.lancia.shared.query.QuerySelector;

/**
 * Represents a page frame and its execution realms.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpFrame implements Harness, Frame {

    /**
     * Current page.
     */
    private final Page page;
    /**
     * Current session.
     */
    private CDPSession session;
    /**
     * Current main world.
     */
    private CdpDomWorld mainWorld;
    /**
     * Current main realm.
     */
    private final CdpRealm mainRealm;
    /**
     * Current isolated realm.
     */
    private final CdpRealm isolatedRealm;
    /**
     * Current accessibility.
     */
    private CdpAccessibility accessibility;
    /**
     * Current parent frame.
     */
    private CdpFrame parentFrame;
    /**
     * Registered child frames values.
     */
    private final List<CdpFrame> childFrames = new ArrayList<>();
    /**
     * Mapped extension realms values.
     */
    private final Map<String, CdpRealm> extensionRealms = new LinkedHashMap<>();
    /**
     * Current URL.
     */
    private String url = Builder.ABOUT_BLANK;
    /**
     * Current identifier.
     */
    private String id;
    /**
     * Current loader ID.
     */
    private String loaderId;
    /**
     * Current name.
     */
    private String name = Normal.EMPTY;
    /**
     * Registered lifecycle events values.
     */
    private final Set<String> lifecycleEvents = new LinkedHashSet<>();
    /**
     * Whether has started loading is enabled.
     */
    private boolean hasStartedLoading;
    /**
     * Thread-safe detached state.
     */
    private final AtomicBoolean detached = new AtomicBoolean();

    /**
     * Creates a frame.
     *
     * @param session protocol session
     */
    public CdpFrame(CDPSession session) {
        this(null, session);
    }

    /**
     * Creates a frame.
     *
     * @param page    page instance
     * @param session protocol session
     */
    public CdpFrame(Page page, CDPSession session) {
        this.page = page;
        this.session = session;
        this.mainWorld = session == null ? null : new CdpDomWorld(session);
        this.mainRealm = new CdpRealm(this);
        this.isolatedRealm = new CdpRealm(this);
        this.accessibility = new CdpAccessibility(session);
        Logger.debug(false, "Page", "CdpFrame initialized: hasPage={}, hasSession={}", page != null, session != null);
    }

    /**
     * Navigates to the specified URL.
     *
     * @param url target URL
     */
    public Response goTo(String url) {
        return goTo(url, (GoToOptions) null);
    }

    /**
     * Navigates to the specified URL.
     *
     * @param url     target URL
     * @param options operation options
     * @return main resource response
     */
    public Response goTo(String url, GoToOptions options) {
        if (page instanceof CdpPage) {
            return CdpPage.Internal.goToFrame(page, this, url, options);
        }
        navigate(
                url,
                options == null ? null : options.getReferer(),
                options == null ? null : options.getReferrerPolicy());
        return null;
    }

    /**
     * Navigates to the specified URL.
     *
     * @param url            target URL
     * @param referer        referer
     * @param referrerPolicy referrer policy
     */
    public Response goTo(String url, String referer, String referrerPolicy) {
        navigate(url, referer, referrerPolicy);
        return null;
    }

    /**
     * Returns the navigate.
     *
     * @param url            target URL
     * @param referer        referer value
     * @param referrerPolicy referrer policy value
     * @return navigate value
     */
    private CdpPayload navigate(String url, String referer, String referrerPolicy) {
        throwIfDetached();
        validateNavigationUrl(url);
        Logger.debug(
                true,
                "Page",
                "CdpFrame navigation requested: frameId={}, url={}",
                id,
                String.valueOf(url).replaceAll("[?#].*$", "?<redacted>"));
        if (session == null) {
            this.url = url;
            Logger.debug(false, "Page", "CdpFrame navigation stored locally: frameId={}", id);
            return CdpPayload.NULL;
        }
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("url", url);
            if (StringKit.isNotBlank(id)) {
                params.put("frameId", id);
            }
            if (StringKit.isNotBlank(referer)) {
                params.put("referrer", referer);
            }
            if (StringKit.isNotBlank(referrerPolicy)) {
                params.put("referrerPolicy", referrerPolicyToProtocol(referrerPolicy));
            }
            CdpPayload result = session.send("Page.navigate", params).get(5, TimeUnit.SECONDS);
            String errorText = PayloadReader.text(result.get("errorText"));
            if (StringKit.isNotBlank(errorText) && !"net::ERR_HTTP_RESPONSE_CODE_FAILURE".equals(errorText)) {
                throw new InternalException(errorText + " at " + url);
            }
            if (!result.get("frameId").isNull()) {
                this.id = result.get("frameId").asText();
            }
            if (!result.get("loaderId").isNull()) {
                this.loaderId = result.get("loaderId").asText();
            }
            this.url = url;
            Logger.debug(
                    false,
                    "Page",
                    "CdpFrame navigation completed: frameId={}, loaderId={}",
                    this.id,
                    this.loaderId);
            return result;
        } catch (Exception ex) {
            if (ex instanceof InternalException internalException) {
                throw internalException;
            }
            Logger.error(false, "Page", "CdpFrame navigation failed: frameId={}, message={}", id, ex.getMessage());
            throw new InternalException("Page navigation failed: " + url, ex);
        }
    }

    /**
     * Handles goto URL.
     *
     * @param url target URL
     */
    void gotoUrl(String url) {
        goTo(url);
    }

    /**
     * Handles goto page.
     *
     * @param url target URL
     */
    void gotoPage(String url) {
        goTo(url);
    }

    /**
     * Returns the current HTML content.
     *
     * @return current HTML content
     */
    public String content() {
        throwIfDetached();
        Logger.debug(true, "Page", "CdpFrame content requested: frameId={}", id);
        if (session == null) {
            return "<html><head></head><body></body></html>";
        }
        Object value = evaluate("document.documentElement.outerHTML");
        Logger.debug(
                false,
                "Page",
                "CdpFrame content received: frameId={}, chars={}",
                id,
                String.valueOf(value).length());
        return String.valueOf(value);
    }

    /**
     * Updates content.
     *
     * @param html HTML content
     */
    public void setContent(String html) {
        throwIfDetached();
        Logger.debug(
                true,
                "Page",
                "CdpFrame content update requested: frameId={}, htmlChars={}",
                id,
                html == null ? Normal._0 : html.length());
        setFrameContent(html);
    }

    /**
     * Sets the current HTML content.
     *
     * @param html    HTML content
     * @param options wait options
     */
    public void setContent(String html, WaitForOptions options) {
        setContent(html);
    }

    /**
     * Updates frame content.
     *
     * @param html HTML content
     */
    public void setFrameContent(String html) {
        if (session == null) {
            Logger.debug(false, "Page", "CdpFrame content update skipped without session: frameId={}", id);
            return;
        }
        String literal = mainWorld.executionContext().literal(html);
        evaluate("document.open();document.write(" + literal + ");document.close();");
    }

    /**
     * Returns the evaluate.
     *
     * @param expression JavaScript expression
     * @return evaluate value
     */
    public Object evaluate(String expression) {
        throwIfDetached();
        Logger.debug(
                true,
                "Page",
                "CdpFrame evaluate requested: frameId={}, expressionChars={}",
                id,
                expression == null ? Normal._0 : expression.length());
        if (mainWorld == null) {
            Logger.debug(false, "Page", "CdpFrame evaluate skipped without main world: frameId={}", id);
            return null;
        }
        return mainWorld.executionContext().evaluate(expression);
    }

    /**
     * Returns the evaluate handle.
     *
     * @param expression JavaScript expression
     * @return evaluate handle value
     */
    public CdpJSHandle evaluateHandle(String expression) {
        throwIfDetached();
        Logger.debug(
                true,
                "Page",
                "CdpFrame evaluate handle requested: frameId={}, expressionChars={}",
                id,
                expression == null ? Normal._0 : expression.length());
        if (mainWorld == null) {
            Logger.debug(false, "Page", "CdpFrame evaluate handle skipped without main world: frameId={}", id);
            return new CdpJSHandle(CdpPayload.NULL);
        }
        return mainRealm.evaluateHandle(expression);
    }

    /**
     * Waits for selector.
     *
     * @param selector selector text
     * @return wait for selector value
     */
    public Optional<CdpElementHandle> waitForSelector(String selector) {
        throwIfDetached();
        Logger.debug(true, "Page", "CdpFrame wait for selector requested: frameId={}, selector={}", id, selector);
        if (mainWorld == null) {
            Logger.debug(false, "Page", "CdpFrame wait for selector skipped without main world: frameId={}", id);
            return Optional.empty();
        }
        QuerySelector querySelector = QueryHandler.parse(selector);
        CdpPayload remote = mainWorld.executionContext()
                .evaluateRemoteObject(querySelector.queryOneExpression("document"), false);
        if ("object".equals(remote.get("type").asText()) && !remote.get("objectId").isNull()) {
            Logger.debug(false, "Page", "CdpFrame selector matched: frameId={}, selector={}", id, selector);
            return Optional.of(new CdpElementHandle(remote, session));
        }
        Logger.debug(false, "Page", "CdpFrame selector not found: frameId={}, selector={}", id, selector);
        return Optional.empty();
    }

    /**
     * Waits for selector.
     *
     * @param selector selector text
     * @param options  selector wait options
     * @return wait for selector value
     */
    public Optional<CdpElementHandle> waitForSelector(String selector, WaitForSelectorOptions options) {
        return waitForSelector(selector);
    }

    /**
     * Returns the $.
     *
     * @param selector selector text
     * @return optional value
     */
    public Optional<CdpElementHandle> $(String selector) {
        return waitForSelector(selector);
    }

    /**
     * Returns the $$.
     *
     * @param selector selector text
     * @return values
     */
    public List<CdpElementHandle> $$(String selector) {
        if (mainWorld == null) {
            return List.of();
        }
        QuerySelector querySelector = QueryHandler.parse(selector);
        CdpPayload remote = mainWorld.executionContext()
                .evaluateRemoteObject(querySelector.queryAllExpression("document"), false);
        return elementsFromArrayRemoteObject(remote);
    }

    /**
     * Returns the $eval.
     *
     * @param selector     selector text
     * @param pageFunction page function value
     * @return $eval value
     */
    public Object $eval(String selector, String pageFunction) {
        String function = Scripts.checkedFunction(pageFunction);
        QuerySelector querySelector = QueryHandler.parse(selector);
        return evaluate(
                "(async()=>{const element=await " + querySelector.queryOneExpression("document")
                        + ";if(!element){throw new Error('No element found for selector: '+ " + literal(selector)
                        + ");}return (" + function + ")(element);})()");
    }

    /**
     * Returns the $$eval.
     *
     * @param selector     selector text
     * @param pageFunction page function value
     * @return $$eval value
     */
    public Object $$eval(String selector, String pageFunction) {
        String function = Scripts.checkedFunction(pageFunction);
        QuerySelector querySelector = QueryHandler.parse(selector);
        return evaluate(
                "(async()=>{const elements=await " + querySelector.queryAllExpression("document") + ";return ("
                        + function + ")(Array.from(elements));})()");
    }

    /**
     * Handles click.
     *
     * @param selector selector text
     */
    public void click(String selector) {
        Logger.debug(true, "Input", "CdpFrame click requested: frameId={}, selector={}", id, selector);
        waitForSelector(selector).orElseThrow(() -> new IllegalStateException("Could not find element: " + selector))
                .click();
    }

    /**
     * Handles click.
     *
     * @param selector selector text
     * @param options  operation options
     */
    public void click(String selector, ClickOptions options) {
        Logger.debug(true, "Input", "CdpFrame click requested: frameId={}, selector={}, hasOptions=true", id, selector);
        waitForSelector(selector).orElseThrow(() -> new IllegalStateException("Could not find element: " + selector))
                .click(options);
    }

    /**
     * Handles focus.
     *
     * @param selector selector text
     */
    public void focus(String selector) {
        Logger.debug(true, "Input", "CdpFrame focus requested: frameId={}, selector={}", id, selector);
        waitForSelector(selector).orElseThrow(() -> new IllegalStateException("Could not find element: " + selector))
                .focus();
    }

    /**
     * Handles hover.
     *
     * @param selector selector text
     */
    public void hover(String selector) {
        Logger.debug(true, "Input", "CdpFrame hover requested: frameId={}, selector={}", id, selector);
        waitForSelector(selector).orElseThrow(() -> new IllegalStateException("Could not find element: " + selector))
                .hover();
    }

    /**
     * Handles tap.
     *
     * @param selector selector text
     */
    public void tap(String selector) {
        Logger.debug(true, "Input", "CdpFrame tap requested: frameId={}, selector={}", id, selector);
        waitForSelector(selector).orElseThrow(() -> new IllegalStateException("Could not find element: " + selector))
                .tap();
    }

    /**
     * Handles type.
     *
     * @param selector selector text
     * @param text     text to use
     */
    public void type(String selector, String text) {
        Logger.debug(
                true,
                "Input",
                "CdpFrame type requested: frameId={}, selector={}, textChars={}",
                id,
                selector,
                text == null ? Normal._0 : text.length());
        waitForSelector(selector).orElseThrow(() -> new IllegalStateException("Could not find element: " + selector))
                .type(text);
    }

    /**
     * Handles type.
     *
     * @param selector selector text
     * @param text     text to use
     * @param options  operation options
     */
    public void type(String selector, String text, KeyboardTypeOptions options) {
        Logger.debug(
                true,
                "Input",
                "CdpFrame type requested: frameId={}, selector={}, textChars={}, hasOptions=true",
                id,
                selector,
                text == null ? Normal._0 : text.length());
        waitForSelector(selector).orElseThrow(() -> new IllegalStateException("Could not find element: " + selector))
                .type(text, options);
    }

    /**
     * Returns the select.
     *
     * @param selector selector text
     * @param values   values value
     * @return values
     */
    public List<String> select(String selector, String... values) {
        Logger.debug(
                true,
                "Input",
                "CdpFrame select requested: frameId={}, selector={}, valueCount={}",
                id,
                selector,
                values == null ? Normal._0 : values.length);
        return waitForSelector(selector)
                .orElseThrow(() -> new IllegalStateException("Could not find element: " + selector)).select(values);
    }

    /**
     * Returns the locator.
     *
     * @param selector selector text
     * @return locator value
     */
    public Locator locator(String selector) {
        return new ElementLocator(this, selector);
    }

    /**
     * Adds script tag.
     *
     * @param content content
     * @return created script element
     */
    public CdpElementHandle addScriptTag(String content) {
        return addScriptTag(TagInjection.scriptTag(content));
    }

    /**
     * Adds script tag.
     *
     * @param options operation options
     * @return created script element
     */
    public CdpElementHandle addScriptTag(ScriptTagOptions options) {
        throwIfDetached();
        ScriptTagOptions actual = options == null ? new ScriptTagOptions() : options;
        TagInjection.validateTagSources("script", actual.getContent(), actual.getUrl(), actual.getPath());
        Logger.debug(
                true,
                "Page",
                "CdpFrame script tag add requested: frameId={}, source={}",
                id,
                tagSource(actual.getContent(), actual.getUrl(), actual.getPath()));
        if (mainWorld == null) {
            throw new InternalException("CdpFrame is missing an execution context; script tag cannot be injected.");
        }
        String expression;
        if (StringKit.isNotBlank(actual.getUrl())) {
            validateRenderRequestUrl(actual.getUrl());
            expression = scriptUrlExpression(actual.getUrl());
        } else {
            String content = actual.getContent();
            if (StringKit.isNotBlank(actual.getPath())) {
                content = TagInjection.readFile(actual.getPath(), "script") + "\n//# sourceURL="
                        + Scripts.sourceUrl(actual.getPath());
            }
            expression = scriptContentExpression(content);
        }
        return evaluateTagHandle(
                expression,
                "script",
                tagSource(actual.getContent(), actual.getUrl(), actual.getPath()));
    }

    /**
     * Adds style tag.
     *
     * @param content content
     * @return created style element
     */
    public CdpElementHandle addStyleTag(String content) {
        return addStyleTag(TagInjection.styleTag(content));
    }

    /**
     * Adds style tag.
     *
     * @param options operation options
     * @return created style or link element
     */
    public CdpElementHandle addStyleTag(StyleTagOptions options) {
        throwIfDetached();
        StyleTagOptions actual = options == null ? new StyleTagOptions() : options;
        TagInjection.validateTagSources("style", actual.getContent(), actual.getUrl(), actual.getPath());
        Logger.debug(
                true,
                "Page",
                "CdpFrame style tag add requested: frameId={}, source={}",
                id,
                tagSource(actual.getContent(), actual.getUrl(), actual.getPath()));
        if (mainWorld == null) {
            throw new InternalException("CdpFrame is missing an execution context; style tag cannot be injected.");
        }
        String expression;
        if (StringKit.isNotBlank(actual.getUrl())) {
            validateRenderRequestUrl(actual.getUrl());
            expression = styleUrlExpression(actual.getUrl());
        } else {
            String content = actual.getContent();
            if (StringKit.isNotBlank(actual.getPath())) {
                content = TagInjection.readFile(actual.getPath(), "style");
            }
            expression = styleContentExpression(content);
        }
        return evaluateTagHandle(
                expression,
                "style",
                tagSource(actual.getContent(), actual.getUrl(), actual.getPath()));
    }

    /**
     * Creates a script tag expression from inline content.
     *
     * @param content content
     * @return expression
     */
    private String scriptContentExpression(String content) {
        return "(async()=>{const script=document.createElement('script');script.textContent=" + literal(content)
                + ";(document.head||document.documentElement).appendChild(script);return script;})()";
    }

    /**
     * Creates a script tag expression from URL.
     *
     * @param url url
     * @return expression
     */
    private String scriptUrlExpression(String url) {
        String value = literal(url);
        return "(async()=>{const script=document.createElement('script');script.src=" + value
                + ";const promise=new Promise((resolve,reject)=>{script.onload=()=>resolve(script);"
                + "script.onerror=()=>reject(new Error('Could not load script: '+script.src));});"
                + "(document.head||document.documentElement).appendChild(script);await promise;return script;})()";
    }

    /**
     * Creates a style tag expression from inline content.
     *
     * @param content content
     * @return expression
     */
    private String styleContentExpression(String content) {
        return "(async()=>{const style=document.createElement('style');style.textContent=" + literal(content)
                + ";(document.head||document.documentElement).appendChild(style);return style;})()";
    }

    /**
     * Creates a stylesheet link expression from URL.
     *
     * @param url url
     * @return expression
     */
    private String styleUrlExpression(String url) {
        String value = literal(url);
        return "(async()=>{const link=document.createElement('link');link.rel='stylesheet';link.href=" + value
                + ";const promise=new Promise((resolve,reject)=>{link.onload=()=>resolve(link);"
                + "link.onerror=()=>reject(new Error('Could not load style: '+link.href));});"
                + "(document.head||document.documentElement).appendChild(link);await promise;return link;})()";
    }

    /**
     * Evaluates a tag expression and returns the created element.
     *
     * @param expression expression
     * @param tag        tag type
     * @param source     source description
     * @return element handle
     */
    private CdpElementHandle evaluateTagHandle(String expression, String tag, String source) {
        try {
            CdpElementHandle element = evaluateHandle(expression).asElement();
            if (element == null) {
                throw new InternalException("Injecting " + tag + " tag did not return a DOM node: " + source);
            }
            return element;
        } catch (InternalException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new InternalException("Injecting " + tag + " tag failed: " + source, ex);
        }
    }

    /**
     * Returns a diagnostic source description.
     *
     * @param content content
     * @param url     url
     * @param path    path
     * @return source
     */
    private String tagSource(String content, String url, String path) {
        if (StringKit.isNotBlank(url)) {
            return url;
        }
        if (StringKit.isNotBlank(path)) {
            return path;
        }
        return "content(" + (content == null ? Normal._0 : content.length()) + ")";
    }

    /**
     * Validates a render request URL.
     *
     * @param url target URL
     */
    private void validateRenderRequestUrl(String url) {
        if (page instanceof CdpPage) {
            CdpPage.Internal.validateRenderRequestUrl(page, url);
            return;
        }
        SecurityPolicy.defaultPolicy().validateRenderRequestUrl(URI.create(Assert.notBlank(url, "url")));
    }

    /**
     * Validates a navigation URL.
     *
     * @param url target URL
     */
    private void validateNavigationUrl(String url) {
        SecurityPolicy.defaultPolicy().validateNavigationUrl(URI.create(Assert.notBlank(url, "url")));
    }

    /**
     * Returns the title.
     *
     * @return title value
     */
    public String title() {
        Object value = evaluate("document.title");
        return String.valueOf(value);
    }

    /**
     * Waits for navigation.
     *
     * @param timeout timeout value
     * @return wait for navigation value
     */
    public Response waitForNavigation(Duration timeout) {
        WaitForOptions options = new WaitForOptions();
        options.setTimeout(timeout);
        return waitForNavigation(options);
    }

    /**
     * Waits for navigation.
     *
     * @return wait for navigation value
     */
    public Response waitForNavigation() {
        return waitForNavigation((WaitForOptions) null);
    }

    /**
     * Waits for navigation.
     *
     * @param options wait options
     * @return wait for navigation value
     */
    public Response waitForNavigation(WaitForOptions options) {
        throwIfDetached();
        Duration timeout = options == null || options.getTimeout() == null ? Duration.ofSeconds(30)
                : options.getTimeout();
        List<String> waitUntil = options == null || options.getWaitUntil() == null || options.getWaitUntil().isEmpty()
                ? List.of("load")
                : options.getWaitUntil();
        Logger.debug(
                true,
                "Page",
                "CdpFrame wait for navigation requested: frameId={}, timeoutMillis={}",
                id,
                timeout.toMillis());
        if (page == null) {
            Logger.debug(false, "Page", "CdpFrame wait for navigation skipped without page: frameId={}", id);
            return null;
        }
        try {
            Response result = CdpPage.Internal.waitForNavigationAsync(page, timeout, waitUntil.toArray(String[]::new))
                    .get(Math.max(1, timeout.toSeconds() + 1), TimeUnit.SECONDS);
            Logger.debug(false, "Page", "CdpFrame wait for navigation completed: frameId={}", id);
            return result;
        } catch (Exception ex) {
            Logger.error(
                    false,
                    "Page",
                    "CdpFrame wait for navigation failed: frameId={}, message={}",
                    id,
                    ex.getMessage());
            throw new InternalException("Failed to wait for frame navigation.", ex);
        }
    }

    /**
     * Waits for function.
     *
     * @param expression JavaScript expression
     * @return wait for function value
     */
    public CdpJSHandle waitForFunction(String expression) {
        return waitForFunction(expression, Duration.ofSeconds(30L));
    }

    /**
     * Waits for function.
     *
     * @param expression JavaScript expression
     * @param timeout    timeout value
     * @return wait for function value
     */
    public CdpJSHandle waitForFunction(String expression, Duration timeout) {
        Duration actualTimeout = timeout == null ? Duration.ofSeconds(30L) : timeout;
        long deadline = actualTimeout.isZero() || actualTimeout.isNegative() ? Long.MAX_VALUE
                : System.nanoTime() + actualTimeout.toNanos();
        while (true) {
            Object value = evaluate(expression);
            if (truthy(value)) {
                return evaluateHandle(expression);
            }
            if (System.nanoTime() >= deadline) {
                throw new TimeoutException("CdpFrame waitForFunction timed out.");
            }
            ThreadKit.sleep(50L);
        }
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
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        return id;
    }

    /**
     * Handles update ID.
     *
     * @param id identifier
     */
    public void updateId(String id) {
        this.id = id;
    }

    /**
     * Handles update URL.
     *
     * @param url target URL
     */
    void updateUrl(String url) {
        this.url = url;
    }

    /**
     * Returns the loader ID.
     *
     * @return loader ID value
     */
    String loaderId() {
        return loaderId;
    }

    /**
     * Handles update loader ID.
     *
     * @param loaderId loader ID value
     */
    void updateLoaderId(String loaderId) {
        this.loaderId = loaderId;
    }

    /**
     * Returns the main world.
     *
     * @return main world value
     */
    CdpDomWorld mainWorld() {
        return mainWorld;
    }

    /**
     * Returns the page.
     *
     * @return page value
     */
    public Page page() {
        return page;
    }

    /**
     * Returns the client.
     *
     * @return client value
     */
    @Override
    public CDPSession client() {
        return session;
    }

    /**
     * Handles update client.
     *
     * @param client protocol client
     */
    void updateClient(CDPSession client) {
        this.session = client;
        this.mainWorld = client == null ? null : new CdpDomWorld(client);
        this.accessibility = new CdpAccessibility(client);
        Logger.debug(false, "Page", "CdpFrame client updated: frameId={}, hasClient={}", id, client != null);
    }

    /**
     * Returns the accessibility.
     *
     * @return accessibility value
     */
    public CdpAccessibility accessibility() {
        return accessibility;
    }

    /**
     * Returns the main realm.
     *
     * @return main realm value
     */
    @Override
    public CdpRealm mainRealm() {
        return mainRealm;
    }

    /**
     * Returns whether olated realm is enabled.
     *
     * @return isolated realm value
     */
    public CdpRealm isolatedRealm() {
        return isolatedRealm;
    }

    /**
     * Clears the cached document handle for this frame.
     */
    public void clearDocumentHandle() {
        Logger.debug(false, "Page", "CdpFrame document handle cleared: frameId={}", id);
    }

    /**
     * Returns the extension realm.
     *
     * @param extensionId extension ID value
     * @return extension realm value
     */
    CdpRealm extensionRealm(String extensionId) {
        String key = StringKit.isBlank(extensionId) ? Normal.EMPTY : extensionId;
        return extensionRealms.computeIfAbsent(key, ignored -> new CdpRealm(this));
    }

    /**
     * Returns the extension realms.
     *
     * @return values
     */
    public List<CdpRealm> extensionRealms() {
        return List.copyOf(extensionRealms.values());
    }

    /**
     * Returns the name.
     *
     * @return name value
     */
    public String name() {
        return name;
    }

    /**
     * Handles update name.
     *
     * @param name name to use
     */
    void updateName(String name) {
        this.name = name == null ? Normal.EMPTY : name;
    }

    /**
     * Returns the parent frame.
     *
     * @return parent frame value
     */
    public CdpFrame parentFrame() {
        return parentFrame;
    }

    /**
     * Handles update parent frame.
     *
     * @param parentFrame parent frame value
     */
    void updateParentFrame(CdpFrame parentFrame) {
        this.parentFrame = parentFrame;
    }

    /**
     * Returns the child frames.
     *
     * @return values
     */
    public List<CdpFrame> childFrames() {
        return List.copyOf(childFrames);
    }

    /**
     * Handles add child frame.
     *
     * @param childFrame child frame value
     */
    void addChildFrame(CdpFrame childFrame) {
        if (childFrame != null && !childFrames.contains(childFrame)) {
            childFrame.updateParentFrame(this);
            childFrames.add(childFrame);
            Logger.debug(false, "Page", "CdpFrame child added: frameId={}, childCount={}", id, childFrames.size());
        }
    }

    /**
     * Handles remove child frame.
     *
     * @param childFrame child frame value
     */
    void removeChildFrame(CdpFrame childFrame) {
        if (childFrames.remove(childFrame) && childFrame != null) {
            childFrame.updateParentFrame(null);
            Logger.debug(false, "Page", "CdpFrame child removed: frameId={}, childCount={}", id, childFrames.size());
        }
    }

    /**
     * Returns the add preload script.
     *
     * @param preloadScript preload script value
     * @return completion future
     */
    CompletableFuture<Void> addPreloadScript(CdpPreloadScript preloadScript) {
        throwIfDetached();
        if (session == null || preloadScript == null || StringKit.isNotBlank(preloadScript.getIdForFrame(this))) {
            Logger.debug(false, "Page", "CdpFrame preload script add skipped: frameId={}", id);
            return CompletableFuture.completedFuture(null);
        }
        CdpFrame parent = parentFrame();
        if (parent != null && parent.client() == session) {
            return CompletableFuture.completedFuture(null);
        }
        Logger.debug(
                true,
                "Page",
                "CdpFrame preload script add requested: frameId={}, sourceChars={}",
                id,
                preloadScript.source().length());
        return session.send("Page.addScriptToEvaluateOnNewDocument", Map.of("source", preloadScript.source()))
                .thenAccept(result -> {
                    preloadScript.setIdForFrame(this, PayloadReader.text(result.get("identifier")));
                    Logger.debug(false, "Page", "CdpFrame preload script added: frameId={}", id);
                });
    }

    /**
     * Returns the add exposed function.
     *
     * @param function function to invoke
     * @return completion future
     */
    CompletableFuture<Void> addExposedFunction(ExposedFunction function) {
        throwIfDetached();
        if (session == null || function == null || (parentFrame != null && !hasStartedLoading)) {
            Logger.debug(false, "Page", "CdpFrame exposed function add skipped: frameId={}", id);
            return CompletableFuture.completedFuture(null);
        }
        Logger.debug(true, "Page", "CdpFrame exposed function add requested: frameId={}, name={}", id, function.name());
        return CompletableFuture.supplyAsync(() -> {
            try {
                mainWorld.executionContext().addExposedFunction(function);
            } catch (RuntimeException ignored) {
                // Implementation note.
            }
            return null;
        });
    }

    /**
     * Returns the remove exposed function.
     *
     * @param function function to invoke
     * @return completion future
     */
    CompletableFuture<Void> removeExposedFunction(ExposedFunction function) {
        throwIfDetached();
        if (session == null || function == null || (parentFrame != null && !hasStartedLoading)) {
            Logger.debug(false, "Page", "CdpFrame exposed function remove skipped: frameId={}", id);
            return CompletableFuture.completedFuture(null);
        }
        Logger.debug(
                true,
                "Page",
                "CdpFrame exposed function remove requested: frameId={}, name={}",
                id,
                function.name());
        return CompletableFuture.supplyAsync(() -> {
            try {
                mainWorld.executionContext().removeExposedFunction(function.name());
            } catch (RuntimeException ignored) {
                // Implementation note.
            }
            return null;
        });
    }

    /**
     * Waits for device prompt.
     *
     * @param timeout timeout value
     * @return wait for device prompt value
     */
    public CompletableFuture<CdpDevicePrompt> waitForDevicePrompt(Duration timeout) {
        throwIfDetached();
        Logger.debug(
                true,
                "Device",
                "CdpFrame wait for device prompt requested: frameId={}, timeoutMillis={}",
                id,
                timeout == null ? Normal._0 : timeout.toMillis());
        if (page == null) {
            CompletableFuture<CdpDevicePrompt> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(
                    new InternalException(
                            "CdpFrame is missing its owning page; cannot wait for device request prompt."));
            Logger.warn(false, "Device", "CdpFrame wait for device prompt rejected without page: frameId={}", id);
            return rejected;
        }
        return page.waitForDevicePrompt(timeout).thenApply(CdpDevicePrompt.class::cast);
    }

    /**
     * Returns the frame element.
     *
     * @return optional value
     */
    public Optional<CdpElementHandle> frameElement() {
        throwIfDetached();
        Logger.debug(true, "Page", "CdpFrame owner element requested: frameId={}", id);
        CdpFrame parent = parentFrame();
        if (parent == null || parent.client() == null) {
            Logger.debug(false, "Page", "CdpFrame owner element skipped without parent client: frameId={}", id);
            return Optional.empty();
        }
        try {
            CdpPayload result = parent.client().send("DOM.getFrameOwner", Map.of("frameId", id()))
                    .get(5, TimeUnit.SECONDS);
            CdpJSHandle handle = parent.mainRealm().adoptBackendNode(result.get("backendNodeId").asInt());
            Logger.debug(false, "Page", "CdpFrame owner element resolved: frameId={}", id);
            return Optional.ofNullable(handle.asElement());
        } catch (Exception ex) {
            Logger.error(false, "Page", "CdpFrame owner element failed: frameId={}, message={}", id, ex.getMessage());
            throw new InternalException("Failed to get frame element.", ex);
        }
    }

    /**
     * Handles navigated.
     *
     * @param framePayload frame payload value
     */
    void _navigated(CdpPayload framePayload) {
        updateName(PayloadReader.text(framePayload.get("name")));
        String nextUrl = PayloadReader.text(framePayload.get("url"))
                + PayloadReader.text(framePayload.get("urlFragment"));
        if (StringKit.isNotBlank(nextUrl)) {
            updateUrl(nextUrl);
        }
        String loader = PayloadReader.text(framePayload.get("loaderId"));
        if (StringKit.isNotBlank(loader)) {
            updateLoaderId(loader);
        }
        Logger.debug(
                false,
                "Page",
                "CdpFrame navigated: frameId={}, loaderId={}, url={}",
                id,
                loaderId,
                String.valueOf(url).replaceAll("[?#].*$", "?<redacted>"));
    }

    /**
     * Handles navigated within document.
     *
     * @param url target URL
     */
    void _navigatedWithinDocument(String url) {
        updateUrl(url);
        Logger.debug(
                false,
                "Page",
                "CdpFrame same-document navigation: frameId={}, url={}",
                id,
                String.valueOf(url).replaceAll("[?#].*$", "?<redacted>"));
    }

    /**
     * Handles on lifecycle event.
     *
     * @param loaderId loader ID value
     * @param name     name to use
     */
    void _onLifecycleEvent(String loaderId, String name) {
        if ("init".equals(name)) {
            updateLoaderId(loaderId);
            lifecycleEvents.clear();
        }
        if (StringKit.isNotBlank(name)) {
            lifecycleEvents.add(name);
        }
        Logger.debug(false, "Page", "CdpFrame lifecycle event: frameId={}, event={}, loaderId={}", id, name, loaderId);
    }

    /**
     * Handles on loading started.
     */
    void _onLoadingStarted() {
        hasStartedLoading = true;
        Logger.debug(false, "Page", "CdpFrame loading started: frameId={}", id);
    }

    /**
     * Handles on loading stopped.
     */
    void _onLoadingStopped() {
        lifecycleEvents.add("DOMContentLoaded");
        lifecycleEvents.add("load");
        Logger.debug(false, "Page", "CdpFrame loading stopped: frameId={}", id);
    }

    /**
     * Returns the lifecycle events.
     *
     * @return values
     */
    Set<String> lifecycleEvents() {
        return Set.copyOf(lifecycleEvents);
    }

    /**
     * Returns whether started loading is available.
     *
     * @return {@code true} when the condition matches
     */
    boolean hasStartedLoading() {
        return hasStartedLoading;
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
     * Returns whether this object is detached.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isDetached() {
        return detached();
    }

    /**
     * Returns the disposed.
     *
     * @return {@code true} when the condition matches
     */
    public boolean disposed() {
        return detached();
    }

    /**
     * Handles detach.
     */
    void detach() {
        if (detached.compareAndSet(false, true)) {
            Logger.debug(true, "Page", "CdpFrame detach requested: frameId={}, childCount={}", id, childFrames.size());
            for (CdpFrame childFrame : List.copyOf(childFrames)) {
                childFrame.updateParentFrame(null);
            }
            childFrames.clear();
            mainRealm.dispose();
            isolatedRealm.dispose();
            for (CdpRealm realm : extensionRealms.values()) {
                realm.dispose();
            }
            extensionRealms.clear();
            Logger.debug(false, "Page", "CdpFrame detached: frameId={}", id);
        }
    }

    /**
     * Handles throw if detached.
     */
    private void throwIfDetached() {
        if (detached()) {
            Logger.warn(false, "Page", "Detached frame usage rejected: frameId={}", id);
            throw new InternalException("Attempted to use detached CdpFrame '" + id + "'.");
        }
    }

    /**
     * Reads element handles from a remote JavaScript array.
     *
     * @param remote remote array object
     * @return element handles
     */
    private List<CdpElementHandle> elementsFromArrayRemoteObject(CdpPayload remote) {
        String arrayObjectId = PayloadReader.text(remote.get("objectId"));
        if (StringKit.isBlank(arrayObjectId)) {
            return List.of();
        }
        CdpPayload result = Awaitable.await(
                session.send("Runtime.getProperties", Map.of("objectId", arrayObjectId, "ownProperties", true)),
                "Failed to read frame query results.");
        List<CdpElementHandle> elements = new ArrayList<>();
        for (CdpPayload descriptor : result.get("result").elements()) {
            CdpPayload value = descriptor.get("value");
            if (!value.get("objectId").isNull()) {
                elements.add(new CdpElementHandle(value, session));
            }
        }
        CdpJSHandle.releaseObject(session, remote);
        return List.copyOf(elements);
    }

    /**
     * Creates a JavaScript literal string.
     *
     * @param value JavaScript string value
     * @return literal
     */
    private String literal(String value) {
        if (mainWorld != null) {
            return mainWorld.executionContext().literal(value);
        }
        String text = value == null ? Normal.EMPTY : value;
        return "'" + text.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r") + "'";
    }

    /**
     * Returns the truthy.
     *
     * @param value to use
     * @return {@code true} when the condition matches
     */
    private boolean truthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.doubleValue() != 0D && !Double.isNaN(number.doubleValue());
        }
        if (value instanceof String text) {
            return StringKit.isNotBlank(text);
        }
        return true;
    }

    /**
     * Returns the referrer policy to protocol.
     *
     * @param referrerPolicy referrer policy value
     * @return referrer policy to protocol value
     */
    static String referrerPolicyToProtocol(String referrerPolicy) {
        if (StringKit.isBlank(referrerPolicy)) {
            return Normal.EMPTY;
        }
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;
        for (int i = 0; i < referrerPolicy.length(); i++) {
            char current = referrerPolicy.charAt(i);
            if (current == Symbol.C_MINUS) {
                upperNext = true;
                continue;
            }
            builder.append(upperNext ? Character.toUpperCase(current) : current);
            upperNext = false;
        }
        return builder.toString();
    }

    /**
     * Provides package-safe frame operations for collaborators outside this package.
     */
    public static final class Internal {

        /**
         * Creates no Internal instance.
         */
        private Internal() {
            // No initialization required.
        }

        /**
         * Sends a navigation command for the frame.
         *
         * @param frame          frame
         * @param url            url
         * @param referer        referer
         * @param referrerPolicy referrer policy
         * @return protocol result
         */
        public static CdpPayload navigate(CdpFrame frame, String url, String referer, String referrerPolicy) {
            return frame.navigate(url, referer, referrerPolicy);
        }

    }

}
