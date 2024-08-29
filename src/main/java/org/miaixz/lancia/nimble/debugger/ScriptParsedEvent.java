/*
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
 ~                                                                               ~
 ~ The MIT License (MIT)                                                         ~
 ~                                                                               ~
 ~ Copyright (c) 2015-2024 miaixz.org and other contributors.                    ~
 ~                                                                               ~
 ~ Permission is hereby granted, free of charge, to any person obtaining a copy  ~
 ~ of this software and associated documentation files (the "Software"), to deal ~
 ~ in the Software without restriction, including without limitation the rights  ~
 ~ to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     ~
 ~ copies of the Software, and to permit persons to whom the Software is         ~
 ~ furnished to do so, subject to the following conditions:                      ~
 ~                                                                               ~
 ~ The above copyright notice and this permission notice shall be included in    ~
 ~ all copies or substantial portions of the Software.                           ~
 ~                                                                               ~
 ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    ~
 ~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      ~
 ~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   ~
 ~ AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        ~
 ~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, ~
 ~ OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     ~
 ~ THE SOFTWARE.                                                                 ~
 ~                                                                               ~
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
*/
package org.miaixz.lancia.nimble.debugger;

import org.miaixz.lancia.nimble.runtime.AuxData;
import org.miaixz.lancia.nimble.runtime.StackTrace;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Fired when virtual machine parses script. This event is also fired for all known and uncollected scripts upon
 * enabling debugger.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptParsedEvent {

    /**
     * Identifier of the script parsed.
     */
    private String scriptId;
    /**
     * URL or name of the script parsed (if any).
     */
    private String url;
    /**
     * Line offset of the script within the resource with given URL (for script tags).
     */
    private int startLine;
    /**
     * Column offset of the script within the resource with given URL.
     */
    private int startColumn;
    /**
     * Last line of the script.
     */
    private int endLine;
    /**
     * Length of the last line of the script.
     */
    private int endColumn;
    /**
     * Specifies script creation context.
     */
    private int executionContextId;
    /**
     * Content hash of the script.
     */
    private String hash;
    /**
     * Embedder-specific auxiliary data.
     */
    private AuxData executionContextAuxData;
    /**
     * True, if this script is generated as a result of the live edit operation.
     */
    private boolean isLiveEdit;
    /**
     * URL of source map associated with script (if any).
     */
    private String sourceMapURL;
    /**
     * True, if this script has sourceURL.
     */
    private boolean hasSourceURL;
    /**
     * True, if this script is ES6 module.
     */
    private boolean isModule;
    /**
     * This script length.
     */
    private int length;
    /**
     * JavaScript top stack frame of where the script parsed event was triggered if available.
     */
    private StackTrace stackTrace;

}
