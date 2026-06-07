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

import java.util.Map;

/**
 * Defines options for permission operations.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class PermissionOptions {

    /**
     * Creates permission options.
     */
    public PermissionOptions() {
        // No initialization required.
    }

    /**
     * Current name.
     */
    private String name;
    /**
     * Current setting.
     */
    private String setting = "granted";

    /**
     * Converts this value to a permission descriptor.
     *
     * @return descriptor
     */
    public Map<String, Object> toDescriptor() {
        return Map.of("name", name);
    }

    /**
     * Returns the name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Updates name.
     *
     * @param name name to use
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the setting.
     *
     * @return setting
     */
    public String getSetting() {
        return setting;
    }

    /**
     * Updates setting.
     *
     * @param setting setting value
     */
    public void setSetting(String setting) {
        this.setting = setting;
    }

}
