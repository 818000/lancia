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

import java.util.List;

import org.miaixz.bus.core.lang.Optional;
import org.miaixz.lancia.kernel.Element;

/**
 * Shared text query handler.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class TextQueryHandler {

    /**
     * Creates a text query handler.
     */
    private TextQueryHandler() {
        // No initialization required.
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
        return QueryHandler.lanciaBlock(
                "for(const node of Lancia.textQuerySelectorAll(" + QueryHandler.root(rootExpression) + ","
                        + QueryHandler.literal(QueryHandler.assertSelector(selector, "text"))
                        + ")){return node;}return null;");
    }

    /**
     * Builds a query-all expression.
     *
     * @param rootExpression root expression
     * @param selector       selector
     * @return expression
     */
    public static String querySelectorAllExpression(String rootExpression, String selector) {
        return QueryHandler.lanciaExpression(
                "Array.from(Lancia.textQuerySelectorAll(" + QueryHandler.root(rootExpression) + ","
                        + QueryHandler.literal(QueryHandler.assertSelector(selector, "text")) + "))");
    }

}
