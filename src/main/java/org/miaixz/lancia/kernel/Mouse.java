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

import java.util.concurrent.CompletableFuture;

import org.miaixz.lancia.Payload;
import org.miaixz.lancia.nimble.input.DragData;
import org.miaixz.lancia.options.MouseClickOptions;
import org.miaixz.lancia.options.MouseMoveOptions;
import org.miaixz.lancia.options.MouseWheelOptions;

/**
 * Sends mouse input to a page.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Mouse {

    /**
     * Moves the mouse to the coordinates.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return command future
     */
    CompletableFuture<? extends Payload> move(double x, double y);

    /**
     * Moves the mouse to the coordinates with options.
     *
     * @param x       x coordinate
     * @param y       y coordinate
     * @param options move options
     * @return command future
     */
    CompletableFuture<? extends Payload> move(double x, double y, MouseMoveOptions options);

    /**
     * Moves the mouse to the coordinates in steps.
     *
     * @param x     x coordinate
     * @param y     y coordinate
     * @param steps movement steps
     * @return command future
     */
    CompletableFuture<? extends Payload> move(double x, double y, int steps);

    /**
     * Presses down a mouse button.
     *
     * @param button button name
     * @return command future
     */
    CompletableFuture<? extends Payload> down(String button);

    /**
     * Presses down a mouse button with options.
     *
     * @param options click options
     * @return command future
     */
    CompletableFuture<? extends Payload> down(MouseClickOptions options);

    /**
     * Presses down a mouse button with click count.
     *
     * @param button     button name
     * @param clickCount click count
     * @return command future
     */
    CompletableFuture<? extends Payload> down(String button, int clickCount);

    /**
     * Releases a mouse button.
     *
     * @param button button name
     * @return command future
     */
    CompletableFuture<? extends Payload> up(String button);

    /**
     * Releases a mouse button with options.
     *
     * @param options click options
     * @return command future
     */
    CompletableFuture<? extends Payload> up(MouseClickOptions options);

    /**
     * Releases a mouse button with click count.
     *
     * @param button     button name
     * @param clickCount click count
     * @return command future
     */
    CompletableFuture<? extends Payload> up(String button, int clickCount);

    /**
     * Clicks at the coordinates.
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    void click(double x, double y);

    /**
     * Clicks at the coordinates with options.
     *
     * @param x       x coordinate
     * @param y       y coordinate
     * @param options click options
     */
    void click(double x, double y, MouseClickOptions options);

    /**
     * Clicks at the coordinates with button and click count.
     *
     * @param x          x coordinate
     * @param y          y coordinate
     * @param button     button name
     * @param clickCount click count
     */
    void click(double x, double y, String button, int clickCount);

    /**
     * Dispatches a mouse wheel event.
     *
     * @param deltaX horizontal delta
     * @param deltaY vertical delta
     * @return command future
     */
    CompletableFuture<? extends Payload> wheel(double deltaX, double deltaY);

    /**
     * Dispatches a mouse wheel event with options.
     *
     * @param options wheel options
     * @return command future
     */
    CompletableFuture<? extends Payload> wheel(MouseWheelOptions options);

    /**
     * Starts a drag operation between two coordinates.
     *
     * @param startX start x
     * @param startY start y
     * @param endX   end x
     * @param endY   end y
     * @return drag data
     */
    DragData drag(double startX, double startY, double endX, double endY);

    /**
     * Dispatches a dragEnter event.
     *
     * @param x    x coordinate
     * @param y    y coordinate
     * @param data drag data
     * @return command future
     */
    CompletableFuture<? extends Payload> dragEnter(double x, double y, DragData data);

    /**
     * Dispatches a dragOver event.
     *
     * @param x    x coordinate
     * @param y    y coordinate
     * @param data drag data
     * @return command future
     */
    CompletableFuture<? extends Payload> dragOver(double x, double y, DragData data);

    /**
     * Dispatches a drop event.
     *
     * @param x    x coordinate
     * @param y    y coordinate
     * @param data drag data
     * @return command future
     */
    CompletableFuture<? extends Payload> drop(double x, double y, DragData data);

    /**
     * Drags and drops between two coordinates.
     *
     * @param startX start x
     * @param startY start y
     * @param endX   end x
     * @param endY   end y
     */
    void dragAndDrop(double startX, double startY, double endX, double endY);

    /**
     * Resets the mouse state.
     */
    void reset();

}
