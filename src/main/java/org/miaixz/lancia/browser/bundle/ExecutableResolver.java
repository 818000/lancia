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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.browser.BrowserPlatform;
import org.miaixz.lancia.browser.metadata.BrowserData;
import org.miaixz.lancia.browser.metadata.BrowserDataTypes.Browser;

/**
 * Resolves browser executable paths.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class ExecutableResolver {

    /**
     * Shared constant for chrome.
     */
    public static final String CHROME = "chrome";

    /**
     * Shared constant for chrome headless shell.
     */
    public static final String CHROME_HEADLESS_SHELL = "chrome-headless-shell";

    /**
     * Creates an ExecutableResolver instance.
     */
    private ExecutableResolver() {
        // No initialization required.
    }

    /**
     * Resolves system executable path.
     *
     * @param platform             platform
     * @param additionalCandidates additional candidates
     * @return resolve system executable path value
     */
    public static Optional<Path> resolveSystemExecutablePath(
            BrowserPlatform platform,
            List<Path> additionalCandidates) {
        List<Path> candidates = new ArrayList<>();
        if (additionalCandidates != null) {
            candidates.addAll(additionalCandidates);
        }
        candidates.addAll(defaultSystemCandidates(platform));
        for (Path candidate : candidates) {
            if (candidate != null && FileKit.isFile(candidate.toFile())) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the relative executable path.
     *
     * @param browser  browser instance
     * @param platform platform value
     * @return relative executable path value
     */
    public static Path relativeExecutablePath(String browser, BrowserPlatform platform) {
        return BrowserData.relativeExecutablePath(
                Browser.fromValue(StringKit.isBlank(browser) ? CHROME : browser),
                platform,
                Normal.EMPTY);
    }

    /**
     * Returns the default system candidates.
     *
     * @param platform platform value
     * @return values
     */
    public static List<Path> defaultSystemCandidates(BrowserPlatform platform) {
        return switch (platform) {
            case MAC, MAC_ARM64 -> List.of(
                    Path.of("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"),
                    Path.of("/Applications/Google Chrome Beta.app/Contents/MacOS/Google Chrome Beta"),
                    Path.of("/Applications/Chromium.app/Contents/MacOS/Chromium"));
            case LINUX, LINUX_ARM64 -> List.of(
                    Path.of("/usr/bin/google-chrome"),
                    Path.of("/usr/bin/google-chrome-stable"),
                    Path.of("/usr/bin/chromium"),
                    Path.of("/usr/bin/chromium-browser"));
            case WIN32, WIN64 -> windowsCandidates();
        };
    }

    /**
     * Returns the windows candidates.
     *
     * @return values
     */
    private static List<Path> windowsCandidates() {
        List<Path> candidates = new ArrayList<>();
        addWindowsCandidate(candidates, System.getenv("PROGRAMFILES"), "Google", "Chrome", "Application", "chrome.exe");
        addWindowsCandidate(
                candidates,
                System.getenv("PROGRAMFILES(X86)"),
                "Google",
                "Chrome",
                "Application",
                "chrome.exe");
        addWindowsCandidate(candidates, System.getenv("LOCALAPPDATA"), "Google", "Chrome", "Application", "chrome.exe");
        return List.copyOf(candidates);
    }

    /**
     * Handles add windows candidate.
     *
     * @param candidates candidates value
     * @param root       root value
     * @param segments   segments value
     */
    private static void addWindowsCandidate(List<Path> candidates, String root, String... segments) {
        if (StringKit.isBlank(root)) {
            return;
        }
        candidates.add(Path.of(root, segments));
    }

}
