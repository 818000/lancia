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

import org.miaixz.lancia.Page;
import org.miaixz.lancia.kernel.Element;

/**
 * Represents a locator that starts from a DOM node.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class NodeLocator extends ElementLocator {

    /**
     * Creates a node locator.
     *
     * @param root root element
     */
    public NodeLocator(Element root) {
        super(root);
    }

    /**
     * Creates a node locator.
     *
     * @param page     page instance
     * @param selector selector value
     */
    public NodeLocator(Page page, String selector) {
        super(page, selector);
    }

    /**
     * Creates a node locator.
     *
     * @param page     page instance
     * @param selector selector value
     * @param root     root element
     */
    public NodeLocator(Page page, String selector, Element root) {
        super(page, selector, root);
    }

}
