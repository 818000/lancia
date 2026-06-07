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
package org.miaixz.lancia.shared.locator;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.core.xyz.ThreadKit;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.events.EventEmitter;
import org.miaixz.lancia.events.LocatorEvent;
import org.miaixz.lancia.kernel.Element;
import org.miaixz.lancia.kernel.Frame;
import org.miaixz.lancia.kernel.Handle;
import org.miaixz.lancia.kernel.Locator;
import org.miaixz.lancia.options.ClickOptions;

/**
 * Represents locator.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class ElementLocator implements Locator {

    /**
     * Returns the value.
     */
    public static final String ACTION = LocatorEvent.ACTION.value();
    /**
     * Default timeout.
     */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    /**
     * Default polling.
     */
    private static final Duration DEFAULT_POLLING = Duration.ofMillis(50);
    /**
     * Current page.
     */
    private final Page page;
    /**
     * Current frame.
     */
    private final Frame frame;
    /**
     * Current selector.
     */
    private final String selector;
    /**
     * Current root.
     */
    private final Element root;
    /**
     * Current emitter.
     */
    private final EventEmitter<String> emitter = new EventEmitter<>();
    /**
     * Current timeout.
     */
    private Duration timeout = DEFAULT_TIMEOUT;
    /**
     * Current visibility.
     */
    private Locator.Visibility visibility = Locator.Visibility.ANY;
    /**
     * Whether wait-for checks are enabled.
     */
    private boolean waitForEnabled = true;
    /**
     * Whether ensure element is in the viewport is enabled.
     */
    private boolean ensureElementIsInTheViewport = true;
    /**
     * Whether wait for stable rectangle is enabled.
     */
    private boolean waitForStableRectangle = true;
    /**
     * Current handle predicate.
     */
    private Predicate<Element> handlePredicate = handle -> true;

    /**
     * Creates a locator.
     *
     * @param page     page instance
     * @param selector selector text
     */
    public ElementLocator(Page page, String selector) {
        this.page = Assert.notNull(page, "page");
        this.frame = null;
        this.selector = Assert.notBlank(selector, "selector");
        this.root = null;
    }

    /**
     * Creates a locator.
     *
     * @param frame    frame instance
     * @param selector selector text
     */
    public ElementLocator(Frame frame, String selector) {
        this.page = null;
        this.frame = Assert.notNull(frame, "frame");
        this.selector = Assert.notBlank(selector, "selector");
        this.root = null;
    }

    /**
     * Creates a locator.
     *
     * @param root root
     */
    public ElementLocator(Element root) {
        this(null, ":scope", root);
    }

    /**
     * Creates a locator.
     *
     * @param page     page instance
     * @param selector selector text
     * @param root     root
     */
    public ElementLocator(Page page, String selector, Element root) {
        this.page = page;
        this.frame = null;
        this.selector = Assert.notBlank(selector, "selector");
        this.root = Assert.notNull(root, "root");
    }

    /**
     * Creates a locator.
     *
     * @param source source value
     */
    protected ElementLocator(ElementLocator source) {
        this.page = source.page;
        this.frame = source.frame;
        this.selector = source.selector;
        this.root = source.root;
        copyOptions(source);
    }

    /**
     * Returns the race.
     *
     * @param locators locators value
     * @return race value
     */
    public static ElementLocator race(List<ElementLocator> locators) {
        if (locators == null || locators.isEmpty()) {
            throw new InternalException("ElementLocator.race requires at least one locator.");
        }
        for (ElementLocator locator : locators) {
            if (locator != null && locator.element().isPresent()) {
                return locator.clone();
            }
        }
        return locators.get(0).clone();
    }

    /**
     * Registers an event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return listener binding
     */
    public Binding on(String event, Consumer<Object> listener) {
        emitter.on(Assert.notBlank(event, "event"), Assert.notNull(listener, "listener"));
        return new EventBinding(() -> emitter.off(event, listener));
    }

    /**
     * Returns the on action.
     *
     * @param listener event listener
     * @return on action value
     */
    public Binding onAction(Consumer<Object> listener) {
        return on(ACTION, listener);
    }

    /**
     * Returns the clone.
     *
     * @return clone value
     */
    @Override
    public ElementLocator clone() {
        return new ElementLocator(this);
    }

    /**
     * Returns the copy options.
     *
     * @param source source value
     * @return copy options value
     */
    public ElementLocator copyOptions(ElementLocator source) {
        ElementLocator actualSource = Assert.notNull(source, "source");
        this.timeout = actualSource.timeout;
        this.visibility = actualSource.visibility;
        this.waitForEnabled = actualSource.waitForEnabled;
        this.ensureElementIsInTheViewport = actualSource.ensureElementIsInTheViewport;
        this.waitForStableRectangle = actualSource.waitForStableRectangle;
        this.handlePredicate = actualSource.handlePredicate;
        return this;
    }

    /**
     * Updates timeout.
     *
     * @param timeout timeout value
     * @return set timeout value
     */
    public ElementLocator setTimeout(Duration timeout) {
        ElementLocator locator = clone();
        locator.timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        return locator;
    }

    /**
     * Returns the timeout.
     *
     * @return timeout value
     */
    public Duration timeout() {
        return timeout;
    }

    /**
     * Updates visibility.
     *
     * @param visibility visibility
     * @return set visibility value
     */
    public ElementLocator setVisibility(Locator.Visibility visibility) {
        ElementLocator locator = clone();
        locator.visibility = visibility == null ? Locator.Visibility.ANY : visibility;
        return locator;
    }

    /**
     * Returns the visibility.
     *
     * @return visibility value
     */
    public Locator.Visibility visibility() {
        return visibility;
    }

    /**
     * Updates wait for enabled.
     *
     * @param value to use
     * @return set wait for enabled value
     */
    public ElementLocator setWaitForEnabled(boolean value) {
        ElementLocator locator = clone();
        locator.waitForEnabled = value;
        return locator;
    }

    /**
     * Updates ensure element is in the viewport.
     *
     * @param value to use
     * @return set ensure element is in the viewport value
     */
    public ElementLocator setEnsureElementIsInTheViewport(boolean value) {
        ElementLocator locator = clone();
        locator.ensureElementIsInTheViewport = value;
        return locator;
    }

    /**
     * Updates wait for stable rectangle.
     *
     * @param value to use
     * @return set wait for stable rectangle value
     */
    public ElementLocator setWaitForStableRectangle(boolean value) {
        ElementLocator locator = clone();
        locator.waitForStableRectangle = value;
        return locator;
    }

    /**
     * Returns the filter.
     *
     * @param predicate predicate value
     * @return filter value
     */
    public ElementLocator filter(Predicate<? super Element> predicate) {
        ElementLocator locator = clone();
        Predicate<? super Element> actualPredicate = Assert.notNull(predicate, "predicate");
        locator.handlePredicate = handle -> this.handlePredicate.test(handle) && actualPredicate.test(handle);
        return locator;
    }

    /**
     * Creates a descendant locator scoped to this locator root.
     *
     * @param selector selector
     * @return locator value
     */
    public ElementLocator locator(String selector) {
        ElementLocator locator = new ElementLocator(page, selector, root == null ? waitHandle() : root);
        locator.copyOptions(this);
        return locator;
    }

    /**
     * Returns the map.
     *
     * @param mapper mapper value
     * @return map value
     */
    public Handle map(Function<? super Element, ? extends Handle> mapper) {
        return Assert.notNull(mapper, "mapper").apply(waitHandle());
    }

    /**
     * Returns the element.
     *
     * @return optional value
     */
    public Optional<? extends Element> element() {
        if (root != null) {
            Optional<? extends Element> handle = isRootSelfSelector() ? Optional.of(root) : root.$(selector);
            return handle.filter(this::matches);
        }
        if (page == null) {
            if (frame != null) {
                Optional<? extends Element> handle = frame.$(selector);
                return handle.filter(this::matches);
            }
            return Optional.empty();
        }
        Optional<? extends Element> handle = page.$(selector);
        return handle.filter(this::matches);
    }

    /**
     * Returns the wait handle.
     *
     * @return wait handle value
     */
    public Element waitHandle() {
        return waitHandle(timeout);
    }

    /**
     * Returns the wait handle.
     *
     * @param timeout timeout value
     * @return wait handle value
     */
    public Element waitHandle(Duration timeout) {
        Duration actualTimeout = timeout == null ? this.timeout : timeout;
        long deadline = actualTimeout.isZero() || actualTimeout.isNegative() ? Long.MAX_VALUE
                : System.nanoTime() + actualTimeout.toNanos();
        while (true) {
            Optional<? extends Element> handle = element();
            if (handle.isPresent()) {
                return handle.getOrThrow();
            }
            if (System.nanoTime() >= deadline) {
                throw new TimeoutException("Timed out waiting for locator element: " + selector);
            }
            sleepPolling();
        }
    }

    /**
     * Returns the wait value.
     *
     * @return wait value
     */
    public Object waitValue() {
        return waitHandle().jsonValue();
    }

    /**
     * Handles click.
     */
    public void click() {
        Element handle = prepareForAction();
        emitAction();
        handle.click();
    }

    /**
     * Handles click.
     *
     * @param options operation options
     */
    public void click(ClickOptions options) {
        Element handle = prepareForAction();
        emitAction();
        handle.click(options);
    }

    /**
     * Handles hover.
     */
    public void hover() {
        Element handle = prepareForAction();
        emitAction();
        handle.hover();
    }

    /**
     * Handles tap.
     */
    public void tap() {
        Element handle = prepareForAction();
        emitAction();
        handle.tap();
    }

    /**
     * Handles fill.
     *
     * @param text text to use
     */
    public void fill(String text) {
        Element handle = prepareForAction();
        emitAction();
        handle.type(text == null ? Normal.EMPTY : text);
    }

    /**
     * Handles fill.
     *
     * @param value to use
     */
    public void fill(boolean value) {
        if (value) {
            click();
        }
    }

    /**
     * Handles scroll.
     *
     * @param scrollTop  scroll top value
     * @param scrollLeft scroll left value
     */
    public void scroll(int scrollTop, int scrollLeft) {
        Element handle = prepareForAction();
        emitAction();
        handle.evaluate("function(){this.scrollTop=" + scrollTop + ";this.scrollLeft=" + scrollLeft + ";}");
    }

    /**
     * Returns the selector.
     *
     * @return selector value
     */
    public String selector() {
        return selector;
    }

    /**
     * Returns the root.
     *
     * @return optional value
     */
    public Optional<? extends Element> root() {
        return Optional.ofNullable(root);
    }

    /**
     * Waits for enabled.
     *
     * @return wait for enabled value
     */
    public boolean waitForEnabled() {
        return waitForEnabled;
    }

    /**
     * Returns the ensure element is in the viewport.
     *
     * @return {@code true} when the condition matches
     */
    public boolean ensureElementIsInTheViewport() {
        return ensureElementIsInTheViewport;
    }

    /**
     * Waits for stable rectangle.
     *
     * @return wait for stable rectangle value
     */
    public boolean waitForStableRectangle() {
        return waitForStableRectangle;
    }

    /**
     * Returns the matches.
     *
     * @param handle handle value
     * @return {@code true} when the condition matches
     */
    private boolean matches(Element handle) {
        if (!handlePredicate.test(handle)) {
            return false;
        }
        if (visibility == Locator.Visibility.VISIBLE) {
            return handle.isIntersectingViewport();
        }
        if (visibility == Locator.Visibility.HIDDEN) {
            return !handle.isIntersectingViewport();
        }
        return true;
    }

    /**
     * Returns the prepare for action.
     *
     * @return prepare for action value
     */
    private Element prepareForAction() {
        Element handle = waitHandle();
        if (ensureElementIsInTheViewport && !handle.isIntersectingViewport()) {
            handle.scrollIntoView();
        }
        if (waitForStableRectangle) {
            handle.rectangle();
        }
        if (waitForEnabled) {
            handle.evaluate("function(){return !this.disabled;}");
        }
        return handle;
    }

    /**
     * Handles emit action.
     */
    private void emitAction() {
        emitter.emit(ACTION, null);
    }

    /**
     * Returns whether root self selector is enabled.
     *
     * @return {@code true} when the condition matches
     */
    private boolean isRootSelfSelector() {
        return ":scope".equals(selector);
    }

    /**
     * Handles sleep polling.
     */
    private void sleepPolling() {
        if (!ThreadKit.sleep(DEFAULT_POLLING.toMillis())) {
            throw new InternalException("ElementLocator wait was interrupted.");
        }
    }

}
