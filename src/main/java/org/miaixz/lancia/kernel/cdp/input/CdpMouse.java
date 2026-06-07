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
package org.miaixz.lancia.kernel.cdp.input;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.kernel.Mouse;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.nimble.input.DragData;
import org.miaixz.lancia.nimble.input.DragPoint;
import org.miaixz.lancia.options.MouseClickOptions;
import org.miaixz.lancia.options.MouseMoveOptions;
import org.miaixz.lancia.options.MouseWheelOptions;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.input.InputAction;

/**
 * Sends mouse input actions.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpMouse implements Mouse {

    /**
     * Current input.
     */
    private final CdpInput input;
    /**
     * Current x.
     */
    private double x;
    /**
     * Current y.
     */
    private double y;
    /**
     * Current buttons.
     */
    private int buttons;
    /**
     * Current button.
     */
    private String button = "none";

    /**
     * Creates a mouse.
     *
     * @param input input source
     */
    public CdpMouse(CdpInput input) {
        this.input = input;
    }

    /**
     * Dispatches a move input action.
     *
     * @param x x value
     * @param y y value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> move(double x, double y) {
        return move(x, y, 1);
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
        int safeSteps = Math.max(1, steps);
        Logger.debug(
                true,
                "Input",
                "CdpMouse move requested: from=({},{}), to=({},{}), steps={}",
                this.x,
                this.y,
                x,
                y,
                safeSteps);
        double fromX = this.x;
        double fromY = this.y;
        CompletableFuture<CdpPayload> result = CompletableFuture.completedFuture(CdpPayload.NULL);
        for (int index = 1; index <= safeSteps; index++) {
            double nextX = fromX + (x - fromX) * index / safeSteps;
            double nextY = fromY + (y - fromY) * index / safeSteps;
            this.x = nextX;
            this.y = nextY;
            result = input.dispatchMouseEvent("mouseMoved", nextX, nextY, buttonFromPressedButtons(buttons), buttons);
        }
        Logger.debug(false, "Input", "CdpMouse move completed: x={}, y={}", this.x, this.y);
        return result;
    }

    /**
     * Returns the move immediate.
     *
     * @param x x value
     * @param y y value
     * @return completion future
     */
    private CompletableFuture<CdpPayload> moveImmediate(double x, double y) {
        this.x = x;
        this.y = y;
        return input.dispatchMouseEvent("mouseMoved", x, y, buttonFromPressedButtons(buttons), buttons);
    }

    /**
     * Dispatches a down input action.
     *
     * @param button button value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> down(String button) {
        return down(button, 1);
    }

    /**
     * Dispatches a down input action.
     *
     * @param options operation options
     * @return completion future
     */
    public CompletableFuture<CdpPayload> down(MouseClickOptions options) {
        MouseClickOptions actualOptions = options == null ? new MouseClickOptions() : options;
        return down(actualOptions.getButton(), actualOptions.getCount());
    }

    /**
     * Dispatches a down input action.
     *
     * @param button     button value
     * @param clickCount click count value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> down(String button, int clickCount) {
        String actualButton = button == null ? "left" : button;
        int flag = buttonMask(actualButton);
        if (flag == 0) {
            throw new InternalException("Invalid mouse button: " + actualButton);
        }
        if ((buttons & flag) != 0) {
            throw new InternalException(Symbol.SINGLE_QUOTE + actualButton + "' is already pressed.");
        }
        Logger.debug(
                true,
                "Input",
                "CdpMouse down requested: button={}, clickCount={}, x={}, y={}",
                actualButton,
                clickCount,
                x,
                y);
        buttons |= flag;
        this.button = actualButton;
        return input.dispatchMouseEvent("mousePressed", x, y, actualButton, buttons, 0, 0, clickCount, "mouse");
    }

    /**
     * Dispatches an up input action.
     *
     * @param button button value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> up(String button) {
        return up(button, 1);
    }

    /**
     * Dispatches an up input action.
     *
     * @param options operation options
     * @return completion future
     */
    public CompletableFuture<CdpPayload> up(MouseClickOptions options) {
        MouseClickOptions actualOptions = options == null ? new MouseClickOptions() : options;
        return up(actualOptions.getButton(), actualOptions.getCount());
    }

    /**
     * Dispatches an up input action.
     *
     * @param button     button value
     * @param clickCount click count value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> up(String button, int clickCount) {
        String released = button == null ? this.button : button;
        int flag = buttonMask(released);
        if (flag == 0) {
            throw new InternalException("Invalid mouse button: " + released);
        }
        if ((buttons & flag) == 0) {
            throw new InternalException(Symbol.SINGLE_QUOTE + released + "' is not pressed.");
        }
        Logger.debug(
                false,
                "Input",
                "CdpMouse up requested: button={}, clickCount={}, x={}, y={}",
                released,
                clickCount,
                x,
                y);
        buttons &= ~flag;
        CompletableFuture<CdpPayload> result = input
                .dispatchMouseEvent("mouseReleased", x, y, released, buttons, 0, 0, clickCount, "mouse");
        this.button = buttonFromPressedButtons(buttons);
        return result;
    }

    /**
     * Handles click.
     *
     * @param x x value
     * @param y y value
     */
    public void click(double x, double y) {
        click(x, y, "left", 1);
    }

    /**
     * Handles click.
     *
     * @param x       x value
     * @param y       y value
     * @param options operation options
     */
    public void click(double x, double y, MouseClickOptions options) {
        MouseClickOptions actualOptions = options == null ? new MouseClickOptions() : options;
        Logger.debug(
                true,
                "Input",
                "CdpMouse click requested: button={}, clickCount={}, delayMillis={}, x={}, y={}",
                actualOptions.getButton(),
                actualOptions.getCount(),
                actualOptions.getDelay(),
                x,
                y);
        move(x, y);
        int clickCount = actualOptions.getCount();
        if (clickCount < 1) {
            throw new InternalException("Click must occur a positive number of times.");
        }
        for (int index = 1; index <= clickCount; index++) {
            down(actualOptions.getButton(), index);
            sleep(actualOptions.getDelay());
            up(actualOptions.getButton(), index);
        }
        Logger.debug(
                false,
                "Input",
                "CdpMouse click completed: button={}, clickCount={}",
                actualOptions.getButton(),
                clickCount);
    }

    /**
     * Handles click.
     *
     * @param x          x value
     * @param y          y value
     * @param button     button value
     * @param clickCount click count value
     */
    public void click(double x, double y, String button, int clickCount) {
        Logger.debug(
                true,
                "Input",
                "CdpMouse click requested: button={}, clickCount={}, x={}, y={}",
                button,
                clickCount,
                x,
                y);
        move(x, y);
        if (clickCount < 1) {
            throw new InternalException("Click must occur a positive number of times.");
        }
        for (int index = 1; index <= clickCount; index++) {
            down(button, index);
            up(button, index);
        }
        Logger.debug(false, "Input", "CdpMouse click completed: button={}, clickCount={}", button, clickCount);
    }

    /**
     * Returns the wheel.
     *
     * @param deltaX delta x value
     * @param deltaY delta y value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> wheel(double deltaX, double deltaY) {
        Logger.debug(true, "Input", "CdpMouse wheel requested: x={}, y={}, deltaX={}, deltaY={}", x, y, deltaX, deltaY);
        return input.dispatchMouseEvent("mouseWheel", x, y, "none", buttons, deltaX, deltaY, 0, "mouse");
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
     * Handles reset.
     */
    public void reset() {
        for (String value : List.of("left", "middle", "right", "forward", "back")) {
            if ((buttons & buttonMask(value)) != 0) {
                up(value);
            }
        }
        if (x != 0 || y != 0) {
            moveImmediate(0, 0);
        }
    }

    /**
     * Dispatches a drag input action.
     *
     * @param startX start x value
     * @param startY start y value
     * @param endX   end x value
     * @param endY   end y value
     * @return drag value
     */
    public DragData drag(double startX, double startY, double endX, double endY) {
        Logger.debug(true, "Input", "CdpMouse drag requested: start=({},{}), end=({},{})", startX, startY, endX, endY);
        move(startX, startY);
        down("left");
        move(endX, endY);
        Logger.debug(false, "Input", "CdpMouse drag prepared: start=({},{}), end=({},{})", startX, startY, endX, endY);
        return new DragData(new DragPoint(startX, startY), new DragPoint(endX, endY));
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
        return input.dispatchDragEvent("dragEnter", x, y, data);
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
        return input.dispatchDragEvent("dragOver", x, y, data);
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
        return input.dispatchDragEvent("drop", x, y, data);
    }

    /**
     * Handles drag and drop.
     *
     * @param startX start x value
     * @param startY start y value
     * @param endX   end x value
     * @param endY   end y value
     */
    public void dragAndDrop(double startX, double startY, double endX, double endY) {
        DragData data = drag(startX, startY, endX, endY);
        dragEnter(endX, endY, data);
        dragOver(endX, endY, data);
        drop(endX, endY, data);
        up("left");
    }

    /**
     * Handles sleep.
     *
     * @param delayMillis delay in milliseconds
     */
    private void sleep(long delayMillis) {
        Awaitable.sleep(delayMillis, "CdpMouse input was interrupted.");
    }

    /**
     * Returns the button mask.
     *
     * @param button button value
     * @return button mask value
     */
    private int buttonMask(String button) {
        return switch (button) {
            case "left" -> 1;
            case "right" -> 2;
            case "middle" -> 4;
            case "back" -> 8;
            case "forward" -> 16;
            case "none" -> 0;
            default -> 0;
        };
    }

    /**
     * Returns the button from pressed buttons.
     *
     * @param buttons buttons value
     * @return button from pressed buttons value
     */
    private String buttonFromPressedButtons(int buttons) {
        if ((buttons & 1) != 0) {
            return "left";
        }
        if ((buttons & 2) != 0) {
            return "right";
        }
        if ((buttons & 4) != 0) {
            return "middle";
        }
        if ((buttons & 8) != 0) {
            return "back";
        }
        if ((buttons & 16) != 0) {
            return "forward";
        }
        return "none";
    }

}
