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
package org.miaixz.lancia.options;

import java.nio.file.Path;
import java.util.List;

/**
 * tracing options.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class TracingOptions {

    /**
     * Current path.
     */
    private final Path path;
    /**
     * Whether screenshots is enabled.
     */
    private final boolean screenshots;
    /**
     * Registered categories values.
     */
    private final List<String> categories;

    /**
     * Creates tracing options.
     */
    public TracingOptions() {
        this(null, false, List.of());
    }

    /**
     * Creates tracing options.
     *
     * @param path        output path
     * @param screenshots screenshot tracing state
     * @param categories  tracing categories
     */
    public TracingOptions(Path path, boolean screenshots, List<String> categories) {
        this.path = path;
        this.screenshots = screenshots;
        this.categories = categories == null ? List.of() : List.copyOf(categories);
    }

    /**
     * Returns the output path.
     *
     * @return output path
     */
    public Path path() {
        return path;
    }

    /**
     * Returns the screenshots.
     *
     * @return {@code true} when the condition matches
     */
    public boolean screenshots() {
        return screenshots;
    }

    /**
     * Returns tracing categories.
     *
     * @return tracing categories
     */
    public List<String> categories() {
        return categories;
    }

}
