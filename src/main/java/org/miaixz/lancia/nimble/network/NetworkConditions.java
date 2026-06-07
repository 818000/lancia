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

/**
 * page network conditions.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class NetworkConditions {

    /**
     * Whether offline is enabled.
     */
    private final boolean offline;

    /**
     * Current latency.
     */
    private final double latency;

    /**
     * Current download throughput.
     */
    private final double downloadThroughput;

    /**
     * Current upload throughput.
     */
    private final double uploadThroughput;

    /**
     * Creates network conditions.
     *
     * @param offline            offline state
     * @param latency            latency
     * @param downloadThroughput download throughput
     * @param uploadThroughput   upload throughput
     */
    public NetworkConditions(boolean offline, double latency, double downloadThroughput, double uploadThroughput) {
        this.offline = offline;
        this.latency = latency;
        this.downloadThroughput = downloadThroughput;
        this.uploadThroughput = uploadThroughput;
    }

    /**
     * Returns online network conditions.
     *
     * @return network conditions
     */
    public static NetworkConditions online() {
        return new NetworkConditions(false, 0, -1, -1);
    }

    /**
     * Returns offline state.
     *
     * @return offline state
     */
    public boolean offline() {
        return offline;
    }

    /**
     * Returns latency.
     *
     * @return latency
     */
    public double latency() {
        return latency;
    }

    /**
     * Returns download throughput.
     *
     * @return download throughput
     */
    public double downloadThroughput() {
        return downloadThroughput;
    }

    /**
     * Returns upload throughput.
     *
     * @return upload throughput
     */
    public double uploadThroughput() {
        return uploadThroughput;
    }

}
