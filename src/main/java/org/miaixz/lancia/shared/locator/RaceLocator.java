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

import java.util.List;

/**
 * Represents a locator that races multiple locators.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class RaceLocator extends ElementLocator {

    /**
     * Registered locators values.
     */
    private final List<ElementLocator> locators;

    /**
     * Creates a race locator.
     *
     * @param locators candidate locators
     */
    public RaceLocator(List<ElementLocator> locators) {
        super(ElementLocator.race(locators));
        this.locators = List.copyOf(locators);
    }

    /**
     * Returns the create.
     *
     * @param locators locators value
     * @return create value
     */
    public static RaceLocator create(List<ElementLocator> locators) {
        return new RaceLocator(locators);
    }

    /**
     * Returns candidate locators.
     *
     * @return candidate locators
     */
    public List<ElementLocator> locators() {
        return locators;
    }

}
