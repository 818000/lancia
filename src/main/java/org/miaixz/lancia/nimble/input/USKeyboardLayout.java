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
package org.miaixz.lancia.nimble.input;

import java.util.LinkedHashMap;
import java.util.Map;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.ObjectKit;
import org.miaixz.bus.core.xyz.StringKit;

/**
 * Represents an us keyboard layout value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class USKeyboardLayout {

    /**
     * Shared constant for shift modifier.
     */
    public static final int SHIFT_MODIFIER = 8;

    /**
     * Returns the value of.
     */
    public static final String NUL = String.valueOf((char) 0);
    /**
     * Shared constant for key definitions.
     */
    private static final Map<String, KeyDefinition> KEY_DEFINITIONS = createKeyDefinitions();

    /**
     * Creates an USKeyboardLayout instance.
     */
    private USKeyboardLayout() {
        // No initialization required.
    }

    /**
     * Returns the key definitions.
     *
     * @return mapped values
     */
    public static Map<String, KeyDefinition> keyDefinitions() {
        return KEY_DEFINITIONS;
    }

    /**
     * Returns the definition.
     *
     * @param key key value
     * @return optional value
     */
    public static Optional<KeyDefinition> definition(String key) {
        if (StringKit.isEmpty(key)) {
            return Optional.empty();
        }
        return Optional.ofNullable(KEY_DEFINITIONS.get(key));
    }

    /**
     * Returns whether this entry represents a key definition.
     *
     * @param key key value
     * @return {@code true} when the condition matches
     */
    public static boolean isKey(String key) {
        return definition(key).isPresent();
    }

    /**
     * Returns the describe.
     *
     * @param key       key value
     * @param modifiers modifiers value
     * @return describe value
     */
    public static KeyDescription describe(String key, int modifiers) {
        KeyDefinition definition = definition(key)
                .orElseThrow(() -> new InternalException("Unknown key: \"" + key + Symbol.DOUBLE_QUOTES));
        boolean shift = (modifiers & SHIFT_MODIFIER) != 0;
        String actualKey = ObjectKit.defaultIfNull(definition.key(), Normal.EMPTY);
        if (shift && definition.shiftKey() != null) {
            actualKey = definition.shiftKey();
        }
        int keyCode = ObjectKit.defaultIfNull(definition.keyCode(), 0);
        if (shift && definition.shiftKeyCode() != null) {
            keyCode = definition.shiftKeyCode();
        }
        String code = ObjectKit.defaultIfNull(definition.code(), Normal.EMPTY);
        int location = ObjectKit.defaultIfNull(definition.location(), 0);
        String text = actualKey.length() == 1 ? actualKey : Normal.EMPTY;
        if (definition.text() != null) {
            text = definition.text();
        }
        if (shift && definition.shiftText() != null) {
            text = definition.shiftText();
        }
        if ((modifiers & ~SHIFT_MODIFIER) != 0) {
            text = Normal.EMPTY;
        }
        return new KeyDescription(actualKey, keyCode, code, text, location);
    }

    /**
     * Creates key definitions.
     *
     * @return mapped values
     */
    private static Map<String, KeyDefinition> createKeyDefinitions() {
        Map<String, KeyDefinition> map = new LinkedHashMap<>();
        put(map, "0", new KeyDefinition(48, null, "0", null, "Digit0", null, null, null));
        put(map, "1", new KeyDefinition(49, null, "1", null, "Digit1", null, null, null));
        put(map, "2", new KeyDefinition(50, null, "2", null, "Digit2", null, null, null));
        put(map, "3", new KeyDefinition(51, null, "3", null, "Digit3", null, null, null));
        put(map, "4", new KeyDefinition(52, null, "4", null, "Digit4", null, null, null));
        put(map, "5", new KeyDefinition(53, null, "5", null, "Digit5", null, null, null));
        put(map, "6", new KeyDefinition(54, null, "6", null, "Digit6", null, null, null));
        put(map, "7", new KeyDefinition(55, null, "7", null, "Digit7", null, null, null));
        put(map, "8", new KeyDefinition(56, null, "8", null, "Digit8", null, null, null));
        put(map, "9", new KeyDefinition(57, null, "9", null, "Digit9", null, null, null));
        put(map, "Power", new KeyDefinition(null, null, "Power", null, "Power", null, null, null));
        put(map, "Eject", new KeyDefinition(null, null, "Eject", null, "Eject", null, null, null));
        put(map, "Abort", new KeyDefinition(3, null, "Cancel", null, "Abort", null, null, null));
        put(map, "Help", new KeyDefinition(6, null, "Help", null, "Help", null, null, null));
        put(map, "Backspace", new KeyDefinition(8, null, "Backspace", null, "Backspace", null, null, null));
        put(map, "Tab", new KeyDefinition(9, null, "Tab", null, "Tab", null, null, null));
        put(map, "Numpad5", new KeyDefinition(12, 101, "Clear", "5", "Numpad5", null, null, 3));
        put(map, "NumpadEnter", new KeyDefinition(13, null, "Enter", null, "NumpadEnter", Symbol.CR, null, 3));
        put(map, "Enter", new KeyDefinition(13, null, "Enter", null, "Enter", Symbol.CR, null, null));
        put(map, Symbol.CR, new KeyDefinition(13, null, "Enter", null, "Enter", Symbol.CR, null, null));
        put(map, Symbol.LF, new KeyDefinition(13, null, "Enter", null, "Enter", Symbol.CR, null, null));
        put(map, "ShiftLeft", new KeyDefinition(16, null, "Shift", null, "ShiftLeft", null, null, 1));
        put(map, "ShiftRight", new KeyDefinition(16, null, "Shift", null, "ShiftRight", null, null, 2));
        put(map, "ControlLeft", new KeyDefinition(17, null, "Control", null, "ControlLeft", null, null, 1));
        put(map, "ControlRight", new KeyDefinition(17, null, "Control", null, "ControlRight", null, null, 2));
        put(map, "AltLeft", new KeyDefinition(18, null, "Alt", null, "AltLeft", null, null, 1));
        put(map, "AltRight", new KeyDefinition(18, null, "Alt", null, "AltRight", null, null, 2));
        put(map, "Pause", new KeyDefinition(19, null, "Pause", null, "Pause", null, null, null));
        put(map, "CapsLock", new KeyDefinition(20, null, "CapsLock", null, "CapsLock", null, null, null));
        put(map, "Escape", new KeyDefinition(27, null, "Escape", null, "Escape", null, null, null));
        put(map, "Convert", new KeyDefinition(28, null, "Convert", null, "Convert", null, null, null));
        put(map, "NonConvert", new KeyDefinition(29, null, "NonConvert", null, "NonConvert", null, null, null));
        put(map, "Space", new KeyDefinition(32, null, Symbol.SPACE, null, "Space", null, null, null));
        put(map, "Numpad9", new KeyDefinition(33, 105, "PageUp", "9", "Numpad9", null, null, 3));
        put(map, "PageUp", new KeyDefinition(33, null, "PageUp", null, "PageUp", null, null, null));
        put(map, "Numpad3", new KeyDefinition(34, 99, "PageDown", "3", "Numpad3", null, null, 3));
        put(map, "PageDown", new KeyDefinition(34, null, "PageDown", null, "PageDown", null, null, null));
        put(map, "End", new KeyDefinition(35, null, "End", null, "End", null, null, null));
        put(map, "Numpad1", new KeyDefinition(35, 97, "End", "1", "Numpad1", null, null, 3));
        put(map, "Home", new KeyDefinition(36, null, "Home", null, "Home", null, null, null));
        put(map, "Numpad7", new KeyDefinition(36, 103, "Home", "7", "Numpad7", null, null, 3));
        put(map, "ArrowLeft", new KeyDefinition(37, null, "ArrowLeft", null, "ArrowLeft", null, null, null));
        put(map, "Numpad4", new KeyDefinition(37, 100, "ArrowLeft", "4", "Numpad4", null, null, 3));
        put(map, "Numpad8", new KeyDefinition(38, 104, "ArrowUp", "8", "Numpad8", null, null, 3));
        put(map, "ArrowUp", new KeyDefinition(38, null, "ArrowUp", null, "ArrowUp", null, null, null));
        put(map, "ArrowRight", new KeyDefinition(39, null, "ArrowRight", null, "ArrowRight", null, null, null));
        put(map, "Numpad6", new KeyDefinition(39, 102, "ArrowRight", "6", "Numpad6", null, null, 3));
        put(map, "Numpad2", new KeyDefinition(40, 98, "ArrowDown", "2", "Numpad2", null, null, 3));
        put(map, "ArrowDown", new KeyDefinition(40, null, "ArrowDown", null, "ArrowDown", null, null, null));
        put(map, "Select", new KeyDefinition(41, null, "Select", null, "Select", null, null, null));
        put(map, "Open", new KeyDefinition(43, null, "Execute", null, "Open", null, null, null));
        put(map, "PrintScreen", new KeyDefinition(44, null, "PrintScreen", null, "PrintScreen", null, null, null));
        put(map, "Insert", new KeyDefinition(45, null, "Insert", null, "Insert", null, null, null));
        put(map, "Numpad0", new KeyDefinition(45, 96, "Insert", "0", "Numpad0", null, null, 3));
        put(map, "Delete", new KeyDefinition(46, null, "Delete", null, "Delete", null, null, null));
        put(map, "NumpadDecimal", new KeyDefinition(46, 110, NUL, Symbol.DOT, "NumpadDecimal", null, null, 3));
        put(map, "Digit0", new KeyDefinition(48, null, "0", Symbol.PARENTHESE_RIGHT, "Digit0", null, null, null));
        put(map, "Digit1", new KeyDefinition(49, null, "1", Symbol.NOT, "Digit1", null, null, null));
        put(map, "Digit2", new KeyDefinition(50, null, "2", Symbol.AT, "Digit2", null, null, null));
        put(map, "Digit3", new KeyDefinition(51, null, "3", Symbol.HASH, "Digit3", null, null, null));
        put(map, "Digit4", new KeyDefinition(52, null, "4", Symbol.DOLLAR, "Digit4", null, null, null));
        put(map, "Digit5", new KeyDefinition(53, null, "5", Symbol.PERCENT, "Digit5", null, null, null));
        put(map, "Digit6", new KeyDefinition(54, null, "6", Symbol.CARET, "Digit6", null, null, null));
        put(map, "Digit7", new KeyDefinition(55, null, "7", Symbol.AND, "Digit7", null, null, null));
        put(map, "Digit8", new KeyDefinition(56, null, "8", Symbol.STAR, "Digit8", null, null, null));
        put(map, "Digit9", new KeyDefinition(57, null, "9", Symbol.PARENTHESE_LEFT, "Digit9", null, null, null));
        put(map, "KeyA", new KeyDefinition(65, null, "a", "A", "KeyA", null, null, null));
        put(map, "KeyB", new KeyDefinition(66, null, "b", "B", "KeyB", null, null, null));
        put(map, "KeyC", new KeyDefinition(67, null, "c", "C", "KeyC", null, null, null));
        put(map, "KeyD", new KeyDefinition(68, null, "d", "D", "KeyD", null, null, null));
        put(map, "KeyE", new KeyDefinition(69, null, "e", "E", "KeyE", null, null, null));
        put(map, "KeyF", new KeyDefinition(70, null, "f", "F", "KeyF", null, null, null));
        put(map, "KeyG", new KeyDefinition(71, null, "g", "G", "KeyG", null, null, null));
        put(map, "KeyH", new KeyDefinition(72, null, "h", "H", "KeyH", null, null, null));
        put(map, "KeyI", new KeyDefinition(73, null, "i", "I", "KeyI", null, null, null));
        put(map, "KeyJ", new KeyDefinition(74, null, "j", "J", "KeyJ", null, null, null));
        put(map, "KeyK", new KeyDefinition(75, null, "k", "K", "KeyK", null, null, null));
        put(map, "KeyL", new KeyDefinition(76, null, "l", "L", "KeyL", null, null, null));
        put(map, "KeyM", new KeyDefinition(77, null, "m", "M", "KeyM", null, null, null));
        put(map, "KeyN", new KeyDefinition(78, null, "n", "N", "KeyN", null, null, null));
        put(map, "KeyO", new KeyDefinition(79, null, "o", "O", "KeyO", null, null, null));
        put(map, "KeyP", new KeyDefinition(80, null, "p", "P", "KeyP", null, null, null));
        put(map, "KeyQ", new KeyDefinition(81, null, "q", "Q", "KeyQ", null, null, null));
        put(map, "KeyR", new KeyDefinition(82, null, "r", "R", "KeyR", null, null, null));
        put(map, "KeyS", new KeyDefinition(83, null, "s", "S", "KeyS", null, null, null));
        put(map, "KeyT", new KeyDefinition(84, null, "t", "T", "KeyT", null, null, null));
        put(map, "KeyU", new KeyDefinition(85, null, "u", "U", "KeyU", null, null, null));
        put(map, "KeyV", new KeyDefinition(86, null, "v", "V", "KeyV", null, null, null));
        put(map, "KeyW", new KeyDefinition(87, null, "w", "W", "KeyW", null, null, null));
        put(map, "KeyX", new KeyDefinition(88, null, "x", "X", "KeyX", null, null, null));
        put(map, "KeyY", new KeyDefinition(89, null, "y", "Y", "KeyY", null, null, null));
        put(map, "KeyZ", new KeyDefinition(90, null, "z", "Z", "KeyZ", null, null, null));
        put(map, "MetaLeft", new KeyDefinition(91, null, "Meta", null, "MetaLeft", null, null, 1));
        put(map, "MetaRight", new KeyDefinition(92, null, "Meta", null, "MetaRight", null, null, 2));
        put(map, "ContextMenu", new KeyDefinition(93, null, "ContextMenu", null, "ContextMenu", null, null, null));
        put(map, "NumpadMultiply", new KeyDefinition(106, null, Symbol.STAR, null, "NumpadMultiply", null, null, 3));
        put(map, "NumpadAdd", new KeyDefinition(107, null, Symbol.PLUS, null, "NumpadAdd", null, null, 3));
        put(map, "NumpadSubtract", new KeyDefinition(109, null, Symbol.MINUS, null, "NumpadSubtract", null, null, 3));
        put(map, "NumpadDivide", new KeyDefinition(111, null, Symbol.SLASH, null, "NumpadDivide", null, null, 3));
        put(map, "F1", new KeyDefinition(112, null, "F1", null, "F1", null, null, null));
        put(map, "F2", new KeyDefinition(113, null, "F2", null, "F2", null, null, null));
        put(map, "F3", new KeyDefinition(114, null, "F3", null, "F3", null, null, null));
        put(map, "F4", new KeyDefinition(115, null, "F4", null, "F4", null, null, null));
        put(map, "F5", new KeyDefinition(116, null, "F5", null, "F5", null, null, null));
        put(map, "F6", new KeyDefinition(117, null, "F6", null, "F6", null, null, null));
        put(map, "F7", new KeyDefinition(118, null, "F7", null, "F7", null, null, null));
        put(map, "F8", new KeyDefinition(119, null, "F8", null, "F8", null, null, null));
        put(map, "F9", new KeyDefinition(120, null, "F9", null, "F9", null, null, null));
        put(map, "F10", new KeyDefinition(121, null, "F10", null, "F10", null, null, null));
        put(map, "F11", new KeyDefinition(122, null, "F11", null, "F11", null, null, null));
        put(map, "F12", new KeyDefinition(123, null, "F12", null, "F12", null, null, null));
        put(map, "F13", new KeyDefinition(124, null, "F13", null, "F13", null, null, null));
        put(map, "F14", new KeyDefinition(125, null, "F14", null, "F14", null, null, null));
        put(map, "F15", new KeyDefinition(126, null, "F15", null, "F15", null, null, null));
        put(map, "F16", new KeyDefinition(127, null, "F16", null, "F16", null, null, null));
        put(map, "F17", new KeyDefinition(128, null, "F17", null, "F17", null, null, null));
        put(map, "F18", new KeyDefinition(129, null, "F18", null, "F18", null, null, null));
        put(map, "F19", new KeyDefinition(130, null, "F19", null, "F19", null, null, null));
        put(map, "F20", new KeyDefinition(131, null, "F20", null, "F20", null, null, null));
        put(map, "F21", new KeyDefinition(132, null, "F21", null, "F21", null, null, null));
        put(map, "F22", new KeyDefinition(133, null, "F22", null, "F22", null, null, null));
        put(map, "F23", new KeyDefinition(134, null, "F23", null, "F23", null, null, null));
        put(map, "F24", new KeyDefinition(135, null, "F24", null, "F24", null, null, null));
        put(map, "NumLock", new KeyDefinition(144, null, "NumLock", null, "NumLock", null, null, null));
        put(map, "ScrollLock", new KeyDefinition(145, null, "ScrollLock", null, "ScrollLock", null, null, null));
        put(
                map,
                "AudioVolumeMute",
                new KeyDefinition(173, null, "AudioVolumeMute", null, "AudioVolumeMute", null, null, null));
        put(
                map,
                "AudioVolumeDown",
                new KeyDefinition(174, null, "AudioVolumeDown", null, "AudioVolumeDown", null, null, null));
        put(
                map,
                "AudioVolumeUp",
                new KeyDefinition(175, null, "AudioVolumeUp", null, "AudioVolumeUp", null, null, null));
        put(
                map,
                "MediaTrackNext",
                new KeyDefinition(176, null, "MediaTrackNext", null, "MediaTrackNext", null, null, null));
        put(
                map,
                "MediaTrackPrevious",
                new KeyDefinition(177, null, "MediaTrackPrevious", null, "MediaTrackPrevious", null, null, null));
        put(map, "MediaStop", new KeyDefinition(178, null, "MediaStop", null, "MediaStop", null, null, null));
        put(
                map,
                "MediaPlayPause",
                new KeyDefinition(179, null, "MediaPlayPause", null, "MediaPlayPause", null, null, null));
        put(
                map,
                "Semicolon",
                new KeyDefinition(186, null, Symbol.SEMICOLON, Symbol.COLON, "Semicolon", null, null, null));
        put(map, "Equal", new KeyDefinition(187, null, Symbol.EQUAL, Symbol.PLUS, "Equal", null, null, null));
        put(map, "NumpadEqual", new KeyDefinition(187, null, Symbol.EQUAL, null, "NumpadEqual", null, null, 3));
        put(map, "Comma", new KeyDefinition(188, null, Symbol.COMMA, Symbol.LT, "Comma", null, null, null));
        put(map, "Minus", new KeyDefinition(189, null, Symbol.MINUS, Symbol.UNDERLINE, "Minus", null, null, null));
        put(map, "Period", new KeyDefinition(190, null, Symbol.DOT, Symbol.GT, "Period", null, null, null));
        put(map, "Slash", new KeyDefinition(191, null, Symbol.SLASH, Symbol.QUESTION_MARK, "Slash", null, null, null));
        put(map, "Backquote", new KeyDefinition(192, null, "`", Symbol.TILDE, "Backquote", null, null, null));
        put(
                map,
                "BracketLeft",
                new KeyDefinition(219, null, Symbol.BRACKET_LEFT, Symbol.BRACE_LEFT, "BracketLeft", null, null, null));
        put(map, "Backslash", new KeyDefinition(220, null, Symbol.BACKSLASH, Symbol.OR, "Backslash", null, null, null));
        put(
                map,
                "BracketRight",
                new KeyDefinition(221, null, Symbol.BRACKET_RIGHT, Symbol.BRACE_RIGHT, "BracketRight", null, null,
                        null));
        put(
                map,
                "Quote",
                new KeyDefinition(222, null, Symbol.SINGLE_QUOTE, Symbol.DOUBLE_QUOTES, "Quote", null, null, null));
        put(map, "AltGraph", new KeyDefinition(225, null, "AltGraph", null, "AltGraph", null, null, null));
        put(map, "Props", new KeyDefinition(247, null, "CrSel", null, "Props", null, null, null));
        put(map, "Cancel", new KeyDefinition(3, null, "Cancel", null, "Abort", null, null, null));
        put(map, "Clear", new KeyDefinition(12, null, "Clear", null, "Numpad5", null, null, 3));
        put(map, "Shift", new KeyDefinition(16, null, "Shift", null, "ShiftLeft", null, null, 1));
        put(map, "Control", new KeyDefinition(17, null, "Control", null, "ControlLeft", null, null, 1));
        put(map, "Alt", new KeyDefinition(18, null, "Alt", null, "AltLeft", null, null, 1));
        put(map, "Accept", new KeyDefinition(30, null, "Accept", null, null, null, null, null));
        put(map, "ModeChange", new KeyDefinition(31, null, "ModeChange", null, null, null, null, null));
        put(map, Symbol.SPACE, new KeyDefinition(32, null, Symbol.SPACE, null, "Space", null, null, null));
        put(map, "Print", new KeyDefinition(42, null, "Print", null, null, null, null, null));
        put(map, "Execute", new KeyDefinition(43, null, "Execute", null, "Open", null, null, null));
        put(map, NUL, new KeyDefinition(46, null, NUL, null, "NumpadDecimal", null, null, 3));
        put(map, "a", new KeyDefinition(65, null, "a", null, "KeyA", null, null, null));
        put(map, "b", new KeyDefinition(66, null, "b", null, "KeyB", null, null, null));
        put(map, "c", new KeyDefinition(67, null, "c", null, "KeyC", null, null, null));
        put(map, "d", new KeyDefinition(68, null, "d", null, "KeyD", null, null, null));
        put(map, "e", new KeyDefinition(69, null, "e", null, "KeyE", null, null, null));
        put(map, "f", new KeyDefinition(70, null, "f", null, "KeyF", null, null, null));
        put(map, "g", new KeyDefinition(71, null, "g", null, "KeyG", null, null, null));
        put(map, "h", new KeyDefinition(72, null, "h", null, "KeyH", null, null, null));
        put(map, "i", new KeyDefinition(73, null, "i", null, "KeyI", null, null, null));
        put(map, "j", new KeyDefinition(74, null, "j", null, "KeyJ", null, null, null));
        put(map, "k", new KeyDefinition(75, null, "k", null, "KeyK", null, null, null));
        put(map, "l", new KeyDefinition(76, null, "l", null, "KeyL", null, null, null));
        put(map, "m", new KeyDefinition(77, null, "m", null, "KeyM", null, null, null));
        put(map, "n", new KeyDefinition(78, null, "n", null, "KeyN", null, null, null));
        put(map, "o", new KeyDefinition(79, null, "o", null, "KeyO", null, null, null));
        put(map, "p", new KeyDefinition(80, null, "p", null, "KeyP", null, null, null));
        put(map, "q", new KeyDefinition(81, null, "q", null, "KeyQ", null, null, null));
        put(map, "r", new KeyDefinition(82, null, "r", null, "KeyR", null, null, null));
        put(map, "s", new KeyDefinition(83, null, "s", null, "KeyS", null, null, null));
        put(map, "t", new KeyDefinition(84, null, "t", null, "KeyT", null, null, null));
        put(map, "u", new KeyDefinition(85, null, "u", null, "KeyU", null, null, null));
        put(map, "v", new KeyDefinition(86, null, "v", null, "KeyV", null, null, null));
        put(map, "w", new KeyDefinition(87, null, "w", null, "KeyW", null, null, null));
        put(map, "x", new KeyDefinition(88, null, "x", null, "KeyX", null, null, null));
        put(map, "y", new KeyDefinition(89, null, "y", null, "KeyY", null, null, null));
        put(map, "z", new KeyDefinition(90, null, "z", null, "KeyZ", null, null, null));
        put(map, "Meta", new KeyDefinition(91, null, "Meta", null, "MetaLeft", null, null, 1));
        put(map, Symbol.STAR, new KeyDefinition(106, null, Symbol.STAR, null, "NumpadMultiply", null, null, 3));
        put(map, Symbol.PLUS, new KeyDefinition(107, null, Symbol.PLUS, null, "NumpadAdd", null, null, 3));
        put(map, Symbol.MINUS, new KeyDefinition(109, null, Symbol.MINUS, null, "NumpadSubtract", null, null, 3));
        put(map, Symbol.SLASH, new KeyDefinition(111, null, Symbol.SLASH, null, "NumpadDivide", null, null, 3));
        put(map, Symbol.SEMICOLON, new KeyDefinition(186, null, Symbol.SEMICOLON, null, "Semicolon", null, null, null));
        put(map, Symbol.EQUAL, new KeyDefinition(187, null, Symbol.EQUAL, null, "Equal", null, null, null));
        put(map, Symbol.COMMA, new KeyDefinition(188, null, Symbol.COMMA, null, "Comma", null, null, null));
        put(map, Symbol.DOT, new KeyDefinition(190, null, Symbol.DOT, null, "Period", null, null, null));
        put(map, "`", new KeyDefinition(192, null, "`", null, "Backquote", null, null, null));
        put(
                map,
                Symbol.BRACKET_LEFT,
                new KeyDefinition(219, null, Symbol.BRACKET_LEFT, null, "BracketLeft", null, null, null));
        put(map, Symbol.BACKSLASH, new KeyDefinition(220, null, Symbol.BACKSLASH, null, "Backslash", null, null, null));
        put(
                map,
                Symbol.BRACKET_RIGHT,
                new KeyDefinition(221, null, Symbol.BRACKET_RIGHT, null, "BracketRight", null, null, null));
        put(
                map,
                Symbol.SINGLE_QUOTE,
                new KeyDefinition(222, null, Symbol.SINGLE_QUOTE, null, "Quote", null, null, null));
        put(map, "Attn", new KeyDefinition(246, null, "Attn", null, null, null, null, null));
        put(map, "CrSel", new KeyDefinition(247, null, "CrSel", null, "Props", null, null, null));
        put(map, "ExSel", new KeyDefinition(248, null, "ExSel", null, null, null, null, null));
        put(map, "EraseEof", new KeyDefinition(249, null, "EraseEof", null, null, null, null, null));
        put(map, "Play", new KeyDefinition(250, null, "Play", null, null, null, null, null));
        put(map, "ZoomOut", new KeyDefinition(251, null, "ZoomOut", null, null, null, null, null));
        put(
                map,
                Symbol.PARENTHESE_RIGHT,
                new KeyDefinition(48, null, Symbol.PARENTHESE_RIGHT, null, "Digit0", null, null, null));
        put(map, Symbol.NOT, new KeyDefinition(49, null, Symbol.NOT, null, "Digit1", null, null, null));
        put(map, Symbol.AT, new KeyDefinition(50, null, Symbol.AT, null, "Digit2", null, null, null));
        put(map, Symbol.HASH, new KeyDefinition(51, null, Symbol.HASH, null, "Digit3", null, null, null));
        put(map, Symbol.DOLLAR, new KeyDefinition(52, null, Symbol.DOLLAR, null, "Digit4", null, null, null));
        put(map, Symbol.PERCENT, new KeyDefinition(53, null, Symbol.PERCENT, null, "Digit5", null, null, null));
        put(map, Symbol.CARET, new KeyDefinition(54, null, Symbol.CARET, null, "Digit6", null, null, null));
        put(map, Symbol.AND, new KeyDefinition(55, null, Symbol.AND, null, "Digit7", null, null, null));
        put(
                map,
                Symbol.PARENTHESE_LEFT,
                new KeyDefinition(57, null, Symbol.PARENTHESE_LEFT, null, "Digit9", null, null, null));
        put(map, "A", new KeyDefinition(65, null, "A", null, "KeyA", null, null, null));
        put(map, "B", new KeyDefinition(66, null, "B", null, "KeyB", null, null, null));
        put(map, "C", new KeyDefinition(67, null, "C", null, "KeyC", null, null, null));
        put(map, "D", new KeyDefinition(68, null, "D", null, "KeyD", null, null, null));
        put(map, "E", new KeyDefinition(69, null, "E", null, "KeyE", null, null, null));
        put(map, "F", new KeyDefinition(70, null, "F", null, "KeyF", null, null, null));
        put(map, "G", new KeyDefinition(71, null, "G", null, "KeyG", null, null, null));
        put(map, "H", new KeyDefinition(72, null, "H", null, "KeyH", null, null, null));
        put(map, "I", new KeyDefinition(73, null, "I", null, "KeyI", null, null, null));
        put(map, "J", new KeyDefinition(74, null, "J", null, "KeyJ", null, null, null));
        put(map, "K", new KeyDefinition(75, null, "K", null, "KeyK", null, null, null));
        put(map, "L", new KeyDefinition(76, null, "L", null, "KeyL", null, null, null));
        put(map, "M", new KeyDefinition(77, null, "M", null, "KeyM", null, null, null));
        put(map, "N", new KeyDefinition(78, null, "N", null, "KeyN", null, null, null));
        put(map, "O", new KeyDefinition(79, null, "O", null, "KeyO", null, null, null));
        put(map, "P", new KeyDefinition(80, null, "P", null, "KeyP", null, null, null));
        put(map, "Q", new KeyDefinition(81, null, "Q", null, "KeyQ", null, null, null));
        put(map, "R", new KeyDefinition(82, null, "R", null, "KeyR", null, null, null));
        put(map, "S", new KeyDefinition(83, null, "S", null, "KeyS", null, null, null));
        put(map, "T", new KeyDefinition(84, null, "T", null, "KeyT", null, null, null));
        put(map, "U", new KeyDefinition(85, null, "U", null, "KeyU", null, null, null));
        put(map, "V", new KeyDefinition(86, null, "V", null, "KeyV", null, null, null));
        put(map, "W", new KeyDefinition(87, null, "W", null, "KeyW", null, null, null));
        put(map, "X", new KeyDefinition(88, null, "X", null, "KeyX", null, null, null));
        put(map, "Y", new KeyDefinition(89, null, "Y", null, "KeyY", null, null, null));
        put(map, "Z", new KeyDefinition(90, null, "Z", null, "KeyZ", null, null, null));
        put(map, Symbol.COLON, new KeyDefinition(186, null, Symbol.COLON, null, "Semicolon", null, null, null));
        put(map, Symbol.LT, new KeyDefinition(188, null, Symbol.LT, null, "Comma", null, null, null));
        put(map, Symbol.UNDERLINE, new KeyDefinition(189, null, Symbol.UNDERLINE, null, "Minus", null, null, null));
        put(map, Symbol.GT, new KeyDefinition(190, null, Symbol.GT, null, "Period", null, null, null));
        put(
                map,
                Symbol.QUESTION_MARK,
                new KeyDefinition(191, null, Symbol.QUESTION_MARK, null, "Slash", null, null, null));
        put(map, Symbol.TILDE, new KeyDefinition(192, null, Symbol.TILDE, null, "Backquote", null, null, null));
        put(
                map,
                Symbol.BRACE_LEFT,
                new KeyDefinition(219, null, Symbol.BRACE_LEFT, null, "BracketLeft", null, null, null));
        put(map, Symbol.OR, new KeyDefinition(220, null, Symbol.OR, null, "Backslash", null, null, null));
        put(
                map,
                Symbol.BRACE_RIGHT,
                new KeyDefinition(221, null, Symbol.BRACE_RIGHT, null, "BracketRight", null, null, null));
        put(
                map,
                Symbol.DOUBLE_QUOTES,
                new KeyDefinition(222, null, Symbol.DOUBLE_QUOTES, null, "Quote", null, null, null));
        put(map, "SoftLeft", new KeyDefinition(null, null, "SoftLeft", null, "SoftLeft", null, null, 4));
        put(map, "SoftRight", new KeyDefinition(null, null, "SoftRight", null, "SoftRight", null, null, 4));
        put(map, "Camera", new KeyDefinition(44, null, "Camera", null, "Camera", null, null, 4));
        put(map, "Call", new KeyDefinition(null, null, "Call", null, "Call", null, null, 4));
        put(map, "EndCall", new KeyDefinition(95, null, "EndCall", null, "EndCall", null, null, 4));
        put(map, "VolumeDown", new KeyDefinition(182, null, "VolumeDown", null, "VolumeDown", null, null, 4));
        put(map, "VolumeUp", new KeyDefinition(183, null, "VolumeUp", null, "VolumeUp", null, null, 4));
        return Map.copyOf(map);
    }

    /**
     * Handles put.
     *
     * @param map        map value
     * @param key        key value
     * @param definition definition value
     */
    private static void put(Map<String, KeyDefinition> map, String key, KeyDefinition definition) {
        map.put(key, definition);
    }

    /**
     * Represents a key definition value.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class KeyDefinition {

        /**
         * Current key code.
         */
        private final Integer keyCode;
        /**
         * Current shift key code.
         */
        private final Integer shiftKeyCode;
        /**
         * Current key.
         */
        private final String key;
        /**
         * Current shift key.
         */
        private final String shiftKey;
        /**
         * Current code.
         */
        private final String code;
        /**
         * Current text.
         */
        private final String text;
        /**
         * Current shift text.
         */
        private final String shiftText;
        /**
         * Current location.
         */
        private final Integer location;

        /**
         * Creates an instance.
         *
         * @param keyCode      key code value
         * @param shiftKeyCode shift key code value
         * @param key          key value
         * @param shiftKey     shift key value
         * @param code         code value
         * @param text         text to use
         * @param shiftText    shift text value
         * @param location     location value
         */
        public KeyDefinition(Integer keyCode, Integer shiftKeyCode, String key, String shiftKey, String code,
                String text, String shiftText, Integer location) {
            this.keyCode = keyCode;
            this.shiftKeyCode = shiftKeyCode;
            this.key = key;
            this.shiftKey = shiftKey;
            this.code = code;
            this.text = text;
            this.shiftText = shiftText;
            this.location = location;
        }

        /**
         * Returns the key code.
         *
         * @return key code value
         */
        public Integer keyCode() {
            return keyCode;
        }

        /**
         * Returns the shift key code.
         *
         * @return shift key code value
         */
        public Integer shiftKeyCode() {
            return shiftKeyCode;
        }

        /**
         * Returns the key.
         *
         * @return key value
         */
        public String key() {
            return key;
        }

        /**
         * Returns the shift key.
         *
         * @return shift key value
         */
        public String shiftKey() {
            return shiftKey;
        }

        /**
         * Returns the code.
         *
         * @return code value
         */
        public String code() {
            return code;
        }

        /**
         * Returns the text.
         *
         * @return text value
         */
        public String text() {
            return text;
        }

        /**
         * Returns the shift text.
         *
         * @return shift text value
         */
        public String shiftText() {
            return shiftText;
        }

        /**
         * Returns the location.
         *
         * @return location value
         */
        public Integer location() {
            return location;
        }
    }

    /**
     * Represents a key description value.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class KeyDescription {

        /**
         * Current key.
         */
        private final String key;
        /**
         * Current key code.
         */
        private final int keyCode;
        /**
         * Current code.
         */
        private final String code;
        /**
         * Current text.
         */
        private final String text;
        /**
         * Current location.
         */
        private final int location;

        /**
         * Creates an instance.
         *
         * @param key      key value
         * @param keyCode  key code value
         * @param code     code value
         * @param text     text to use
         * @param location location value
         */
        public KeyDescription(String key, int keyCode, String code, String text, int location) {
            this.key = key;
            this.keyCode = keyCode;
            this.code = code;
            this.text = text;
            this.location = location;
        }

        /**
         * Returns the key.
         *
         * @return key value
         */
        public String key() {
            return key;
        }

        /**
         * Returns the key code.
         *
         * @return key code value
         */
        public int keyCode() {
            return keyCode;
        }

        /**
         * Returns the code.
         *
         * @return code value
         */
        public String code() {
            return code;
        }

        /**
         * Returns the text.
         *
         * @return text value
         */
        public String text() {
            return text;
        }

        /**
         * Returns the location.
         *
         * @return location value
         */
        public int location() {
            return location;
        }
    }

}
