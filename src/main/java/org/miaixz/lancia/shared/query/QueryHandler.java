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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.kernel.Element;
import org.miaixz.lancia.kernel.Frame;
import org.miaixz.lancia.kernel.Handle;
import org.miaixz.lancia.nimble.PollingMode;
import org.miaixz.lancia.options.WaitForSelectorOptions;
import org.miaixz.lancia.shared.runtime.HandleIterator;

/**
 * Shared selector query handler aligned with Puppeteer's common query layer.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class QueryHandler {

    /**
     * Built-in non-CSS query handlers.
     */
    private static final List<BuiltinHandler> BUILTIN_QUERY_HANDLERS = List.of(
            new BuiltinHandler("aria", Type.ARIA, PollingMode.RAF),
            new BuiltinHandler("pierce", Type.PIERCE, PollingMode.MUTATION),
            new BuiltinHandler("xpath", Type.XPATH, PollingMode.MUTATION),
            new BuiltinHandler("text", Type.TEXT, PollingMode.MUTATION));
    /**
     * Prefix separators accepted by Puppeteer selectors.
     */
    private static final List<String> QUERY_SEPARATORS = List.of(Symbol.EQUAL, Symbol.SLASH);

    /**
     * Creates a query handler.
     */
    private QueryHandler() {
        // No initialization required.
    }

    /**
     * Parses a selector and resolves the matching shared query handler.
     *
     * @param selector selector
     * @return parsed selector
     */
    public static QuerySelector parse(String selector) {
        if (StringKit.isBlank(selector)) {
            throw new IllegalArgumentException("Selector must not be blank.");
        }
        String trimmed = selector.trim();
        Optional<QuerySelector> customSelector = CustomQueryHandler.parse(trimmed, selector);
        if (customSelector.isPresent()) {
            return customSelector.orElseThrow();
        }
        for (BuiltinHandler handler : BUILTIN_QUERY_HANDLERS) {
            Optional<String> body = bodyForNamedSelector(trimmed, handler.name());
            if (body.isPresent()) {
                return new QuerySelector(handler.type(), body.orElseThrow(), selector, null, handler.polling(),
                        handler.queryHandlerName());
            }
        }
        Optional<QuerySelector> pSelector = parsePSelector(trimmed, selector);
        if (pSelector.isPresent()) {
            return pSelector.orElseThrow();
        }
        if (trimmed.startsWith("//") || trimmed.startsWith(".//")) {
            return new QuerySelector(Type.XPATH, trimmed, selector);
        }
        PollingMode polling = hasCssPseudoClass(trimmed) ? PollingMode.RAF : PollingMode.MUTATION;
        return new QuerySelector(Type.CSS, trimmed, selector, null, polling);
    }

    /**
     * Queries one element under an element root.
     *
     * @param element  root element
     * @param selector selector
     * @return matching element
     */
    public static Optional<Element> queryOne(Element element, String selector) {
        return queryOne(Assert.notNull(element, "element"), parse(selector));
    }

    /**
     * Queries all elements under an element root.
     *
     * @param element  root element
     * @param selector selector
     * @return matching elements
     */
    public static List<Element> queryAll(Element element, String selector) {
        return queryAll(Assert.notNull(element, "element"), parse(selector));
    }

    /**
     * Queries one element in a frame.
     *
     * @param frame    frame
     * @param selector selector
     * @return matching element
     */
    public static Optional<Element> queryOne(Frame frame, String selector) {
        return queryOne(Assert.notNull(frame, "frame"), parse(selector));
    }

    /**
     * Queries all elements in a frame.
     *
     * @param frame    frame
     * @param selector selector
     * @return matching elements
     */
    public static List<Element> queryAll(Frame frame, String selector) {
        return queryAll(Assert.notNull(frame, "frame"), parse(selector));
    }

    /**
     * Waits for one matching element under an element root.
     *
     * @param element  root element
     * @param selector selector
     * @param options  wait options
     * @return matching element
     */
    public static Optional<Element> waitFor(Element element, String selector, WaitForSelectorOptions options) {
        QuerySelector querySelector = parse(selector);
        WaitForSelectorOptions actualOptions = options == null ? new WaitForSelectorOptions() : options;
        return waitLoop(selector, actualOptions, querySelector.polling(), () -> queryOne(element, querySelector));
    }

    /**
     * Waits for one matching element in a frame.
     *
     * @param frame    frame
     * @param selector selector
     * @param options  wait options
     * @return matching element
     */
    public static Optional<Element> waitFor(Frame frame, String selector, WaitForSelectorOptions options) {
        QuerySelector querySelector = parse(selector);
        WaitForSelectorOptions actualOptions = options == null ? new WaitForSelectorOptions() : options;
        return waitLoop(selector, actualOptions, querySelector.polling(), () -> queryOne(frame, querySelector));
    }

    /**
     * Builds a query-one expression for a parsed selector.
     *
     * @param querySelector  parsed selector
     * @param rootExpression root expression
     * @return JavaScript expression
     */
    public static String queryOneExpression(QuerySelector querySelector, String rootExpression) {
        QuerySelector actualSelector = Assert.notNull(querySelector, "querySelector");
        String root = root(rootExpression);
        return switch (actualSelector.type()) {
            case CSS -> CSSQueryHandler.querySelectorExpression(root, actualSelector.selector());
            case XPATH -> XPathQueryHandler.querySelectorExpression(root, actualSelector.selector());
            case TEXT -> TextQueryHandler.querySelectorExpression(root, actualSelector.selector());
            case ARIA -> AriaQueryHandler.querySelectorExpression(root, actualSelector.selector());
            case PIERCE -> PierceQueryHandler.querySelectorExpression(root, actualSelector.selector());
            case CUSTOM -> CustomQueryHandler
                    .querySelectorExpression(actualSelector.customName(), root, actualSelector.selector());
            case PQUERY -> PQueryHandler.querySelectorExpression(root, actualSelector.selector());
        };
    }

    /**
     * Builds a query-all expression for a parsed selector.
     *
     * @param querySelector  parsed selector
     * @param rootExpression root expression
     * @return JavaScript expression
     */
    public static String queryAllExpression(QuerySelector querySelector, String rootExpression) {
        QuerySelector actualSelector = Assert.notNull(querySelector, "querySelector");
        String root = root(rootExpression);
        return switch (actualSelector.type()) {
            case CSS -> CSSQueryHandler.querySelectorAllExpression(root, actualSelector.selector());
            case XPATH -> XPathQueryHandler.querySelectorAllExpression(root, actualSelector.selector());
            case TEXT -> TextQueryHandler.querySelectorAllExpression(root, actualSelector.selector());
            case ARIA -> AriaQueryHandler.querySelectorAllExpression(root, actualSelector.selector());
            case PIERCE -> PierceQueryHandler.querySelectorAllExpression(root, actualSelector.selector());
            case CUSTOM -> CustomQueryHandler
                    .querySelectorAllExpression(actualSelector.customName(), root, actualSelector.selector());
            case PQUERY -> PQueryHandler.querySelectorAllExpression(root, actualSelector.selector());
        };
    }

    /**
     * Asserts a selector string.
     *
     * @param selector selector
     * @param type     selector type
     * @return selector
     */
    static String assertSelector(String selector, String type) {
        if (StringKit.isBlank(selector)) {
            String prefix = StringKit.isBlank(type) ? Normal.EMPTY : type + Symbol.SPACE;
            throw new IllegalArgumentException(prefix + "Selector must not be blank.");
        }
        return selector;
    }

    /**
     * Wraps a selector expression with the injected runtime.
     *
     * @param expression expression
     * @return wrapped expression
     */
    static String lanciaExpression(String expression) {
        return ScriptInjector.expression(expression);
    }

    /**
     * Wraps an async selector expression with the injected runtime.
     *
     * @param expression expression
     * @return wrapped expression
     */
    static String asyncLanciaExpression(String expression) {
        return ScriptInjector.asyncExpression(expression);
    }

    /**
     * Wraps a selector block with the injected runtime.
     *
     * @param body body
     * @return wrapped expression
     */
    static String lanciaBlock(String body) {
        return ScriptInjector.block(body);
    }

    /**
     * Wraps an async selector block with the injected runtime.
     *
     * @param body body
     * @return wrapped expression
     */
    static String asyncLanciaBlock(String body) {
        return ScriptInjector.asyncBlock(body);
    }

    /**
     * Returns a root expression.
     *
     * @param rootExpression root expression
     * @return root expression
     */
    static String root(String rootExpression) {
        return StringKit.isBlank(rootExpression) ? "document" : rootExpression;
    }

    /**
     * Converts a Java value into a JavaScript literal.
     *
     * @param value value
     * @return JavaScript literal
     */
    static String literal(Object value) {
        StringBuilder builder = new StringBuilder();
        writeJson(value, builder);
        return builder.toString();
    }

    /**
     * Queries one element under an element root using a prepared expression.
     *
     * @param element    root element
     * @param expression expression
     * @return matching element
     */
    static Optional<Element> queryOneByExpression(Element element, String expression) {
        return elementFromHandle(Assert.notNull(element, "element").evaluateHandle(function(expression)));
    }

    /**
     * Queries all elements under an element root using a prepared expression.
     *
     * @param element    root element
     * @param expression expression
     * @return matching elements
     */
    static List<Element> queryAllByExpression(Element element, String expression) {
        return elementsFromHandle(Assert.notNull(element, "element").evaluateHandle(function(expression)));
    }

    /**
     * Returns named-selector body.
     *
     * @param selector selector
     * @param name     handler name
     * @return selector body
     */
    static Optional<String> bodyForNamedSelector(String selector, String name) {
        for (String separator : QUERY_SEPARATORS) {
            String prefix = name + separator;
            if (selector.startsWith(prefix)) {
                return Optional.of(selector.substring(prefix.length()));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns whether a colon starts a CSS pseudo-class.
     *
     * @param selector selector
     * @param index    index
     * @return whether pseudo-class colon
     */
    static boolean isPseudoClassColon(String selector, int index) {
        if (selector == null || index < Normal._0 || index >= selector.length()
                || selector.charAt(index) != Symbol.C_COLON) {
            return false;
        }
        if (index + 1 < selector.length() && selector.charAt(index + 1) == Symbol.C_COLON) {
            return false;
        }
        return index == Normal._0 || selector.charAt(index - 1) != Symbol.C_COLON;
    }

    /**
     * Queries one element under an element root.
     *
     * @param element       root element
     * @param querySelector parsed selector
     * @return matching element
     */
    private static Optional<Element> queryOne(Element element, QuerySelector querySelector) {
        return queryOneByExpression(element, querySelector.queryOneExpression("this"));
    }

    /**
     * Queries all elements under an element root.
     *
     * @param element       root element
     * @param querySelector parsed selector
     * @return matching elements
     */
    private static List<Element> queryAll(Element element, QuerySelector querySelector) {
        return queryAllByExpression(element, querySelector.queryAllExpression("this"));
    }

    /**
     * Queries one element in a frame.
     *
     * @param frame         frame
     * @param querySelector parsed selector
     * @return matching element
     */
    private static Optional<Element> queryOne(Frame frame, QuerySelector querySelector) {
        return elementFromHandle(frame.evaluateHandle(querySelector.queryOneExpression("document")));
    }

    /**
     * Queries all elements in a frame.
     *
     * @param frame         frame
     * @param querySelector parsed selector
     * @return matching elements
     */
    private static List<Element> queryAll(Frame frame, QuerySelector querySelector) {
        return elementsFromHandle(frame.evaluateHandle(querySelector.queryAllExpression("document")));
    }

    /**
     * Converts a handle into an optional element.
     *
     * @param handle handle
     * @return optional element
     */
    private static Optional<Element> elementFromHandle(Handle handle) {
        if (handle == null) {
            return Optional.empty();
        }
        Element element = handle.asElement();
        if (element == null) {
            handle.dispose();
            return Optional.empty();
        }
        return Optional.of(element);
    }

    /**
     * Converts an iterable/array handle into elements.
     *
     * @param handle handle
     * @return elements
     */
    private static List<Element> elementsFromHandle(Handle handle) {
        if (handle == null) {
            return List.of();
        }
        try {
            List<Element> elements = new ArrayList<>();
            for (Handle item : HandleIterator.toList(handle)) {
                Element element = item == null ? null : item.asElement();
                if (element == null) {
                    if (item != null) {
                        item.dispose();
                    }
                } else {
                    elements.add(element);
                }
            }
            return List.copyOf(elements);
        } finally {
            handle.dispose();
        }
    }

    /**
     * Wraps an expression for handle-level evaluation.
     *
     * @param expression expression
     * @return function declaration
     */
    private static String function(String expression) {
        return "function(){return " + expression + ";}";
    }

    /**
     * Waits until lookup result matches visibility options.
     *
     * @param selector       selector
     * @param options        options
     * @param defaultPolling default polling
     * @param lookup         lookup
     * @return matching element
     */
    private static Optional<Element> waitLoop(
            String selector,
            WaitForSelectorOptions options,
            PollingMode defaultPolling,
            ElementLookup lookup) {
        long timeoutMillis = options.timeoutMillis();
        long deadline = timeoutMillis <= Normal._0 ? Long.MAX_VALUE
                : System.nanoTime() + java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        long pollingMillis = options.pollingIntervalMillis() > Normal._0 ? options.pollingIntervalMillis()
                : pollingMillis(
                        options.visible() || options.hidden() ? PollingMode.RAF
                                : options.polling() == null ? defaultPolling : options.polling());
        while (true) {
            Optional<Element> handle = lookup.find();
            Optional<Element> accepted = acceptedHandle(handle, options);
            if (accepted.isPresent() || (options.hidden() && handle.isEmpty())) {
                return accepted;
            }
            if (System.nanoTime() >= deadline) {
                throw new TimeoutException("Waiting for selector `" + selector + "` failed");
            }
            sleep(pollingMillis);
        }
    }

    /**
     * Applies visibility constraints.
     *
     * @param handle  handle
     * @param options options
     * @return accepted handle
     */
    private static Optional<Element> acceptedHandle(Optional<Element> handle, WaitForSelectorOptions options) {
        if (handle.isEmpty()) {
            return Optional.empty();
        }
        Element element = handle.orElseThrow();
        if (options.visible()) {
            return element.isVisible() ? Optional.of(element) : Optional.empty();
        }
        if (options.hidden()) {
            return element.isHidden() ? Optional.of(element) : Optional.empty();
        }
        return Optional.of(element);
    }

    /**
     * Returns polling delay.
     *
     * @param polling polling
     * @return delay in milliseconds
     */
    private static long pollingMillis(PollingMode polling) {
        return polling == PollingMode.RAF ? 16L : 50L;
    }

    /**
     * Sleeps during wait loops.
     *
     * @param millis delay
     */
    private static void sleep(long millis) {
        try {
            Thread.sleep(Math.max(1L, millis));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TimeoutException("Waiting for selector interrupted", ex);
        }
    }

    /**
     * Parses a Puppeteer selector.
     *
     * @param selector    selector
     * @param rawSelector raw selector
     * @return parsed selector
     */
    private static Optional<QuerySelector> parsePSelector(String selector, String rawSelector) {
        PSelectorParser.ParseResult result = PSelectorParser.parse(selector);
        if (result.isPureCss()) {
            return Optional.empty();
        }
        PollingMode polling = result.hasAria() ? PollingMode.RAF : PollingMode.MUTATION;
        return Optional
                .of(new QuerySelector(Type.PQUERY, result.toJson(), rawSelector, null, polling, "PQueryHandler"));
    }

    /**
     * Returns whether the selector contains CSS pseudo-classes.
     *
     * @param selector selector
     * @return whether pseudo-classes exist
     */
    private static boolean hasCssPseudoClass(String selector) {
        char quote = Normal._0;
        boolean escaped = false;
        int squareDepth = Normal._0;
        int parenthesisDepth = Normal._0;
        for (int index = Normal._0; index < selector.length(); index++) {
            char current = selector.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == Symbol.C_BACKSLASH) {
                escaped = true;
                continue;
            }
            if (quote != Normal._0) {
                if (current == quote) {
                    quote = Normal._0;
                }
                continue;
            }
            if (current == Symbol.C_DOUBLE_QUOTES || current == Symbol.C_SINGLE_QUOTE) {
                quote = current;
                continue;
            }
            if (current == Symbol.C_BRACKET_LEFT) {
                squareDepth++;
            } else if (current == Symbol.C_BRACKET_RIGHT && squareDepth > Normal._0) {
                squareDepth--;
            } else if (current == Symbol.C_PARENTHESE_LEFT) {
                parenthesisDepth++;
            } else if (current == Symbol.C_PARENTHESE_RIGHT && parenthesisDepth > Normal._0) {
                parenthesisDepth--;
            } else if (squareDepth == Normal._0 && parenthesisDepth == Normal._0
                    && isPseudoClassColon(selector, index)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes JSON.
     *
     * @param value   value
     * @param builder builder
     */
    private static void writeJson(Object value, StringBuilder builder) {
        if (value == null) {
            builder.append(Normal.NULL);
        } else if (value instanceof String text) {
            writeString(text, builder);
        } else if (value instanceof Character character) {
            writeString(String.valueOf(character), builder);
        } else if (value instanceof Boolean bool) {
            builder.append(bool ? Normal.TRUE : Normal.FALSE);
        } else if (value instanceof Number number) {
            builder.append(number);
        } else if (value instanceof Map<?, ?> map) {
            writeMap(map, builder);
        } else if (value instanceof Iterable<?> iterable) {
            writeIterable(iterable, builder);
        } else if (value.getClass().isArray()) {
            writeArray(value, builder);
        } else {
            writeString(String.valueOf(value), builder);
        }
    }

    /**
     * Writes a JSON string.
     *
     * @param value   value
     * @param builder builder
     */
    private static void writeString(String value, StringBuilder builder) {
        builder.append(Symbol.C_DOUBLE_QUOTES);
        for (int index = Normal._0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case Symbol.C_DOUBLE_QUOTES -> builder.append("\\\"");
                case Symbol.C_BACKSLASH -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case Symbol.C_LF -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (current < 0x20) {
                        builder.append(String.format("\\u%04x", (int) current));
                    } else {
                        builder.append(current);
                    }
                }
            }
        }
        builder.append(Symbol.C_DOUBLE_QUOTES);
    }

    /**
     * Writes a JSON map.
     *
     * @param map     map
     * @param builder builder
     */
    private static void writeMap(Map<?, ?> map, StringBuilder builder) {
        builder.append(Symbol.C_BRACE_LEFT);
        Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<?, ?> entry = iterator.next();
            writeString(String.valueOf(entry.getKey()), builder);
            builder.append(Symbol.C_COLON);
            writeJson(entry.getValue(), builder);
            if (iterator.hasNext()) {
                builder.append(Symbol.C_COMMA);
            }
        }
        builder.append(Symbol.C_BRACE_RIGHT);
    }

    /**
     * Writes a JSON iterable.
     *
     * @param iterable iterable
     * @param builder  builder
     */
    private static void writeIterable(Iterable<?> iterable, StringBuilder builder) {
        builder.append(Symbol.C_BRACKET_LEFT);
        Iterator<?> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            writeJson(iterator.next(), builder);
            if (iterator.hasNext()) {
                builder.append(Symbol.C_COMMA);
            }
        }
        builder.append(Symbol.C_BRACKET_RIGHT);
    }

    /**
     * Writes a JSON array.
     *
     * @param value   array
     * @param builder builder
     */
    private static void writeArray(Object value, StringBuilder builder) {
        builder.append(Symbol.C_BRACKET_LEFT);
        int length = Array.getLength(value);
        for (int index = Normal._0; index < length; index++) {
            if (index > Normal._0) {
                builder.append(Symbol.C_COMMA);
            }
            writeJson(Array.get(value, index), builder);
        }
        builder.append(Symbol.C_BRACKET_RIGHT);
    }

    /**
     * Looks up an element during wait loops.
     */
    private interface ElementLookup {

        /**
         * Finds an element.
         *
         * @return element
         */
        Optional<Element> find();
    }

    /**
     * Selector types.
     */
    public enum Type {

        /**
         * CSS selectors.
         */
        CSS,

        /**
         * XPath selectors.
         */
        XPATH,

        /**
         * Text selectors.
         */
        TEXT,

        /**
         * ARIA selectors.
         */
        ARIA,

        /**
         * Pierce selectors.
         */
        PIERCE,

        /**
         * Custom query selectors.
         */
        CUSTOM,

        /**
         * Puppeteer selectors.
         */
        PQUERY
    }

    /**
     * Built-in handler metadata.
     */
    private static final class BuiltinHandler {

        /**
         * Handler name.
         */
        private final String name;
        /**
         * Selector type.
         */
        private final Type type;
        /**
         * Default polling.
         */
        private final PollingMode polling;

        /**
         * Creates a built-in handler.
         *
         * @param name    name
         * @param type    type
         * @param polling polling
         */
        private BuiltinHandler(String name, Type type, PollingMode polling) {
            this.name = name;
            this.type = type;
            this.polling = polling;
        }

        /**
         * Returns handler name.
         *
         * @return name
         */
        private String name() {
            return name;
        }

        /**
         * Returns selector type.
         *
         * @return type
         */
        private Type type() {
            return type;
        }

        /**
         * Returns default polling.
         *
         * @return polling
         */
        private PollingMode polling() {
            return polling;
        }

        /**
         * Returns query handler name.
         *
         * @return query handler name
         */
        private String queryHandlerName() {
            return switch (type) {
                case CSS -> "CSSQueryHandler";
                case XPATH -> "XPathQueryHandler";
                case TEXT -> "TextQueryHandler";
                case ARIA -> "ARIAQueryHandler";
                case PIERCE -> "PierceQueryHandler";
                case CUSTOM -> "CustomQueryHandler";
                case PQUERY -> "PQueryHandler";
            };
        }
    }

}
