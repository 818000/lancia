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

import java.nio.file.Path;

import org.miaixz.lancia.nimble.network.DownloadBehavior;

/**
 * Defines options for context operations.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class ContextOptions {

    /**
     * Creates context options.
     */
    public ContextOptions() {
        // No initialization required.
    }

    /**
     * Current download behavior.
     */
    private DownloadBehavior downloadBehavior;

    /**
     * Returns the download path.
     *
     * @return download path
     */
    public Path getDownloadPath() {
        return downloadBehavior == null ? null : downloadBehavior.getDownloadPath();
    }

    /**
     * Updates download path.
     *
     * @param downloadPath download path value
     */
    public void setDownloadPath(Path downloadPath) {
        this.downloadBehavior = downloadPath == null ? null : DownloadBehavior.allow(downloadPath);
    }

    /**
     * Returns the download behavior.
     *
     * @return download behavior
     */
    public DownloadBehavior getDownloadBehavior() {
        return downloadBehavior;
    }

    /**
     * Updates download behavior.
     *
     * @param downloadBehavior download behavior value
     */
    public void setDownloadBehavior(DownloadBehavior downloadBehavior) {
        this.downloadBehavior = downloadBehavior;
    }

}
