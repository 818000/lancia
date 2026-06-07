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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.miaixz.bus.core.lang.Optional;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.options.ClickOptions;
import org.miaixz.lancia.shared.locator.ElementLocator;
import org.miaixz.lancia.shared.locator.RaceLocator;

/**
 * Locates and acts on elements matched by a selector.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Locator {

    /**
     * Creates a race between multiple locators.
     *
     * @param locators candidate locators
     * @return race locator
     */
    static Locator race(List<? extends Locator> locators) {
        if (locators == null || locators.isEmpty()) {
            throw new IllegalArgumentException("Locator.race requires at least one locator.");
        }
        List<ElementLocator> actualLocators = locators.stream().map(locator -> {
            if (locator instanceof ElementLocator actualLocator) {
                return actualLocator;
            }
            throw new IllegalArgumentException("Locator.race requires Lancia locator instances.");
        }).toList();
        return RaceLocator.create(actualLocators);
    }

    /**
     * Registers an action listener.
     *
     * @param listener listener
     * @return unbinder
     */
    Binding onAction(Consumer<Object> listener);

    /**
     * Clones the locator.
     *
     * @return cloned locator
     */
    Locator clone();

    /**
     * Returns a locator with a new timeout.
     *
     * @param timeout timeout
     * @return configured locator
     */
    Locator setTimeout(Duration timeout);

    /**
     * Returns the configured timeout.
     *
     * @return timeout
     */
    Duration timeout();

    /**
     * Returns a locator with a new visibility check.
     *
     * @param visibility visibility option
     * @return configured locator
     */
    Locator setVisibility(Visibility visibility);

    /**
     * Returns the configured visibility check.
     *
     * @return visibility option
     */
    Visibility visibility();

    /**
     * Returns a locator with the enabled check configured.
     *
     * @param value whether to wait for enabled inputs
     * @return configured locator
     */
    Locator setWaitForEnabled(boolean value);

    /**
     * Waits for enabled.
     *
     * @return {@code true} when the condition matches
     */
    boolean waitForEnabled();

    /**
     * Returns a locator with viewport scrolling configured.
     *
     * @param value whether to scroll into the viewport
     * @return configured locator
     */
    Locator setEnsureElementIsInTheViewport(boolean value);

    /**
     * Returns the ensure element is in the viewport.
     *
     * @return {@code true} when the condition matches
     */
    boolean ensureElementIsInTheViewport();

    /**
     * Returns a locator with stable rectangle waiting configured.
     *
     * @param value whether to wait for a stable rectangle
     * @return configured locator
     */
    Locator setWaitForStableRectangle(boolean value);

    /**
     * Waits for stable rectangle.
     *
     * @return {@code true} when the condition matches
     */
    boolean waitForStableRectangle();

    /**
     * Returns a locator filtered by an element predicate.
     *
     * @param predicate element predicate
     * @return filtered locator
     */
    Locator filter(Predicate<? super Element> predicate);

    /**
     * Returns a descendant locator scoped to this locator.
     *
     * @param selector selector
     * @return descendant locator
     */
    Locator locator(String selector);

    /**
     * Maps the located element to a handle.
     *
     * @param mapper element mapper
     * @return mapped handle
     */
    Handle map(Function<? super Element, ? extends Handle> mapper);

    /**
     * Returns the currently matching element if present.
     *
     * @return matching element
     */
    Optional<? extends Element> element();

    /**
     * Waits for the located element.
     *
     * @return element handle
     */
    Element waitHandle();

    /**
     * Waits for the located element.
     *
     * @param timeout timeout
     * @return element handle
     */
    Element waitHandle(Duration timeout);

    /**
     * Waits for the located element and returns its serialized value.
     *
     * @return serialized value
     */
    Object waitValue();

    /**
     * Clicks the located element.
     */
    void click();

    /**
     * Clicks the located element with options.
     *
     * @param options click options
     */
    void click(ClickOptions options);

    /**
     * Hovers over the located element.
     */
    void hover();

    /**
     * Taps the located element.
     */
    void tap();

    /**
     * Fills the located element with text.
     *
     * @param text text value
     */
    void fill(String text);

    /**
     * Fills a checkable located element.
     *
     * @param value boolean value
     */
    void fill(boolean value);

    /**
     * Scrolls the located element.
     *
     * @param scrollTop  scroll top
     * @param scrollLeft scroll left
     */
    void scroll(int scrollTop, int scrollLeft);

    /**
     * Returns the selector.
     *
     * @return selector
     */
    String selector();

    /**
     * Locator visibility option.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    enum Visibility {

        /**
         * Disables visibility checks.
         */
        ANY,

        /**
         * Requires visible elements.
         */
        VISIBLE,

        /**
         * Requires hidden elements.
         */
        HIDDEN
    }

}
