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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;

/**
 * Dispatches CDP compatibility events without depending on kernel runtime classes.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class Dispatcher {

    /**
     * Event handlers keyed by CDP-compatible session ID.
     */
    private static final Map<String, Handler> HANDLERS = new ConcurrentHashMap<>();

    /**
     * Creates a dispatcher.
     */
    private Dispatcher() {
        // No initialization required.
    }

    /**
     * Registers a handler for a CDP-compatible session.
     *
     * @param sessionId session ID value
     * @param handler   handler to invoke
     */
    public static void register(String sessionId, Handler handler) {
        HANDLERS.put(Assert.notBlank(sessionId, "sessionId"), Assert.notNull(handler, "handler"));
    }

    /**
     * Removes the handler registered for a CDP-compatible session.
     *
     * @param sessionId session ID value
     */
    public static void unregister(String sessionId) {
        HANDLERS.remove(Assert.notBlank(sessionId, "sessionId"));
    }

    /**
     * Emits an event to registered listeners.
     *
     * @param sessionId session id
     * @param method    protocol method
     * @param params    protocol parameters
     */
    public static void emit(String sessionId, String method, CdpPayload params) {
        Handler handler = HANDLERS.get(Assert.notBlank(sessionId, "sessionId"));
        if (handler != null) {
            handler.emit(Assert.notBlank(method, "method"), params == null ? CdpPayload.NULL : params);
        }
    }

    /**
     * Defines the handler contract.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public interface Handler {

        /**
         * Emits an event to registered listeners.
         *
         * @param method protocol method
         * @param params protocol parameters
         */
        void emit(String method, CdpPayload params);

    }

}
