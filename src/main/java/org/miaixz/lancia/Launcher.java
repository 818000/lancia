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
package org.miaixz.lancia;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.nimble.browser.BrowserVariant;
import org.miaixz.lancia.options.ConnectOptions;
import org.miaixz.lancia.options.LaunchOptions;
import org.miaixz.lancia.runtime.BrowserRuntime;

/**
 * Launches and connects browser variants.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Launcher {

    /**
     * Creates a launcher for a browser variant.
     *
     * @param browser browser variant
     * @return launcher instance
     */
    static Launcher of(BrowserVariant browser) {
        return BrowserRuntime.launcher(browser);
    }

    /**
     * Runs the command line entry point.
     *
     * @param args command line arguments
     * @return process exit code
     */
    static int main(String[] args) {
        List<String> values = args == null ? List.of() : Arrays.asList(args);
        String command = values.isEmpty() ? "help" : StringKit.toString(values.get(0)).trim();
        return switch (command) {
            case Normal.EMPTY, "help", "--help", "-h" -> {
                Logger.info(false, "Launcher", "Usage: lancia <help|version|install|launch>");
                yield 0;
            }
            case "version", "--version", "-v" -> {
                Logger.info(false, "Launcher", "{}", Puppeteer.packageVersion());
                yield 0;
            }
            case "install" -> {
                Puppeteer.downloadBrowsers().forEach(System.out::println);
                yield 0;
            }
            case "launch" -> runLaunch(values);
            default -> throw new InternalException("Unknown launcher command: " + command);
        };
    }

    /**
     * Runs the launch command.
     *
     * @param values command line values
     * @return process exit code
     */
    private static int runLaunch(List<String> values) {
        if (values.contains("--dry-run")) {
            Puppeteer.defaultArgs(new LaunchOptions()).forEach(System.out::println);
            return 0;
        }
        try (AutoCloseable browser = Puppeteer.launch(new LaunchOptions())) {
            return 0;
        } catch (Exception ex) {
            throw new InternalException("Failed to launch browser from CLI.", ex);
        }
    }

    /**
     * Launches a browser with the supplied options.
     *
     * @param options launch options
     * @return launched browser
     */
    Browser launch(LaunchOptions options);

    /**
     * Connects to an existing browser with the supplied options.
     *
     * @param options connection options
     * @return connected browser
     */
    Browser connect(ConnectOptions options);

    /**
     * Resolves the browser executable path.
     *
     * @param options launch options
     * @return executable path
     */
    Path executable(LaunchOptions options);

    /**
     * Resolves browser command line arguments.
     *
     * @param options launch options
     * @return command line arguments
     */
    List<String> args(LaunchOptions options);

}
