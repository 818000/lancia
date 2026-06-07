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
package org.miaixz.lancia.shared.locator;

import java.util.function.Predicate;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.lancia.kernel.Element;

/**
 * Represents a locator filtered by an element predicate.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class FilteredLocator extends DelegatedLocator {

    /**
     * Current predicate.
     */
    private final Predicate<? super Element> predicate;

    /**
     * Creates a filtered locator.
     *
     * @param delegate  delegate locator
     * @param predicate element predicate
     */
    public FilteredLocator(ElementLocator delegate, Predicate<? super Element> predicate) {
        super(filter(delegate, predicate));
        this.predicate = Assert.notNull(predicate, "predicate");
    }

    /**
     * Returns the element predicate.
     *
     * @return element predicate
     */
    public Predicate<? super Element> predicate() {
        return predicate;
    }

    /**
     * Applies a predicate to a locator.
     *
     * @param delegate  delegate locator
     * @param predicate element predicate
     * @return filtered locator
     */
    private static ElementLocator filter(ElementLocator delegate, Predicate<? super Element> predicate) {
        return Assert.notNull(delegate, "delegate").filter(Assert.notNull(predicate, "predicate"));
    }

}
