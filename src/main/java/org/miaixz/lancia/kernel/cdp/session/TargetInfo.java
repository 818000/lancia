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
package org.miaixz.lancia.kernel.cdp.session;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;

/**
 * Represents target info.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class TargetInfo {

    /**
     * Current target ID.
     */
    private final String targetId;
    /**
     * Current type.
     */
    private final String type;
    /**
     * Current URL.
     */
    private final String url;
    /**
     * Current session ID.
     */
    private final String sessionId;
    /**
     * Current opener ID.
     */
    private final String openerId;
    /**
     * Current subtype.
     */
    private final String subtype;
    /**
     * Current browser context ID.
     */
    private final String browserContextId;

    /**
     * Creates a target info.
     *
     * @param targetId  target id
     * @param type      type name
     * @param url       target URL
     * @param sessionId session id
     */
    public TargetInfo(String targetId, String type, String url, String sessionId) {
        this(targetId, type, url, sessionId, Normal.EMPTY);
    }

    /**
     * Creates a target info.
     *
     * @param targetId  target id
     * @param type      type name
     * @param url       target URL
     * @param sessionId session id
     * @param openerId  opener id
     */
    public TargetInfo(String targetId, String type, String url, String sessionId, String openerId) {
        this(targetId, type, url, sessionId, openerId, Normal.EMPTY);
    }

    /**
     * Creates a target info.
     *
     * @param targetId  target id
     * @param type      type name
     * @param url       target URL
     * @param sessionId session id
     * @param openerId  opener id
     * @param subtype   subtype
     */
    public TargetInfo(String targetId, String type, String url, String sessionId, String openerId, String subtype) {
        this(targetId, type, url, sessionId, openerId, subtype, Normal.EMPTY);
    }

    /**
     * Creates a target info.
     *
     * @param targetId         target id
     * @param type             type name
     * @param url              target URL
     * @param sessionId        session id
     * @param openerId         opener id
     * @param subtype          subtype
     * @param browserContextId browser context id
     */
    public TargetInfo(String targetId, String type, String url, String sessionId, String openerId, String subtype,
            String browserContextId) {
        this.targetId = targetId;
        this.type = type;
        this.url = url;
        this.sessionId = sessionId;
        this.openerId = openerId;
        this.subtype = subtype;
        this.browserContextId = browserContextId;
    }

    /**
     * Creates this value from attached to target.
     *
     * @param params protocol parameters
     * @return from attached to target value
     */
    public static TargetInfo fromAttachedToTarget(CdpPayload params) {
        CdpPayload targetInfo = params.get("targetInfo");
        String targetId = targetInfo.get("targetId").isNull() ? Normal.EMPTY : targetInfo.get("targetId").asText();
        String type = targetInfo.get("type").isNull() ? Normal.EMPTY : targetInfo.get("type").asText();
        String url = targetInfo.get("url").isNull() ? Normal.EMPTY : targetInfo.get("url").asText();
        String openerId = targetInfo.get("openerId").isNull() ? Normal.EMPTY : targetInfo.get("openerId").asText();
        String subtype = targetInfo.get("subtype").isNull() ? Normal.EMPTY : targetInfo.get("subtype").asText();
        String browserContextId = targetInfo.get("browserContextId").isNull() ? Normal.EMPTY
                : targetInfo.get("browserContextId").asText();
        String sessionId = params.get("sessionId").isNull() ? Normal.EMPTY : params.get("sessionId").asText();
        return new TargetInfo(targetId, type, url, sessionId, openerId, subtype, browserContextId);
    }

    /**
     * Creates target info from a Target.targetCreated payload.
     *
     * @param payload targetCreated payload
     * @return parsed target info
     */
    public static TargetInfo fromTargetCreated(CdpPayload payload) {
        return fromTargetInfoPayload(payload.get("targetInfo"), Normal.EMPTY);
    }

    /**
     * Creates target info from a Target.targetInfoChanged payload.
     *
     * @param payload targetInfoChanged payload
     * @return parsed target info
     */
    public static TargetInfo fromTargetInfoChanged(CdpPayload payload) {
        return fromTargetInfoPayload(payload.get("targetInfo"), Normal.EMPTY);
    }

    /**
     * Creates target info from a targetInfo object.
     *
     * @param targetInfo targetInfo object
     * @param sessionId  session id
     * @return parsed target info
     */
    private static TargetInfo fromTargetInfoPayload(CdpPayload targetInfo, String sessionId) {
        String targetId = targetInfo.get("targetId").isNull() ? Normal.EMPTY : targetInfo.get("targetId").asText();
        String type = targetInfo.get("type").isNull() ? Normal.EMPTY : targetInfo.get("type").asText();
        String url = targetInfo.get("url").isNull() ? Normal.EMPTY : targetInfo.get("url").asText();
        String openerId = targetInfo.get("openerId").isNull() ? Normal.EMPTY : targetInfo.get("openerId").asText();
        String subtype = targetInfo.get("subtype").isNull() ? Normal.EMPTY : targetInfo.get("subtype").asText();
        String browserContextId = targetInfo.get("browserContextId").isNull() ? Normal.EMPTY
                : targetInfo.get("browserContextId").asText();
        return new TargetInfo(targetId, type, url, sessionId, openerId, subtype, browserContextId);
    }

    /**
     * Returns the target ID.
     *
     * @return target ID
     */
    public String getTargetId() {
        return targetId;
    }

    /**
     * Returns the type.
     *
     * @return type
     */
    public String getType() {
        return type;
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
     * Returns the session ID.
     *
     * @return session ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the opener ID.
     *
     * @return opener ID
     */
    public String getOpenerId() {
        return openerId;
    }

    /**
     * Returns the subtype.
     *
     * @return subtype
     */
    public String getSubtype() {
        return subtype;
    }

    /**
     * Returns the browser context ID.
     *
     * @return browser context ID
     */
    public String getBrowserContextId() {
        return browserContextId;
    }

}
