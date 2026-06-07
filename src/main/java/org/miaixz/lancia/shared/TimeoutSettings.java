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
package org.miaixz.lancia.shared;

import java.time.Duration;

import org.miaixz.lancia.Builder;

/**
 * Stores default timeout settings for page operations.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class TimeoutSettings {

    /**
     * Default timeout millis.
     */
    public static final long DEFAULT_TIMEOUT_MILLIS = Builder.DEFAULT_TIMEOUT_MILLIS;
    /**
     * Current default timeout.
     */
    private Long defaultTimeout;
    /**
     * Current default navigation timeout.
     */
    private Long defaultNavigationTimeout;

    /**
     * Creates a timeout settings.
     */
    public TimeoutSettings() {
        this.defaultTimeout = null;
        this.defaultNavigationTimeout = null;
    }

    /**
     * Updates default timeout.
     *
     * @param timeoutMillis timeout in milliseconds
     */
    public void setDefaultTimeout(long timeoutMillis) {
        this.defaultTimeout = timeoutMillis;
    }

    /**
     * Updates default timeout.
     *
     * @param timeout timeout value
     */
    public void setDefaultTimeout(Duration timeout) {
        this.defaultTimeout = timeout == null ? null : timeout.toMillis();
    }

    /**
     * Updates default navigation timeout.
     *
     * @param timeoutMillis timeout in milliseconds
     */
    public void setDefaultNavigationTimeout(long timeoutMillis) {
        this.defaultNavigationTimeout = timeoutMillis;
    }

    /**
     * Updates default navigation timeout.
     *
     * @param timeout timeout value
     */
    public void setDefaultNavigationTimeout(Duration timeout) {
        this.defaultNavigationTimeout = timeout == null ? null : timeout.toMillis();
    }

    /**
     * Returns the navigation timeout.
     *
     * @return navigation timeout value
     */
    public long navigationTimeout() {
        if (defaultNavigationTimeout != null) {
            return defaultNavigationTimeout;
        }
        if (defaultTimeout != null) {
            return defaultTimeout;
        }
        return DEFAULT_TIMEOUT_MILLIS;
    }

    /**
     * Returns the timeout.
     *
     * @return timeout value
     */
    public long timeout() {
        if (defaultTimeout != null) {
            return defaultTimeout;
        }
        return DEFAULT_TIMEOUT_MILLIS;
    }

    /**
     * Returns the navigation timeout duration.
     *
     * @return navigation timeout duration value
     */
    public Duration navigationTimeoutDuration() {
        return Duration.ofMillis(navigationTimeout());
    }

    /**
     * Returns the timeout duration.
     *
     * @return timeout duration value
     */
    public Duration timeoutDuration() {
        return Duration.ofMillis(timeout());
    }

    /**
     * Returns the default timeout millis.
     *
     * @return default timeout millis value
     */
    public Long defaultTimeoutMillis() {
        return defaultTimeout;
    }

    /**
     * Returns the default navigation timeout millis.
     *
     * @return default navigation timeout millis value
     */
    public Long defaultNavigationTimeoutMillis() {
        return defaultNavigationTimeout;
    }

}
