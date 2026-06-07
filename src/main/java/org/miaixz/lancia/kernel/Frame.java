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
package org.miaixz.lancia.kernel;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.miaixz.bus.core.lang.Optional;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.Realm;
import org.miaixz.lancia.Response;
import org.miaixz.lancia.options.GoToOptions;
import org.miaixz.lancia.options.ScriptTagOptions;
import org.miaixz.lancia.options.StyleTagOptions;
import org.miaixz.lancia.options.WaitForOptions;
import org.miaixz.lancia.options.WaitForSelectorOptions;

/**
 * Represents a page frame.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Frame {

    /**
     * Returns the frame identifier.
     *
     * @return frame identifier
     */
    String id();

    /**
     * Returns the page that owns this frame.
     *
     * @return owning page
     */
    Page page();

    /**
     * Navigates this frame.
     *
     * @param url target URL
     * @return main resource response or {@code null}
     */
    Response goTo(String url);

    /**
     * Navigates this frame.
     *
     * @param url     target URL
     * @param options navigation options
     * @return main resource response or {@code null}
     */
    Response goTo(String url, GoToOptions options);

    /**
     * Waits for this frame to navigate.
     *
     * @return main resource response or {@code null}
     */
    Response waitForNavigation();

    /**
     * Waits for this frame to navigate.
     *
     * @param options wait options
     * @return main resource response or {@code null}
     */
    Response waitForNavigation(WaitForOptions options);

    /**
     * Returns the frame URL.
     *
     * @return frame URL
     */
    String url();

    /**
     * Returns the main execution realm.
     *
     * @return main realm
     */
    Realm mainRealm();

    /**
     * Returns the isolated execution realm.
     *
     * @return isolated realm
     */
    Realm isolatedRealm();

    /**
     * Clears cached document handles for this frame.
     */
    void clearDocumentHandle();

    /**
     * Returns this frame's owner element.
     *
     * @return frame element
     */
    Optional<? extends Element> frameElement();

    /**
     * Evaluates JavaScript in this frame.
     *
     * @param expression expression
     * @return evaluation result
     */
    Object evaluate(String expression);

    /**
     * Evaluates JavaScript and returns a handle.
     *
     * @param expression expression
     * @return handle result
     */
    Handle evaluateHandle(String expression);

    /**
     * Creates a locator rooted in this frame.
     *
     * @param selector selector
     * @return locator
     */
    Locator locator(String selector);

    /**
     * Queries one element.
     *
     * @param selector selector
     * @return matching element
     */
    Optional<? extends Element> $(String selector);

    /**
     * Queries all matching elements.
     *
     * @param selector selector
     * @return matching elements
     */
    List<? extends Element> $$(String selector);

    /**
     * Evaluates a function against the first matching element.
     *
     * @param selector     selector
     * @param pageFunction page function
     * @return evaluation result
     */
    Object $eval(String selector, String pageFunction);

    /**
     * Evaluates a function against all matching elements.
     *
     * @param selector     selector
     * @param pageFunction page function
     * @return evaluation result
     */
    Object $$eval(String selector, String pageFunction);

    /**
     * Waits for a selector.
     *
     * @param selector selector
     * @return matching element
     */
    Optional<? extends Element> waitForSelector(String selector);

    /**
     * Waits for a selector.
     *
     * @param selector selector
     * @param options  selector options
     * @return matching element
     */
    Optional<? extends Element> waitForSelector(String selector, WaitForSelectorOptions options);

    /**
     * Waits for a function to become truthy.
     *
     * @param expression expression
     * @return handle result
     */
    Handle waitForFunction(String expression);

    /**
     * Waits for a function to become truthy.
     *
     * @param expression expression
     * @param timeout    timeout value
     * @return handle result
     */
    Handle waitForFunction(String expression, Duration timeout);

    /**
     * Returns this frame's HTML content.
     *
     * @return HTML content
     */
    String content();

    /**
     * Updates content.
     *
     * @param html HTML content
     */
    void setContent(String html);

    /**
     * Updates content.
     *
     * @param html    HTML content
     * @param options operation options
     */
    void setContent(String html, WaitForOptions options);

    /**
     * Returns this frame's name.
     *
     * @return frame name
     */
    String name();

    /**
     * Returns this frame's parent frame.
     *
     * @return parent frame or {@code null}
     */
    Frame parentFrame();

    /**
     * Returns child frames.
     *
     * @return child frames
     */
    List<? extends Frame> childFrames();

    /**
     * Returns whether this object is detached.
     *
     * @return {@code true} when the condition matches
     */
    boolean isDetached();

    /**
     * Returns the disposed.
     *
     * @return {@code true} when the condition matches
     */
    boolean disposed();

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
     * @return created style or link element
     */
    Element addStyleTag(StyleTagOptions options);

    /**
     * Clicks an element.
     *
     * @param selector selector
     */
    void click(String selector);

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
     * Selects option values.
     *
     * @param selector selector
     * @param values   values
     * @return selected values
     */
    List<String> select(String selector, String... values);

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
     * Returns the frame title.
     *
     * @return title
     */
    String title();

    /**
     * Waits for a device request prompt.
     *
     * @param timeout timeout value
     * @return prompt future
     */
    CompletableFuture<? extends Prompts> waitForDevicePrompt(Duration timeout);

    /**
     * Returns extension realms associated with this frame.
     *
     * @return extension realms
     */
    List<? extends Realm> extensionRealms();

}
