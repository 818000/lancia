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
package org.miaixz.lancia.browser.bundle;

import java.net.URI;
import java.nio.file.Path;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.browser.BrowserPlatform;

/**
 * Defines the browser provider contract.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface BrowserProvider {

    /**
     * Returns the supports.
     *
     * @param options operation options
     * @return {@code true} when the condition matches
     */
    boolean supports(FetcherOptions options);

    /**
     * Returns the supports.
     *
     * @param options operation options
     * @return {@code true} when the condition matches
     */
    default boolean supports(DownloadOptions options) {
        return supports(Assert.notNull(options, "options").toFetcherOptions());
    }

    /**
     * Returns the download URL.
     *
     * @param options operation options
     * @return download URL
     */
    URI getDownloadUrl(FetcherOptions options);

    /**
     * Returns the download URL.
     *
     * @param options operation options
     * @return download URL
     */
    default URI getDownloadUrl(DownloadOptions options) {
        return getDownloadUrl(Assert.notNull(options, "options").toFetcherOptions());
    }

    /**
     * Returns expected archive SHA-256.
     *
     * @param options operation options
     * @return expected archive SHA-256
     */
    default String expectedArchiveSha256(FetcherOptions options) {
        return Assert.notNull(options, "options").getExpectedArchiveSha256();
    }

    /**
     * Returns the executable path.
     *
     * @param options operation options
     * @return executable path
     */
    Path getExecutablePath(FetcherOptions options);

    /**
     * Returns the executable path.
     *
     * @param options operation options
     * @return executable path
     */
    default Path getExecutablePath(DownloadOptions options) {
        return getExecutablePath(Assert.notNull(options, "options").toFetcherOptions());
    }

    /**
     * Returns the name.
     *
     * @return name
     */
    String getName();

    /**
     * Builds archive filename.
     *
     * @param browser   browser instance
     * @param platform  platform value
     * @param buildId   build ID value
     * @param extension extension value
     * @return build archive filename value
     */
    static String buildArchiveFilename(String browser, BrowserPlatform platform, String buildId, String extension) {
        String actualExtension = StringKit.isBlank(extension) ? "zip" : extension;
        return Assert.notBlank(browser, "browser") + Symbol.MINUS + Assert.notNull(platform, "platform").cacheId()
                + Symbol.MINUS + Assert.notBlank(buildId, "buildId") + Symbol.DOT + actualExtension;
    }

    /**
     * Defines options for download operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    final class DownloadOptions {

        /**
         * Current browser.
         */
        private String browser;
        /**
         * Current platform.
         */
        private BrowserPlatform platform;
        /**
         * Current build ID.
         */
        private String buildId;

        /**
         * Creates an instance.
         */
        public DownloadOptions() {
            // No initialization required.
        }

        /**
         * Creates an instance.
         *
         * @param browser  browser instance
         * @param platform platform value
         * @param buildId  build ID value
         */
        public DownloadOptions(String browser, BrowserPlatform platform, String buildId) {
            this.browser = browser;
            this.platform = platform;
            this.buildId = buildId;
        }

        /**
         * Returns the browser.
         *
         * @return browser
         */
        public String getBrowser() {
            return browser;
        }

        /**
         * Updates browser.
         *
         * @param browser browser instance
         */
        public void setBrowser(String browser) {
            this.browser = browser;
        }

        /**
         * Returns the platform.
         *
         * @return platform
         */
        public BrowserPlatform getPlatform() {
            return platform;
        }

        /**
         * Updates platform.
         *
         * @param platform platform value
         */
        public void setPlatform(BrowserPlatform platform) {
            this.platform = platform;
        }

        /**
         * Returns the build ID.
         *
         * @return build ID
         */
        public String getBuildId() {
            return buildId;
        }

        /**
         * Updates build ID.
         *
         * @param buildId build id
         */
        public void setBuildId(String buildId) {
            this.buildId = buildId;
        }

        /**
         * Converts this value to fetcher options.
         *
         * @return fetcher options
         */
        public FetcherOptions toFetcherOptions() {
            FetcherOptions options = new FetcherOptions();
            options.setBrowser(browser);
            options.setPlatform(platform);
            options.setBuildId(buildId);
            return options;
        }
    }

}
