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

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;

import org.miaixz.lancia.nimble.PollingMode;

/**
 * Public selector wait options matching Puppeteer's WaitForSelectorOptions name.
 */
@Getter
@Setter
/**
 * Defines options for wait for selector operations.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class WaitForSelectorOptions {

    /**
     * Maximum wait duration.
     */
    private Duration timeout;

    /**
     * Whether the selector must become visible.
     */
    private Boolean visible;

    /**
     * Whether the selector must become hidden.
     */
    private Boolean hidden;

    /**
     * Polling strategy.
     */
    private PollingMode polling;

    /**
     * Polling interval in milliseconds.
     */
    private long pollingIntervalMillis;

    /**
     * Creates selector wait options.
     */
    public WaitForSelectorOptions() {
        // No initialization required.
    }

    /**
     * Returns the visible.
     *
     * @return {@code true} when the condition matches
     */
    public boolean visible() {
        return Boolean.TRUE.equals(visible);
    }

    /**
     * Updates visible.
     *
     * @param visible visible mode
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Returns the hidden.
     *
     * @return {@code true} when the condition matches
     */
    public boolean hidden() {
        return Boolean.TRUE.equals(hidden);
    }

    /**
     * Updates hidden.
     *
     * @param hidden hidden mode
     */
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * Converts timeout to milliseconds with the default Puppeteer timeout.
     *
     * @return timeout in milliseconds
     */
    public long timeoutMillis() {
        return timeoutMillis(30_000L);
    }

    /**
     * Converts timeout to milliseconds.
     *
     * @param fallback fallback timeout
     * @return timeout in milliseconds
     */
    public long timeoutMillis(long fallback) {
        return timeout == null ? fallback : timeout.toMillis();
    }

    /**
     * Updates timeout millis.
     *
     * @param timeoutMillis timeout in milliseconds
     */
    public void setTimeoutMillis(long timeoutMillis) {
        this.timeout = Duration.ofMillis(timeoutMillis);
    }

    /**
     * Returns polling strategy.
     *
     * @return polling strategy
     */
    public PollingMode polling() {
        return polling;
    }

    /**
     * Returns polling interval in milliseconds.
     *
     * @return polling interval in milliseconds
     */
    public long pollingIntervalMillis() {
        return pollingIntervalMillis;
    }

}
