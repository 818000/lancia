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
package org.miaixz.lancia.browser.metadata;

import java.nio.file.Path;
import java.util.List;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.lancia.browser.BrowserPlatform;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.ChromeReleaseChannel;

/**
 * Describes chrome headless shell browser metadata.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class ChromeHeadlessShellBrowserData {

    /**
     * Creates a chrome headless shell browser data.
     */
    private ChromeHeadlessShellBrowserData() {
        // No initialization required.
    }

    /**
     * Resolves download URL.
     *
     * @param platform platform
     * @param buildId  build id
     * @param baseUrl  base url
     * @return resolve download URL value
     */
    public static String resolveDownloadUrl(BrowserPlatform platform, String buildId, String baseUrl) {
        return ChromeBrowserData.resolveArtifactDownloadUrl(platform, buildId, baseUrl, "chrome-headless-shell");
    }

    /**
     * Resolves download path.
     *
     * @param platform platform
     * @param buildId  build id
     * @return resolve download path value
     */
    public static List<String> resolveDownloadPath(BrowserPlatform platform, String buildId) {
        return ChromeBrowserData.resolveArtifactDownloadPath(platform, buildId, "chrome-headless-shell");
    }

    /**
     * Returns the relative executable path.
     *
     * @param platform platform value
     * @param buildId  build ID value
     * @return relative executable path value
     */
    public static Path relativeExecutablePath(BrowserPlatform platform, String buildId) {
        String folder = ChromeBrowserData.folder(platform);
        return switch (Assert.notNull(platform, "platform")) {
            case MAC, MAC_ARM64, LINUX, LINUX_ARM64 -> Path
                    .of("chrome-headless-shell-" + folder, "chrome-headless-shell");
            case WIN32, WIN64 -> Path.of("chrome-headless-shell-" + folder, "chrome-headless-shell.exe");
        };
    }

    /**
     * Resolves build ID.
     *
     * @param channel channel
     * @return resolve build ID value
     */
    public static String resolveBuildId(ChromeReleaseChannel channel) {
        return ChromeBrowserData.resolveBuildId(channel);
    }

    /**
     * Resolves build ID.
     *
     * @param channelOrPrefix channel or prefix
     * @return resolve build ID value
     */
    public static Optional<String> resolveBuildId(String channelOrPrefix) {
        return ChromeBrowserData.resolveBuildId(channelOrPrefix);
    }

    /**
     * Returns the compare versions.
     *
     * @param left  left value
     * @param right right value
     * @return compare versions value
     */
    public static int compareVersions(String left, String right) {
        return ChromeBrowserData.compareVersions(left, right);
    }

}
