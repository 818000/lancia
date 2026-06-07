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

import org.miaixz.bus.core.lang.Optional;
import org.miaixz.lancia.nimble.browser.TargetType;

/**
 * Public browser target API for pages, workers, and protocol sessions.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Target {

    /**
     * Returns the target identifier.
     *
     * @return target identifier
     */
    String id();

    /**
     * Returns the target page.
     *
     * @return page
     */
    Optional<Page> page();

    /**
     * Returns the target page.
     *
     * @return page
     */
    Page asPage();

    /**
     * Returns the target worker.
     *
     * @return worker
     */
    Optional<Worker> worker();

    /**
     * Creates a CDP-compatible protocol session.
     *
     * @return session
     */
    Optional<? extends Session> createCDPSession();

    /**
     * Returns the opener target.
     *
     * @return opener target
     */
    Optional<? extends Target> opener();

    /**
     * Returns the owning browser.
     *
     * @return browser
     */
    Browser browser();

    /**
     * Returns the browser context.
     *
     * @return browser context
     */
    Context browserContext();

    /**
     * Returns the target URL.
     *
     * @return target URL
     */
    String url();

    /**
     * Returns the target type.
     *
     * @return target type
     */
    TargetType type();

}
