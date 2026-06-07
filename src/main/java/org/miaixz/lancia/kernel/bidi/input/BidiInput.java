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
package org.miaixz.lancia.kernel.bidi.input;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.kernel.Keyboard;
import org.miaixz.lancia.kernel.Mouse;
import org.miaixz.lancia.kernel.Pointer;
import org.miaixz.lancia.kernel.Touch;
import org.miaixz.lancia.kernel.bidi.accessor.BidiSession;
import org.miaixz.lancia.kernel.bidi.page.BidiPage;
import org.miaixz.lancia.kernel.bidi.protocol.BidiSerializer;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.nimble.input.DragData;
import org.miaixz.lancia.nimble.input.DragPoint;
import org.miaixz.lancia.options.KeyboardTypeOptions;
import org.miaixz.lancia.options.MouseClickOptions;
import org.miaixz.lancia.options.MouseMoveOptions;
import org.miaixz.lancia.options.MouseOptions;
import org.miaixz.lancia.options.MouseWheelOptions;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.input.InputAction;

/**
 * Sends keyboard, mouse, wheel, and touch actions through WebDriver BiDi.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiInput {

    /**
     * Shared constant for mouse ID.
     */
    private static final String MOUSE_ID = "__lancia_mouse";
    /**
     * Shared constant for keyboard ID.
     */
    private static final String KEYBOARD_ID = "__lancia_keyboard";
    /**
     * Shared constant for wheel ID.
     */
    private static final String WHEEL_ID = "__lancia_wheel";
    /**
     * Shared constant for finger ID.
     */
    private static final String FINGER_ID = "__lancia_finger";
    /**
     * Current page.
     */
    private final BidiPage page;
    /**
     * Current keyboard.
     */
    private final BidiKeyboard keyboard;
    /**
     * Current mouse.
     */
    private final BidiMouse mouse;
    /**
     * Current touchscreen.
     */
    private final Touchscreen touchscreen;

    /**
     * Creates a bidi input.
     *
     * @param page page instance
     */
    public BidiInput(BidiPage page) {
        this.page = Assert.notNull(page, "page");
        this.keyboard = new BidiKeyboard(this);
        this.mouse = new BidiMouse(this);
        this.touchscreen = new Touchscreen(this);
        Logger.debug(false, "Input", "BiDi input initialized: contextId={}", page.contextId());
    }

    /**
     * Performs actions.
     *
     * @param actions actions
     * @return perform actions value
     */
    public CompletableFuture<CdpPayload> performActions(List<Map<String, Object>> actions) {
        Logger.debug(
                true,
                "Input",
                "BiDi input actions requested: contextId={}, sources={}",
                page.contextId(),
                actions == null ? Normal._0 : actions.size());
        return session().send("input.performActions", Map.of("context", page.contextId(), "actions", actions));
    }

    /**
     * Releases actions.
     *
     * @return release actions value
     */
    public CompletableFuture<CdpPayload> releaseActions() {
        Logger.debug(true, "Input", "BiDi input release requested: contextId={}", page.contextId());
        return session().send("input.releaseActions", Map.of("context", page.contextId()));
    }

    /**
     * Returns the keyboard input controller.
     *
     * @return keyboard input controller
     */
    public BidiKeyboard keyboard() {
        return keyboard;
    }

    /**
     * Returns the mouse input controller.
     *
     * @return mouse input controller
     */
    public BidiMouse mouse() {
        return mouse;
    }

    /**
     * Returns the touchscreen input controller.
     *
     * @return touchscreen
     */
    public Touchscreen touchscreen() {
        return touchscreen;
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
     * Returns the source.
     *
     * @param type    type to use
     * @param id      identifier
     * @param actions actions value
     * @return mapped values
     */
    private static Map<String, Object> source(String type, String id, List<Map<String, Object>> actions) {
        return Map.of("type", type, "id", id, "actions", actions);
    }

    /**
     * Converts this value to touch source.
     *
     * @param id      identifier
     * @param actions actions value
     * @return mapped values
     */
    private static Map<String, Object> touchSource(String id, List<Map<String, Object>> actions) {
        return Map.of("type", "pointer", "id", id, "parameters", Map.of("pointerType", "touch"), "actions", actions);
    }

    /**
     * Sends keyboard input actions.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class BidiKeyboard implements Keyboard {

        /**
         * Current input.
         */
        private final BidiInput input;

        /**
         * Creates an instance.
         *
         * @param input input source
         */
        private BidiKeyboard(BidiInput input) {
            this.input = input;
        }

        /**
         * Dispatches a down input action.
         *
         * @param key key value
         * @return completion future
         */
        public CompletableFuture<CdpPayload> down(String key) {
            Logger.debug(true, "Input", "BiDi keyboard down requested: key={}", key);
            return input.performActions(List.of(source("key", KEYBOARD_ID, List.of(keyAction("keyDown", key)))));
        }

        /**
         * Dispatches a down input action.
         *
         * @param key     key value
         * @param options operation options
         * @return completion future
         */
        public CompletableFuture<CdpPayload> down(String key, KeyboardTypeOptions options) {
            return InputAction.down(key, options, this::down);
        }

        /**
         * Dispatches a down input action.
         *
         * @param key      key value
         * @param text     text to use
         * @param commands commands to send
         * @return completion future
         */
        public CompletableFuture<CdpPayload> down(String key, String text, List<String> commands) {
            return down(text == null || text.isEmpty() ? key : text);
        }

        /**
         * Dispatches an up input action.
         *
         * @param key key value
         * @return completion future
         */
        public CompletableFuture<CdpPayload> up(String key) {
            Logger.debug(false, "Input", "BiDi keyboard up requested: key={}", key);
            return input.performActions(List.of(source("key", KEYBOARD_ID, List.of(keyAction("keyUp", key)))));
        }

        /**
         * Dispatches a press input action.
         *
         * @param key key value
         */
        public void press(String key) {
            Awaitable.await(pressAsync(key), "BiDi keyboard press failed.", 5_000L);
        }

        /**
         * Dispatches a press input action.
         *
         * @param key         key value
         * @param delayMillis delay in milliseconds
         * @return completion future
         */
        public CompletableFuture<CdpPayload> pressAsync(String key, long delayMillis) {
            Logger.debug(true, "Input", "BiDi keyboard press requested: key={}, delayMillis={}", key, delayMillis);
            List<Map<String, Object>> actions = new ArrayList<>();
            actions.add(keyAction("keyDown", key));
            if (delayMillis > 0) {
                actions.add(Map.of("type", "pause", "duration", delayMillis));
            }
            actions.add(keyAction("keyUp", key));
            return input.performActions(List.of(source("key", KEYBOARD_ID, actions)));
        }

        /**
         * Dispatches a press input action.
         *
         * @param key     key value
         * @param options operation options
         */
        public void press(String key, KeyboardTypeOptions options) {
            Awaitable.await(pressAsync(key, options), "BiDi keyboard press failed.", 5_000L);
        }

        /**
         * Dispatches a press input action.
         *
         * @param key key value
         * @return completion future
         */
        public CompletableFuture<CdpPayload> pressAsync(String key) {
            return pressAsync(key, 0L);
        }

        /**
         * Dispatches a press input action.
         *
         * @param key     key value
         * @param options operation options
         * @return completion future
         */
        public CompletableFuture<CdpPayload> pressAsync(String key, KeyboardTypeOptions options) {
            KeyboardTypeOptions actualOptions = options == null ? new KeyboardTypeOptions() : options;
            String actualKey = actualOptions.getText() == null || actualOptions.getText().isEmpty() ? key
                    : actualOptions.getText();
            return pressAsync(actualKey, actualOptions.getDelay());
        }

        /**
         * Returns the type.
         *
         * @param text text to use
         */
        public void type(String text) {
            Awaitable.await(typeAsync(text), "BiDi keyboard type failed.", 5_000L);
        }

        /**
         * Returns the type.
         *
         * @param text        text to use
         * @param delayMillis delay in milliseconds
         * @return completion future
         */
        public CompletableFuture<CdpPayload> typeAsync(String text, long delayMillis) {
            Logger.debug(
                    true,
                    "Input",
                    "BiDi keyboard type requested: chars={}, delayMillis={}",
                    text == null ? Normal._0 : text.length(),
                    delayMillis);
            List<Map<String, Object>> actions = new ArrayList<>();
            String actualText = text == null ? Normal.EMPTY : text;
            actualText.codePoints().forEach(codePoint -> {
                String value = keyValue(new String(Character.toChars(codePoint)));
                actions.add(Map.of("type", "keyDown", "value", value));
                if (delayMillis > 0) {
                    actions.add(Map.of("type", "pause", "duration", delayMillis));
                }
                actions.add(Map.of("type", "keyUp", "value", value));
            });
            return input.performActions(List.of(source("key", KEYBOARD_ID, actions)));
        }

        /**
         * Returns the type.
         *
         * @param text    text to use
         * @param options operation options
         */
        public void type(String text, KeyboardTypeOptions options) {
            Awaitable.await(typeAsync(text, options), "BiDi keyboard type failed.", 5_000L);
        }

        /**
         * Returns the type.
         *
         * @param text text to use
         * @return completion future
         */
        public CompletableFuture<CdpPayload> typeAsync(String text) {
            return typeAsync(text, 0L);
        }

        /**
         * Returns the type.
         *
         * @param text    text to use
         * @param options operation options
         * @return completion future
         */
        public CompletableFuture<CdpPayload> typeAsync(String text, KeyboardTypeOptions options) {
            KeyboardTypeOptions actualOptions = options == null ? new KeyboardTypeOptions() : options;
            return typeAsync(text, actualOptions.getDelay());
        }

        /**
         * Returns the send character.
         *
         * @param character character value
         * @return completion future
         */
        public CompletableFuture<CdpPayload> sendCharacter(String character) {
            String actual = character == null ? Normal.EMPTY : character;
            if (actual.codePointCount(0, actual.length()) > 1) {
                CompletableFuture<CdpPayload> rejected = new CompletableFuture<>();
                rejected.completeExceptionally(
                        new InternalException("BiDi sendCharacter can only send one character."));
                Logger.warn(false, "Input", "BiDi keyboard character rejected: chars={}", actual.length());
                return rejected;
            }
            Logger.debug(true, "Input", "BiDi keyboard character requested: chars={}", actual.length());
            return input.session().send(
                    "script.callFunction",
                    Map.of(
                            "target",
                            Map.of("context", input.page.contextId()),
                            "awaitPromise",
                            true,
                            "functionDeclaration",
                            "(char)=>document.execCommand('insertText',false,char)",
                            "arguments",
                            List.of(Map.of("type", "string", "value", actual))));
        }

        /**
         * Returns the active modifier mask.
         *
         * @return modifier mask
         */
        public int modifiers() {
            return 0;
        }

        /**
         * Returns the key action.
         *
         * @param type type to use
         * @param key  key value
         * @return mapped values
         */
        private static Map<String, Object> keyAction(String type, String key) {
            return Map.of("type", type, "value", keyValue(key));
        }

        /**
         * Returns the key value.
         *
         * @param key key value
         * @return key value
         */
        private static String keyValue(String key) {
            String actual = key == null ? Normal.EMPTY : key;
            if (Symbol.CR.equals(actual) || Symbol.LF.equals(actual)) {
                actual = "Enter";
            }
            if (actual.codePointCount(0, actual.length()) == 1) {
                return actual;
            }
            return switch (actual) {
                case "Enter" -> "\uE007";
                case "Tab" -> "\uE004";
                case "Backspace" -> "\uE003";
                case "Escape" -> "\uE00C";
                case "ArrowLeft" -> "\uE012";
                case "ArrowUp" -> "\uE013";
                case "ArrowRight" -> "\uE014";
                case "ArrowDown" -> "\uE015";
                case "Delete" -> "\uE017";
                case "Shift", "ShiftLeft" -> "\uE008";
                case "Control", "ControlLeft" -> "\uE009";
                case "Alt", "AltLeft" -> "\uE00A";
                case "Meta", "MetaLeft" -> "\uE03D";
                default -> actual;
            };
        }
    }

    /**
     * Sends mouse input actions.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class BidiMouse implements Mouse {

        /**
         * Current input.
         */
        private final BidiInput input;
        /**
         * Current x.
         */
        private int x;
        /**
         * Current y.
         */
        private int y;

        /**
         * Creates an instance.
         *
         * @param input input source
         */
        private BidiMouse(BidiInput input) {
            this.input = input;
        }

        /**
         * Returns the reset.
         */
        public void reset() {
            Awaitable.await(resetAsync(), "BiDi mouse reset failed.", 5_000L);
        }

        /**
         * Returns the reset.
         *
         * @return completion future
         */
        public CompletableFuture<CdpPayload> resetAsync() {
            x = 0;
            y = 0;
            Logger.debug(true, "Input", "BiDi mouse reset requested.");
            return input.releaseActions();
        }

        /**
         * Dispatches a move input action.
         *
         * @param x x value
         * @param y y value
         * @return completion future
         */
        public CompletableFuture<CdpPayload> move(double x, double y) {
            return move(x, y, 0);
        }

        /**
         * Dispatches a move input action.
         *
         * @param x       x value
         * @param y       y value
         * @param options operation options
         * @return completion future
         */
        public CompletableFuture<CdpPayload> move(double x, double y, MouseMoveOptions options) {
            return InputAction.move(x, y, options, this::move);
        }

        /**
         * Dispatches a move input action.
         *
         * @param x     x value
         * @param y     y value
         * @param steps steps value
         * @return completion future
         */
        public CompletableFuture<CdpPayload> move(double x, double y, int steps) {
            Logger.debug(
                    true,
                    "Input",
                    "BiDi mouse move requested: from=({},{}), to=({},{}), steps={}",
                    this.x,
                    this.y,
                    x,
                    y,
                    steps);
            int targetX = (int) Math.round(x);
            int targetY = (int) Math.round(y);
            List<Map<String, Object>> actions = new ArrayList<>();
            int safeSteps = Math.max(0, steps);
            for (int index = 0; index < safeSteps; index++) {
                actions.add(
                        pointerMove(
                                this.x + (targetX - this.x) * index / Math.max(1, safeSteps),
                                this.y + (targetY - this.y) * index / Math.max(1, safeSteps)));
            }
            actions.add(pointerMove(targetX, targetY));
            this.x = targetX;
            this.y = targetY;
            return input.performActions(List.of(source("pointer", MOUSE_ID, actions)));
        }

        /**
         * Dispatches a down input action.
         *
         * @param button button value
         * @return completion future
         */
        public CompletableFuture<CdpPayload> down(String button) {
            Logger.debug(true, "Input", "BiDi mouse down requested: button={}, x={}, y={}", button, x, y);
            return input.performActions(
                    List.of(source("pointer", MOUSE_ID, List.of(pointerButton("pointerDown", button)))));
        }

        /**
         * Dispatches a down input action.
         *
         * @param options operation options
         * @return completion future
         */
        public CompletableFuture<CdpPayload> down(MouseClickOptions options) {
            MouseClickOptions actualOptions = options == null ? new MouseClickOptions() : options;
            return down(actualOptions.getButton());
        }

        /**
         * Dispatches a down input action.
         *
         * @param button     button value
         * @param clickCount click count value
         * @return completion future
         */
        public CompletableFuture<CdpPayload> down(String button, int clickCount) {
            return down(button);
        }

        /**
         * Dispatches an up input action.
         *
         * @param button button value
         * @return completion future
         */
        public CompletableFuture<CdpPayload> up(String button) {
            Logger.debug(false, "Input", "BiDi mouse up requested: button={}, x={}, y={}", button, x, y);
            return input
                    .performActions(List.of(source("pointer", MOUSE_ID, List.of(pointerButton("pointerUp", button)))));
        }

        /**
         * Dispatches an up input action.
         *
         * @param options operation options
         * @return completion future
         */
        public CompletableFuture<CdpPayload> up(MouseClickOptions options) {
            MouseClickOptions actualOptions = options == null ? new MouseClickOptions() : options;
            return up(actualOptions.getButton());
        }

        /**
         * Dispatches an up input action.
         *
         * @param button     button value
         * @param clickCount click count value
         * @return completion future
         */
        public CompletableFuture<CdpPayload> up(String button, int clickCount) {
            return up(button);
        }

        /**
         * Dispatches a click input action.
         *
         * @param x x value
         * @param y y value
         */
        public void click(double x, double y) {
            Awaitable.await(clickAsync(x, y), "BiDi mouse click failed.", 5_000L);
        }

        /**
         * Dispatches a click input action.
         *
         * @param x           x value
         * @param y           y value
         * @param button      button value
         * @param clickCount  click count value
         * @param delayMillis delay in milliseconds
         * @return completion future
         */
        public CompletableFuture<CdpPayload> clickAsync(
                double x,
                double y,
                String button,
                int clickCount,
                long delayMillis) {
            Logger.debug(
                    true,
                    "Input",
                    "BiDi mouse click requested: button={}, count={}, x={}, y={}",
                    button,
                    clickCount,
                    x,
                    y);
            List<Map<String, Object>> actions = new ArrayList<>();
            actions.add(pointerMove((int) Math.round(x), (int) Math.round(y)));
            int safeCount = Math.max(1, clickCount);
            for (int index = 0; index < safeCount; index++) {
                actions.add(pointerButton("pointerDown", button));
                if (delayMillis > 0) {
                    actions.add(Map.of("type", "pause", "duration", delayMillis));
                }
                actions.add(pointerButton("pointerUp", button));
            }
            this.x = (int) Math.round(x);
            this.y = (int) Math.round(y);
            return input.performActions(List.of(source("pointer", MOUSE_ID, actions)));
        }

        /**
         * Dispatches a click input action.
         *
         * @param x       x value
         * @param y       y value
         * @param options operation options
         */
        public void click(double x, double y, MouseClickOptions options) {
            Awaitable.await(clickAsync(x, y, options), "BiDi mouse click failed.", 5_000L);
        }

        /**
         * Dispatches a click input action.
         *
         * @param x x value
         * @param y y value
         * @return completion future
         */
        public CompletableFuture<CdpPayload> clickAsync(double x, double y) {
            return clickAsync(x, y, "left", 1, 0L);
        }

        /**
         * Dispatches a click input action.
         *
         * @param x          x value
         * @param y          y value
         * @param button     button value
         * @param clickCount click count value
         */
        public void click(double x, double y, String button, int clickCount) {
            Awaitable.await(clickAsync(x, y, button, clickCount, 0L), "BiDi mouse click failed.", 5_000L);
        }

        /**
         * Dispatches a click input action.
         *
         * @param x       x value
         * @param y       y value
         * @param options operation options
         * @return completion future
         */
        public CompletableFuture<CdpPayload> clickAsync(double x, double y, MouseClickOptions options) {
            MouseClickOptions actualOptions = options == null ? new MouseClickOptions() : options;
            return clickAsync(x, y, actualOptions.getButton(), actualOptions.getCount(), actualOptions.getDelay());
        }

        /**
         * Returns the wheel.
         *
         * @param deltaX delta x value
         * @param deltaY delta y value
         * @return completion future
         */
        public CompletableFuture<CdpPayload> wheel(double deltaX, double deltaY) {
            Logger.debug(
                    true,
                    "Input",
                    "BiDi mouse wheel requested: x={}, y={}, deltaX={}, deltaY={}",
                    x,
                    y,
                    deltaX,
                    deltaY);
            return input.performActions(
                    List.of(
                            source(
                                    "wheel",
                                    WHEEL_ID,
                                    List.of(
                                            Map.of(
                                                    "type",
                                                    "scroll",
                                                    "x",
                                                    x,
                                                    "y",
                                                    y,
                                                    "deltaX",
                                                    deltaX,
                                                    "deltaY",
                                                    deltaY)))));
        }

        /**
         * Returns the wheel.
         *
         * @param options operation options
         * @return completion future
         */
        public CompletableFuture<CdpPayload> wheel(MouseWheelOptions options) {
            return InputAction.wheel(options, this::wheel);
        }

        /**
         * Dispatches a drag input action.
         *
         * @param start  start value
         * @param target target object
         * @return completion future
         */
        public CompletableFuture<DragData> drag(DragPoint start, DragPoint target) {
            DragPoint actualStart = Assert.notNull(start, "start");
            DragPoint actualTarget = Assert.notNull(target, "target");
            Logger.debug(
                    true,
                    "Input",
                    "BiDi mouse drag requested: start=({},{}), target=({},{})",
                    actualStart.x(),
                    actualStart.y(),
                    actualTarget.x(),
                    actualTarget.y());
            DragData data = new DragData(actualStart, actualTarget);
            List<Map<String, Object>> actions = new ArrayList<>();
            actions.add(pointerMove(actualStart));
            actions.add(pointerButton("pointerDown", "left"));
            actions.add(pointerMove(actualTarget));
            this.x = (int) Math.round(actualTarget.x());
            this.y = (int) Math.round(actualTarget.y());
            return input.performActions(List.of(source("pointer", MOUSE_ID, actions)))
                    .thenCompose(result -> dispatchDragEvent("dragstart", actualStart, data)).thenApply(result -> data);
        }

        /**
         * Dispatches a drag input action.
         *
         * @param startX start x value
         * @param startY start y value
         * @param endX   end x value
         * @param endY   end y value
         * @return completion future
         */
        public DragData drag(double startX, double startY, double endX, double endY) {
            return Awaitable.await(dragAsync(startX, startY, endX, endY), "BiDi mouse drag failed.", 5_000L);
        }

        /**
         * Dispatches a drag input action.
         *
         * @param startX start x value
         * @param startY start y value
         * @param endX   end x value
         * @param endY   end y value
         * @return completion future
         */
        public CompletableFuture<DragData> dragAsync(double startX, double startY, double endX, double endY) {
            return drag(new DragPoint(startX, startY), new DragPoint(endX, endY));
        }

        /**
         * Returns the drag enter.
         *
         * @param target target object
         * @param data   data to use
         * @return completion future
         */
        public CompletableFuture<Void> dragEnter(DragPoint target, DragData data) {
            Logger.debug(true, "Input", "BiDi mouse dragEnter requested.");
            return dispatchDragEvent("dragenter", target, data);
        }

        /**
         * Returns the drag enter.
         *
         * @param x    x value
         * @param y    y value
         * @param data data to use
         * @return completion future
         */
        public CompletableFuture<CdpPayload> dragEnter(double x, double y, DragData data) {
            return dragEnter(new DragPoint(x, y), data).thenApply(result -> CdpPayload.NULL);
        }

        /**
         * Returns the drag over.
         *
         * @param target target object
         * @param data   data to use
         * @return completion future
         */
        public CompletableFuture<Void> dragOver(DragPoint target, DragData data) {
            Logger.debug(true, "Input", "BiDi mouse dragOver requested.");
            return dispatchDragEvent("dragover", target, data);
        }

        /**
         * Returns the drag over.
         *
         * @param x    x value
         * @param y    y value
         * @param data data to use
         * @return completion future
         */
        public CompletableFuture<CdpPayload> dragOver(double x, double y, DragData data) {
            return dragOver(new DragPoint(x, y), data).thenApply(result -> CdpPayload.NULL);
        }

        /**
         * Returns the drop.
         *
         * @param target target object
         * @param data   data to use
         * @return completion future
         */
        public CompletableFuture<Void> drop(DragPoint target, DragData data) {
            DragPoint actualTarget = Assert.notNull(target, "target");
            Logger.debug(
                    true,
                    "Input",
                    "BiDi mouse drop requested: target=({},{})",
                    actualTarget.x(),
                    actualTarget.y());
            return dispatchDragEvent("drop", actualTarget, data)
                    .thenCompose(
                            result -> input.performActions(
                                    List.of(source("pointer", MOUSE_ID, List.of(pointerButton("pointerUp", "left"))))))
                    .thenApply(result -> null);
        }

        /**
         * Returns the drop.
         *
         * @param x    x value
         * @param y    y value
         * @param data data to use
         * @return completion future
         */
        public CompletableFuture<CdpPayload> drop(double x, double y, DragData data) {
            return drop(new DragPoint(x, y), data).thenApply(result -> CdpPayload.NULL);
        }

        /**
         * Returns the drag and drop.
         *
         * @param startX start x value
         * @param startY start y value
         * @param endX   end x value
         * @param endY   end y value
         */
        public void dragAndDrop(double startX, double startY, double endX, double endY) {
            Awaitable.await(dragAndDropAsync(startX, startY, endX, endY), "BiDi mouse drag and drop failed.", 5_000L);
        }

        /**
         * Returns the drag and drop.
         *
         * @param startX start x value
         * @param startY start y value
         * @param endX   end x value
         * @param endY   end y value
         * @return completion future
         */
        public CompletableFuture<Void> dragAndDropAsync(double startX, double startY, double endX, double endY) {
            return dragAndDrop(new DragPoint(startX, startY), new DragPoint(endX, endY), null);
        }

        /**
         * Returns the drag and drop.
         *
         * @param start   start value
         * @param target  target object
         * @param options operation options
         * @return completion future
         */
        public CompletableFuture<Void> dragAndDrop(DragPoint start, DragPoint target, MouseOptions options) {
            MouseOptions actualOptions = options == null ? new MouseOptions() : options;
            DragPoint actualStart = Assert.notNull(start, "start");
            DragPoint actualTarget = Assert.notNull(target, "target");
            Logger.debug(
                    true,
                    "Input",
                    "BiDi mouse dragAndDrop requested: steps={}, delayMillis={}",
                    actualOptions.steps(),
                    actualOptions.delayMillis());
            return move(actualStart.x(), actualStart.y(), actualOptions.steps())
                    .thenCompose(result -> drag(actualStart, actualTarget)).thenCompose(
                            data -> pause(actualOptions.delayMillis())
                                    .thenCompose(result -> dragEnter(actualTarget, data))
                                    .thenCompose(result -> dragOver(actualTarget, data))
                                    .thenCompose(result -> drop(actualTarget, data)));
        }

        /**
         * Returns the dispatch drag event.
         *
         * @param event  event type
         * @param target target object
         * @param data   data to use
         * @return completion future
         */
        private CompletableFuture<Void> dispatchDragEvent(String event, DragPoint target, DragData data) {
            DragPoint actualTarget = Assert.notNull(target, "target");
            DragData actualData = Assert.notNull(data, "data");
            Map<String, Object> payload = Map
                    .of("event", event, "x", actualTarget.x(), "y", actualTarget.y(), "data", actualData.toMap());
            return input.session()
                    .send(
                            "script.callFunction",
                            Map.of(
                                    "target",
                                    Map.of("context", input.page.contextId()),
                                    "awaitPromise",
                                    true,
                                    "functionDeclaration",
                                    dragEventFunction(),
                                    "arguments",
                                    List.of(BidiSerializer.serialize(payload))))
                    .thenApply(result -> null);
        }

        /**
         * Returns the pause.
         *
         * @param delayMillis delay in milliseconds
         * @return completion future
         */
        private CompletableFuture<CdpPayload> pause(long delayMillis) {
            if (delayMillis <= 0) {
                return CompletableFuture.completedFuture(CdpPayload.NULL);
            }
            return input.performActions(
                    List.of(source("pointer", MOUSE_ID, List.of(Map.of("type", "pause", "duration", delayMillis)))));
        }

        /**
         * Returns the pointer move.
         *
         * @param x x value
         * @param y y value
         * @return mapped values
         */
        private static Map<String, Object> pointerMove(int x, int y) {
            return Map.of("type", "pointerMove", "x", x, "y", y);
        }

        /**
         * Returns the pointer move.
         *
         * @param point point value
         * @return mapped values
         */
        private static Map<String, Object> pointerMove(DragPoint point) {
            return pointerMove((int) Math.round(point.x()), (int) Math.round(point.y()));
        }

        /**
         * Returns the pointer button.
         *
         * @param type   type to use
         * @param button button value
         * @return mapped values
         */
        private static Map<String, Object> pointerButton(String type, String button) {
            return Map.of("type", type, "button", button(button));
        }

        /**
         * Returns the button.
         *
         * @param button button value
         * @return button value
         */
        private static int button(String button) {
            return switch (button == null ? "left" : button) {
                case "middle" -> 1;
                case "right" -> 2;
                case "back" -> 3;
                case "forward" -> 4;
                default -> 0;
            };
        }

        /**
         * Returns the drag event function.
         *
         * @return drag event function value
         */
        private static String dragEventFunction() {
            return "(payload)=>{" + "const point=document.elementFromPoint(payload.x,payload.y);"
                    + "if(!point){return false;}"
                    + "const event=new DragEvent(payload.event,{bubbles:true,cancelable:true,clientX:payload.x,clientY:payload.y});"
                    + "Object.defineProperty(event,'dataTransfer',{value:payload.data});"
                    + "point.dispatchEvent(event);" + "return true;" + Symbol.BRACE_RIGHT;
        }
    }

    /**
     * Defines options for mouse operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class MouseOptions {

        /**
         * Current steps.
         */
        private int steps = 1;
        /**
         * Current delay millis.
         */
        private long delayMillis;

        /**
         * Creates an instance.
         */
        public MouseOptions() {
            // No initialization required.
        }

        /**
         * Returns the steps.
         *
         * @return steps value
         */
        public int steps() {
            return Math.max(1, steps);
        }

        /**
         * Updates steps.
         *
         * @param steps steps value
         */
        public void setSteps(int steps) {
            this.steps = steps;
        }

        /**
         * Returns the delay millis.
         *
         * @return delay millis value
         */
        public long delayMillis() {
            return Math.max(0L, delayMillis);
        }

        /**
         * Updates delay millis.
         *
         * @param delayMillis delay in milliseconds
         */
        public void setDelayMillis(long delayMillis) {
            this.delayMillis = delayMillis;
        }

    }

    /**
     * Sends touchscreen input actions.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class Touchscreen implements Touch {

        /**
         * Current input.
         */
        private final BidiInput input;
        /**
         * Thread-safe ids state.
         */
        private final AtomicInteger ids = new AtomicInteger();
        /**
         * Registered touches values.
         */
        private final List<BidiPointer> touches = new ArrayList<>();

        /**
         * Creates an instance.
         *
         * @param input input source
         */
        private Touchscreen(BidiInput input) {
            this.input = input;
        }

        /**
         * Converts this value to touch start.
         *
         * @param x x value
         * @param y y value
         * @return completion future
         */
        public CompletableFuture<CdpPayload> touchStart(double x, double y) {
            return touchStartPointerAsync(x, y).thenCompose(BidiPointer::started);
        }

        /**
         * Converts this value to touch start.
         *
         * @param x x value
         * @param y y value
         * @return completion future
         */
        public CompletableFuture<BidiPointer> touchStartPointerAsync(double x, double y) {
            BidiPointer handle = new BidiPointer(input, this, FINGER_ID + Symbol.UNDERLINE + ids.incrementAndGet(), x,
                    y);
            Logger.debug(true, "Input", "BiDi touch start requested: id={}, x={}, y={}", handle.id, x, y);
            return handle.start().thenApply(value -> {
                touches.add(handle);
                Logger.debug(false, "Input", "BiDi touch started: id={}, active={}", handle.id, touches.size());
                return handle;
            });
        }

        /**
         * Returns the start touch.
         *
         * @param x x value
         * @param y y value
         * @return completion future
         */
        public BidiPointer startTouch(double x, double y) {
            return Awaitable.await(touchStartPointerAsync(x, y), "BiDi touch start failed.", 5_000L);
        }

        /**
         * Dispatches a tap input action.
         *
         * @param x x value
         * @param y y value
         */
        public void tap(double x, double y) {
            Awaitable.await(tapAsync(x, y), "BiDi touch tap failed.", 5_000L);
        }

        /**
         * Dispatches a tap input action.
         *
         * @param x x value
         * @param y y value
         * @return completion future
         */
        public CompletableFuture<CdpPayload> tapAsync(double x, double y) {
            Logger.debug(true, "Input", "BiDi touch tap requested: x={}, y={}", x, y);
            return touchStartPointerAsync(x, y).thenCompose(BidiPointer::end);
        }

        /**
         * Converts this value to touch move.
         *
         * @param x x value
         * @param y y value
         * @return completion future
         */
        public CompletableFuture<CdpPayload> touchMove(double x, double y) {
            if (touches.isEmpty()) {
                Logger.debug(false, "Input", "BiDi touch move skipped: active=0");
                return CompletableFuture.completedFuture(CdpPayload.NULL);
            }
            Logger.debug(true, "Input", "BiDi touch move requested: x={}, y={}", x, y);
            return touches.get(0).move(x, y);
        }

        /**
         * Converts this value to touch end.
         *
         * @return completion future
         */
        public CompletableFuture<CdpPayload> touchEnd() {
            if (touches.isEmpty()) {
                Logger.debug(false, "Input", "BiDi touch end skipped: active=0");
                return CompletableFuture.completedFuture(CdpPayload.NULL);
            }
            Logger.debug(false, "Input", "BiDi touch end requested: id={}", touches.get(0).id);
            return touches.get(0).end();
        }

        /**
         * Converts this value to touches.
         *
         * @return values
         */
        public List<BidiPointer> touches() {
            return List.copyOf(touches);
        }

        /**
         * Handles remove.
         *
         * @param handle handle value
         */
        private void remove(BidiPointer handle) {
            touches.remove(handle);
        }
    }

    /**
     * Represents BiDi pointer.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class BidiPointer implements Pointer {

        /**
         * Current input.
         */
        private final BidiInput input;
        /**
         * Current touchscreen.
         */
        private final Touchscreen touchscreen;
        /**
         * Current identifier.
         */
        private final String id;
        /**
         * Current x.
         */
        private int x;
        /**
         * Current y.
         */
        private int y;
        /**
         * Whether started is enabled.
         */
        private boolean started;
        /**
         * Current started future.
         */
        private CompletableFuture<CdpPayload> startedFuture = CompletableFuture.completedFuture(CdpPayload.NULL);

        /**
         * Creates an instance.
         *
         * @param input       input source
         * @param touchscreen touchscreen value
         * @param id          identifier
         * @param x           x value
         * @param y           y value
         */
        private BidiPointer(BidiInput input, Touchscreen touchscreen, String id, double x, double y) {
            this.input = input;
            this.touchscreen = touchscreen;
            this.id = id;
            this.x = (int) Math.round(x);
            this.y = (int) Math.round(y);
        }

        /**
         * Returns the start.
         *
         * @return completion future
         */
        private CompletableFuture<CdpPayload> start() {
            if (started) {
                CompletableFuture<CdpPayload> rejected = new CompletableFuture<>();
                rejected.completeExceptionally(new InternalException("Touch has already started."));
                Logger.warn(false, "Input", "BiDi touch start rejected: id={}", id);
                return rejected;
            }
            started = true;
            startedFuture = input.performActions(
                    List.of(
                            touchSource(
                                    id,
                                    List.of(
                                            touchMove(x, y),
                                            Map.of(
                                                    "type",
                                                    "pointerDown",
                                                    "button",
                                                    0,
                                                    "width",
                                                    1,
                                                    "height",
                                                    1,
                                                    "pressure",
                                                    0.5)))));
            return startedFuture;
        }

        /**
         * Returns the started.
         *
         * @return completion future
         */
        public CompletableFuture<CdpPayload> started() {
            return startedFuture;
        }

        /**
         * Returns the ID.
         *
         * @return ID value
         */
        public Object id() {
            return id;
        }

        /**
         * Dispatches a move input action.
         *
         * @param x x value
         * @param y y value
         * @return completion future
         */
        public CompletableFuture<CdpPayload> move(double x, double y) {
            this.x = (int) Math.round(x);
            this.y = (int) Math.round(y);
            Logger.debug(true, "Input", "BiDi touch handle move requested: id={}, x={}, y={}", id, this.x, this.y);
            return input.performActions(List.of(touchSource(id, List.of(touchMove(this.x, this.y)))));
        }

        /**
         * Returns the end.
         *
         * @return completion future
         */
        public CompletableFuture<CdpPayload> end() {
            touchscreen.remove(this);
            Logger.debug(false, "Input", "BiDi touch handle end requested: id={}", id);
            return input.performActions(List.of(touchSource(id, List.of(Map.of("type", "pointerUp", "button", 0)))));
        }

        /**
         * Returns the x.
         *
         * @return x value
         */
        public double x() {
            return x;
        }

        /**
         * Returns the y.
         *
         * @return y value
         */
        public double y() {
            return y;
        }

        /**
         * Converts this value to touch move.
         *
         * @param x x value
         * @param y y value
         * @return mapped values
         */
        private static Map<String, Object> touchMove(int x, int y) {
            Map<String, Object> action = new LinkedHashMap<>();
            action.put("type", "pointerMove");
            action.put("x", x);
            action.put("y", y);
            return action;
        }
    }

}
