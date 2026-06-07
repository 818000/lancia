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
package org.miaixz.lancia.shared.payload;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Payload;

/**
 * protocol neutral extension information.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class PayloadExtensionInfo {

    /**
     * manifest name pattern.
     */
    private static final Pattern NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    /**
     * manifest version pattern.
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile("\"version\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    /**
     * Default version.
     */
    private static final String DEFAULT_VERSION = Normal.UNKNOWN;
    /**
     * Default name.
     */
    private static final String DEFAULT_NAME = "extension";
    /**
     * Current identifier.
     */
    private final String id;
    /**
     * Current version.
     */
    private final String version;
    /**
     * Current name.
     */
    private final String name;
    /**
     * Current path.
     */
    private final String path;
    /**
     * Whether this feature is enabled.
     */
    private final boolean enabled;

    /**
     * Creates a payload extension info.
     *
     * @param id      identifier
     * @param version version
     * @param name    name to use
     * @param path    file path
     * @param enabled enabled
     */
    public PayloadExtensionInfo(String id, String version, String name, String path, boolean enabled) {
        this.id = requireText(id, "Extension id must not be blank.");
        this.version = blankToDefault(version, DEFAULT_VERSION);
        this.name = blankToDefault(name, DEFAULT_NAME);
        this.path = blankToDefault(path, Normal.EMPTY);
        this.enabled = enabled;
    }

    /**
     * Creates extension information from an install result payload.
     *
     * @param payload payload
     * @param path    path
     * @return converted value
     */
    public static PayloadExtensionInfo fromInstallResult(Payload payload, String path) {
        String id = PayloadReader.text(payload.get("id"));
        ManifestMetadata metadata = readManifest(path);
        return new PayloadExtensionInfo(id, metadata.version(), metadata.name(), path, true);
    }

    /**
     * Creates extension information from a protocol payload.
     *
     * @param payload payload
     * @return converted value
     */
    public static PayloadExtensionInfo fromProtocol(Payload payload) {
        Payload actualPayload = Assert.notNull(payload, "payload");
        return new PayloadExtensionInfo(PayloadReader.text(actualPayload.get("id")),
                PayloadReader.text(actualPayload.get("version")), PayloadReader.text(actualPayload.get("name")),
                PayloadReader.text(actualPayload.get("path")),
                actualPayload.get("enabled").isNull() || actualPayload.get("enabled").asBoolean());
    }

    /**
     * Creates local extension information.
     *
     * @param path path
     * @return local value
     */
    public static PayloadExtensionInfo local(String path) {
        ManifestMetadata metadata = readManifest(path);
        String id = "local-" + Integer.toHexString(Assert.notNull(path, "path").hashCode());
        return new PayloadExtensionInfo(id, metadata.version(), metadata.name(), path, true);
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        return id;
    }

    /**
     * Returns the version.
     *
     * @return version value
     */
    public String version() {
        return version;
    }

    /**
     * Returns the name.
     *
     * @return name value
     */
    public String name() {
        return name;
    }

    /**
     * Returns the path.
     *
     * @return path value
     */
    public String path() {
        return path;
    }

    /**
     * Returns the enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", id);
        value.put("version", version);
        value.put("name", name);
        value.put("path", path);
        value.put("enabled", enabled);
        return value;
    }

    /**
     * Reads manifest metadata.
     *
     * @param path path
     * @return read manifest value
     */
    private static ManifestMetadata readManifest(String path) {
        if (StringKit.isBlank(path)) {
            return new ManifestMetadata(DEFAULT_NAME, DEFAULT_VERSION);
        }
        Path manifest = Path.of(path).resolve("manifest.json");
        if (!FileKit.isFile(manifest.toFile())) {
            String fileName = Path.of(path).getFileName() == null ? DEFAULT_NAME
                    : Path.of(path).getFileName().toString();
            return new ManifestMetadata(fileName, DEFAULT_VERSION);
        }
        try {
            String text = FileKit.readUtf8String(manifest.toFile());
            return new ManifestMetadata(blankToDefault(readString(text, NAME_PATTERN), DEFAULT_NAME),
                    blankToDefault(readString(text, VERSION_PATTERN), DEFAULT_VERSION));
        } catch (Exception ignored) {
            return new ManifestMetadata(DEFAULT_NAME, DEFAULT_VERSION);
        }
    }

    /**
     * Reads a string value from manifest text.
     *
     * @param text    text
     * @param pattern pattern
     * @return read string value
     */
    private static String readString(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? unescape(matcher.group(1)) : Normal.EMPTY;
    }

    /**
     * Unescapes a JSON string fragment.
     *
     * @param value escaped JSON fragment
     * @return unescaped value
     */
    private static String unescape(String value) {
        return value.replace("\\\"", Symbol.DOUBLE_QUOTES).replace("\\\\", Symbol.BACKSLASH);
    }

    /**
     * Requires a non-blank text value.
     *
     * @param value   value
     * @param message message
     * @return require text value
     */
    private static String requireText(String value, String message) {
        if (StringKit.isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    /**
     * Converts blank text to a default value.
     *
     * @param value        value
     * @param defaultValue default value
     * @return blank to default value
     */
    private static String blankToDefault(String value, String defaultValue) {
        return StringKit.isBlank(value) ? defaultValue : value;
    }

    /**
     * manifest metadata.
     *
     * @param name    name
     * @param version version
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private record ManifestMetadata(String name, String version) {
    }

}
