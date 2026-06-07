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
package org.miaixz.lancia.kernel.cdp.protocol;

/**
 * Represents CDP envelope.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpEnvelope {

    /**
     * Current identifier.
     */
    private final Integer id;
    /**
     * Current method.
     */
    private final String method;
    /**
     * Current session ID.
     */
    private final String sessionId;
    /**
     * Current params.
     */
    private final CdpPayload params;

    /**
     * Current result.
     */
    private final CdpPayload result;
    /**
     * Current error.
     */
    private final CdpPayload error;

    /**
     * Creates a CDP envelope.
     *
     * @param id        identifier
     * @param method    protocol method
     * @param sessionId session id
     * @param params    protocol parameters
     * @param result    result value
     * @param error     error to propagate
     */
    public CdpEnvelope(Integer id, String method, String sessionId, CdpPayload params, CdpPayload result,
            CdpPayload error) {
        this.id = id;
        this.method = method;
        this.sessionId = sessionId;
        this.params = params == null ? CdpPayload.NULL : params;
        this.result = result == null ? CdpPayload.NULL : result;
        this.error = error == null ? CdpPayload.NULL : error;
    }

    /**
     * Returns the ID.
     *
     * @return ID
     */
    public Integer getId() {
        return id;
    }

    /**
     * Returns the method.
     *
     * @return method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the session ID.
     *
     * @return session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the params.
     *
     * @return params
     */
    public CdpPayload getParams() {
        return params;
    }

    /**
     * Returns the result.
     *
     * @return command result payload
     */
    public CdpPayload getResult() {
        return result;
    }

    /**
     * Returns the error.
     *
     * @return error
     */
    public CdpPayload getError() {
        return error;
    }

    /**
     * Returns whether ID is available.
     *
     * @return {@code true} when the condition matches
     */
    public boolean hasId() {
        return id != null;
    }

    /**
     * Returns whether event is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isEvent() {
        return method != null && id == null;
    }

}
