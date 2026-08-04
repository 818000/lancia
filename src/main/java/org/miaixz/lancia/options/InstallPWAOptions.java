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

import org.miaixz.lancia.nimble.browser.PWADisplayMode;

/**
 * Options used when installing a Progressive Web App.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class InstallPWAOptions {

    /**
     * Manifest id from the web app manifest.
     */
    private String manifestId;

    /**
     * URL used to install the app, or the URL of its signed web bundle.
     */
    private String installUrlOrBundleUrl;

    /**
     * Preferred display mode.
     */
    private PWADisplayMode displayMode;

    /**
     * Creates default install options.
     */
    public InstallPWAOptions() {
        // No initialization required.
    }

    /**
     * Returns the manifest id.
     *
     * @return manifest id
     */
    public String getManifestId() {
        return manifestId;
    }

    /**
     * Updates the manifest id.
     *
     * @param manifestId manifest id
     */
    public void setManifestId(String manifestId) {
        this.manifestId = manifestId;
    }

    /**
     * Returns the install URL or signed web bundle URL.
     *
     * @return install URL or signed web bundle URL
     */
    public String getInstallUrlOrBundleUrl() {
        return installUrlOrBundleUrl;
    }

    /**
     * Updates the install URL or signed web bundle URL.
     *
     * @param installUrlOrBundleUrl install URL or signed web bundle URL
     */
    public void setInstallUrlOrBundleUrl(String installUrlOrBundleUrl) {
        this.installUrlOrBundleUrl = installUrlOrBundleUrl;
    }

    /**
     * Returns the preferred display mode.
     *
     * @return preferred display mode
     */
    public PWADisplayMode getDisplayMode() {
        return displayMode;
    }

    /**
     * Updates the preferred display mode.
     *
     * @param displayMode preferred display mode
     */
    public void setDisplayMode(PWADisplayMode displayMode) {
        this.displayMode = displayMode;
    }

}
