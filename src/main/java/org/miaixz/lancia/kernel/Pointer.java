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

/**
 * Public pointer for an active touch point.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Pointer {

    /**
     * Returns the touch identifier.
     *
     * @return touch identifier
     */
    Object id();

    /**
     * Returns the last x coordinate.
     *
     * @return x coordinate
     */
    double x();

    /**
     * Returns the last y coordinate.
     *
     * @return y coordinate
     */
    double y();

    /**
     * Returns the touch start command future.
     *
     * @return command future
     */
    CompletableFuture<? extends Payload> started();

    /**
     * Moves this touch.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return command future
     */
    CompletableFuture<? extends Payload> move(double x, double y);

    /**
     * Ends this touch.
     *
     * @return command future
     */
    CompletableFuture<? extends Payload> end();

}
