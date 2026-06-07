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
 * Represents a cookie param value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CookieParam {

    /**
     * Creates a cookie param.
     */
    public CookieParam() {
        // No initialization required.
    }

    /**
     * Current name.
     */
    private String name;
    /**
     * Current value.
     */
    private String value;

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
     * Whether secure is enabled.
     */
    private Boolean secure;
    /**
     * Whether HTTP only is enabled.
     */
    private Boolean httpOnly;
    /**
     * Current same site.
     */
    private String sameSite;
    /**
     * Current expires.
     */
    private Double expires;
    /**
     * Current priority.
     */
    private String priority;
    /**
     * Current source scheme.
     */
    private String sourceScheme;
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
        put(result, "value", value);
        put(result, "url", url);
        put(result, "domain", domain);
        put(result, "path", path);
        put(result, "secure", secure);
        put(result, "httpOnly", httpOnly);
        put(result, "sameSite", sameSite);
        put(result, "expires", expires);
        put(result, "priority", priority);
        put(result, "sourceScheme", sourceScheme);
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
     * Returns the value.
     *
     * @return value
     */
    public String getValue() {
        return value;
    }

    /**
     * Updates value.
     *
     * @param value to use
     */
    public void setValue(String value) {
        this.value = value;
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
     * Returns the secure.
     *
     * @return {@code true} when the condition matches
     */
    public Boolean getSecure() {
        return secure;
    }

    /**
     * Updates secure.
     *
     * @param secure secure value
     */
    public void setSecure(Boolean secure) {
        this.secure = secure;
    }

    /**
     * Returns the HTTP only.
     *
     * @return {@code true} when the condition matches
     */
    public Boolean getHttpOnly() {
        return httpOnly;
    }

    /**
     * Updates HTTP only.
     *
     * @param httpOnly http only
     */
    public void setHttpOnly(Boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    /**
     * Returns the same site.
     *
     * @return same site
     */
    public String getSameSite() {
        return sameSite;
    }

    /**
     * Updates same site.
     *
     * @param sameSite same site value
     */
    public void setSameSite(String sameSite) {
        this.sameSite = sameSite;
    }

    /**
     * Returns the expires.
     *
     * @return expires
     */
    public Double getExpires() {
        return expires;
    }

    /**
     * Updates expires.
     *
     * @param expires expires value
     */
    public void setExpires(Double expires) {
        this.expires = expires;
    }

    /**
     * Returns the priority.
     *
     * @return priority
     */
    public String getPriority() {
        return priority;
    }

    /**
     * Updates priority.
     *
     * @param priority priority value
     */
    public void setPriority(String priority) {
        this.priority = priority;
    }

    /**
     * Returns the source scheme.
     *
     * @return source scheme
     */
    public String getSourceScheme() {
        return sourceScheme;
    }

    /**
     * Updates source scheme.
     *
     * @param sourceScheme source scheme value
     */
    public void setSourceScheme(String sourceScheme) {
        this.sourceScheme = sourceScheme;
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
