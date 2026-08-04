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

import org.miaixz.lancia.Builder;

/**
 * Options used when launching an installed Progressive Web App.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class LaunchPWAOptions {

    /**
     * Manifest id from the web app manifest.
     */
    private String manifestId;

    /**
     * Optional URL within the app scope.
     */
    private String url;

    /**
     * Maximum time to wait for the app page target, in milliseconds.
     */
    private long timeoutMillis = Builder.DEFAULT_TIMEOUT_MILLIS;

    /**
     * Creates default launch options.
     */
    public LaunchPWAOptions() {
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
     * Returns the optional launch URL.
     *
     * @return launch URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Updates the optional launch URL.
     *
     * @param url launch URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Returns the timeout in milliseconds.
     *
     * @return timeout in milliseconds
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * Updates the timeout in milliseconds. Use {@code 0} to disable the timeout.
     *
     * @param timeoutMillis timeout in milliseconds
     */
    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = Math.max(0L, timeoutMillis);
    }

}
