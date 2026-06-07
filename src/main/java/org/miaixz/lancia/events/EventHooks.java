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
package org.miaixz.lancia.events;

import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.lancia.Binding;

/**
 * Provides shared event binding helpers.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class EventHooks {

    /**
     * Prevents instantiation.
     */
    private EventHooks() {
        // No initialization required.
    }

    /**
     * Subscribes a persistent listener.
     *
     * @param emitter  emitter
     * @param event    event
     * @param listener listener
     * @param <E>      event type
     * @return binding
     */
    public static <E> Binding on(EventEmitter<E> emitter, E event, Consumer<Object> listener) {
        return bind(emitter, Assert.notNull(event, "event"), listener, false);
    }

    /**
     * Subscribes a one-shot listener.
     *
     * @param emitter  emitter
     * @param event    event
     * @param listener listener
     * @param <E>      event type
     * @return binding
     */
    public static <E> Binding once(EventEmitter<E> emitter, E event, Consumer<Object> listener) {
        return bind(emitter, Assert.notNull(event, "event"), listener, true);
    }

    /**
     * Subscribes a persistent named listener.
     *
     * @param emitter  emitter
     * @param event    event
     * @param listener listener
     * @return binding
     */
    public static Binding onNamed(EventEmitter<String> emitter, String event, Consumer<Object> listener) {
        return bind(emitter, Assert.notBlank(event, "event"), listener, false);
    }

    /**
     * Subscribes a one-shot named listener.
     *
     * @param emitter  emitter
     * @param event    event
     * @param listener listener
     * @return binding
     */
    public static Binding onceNamed(EventEmitter<String> emitter, String event, Consumer<Object> listener) {
        return bind(emitter, Assert.notBlank(event, "event"), listener, true);
    }

    /**
     * Subscribes a persistent typed payload listener.
     *
     * @param emitter  emitter
     * @param event    event
     * @param listener listener
     * @param type     payload type
     * @param <E>      event type
     * @param <T>      payload type
     * @return binding
     */
    public static <E, T> Binding onPayload(EventEmitter<E> emitter, E event, Consumer<T> listener, Class<T> type) {
        return bindPayload(emitter, Assert.notNull(event, "event"), listener, type, false);
    }

    /**
     * Subscribes a one-shot typed payload listener.
     *
     * @param emitter  emitter
     * @param event    event
     * @param listener listener
     * @param type     payload type
     * @param <E>      event type
     * @param <T>      payload type
     * @return binding
     */
    public static <E, T> Binding oncePayload(EventEmitter<E> emitter, E event, Consumer<T> listener, Class<T> type) {
        return bindPayload(emitter, Assert.notNull(event, "event"), listener, type, true);
    }

    /**
     * Binds a listener.
     *
     * @param emitter  emitter
     * @param event    event
     * @param listener listener
     * @param once     one-shot state
     * @param <E>      event type
     * @return binding
     */
    private static <E> Binding bind(EventEmitter<E> emitter, E event, Consumer<Object> listener, boolean once) {
        EventEmitter<E> actualEmitter = Assert.notNull(emitter, "emitter");
        Consumer<Object> actualListener = Assert.notNull(listener, "listener");
        if (once) {
            actualEmitter.once(event, actualListener);
        } else {
            actualEmitter.on(event, actualListener);
        }
        return new EventBinding(() -> actualEmitter.off(event, actualListener));
    }

    /**
     * Binds a typed payload listener.
     *
     * @param emitter  emitter
     * @param event    event
     * @param listener listener
     * @param type     payload type
     * @param once     one-shot state
     * @param <E>      event type
     * @param <T>      payload type
     * @return binding
     */
    private static <E, T> Binding bindPayload(
            EventEmitter<E> emitter,
            E event,
            Consumer<T> listener,
            Class<T> type,
            boolean once) {
        Consumer<T> actualListener = Assert.notNull(listener, "listener");
        Class<T> actualType = Assert.notNull(type, "type");
        Consumer<Object> bridge = payload -> actualListener.accept(actualType.cast(payload));
        return bind(emitter, event, bridge, once);
    }

}
