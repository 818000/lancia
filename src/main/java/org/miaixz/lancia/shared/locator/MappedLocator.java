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

import java.util.function.Function;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.lancia.kernel.Element;
import org.miaixz.lancia.kernel.Handle;

/**
 * Represents a locator mapped to a handle value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class MappedLocator extends DelegatedLocator {

    /**
     * Current mapper.
     */
    private final Function<? super Element, ? extends Handle> mapper;

    /**
     * Creates a mapped locator.
     *
     * @param delegate delegate locator
     * @param mapper   element mapper
     */
    public MappedLocator(ElementLocator delegate, Function<? super Element, ? extends Handle> mapper) {
        super(delegate);
        this.mapper = Assert.notNull(mapper, "mapper");
    }

    /**
     * Returns mapper.
     *
     * @return mapper value
     */
    public Function<? super Element, ? extends Handle> mapper() {
        return mapper;
    }

    /**
     * Resolves and maps the current element handle.
     *
     * @return mapped handle
     */
    public Handle mappedValue() {
        return delegate().map(mapper);
    }

}
