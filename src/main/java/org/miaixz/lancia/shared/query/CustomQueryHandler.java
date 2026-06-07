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
package org.miaixz.lancia.shared.query;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Handler;
import org.miaixz.lancia.kernel.Element;
import org.miaixz.lancia.nimble.PollingMode;

/**
 * Shared custom query handler and registry.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class CustomQueryHandler implements Handler {

    /**
     * Valid custom handler names.
     */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z]+$");
    /**
     * Shared registry.
     */
    private static final Registry REGISTRY = new Registry();
    /**
     * Shared script injector.
     */
    private static final ScriptInjector SCRIPT_INJECTOR = ScriptInjector.shared();
    /**
     * Single-element query function source.
     */
    private final String queryOne;
    /**
     * Multiple-element query function source.
     */
    private final String queryAll;

    /**
     * Creates a custom query handler.
     *
     * @param queryOne single-element query function source
     * @param queryAll multiple-element query function source
     */
    public CustomQueryHandler(String queryOne, String queryAll) {
        this.queryOne = blankToNull(queryOne);
        this.queryAll = blankToNull(queryAll);
        if (this.queryOne == null && this.queryAll == null) {
            throw new IllegalArgumentException("At least one query method must be implemented.");
        }
    }

    /**
     * Creates a single-element custom query handler.
     *
     * @param queryOne query function source
     * @return handler
     */
    public static CustomQueryHandler queryOne(String queryOne) {
        return new CustomQueryHandler(queryOne, null);
    }

    /**
     * Creates a multiple-element custom query handler.
     *
     * @param queryAll query function source
     * @return handler
     */
    public static CustomQueryHandler queryAll(String queryAll) {
        return new CustomQueryHandler(null, queryAll);
    }

    /**
     * Returns the shared registry.
     *
     * @return registry
     */
    public static Registry registry() {
        return REGISTRY;
    }

    /**
     * Returns the shared script injector.
     *
     * @return injector
     */
    public static ScriptInjector scriptInjector() {
        return SCRIPT_INJECTOR;
    }

    /**
     * Registers a custom query handler.
     *
     * @param name    name
     * @param handler handler
     */
    public static void register(String name, CustomQueryHandler handler) {
        REGISTRY.register(name, handler);
    }

    /**
     * Unregisters a custom query handler.
     *
     * @param name name
     */
    public static void unregister(String name) {
        REGISTRY.unregister(name);
    }

    /**
     * Clears all custom handlers.
     */
    public static void clear() {
        REGISTRY.clear();
    }

    /**
     * Returns custom handler names.
     *
     * @return names
     */
    public static List<String> names() {
        return REGISTRY.names();
    }

    /**
     * Returns a custom handler.
     *
     * @param name name
     * @return handler
     */
    public static Optional<CustomQueryHandler> get(String name) {
        return REGISTRY.get(name);
    }

    /**
     * Parses a custom selector.
     *
     * @param selector    selector
     * @param rawSelector raw selector
     * @return parsed selector
     */
    public static Optional<QuerySelector> parse(String selector, String rawSelector) {
        int separator = selectorSeparatorIndex(selector);
        if (separator <= Normal._0) {
            return Optional.empty();
        }
        String name = selector.substring(Normal._0, separator);
        if (!REGISTRY.get(name).isPresent()) {
            return Optional.empty();
        }
        return Optional.of(
                new QuerySelector(QueryHandler.Type.CUSTOM, selector.substring(separator + 1), rawSelector, name,
                        PollingMode.MUTATION, name));
    }

    /**
     * Queries one element.
     *
     * @param element  root element
     * @param name     handler name
     * @param selector selector
     * @return matching element
     */
    public static Optional<Element> queryOne(Element element, String name, String selector) {
        return QueryHandler.queryOneByExpression(element, querySelectorExpression(name, "this", selector));
    }

    /**
     * Queries all elements.
     *
     * @param element  root element
     * @param name     handler name
     * @param selector selector
     * @return matching elements
     */
    public static List<Element> queryAll(Element element, String name, String selector) {
        return QueryHandler.queryAllByExpression(element, querySelectorAllExpression(name, "this", selector));
    }

    /**
     * Builds a query-one expression.
     *
     * @param name           handler name
     * @param rootExpression root expression
     * @param selector       selector
     * @return expression
     */
    public static String querySelectorExpression(String name, String rootExpression, String selector) {
        CustomQueryHandler handler = REGISTRY.get(name)
                .orElseThrow(() -> new IllegalArgumentException("Cannot find custom query handler: " + name));
        String root = QueryHandler.root(rootExpression);
        String literal = QueryHandler.literal(selector);
        String handlerName = QueryHandler.literal(name);
        return QueryHandler.lanciaBlock(
                registerHandler(handlerName, handler) + "return Lancia.customQuerySelectors.get(" + handlerName
                        + ").querySelector(" + root + "," + literal + ");");
    }

    /**
     * Builds a query-all expression.
     *
     * @param name           handler name
     * @param rootExpression root expression
     * @param selector       selector
     * @return expression
     */
    public static String querySelectorAllExpression(String name, String rootExpression, String selector) {
        CustomQueryHandler handler = REGISTRY.get(name)
                .orElseThrow(() -> new IllegalArgumentException("Cannot find custom query handler: " + name));
        String root = QueryHandler.root(rootExpression);
        String literal = QueryHandler.literal(selector);
        String handlerName = QueryHandler.literal(name);
        return QueryHandler.lanciaBlock(
                registerHandler(handlerName, handler) + "return Array.from(Lancia.customQuerySelectors.get("
                        + handlerName + ").querySelectorAll(" + root + "," + literal + "));");
    }

    /**
     * Returns query-one source.
     *
     * @return source
     */
    public String queryOneSource() {
        return queryOne;
    }

    /**
     * Returns query-one source.
     *
     * @return source
     */
    @Override
    public Optional<String> queryOne() {
        return Optional.ofNullable(queryOne);
    }

    /**
     * Returns query-all source.
     *
     * @return source
     */
    public String queryAllSource() {
        return queryAll;
    }

    /**
     * Returns query-all source.
     *
     * @return source
     */
    @Override
    public Optional<String> queryAll() {
        return Optional.ofNullable(queryAll);
    }

    /**
     * Converts blank text to null.
     *
     * @param value value
     * @return normalized value
     */
    private static String blankToNull(String value) {
        return StringKit.isBlank(value) ? null : value.trim();
    }

    /**
     * Returns selector separator index.
     *
     * @param selector selector
     * @return separator index
     */
    private static int selectorSeparatorIndex(String selector) {
        int slash = selector.indexOf(Symbol.C_SLASH);
        int equals = selector.indexOf(Symbol.C_EQUAL);
        if (slash < Normal._0) {
            return equals;
        }
        if (equals < Normal._0) {
            return slash;
        }
        return Math.min(slash, equals);
    }

    /**
     * Builds registration amendment.
     *
     * @param name    name
     * @param handler handler
     * @return script
     */
    private static String registerScript(String name, CustomQueryHandler handler) {
        return "(Lancia)=>{" + registerHandler(QueryHandler.literal(name), handler) + "}";
    }

    /**
     * Builds registration statement.
     *
     * @param name    JavaScript literal name
     * @param handler handler
     * @return statement
     */
    private static String registerHandler(String name, CustomQueryHandler handler) {
        String queryAll = handler.queryAll == null ? "undefined" : handler.queryAll;
        String queryOne = handler.queryOne == null ? "undefined" : handler.queryOne;
        return "Lancia.customQuerySelectors.register(" + name + ",{queryAll:" + queryAll + ",queryOne:" + queryOne
                + "});";
    }

    /**
     * Custom query handler registry.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class Registry {

        /**
         * Registered handlers.
         */
        private final Map<String, RegisteredHandler> handlers = new LinkedHashMap<>();

        /**
         * Creates a registry.
         */
        public Registry() {
            // No initialization required.
        }

        /**
         * Returns a handler.
         *
         * @param name name
         * @return handler
         */
        public synchronized Optional<CustomQueryHandler> get(String name) {
            RegisteredHandler registered = handlers.get(name);
            return registered == null ? Optional.empty() : Optional.of(registered.handler());
        }

        /**
         * Registers a handler.
         *
         * @param name    name
         * @param handler handler
         */
        public synchronized void register(String name, CustomQueryHandler handler) {
            String actualName = Assert.notBlank(name, "name");
            if (handlers.containsKey(actualName)) {
                throw new IllegalArgumentException("Cannot register over existing handler: " + actualName);
            }
            if (!NAME_PATTERN.matcher(actualName).matches()) {
                throw new IllegalArgumentException("Custom query handler names may only contain [a-zA-Z]");
            }
            CustomQueryHandler actualHandler = Assert.notNull(handler, "handler");
            String script = registerScript(actualName, actualHandler);
            handlers.put(actualName, new RegisteredHandler(script, actualHandler));
            SCRIPT_INJECTOR.append(script);
        }

        /**
         * Unregisters a handler.
         *
         * @param name name
         */
        public synchronized void unregister(String name) {
            RegisteredHandler removed = handlers.remove(name);
            if (removed == null) {
                throw new IllegalArgumentException("Cannot unregister unknown handler: " + name);
            }
            SCRIPT_INJECTOR.pop(removed.script());
        }

        /**
         * Returns handler names.
         *
         * @return names
         */
        public synchronized List<String> names() {
            return List.copyOf(handlers.keySet());
        }

        /**
         * Returns registration scripts.
         *
         * @return scripts
         */
        public synchronized List<String> scripts() {
            return SCRIPT_INJECTOR.amendments();
        }

        /**
         * Clears handlers.
         */
        public synchronized void clear() {
            for (RegisteredHandler handler : handlers.values()) {
                SCRIPT_INJECTOR.pop(handler.script());
            }
            handlers.clear();
        }
    }

    /**
     * Registered custom handler.
     */
    private static final class RegisteredHandler {

        /**
         * Registration script.
         */
        private final String script;
        /**
         * Handler.
         */
        private final CustomQueryHandler handler;

        /**
         * Creates a registered handler.
         *
         * @param script  script
         * @param handler handler
         */
        private RegisteredHandler(String script, CustomQueryHandler handler) {
            this.script = script;
            this.handler = handler;
        }

        /**
         * Returns script.
         *
         * @return script
         */
        private String script() {
            return script;
        }

        /**
         * Returns handler.
         *
         * @return handler
         */
        private CustomQueryHandler handler() {
            return handler;
        }
    }

}
