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
package org.miaixz.lancia.kernel.cdp.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.codec.binary.Base64;
import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.ByteKit;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.core.xyz.ThreadKit;
import org.miaixz.lancia.kernel.Element;
import org.miaixz.lancia.kernel.Locator;
import org.miaixz.lancia.kernel.cdp.input.CdpInput;
import org.miaixz.lancia.kernel.cdp.input.CdpKeyboard;
import org.miaixz.lancia.kernel.cdp.input.CdpMouse;
import org.miaixz.lancia.kernel.cdp.input.CdpTouchscreen;
import org.miaixz.lancia.kernel.cdp.page.CdpFrame;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.nimble.Rectangle;
import org.miaixz.lancia.nimble.input.DragData;
import org.miaixz.lancia.nimble.input.DragPoint;
import org.miaixz.lancia.options.ClickOptions;
import org.miaixz.lancia.options.KeyboardTypeOptions;
import org.miaixz.lancia.options.ScreenshotOptions;
import org.miaixz.lancia.options.WaitForSelectorOptions;
import org.miaixz.lancia.runtime.Scripts;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.locator.ElementLocator;
import org.miaixz.lancia.shared.page.PageDefaults;
import org.miaixz.lancia.shared.payload.PayloadReader;
import org.miaixz.lancia.shared.query.QueryHandler;
import org.miaixz.lancia.shared.query.QuerySelector;

/**
 * Represents a handle to a DOM element in a page execution context.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpElementHandle extends CdpJSHandle implements Element {

    /**
     * Default selector timeout millis.
     */
    private static final long DEFAULT_SELECTOR_TIMEOUT_MILLIS = 30000L;
    /**
     * Shared constant for selector poll interval millis.
     */
    private static final long SELECTOR_POLL_INTERVAL_MILLIS = 25L;

    /**
     * Shared constant for is element handle.
     */
    public static final String IS_ELEMENT_HANDLE = "_isElementHandle";
    /**
     * Shared constant for non element node roles.
     */
    private static final Set<String> NON_ELEMENT_NODE_ROLES = Set.of("StaticText", "InlineTextBox");
    /**
     * Current session.
     */
    private final CDPSession session;
    /**
     * Thread-safe disposed state.
     */
    private final AtomicBoolean disposed = new AtomicBoolean();
    /**
     * Current backend node ID.
     */
    private volatile Integer backendNodeId;

    /**
     * Creates an CdpElementHandle instance.
     *
     * @param remoteObject remote object
     */
    public CdpElementHandle(CdpPayload remoteObject) {
        this(remoteObject, null);
    }

    /**
     * Creates an CdpElementHandle instance.
     *
     * @param remoteObject remote object
     * @param session      session
     */
    public CdpElementHandle(CdpPayload remoteObject, CDPSession session) {
        super(remoteObject, session);
        this.session = session;
    }

    /**
     * Returns the content frame.
     *
     * @return optional value
     */
    public Optional<CdpFrame> contentFrame() {
        CdpPayload node = describeNode();
        String frameId = PayloadReader.text(node.get("node").get("frameId"));
        if (StringKit.isBlank(frameId)) {
            return Optional.empty();
        }
        CdpFrame frame = new CdpFrame(session);
        frame.updateId(frameId);
        return Optional.of(frame);
    }

    /**
     * Returns the frame.
     *
     * @return frame value
     */
    public CdpFrame frame() {
        return new CdpFrame(session);
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        return PayloadReader.text(remoteObject().get("objectId"));
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
    public CdpElementHandle asElement() {
        return this;
    }

    /**
     * Converts this handle to an element handle.
     *
     * @return element handle
     */
    public CdpElementHandle toElement() {
        return this;
    }

    /**
     * Creates a locator bound to this element.
     *
     * @return as locator value
     */
    public Locator asLocator() {
        return new ElementLocator(this);
    }

    /**
     * Returns the remote object.
     *
     * @return remote object value
     */
    @Override
    public CdpPayload remoteObject() {
        return super.remoteObject();
    }

    /**
     * Returns whether element handle is enabled.
     *
     * @return {@code true} when the condition matches
     */
    boolean isElementHandle() {
        return true;
    }

    /**
     * Returns the json value.
     *
     * @return JSON value
     */
    public Object jsonValue() {
        CdpPayload value = remoteObject().get("value");
        return value.isNull() ? null : value.raw();
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
        String objectId = id();
        if (session == null || StringKit.isBlank(objectId)) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        return session.send("Runtime.releaseObject", Map.of("objectId", objectId));
    }

    /**
     * Returns the property.
     *
     * @param name name to use
     * @return property
     */
    public Optional<CdpJSHandle> getProperty(String name) {
        Map<String, CdpJSHandle> properties = getProperties();
        return Optional.ofNullable(properties.get(name));
    }

    /**
     * Returns the properties.
     *
     * @return mapped values
     */
    public Map<String, CdpJSHandle> getProperties() {
        CdpPayload result = Awaitable.await(
                session.send("Runtime.getProperties", Map.of("objectId", objectId(), "ownProperties", true)),
                "Failed to read element property.");
        Map<String, CdpJSHandle> properties = new LinkedHashMap<>();
        CdpPayload descriptors = result.get("result");
        if (descriptors.isArray()) {
            for (CdpPayload descriptor : descriptors.elements()) {
                String name = PayloadReader.text(descriptor.get("name"));
                CdpPayload value = descriptor.get("value");
                if (StringKit.isNotBlank(name) && !value.isNull()) {
                    properties.put(name, new CdpJSHandle(value));
                }
            }
        }
        return Map.copyOf(properties);
    }

    /**
     * Returns the $.
     *
     * @param selector selector text
     * @return optional value
     */
    public Optional<CdpElementHandle> $(String selector) {
        QuerySelector querySelector = QueryHandler.parse(selector);
        CdpPayload remote = callFunctionOn(
                "async function(){return await " + querySelector.queryOneExpression("this") + ";}",
                false);
        return elementFromRemote(remote);
    }

    /**
     * Returns the $$.
     *
     * @param selector selector text
     * @return values
     */
    public List<CdpElementHandle> $$(String selector) {
        QuerySelector querySelector = QueryHandler.parse(selector);
        CdpPayload remote = callFunctionOn(
                "async function(){return await " + querySelector.queryAllExpression("this") + ";}",
                false);
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
        CdpElementHandle handle = $(selector)
                .orElseThrow(() -> new InternalException("No element found for selector: " + selector));
        return handle.evaluate("function(){return (" + Scripts.checkedFunction(pageFunction) + ")(this);}");
    }

    /**
     * Returns the $$eval.
     *
     * @param selector     selector text
     * @param pageFunction page function value
     * @return $$eval value
     */
    public Object $$eval(String selector, String pageFunction) {
        QuerySelector querySelector = QueryHandler.parse(selector);
        CdpPayload result = callFunctionOn(
                "async function(){const elements=await " + querySelector.queryAllExpression("this") + ";return ("
                        + Scripts.checkedFunction(pageFunction) + ")(Array.from(elements));}",
                true);
        return valueFromPrimitiveRemoteObject(result);
    }

    /**
     * Waits for selector.
     *
     * @param selector selector text
     * @return wait for selector value
     */
    public Optional<CdpElementHandle> waitForSelector(String selector) {
        return waitForSelector(selector, DEFAULT_SELECTOR_TIMEOUT_MILLIS);
    }

    /**
     * Waits for selector.
     *
     * @param selector selector text
     * @param options  selector wait options
     * @return wait for selector value
     */
    public Optional<CdpElementHandle> waitForSelector(String selector, WaitForSelectorOptions options) {
        WaitForSelectorOptions actualOptions = options == null ? new WaitForSelectorOptions() : options;
        return waitForSelector(selector, actualOptions.timeoutMillis(DEFAULT_SELECTOR_TIMEOUT_MILLIS));
    }

    /**
     * Waits for selector.
     *
     * @param selector      selector text
     * @param timeoutMillis timeout in milliseconds
     * @return wait for selector value
     */
    public Optional<CdpElementHandle> waitForSelector(String selector, long timeoutMillis) {
        long deadline = timeoutMillis <= Normal._0 ? Long.MAX_VALUE
                : System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (true) {
            Optional<CdpElementHandle> element = $(selector);
            if (element.isPresent()) {
                return element;
            }
            if (System.nanoTime() >= deadline) {
                return Optional.empty();
            }
            sleepSelectorPollInterval();
        }
    }

    /**
     * Handles scroll into view.
     */
    public void scrollIntoView() {
        assertConnectedElement();
        try {
            Awaitable.await(
                    session.send("DOM.scrollIntoViewIfNeeded", Map.of("objectId", objectId())),
                    "Failed to scroll element through CDP.");
        } catch (InternalException ex) {
            callFunctionOn(
                    "function(){this.scrollIntoView({block:'center',inline:'center',behavior:'instant'});}",
                    false);
        }
    }

    /**
     * Handles scroll into view if needed.
     */
    void scrollIntoViewIfNeeded() {
        if (isIntersectingViewport()) {
            return;
        }
        scrollIntoView();
    }

    /**
     * Returns the clickable point.
     *
     * @return clickable point value
     */
    public DragPoint clickablePoint() {
        Rectangle rectangle = nonEmptyVisibleRectangle();
        return new DragPoint(rectangle.x() + rectangle.width() / 2.0, rectangle.y() + rectangle.height() / 2.0);
    }

    /**
     * Returns the rectangle.
     *
     * @return rectangle value
     */
    public Rectangle rectangle() {
        List<CdpPayload> content = rectangleModel().get("model").get("content").elements();
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < content.size(); index += 2) {
            double x = content.get(index).asDouble();
            double y = content.get(index + 1).asDouble();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Returns the rectangle model.
     *
     * @return rectangle model value
     */
    public CdpPayload rectangleModel() {
        return Awaitable.await(
                session.send("DOM.getBoxModel", Map.of("objectId", objectId())),
                "Failed to read element rectangle model.");
    }

    /**
     * Returns the screenshot.
     *
     * @param format format value
     * @return screenshot value
     */
    public byte[] screenshot(String format) {
        ScreenshotOptions options = new ScreenshotOptions();
        options.setType(format);
        return screenshot(options);
    }

    /**
     * Returns the screenshot.
     *
     * @param options operation options
     * @return screenshot value
     */
    public byte[] screenshot(ScreenshotOptions options) {
        ScreenshotOptions actual = PageDefaults.copyScreenshotOptions(options);
        if (actual.isScrollIntoView()) {
            scrollIntoViewIfNeeded();
        }
        Rectangle rectangle = nonEmptyVisibleRectangle();
        actual.setClip(elementScreenshotClip(rectangle));
        CdpPayload result = Awaitable
                .await(session.send("Page.captureScreenshot", actual.toMap()), "Element screenshot failed.");
        String base64 = PayloadReader.text(result.get("data"));
        byte[] imageBytes = Base64.decode(base64);
        writePath(actual.getPath(), imageBytes);
        return "base64".equalsIgnoreCase(actual.getEncoding()) ? ByteKit.toBytes(base64, Charset.UTF_8) : imageBytes;
    }

    /**
     * Creates an element screenshot clip.
     *
     * @param rectangle rectangle
     * @return clip
     */
    private Map<String, Object> elementScreenshotClip(Rectangle rectangle) {
        double x = rectangle.x();
        double y = rectangle.y();
        CdpPayload metrics = Awaitable
                .await(session.send("Page.getLayoutMetrics"), "Failed to read element screenshot viewport.");
        CdpPayload viewport = metrics.get("cssVisualViewport");
        if (viewport.isNull()) {
            viewport = metrics.get("visualViewport");
        }
        x += metricNumber(viewport, "pageX", metricNumber(viewport, "offsetX", 0));
        y += metricNumber(viewport, "pageY", metricNumber(viewport, "offsetY", 0));
        Map<String, Object> clip = new LinkedHashMap<>();
        clip.put("x", x);
        clip.put("y", y);
        clip.put("width", rectangle.width());
        clip.put("height", rectangle.height());
        clip.put("scale", 1);
        return clip;
    }

    /**
     * Writes screenshot bytes to a path.
     *
     * @param path  path
     * @param bytes bytes
     */
    private void writePath(Path path, byte[] bytes) {
        if (path == null) {
            return;
        }
        try {
            Files.write(path, bytes);
        } catch (Exception ex) {
            throw new InternalException("Failed to write element screenshot: " + path, ex);
        }
    }

    /**
     * Reads a numeric metric value.
     *
     * @param payload  payload
     * @param key      key
     * @param fallback fallback
     * @return value
     */
    private double metricNumber(CdpPayload payload, String key, double fallback) {
        if (payload == null || payload.isNull() || payload.get(key).isNull()) {
            return fallback;
        }
        Object raw = payload.get(key).raw();
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw instanceof String text && StringKit.isNotBlank(text)) {
            return Double.parseDouble(text);
        }
        return fallback;
    }

    /**
     * Handles click.
     */
    public void click() {
        DragPoint point = clickablePoint();
        new CdpMouse(new CdpInput(session)).click(point.x(), point.y());
    }

    /**
     * Handles click.
     *
     * @param options operation options
     */
    public void click(ClickOptions options) {
        ClickOptions actualOptions = options == null ? new ClickOptions() : options;
        DragPoint point = clickPoint(actualOptions);
        CdpMouse mouse = new CdpMouse(new CdpInput(session));
        int count = Math.max(1, actualOptions.getCount());
        String button = StringKit.isBlank(actualOptions.getButton()) ? "left" : actualOptions.getButton();
        mouse.move(point.x(), point.y());
        for (int index = 1; index <= count; index++) {
            mouse.down(button, index);
            ThreadKit.sleep(actualOptions.getDelay());
            mouse.up(button, index);
        }
    }

    /**
     * Resolves click point.
     *
     * @param options options
     * @return click point
     */
    private DragPoint clickPoint(ClickOptions options) {
        if (options == null || !options.hasOffset()) {
            return clickablePoint();
        }
        Rectangle rectangle = rectangle();
        double offsetX = options.getOffsetX() == null ? 0D : options.getOffsetX();
        double offsetY = options.getOffsetY() == null ? 0D : options.getOffsetY();
        return new DragPoint(rectangle.x() + offsetX, rectangle.y() + offsetY);
    }

    /**
     * Handles hover.
     */
    public void hover() {
        DragPoint point = clickablePoint();
        new CdpMouse(new CdpInput(session)).move(point.x(), point.y());
    }

    /**
     * Handles tap.
     */
    public void tap() {
        DragPoint point = clickablePoint();
        new CdpTouchscreen(new CdpInput(session)).tap(point.x(), point.y());
    }

    /**
     * Converts this value to touch start.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> touchStart() {
        DragPoint point = clickablePoint();
        return new CdpTouchscreen(new CdpInput(session)).touchStart(point.x(), point.y());
    }

    /**
     * Converts this value to touch move.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> touchMove() {
        DragPoint point = clickablePoint();
        return new CdpTouchscreen(new CdpInput(session)).touchMove(point.x(), point.y());
    }

    /**
     * Converts this value to touch end.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> touchEnd() {
        return new CdpTouchscreen(new CdpInput(session)).touchEnd();
    }

    /**
     * Handles type.
     *
     * @param text text to use
     */
    public void type(String text) {
        focus();
        new CdpKeyboard(new CdpInput(session)).type(text);
    }

    /**
     * Handles type.
     *
     * @param text    text to use
     * @param options operation options
     */
    public void type(String text, KeyboardTypeOptions options) {
        focus();
        new CdpKeyboard(new CdpInput(session)).type(text, options == null ? 0L : options.getDelay());
    }

    /**
     * Handles press.
     *
     * @param key key value
     */
    public void press(String key) {
        focus();
        new CdpKeyboard(new CdpInput(session)).press(key);
    }

    /**
     * Handles press.
     *
     * @param key     key value
     * @param options operation options
     */
    public void press(String key, KeyboardTypeOptions options) {
        focus();
        new CdpKeyboard(new CdpInput(session)).press(key, options == null ? 0L : options.getDelay());
    }

    /**
     * Returns the upload file.
     *
     * @param files files to use
     * @return completion future
     */
    public CompletableFuture<CdpPayload> uploadFile(String... files) {
        List<String> actualFiles = resolveUploadFiles(files);
        boolean isMultiple = Boolean.TRUE.equals(evaluate("function(){return !!this.multiple;}"));
        if (actualFiles.size() > 1 && !isMultiple) {
            throw new InternalException("Invalid multiple file upload target: <input type=file multiple> required.");
        }
        if (actualFiles.isEmpty()) {
            callFunctionOn(
                    "function(){this.files=new DataTransfer().files;"
                            + "this.dispatchEvent(new Event('input',{bubbles:true,composed:true}));"
                            + "this.dispatchEvent(new Event('change',{bubbles:true}));}",
                    false);
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        int backendNodeId = backendNodeId();
        return session.send(
                "DOM.setFileInputFiles",
                Map.of("objectId", objectId(), "files", actualFiles, "backendNodeId", backendNodeId));
    }

    /**
     * Returns whether visible is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isVisible() {
        CdpPayload result = callFunctionOn(
                "function(){const style=window.getComputedStyle(this);" + "const rect=this.getBoundingClientRect();"
                        + "return !!(rect.width||rect.height)&&style.visibility!=='hidden'&&style.display!=='none';}",
                true);
        return result.get("value").asBoolean();
    }

    /**
     * Returns whether hidden is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isHidden() {
        return !isVisible();
    }

    /**
     * Returns whether intersecting viewport is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isIntersectingViewport() {
        CdpPayload result = callFunctionOn(
                "function(){const r=this.getBoundingClientRect();return r.width>0&&r.height>0&&r.bottom>0&&r.right>0&&r.top<(window.innerHeight||document.documentElement.clientHeight)&&r.left<(window.innerWidth||document.documentElement.clientWidth);}",
                true);
        return result.get("value").asBoolean();
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
     * Returns the query AX tree.
     *
     * @param name name to use
     * @param role role value
     * @return query AX tree value
     */
    public CdpPayload queryAXTree(String name, String role) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("objectId", objectId());
        if (name != null) {
            params.put("accessibleName", name);
        }
        if (role != null) {
            params.put("role", role);
        }
        return Awaitable
                .await(session.send("Accessibility.queryAXTree", params), "Failed to query accessibility tree.");
    }

    /**
     * Returns the query AX element backend node ids.
     *
     * @param name name to use
     * @param role role value
     * @return values
     */
    List<Integer> queryAXElementBackendNodeIds(String name, String role) {
        CdpPayload nodes = queryAXTree(name, role).get("nodes");
        if (!nodes.isArray()) {
            return List.of();
        }
        List<Integer> backendNodeIds = new ArrayList<>();
        for (CdpPayload node : nodes.elements()) {
            if (isElementAccessibilityNode(node)) {
                backendNodeIds.add(node.get("backendDOMNodeId").asInt());
            }
        }
        return List.copyOf(backendNodeIds);
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
        Map<String, Object> actualData = data == null ? Map.of() : data;
        if (actualData.containsKey("creditCard") || actualData.containsKey("address")) {
            Object card = actualData.get("creditCard");
            Object address = actualData.get("address");
            if (card != null) {
                params.put("card", card);
            }
            if (address != null) {
                params.put("address", address);
            }
        } else {
            params.put("card", actualData);
        }
        return session.send("Autofill.trigger", params);
    }

    /**
     * Returns the backend node ID.
     *
     * @return backend node ID value
     */
    public int backendNodeId() {
        return PayloadReader.backendNodeId(backendNodeId, this::describeNode, value -> backendNodeId = value);
    }

    /**
     * Returns the select.
     *
     * @param values values value
     * @return values
     */
    public List<String> select(String... values) {
        List<String> selectedValues = values == null ? List.of() : List.of(values);
        CdpPayload result = callFunctionOn(
                "function(values){if(this.nodeName.toLowerCase()!=='select'){return [];}const set=new Set(values);const selected=[];for(const option of this.options){option.selected=set.has(option.value);if(option.selected){selected.push(option.value);}}this.dispatchEvent(new Event('input',{bubbles:true}));this.dispatchEvent(new Event('change',{bubbles:true}));return selected;}",
                List.of(Map.of("value", selectedValues)),
                true);
        CdpPayload value = result.get("value");
        if (!value.isArray()) {
            return List.of();
        }
        return value.elements().stream().map(PayloadReader::text).toList();
    }

    /**
     * Handles drag and drop.
     *
     * @param target target object
     */
    public void dragAndDrop(CdpElementHandle target) {
        DragPoint start = clickablePoint();
        DragPoint end = target.clickablePoint();
        new CdpMouse(new CdpInput(session)).dragAndDrop(start.x(), start.y(), end.x(), end.y());
    }

    /**
     * Handles drag and drop.
     *
     * @param target target object
     */
    public void dragAndDrop(Element target) {
        if (target instanceof CdpElementHandle handle) {
            dragAndDrop(handle);
            return;
        }
        throw new InternalException("dragAndDrop target must be an CdpElementHandle implementation.");
    }

    /**
     * Dispatches a drag input action.
     *
     * @param target target object
     * @return drag value
     */
    public DragData drag(DragPoint target) {
        DragPoint start = clickablePoint();
        return new CdpMouse(new CdpInput(session)).drag(start.x(), start.y(), target.x(), target.y());
    }

    /**
     * Returns the drag enter.
     *
     * @param data data to use
     * @return completion future
     */
    public CompletableFuture<CdpPayload> dragEnter(DragData data) {
        DragPoint point = clickablePoint();
        return new CdpMouse(new CdpInput(session)).dragEnter(point.x(), point.y(), Assert.notNull(data, "data"));
    }

    /**
     * Returns the drag over.
     *
     * @param data data to use
     * @return completion future
     */
    public CompletableFuture<CdpPayload> dragOver(DragData data) {
        DragPoint point = clickablePoint();
        return new CdpMouse(new CdpInput(session)).dragOver(point.x(), point.y(), Assert.notNull(data, "data"));
    }

    /**
     * Returns the drop.
     *
     * @param data data to use
     * @return completion future
     */
    public CompletableFuture<CdpPayload> drop(DragData data) {
        DragPoint point = clickablePoint();
        return new CdpMouse(new CdpInput(session)).drop(point.x(), point.y(), Assert.notNull(data, "data"));
    }

    /**
     * Handles focus.
     */
    public void focus() {
        callFunctionOn("function(){this.focus();}", false);
    }

    /**
     * Returns the non empty visible rectangle.
     *
     * @return non empty visible rectangle value
     */
    private Rectangle nonEmptyVisibleRectangle() {
        Rectangle rectangle = rectangle();
        if (rectangle.width() == Normal._0) {
            throw new InternalException("Node has 0 width.");
        }
        if (rectangle.height() == Normal._0) {
            throw new InternalException("Node has 0 height.");
        }
        return rectangle;
    }

    /**
     * Asserts the connected element condition.
     */
    private void assertConnectedElement() {
        CdpPayload result = callFunctionOn(
                "function(){if(!this.isConnected){return 'Node is detached from document';}"
                        + "if(this.nodeType!==Node.ELEMENT_NODE){return 'Node is not of type HTMLElement';}"
                        + "return '';}",
                true);
        String error = PayloadReader.text(result.get("value"));
        if (StringKit.isNotBlank(error)) {
            throw new InternalException(error);
        }
    }

    /**
     * Returns the resolve upload files.
     *
     * @param files files to use
     * @return values
     */
    private List<String> resolveUploadFiles(String... files) {
        if (files == null || files.length == Normal._0) {
            return List.of();
        }
        return Arrays.stream(files).map(this::resolveUploadFile).toList();
    }

    /**
     * Returns the resolve upload file.
     *
     * @param file file to use
     * @return resolve upload file value
     */
    private String resolveUploadFile(String file) {
        if (StringKit.isBlank(file) || FileKit.isAbsolutePath(file)) {
            return file == null ? Normal.EMPTY : file;
        }
        return Path.of(file).toAbsolutePath().normalize().toString();
    }

    /**
     * Returns whether element accessibility node is enabled.
     *
     * @param node node value
     * @return {@code true} when the condition matches
     */
    private boolean isElementAccessibilityNode(CdpPayload node) {
        if (node.get("ignored").raw() instanceof Boolean ignored && ignored) {
            return false;
        }
        CdpPayload role = node.get("role").get("value");
        if (role.isNull() || NON_ELEMENT_NODE_ROLES.contains(PayloadReader.text(role))) {
            return false;
        }
        return !node.get("backendDOMNodeId").isNull();
    }

    /**
     * Returns the describe node.
     *
     * @return describe node value
     */
    private CdpPayload describeNode() {
        return Awaitable
                .await(session.send("DOM.describeNode", Map.of("objectId", objectId())), "Failed to read DOM node.");
    }

    /**
     * Returns the call function on.
     *
     * @param functionDeclaration function declaration value
     * @param returnByValue       whether the result should be returned by value
     * @return call function on value
     */
    private CdpPayload callFunctionOn(String functionDeclaration, boolean returnByValue) {
        return callFunctionOn(functionDeclaration, List.of(), returnByValue);
    }

    /**
     * Returns the call function on.
     *
     * @param functionDeclaration function declaration value
     * @param arguments           arguments value
     * @param returnByValue       whether the result should be returned by value
     * @return call function on value
     */
    private CdpPayload callFunctionOn(
            String functionDeclaration,
            List<Map<String, Object>> arguments,
            boolean returnByValue) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("objectId", objectId());
        params.put("functionDeclaration", functionDeclaration);
        params.put("returnByValue", returnByValue);
        params.put("awaitPromise", true);
        if (arguments != null && !arguments.isEmpty()) {
            params.put("arguments", arguments);
        }
        return Awaitable.await(session.send("Runtime.callFunctionOn", params), "Failed to call element function.")
                .get("result");
    }

    /**
     * Returns the object ID.
     *
     * @return object ID value
     */
    private String objectId() {
        String objectId = PayloadReader.text(remoteObject().get("objectId"));
        if (StringKit.isBlank(objectId)) {
            throw new InternalException("CdpElementHandle is missing remote objectId.");
        }
        return objectId;
    }

    /**
     * Returns the element from remote.
     *
     * @param remote remote value
     * @return optional value
     */
    private Optional<CdpElementHandle> elementFromRemote(CdpPayload remote) {
        if (remote == null || remote.isNull()) {
            return Optional.empty();
        }
        if (!remote.get("objectId").isNull()) {
            return Optional.of(new CdpElementHandle(remote, session));
        }
        return Optional.empty();
    }

    /**
     * Returns the elements from remote value.
     *
     * @param remote remote value
     * @return values
     */
    private List<CdpElementHandle> elementsFromRemoteValue(CdpPayload remote) {
        CdpPayload value = remote.get("value");
        if (!value.isArray()) {
            return List.of();
        }
        List<CdpElementHandle> elements = new ArrayList<>();
        for (CdpPayload item : value.elements()) {
            String objectId = PayloadReader.text(item.get("objectId"));
            if (StringKit.isNotBlank(objectId)) {
                elements.add(new CdpElementHandle(CdpPayload.of(Map.of("objectId", objectId)), session));
            }
        }
        return List.copyOf(elements);
    }

    /**
     * Reads element handles from a remote JavaScript array.
     *
     * @param remote remote array object
     * @return values
     */
    private List<CdpElementHandle> elementsFromArrayRemoteObject(CdpPayload remote) {
        String arrayObjectId = PayloadReader.text(remote.get("objectId"));
        if (StringKit.isBlank(arrayObjectId)) {
            return elementsFromRemoteValue(remote);
        }
        CdpPayload result = Awaitable.await(
                session.send("Runtime.getProperties", Map.of("objectId", arrayObjectId, "ownProperties", true)),
                "Failed to read queried element handles.");
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
     * Handles sleep selector poll interval.
     */
    private void sleepSelectorPollInterval() {
        if (!ThreadKit.sleep(SELECTOR_POLL_INTERVAL_MILLIS)) {
            throw new InternalException("Interrupted while waiting for selector.");
        }
    }

    /**
     * Provides internal element collaboration without creating a standalone helper type.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class Internal {

        /**
         * Creates no Internal instance.
         */
        private Internal() {
            // No initialization required.
        }

        /**
         * Returns the CDP client attached to an element handle.
         *
         * @param element element handle
         * @return CDP client
         */
        public static CDPSession client(CdpElementHandle element) {
            return Assert.notNull(element, "element").client();
        }

        /**
         * Queries accessibility backend node ids for an element.
         *
         * @param element element handle
         * @param name    accessible name
         * @param role    accessible role
         * @return backend node ids
         */
        public static List<Integer> queryAXElementBackendNodeIds(CdpElementHandle element, String name, String role) {
            return Assert.notNull(element, "element").queryAXElementBackendNodeIds(name, role);
        }
    }

}
