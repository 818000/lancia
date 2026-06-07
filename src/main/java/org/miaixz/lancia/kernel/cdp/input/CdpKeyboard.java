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
package org.miaixz.lancia.kernel.cdp.input;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.kernel.Keyboard;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.nimble.input.USKeyboardLayout;
import org.miaixz.lancia.nimble.input.USKeyboardLayout.KeyDescription;
import org.miaixz.lancia.options.KeyboardTypeOptions;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.input.InputAction;

/**
 * Sends keyboard input actions.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpKeyboard implements Keyboard {

    /**
     * Current input.
     */
    private final CdpInput input;
    /**
     * Registered pressed keys values.
     */
    private final Set<String> pressedKeys = new LinkedHashSet<>();
    /**
     * Current modifiers.
     */
    private int modifiers;

    /**
     * Creates a keyboard.
     *
     * @param input input source
     */
    public CdpKeyboard(CdpInput input) {
        this.input = input;
    }

    /**
     * Dispatches a down input action.
     *
     * @param key key value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> down(String key) {
        return down(key, null, List.of());
    }

    /**
     * Dispatches a down input action.
     *
     * @param key     key value
     * @param options operation options
     * @return completion future
     */
    public CompletableFuture<CdpPayload> down(String key, KeyboardTypeOptions options) {
        return InputAction.down(key, options, this::down);
    }

    /**
     * Dispatches a down input action.
     *
     * @param key      key value
     * @param text     text to use
     * @param commands commands to send
     * @return completion future
     */
    public CompletableFuture<CdpPayload> down(String key, String text, List<String> commands) {
        KeyDescription description = keyDescriptionForString(key);
        boolean autoRepeat = pressedKeys.contains(description.code());
        pressedKeys.add(description.code());
        modifiers |= modifierBit(description.key());
        input.setModifiers(modifiers);
        String actualText = text == null ? description.text() : text;
        Logger.debug(
                true,
                "Input",
                "CdpKeyboard down requested: key={}, code={}, autoRepeat={}",
                description.key(),
                description.code(),
                autoRepeat);
        return input.dispatchKeyEvent(
                actualText == null || actualText.isEmpty() ? "rawKeyDown" : "keyDown",
                description.key(),
                actualText,
                description.keyCode(),
                description.code(),
                description.location(),
                autoRepeat,
                commands == null ? List.of() : commands);
    }

    /**
     * Dispatches an up input action.
     *
     * @param key key value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> up(String key) {
        KeyDescription description = keyDescriptionForString(key);
        modifiers &= ~modifierBit(description.key());
        pressedKeys.remove(description.code());
        input.setModifiers(modifiers);
        Logger.debug(
                false,
                "Input",
                "CdpKeyboard up requested: key={}, code={}",
                description.key(),
                description.code());
        return input.dispatchKeyEvent(
                "keyUp",
                description.key(),
                null,
                description.keyCode(),
                description.code(),
                description.location(),
                false,
                List.of());
    }

    /**
     * Handles press.
     *
     * @param key key value
     */
    public void press(String key) {
        press(key, 0L);
    }

    /**
     * Handles press.
     *
     * @param key         key value
     * @param delayMillis delay in milliseconds
     */
    public void press(String key, long delayMillis) {
        Logger.debug(true, "Input", "CdpKeyboard press requested: key={}, delayMillis={}", key, delayMillis);
        down(key);
        sleep(delayMillis);
        up(key);
        Logger.debug(false, "Input", "CdpKeyboard press completed: key={}", key);
    }

    /**
     * Handles press.
     *
     * @param key     key value
     * @param options operation options
     */
    public void press(String key, KeyboardTypeOptions options) {
        KeyboardTypeOptions actualOptions = options == null ? new KeyboardTypeOptions() : options;
        Logger.debug(
                true,
                "Input",
                "CdpKeyboard press requested: key={}, delayMillis={}",
                key,
                actualOptions.getDelay());
        down(key, actualOptions);
        sleep(actualOptions.getDelay());
        up(key);
        Logger.debug(false, "Input", "CdpKeyboard press completed: key={}", key);
    }

    /**
     * Returns the send character.
     *
     * @param character character value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> sendCharacter(String character) {
        Logger.debug(
                true,
                "Input",
                "CdpKeyboard character requested: chars={}",
                character == null ? 0 : character.length());
        return input.insertText(character);
    }

    /**
     * Handles type.
     *
     * @param text text to use
     */
    public void type(String text) {
        type(text, 0L);
    }

    /**
     * Handles type.
     *
     * @param text        text to use
     * @param delayMillis delay in milliseconds
     */
    public void type(String text, long delayMillis) {
        if (text == null) {
            return;
        }
        Logger.debug(true, "Input", "CdpKeyboard type requested: chars={}, delayMillis={}", text.length(), delayMillis);
        for (int index = 0; index < text.length(); index++) {
            String value = String.valueOf(text.charAt(index));
            if (charIsKey(value)) {
                press(value);
            } else {
                sendCharacter(value);
            }
            sleep(delayMillis);
        }
        Logger.debug(false, "Input", "CdpKeyboard type completed: chars={}", text.length());
    }

    /**
     * Handles type.
     *
     * @param text    text to use
     * @param options operation options
     */
    public void type(String text, KeyboardTypeOptions options) {
        KeyboardTypeOptions actualOptions = options == null ? new KeyboardTypeOptions() : options;
        type(text, actualOptions.getDelay());
    }

    /**
     * Returns the modifiers.
     *
     * @return modifiers value
     */
    public int modifiers() {
        return modifiers;
    }

    /**
     * Returns the char is key.
     *
     * @param value to use
     * @return {@code true} when the condition matches
     */
    private boolean charIsKey(String value) {
        return USKeyboardLayout.isKey(value);
    }

    /**
     * Handles sleep.
     *
     * @param delayMillis delay in milliseconds
     */
    private void sleep(long delayMillis) {
        Awaitable.sleep(delayMillis, "CdpKeyboard input was interrupted.");
    }

    /**
     * Returns the key description for string.
     *
     * @param key key value
     * @return key description for string value
     */
    private KeyDescription keyDescriptionForString(String key) {
        return USKeyboardLayout.describe(key, modifiers);
    }

    /**
     * Returns the modifier bit.
     *
     * @param key key value
     * @return modifier bit value
     */
    private int modifierBit(String key) {
        return switch (key) {
            case "Alt" -> 1;
            case "Control" -> 2;
            case "Meta" -> 4;
            case "Shift" -> 8;
            default -> 0;
        };
    }

}
