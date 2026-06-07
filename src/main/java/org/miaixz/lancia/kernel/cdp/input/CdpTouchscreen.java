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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.kernel.Pointer;
import org.miaixz.lancia.kernel.Touch;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;

/**
 * Sends touchscreen input actions.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpTouchscreen implements Touch {

    /**
     * Current input.
     */
    private final CdpInput input;
    /**
     * Registered touches values.
     */
    private final List<CdpPointer> touches = new ArrayList<>();
    /**
     * Current touch ID.
     */
    private int touchId;

    /**
     * Creates a touchscreen.
     *
     * @param input input source
     */
    public CdpTouchscreen(CdpInput input) {
        this.input = input;
    }

    /**
     * Handles tap.
     *
     * @param x x value
     * @param y y value
     */
    public void tap(double x, double y) {
        Logger.debug(true, "Input", "Touch tap requested: x={}, y={}", x, y);
        CdpPointer touch = startTouch(x, y);
        touch.end();
        Logger.debug(false, "Input", "Touch tap completed: x={}, y={}", x, y);
    }

    /**
     * Converts this value to touch start.
     *
     * @param x x value
     * @param y y value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> touchStart(double x, double y) {
        return startTouch(x, y).started();
    }

    /**
     * Returns the start touch.
     *
     * @param x x value
     * @param y y value
     * @return start touch value
     */
    public CdpPointer startTouch(double x, double y) {
        CdpPointer handle = new CdpPointer(this, ++touchId, (int) Math.round(x), (int) Math.round(y));
        Logger.debug(true, "Input", "Touch start requested: id={}, x={}, y={}", handle.id, x, y);
        handle.start();
        touches.add(handle);
        Logger.debug(false, "Input", "Touch started: id={}, active={}", handle.id, touches.size());
        return handle;
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
            Logger.trace(false, "Input", "Touch move skipped without active touch.");
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        Logger.debug(true, "Input", "Touch move requested: x={}, y={}", x, y);
        return touches.get(0).move(x, y);
    }

    /**
     * Converts this value to touch end.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> touchEnd() {
        if (touches.isEmpty()) {
            Logger.trace(false, "Input", "Touch end skipped without active touch.");
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        CdpPointer handle = touches.get(0);
        Logger.debug(false, "Input", "Touch end requested: id={}", handle.id);
        return handle.end();
    }

    /**
     * Converts this value to touch point.
     *
     * @param id identifier
     * @param x  x value
     * @param y  y value
     * @return mapped values
     */
    private Map<String, Object> touchPoint(int id, double x, double y) {
        return Map.of("x", Math.round(x), "y", Math.round(y), "radiusX", 0.5, "radiusY", 0.5, "force", 0.5, "id", id);
    }

    /**
     * Converts this value to touches.
     *
     * @return values
     */
    public List<CdpPointer> touches() {
        return List.copyOf(touches);
    }

    /**
     * Handles remove.
     *
     * @param handle handle value
     */
    private void remove(CdpPointer handle) {
        touches.remove(handle);
    }

    /**
     * Represents CDP pointer.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class CdpPointer implements Pointer {

        /**
         * Current touchscreen.
         */
        private final CdpTouchscreen touchscreen;
        /**
         * Current x.
         */
        private int x;
        /**
         * Current y.
         */
        private int y;
        /**
         * Current identifier.
         */
        private final int id;
        /**
         * Whether started state is enabled.
         */
        private boolean startedState;
        /**
         * Current started.
         */
        private CompletableFuture<CdpPayload> started = CompletableFuture.completedFuture(CdpPayload.NULL);

        /**
         * Creates an instance.
         *
         * @param touchscreen touchscreen value
         * @param id          identifier
         * @param x           x value
         * @param y           y value
         */
        private CdpPointer(CdpTouchscreen touchscreen, int id, int x, int y) {
            this.touchscreen = touchscreen;
            this.id = id;
            this.x = x;
            this.y = y;
        }

        /**
         * Handles start.
         */
        private void start() {
            if (startedState) {
                throw new InternalException("Touch has already started");
            }
            Logger.trace(true, "Input", "Touch event dispatch requested: type=touchStart, id={}", id);
            started = touchscreen.input.dispatchTouchEvent("touchStart", List.of(touchscreen.touchPoint(id, x, y)));
            startedState = true;
        }

        /**
         * Returns the started.
         *
         * @return completion future
         */
        public CompletableFuture<CdpPayload> started() {
            return started;
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
            Logger.trace(true, "Input", "Touch event dispatch requested: type=touchMove, id={}", id);
            return touchscreen.input
                    .dispatchTouchEvent("touchMove", List.of(touchscreen.touchPoint(id, this.x, this.y)));
        }

        /**
         * Returns the end.
         *
         * @return completion future
         */
        public CompletableFuture<CdpPayload> end() {
            touchscreen.remove(this);
            Logger.trace(false, "Input", "Touch event dispatch requested: type=touchEnd, id={}", id);
            return touchscreen.input.dispatchTouchEvent("touchEnd", List.of(touchscreen.touchPoint(id, x, y)));
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
    }

}
