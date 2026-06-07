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
package org.miaixz.lancia.runtime;

import java.util.List;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.lancia.Handler;
import org.miaixz.lancia.shared.query.CustomQueryHandler;

/**
 * Provides custom query handler runtime operations.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class QueryHandlers {

    /**
     * internal registry.
     */
    private static final CustomQueryHandler.Registry REGISTRY = CustomQueryHandler.registry();

    /**
     * Hides the query handlers constructor.
     */
    private QueryHandlers() {
        // No initialization required.
    }

    /**
     * Creates a single-element custom query handler.
     *
     * @param queryOne queryOne JavaScript source
     * @return custom query handler
     */
    public static Handler queryOne(String queryOne) {
        return CustomQueryHandler.queryOne(queryOne);
    }

    /**
     * Creates a multi-element custom query handler.
     *
     * @param queryAll queryAll JavaScript source
     * @return custom query handler
     */
    public static Handler queryAll(String queryAll) {
        return CustomQueryHandler.queryAll(queryAll);
    }

    /**
     * Registers a custom query handler.
     *
     * @param name    query handler name
     * @param handler custom query handler
     */
    public static void register(String name, Handler handler) {
        REGISTRY.register(name, internal(handler));
    }

    /**
     * Unregisters a custom query handler.
     *
     * @param name query handler name
     */
    public static void unregister(String name) {
        REGISTRY.unregister(name);
    }

    /**
     * Clears all custom query handlers.
     */
    public static void clear() {
        REGISTRY.clear();
    }

    /**
     * Returns custom query handler names.
     *
     * @return custom query handler names
     */
    public static List<String> names() {
        return REGISTRY.names();
    }

    /**
     * Returns registered script fragments.
     *
     * @return registered script fragments
     */
    public static List<String> scripts() {
        return REGISTRY.scripts();
    }

    /**
     * Returns a registered custom query handler.
     *
     * @param name query handler name
     * @return registered custom query handler
     */
    public static Optional<? extends Handler> get(String name) {
        return REGISTRY.get(name);
    }

    /**
     * Converts a public handler into the internal implementation.
     *
     * @param handler public handler
     * @return internal handler
     */
    private static CustomQueryHandler internal(Handler handler) {
        Handler actual = Assert.notNull(handler, "handler");
        if (actual instanceof CustomQueryHandler queryHandler) {
            return queryHandler;
        }
        return new CustomQueryHandler(actual.queryOne().orElse(null), actual.queryAll().orElse(null));
    }

}
