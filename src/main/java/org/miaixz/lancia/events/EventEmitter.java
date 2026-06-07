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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.lancia.Emitter;

/**
 * Provides a bridge-local event emitter implementation.
 *
 * @param <E> event type
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class EventEmitter<E> implements Emitter<E>, AutoCloseable {

    /**
     * Mapped listeners values.
     */
    private final Map<E, CopyOnWriteArrayList<Listener>> listeners = new ConcurrentHashMap<>();

    /**
     * Creates an EventEmitter instance.
     */
    public EventEmitter() {
        // No initialization required.
    }

    /**
     * Subscribes a persistent listener.
     *
     * @param event    event value
     * @param listener event listener
     * @return emitter
     */
    @Override
    public EventEmitter<E> on(E event, Consumer<Object> listener) {
        add(event, listener, false);
        return this;
    }

    /**
     * Subscribes a one-shot listener.
     *
     * @param event    event value
     * @param listener event listener
     * @return emitter
     */
    @Override
    public EventEmitter<E> once(E event, Consumer<Object> listener) {
        add(event, listener, true);
        return this;
    }

    /**
     * Removes a listener.
     *
     * @param event    event value
     * @param listener event listener
     * @return emitter
     */
    @Override
    public EventEmitter<E> off(E event, Consumer<Object> listener) {
        Assert.notNull(event, "event");
        Assert.notNull(listener, "listener");
        List<Listener> eventListeners = listeners.get(event);
        if (eventListeners != null) {
            for (int i = eventListeners.size() - 1; i >= 0; i--) {
                if (eventListeners.get(i).listener == listener) {
                    eventListeners.remove(i);
                    break;
                }
            }
            if (eventListeners.isEmpty()) {
                listeners.remove(event);
            }
        }
        return this;
    }

    /**
     * Removes all listeners for the event.
     *
     * @param event event type
     * @return emitter
     */
    @Override
    public EventEmitter<E> off(E event) {
        return removeAllListeners(event);
    }

    /**
     * Emits an event payload.
     *
     * @param event   event value
     * @param payload event payload
     * @return emitted state
     */
    public boolean emit(E event, Object payload) {
        Assert.notNull(event, "event");
        boolean emitted = emitTo(event, payload);
        if (!Symbol.STAR.equals(event)) {
            emitted = emitTo(wildcardEvent(), payload) || emitted;
        }
        return emitted;
    }

    /**
     * Counts listeners for the event.
     *
     * @param event event type
     * @return listener count
     */
    @Override
    public int listenerCount(E event) {
        Assert.notNull(event, "event");
        List<Listener> eventListeners = listeners.get(event);
        return eventListeners == null ? 0 : eventListeners.size();
    }

    /**
     * Removes all listeners for the event.
     *
     * @param event event type
     * @return emitter
     */
    @Override
    public EventEmitter<E> removeAllListeners(E event) {
        Assert.notNull(event, "event");
        listeners.remove(event);
        return this;
    }

    /**
     * Removes all listeners.
     *
     * @return emitter
     */
    @Override
    public EventEmitter<E> removeAllListeners() {
        listeners.clear();
        return this;
    }

    /**
     * Disposes emitter state.
     */
    protected void dispose() {
        removeAllListeners();
    }

    /**
     * Closes the emitter.
     */
    @Override
    public void close() {
        dispose();
    }

    /**
     * Adds one listener.
     *
     * @param event    event value
     * @param listener event listener
     * @param once     one-shot state
     */
    private void add(E event, Consumer<Object> listener, boolean once) {
        Assert.notNull(event, "event");
        Assert.notNull(listener, "listener");
        listeners.computeIfAbsent(event, key -> new CopyOnWriteArrayList<>()).add(new Listener(listener, once));
    }

    /**
     * Emits a payload to listeners of one event key.
     *
     * @param event   event value
     * @param payload event payload
     * @return emitted state
     */
    private boolean emitTo(E event, Object payload) {
        List<Listener> eventListeners = listeners.get(event);
        if (eventListeners == null || eventListeners.isEmpty()) {
            return false;
        }
        for (Listener listener : List.copyOf(eventListeners)) {
            listener.listener.accept(payload);
            if (listener.once) {
                eventListeners.remove(listener);
            }
        }
        if (eventListeners.isEmpty()) {
            listeners.remove(event);
        }
        return true;
    }

    /**
     * Returns the wildcard event key.
     *
     * @return wildcard event key
     */
    private E wildcardEvent() {
        return (E) Symbol.STAR;
    }

    /**
     * Stores a registered event listener and one-shot flag.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class Listener {

        /**
         * Current listener.
         */
        private final Consumer<Object> listener;
        /**
         * Whether once is enabled.
         */
        private final boolean once;

        /**
         * Creates an instance.
         *
         * @param listener event listener
         * @param once     once value
         */
        private Listener(Consumer<Object> listener, boolean once) {
            this.listener = listener;
            this.once = once;
        }

    }

}
