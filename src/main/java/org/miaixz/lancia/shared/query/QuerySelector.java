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

import org.miaixz.lancia.nimble.PollingMode;

/**
 * Parsed selector and query handler metadata.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class QuerySelector {

    /**
     * Selector type.
     */
    private final QueryHandler.Type type;
    /**
     * Handler selector body.
     */
    private final String selector;
    /**
     * Original selector.
     */
    private final String rawSelector;
    /**
     * Custom handler name.
     */
    private final String customName;
    /**
     * Default polling.
     */
    private final PollingMode polling;
    /**
     * Query handler name.
     */
    private final String queryHandlerName;

    /**
     * Creates a parsed selector.
     *
     * @param type        type
     * @param selector    selector
     * @param rawSelector raw selector
     */
    public QuerySelector(QueryHandler.Type type, String selector, String rawSelector) {
        this(type, selector, rawSelector, null);
    }

    /**
     * Creates a parsed selector.
     *
     * @param type        type
     * @param selector    selector
     * @param rawSelector raw selector
     * @param customName  custom name
     */
    public QuerySelector(QueryHandler.Type type, String selector, String rawSelector, String customName) {
        this(type, selector, rawSelector, customName, defaultPolling(type), defaultQueryHandlerName(type, customName));
    }

    /**
     * Creates a parsed selector.
     *
     * @param type        type
     * @param selector    selector
     * @param rawSelector raw selector
     * @param customName  custom name
     * @param polling     polling
     */
    public QuerySelector(QueryHandler.Type type, String selector, String rawSelector, String customName,
            PollingMode polling) {
        this(type, selector, rawSelector, customName, polling, defaultQueryHandlerName(type, customName));
    }

    /**
     * Creates a parsed selector.
     *
     * @param type             type
     * @param selector         selector
     * @param rawSelector      raw selector
     * @param customName       custom name
     * @param polling          polling
     * @param queryHandlerName query handler name
     */
    public QuerySelector(QueryHandler.Type type, String selector, String rawSelector, String customName,
            PollingMode polling, String queryHandlerName) {
        this.type = type == null ? QueryHandler.Type.CSS : type;
        this.selector = selector == null ? "" : selector;
        this.rawSelector = rawSelector == null ? this.selector : rawSelector;
        this.customName = customName;
        this.polling = polling == null ? defaultPolling(this.type) : polling;
        this.queryHandlerName = queryHandlerName == null ? defaultQueryHandlerName(this.type, customName)
                : queryHandlerName;
    }

    /**
     * Returns selector type.
     *
     * @return type
     */
    public QueryHandler.Type type() {
        return type;
    }

    /**
     * Returns selector body.
     *
     * @return selector
     */
    public String selector() {
        return selector;
    }

    /**
     * Returns original selector.
     *
     * @return original selector
     */
    public String rawSelector() {
        return rawSelector;
    }

    /**
     * Returns custom handler name.
     *
     * @return custom handler name
     */
    public String customName() {
        return customName;
    }

    /**
     * Returns default polling.
     *
     * @return polling
     */
    public PollingMode polling() {
        return polling;
    }

    /**
     * Returns query handler name.
     *
     * @return query handler name
     */
    public String queryHandlerName() {
        return queryHandlerName;
    }

    /**
     * Builds a query-one expression.
     *
     * @param rootExpression root expression
     * @return expression
     */
    public String queryOneExpression(String rootExpression) {
        return QueryHandler.queryOneExpression(this, rootExpression);
    }

    /**
     * Builds a query-all expression.
     *
     * @param rootExpression root expression
     * @return expression
     */
    public String queryAllExpression(String rootExpression) {
        return QueryHandler.queryAllExpression(this, rootExpression);
    }

    /**
     * Returns default polling for a selector type.
     *
     * @param type selector type
     * @return polling
     */
    private static PollingMode defaultPolling(QueryHandler.Type type) {
        return type == QueryHandler.Type.ARIA ? PollingMode.RAF : PollingMode.MUTATION;
    }

    /**
     * Returns default query handler name.
     *
     * @param type       selector type
     * @param customName custom name
     * @return query handler name
     */
    private static String defaultQueryHandlerName(QueryHandler.Type type, String customName) {
        return switch (type == null ? QueryHandler.Type.CSS : type) {
            case CSS -> "CSSQueryHandler";
            case XPATH -> "XPathQueryHandler";
            case TEXT -> "TextQueryHandler";
            case ARIA -> "ARIAQueryHandler";
            case PIERCE -> "PierceQueryHandler";
            case CUSTOM -> customName == null ? "CustomQueryHandler" : customName;
            case PQUERY -> "PQueryHandler";
        };
    }

}
