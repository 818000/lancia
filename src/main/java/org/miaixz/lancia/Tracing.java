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
import java.util.List;

import org.miaixz.lancia.options.TracingOptions;

/**
 * Public tracing API for recording browser performance traces.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Tracing {

    /**
     * Starts tracing with categories and an optional output path.
     *
     * @param categories tracing categories
     * @param path       output path
     */
    void start(List<String> categories, Path path);

    /**
     * Starts tracing with full tracing options.
     *
     * @param options tracing options
     */
    void start(TracingOptions options);

    /**
     * Stops tracing and returns trace bytes.
     *
     * @return trace bytes
     */
    byte[] stop();

    /**
     * Returns the recording.
     *
     * @return {@code true} when the condition matches
     */
    boolean recording();

    /**
     * Returns the configured trace output path.
     *
     * @return output path
     */
    Path path();

}
