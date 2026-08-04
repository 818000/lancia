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
 * Options used when reading Progressive Web App OS integration state.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class GetPWAStateOptions {

    /**
     * Manifest id from the web app manifest.
     */
    private String manifestId;

    /**
     * Creates default state options.
     */
    public GetPWAStateOptions() {
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

}
