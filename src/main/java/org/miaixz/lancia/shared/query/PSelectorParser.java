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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.xyz.StringKit;

/**
 * Shared Puppeteer selector parser.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class PSelectorParser {

    /**
     * Descendent combinator.
     */
    public static final String DESCENDENT_COMBINATOR = ">>>";
    /**
     * Child combinator.
     */
    public static final String CHILD_COMBINATOR = ">>>>";
    /**
     * Puppeteer pseudo selector prefix.
     */
    private static final String PSEUDO_PREFIX = "::-p-";

    /**
     * Creates a parser.
     */
    private PSelectorParser() {
        // No initialization required.
    }

    /**
     * Parses a Puppeteer selector.
     *
     * @param selector selector
     * @return parse result
     */
    public static ParseResult parse(String selector) {
        return new Parser(selector == null ? Normal.EMPTY : selector).parse();
    }

    /**
     * Unquotes selector text.
     *
     * @param text text
     * @return unquoted text
     */
    public static String unquote(String text) {
        String value = text == null ? Normal.EMPTY : text;
        if (value.length() <= 1) {
            return value;
        }
        char quote = value.charAt(Normal._0);
        if ((quote == Symbol.C_DOUBLE_QUOTES || quote == Symbol.C_SINGLE_QUOTE)
                && value.charAt(value.length() - 1) == quote) {
            value = value.substring(1, value.length() - 1);
        }
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        for (int i = Normal._0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (escaped) {
                builder.append(current);
                escaped = false;
            } else if (current == Symbol.C_BACKSLASH) {
                escaped = true;
            } else {
                builder.append(current);
            }
        }
        if (escaped) {
            builder.append(Symbol.C_BACKSLASH);
        }
        return builder.toString();
    }

    /**
     * Parser implementation.
     */
    private static final class Parser {

        /**
         * Selector text.
         */
        private final String selector;
        /**
         * Complex selectors.
         */
        private final List<Object> selectors = new ArrayList<>();
        /**
         * Current complex selector.
         */
        private List<Object> complexSelector = new ArrayList<>();
        /**
         * Current compound selector.
         */
        private List<Object> compoundSelector = new ArrayList<>();
        /**
         * Current CSS buffer.
         */
        private final StringBuilder css = new StringBuilder();
        /**
         * Whether this selector is pure CSS.
         */
        private boolean pureCss = true;
        /**
         * Whether CSS pseudo-classes exist.
         */
        private boolean hasPseudoClasses;
        /**
         * Whether ARIA pseudo selectors exist.
         */
        private boolean hasAria;

        /**
         * Creates a parser.
         *
         * @param selector selector
         */
        private Parser(String selector) {
            this.selector = selector;
        }

        /**
         * Parses this selector.
         *
         * @return result
         */
        private ParseResult parse() {
            char quote = Normal._0;
            boolean escaped = false;
            int squareDepth = Normal._0;
            int parenthesisDepth = Normal._0;
            for (int index = Normal._0; index < selector.length(); index++) {
                char current = selector.charAt(index);
                if (escaped) {
                    css.append(current);
                    escaped = false;
                    continue;
                }
                if (current == Symbol.C_BACKSLASH) {
                    css.append(current);
                    escaped = true;
                    continue;
                }
                if (quote != Normal._0) {
                    css.append(current);
                    if (current == quote) {
                        quote = Normal._0;
                    }
                    continue;
                }
                if (current == Symbol.C_DOUBLE_QUOTES || current == Symbol.C_SINGLE_QUOTE) {
                    css.append(current);
                    quote = current;
                    continue;
                }
                if (squareDepth == Normal._0 && parenthesisDepth == Normal._0
                        && selector.startsWith(PSEUDO_PREFIX, index)) {
                    index = parsePseudo(index);
                    continue;
                }
                if (squareDepth == Normal._0 && parenthesisDepth == Normal._0
                        && selector.startsWith(CHILD_COMBINATOR, index)) {
                    addCombinator(CHILD_COMBINATOR);
                    index += CHILD_COMBINATOR.length() - 1;
                    continue;
                }
                if (squareDepth == Normal._0 && parenthesisDepth == Normal._0
                        && selector.startsWith(DESCENDENT_COMBINATOR, index)) {
                    addCombinator(DESCENDENT_COMBINATOR);
                    index += DESCENDENT_COMBINATOR.length() - 1;
                    continue;
                }
                if (squareDepth == Normal._0 && parenthesisDepth == Normal._0 && current == Symbol.C_COMMA) {
                    finishComplexSelector();
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
                } else if (current == Symbol.C_COLON && QueryHandler.isPseudoClassColon(selector, index)) {
                    hasPseudoClasses = true;
                }
                css.append(current);
            }
            finishComplexSelector();
            return new ParseResult(List.copyOf(selectors), pureCss, hasPseudoClasses, hasAria);
        }

        /**
         * Parses one Puppeteer pseudo selector.
         *
         * @param start start index
         * @return end index
         */
        private int parsePseudo(int start) {
            pureCss = false;
            flushCss();
            int nameStart = start + PSEUDO_PREFIX.length();
            int open = selector.indexOf(Symbol.C_PARENTHESE_LEFT, nameStart);
            if (open < Normal._0) {
                css.append(selector.substring(start));
                return selector.length();
            }
            int close = closingParenthesis(open);
            if (close < Normal._0) {
                css.append(selector.substring(start));
                return selector.length();
            }
            String name = selector.substring(nameStart, open);
            if ("aria".equals(name)) {
                hasAria = true;
            }
            Map<String, Object> pseudo = new LinkedHashMap<>();
            pseudo.put("name", name);
            pseudo.put("value", unquote(selector.substring(open + 1, close).trim()));
            compoundSelector.add(pseudo);
            return close;
        }

        /**
         * Adds a combinator.
         *
         * @param combinator combinator
         */
        private void addCombinator(String combinator) {
            pureCss = false;
            flushCss();
            if (!compoundSelector.isEmpty()) {
                complexSelector.add(compoundSelector);
            }
            complexSelector.add(combinator);
            compoundSelector = new ArrayList<>();
        }

        /**
         * Finishes the current complex selector.
         */
        private void finishComplexSelector() {
            flushCss();
            if (!compoundSelector.isEmpty() || complexSelector.isEmpty()) {
                complexSelector.add(compoundSelector);
            }
            selectors.add(complexSelector);
            complexSelector = new ArrayList<>();
            compoundSelector = new ArrayList<>();
        }

        /**
         * Flushes buffered CSS.
         */
        private void flushCss() {
            String value = css.toString();
            if (StringKit.isNotBlank(value)) {
                compoundSelector.add(value);
            }
            css.setLength(Normal._0);
        }

        /**
         * Finds closing parenthesis.
         *
         * @param open open index
         * @return close index
         */
        private int closingParenthesis(int open) {
            char quote = Normal._0;
            boolean escaped = false;
            int depth = Normal._0;
            for (int index = open; index < selector.length(); index++) {
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
                if (current == Symbol.C_PARENTHESE_LEFT) {
                    depth++;
                } else if (current == Symbol.C_PARENTHESE_RIGHT) {
                    depth--;
                    if (depth == Normal._0) {
                        return index;
                    }
                }
            }
            return -1;
        }
    }

    /**
     * Puppeteer selector parse result.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class ParseResult {

        /**
         * Parsed selectors.
         */
        private final List<Object> selectors;
        /**
         * Whether selector is pure CSS.
         */
        private final boolean pureCss;
        /**
         * Whether CSS pseudo-classes exist.
         */
        private final boolean hasPseudoClasses;
        /**
         * Whether ARIA pseudo selector exists.
         */
        private final boolean hasAria;

        /**
         * Creates a parse result.
         *
         * @param selectors        selectors
         * @param pureCss          pure CSS
         * @param hasPseudoClasses has pseudo-classes
         * @param hasAria          has ARIA
         */
        public ParseResult(List<Object> selectors, boolean pureCss, boolean hasPseudoClasses, boolean hasAria) {
            this.selectors = selectors == null ? List.of() : List.copyOf(selectors);
            this.pureCss = pureCss;
            this.hasPseudoClasses = hasPseudoClasses;
            this.hasAria = hasAria;
        }

        /**
         * Returns selectors.
         *
         * @return selectors
         */
        public List<Object> selectors() {
            return selectors;
        }

        /**
         * Returns whether selector is pure CSS.
         *
         * @return whether pure CSS
         */
        public boolean isPureCss() {
            return pureCss;
        }

        /**
         * Returns whether CSS pseudo-classes exist.
         *
         * @return whether pseudo-classes exist
         */
        public boolean hasPseudoClasses() {
            return hasPseudoClasses;
        }

        /**
         * Returns whether ARIA pseudo selector exists.
         *
         * @return whether ARIA exists
         */
        public boolean hasAria() {
            return hasAria;
        }

        /**
         * Converts selectors to JSON.
         *
         * @return JSON
         */
        public String toJson() {
            return QueryHandler.literal(selectors);
        }
    }

}
