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
package org.miaixz.lancia.runtime;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.browser.bundle.FetcherOptions;

/**
 * Global Lancia configuration aligned with Puppeteer's getConfiguration.ts.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class Configuration {

    /**
     * Cache directory environment variable.
     */
    public static final String ENV_CACHE_DIR = "PUPPETEER_CACHE_DIR";

    /**
     * Executable path environment variable.
     */
    public static final String ENV_EXECUTABLE_PATH = "PUPPETEER_EXECUTABLE_PATH";

    /**
     * Default browser environment variable.
     */
    public static final String ENV_BROWSER = "PUPPETEER_BROWSER";

    /**
     * Temporary directory environment variable.
     */
    public static final String ENV_TMP_DIR = "PUPPETEER_TMP_DIR";

    /**
     * Global skip-download environment variable.
     */
    public static final String ENV_SKIP_DOWNLOAD = "PUPPETEER_SKIP_DOWNLOAD";

    /**
     * Log level environment variable.
     */
    public static final String ENV_LOG_LEVEL = "PUPPETEER_LOGLEVEL";

    /**
     * Default cache directory.
     */
    public static final Path DEFAULT_CACHE_DIRECTORY = Path.of(System.getProperty("user.home"), ".cache", "puppeteer");

    /**
     * Default temporary directory.
     */
    public static final Path DEFAULT_TEMPORARY_DIRECTORY = Path.of(System.getProperty("java.io.tmpdir"));

    /**
     * Cache directory.
     */
    private Path cacheDirectory = DEFAULT_CACHE_DIRECTORY;

    /**
     * Browser executable path.
     */
    private Path executablePath;

    /**
     * Default browser.
     */
    private String defaultBrowser = "chrome";

    /**
     * Temporary directory.
     */
    private Path temporaryDirectory = DEFAULT_TEMPORARY_DIRECTORY;

    /**
     * Global skip-download flag.
     */
    private Boolean skipDownload;

    /**
     * Log level.
     */
    private LogLevel logLevel = LogLevel.WARN;

    /**
     * Runtime security policy.
     */
    private SecurityPolicy securityPolicy = SecurityPolicy.defaultPolicy();

    /**
     * Runtime resource limits.
     */
    private ResourceLimits resourceLimits = ResourceLimits.defaults();

    /**
     * Experimental feature flags.
     */
    private Map<String, Object> experiments = new LinkedHashMap<>();

    /**
     * Chrome download settings.
     */
    private Chrome chrome = new Chrome();

    /**
     * Chrome Headless Shell download settings.
     */
    private Headless headless = new Headless();

    /**
     * Firefox download settings.
     */
    private Firefox firefox = new Firefox();

    /**
     * Creates default configuration.
     */
    public Configuration() {
        // No initialization required.
    }

    /**
     * Creates default configuration.
     *
     * @return default configuration
     */
    public static Configuration create() {
        return new Configuration();
    }

    /**
     * Creates configuration from environment variables.
     *
     * @param environment environment variables
     * @return configuration
     */
    public static Configuration fromEnvironment(Map<String, String> environment) {
        Configuration configuration = create();
        configuration.applyEnvironment(environment);
        return configuration;
    }

    /**
     * Applies environment variable overrides.
     *
     * @param environment environment variables
     */
    public void applyEnvironment(Map<String, String> environment) {
        Map<String, String> env = environment == null ? Map.of() : environment;
        if (env.containsKey(ENV_LOG_LEVEL)) {
            logLevel = LogLevel.fromValue(env.get(ENV_LOG_LEVEL));
        }
        if (env.containsKey(ENV_BROWSER)) {
            setDefaultBrowser(env.get(ENV_BROWSER));
        }
        if (StringKit.isNotBlank(env.get(ENV_EXECUTABLE_PATH))) {
            executablePath = Path.of(env.get(ENV_EXECUTABLE_PATH));
            skipDownload = Boolean.TRUE;
        }
        Boolean explicitSkipDownload = booleanEnvVar(env, ENV_SKIP_DOWNLOAD);
        if (explicitSkipDownload != null) {
            skipDownload = explicitSkipDownload;
        }
        if (StringKit.isNotBlank(env.get(ENV_CACHE_DIR))) {
            cacheDirectory = Path.of(env.get(ENV_CACHE_DIR));
        }
        if (StringKit.isNotBlank(env.get(ENV_TMP_DIR))) {
            temporaryDirectory = Path.of(env.get(ENV_TMP_DIR));
        }
        applyBrowserSetting("chrome", chrome, new Chrome(), env, skipDownload);
        applyBrowserSetting("chrome-headless-shell", headless, new Headless(), env, skipDownload);
        applyBrowserSetting("firefox", firefox, new Firefox(), env, skipDownload);
    }

    /**
     * Returns the cache directory.
     *
     * @return cache directory
     */
    public Path getCacheDirectory() {
        return cacheDirectory;
    }

    /**
     * Updates cache directory.
     *
     * @param cacheDirectory cache directory value
     */
    public void setCacheDirectory(Path cacheDirectory) {
        this.cacheDirectory = cacheDirectory == null ? DEFAULT_CACHE_DIRECTORY : cacheDirectory;
    }

    /**
     * Returns the browser executable path.
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
     * Returns the default browser.
     *
     * @return default browser
     */
    public String getDefaultBrowser() {
        return defaultBrowser;
    }

    /**
     * Updates default browser.
     *
     * @param defaultBrowser default browser value
     */
    public void setDefaultBrowser(String defaultBrowser) {
        this.defaultBrowser = defaultBrowser(defaultBrowser);
    }

    /**
     * Returns the temporary directory.
     *
     * @return temporary directory
     */
    public Path getTemporaryDirectory() {
        return temporaryDirectory;
    }

    /**
     * Updates temporary directory.
     *
     * @param temporaryDirectory temporary directory value
     */
    public void setTemporaryDirectory(Path temporaryDirectory) {
        this.temporaryDirectory = temporaryDirectory == null ? DEFAULT_TEMPORARY_DIRECTORY : temporaryDirectory;
    }

    /**
     * Returns the global skip-download flag.
     *
     * @return skip-download flag
     */
    public Boolean getSkipDownload() {
        return skipDownload;
    }

    /**
     * Updates skip download.
     *
     * @param skipDownload skip-download flag
     */
    public void setSkipDownload(Boolean skipDownload) {
        this.skipDownload = skipDownload;
    }

    /**
     * Returns the log level.
     *
     * @return log level
     */
    public LogLevel getLogLevel() {
        return logLevel;
    }

    /**
     * Updates log level.
     *
     * @param logLevel log level value
     */
    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel == null ? LogLevel.WARN : logLevel;
    }

    /**
     * Returns the security policy.
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
     * Returns experimental feature flags.
     *
     * @return experimental feature flags
     */
    public Map<String, Object> getExperiments() {
        return Map.copyOf(experiments);
    }

    /**
     * Updates experiments.
     *
     * @param experiments experimental feature flags
     */
    public void setExperiments(Map<String, Object> experiments) {
        this.experiments = experiments == null ? new LinkedHashMap<>() : new LinkedHashMap<>(experiments);
    }

    /**
     * Returns Chrome download settings.
     *
     * @return Chrome download settings
     */
    public Chrome getChrome() {
        return chrome;
    }

    /**
     * Updates chrome.
     *
     * @param chrome Chrome download settings
     */
    public void setChrome(Chrome chrome) {
        this.chrome = chrome == null ? new Chrome() : chrome;
    }

    /**
     * Returns Chrome Headless Shell download settings.
     *
     * @return Chrome Headless Shell download settings
     */
    public Headless getChromeHeadlessShell() {
        return headless;
    }

    /**
     * Updates chrome headless shell.
     *
     * @param headless Chrome Headless Shell download settings
     */
    public void setChromeHeadlessShell(Headless headless) {
        this.headless = headless == null ? new Headless() : headless;
    }

    /**
     * Returns Firefox download settings.
     *
     * @return Firefox download settings
     */
    public Firefox getFirefox() {
        return firefox;
    }

    /**
     * Updates firefox.
     *
     * @param firefox Firefox download settings
     */
    public void setFirefox(Firefox firefox) {
        this.firefox = firefox == null ? new Firefox() : firefox;
    }

    /**
     * Applies browser-specific environment variables.
     *
     * @param browser            browser name
     * @param settings           target settings
     * @param defaultSettings    default settings
     * @param environment        environment variables
     * @param globalSkipDownload global skip-download flag
     */
    private static void applyBrowserSetting(
            String browser,
            Browser settings,
            Browser defaultSettings,
            Map<String, String> environment,
            Boolean globalSkipDownload) {
        String browserEnvName = browser.replace(Symbol.C_MINUS, Symbol.C_UNDERLINE).toUpperCase(Locale.ROOT);
        String envPrefix = "PUPPETEER_" + browserEnvName;
        if (environment.containsKey(envPrefix + "_VERSION")) {
            settings.setVersion(environment.get(envPrefix + "_VERSION"));
        } else if (settings.getVersion() == null) {
            settings.setVersion(defaultSettings.getVersion());
        }
        if (environment.containsKey(envPrefix + "_DOWNLOAD_BASE_URL")) {
            settings.setDownloadBaseUrl(environment.get(envPrefix + "_DOWNLOAD_BASE_URL"));
        } else if (settings.getDownloadBaseUrl() == null) {
            settings.setDownloadBaseUrl(defaultSettings.getDownloadBaseUrl());
        }
        if (environment.containsKey(envPrefix + "_EXPECTED_ARCHIVE_SHA256")) {
            settings.setExpectedArchiveSha256(environment.get(envPrefix + "_EXPECTED_ARCHIVE_SHA256"));
        } else if (settings.getExpectedArchiveSha256() == null) {
            settings.setExpectedArchiveSha256(defaultSettings.getExpectedArchiveSha256());
        }
        Boolean allowUnverifiedDownload = booleanEnvVar(environment, envPrefix + "_ALLOW_UNVERIFIED_DOWNLOAD");
        if (allowUnverifiedDownload != null) {
            settings.setAllowUnverifiedDownload(allowUnverifiedDownload);
        } else {
            settings.setAllowUnverifiedDownload(defaultSettings.isAllowUnverifiedDownload());
        }
        Boolean skipDownload = booleanEnvVar(environment, envPrefix + "_SKIP_DOWNLOAD");
        if (skipDownload == null) {
            skipDownload = booleanEnvVar(environment, "PUPPETEER_SKIP_" + browserEnvName + "_DOWNLOAD");
        }
        if (skipDownload != null) {
            settings.setSkipDownload(skipDownload);
        } else if (settings.getSkipDownload() == null) {
            settings.setSkipDownload(
                    globalSkipDownload != null ? globalSkipDownload : defaultSettings.getSkipDownload());
        }
    }

    /**
     * Reads a Puppeteer boolean environment variable.
     *
     * @param environment environment variables
     * @param name        variable name
     * @return boolean value, or {@code null} when the variable is absent
     */
    private static Boolean booleanEnvVar(Map<String, String> environment, String name) {
        if (!environment.containsKey(name)) {
            return null;
        }
        return booleanValue(environment.get(name));
    }

    /**
     * Parses a Puppeteer boolean environment value.
     *
     * @param value environment value
     * @return parsed boolean value
     */
    private static boolean booleanValue(String value) {
        String normalized = value == null ? Normal.EMPTY : value.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case Normal.EMPTY, "0", "false", "off" -> false;
            default -> true;
        };
    }

    /**
     * Resolves the default browser variant.
     *
     * @param browser configured browser
     * @return browser variant
     */
    private static String defaultBrowser(String browser) {
        if (StringKit.isBlank(browser)) {
            return "chrome";
        }
        String normalized = browser.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "chrome", "firefox" -> normalized;
            default -> throw new InternalException("Unsupported browser " + browser);
        };
    }

    /**
     * Log level.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public enum LogLevel {

        /**
         * Silent logging.
         */
        SILENT("silent"),

        /**
         * Error logging.
         */
        ERROR("error"),

        /**
         * Warning logging.
         */
        WARN("warn");

        /**
         * Configuration value.
         */
        private final String value;

        /**
         * Creates a log level.
         *
         * @param value configuration value
         */
        LogLevel(String value) {
            this.value = value;
        }

        /**
         * Returns configuration.
         *
         * @return configuration value
         */
        public String value() {
            return value;
        }

        /**
         * Parses a log level.
         *
         * @param value configuration value
         * @return log level
         */
        public static LogLevel fromValue(String value) {
            if (StringKit.isBlank(value)) {
                return WARN;
            }
            String actualValue = value.toLowerCase(Locale.ROOT);
            for (LogLevel level : values()) {
                if (level.value.equals(actualValue)) {
                    return level;
                }
            }
            return WARN;
        }
    }

    /**
     * Browser download settings.
     */
    @Getter
    @Setter
    /**
     * Coordinates browser operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static class Browser {

        /**
         * Browser skip-download flag.
         */
        private Boolean skipDownload;

        /**
         * Browser download base URL.
         */
        private String downloadBaseUrl;

        /**
         * Browser version.
         */
        private String version;

        /**
         * Expected archive SHA-256.
         */
        private String expectedArchiveSha256;

        /**
         * Whether unverified downloads are allowed.
         */
        private boolean allowUnverifiedDownload;

        /**
         * Creates browser download settings.
         */
        public Browser() {
            // No initialization required.
        }

        /**
         * Updates expected archive SHA-256.
         *
         * @param expectedArchiveSha256 expected archive SHA-256 value
         */
        public void setExpectedArchiveSha256(String expectedArchiveSha256) {
            this.expectedArchiveSha256 = FetcherOptions.normalizeArchiveSha256(expectedArchiveSha256);
        }
    }

    /**
     * Firefox download settings.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class Firefox extends Browser {

        /**
         * Creates Firefox download settings.
         */
        public Firefox() {
            setSkipDownload(Boolean.TRUE);
        }
    }

    /**
     * Chrome download settings.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class Chrome extends Browser {

        /**
         * Creates Chrome download settings.
         */
        public Chrome() {
            // No initialization required.
        }
    }

    /**
     * Chrome Headless Shell download settings.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class Headless extends Browser {

        /**
         * Creates Chrome Headless Shell download settings.
         */
        public Headless() {
            // No initialization required.
        }
    }

}
