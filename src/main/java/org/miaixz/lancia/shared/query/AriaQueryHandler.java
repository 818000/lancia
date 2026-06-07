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
import java.util.Set;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.lancia.kernel.Element;

/**
 * Shared ARIA query handler.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class AriaQueryHandler {

    /**
     * Maximum ARIA selector length.
     */
    private static final int MAX_SELECTOR_LENGTH = 10_000;
    /**
     * Supported ARIA attributes.
     */
    private static final Set<String> KNOWN_ATTRIBUTES = Set.of("name", "role");

    /**
     * Creates an ARIA query handler.
     */
    private AriaQueryHandler() {
        // No initialization required.
    }

    /**
     * Parses an ARIA selector.
     *
     * @param selector selector
     * @return ARIA selector
     */
    public static AriaSelector parse(String selector) {
        String actualSelector = QueryHandler.assertSelector(selector, "ARIA");
        if (actualSelector.length() > MAX_SELECTOR_LENGTH) {
            throw new IllegalArgumentException("Selector " + actualSelector + " is too long");
        }
        AriaSelectorBuilder builder = new AriaSelectorBuilder();
        StringBuilder name = new StringBuilder();
        int index = Normal._0;
        while (index < actualSelector.length()) {
            char current = actualSelector.charAt(index);
            if (current == Symbol.C_BRACKET_LEFT) {
                index = parseAttribute(actualSelector, index, builder);
            } else {
                name.append(current);
                index++;
            }
        }
        if (builder.name() == null && !name.toString().trim().isEmpty()) {
            builder.name(name.toString().trim());
        }
        return builder.build();
    }

    /**
     * Queries one element.
     *
     * @param element  root element
     * @param selector selector
     * @return matching element
     */
    public static Optional<Element> queryOne(Element element, String selector) {
        return QueryHandler.queryOneByExpression(element, querySelectorExpression("this", selector));
    }

    /**
     * Queries all elements.
     *
     * @param element  root element
     * @param selector selector
     * @return matching elements
     */
    public static List<Element> queryAll(Element element, String selector) {
        return QueryHandler.queryAllByExpression(element, querySelectorAllExpression("this", selector));
    }

    /**
     * Builds a query-one expression.
     *
     * @param rootExpression root expression
     * @param selector       selector
     * @return expression
     */
    public static String querySelectorExpression(String rootExpression, String selector) {
        return QueryHandler.asyncLanciaExpression(
                "Lancia.ariaQuerySelector(" + QueryHandler.root(rootExpression) + ","
                        + QueryHandler.literal(QueryHandler.assertSelector(selector, "ARIA")) + ")");
    }

    /**
     * Builds a query-all expression.
     *
     * @param rootExpression root expression
     * @param selector       selector
     * @return expression
     */
    public static String querySelectorAllExpression(String rootExpression, String selector) {
        return QueryHandler.asyncLanciaBlock(
                "const out=[];for await(const node of Lancia.ariaQuerySelectorAll(" + QueryHandler.root(rootExpression)
                        + "," + QueryHandler.literal(QueryHandler.assertSelector(selector, "ARIA"))
                        + ")){out.push(node);}return out;");
    }

    /**
     * Parses one ARIA attribute.
     *
     * @param selector selector
     * @param start    start
     * @param builder  builder
     * @return next index
     */
    private static int parseAttribute(String selector, int start, AriaSelectorBuilder builder) {
        int index = start + 1;
        index = skipWhitespace(selector, index);
        int attributeStart = index;
        while (index < selector.length() && Character.isLetterOrDigit(selector.charAt(index))) {
            index++;
        }
        String attribute = selector.substring(attributeStart, index);
        index = skipWhitespace(selector, index);
        if (index >= selector.length() || selector.charAt(index) != Symbol.C_EQUAL) {
            throw new InternalException("ARIA selector attribute must have a value: " + attribute);
        }
        index++;
        index = skipWhitespace(selector, index);
        if (index >= selector.length() || !isQuote(selector.charAt(index))) {
            throw new InternalException("ARIA selector attribute value must be quoted: " + attribute);
        }
        char quote = selector.charAt(index++);
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        while (index < selector.length()) {
            char current = selector.charAt(index++);
            if (escaped) {
                value.append(current);
                escaped = false;
            } else if (current == Symbol.C_BACKSLASH) {
                escaped = true;
            } else if (current == quote) {
                break;
            } else {
                value.append(current);
            }
        }
        index = skipWhitespace(selector, index);
        if (index >= selector.length() || selector.charAt(index) != Symbol.C_BRACKET_RIGHT) {
            throw new InternalException("ARIA selector attribute must end with ]: " + attribute);
        }
        assignAttribute(builder, attribute, value.toString());
        return index + 1;
    }

    /**
     * Assigns an ARIA attribute.
     *
     * @param builder   builder
     * @param attribute attribute
     * @param value     value
     */
    private static void assignAttribute(AriaSelectorBuilder builder, String attribute, String value) {
        if (!KNOWN_ATTRIBUTES.contains(attribute)) {
            throw new InternalException("Unknown aria attribute \"" + attribute + "\" in selector");
        }
        if ("name".equals(attribute)) {
            builder.name(value);
        } else if ("role".equals(attribute)) {
            builder.role(value);
        }
    }

    /**
     * Skips whitespace.
     *
     * @param selector selector
     * @param index    index
     * @return next index
     */
    private static int skipWhitespace(String selector, int index) {
        int current = index;
        while (current < selector.length() && Character.isWhitespace(selector.charAt(current))) {
            current++;
        }
        return current;
    }

    /**
     * Returns whether character is a quote.
     *
     * @param value character
     * @return whether quote
     */
    private static boolean isQuote(char value) {
        return value == Symbol.C_DOUBLE_QUOTES || value == Symbol.C_SINGLE_QUOTE;
    }

    /**
     * Parsed ARIA selector.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class AriaSelector {

        /**
         * Accessible name.
         */
        private final String name;
        /**
         * ARIA role.
         */
        private final String role;

        /**
         * Creates an ARIA selector.
         *
         * @param name name
         * @param role role
         */
        private AriaSelector(String name, String role) {
            this.name = name;
            this.role = role;
        }

        /**
         * Returns accessible name.
         *
         * @return name
         */
        public String name() {
            return name;
        }

        /**
         * Returns ARIA role.
         *
         * @return role
         */
        public String role() {
            return role;
        }

        /**
         * Converts selector to map.
         *
         * @return mapped selector
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            if (name != null) {
                map.put("name", name);
            }
            if (role != null) {
                map.put("role", role);
            }
            return Map.copyOf(map);
        }
    }

    /**
     * ARIA selector builder.
     */
    private static final class AriaSelectorBuilder {

        /**
         * Accessible name.
         */
        private String name;
        /**
         * ARIA role.
         */
        private String role;

        /**
         * Updates accessible name.
         *
         * @param name name
         */
        private void name(String name) {
            this.name = name;
        }

        /**
         * Returns accessible name.
         *
         * @return name
         */
        private String name() {
            return name;
        }

        /**
         * Updates ARIA role.
         *
         * @param role role
         */
        private void role(String role) {
            this.role = role;
        }

        /**
         * Builds a selector.
         *
         * @return selector
         */
        private AriaSelector build() {
            return new AriaSelector(name, role);
        }
    }

}
