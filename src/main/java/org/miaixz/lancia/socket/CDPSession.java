/*
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
 ~                                                                               ~
 ~ The MIT License (MIT)                                                         ~
 ~                                                                               ~
 ~ Copyright (c) 2015-2024 miaixz.org and other contributors.                    ~
 ~                                                                               ~
 ~ Permission is hereby granted, free of charge, to any person obtaining a copy  ~
 ~ of this software and associated documentation files (the "Software"), to deal ~
 ~ in the Software without restriction, including without limitation the rights  ~
 ~ to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     ~
 ~ copies of the Software, and to permit persons to whom the Software is         ~
 ~ furnished to do so, subject to the following conditions:                      ~
 ~                                                                               ~
 ~ The above copyright notice and this permission notice shall be included in    ~
 ~ all copies or substantial portions of the Software.                           ~
 ~                                                                               ~
 ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    ~
 ~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      ~
 ~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   ~
 ~ AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        ~
 ~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, ~
 ~ OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     ~
 ~ THE SOFTWARE.                                                                 ~
 ~                                                                               ~
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
*/
package org.miaixz.lancia.socket;

import static org.miaixz.lancia.Builder.createProtocolErrorMessage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.exception.ProtocolException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Emitter;
import org.miaixz.lancia.kernel.page.Target;
import org.miaixz.lancia.worker.enums.CDPSessionEvent;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The CDPSession instances are used to talk raw Chrome Devtools Protocol:
 *
 * protocol methods can be called with session.send method. protocol events can be subscribed to with session.on method.
 * Useful links:
 *
 * Documentation on DevTools Protocol can be found here: DevTools Protocol Viewer. Getting Started with :
 * <a href="https://github.com/aslushnikov/getting-started-with-cdp/blob/master/README.md">DevTools Protocol</a>
 */
public class CDPSession extends Emitter<CDPSessionEvent> {

    private final CallbackRegistry callbacks = new CallbackRegistry();
    private final String targetType;
    private final String sessionId;
    private final String parentSessionId;
    private Connection connection;
    private Target target;
    private List<String> events = null;

    public CDPSession(Connection connection, String targetType, String sessionId, String parentSessionId) {
        super();
        this.targetType = targetType;
        this.sessionId = sessionId;
        this.connection = connection;
        this.parentSessionId = parentSessionId;
    }

    public CDPSession parentSession() {
        if (StringKit.isEmpty(this.parentSessionId)) {
            // To make it work in Firefox that does not have parent (tab) sessions.
            return this;
        }
        if (this.connection != null) {
            return this.connection.session(this.parentSessionId);
        } else {
            return null;
        }
    }

    public void onClosed() {
        this.callbacks.clear();
        this.connection = null;
        this.emit(CDPSessionEvent.CDPSession_Disconnected, null);
    }

    public JsonNode send(String method) {
        if (connection == null) {
            throw new InternalException("Protocol error (" + method + "): Session closed. Most likely the"
                    + this.targetType + "has been closed.");
        }
        return this.connection.rawSend(this.callbacks, method, null, this.sessionId, null, true);
    }

    public JsonNode send(String method, Map<String, Object> params) {
        if (connection == null) {
            throw new InternalException("Protocol error (" + method + "): Session closed. Most likely the"
                    + this.targetType + "has been closed.");
        }
        return this.connection.rawSend(this.callbacks, method, params, this.sessionId, null, true);
    }

    public JsonNode send(String method, Map<String, Object> params, Integer timeout, boolean isBlocking) {
        if (connection == null) {
            throw new InternalException("Protocol error (" + method + "): Session closed. Most likely the"
                    + this.targetType + "has been closed.");
        }
        return this.connection.rawSend(this.callbacks, method, params, this.sessionId, timeout, isBlocking);
    }

    /**
     * 页面分离浏览器
     */
    public void detach() {
        if (connection == null) {
            throw new InternalException(
                    "Session already detached. Most likely the" + this.targetType + "has been closed.");
        }
        Map<String, Object> params = new HashMap<>();
        params.put("sessionId", this.sessionId);
        this.connection.send("Target.detachFromTarget", params, null, true);
    }

    /**
     * receivedNode的结构
     *
     * <pre>
     *  {
     *    id?: number;
     *    method: keyof CDPEvents;
     *    params: CDPEvents[keyof CDPEvents];
     *    error: {message: string; data: any; code: number};
     *    result?: any;
     *   }
     * </pre>
     *
     * @param receivedNode 接受到的返回值
     */
    public void onMessage(JsonNode receivedNode) {
        JsonNode idNode = receivedNode.get(Builder.MESSAGE_ID_PROPERTY);
        if (idNode != null) {// 有id,表示有callback
            int id = idNode.asInt();
            if (receivedNode.hasNonNull("error")) {// 发生错误，callback设置错误
                this.callbacks.reject(id, createProtocolErrorMessage(receivedNode),
                        receivedNode.get("error").get("message").asText());
            } else {
                this.callbacks.resolve(id, receivedNode.get("result"));
            }
        } else {// 没有id,是事件
            JsonNode paramsNode = receivedNode.get(Builder.MESSAGE_PARAMS_PROPERTY);
            JsonNode methodNode = receivedNode.get(Builder.MESSAGE_METHOD_PROPERTY);

            if (methodNode != null) {// 发射数据，执行事件的监听方法
                String method = methodNode.asText();
                if (events == null) {
                    events = Arrays.stream(CDPSessionEvent.values()).map(CDPSessionEvent::getEventName)
                            .collect(Collectors.toList());
                }
                boolean match = events.contains(method);
                if (!match) {// 不匹配就是没有监听该事件
                    return;
                }
                /*
                 * CDPSessionEvent cdpSessionEvent = CDPSessionEvent.valueOf(method.replace(".", "_"));
                 * Logger.info("cdpSessionEvent=" + cdpSessionEvent); if
                 * (cdpSessionEvent.equals(CDPSessionEvent.Target_attachedToTarget) ||
                 * cdpSessionEvent.equals(CDPSessionEvent.Target_targetCreated) ||
                 * cdpSessionEvent.equals(CDPSessionEvent.Target_targetInfoChanged)) { Logger.info("CDP method=" +
                 * method); }
                 */
                try {
                    this.emit(CDPSessionEvent.valueOf(method.replace(".", "_")),
                            Connection.classes.get(method) == null ? null
                                    : Builder.OBJECTMAPPER.treeToValue(paramsNode, Connection.classes.get(method)));
                } catch (Exception e) {
                    Logger.warn("emit error out" + receivedNode);
                }
            }
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public String id() {
        return this.sessionId;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public List<ProtocolException> getPendingProtocolErrors() {
        return this.callbacks.getPendingProtocolErrors();
    }

}
