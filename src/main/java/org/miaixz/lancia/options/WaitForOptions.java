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
package org.miaixz.lancia.options;

import java.time.Duration;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Public wait options matching Puppeteer's WaitForOptions name.
 */
@Getter
@Setter
/**
 * Defines options for wait for operations.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class WaitForOptions {

    /**
     * Maximum wait duration.
     */
    private Duration timeout;

    /**
     * Lifecycle events to wait for.
     */
    private List<String> waitUntil = List.of("load");

    /**
     * Whether reload should bypass cache.
     */
    private boolean ignoreCache;

    /**
     * Creates wait options.
     */
    public WaitForOptions() {
        // No initialization required.
    }

}
