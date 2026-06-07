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
package org.miaixz.lancia.events;

/**
 * Enumerates page lifecycle and runtime events.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public enum PageEvent {

    /**
     * Emitted when the page is closed.
     */
    CLOSE,

    /**
     * Emitted when the page writes a console message.
     */
    CONSOLE,

    /**
     * Emitted when the page opens a dialog.
     */
    DIALOG,

    /**
     * Emitted when the page reports an error.
     */
    ERROR,

    /**
     * Emitted when the page load event fires.
     */
    LOAD,

    /**
     * Emitted when the page starts a request.
     */
    REQUEST,

    /**
     * Emitted when the page receives a response.
     */
    RESPONSE

}
