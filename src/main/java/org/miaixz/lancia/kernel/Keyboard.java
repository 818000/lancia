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
package org.miaixz.lancia.kernel;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.miaixz.lancia.Payload;
import org.miaixz.lancia.options.KeyboardTypeOptions;

/**
 * Sends keyboard input to a page.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Keyboard {

    /**
     * Presses down a key.
     *
     * @param key key name
     * @return command future
     */
    CompletableFuture<? extends Payload> down(String key);

    /**
     * Presses down a key with text and command options.
     *
     * @param key     key name
     * @param options keyboard options
     * @return command future
     */
    CompletableFuture<? extends Payload> down(String key, KeyboardTypeOptions options);

    /**
     * Presses down a key with explicit text and editor commands.
     *
     * @param key      key name
     * @param text     text override
     * @param commands editor commands
     * @return command future
     */
    CompletableFuture<? extends Payload> down(String key, String text, List<String> commands);

    /**
     * Releases a key.
     *
     * @param key key name
     * @return command future
     */
    CompletableFuture<? extends Payload> up(String key);

    /**
     * Presses a key.
     *
     * @param key key name
     */
    void press(String key);

    /**
     * Presses a key with options.
     *
     * @param key     key name
     * @param options keyboard options
     */
    void press(String key, KeyboardTypeOptions options);

    /**
     * Sends a raw character to the focused element.
     *
     * @param character character to insert
     * @return command future
     */
    CompletableFuture<? extends Payload> sendCharacter(String character);

    /**
     * Types text.
     *
     * @param text text to type
     */
    void type(String text);

    /**
     * Types text with options.
     *
     * @param text    text to type
     * @param options keyboard options
     */
    void type(String text, KeyboardTypeOptions options);

    /**
     * Returns the active keyboard modifier bit mask.
     *
     * @return modifier bit mask
     */
    int modifiers();

}
