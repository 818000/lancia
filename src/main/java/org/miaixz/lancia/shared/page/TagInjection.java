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
package org.miaixz.lancia.shared.page;

import java.nio.file.Files;
import java.nio.file.Path;

import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.options.ScriptTagOptions;
import org.miaixz.lancia.options.StyleTagOptions;

/**
 * Provides shared script and style tag injection helpers.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class TagInjection {

    /**
     * Hides the tag injection constructor.
     */
    private TagInjection() {
        // No initialization required.
    }

    /**
     * Validates that a script or style tag has exactly one source.
     *
     * @param tag     tag
     * @param content content
     * @param url     url
     * @param path    path
     */
    public static void validateTagSources(String tag, String content, String url, String path) {
        int sources = (content == null ? Normal._0 : 1) + (StringKit.isBlank(url) ? Normal._0 : 1)
                + (StringKit.isBlank(path) ? Normal._0 : 1);
        if (sources != 1) {
            throw new InternalException(tag + " tag injection requires exactly one of content, url, or path.");
        }
    }

    /**
     * Creates script tag options from inline content.
     *
     * @param content content
     * @return script tag value
     */
    public static ScriptTagOptions scriptTag(String content) {
        ScriptTagOptions options = new ScriptTagOptions();
        options.setContent(content == null ? Normal.EMPTY : content);
        return options;
    }

    /**
     * Creates style tag options from inline content.
     *
     * @param content content
     * @return style tag value
     */
    public static StyleTagOptions styleTag(String content) {
        StyleTagOptions options = new StyleTagOptions();
        options.setContent(content == null ? Normal.EMPTY : content);
        return options;
    }

    /**
     * Reads injected local file content.
     *
     * @param path path
     * @param tag  tag
     * @return file content
     */
    public static String readFile(String path, String tag) {
        try {
            return Files.readString(Path.of(path), Charset.UTF_8);
        } catch (Exception ex) {
            throw new InternalException("Failed to read " + tag + " injection file: " + path, ex);
        }
    }

}
