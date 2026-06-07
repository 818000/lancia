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
package org.miaixz.lancia;

import java.util.function.Consumer;

/**
 * Publishes events and manages listener bindings.
 *
 * @param <E> event type
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Emitter<E> {

    /**
     * Subscribes a persistent listener.
     *
     * @param event    event to listen for
     * @param listener listener invoked for each event occurrence
     * @return this emitter
     */
    Emitter<E> on(E event, Consumer<Object> listener);

    /**
     * Subscribes a one-shot listener.
     *
     * @param event    event to listen for
     * @param listener listener invoked for the next event occurrence
     * @return this emitter
     */
    Emitter<E> once(E event, Consumer<Object> listener);

    /**
     * Removes a listener.
     *
     * @param event    event whose listener is removed
     * @param listener listener to remove
     * @return this emitter
     */
    Emitter<E> off(E event, Consumer<Object> listener);

    /**
     * Removes all listeners for the event.
     *
     * @param event event whose listeners are removed
     * @return this emitter
     */
    Emitter<E> off(E event);

    /**
     * Emits an event payload.
     *
     * @param event   event to emit
     * @param payload event payload
     * @return {@code true} when at least one listener received the event
     */
    boolean emit(E event, Object payload);

    /**
     * Counts listeners for the event.
     *
     * @param event event to inspect
     * @return listener count
     */
    int listenerCount(E event);

    /**
     * Removes all listeners for the event.
     *
     * @param event event whose listeners are removed
     * @return this emitter
     */
    Emitter<E> removeAllListeners(E event);

    /**
     * Removes all listeners.
     *
     * @return this emitter
     */
    Emitter<E> removeAllListeners();

}
