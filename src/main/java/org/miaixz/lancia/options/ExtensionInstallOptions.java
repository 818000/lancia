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

/**
 * Options used when installing an unpacked browser extension.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class ExtensionInstallOptions {

    /**
     * Whether the extension should be enabled in Incognito or off-the-record profiles.
     */
    private boolean enabledInIncognito;

    /**
     * Creates default extension install options.
     */
    public ExtensionInstallOptions() {
        // No initialization required.
    }

    /**
     * Returns whether the extension should be enabled in Incognito profiles.
     *
     * @return {@code true} when Incognito support should be enabled
     */
    public boolean isEnabledInIncognito() {
        return enabledInIncognito;
    }

    /**
     * Updates whether the extension should be enabled in Incognito profiles.
     *
     * @param enabledInIncognito whether Incognito support should be enabled
     */
    public void setEnabledInIncognito(boolean enabledInIncognito) {
        this.enabledInIncognito = enabledInIncognito;
    }

}
