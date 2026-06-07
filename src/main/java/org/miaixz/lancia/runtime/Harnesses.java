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

import java.io.File;
import java.nio.file.FileSystems;

import org.miaixz.lancia.Harness;
import org.miaixz.lancia.kernel.cdp.screen.CdpScreenRecorder;

/**
 * Provides runtime harness dependency factories.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class Harnesses {

    /**
     * Hides the harnesses constructor.
     */
    private Harnesses() {
        // No initialization required.
    }

    /**
     * Creates JVM default dependencies.
     *
     * @return JVM default dependencies
     */
    public static Harness.Dependencies jvmDefaults() {
        Harness.Dependencies dependencies = new Harness.Dependencies();
        dependencies.setFileSystem(FileSystems.getDefault());
        dependencies.setPathSeparator(File.separator);
        dependencies.setScreenRecorderType(CdpScreenRecorder.class);
        dependencies.setDebuglog(message -> {
        });
        return dependencies;
    }

}
