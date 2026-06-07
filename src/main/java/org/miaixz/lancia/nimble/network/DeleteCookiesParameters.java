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
package org.miaixz.lancia.nimble.network;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a delete cookies parameters value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class DeleteCookiesParameters {

    /**
     * Creates a delete cookies parameters.
     */
    public DeleteCookiesParameters() {
        // No initialization required.
    }

    /**
     * Current name.
     */
    private String name;

    /**
     * Cookie URL.
     */
    private String url;
    /**
     * Current domain.
     */
    private String domain;
    /**
     * Current path.
     */
    private String path;
    /**
     * Current partition key.
     */
    private Object partitionKey;

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        put(result, "name", name);
        put(result, "url", url);
        put(result, "domain", domain);
        put(result, "path", path);
        Cookie.putPartitionKey(result, "partitionKey", partitionKey);
        return result;
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
     * Returns the URL.
     *
     * @return URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Updates URL.
     *
     * @param url target URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Returns the domain.
     *
     * @return domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Updates domain.
     *
     * @param domain domain value
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * Returns the path.
     *
     * @return path
     */
    public String getPath() {
        return path;
    }

    /**
     * Updates path.
     *
     * @param path file path
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Returns the partition key.
     *
     * @return partition key
     */
    public Object getPartitionKey() {
        return partitionKey;
    }

    /**
     * Updates partition key.
     *
     * @param partitionKey partition key value
     */
    public void setPartitionKey(Object partitionKey) {
        this.partitionKey = Cookie.normalizePartitionKey(partitionKey);
    }

    /**
     * Handles put.
     *
     * @param target target object
     * @param name   name to use
     * @param value  value to use
     */
    private void put(Map<String, Object> target, String name, Object value) {
        if (value != null) {
            target.put(name, value);
        }
    }

}
