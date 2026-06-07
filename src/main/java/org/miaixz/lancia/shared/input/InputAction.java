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
package org.miaixz.lancia.shared.input;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.miaixz.lancia.Payload;
import org.miaixz.lancia.options.KeyboardTypeOptions;
import org.miaixz.lancia.options.MouseMoveOptions;
import org.miaixz.lancia.options.MouseWheelOptions;

/**
 * Provides shared input option expansion helpers.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class InputAction {

    /**
     * Creates an InputAction instance.
     */
    private InputAction() {
        // No initialization required.
    }

    /**
     * Dispatches a down input action.
     *
     * @param key     key value
     * @param options operation options
     * @param action  action value
     * @return completion future
     */
    public static <T extends Payload> CompletableFuture<T> down(
            String key,
            KeyboardTypeOptions options,
            KeyboardDownAction<T> action) {
        KeyboardTypeOptions actualOptions = options == null ? new KeyboardTypeOptions() : options;
        return action.down(key, actualOptions.getText(), actualOptions.getCommands());
    }

    /**
     * Dispatches a move input action.
     *
     * @param x       x value
     * @param y       y value
     * @param options operation options
     * @param action  action value
     * @return completion future
     */
    public static <T extends Payload> CompletableFuture<T> move(
            double x,
            double y,
            MouseMoveOptions options,
            MouseMoveAction<T> action) {
        MouseMoveOptions actualOptions = options == null ? new MouseMoveOptions() : options;
        return action.move(x, y, actualOptions.getSteps());
    }

    /**
     * Returns the wheel.
     *
     * @param options operation options
     * @param action  action value
     * @return completion future
     */
    public static <T extends Payload> CompletableFuture<T> wheel(
            MouseWheelOptions options,
            MouseWheelAction<T> action) {
        MouseWheelOptions actualOptions = options == null ? new MouseWheelOptions() : options;
        return action.wheel(actualOptions.getDeltaX(), actualOptions.getDeltaY());
    }

    /**
     * Defines the KeyboardDownAction interface.
     */
    @FunctionalInterface
    public interface KeyboardDownAction<T extends Payload> {

        /**
         * Dispatches a down input action.
         *
         * @param key      key value
         * @param text     text to use
         * @param commands commands to send
         * @return completion future
         */
        CompletableFuture<T> down(String key, String text, List<String> commands);
    }

    /**
     * Defines the MouseMoveAction interface.
     */
    @FunctionalInterface
    public interface MouseMoveAction<T extends Payload> {

        /**
         * Dispatches a move input action.
         *
         * @param x     x value
         * @param y     y value
         * @param steps steps value
         * @return completion future
         */
        CompletableFuture<T> move(double x, double y, int steps);
    }

    /**
     * Defines the MouseWheelAction interface.
     */
    @FunctionalInterface
    public interface MouseWheelAction<T extends Payload> {

        /**
         * Returns the wheel.
         *
         * @param deltaX delta x value
         * @param deltaY delta y value
         * @return completion future
         */
        CompletableFuture<T> wheel(double deltaX, double deltaY);
    }

}
