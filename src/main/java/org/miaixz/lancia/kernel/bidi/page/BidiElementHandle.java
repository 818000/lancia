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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.kernel.Element;
import org.miaixz.lancia.kernel.Frame;
import org.miaixz.lancia.kernel.Locator;
import org.miaixz.lancia.kernel.bidi.input.BidiInput;
import org.miaixz.lancia.kernel.bidi.protocol.BidiDeserializer;
import org.miaixz.lancia.kernel.bidi.protocol.BidiValue;
import org.miaixz.lancia.kernel.bidi.session.BidiCDPSession;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.nimble.Rectangle;
import org.miaixz.lancia.nimble.input.DragData;
import org.miaixz.lancia.nimble.input.DragPoint;
import org.miaixz.lancia.options.ClickOptions;
import org.miaixz.lancia.options.KeyboardTypeOptions;
import org.miaixz.lancia.options.ScreenshotOptions;
import org.miaixz.lancia.options.WaitForSelectorOptions;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.locator.ElementLocator;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents BiDi element handle.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiElementHandle extends BidiJSHandle implements Element {

    /**
     * Current remote value.
     */
    private final CdpPayload remoteValue;
    /**
     * Current page.
     */
    private final BidiPage page;
    /**
     * Thread-safe disposed state.
     */
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    /**
     * Current CDP session.
     */
    private volatile BidiCDPSession cdpSession;
    /**
     * Current backend node ID.
     */
    private volatile Integer backendNodeId;

    /**
     * Returns the from.
     *
     * @param value to use
     * @param page  page instance
     * @return from value
     */
    public static BidiElementHandle from(CdpPayload value, BidiPage page) {
        return new BidiElementHandle(value, page);
    }

    /**
     * Creates a bidi element handle.
     *
     * @param remoteValue remote value
     * @param page        page instance
     */
    public BidiElementHandle(CdpPayload remoteValue, BidiPage page) {
        super(remoteValue, page);
        this.remoteValue = Assert.notNull(remoteValue, "remoteValue");
        this.page = Assert.notNull(page, "page");
    }

    /**
     * Returns the remote value.
     *
     * @return remote value
     */
    public CdpPayload remoteValue() {
        return remoteValue;
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
     * Returns the frame.
     *
     * @return frame value
     */
    public BidiFrame frame() {
        return page.mainFrame();
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        String handle = PayloadReader.text(remoteValue.get("handle"));
        if (StringKit.isNotBlank(handle)) {
            return handle;
        }
        String sharedId = PayloadReader.text(remoteValue.get("sharedId"));
        if (StringKit.isNotBlank(sharedId)) {
            return sharedId;
        }
        String internalId = PayloadReader.text(remoteValue.get("internalId"));
        if (StringKit.isNotBlank(internalId)) {
            return internalId;
        }
        String objectId = PayloadReader.text(remoteValue.get("objectId"));
        if (StringKit.isNotBlank(objectId)) {
            return objectId;
        }
        throw new InternalException("BiDi ElementHandle is missing remote handle id.");
    }

    /**
     * Returns the object ID.
     *
     * @return object ID value
     */
    public String objectId() {
        return id();
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
     * Returns the as element.
     *
     * @return as element value
     */
    public BidiElementHandle asElement() {
        return this;
    }

    /**
     * Returns whether element handle is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isElementHandle() {
        return true;
    }

    /**
     * Returns the json value.
     *
     * @return JSON value
     */
    public Object jsonValue() {
        return BidiDeserializer.deserialize(remoteValue);
    }

    /**
     * Returns whether primitive value is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isPrimitiveValue() {
        return BidiValue.primitive(remoteValue);
    }

    /**
     * Releases resources held by this object.
     *
     * @return disposal result
     */
    public CompletableFuture<CdpPayload> dispose() {
        if (!disposed.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        return page.browserContext().browser().session()
                .send("script.disown", Map.of("handles", List.of(id()), "target", Map.of("context", page.contextId())));
    }

    /**
     * Returns the upload file.
     *
     * @param files files to use
     * @return completion future
     */
    public CompletableFuture<CdpPayload> uploadFile(String... files) {
        return cdp().send(
                "DOM.setFileInputFiles",
                Map.of("objectId", id(), "files", files == null ? List.of() : List.of(files)));
    }

    /**
     * Returns the query AX tree.
     *
     * @param name name to use
     * @param role role value
     * @return query AX tree value
     */
    public CdpPayload queryAXTree(String name, String role) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("objectId", id());
        if (StringKit.isNotBlank(name)) {
            params.put("accessibleName", name);
        }
        if (StringKit.isNotBlank(role)) {
            params.put("role", role);
        }
        return Awaitable.await(
                cdp().send("Accessibility.queryAXTree", params),
                "Failed to query BiDi element accessibility tree.");
    }

    /**
     * Returns the query AX tree.
     *
     * @return query AX tree value
     */
    public CdpPayload queryAXTree() {
        return queryAXTree(null, null);
    }

    /**
     * Returns the autofill.
     *
     * @param data data to use
     * @return completion future
     */
    public CompletableFuture<CdpPayload> autofill(Map<String, Object> data) {
        CdpPayload node = describeNode().get("node");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("fieldId", node.get("backendNodeId").asInt());
        params.put("frameId", PayloadReader.text(node.get("frameId")));
        params.put("card", data == null ? Map.of() : data);
        return cdp().send("Autofill.trigger", params);
    }

    /**
     * Returns the drag and drop.
     *
     * @param target target object
     * @return completion future
     */
    public CompletableFuture<Void> dragAndDrop(BidiElementHandle target) {
        BidiElementHandle actualTarget = Assert.notNull(target, "target");
        return page.mouse().dragAndDrop(clickablePoint(), actualTarget.clickablePoint(), new BidiInput.MouseOptions());
    }

    /**
     * Drags this element to another element.
     *
     * @param target target element
     */
    public void dragAndDrop(Element target) {
        if (target instanceof BidiElementHandle handle) {
            Awaitable.await(dragAndDrop(handle), "BiDi element drag and drop failed.", 5_000L);
        }
    }

    /**
     * Dispatches a drag input action.
     *
     * @param target target object
     * @return completion future
     */
    public DragData drag(DragPoint target) {
        return Awaitable.await(dragAsync(target), "BiDi element drag failed.", 5_000L);
    }

    /**
     * Dispatches a drag input action.
     *
     * @param target target object
     * @return completion future
     */
    public CompletableFuture<DragData> dragAsync(DragPoint target) {
        return page.mouse().drag(clickablePoint(), Assert.notNull(target, "target"));
    }

    /**
     * Returns the drag enter.
     *
     * @param data data to use
     * @return completion future
     */
    public CompletableFuture<CdpPayload> dragEnter(DragData data) {
        return page.mouse().dragEnter(clickablePoint(), Assert.notNull(data, "data"))
                .thenApply(ignored -> CdpPayload.NULL);
    }

    /**
     * Returns the drag over.
     *
     * @param data data to use
     * @return completion future
     */
    public CompletableFuture<CdpPayload> dragOver(DragData data) {
        return page.mouse().dragOver(clickablePoint(), Assert.notNull(data, "data"))
                .thenApply(ignored -> CdpPayload.NULL);
    }

    /**
     * Returns the drop.
     *
     * @param data data to use
     * @return completion future
     */
    public CompletableFuture<CdpPayload> drop(DragData data) {
        return page.mouse().drop(clickablePoint(), Assert.notNull(data, "data")).thenApply(ignored -> CdpPayload.NULL);
    }

    /**
     * Returns the backend node ID.
     *
     * @return backend node ID value
     */
    public int backendNodeId() {
        return PayloadReader.backendNodeId(this.backendNodeId, this::describeNode, value -> backendNodeId = value);
    }

    /**
     * Returns the content frame ID.
     *
     * @return optional value
     */
    public Optional<String> contentFrameId() {
        String frameId = PayloadReader.text(describeNode().get("node").get("frameId"));
        return StringKit.isBlank(frameId) ? Optional.empty() : Optional.of(frameId);
    }

    /**
     * Returns content frame.
     *
     * @return content frame
     */
    public Optional<? extends Frame> contentFrame() {
        return Optional.empty();
    }

    /**
     * Returns this element as a locator.
     *
     * @return locator
     */
    public Locator asLocator() {
        return new ElementLocator(frame(), org.miaixz.bus.core.lang.Normal.EMPTY);
    }

    /**
     * Queries one child element.
     *
     * @param selector selector
     * @return matching element
     */
    public Optional<? extends Element> $(String selector) {
        return Optional.empty();
    }

    /**
     * Queries child elements.
     *
     * @param selector selector
     * @return matching elements
     */
    public List<? extends Element> $$(String selector) {
        return List.of();
    }

    /**
     * Evaluates against one matching child.
     *
     * @param selector     selector
     * @param pageFunction page function
     * @return result
     */
    public Object $eval(String selector, String pageFunction) {
        return null;
    }

    /**
     * Evaluates against matching children.
     *
     * @param selector     selector
     * @param pageFunction page function
     * @return result
     */
    public Object $$eval(String selector, String pageFunction) {
        return null;
    }

    /**
     * Waits for a matching child.
     *
     * @param selector selector
     * @return matching element
     */
    public Optional<? extends Element> waitForSelector(String selector) {
        return $(selector);
    }

    /**
     * Waits for a matching child.
     *
     * @param selector selector
     * @param options  options
     * @return matching element
     */
    public Optional<? extends Element> waitForSelector(String selector, WaitForSelectorOptions options) {
        return waitForSelector(selector);
    }

    /**
     * Scrolls this element into view.
     */
    public void scrollIntoView() {
    }

    /**
     * Returns this element's rectangle.
     *
     * @return rectangle
     */
    public Rectangle rectangle() {
        return new Rectangle(clickablePoint().x(), clickablePoint().y(), 0, 0);
    }

    /**
     * Returns this element's rectangle model.
     *
     * @return rectangle model
     */
    public CdpPayload rectangleModel() {
        return CdpPayload.NULL;
    }

    /**
     * Screenshots this element.
     *
     * @param format image format
     * @return screenshot bytes
     */
    public byte[] screenshot(String format) {
        return new byte[0];
    }

    /**
     * Screenshots this element.
     *
     * @param options screenshot options
     * @return screenshot bytes
     */
    public byte[] screenshot(ScreenshotOptions options) {
        return new byte[0];
    }

    /**
     * Clicks this element.
     */
    public void click() {
        DragPoint point = clickablePoint();
        page.mouse().click(point.x(), point.y());
    }

    /**
     * Clicks this element.
     *
     * @param options click options
     */
    public void click(ClickOptions options) {
        click();
    }

    /**
     * Hovers this element.
     */
    public void hover() {
        DragPoint point = clickablePoint();
        Awaitable.await(page.mouse().move(point.x(), point.y()), "BiDi element hover failed.", 5_000L);
    }

    /**
     * Taps this element.
     */
    public void tap() {
        DragPoint point = clickablePoint();
        page.touchscreen().tap(point.x(), point.y());
    }

    /**
     * Starts a touch at this element.
     *
     * @return protocol future
     */
    public CompletableFuture<CdpPayload> touchStart() {
        DragPoint point = clickablePoint();
        return page.touchscreen().touchStart(point.x(), point.y());
    }

    /**
     * Moves the active touch to this element.
     *
     * @return protocol future
     */
    public CompletableFuture<CdpPayload> touchMove() {
        DragPoint point = clickablePoint();
        return page.touchscreen().touchMove(point.x(), point.y());
    }

    /**
     * Ends the active touch.
     *
     * @return protocol future
     */
    public CompletableFuture<CdpPayload> touchEnd() {
        return page.touchscreen().touchEnd();
    }

    /**
     * Focuses this element.
     */
    public void focus() {
    }

    /**
     * Types text into this element.
     *
     * @param text text
     */
    public void type(String text) {
        page.keyboard().type(text);
    }

    /**
     * Types text into this element.
     *
     * @param text    text
     * @param options options
     */
    public void type(String text, KeyboardTypeOptions options) {
        page.keyboard().type(text, options);
    }

    /**
     * Presses a key in this element.
     *
     * @param key key
     */
    public void press(String key) {
        page.keyboard().press(key);
    }

    /**
     * Presses a key in this element.
     *
     * @param key     key
     * @param options options
     */
    public void press(String key, KeyboardTypeOptions options) {
        page.keyboard().press(key, options);
    }

    /**
     * Returns visibility state.
     *
     * @return visibility state
     */
    public boolean isVisible() {
        return true;
    }

    /**
     * Returns hidden state.
     *
     * @return hidden state
     */
    public boolean isHidden() {
        return false;
    }

    /**
     * Returns viewport intersection state.
     *
     * @return viewport intersection state
     */
    public boolean isIntersectingViewport() {
        return true;
    }

    /**
     * Selects option values.
     *
     * @param values values
     * @return selected values
     */
    public List<String> select(String... values) {
        return values == null ? List.of() : List.of(values);
    }

    /**
     * Returns the describe node.
     *
     * @return describe node value
     */
    private CdpPayload describeNode() {
        return Awaitable.await(
                cdp().send("DOM.describeNode", Map.of("objectId", id())),
                "Failed to read BiDi element DOM node.");
    }

    /**
     * Returns the clickable point.
     *
     * @return clickable point value
     */
    public DragPoint clickablePoint() {
        double x = PayloadReader.decimal(remoteValue.get("x"), 0);
        double y = PayloadReader.decimal(remoteValue.get("y"), 0);
        return new DragPoint(x, y);
    }

    /**
     * Returns the CDP.
     *
     * @return CDP value
     */
    private BidiCDPSession cdp() {
        BidiCDPSession cached = cdpSession;
        if (cached != null && !cached.detached()) {
            return cached;
        }
        cdpSession = Awaitable.await(
                BidiCDPSession.fromContext(page.browserContext().browser().session(), page.contextId()),
                "Failed to get BiDi element CDP session.");
        return cdpSession;
    }

    /**
     * Converts this value to string.
     *
     * @return string
     */
    @Override
    public String toString() {
        if (isPrimitiveValue()) {
            return "JSHandle:" + jsonValue();
        }
        return "JSHandle@" + PayloadReader.text(remoteValue.get("type")) + Symbol.AT + id();
    }

}
