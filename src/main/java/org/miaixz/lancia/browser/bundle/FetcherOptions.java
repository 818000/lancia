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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.core.xyz.UrlKit;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.browser.BrowserPlatform;
import org.miaixz.lancia.browser.metadata.BrowserData;
import org.miaixz.lancia.runtime.ResourceLimits;
import org.miaixz.lancia.runtime.SecurityPolicy;

/**
 * Defines options for fetcher operations.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class FetcherOptions {

    /**
     * Creates fetcher options.
     */
    public FetcherOptions() {
        // No initialization required.
    }

    /**
     * Default download base URL.
     */
    private static final URI DEFAULT_DOWNLOAD_BASE_URL = UrlKit
            .toURI("https://storage.googleapis.com/chrome-for-testing-public/");
    /**
     * SHA-256 hex length.
     */
    private static final int SHA256_HEX_LENGTH = 64;
    /**
     * Current browser.
     */
    private String browser = ExecutableResolver.CHROME;
    /**
     * Current build ID.
     */
    private String buildId = BrowserData.defaultBuildId(browser);
    /**
     * Current platform.
     */
    private BrowserPlatform platform;
    /**
     * Current cache dir.
     */
    private Path cacheDir = Path.of(System.getProperty("user.home"), Symbol.DOT + "cache", Builder.PRODUCT_NAME);
    /**
     * Current executable path.
     */
    private Path executablePath;
    /**
     * Whether prefer system executable is enabled.
     */
    private boolean preferSystemExecutable = true;
    /**
     * Whether install if missing is enabled.
     */
    private boolean installIfMissing;
    /**
     * Current download base URL.
     */
    private URI downloadBaseUrl = DEFAULT_DOWNLOAD_BASE_URL;
    /**
     * Current download URL.
     */
    private URI downloadUrl;
    /**
     * Expected archive SHA-256.
     */
    private String expectedArchiveSha256;
    /**
     * Whether unverified downloads are allowed.
     */
    private boolean allowUnverifiedDownload;
    /**
     * Runtime security policy.
     */
    private SecurityPolicy securityPolicy = SecurityPolicy.defaultPolicy();
    /**
     * Runtime resource limits.
     */
    private ResourceLimits resourceLimits = ResourceLimits.defaults();
    /**
     * Registered system candidates values.
     */
    private final List<Path> systemCandidates = new ArrayList<>();

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
        this.browser = StringKit.isBlank(browser) ? ExecutableResolver.CHROME : browser;
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
        this.buildId = StringKit.isBlank(buildId) ? BrowserData.defaultBuildId(browser) : buildId;
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
     * Returns the platform or detect.
     *
     * @return platform or detect
     */
    public BrowserPlatform getPlatformOrDetect() {
        return platform == null ? BrowserPlatform.detect() : platform;
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
     * Returns the cache dir.
     *
     * @return cache dir
     */
    public Path getCacheDir() {
        return cacheDir;
    }

    /**
     * Updates cache dir.
     *
     * @param cacheDir cache dir value
     */
    public void setCacheDir(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    /**
     * Returns the executable path.
     *
     * @return executable path
     */
    public Path getExecutablePath() {
        return executablePath;
    }

    /**
     * Updates executable path.
     *
     * @param executablePath executable path value
     */
    public void setExecutablePath(Path executablePath) {
        this.executablePath = executablePath;
    }

    /**
     * Returns whether prefer system executable is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isPreferSystemExecutable() {
        return preferSystemExecutable;
    }

    /**
     * Updates prefer system executable.
     *
     * @param preferSystemExecutable prefer system executable value
     */
    public void setPreferSystemExecutable(boolean preferSystemExecutable) {
        this.preferSystemExecutable = preferSystemExecutable;
    }

    /**
     * Returns whether install if missing is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isInstallIfMissing() {
        return installIfMissing;
    }

    /**
     * Updates install if missing.
     *
     * @param installIfMissing install if missing value
     */
    public void setInstallIfMissing(boolean installIfMissing) {
        this.installIfMissing = installIfMissing;
    }

    /**
     * Returns the download base URL.
     *
     * @return download base URL
     */
    public URI getDownloadBaseUrl() {
        return downloadBaseUrl;
    }

    /**
     * Updates download base URL.
     *
     * @param downloadBaseUrl download base url
     */
    public void setDownloadBaseUrl(URI downloadBaseUrl) {
        this.downloadBaseUrl = downloadBaseUrl;
    }

    /**
     * Returns the download URL.
     *
     * @return download URL
     */
    public URI getDownloadUrl() {
        return downloadUrl;
    }

    /**
     * Updates download URL.
     *
     * @param downloadUrl download url
     */
    public void setDownloadUrl(URI downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    /**
     * Returns expected archive SHA-256.
     *
     * @return expected archive SHA-256
     */
    public String getExpectedArchiveSha256() {
        return expectedArchiveSha256;
    }

    /**
     * Updates expected archive SHA-256.
     *
     * @param expectedArchiveSha256 expected archive SHA-256 value
     */
    public void setExpectedArchiveSha256(String expectedArchiveSha256) {
        this.expectedArchiveSha256 = normalizeArchiveSha256(expectedArchiveSha256);
    }

    /**
     * Returns whether unverified downloads are allowed.
     *
     * @return {@code true} when unverified downloads are allowed
     */
    public boolean isAllowUnverifiedDownload() {
        return allowUnverifiedDownload;
    }

    /**
     * Updates whether unverified downloads are allowed.
     *
     * @param allowUnverifiedDownload allow unverified download value
     */
    public void setAllowUnverifiedDownload(boolean allowUnverifiedDownload) {
        this.allowUnverifiedDownload = allowUnverifiedDownload;
    }

    /**
     * Returns security policy.
     *
     * @return security policy
     */
    public SecurityPolicy getSecurityPolicy() {
        return securityPolicy;
    }

    /**
     * Updates security policy.
     *
     * @param securityPolicy security policy value
     */
    public void setSecurityPolicy(SecurityPolicy securityPolicy) {
        this.securityPolicy = securityPolicy == null ? SecurityPolicy.defaultPolicy() : securityPolicy;
    }

    /**
     * Returns resource limits.
     *
     * @return resource limits
     */
    public ResourceLimits getResourceLimits() {
        return resourceLimits;
    }

    /**
     * Updates resource limits.
     *
     * @param resourceLimits resource limits value
     */
    public void setResourceLimits(ResourceLimits resourceLimits) {
        this.resourceLimits = resourceLimits == null ? ResourceLimits.defaults() : resourceLimits;
    }

    /**
     * Returns the system candidates.
     *
     * @return values
     */
    public List<Path> getSystemCandidates() {
        return List.copyOf(systemCandidates);
    }

    /**
     * Adds system candidate.
     *
     * @param candidate candidate
     */
    public void addSystemCandidate(Path candidate) {
        if (candidate != null) {
            systemCandidates.add(candidate);
        }
    }

    /**
     * Resolves download URL.
     *
     * @return resolve download URL value
     */
    public URI resolveDownloadUrl() {
        if (downloadUrl != null) {
            return downloadUrl;
        }
        BrowserPlatform actualPlatform = getPlatformOrDetect();
        String path = getBuildId() + Symbol.SLASH + actualPlatform.downloadId() + Symbol.SLASH
                + actualPlatform.archiveName();
        return downloadBaseUrl.resolve(path);
    }

    /**
     * Normalizes an archive SHA-256 value.
     *
     * @param value checksum value
     * @return normalized checksum value
     */
    public static String normalizeArchiveSha256(String value) {
        if (StringKit.isBlank(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() != SHA256_HEX_LENGTH || !normalized.matches("[0-9a-f]+")) {
            throw new IllegalArgumentException("Expected archive SHA-256 must be 64 hex characters.");
        }
        return normalized;
    }

}
