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

import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.exception.InternalException;

/**
 * Public transport API for sending and receiving protocol messages.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Transport extends AutoCloseable {

    /**
     * Sends a protocol message.
     *
     * @param message protocol message
     */
    void send(String message);

    /**
     * Binds an implementation-specific connection object.
     *
     * @param connection implementation-specific connection
     */
    default void setConnection(Object connection) {
        if (connection == null) {
            return;
        }
        try {
            Method method = connectionSetter(connection);
            if (method == null) {
                return;
            }
            method.setAccessible(true);
            method.invoke(this, connection);
        } catch (ReflectiveOperationException e) {
            throw new InternalException(e);
        }
    }

    /**
     * Finds an implementation-specific connection setter without coupling this public contract to bridge classes.
     *
     * @param connection implementation-specific connection
     * @return setter method or null
     */
    private Method connectionSetter(Object connection) {
        Class<?> type = getClass();
        while (type != null) {
            for (Method method : type.getDeclaredMethods()) {
                if (!"setConnection".equals(method.getName()) || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> parameter = method.getParameterTypes()[0];
                if (parameter == Object.class) {
                    continue;
                }
                if (parameter.isAssignableFrom(connection.getClass())) {
                    return method;
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    /**
     * Updates message handler.
     *
     * @param handler handler to invoke
     */
    default void setMessageHandler(Consumer<String> handler) {
    }

    /**
     * Updates close handler.
     *
     * @param handler handler to invoke
     */
    default void setCloseHandler(Runnable handler) {
    }

    /**
     * Closes the transport.
     */
    @Override
    void close();

}
