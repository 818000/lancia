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

import java.util.function.Supplier;

import org.miaixz.bus.core.lang.Assert;

/**
 * Represents a locator backed by a resolver function.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class FunctionLocator extends ElementLocator {

    /**
     * Current resolver.
     */
    private final Supplier<ElementLocator> resolver;

    /**
     * Creates a function locator.
     *
     * @param resolver locator resolver
     */
    public FunctionLocator(Supplier<ElementLocator> resolver) {
        super(resolve(resolver));
        this.resolver = Assert.notNull(resolver, "resolver");
    }

    /**
     * Creates a function locator.
     *
     * @param source source locator
     */
    public FunctionLocator(ElementLocator source) {
        super(source);
        this.resolver = () -> source;
    }

    /**
     * Resolves the current locator.
     *
     * @return resolved locator
     */
    public ElementLocator locate() {
        return resolve(resolver);
    }

    /**
     * Resolves a locator from a supplier.
     *
     * @param resolver locator resolver
     * @return resolved locator
     */
    private static ElementLocator resolve(Supplier<ElementLocator> resolver) {
        return Assert.notNull(Assert.notNull(resolver, "resolver").get(), "locator");
    }

}
