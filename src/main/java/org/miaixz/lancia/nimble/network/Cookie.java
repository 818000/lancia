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

import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.miaixz.bus.core.xyz.StringKit;

/**
 * Represents a cookie value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class Cookie {

    /**
     * Creates a cookie.
     */
    public Cookie() {
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
     * Current domain.
     */
    private String domain;
    /**
     * Current path.
     */
    private String path;
    /**
     * Current expires.
     */
    private double expires;
    /**
     * Current size.
     */
    private int size;
    /**
     * Whether HTTP only is enabled.
     */
    private boolean httpOnly;
    /**
     * Whether secure is enabled.
     */
    private boolean secure;
    /**
     * Whether session is enabled.
     */
    private boolean session;
    /**
     * Current same site.
     */
    private String sameSite;
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
     * Whether partition key opaque is enabled.
     */
    private Boolean partitionKeyOpaque;

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", name);
        result.put("value", value);
        result.put("domain", domain);
        result.put("path", path);
        result.put("expires", expires);
        result.put("size", size);
        result.put("httpOnly", httpOnly);
        result.put("secure", secure);
        result.put("session", session);
        if (StringKit.isNotBlank(sameSite)) {
            result.put("sameSite", sameSite);
        }
        if (StringKit.isNotBlank(priority)) {
            result.put("priority", priority);
        }
        if (StringKit.isNotBlank(sourceScheme)) {
            result.put("sourceScheme", sourceScheme);
        }
        putPartitionKey(result, "partitionKey", partitionKey);
        if (partitionKeyOpaque != null) {
            result.put("partitionKeyOpaque", partitionKeyOpaque);
        }
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
     * Returns the expires.
     *
     * @return expires
     */
    public double getExpires() {
        return expires;
    }

    /**
     * Updates expires.
     *
     * @param expires expires value
     */
    public void setExpires(double expires) {
        this.expires = expires;
    }

    /**
     * Returns the size.
     *
     * @return size
     */
    public int getSize() {
        return size;
    }

    /**
     * Updates size.
     *
     * @param size size value
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Returns whether the cookie is HTTP-only.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isHttpOnly() {
        return httpOnly;
    }

    /**
     * Updates HTTP only.
     *
     * @param httpOnly http only
     */
    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    /**
     * Returns whether the cookie is secure.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * Updates secure.
     *
     * @param secure secure value
     */
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    /**
     * Returns whether the cookie is session-only.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isSession() {
        return session;
    }

    /**
     * Updates session.
     *
     * @param session protocol session
     */
    public void setSession(boolean session) {
        this.session = session;
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
        this.partitionKey = normalizePartitionKey(partitionKey);
    }

    /**
     * Returns the partition key opaque.
     *
     * @return {@code true} when the condition matches
     */
    public Boolean getPartitionKeyOpaque() {
        return partitionKeyOpaque;
    }

    /**
     * Returns whether the partition key is opaque.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isPartitionKeyOpaque() {
        return Boolean.TRUE.equals(partitionKeyOpaque);
    }

    /**
     * Updates partition key opaque.
     *
     * @param partitionKeyOpaque partition key opaque value
     */
    public void setPartitionKeyOpaque(Boolean partitionKeyOpaque) {
        this.partitionKeyOpaque = partitionKeyOpaque;
    }

    /**
     * Normalizes partition key.
     *
     * @param partitionKey partition key
     * @return normalize partition key value
     */
    public static Object normalizePartitionKey(Object partitionKey) {
        if (partitionKey == null || partitionKey instanceof String || partitionKey instanceof CookiePartitionKey) {
            return partitionKey;
        }
        if (partitionKey instanceof URI || partitionKey instanceof URL) {
            return String.valueOf(partitionKey);
        }
        if (partitionKey instanceof Map<?, ?> map) {
            CookiePartitionKey key = new CookiePartitionKey();
            Object sourceOrigin = map.get("sourceOrigin");
            Object hasCrossSiteAncestor = map.get("hasCrossSiteAncestor");
            key.setSourceOrigin(sourceOrigin == null ? null : String.valueOf(sourceOrigin));
            if (hasCrossSiteAncestor instanceof Boolean bool) {
                key.setHasCrossSiteAncestor(bool);
            }
            return key;
        }
        throw new IllegalArgumentException("Invalid cookie partitionKey type: " + partitionKey.getClass().getName());
    }

    /**
     * Handles put partition key.
     *
     * @param target target object
     * @param name   name to use
     * @param value  value to use
     */
    public static void putPartitionKey(Map<String, Object> target, String name, Object value) {
        Object normalized = normalizePartitionKey(value);
        if (normalized instanceof CookiePartitionKey key) {
            target.put(name, key.toMap());
            return;
        }
        if (normalized != null) {
            target.put(name, normalized);
        }
    }

    /**
     * Represents a cookie partition key value.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static class CookiePartitionKey {

        /**
         * Creates an instance.
         */
        public CookiePartitionKey() {
            // No initialization required.
        }

        /**
         * Current source origin.
         */
        private String sourceOrigin;
        /**
         * Whether has cross site ancestor is enabled.
         */
        private Boolean hasCrossSiteAncestor;

        /**
         * Converts this value to protocol parameters.
         *
         * @return protocol parameters
         */
        public Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            if (StringKit.isNotBlank(sourceOrigin)) {
                result.put("sourceOrigin", sourceOrigin);
            }
            if (hasCrossSiteAncestor != null) {
                result.put("hasCrossSiteAncestor", hasCrossSiteAncestor);
            }
            return result;
        }

        /**
         * Returns the source origin.
         *
         * @return source origin
         */
        public String getSourceOrigin() {
            return sourceOrigin;
        }

        /**
         * Updates source origin.
         *
         * @param sourceOrigin source origin value
         */
        public void setSourceOrigin(String sourceOrigin) {
            this.sourceOrigin = sourceOrigin;
        }

        /**
         * Returns the has cross site ancestor.
         *
         * @return {@code true} when the condition matches
         */
        public Boolean getHasCrossSiteAncestor() {
            return hasCrossSiteAncestor;
        }

        /**
         * Returns whether a cross-site ancestor exists.
         *
         * @return {@code true} when the condition matches
         */
        public boolean isHasCrossSiteAncestor() {
            return Boolean.TRUE.equals(hasCrossSiteAncestor);
        }

        /**
         * Updates has cross site ancestor.
         *
         * @param hasCrossSiteAncestor has cross site ancestor value
         */
        public void setHasCrossSiteAncestor(Boolean hasCrossSiteAncestor) {
            this.hasCrossSiteAncestor = hasCrossSiteAncestor;
        }
    }

}
