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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.miaixz.bus.core.lang.Optional;
import org.miaixz.lancia.Payload;
import org.miaixz.lancia.nimble.Rectangle;
import org.miaixz.lancia.nimble.input.DragData;
import org.miaixz.lancia.nimble.input.DragPoint;
import org.miaixz.lancia.options.ClickOptions;
import org.miaixz.lancia.options.KeyboardTypeOptions;
import org.miaixz.lancia.options.ScreenshotOptions;
import org.miaixz.lancia.options.WaitForSelectorOptions;

/**
 * Represents a remote DOM element handle.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Element extends Handle {

    /**
     * Returns the remote element id.
     *
     * @return element id
     */
    String id();

    /**
     * Returns the frame hosted by this element when it is an iframe/frame.
     *
     * @return content frame
     */
    Optional<? extends Frame> contentFrame();

    /**
     * Returns the owning frame.
     *
     * @return frame
     */
    Frame frame();

    /**
     * Returns this handle as an element.
     *
     * @return this element
     */
    Element asElement();

    /**
     * Returns this element as a locator.
     *
     * @return locator
     */
    Locator asLocator();

    /**
     * Queries one child element.
     *
     * @param selector selector
     * @return matching element
     */
    Optional<? extends Element> $(String selector);

    /**
     * Queries child elements.
     *
     * @param selector selector
     * @return matching elements
     */
    List<? extends Element> $$(String selector);

    /**
     * Evaluates a function against the first matching child element.
     *
     * @param selector     selector
     * @param pageFunction page function
     * @return evaluation result
     */
    Object $eval(String selector, String pageFunction);

    /**
     * Evaluates a function against matching child elements.
     *
     * @param selector     selector
     * @param pageFunction page function
     * @return evaluation result
     */
    Object $$eval(String selector, String pageFunction);

    /**
     * Waits for a matching child element.
     *
     * @param selector selector
     * @return matching element
     */
    Optional<? extends Element> waitForSelector(String selector);

    /**
     * Waits for a matching child element.
     *
     * @param selector selector
     * @param options  selector options
     * @return matching element
     */
    Optional<? extends Element> waitForSelector(String selector, WaitForSelectorOptions options);

    /**
     * Scrolls this element into view.
     */
    void scrollIntoView();

    /**
     * Returns a clickable point for this element.
     *
     * @return clickable point
     */
    DragPoint clickablePoint();

    /**
     * Returns this element's rectangle.
     *
     * @return rectangle
     */
    Rectangle rectangle();

    /**
     * Returns this element's rectangle model.
     *
     * @return rectangle model payload
     */
    Payload rectangleModel();

    /**
     * Screenshots this element.
     *
     * @param format image format
     * @return screenshot bytes
     */
    byte[] screenshot(String format);

    /**
     * Screenshots this element.
     *
     * @param options screenshot options
     * @return screenshot bytes
     */
    byte[] screenshot(ScreenshotOptions options);

    /**
     * Clicks this element.
     */
    void click();

    /**
     * Clicks this element.
     *
     * @param options click options
     */
    void click(ClickOptions options);

    /**
     * Hovers this element.
     */
    void hover();

    /**
     * Taps this element.
     */
    void tap();

    /**
     * Starts a touch at this element.
     *
     * @return protocol future
     */
    CompletableFuture<? extends Payload> touchStart();

    /**
     * Moves the active touch to this element.
     *
     * @return protocol future
     */
    CompletableFuture<? extends Payload> touchMove();

    /**
     * Ends the active touch.
     *
     * @return protocol future
     */
    CompletableFuture<? extends Payload> touchEnd();

    /**
     * Focuses this element.
     */
    void focus();

    /**
     * Types text into this element.
     *
     * @param text text
     */
    void type(String text);

    /**
     * Types text into this element.
     *
     * @param text    text
     * @param options keyboard options
     */
    void type(String text, KeyboardTypeOptions options);

    /**
     * Presses a key in this element.
     *
     * @param key key
     */
    void press(String key);

    /**
     * Presses a key in this element.
     *
     * @param key     key
     * @param options keyboard options
     */
    void press(String key, KeyboardTypeOptions options);

    /**
     * Uploads files to this element.
     *
     * @param files files
     * @return protocol future
     */
    CompletableFuture<? extends Payload> uploadFile(String... files);

    /**
     * Returns whether visible is enabled.
     *
     * @return {@code true} when the condition matches
     */
    boolean isVisible();

    /**
     * Returns whether hidden is enabled.
     *
     * @return {@code true} when the condition matches
     */
    boolean isHidden();

    /**
     * Returns whether intersecting viewport is enabled.
     *
     * @return {@code true} when the condition matches
     */
    boolean isIntersectingViewport();

    /**
     * Queries the accessibility tree for this element.
     *
     * @return accessibility payload
     */
    Payload queryAXTree();

    /**
     * Queries the accessibility tree for this element.
     *
     * @param name accessible name
     * @param role role
     * @return accessibility payload
     */
    Payload queryAXTree(String name, String role);

    /**
     * Returns the autofill.
     *
     * @param data data to use
     * @return completion future
     */
    CompletableFuture<? extends Payload> autofill(Map<String, Object> data);

    /**
     * Returns this element's backend node id.
     *
     * @return backend node id
     */
    int backendNodeId();

    /**
     * Selects option values.
     *
     * @param values values
     * @return selected values
     */
    List<String> select(String... values);

    /**
     * Drags this element to another element.
     *
     * @param target target element
     */
    void dragAndDrop(Element target);

    /**
     * Starts dragging this element.
     *
     * @param target target point
     * @return drag data
     */
    DragData drag(DragPoint target);

    /**
     * Dispatches dragEnter on this element.
     *
     * @param data drag data
     * @return protocol future
     */
    CompletableFuture<? extends Payload> dragEnter(DragData data);

    /**
     * Dispatches dragOver on this element.
     *
     * @param data drag data
     * @return protocol future
     */
    CompletableFuture<? extends Payload> dragOver(DragData data);

    /**
     * Dispatches drop on this element.
     *
     * @param data drag data
     * @return protocol future
     */
    CompletableFuture<? extends Payload> drop(DragData data);

}
