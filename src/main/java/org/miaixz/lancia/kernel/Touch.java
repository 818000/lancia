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
import java.util.concurrent.CompletableFuture;

import org.miaixz.lancia.Payload;

/**
 * Sends touch input to a page.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Touch {

    /**
     * Taps at the coordinates.
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    void tap(double x, double y);

    /**
     * Dispatches a touch start event.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return command future
     */
    CompletableFuture<? extends Payload> touchStart(double x, double y);

    /**
     * Starts a touch and returns a pointer for subsequent movement.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return touch pointer
     */
    Pointer startTouch(double x, double y);

    /**
     * Moves the active touch.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return command future
     */
    CompletableFuture<? extends Payload> touchMove(double x, double y);

    /**
     * Ends the active touch.
     *
     * @return command future
     */
    CompletableFuture<? extends Payload> touchEnd();

    /**
     * Returns active touch pointers.
     *
     * @return touch pointers
     */
    List<? extends Pointer> touches();

}
