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
public class CDPSession extends Emitter<CDPSession.CDPSessionEvent> {

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
            try {
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
//                    CDPSessionEvent cdpSessionEvent = CDPSessionEvent.valueOf(method.replace(".", "_"));
//                    System.out.println("cdpSessionEvent="+cdpSessionEvent);
//                    if(cdpSessionEvent.equals(CDPSession.CDPSessionEvent.Target_attachedToTarget) || cdpSessionEvent.equals(CDPSession.CDPSessionEvent.Target_targetCreated) || cdpSessionEvent.equals(CDPSession.CDPSessionEvent.Target_targetInfoChanged)){
//                        System.out.println("CDP method="+method);
//                    }
                    this.emit(CDPSessionEvent.valueOf(method.replace(".", "_")),
                            Connection.classes.get(method) == null ? null
                                    : Builder.OBJECTMAPPER.treeToValue(paramsNode, Connection.classes.get(method)));
                }
            } catch (Exception e) {
                Logger.error("emit error out" + receivedNode);
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

    public enum CDPSessionEvent {
        CDPSession_Disconnected("CDPSession.Disconnected"), CDPSession_Swapped("CDPSession.Swapped"),
        CDPSession_Ready("CDPSession.Ready"), sessionAttached("sessionattached"), sessionDetached("sessiondetached"),
        // 暂时先放这里吧
        Page_domContentEventFired("Page.domContentEventFired"), Page_loadEventFired("Page.loadEventFired"),
        Page_javascriptDialogOpening("Page.javascriptDialogOpening"),

        Runtime_exceptionThrown("Runtime.exceptionThrown"), Inspector_targetCrashed("Inspector.targetCrashed"),
        Performance_metrics("Performance.metrics"), Log_entryAdde("Log.entryAdded"),
        Page_fileChooserOpened("Page.fileChooserOpened"),
        // 先暂时放在这里吧
        Target_targetCreated("Target.targetCreated"), Target_targetDestroyed("Target.targetDestroyed"),
        Target_targetInfoChanged("Target.targetInfoChanged"), Target_attachedToTarget("Target.attachedToTarget"),
        Target_detachedFromTarget("Target.detachedFromTarget"),
        // 先暂时放这里吧
        Debugger_scriptparsed("Debugger.scriptParsed"),
        Runtime_executionContextCreated("Runtime.executionContextCreated"),
        Runtime_executionContextDestroyed("Runtime.executionContextDestroyed"),
        Runtime_executionContextsCleared("Runtime.executionContextsCleared"),
        CSS_styleSheetAdded("CSS.styleSheetAdded"),
        // 先暂时放这里吧
        DeviceAccess_deviceRequestPrompted("DeviceAccess.deviceRequestPrompted"),
        Page_frameAttached("Page.frameAttached"), Page_frameNavigated("Page.frameNavigated"),
        Page_navigatedWithinDocument("Page.navigatedWithinDocument"), Page_frameDetached("Page.frameDetached"),
        Page_frameStoppedLoading("Page.frameStoppedLoading"), Page_lifecycleEvent("Page.lifecycleEvent"),
        targetcreated("targetcreated"), targetdestroyed("targetdestroyed"), targetchanged("targetchanged"),
        disconnected("disconnected"),

        Fetch_requestPaused("Fetch.requestPaused"), Fetch_authRequired("Fetch.authRequired"),
        Network_requestWillBeSent("Network.requestWillBeSent"),

        Network_requestServedFromCache("Network.requestServedFromCache"),
        Network_responseReceived("Network.responseReceived"), Network_loadingFinished("Network.loadingFinished"),
        Network_loadingFailed("Network.loadingFailed"), Runtime_consoleAPICalled("Runtime.consoleAPICalled"),
        Runtime_bindingCalled("Runtime.bindingCalled"), Tracing_tracingComplete("Tracing.tracingComplete");

        private String eventName;

        CDPSessionEvent(String eventName) {
            this.eventName = eventName;
        }

        public String getEventName() {
            return eventName;
        }

        public void setEventName(String eventName) {
            this.eventName = eventName;
        }
    }

}
