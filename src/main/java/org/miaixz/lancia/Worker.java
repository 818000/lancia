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

import org.miaixz.lancia.kernel.Handle;

/**
 * Public worker API that carries Puppeteer's WebWorker contract.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public abstract class Worker implements Harness, AutoCloseable {

    /**
     * Creates a worker.
     */
    protected Worker() {
        // No initialization required.
    }

    /**
     * Returns worker URL.
     *
     * @return worker URL
     */
    public abstract String url();

    /**
     * Returns the protocol session.
     *
     * @return protocol session
     */
    @Override
    public abstract Session client();

    /**
     * Returns timeout settings.
     *
     * @return timeout settings
     */
    public abstract Object timeoutSettings();

    /**
     * Returns the main realm.
     *
     * @return main realm
     */
    @Override
    public abstract Realm mainRealm();

    /**
     * Evaluates an expression.
     *
     * @param expression expression
     * @return evaluation result
     */
    public abstract Object evaluate(String expression);

    /**
     * Evaluates an expression and returns a handle.
     *
     * @param expression expression
     * @return handle
     */
    public abstract Handle evaluateHandle(String expression);

    /**
     * Adds an event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return binding
     */
    public abstract Binding on(String event, Consumer<Object> listener);

    /**
     * Adds a one-shot event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return binding
     */
    public abstract Binding once(String event, Consumer<Object> listener);

    /**
     * Removes an event listener.
     *
     * @param event    event name
     * @param listener event listener
     * @return binding
     */
    public abstract Binding off(String event, Consumer<Object> listener);

    /**
     * Removes all listeners for an event.
     *
     * @param event event name
     * @return binding
     */
    public abstract Binding off(String event);

    /**
     * Emits an event.
     *
     * @param event   event name
     * @param payload event payload
     * @return {@code true} when listeners received the event
     */
    public abstract boolean emit(String event, Object payload);

    /**
     * Counts event listeners.
     *
     * @param event event name
     * @return listener count
     */
    public abstract int listenerCount(String event);

    /**
     * Removes all listeners for an event.
     *
     * @param event event name
     * @return binding
     */
    public abstract Binding removeAllListeners(String event);

    /**
     * Removes all listeners.
     *
     * @return binding
     */
    public abstract Binding removeAllListeners();

    /**
     * Closes the worker target.
     */
    @Override
    public abstract void close();

}
