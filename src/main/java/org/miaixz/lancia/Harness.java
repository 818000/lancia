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

import java.nio.file.FileSystem;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.Setter;

import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.lancia.runtime.Harnesses;

/**
 * Execution harness abstraction.
 *
 * <p>
 * This interface maps the runtime-facing part of Puppeteer's environment model and exposes a unified CDP session plus
 * main realm for frames, workers, and other JavaScript execution harnesses.
 * </p>
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Harness {

    /**
     * Shared dependency holder.
     */
    Holder DEPENDENCIES = new Holder();

    /**
     * Returns whether node is enabled.
     *
     * @return {@code true} when the condition matches
     */
    static boolean isNode() {
        return true;
    }

    /**
     * Returns the current harness dependencies.
     *
     * @return current harness dependencies
     */
    static Dependencies dependencies() {
        return DEPENDENCIES.getValue();
    }

    /**
     * Replaces the current harness dependencies.
     *
     * @param dependencies harness dependencies
     */
    static void setDependencies(Dependencies dependencies) {
        DEPENDENCIES.setValue(dependencies == null ? Dependencies.unavailable() : dependencies);
    }

    /**
     * Installs default JVM dependencies.
     */
    static void installDefaultDependencies() {
        setDependencies(Dependencies.jvmDefaults());
    }

    /**
     * Resets dependencies to the unavailable state.
     */
    static void resetDependencies() {
        setDependencies(Dependencies.unavailable());
    }

    /**
     * Returns the CDP session used by the current harness.
     *
     * @return CDP session
     */
    Session client();

    /**
     * Returns the main realm for the current harness.
     *
     * @return main realm
     */
    Realm mainRealm();

    /**
     * Holder for runtime dependencies.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    final class Holder {

        /**
         * Creates a dependency holder.
         */
        public Holder() {
            // No initialization required.
        }

        /**
         * Current runtime dependencies.
         */
        private volatile Dependencies value = Dependencies.unavailable();

        /**
         * Returns the current runtime dependencies.
         *
         * @return current runtime dependencies
         */
        private Dependencies getValue() {
            return value;
        }

        /**
         * Updates the current runtime dependencies.
         *
         * @param value runtime dependencies
         */
        private void setValue(Dependencies value) {
            this.value = value;
        }
    }

    /**
     * Runtime dependencies installed after module initialization.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    @Getter
    @Setter
    final class Dependencies {

        /**
         * Creates runtime dependencies.
         */
        public Dependencies() {
            // No initialization required.
        }

        /**
         * Host file system dependency.
         */
        private FileSystem fileSystem;

        /**
         * Host path separator.
         */
        private String pathSeparator;

        /**
         * Screen recorder implementation type.
         */
        private Class<?> screenRecorderType;

        /**
         * Debug log sink.
         */
        private Consumer<String> debuglog;

        /**
         * Creates unavailable dependencies.
         *
         * @return unavailable dependencies
         */
        public static Dependencies unavailable() {
            return new Dependencies();
        }

        /**
         * Creates JVM default dependencies.
         *
         * @return JVM default dependencies
         */
        public static Dependencies jvmDefaults() {
            return Harnesses.jvmDefaults();
        }

        /**
         * Returns the available file system or fails with a project exception.
         *
         * @return file system
         */
        public FileSystem requireFileSystem() {
            if (fileSystem == null) {
                throw new InternalException("fs is not available in this harness");
            }
            return fileSystem;
        }

        /**
         * Returns the available screen recorder type or fails with a project exception.
         *
         * @return screen recorder type
         */
        public Class<?> requireScreenRecorderType() {
            if (screenRecorderType == null) {
                throw new InternalException("ScreenRecorder is not available in this harness");
            }
            return screenRecorderType;
        }
    }

}
