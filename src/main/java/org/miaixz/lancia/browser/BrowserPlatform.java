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
package org.miaixz.lancia.browser;

import java.util.Locale;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.health.Platform;
import org.miaixz.bus.health.Platform.OS;

/**
 * Enumerates BrowserPlatforms.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public enum BrowserPlatform {

    /**
     * Represents the linux enum member.
     */
    LINUX("linux", "linux64", "chrome-linux64.zip", "chrome-linux64"),

    /**
     * Represents the linux arm64 enum member.
     */
    LINUX_ARM64("linux_arm", "linux-arm64", "chrome-linux64.zip", "chrome-linux64"),

    /**
     * Represents the mac enum member.
     */
    MAC("mac", "mac-x64", "chrome-mac-x64.zip", "chrome-mac-x64"),

    /**
     * Represents the mac arm64 enum member.
     */
    MAC_ARM64("mac_arm", "mac-arm64", "chrome-mac-arm64.zip", "chrome-mac-arm64"),

    /**
     * Represents the win32 enum member.
     */
    WIN32("win32", "win32", "chrome-win32.zip", "chrome-win32"),

    /**
     * Represents the win64 enum member.
     */
    WIN64("win64", "win64", "chrome-win64.zip", "chrome-win64");

    /**
     * Current cache ID.
     */
    private final String cacheId;
    /**
     * Current download ID.
     */
    private final String downloadId;
    /**
     * Current archive name.
     */
    private final String archiveName;
    /**
     * Current archive root.
     */
    private final String archiveRoot;

    /**
     * Creates a browser platform.
     *
     * @param cacheId     cache id
     * @param downloadId  download id
     * @param archiveName archive name
     * @param archiveRoot archive root
     */
    BrowserPlatform(String cacheId, String downloadId, String archiveName, String archiveRoot) {
        this.cacheId = cacheId;
        this.downloadId = downloadId;
        this.archiveName = archiveName;
        this.archiveRoot = archiveRoot;
    }

    /**
     * Returns the detect.
     *
     * @return detect value
     */
    public static BrowserPlatform detect() {
        return detectBrowserPlatform().orElseThrow(
                () -> new IllegalStateException("Invalid browser platform: " + currentOsName() + " ("
                        + currentArchitecture() + Symbol.PARENTHESE_RIGHT));
    }

    /**
     * Returns the detect.
     *
     * @param osName os name value
     * @param osArch os arch value
     * @return detect value
     */
    public static BrowserPlatform detect(String osName, String osArch) {
        return detect(osName, osArch, currentOsVersion());
    }

    /**
     * Returns the detect.
     *
     * @param osName    os name value
     * @param osArch    os arch value
     * @param osRelease os release value
     * @return detect value
     */
    public static BrowserPlatform detect(String osName, String osArch, String osRelease) {
        return detectBrowserPlatform(osName, osArch, osRelease).orElseThrow(
                () -> new IllegalStateException(
                        "Invalid browser platform: " + osName + " (" + osArch + Symbol.PARENTHESE_RIGHT));
    }

    /**
     * Returns the detect browser platform.
     *
     * @return optional value
     */
    public static Optional<BrowserPlatform> detectBrowserPlatform() {
        OS os = Platform.getCurrentPlatform();
        if (os == OS.UNKNOWN) {
            return Optional.empty();
        }
        return detectBrowserPlatform(browserOsName(os), currentArchitecture(), currentOsVersion());
    }

    /**
     * Returns the detect browser platform.
     *
     * @param osName os name value
     * @param osArch os arch value
     * @return optional value
     */
    public static Optional<BrowserPlatform> detectBrowserPlatform(String osName, String osArch) {
        return detectBrowserPlatform(osName, osArch, currentOsVersion());
    }

    /**
     * Returns the detect browser platform.
     *
     * @param osName    os name value
     * @param osArch    os arch value
     * @param osRelease os release value
     * @return optional value
     */
    public static Optional<BrowserPlatform> detectBrowserPlatform(String osName, String osArch, String osRelease) {
        String name = normalize(osName);
        String arch = normalize(osArch);
        boolean arm64 = arch.contains("aarch64") || arch.contains("arm64");
        if (name.contains("mac") || name.contains("darwin")) {
            return Optional.of(arm64 ? MAC_ARM64 : MAC);
        }
        if (name.contains("win")) {
            boolean x64 = arch.contains("x64") || arch.contains("amd64") || arch.contains("64") && !arm64;
            return Optional.of(x64 || arm64 && isWindows11(osRelease) ? WIN64 : WIN32);
        }
        if (name.contains("linux")) {
            return Optional.of(arm64 ? LINUX_ARM64 : LINUX);
        }
        return Optional.empty();
    }

    /**
     * Returns the cache ID.
     *
     * @return cache ID value
     */
    public String cacheId() {
        return cacheId;
    }

    /**
     * Returns the download ID.
     *
     * @return download ID value
     */
    public String downloadId() {
        return downloadId;
    }

    /**
     * Returns the archive name.
     *
     * @return archive name value
     */
    public String archiveName() {
        return archiveName;
    }

    /**
     * Returns the archive root.
     *
     * @return archive root value
     */
    public String archiveRoot() {
        return archiveRoot;
    }

    /**
     * Returns whether the current platform is Linux.
     *
     * @return {@code true} when the current platform is Linux
     */
    public static boolean isCurrentLinux() {
        return Platform.getCurrentPlatform() == OS.LINUX;
    }

    /**
     * Returns whether the current platform is macOS.
     *
     * @return {@code true} when the current platform is macOS
     */
    public static boolean isCurrentMac() {
        return Platform.getCurrentPlatform() == OS.MACOS;
    }

    /**
     * Returns whether the current platform is Windows.
     *
     * @return {@code true} when the current platform is Windows
     */
    public static boolean isCurrentWindows() {
        return Platform.getCurrentPlatform() == OS.WINDOWS || Platform.getCurrentPlatform() == OS.WINDOWSCE;
    }

    /**
     * Returns the current OS version.
     *
     * @return current OS version
     */
    static String currentOsVersion() {
        return Platform.get("os.version", Normal.EMPTY);
    }

    /**
     * Returns the current OS name.
     *
     * @return current OS name
     */
    private static String currentOsName() {
        return Platform.get("os.name", Platform.getCurrentPlatform().getName());
    }

    /**
     * Returns the current architecture.
     *
     * @return current architecture
     */
    private static String currentArchitecture() {
        String architecture = Platform.get("os.arch", Normal.EMPTY);
        if (!architecture.isBlank()) {
            return architecture;
        }
        if (Platform.isARM()) {
            return Platform.is64Bit() ? "arm64" : "arm";
        }
        if (Platform.isIntel()) {
            return Platform.is64Bit() ? "x64" : "x86";
        }
        return Normal.EMPTY;
    }

    /**
     * Maps the health platform to the browser platform detector OS name.
     *
     * @param os current health platform
     * @return browser platform detector OS name
     */
    private static String browserOsName(OS os) {
        return switch (os) {
            case MACOS -> "mac";
            case LINUX, ANDROID -> "linux";
            case WINDOWS, WINDOWSCE -> "win";
            default -> os.getName();
        };
    }

    /**
     * Returns the normalize.
     *
     * @param value to use
     * @return normalize value
     */
    private static String normalize(String value) {
        return value == null ? Normal.EMPTY : value.toLowerCase(Locale.ROOT);
    }

    /**
     * Returns whether the current platform is Windows 11 or newer.
     *
     * @param version version
     * @return {@code true} when the condition matches
     */
    public static boolean isWindows11(String version) {
        String[] parts = String.valueOf(version).split("\\.");
        if (parts.length <= 2) {
            return false;
        }
        int major = parseVersionPart(parts[0]);
        int minor = parseVersionPart(parts[1]);
        int patch = parseVersionPart(parts[2]);
        return major > 10 || major == 10 && minor > 0 || major == 10 && minor == 0 && patch >= 22000;
    }

    /**
     * Parses version part.
     *
     * @param value to use
     * @return parse version part value
     */
    private static int parseVersionPart(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

}
