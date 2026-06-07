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
package org.miaixz.lancia;

import java.util.Map;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.lancia.kernel.Frame;
import org.miaixz.lancia.nimble.network.SecurityDetails;

/**
 * Public response API.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public abstract class Response {

    /**
     * Creates a response.
     */
    protected Response() {
        // No initialization required.
    }

    /**
     * Returns response status.
     *
     * @return status
     */
    public abstract int status();

    /**
     * Returns the ok.
     *
     * @return {@code true} when the condition matches
     */
    public abstract boolean ok();

    /**
     * Returns response headers.
     *
     * @return headers
     */
    public abstract Map<String, String> headers();

    /**
     * Returns response bytes.
     *
     * @return response bytes
     */
    public abstract byte[] buffer();

    /**
     * Returns response text.
     *
     * @return response text
     */
    public abstract String text();

    /**
     * Returns parsed JSON payload.
     *
     * @return parsed payload
     */
    public abstract Payload json();

    /**
     * Returns response content bytes.
     *
     * @return content bytes
     */
    public abstract byte[] content();

    /**
     * Returns response URL.
     *
     * @return URL
     */
    public abstract String url();

    /**
     * Returns response status text.
     *
     * @return status text
     */
    public abstract String statusText();

    /**
     * Returns the request associated with this response.
     *
     * @return request
     */
    public abstract Request request();

    /**
     * Returns the frame associated with this response.
     *
     * @return frame
     */
    public abstract Optional<? extends Frame> frame();

    /**
     * Returns security details.
     *
     * @return security details
     */
    public abstract Optional<? extends SecurityDetails> securityDetails();

    /**
     * Returns remote address details.
     *
     * @return remote address
     */
    public abstract RemoteAddress remoteAddress();

    /**
     * Returns response timing details.
     *
     * @return timing details
     */
    public abstract Map<String, Object> timing();

    /**
     * Creates this value from cache.
     *
     * @return {@code true} when the condition matches
     */
    public abstract boolean fromCache();

    /**
     * Creates this value from service worker.
     *
     * @return {@code true} when the condition matches
     */
    public abstract boolean fromServiceWorker();

    /**
     * Remote address details for the response connection.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class RemoteAddress {

        /**
         * Remote IP address.
         */
        private final String ip;

        /**
         * Remote port.
         */
        private final int port;

        /**
         * Creates an instance.
         *
         * @param ip   ip value
         * @param port port value
         */
        public RemoteAddress(String ip, int port) {
            this.ip = ip == null ? Normal.EMPTY : ip;
            this.port = port;
        }

        /**
         * Returns the remote IP address.
         *
         * @return ip
         */
        public String ip() {
            return ip;
        }

        /**
         * Returns the remote port.
         *
         * @return port
         */
        public int port() {
            return port;
        }

    }

}
