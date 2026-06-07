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
package org.miaixz.lancia.browser.launch;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.lancia.Launcher;
import org.miaixz.lancia.nimble.browser.BrowserVariant;

/**
 * Provides launcher factory methods for browser variants.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class Launchers {

    /**
     * Hides the launcher factory constructor.
     */
    private Launchers() {
        // No initialization required.
    }

    /**
     * Creates a launcher for a browser variant.
     *
     * @param browser browser variant
     * @return launcher instance
     */
    public static Launcher of(BrowserVariant browser) {
        return switch (Assert.notNull(browser, "browser")) {
            case FIREFOX -> new FirefoxLauncher();
            case CHROME -> new ChromeLauncher();
        };
    }

}
