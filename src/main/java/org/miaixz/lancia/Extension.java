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
package org.miaixz.lancia;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Public browser extension API.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Extension {

    /**
     * Returns the extension id.
     *
     * @return extension id
     */
    String id();

    /**
     * Returns the extension version.
     *
     * @return extension version
     */
    String version();

    /**
     * Returns the extension name.
     *
     * @return extension name
     */
    String name();

    /**
     * Returns the extension path.
     *
     * @return extension path
     */
    String path();

    /**
     * Returns the enabled.
     *
     * @return {@code true} when the condition matches
     */
    boolean enabled();

    /**
     * Returns the owning browser.
     *
     * @return browser
     */
    Browser browser();

    /**
     * Returns map form.
     *
     * @return map representation
     */
    Map<String, Object> toMap();

    /**
     * Returns extension workers.
     *
     * @return worker list
     */
    List<? extends Worker> workers();

    /**
     * Returns extension pages.
     *
     * @return page list
     */
    List<Page> pages();

    /**
     * Triggers the extension action.
     *
     * @param page page
     * @return completion future
     */
    CompletableFuture<Void> triggerAction(Page page);

}
