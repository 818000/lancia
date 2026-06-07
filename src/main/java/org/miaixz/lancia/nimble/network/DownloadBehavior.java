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

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a download behavior value.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class DownloadBehavior {

    /**
     * Shared constant for policy deny.
     */
    public static final String POLICY_DENY = "deny";

    /**
     * Shared constant for policy allow.
     */
    public static final String POLICY_ALLOW = "allow";

    /**
     * Shared constant for policy allow and name.
     */
    public static final String POLICY_ALLOW_AND_NAME = "allowAndName";

    /**
     * Shared constant for policy default.
     */
    public static final String POLICY_DEFAULT = "default";

    /**
     * Creates a download behavior.
     */
    public DownloadBehavior() {
        // No initialization required.
    }

    /**
     * Current policy.
     */
    private String policy = POLICY_ALLOW;
    /**
     * Current download path.
     */
    private Path downloadPath;
    /**
     * Whether download events are enabled.
     */
    private boolean eventsEnabled;

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("behavior", policy);
        if (downloadPath != null) {
            result.put("downloadPath", downloadPath.toString());
        }
        result.put("eventsEnabled", eventsEnabled);
        return result;
    }

    /**
     * Converts this value to common map.
     *
     * @return mapped values
     */
    public Map<String, Object> toCommonMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("policy", policy);
        if (downloadPath != null) {
            result.put("downloadPath", downloadPath.toString());
        }
        return result;
    }

    /**
     * Returns the allow.
     *
     * @param downloadPath download path value
     * @return allow value
     */
    public static DownloadBehavior allow(Path downloadPath) {
        DownloadBehavior behavior = new DownloadBehavior();
        behavior.setPolicy(POLICY_ALLOW);
        behavior.setDownloadPath(downloadPath);
        return behavior;
    }

    /**
     * Returns the deny.
     *
     * @return deny value
     */
    public static DownloadBehavior deny() {
        DownloadBehavior behavior = new DownloadBehavior();
        behavior.setPolicy(POLICY_DENY);
        return behavior;
    }

    /**
     * Returns the allow and name.
     *
     * @param downloadPath download path value
     * @return allow and name value
     */
    public static DownloadBehavior allowAndName(Path downloadPath) {
        DownloadBehavior behavior = new DownloadBehavior();
        behavior.setPolicy(POLICY_ALLOW_AND_NAME);
        behavior.setDownloadPath(downloadPath);
        return behavior;
    }

    /**
     * Returns the default behavior.
     *
     * @return default behavior value
     */
    public static DownloadBehavior defaultBehavior() {
        DownloadBehavior behavior = new DownloadBehavior();
        behavior.setPolicy(POLICY_DEFAULT);
        return behavior;
    }

    /**
     * Returns the behavior.
     *
     * @return behavior
     */
    public String getBehavior() {
        return policy;
    }

    /**
     * Updates behavior.
     *
     * @param behavior behavior value
     */
    public void setBehavior(String behavior) {
        setPolicy(behavior);
    }

    /**
     * Returns the policy.
     *
     * @return policy
     */
    public String getPolicy() {
        return policy;
    }

    /**
     * Updates policy.
     *
     * @param policy policy value
     */
    public void setPolicy(String policy) {
        if (!POLICY_DENY.equals(policy) && !POLICY_ALLOW.equals(policy) && !POLICY_ALLOW_AND_NAME.equals(policy)
                && !POLICY_DEFAULT.equals(policy)) {
            throw new IllegalArgumentException("Unknown download policy: " + policy);
        }
        this.policy = policy;
    }

    /**
     * Returns the download path.
     *
     * @return download path
     */
    public Path getDownloadPath() {
        return downloadPath;
    }

    /**
     * Updates download path.
     *
     * @param downloadPath download path value
     */
    public void setDownloadPath(Path downloadPath) {
        this.downloadPath = downloadPath;
    }

    /**
     * Returns whether download events are enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isEventsEnabled() {
        return eventsEnabled;
    }

    /**
     * Updates events enabled.
     *
     * @param eventsEnabled events enabled value
     */
    public void setEventsEnabled(boolean eventsEnabled) {
        this.eventsEnabled = eventsEnabled;
    }

    /**
     * Returns the requires download path.
     *
     * @return {@code true} when the condition matches
     */
    public boolean requiresDownloadPath() {
        return POLICY_ALLOW.equals(policy) || POLICY_ALLOW_AND_NAME.equals(policy);
    }

    /**
     * Handles validate.
     */
    public void validate() {
        if (requiresDownloadPath() && downloadPath == null) {
            throw new IllegalStateException(policy + " download policy requires downloadPath.");
        }
    }

}
