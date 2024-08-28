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

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.exception.ProtocolException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Emitter;
import org.miaixz.lancia.events.*;
import org.miaixz.lancia.kernel.page.TargetInfo;
import org.miaixz.lancia.nimble.debugger.ScriptParsedEvent;
import org.miaixz.lancia.nimble.fetch.AuthRequiredEvent;
import org.miaixz.lancia.nimble.fetch.RequestPausedEvent;
import org.miaixz.lancia.nimble.logging.EntryAddedEvent;
import org.miaixz.lancia.nimble.network.*;
import org.miaixz.lancia.nimble.page.*;
import org.miaixz.lancia.nimble.performance.MetricsEvent;
import org.miaixz.lancia.nimble.runtime.BindingCalledEvent;
import org.miaixz.lancia.nimble.runtime.ConsoleAPICalledEvent;
import org.miaixz.lancia.nimble.runtime.ExecutionContextCreatedEvent;
import org.miaixz.lancia.nimble.runtime.ExecutionContextDestroyedEvent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * web socket client 浏览器级别的连接
 */
public class Connection extends Emitter<CDPSession.CDPSessionEvent> implements Consumer<String> {

    public static final Map<String, Class<?>> classes = new HashMap<>() {
        {
            for (CDPSession.CDPSessionEvent event : CDPSession.CDPSessionEvent.values()) {
                if (event.getEventName().equals("CDPSession.Disconnected")) {
                    put(event.getEventName(), null);
                } else if (event.getEventName().equals("CDPSession.Swapped")) {
                    // todo
                } else if (event.getEventName().equals("CDPSession.Ready")) {
                    // todo
                } else if (event.getEventName().equals("sessionattached")) {
                    put(event.getEventName(), CDPSession.class);
                } else if (event.getEventName().equals("sessionDetached")) {
                    put(event.getEventName(), CDPSession.class);
                } else if (event.getEventName().equals("Target.targetCreated")) {
                    put(event.getEventName(), TargetCreatedEvent.class);
                } else if (event.getEventName().equals("Target.targetDestroyed")) {
                    put(event.getEventName(), TargetDestroyedEvent.class);
                } else if (event.getEventName().equals("Target.targetInfoChanged")) {
                    put(event.getEventName(), TargetInfoChangedEvent.class);
                } else if (event.getEventName().equals("Target.attachedToTarget")) {
                    put(event.getEventName(), AttachedToTargetEvent.class);
                } else if (event.getEventName().equals("Target.detachedFromTarget")) {
                    put(event.getEventName(), DetachedFromTargetEvent.class);
                } else if (event.getEventName().equals("Page.javascriptDialogOpening")) {
                    put(event.getEventName(), JavascriptDialogOpeningEvent.class);
                } else if (event.getEventName().equals("Runtime.exceptionThrown")) {
                    put(event.getEventName(), ExceptionThrownEvent.class);
                } else if (event.getEventName().equals("Performance.metrics")) {
                    put(event.getEventName(), MetricsEvent.class);
                } else if (event.getEventName().equals("Log.entryAdded")) {
                    put(event.getEventName(), EntryAddedEvent.class);
                } else if (event.getEventName().equals("Page.fileChooserOpened")) {
                    put(event.getEventName(), FileChooserOpenedEvent.class);
                } else if (event.getEventName().equals("Debugger.scriptParsed")) {
                    put(event.getEventName(), ScriptParsedEvent.class);
                } else if (event.getEventName().equals("Runtime.executionContextCreated")) {
                    put(event.getEventName(), ExecutionContextCreatedEvent.class);
                } else if (event.getEventName().equals("Runtime.executionContextDestroyed")) {
                    put(event.getEventName(), ExecutionContextDestroyedEvent.class);
                } else if (event.getEventName().equals("CSS.styleSheetAdded")) {
                    put(event.getEventName(), StyleSheetAddedEvent.class);
                } else if (event.getEventName().equals("Page.frameAttached")) {
                    put(event.getEventName(), FrameAttachedEvent.class);
                } else if (event.getEventName().equals("Page.frameNavigated")) {
                    put(event.getEventName(), FrameNavigatedEvent.class);
                } else if (event.getEventName().equals("Page.navigatedWithinDocument")) {
                    put(event.getEventName(), NavigatedWithinDocumentEvent.class);
                } else if (event.getEventName().equals("Page.frameDetached")) {
                    put(event.getEventName(), FrameDetachedEvent.class);
                } else if (event.getEventName().equals("Page.frameStoppedLoading")) {
                    put(event.getEventName(), FrameStoppedLoadingEvent.class);
                } else if (event.getEventName().equals("Page.lifecycleEvent")) {
                    put(event.getEventName(), LifecycleEvent.class);
                } else if (event.getEventName().equals("Fetch.requestPaused")) {
                    put(event.getEventName(), RequestPausedEvent.class);
                } else if (event.getEventName().equals("Fetch.authRequired")) {
                    put(event.getEventName(), AuthRequiredEvent.class);
                } else if (event.getEventName().equals("Network.requestWillBeSent")) {
                    put(event.getEventName(), RequestWillBeSentEvent.class);
                } else if (event.getEventName().equals("Network.requestServedFromCache")) {
                    put(event.getEventName(), RequestServedFromCacheEvent.class);
                } else if (event.getEventName().equals("Network.responseReceived")) {
                    put(event.getEventName(), ResponseReceivedEvent.class);
                } else if (event.getEventName().equals("Network.loadingFinished")) {
                    put(event.getEventName(), LoadingFinishedEvent.class);
                } else if (event.getEventName().equals("Network.loadingFailed")) {
                    put(event.getEventName(), LoadingFailedEvent.class);
                } else if (event.getEventName().equals("Runtime.consoleAPICalled")) {
                    put(event.getEventName(), ConsoleAPICalledEvent.class);
                } else if (event.getEventName().equals("Runtime.bindingCalled")) {
                    put(event.getEventName(), BindingCalledEvent.class);
                } else if (event.getEventName().equals("Tracing.tracingComplete")) {
                    put(event.getEventName(), TracingCompleteEvent.class);
                }
            }
        }
    };

    private final String url;
    private final ConnectionTransport transport;
    private final int delay;
    private final int timeout;
    private final Map<String, CDPSession> sessions = new HashMap<>();
    private final CallbackRegistry callbacks = new CallbackRegistry();// 并发
    public boolean closed;
    Set<String> manuallyAttached = new HashSet<>();
    private List<String> events = null;

    public Connection(String url, ConnectionTransport transport, int delay, int timeout) {
        super();
        this.url = url;
        this.transport = transport;
        this.delay = delay;
        this.timeout = timeout;
        this.transport.setConnection(this);
    }

    /**
     * 从{@link CDPSession}中拿到对应的{@link Connection}
     *
     * @param client cdpsession
     * @return Connection
     */
    public static Connection fromSession(CDPSession client) {
        return client.getConnection();
    }

    public boolean isAutoAttached(String targetId) {
        return !this.manuallyAttached.contains(targetId);
    }

    public JsonNode send(String method) {
        return this.rawSend(this.callbacks, method, null, null, this.timeout, true);
    }

    public JsonNode send(String method, Map<String, Object> params) {
        return this.rawSend(this.callbacks, method, params, null, this.timeout, true);
    }

    public JsonNode send(String method, Map<String, Object> params, Integer timeout, boolean isBlocking) {
        return this.rawSend(this.callbacks, method, params, null, timeout, isBlocking);
    }

    public JsonNode rawSend(CallbackRegistry callbacks, String method, Map<String, Object> params, String sessionId,
            Integer timeout, boolean isBlocking) {
        Assert.isTrue(!this.closed, "Protocol error: Connection closed.");
        if (timeout == null) {
            timeout = this.timeout;
        }
        return callbacks.create(method, timeout, (id) -> {
            ObjectNode objectNode = Builder.OBJECTMAPPER.createObjectNode();
            objectNode.put(Builder.MESSAGE_METHOD_PROPERTY, method);
            if (params != null) {
                objectNode.set(Builder.MESSAGE_PARAMS_PROPERTY, Builder.OBJECTMAPPER.valueToTree(params));
            }
            objectNode.put(Builder.MESSAGE_ID_PROPERTY, id);
            if (StringKit.isNotEmpty(sessionId)) {
                objectNode.put(Builder.MESSAGE_SESSION_ID_PROPERTY, sessionId);
            }
            String stringifiedMessage = objectNode.toString();
            Logger.trace("lancia:protocol:SEND ► {}", stringifiedMessage);
            this.transport.send(stringifiedMessage);
        }, isBlocking);
    }

    /**
     * recevie message from browser by websocket
     *
     * @param message 从浏览器接受到的消息
     */
    public void onMessage(String message) {
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                // 恢复中断状态
                Thread.currentThread().interrupt();
                Logger.error("slowMo browser Fail:", e);
            }
        }
        Logger.trace("lancia:protocol:RECV ◀ {}", message);
        try {
            if (StringKit.isEmpty(message)) {
                return;
            }
            JsonNode readTree = Builder.OBJECTMAPPER.readTree(message);
            String method = null;
            if (readTree.hasNonNull(Builder.MESSAGE_METHOD_PROPERTY)) {
                method = readTree.get(Builder.MESSAGE_METHOD_PROPERTY).asText();
            }
            String sessionId = null;
            JsonNode paramsNode = null;
            if (readTree.hasNonNull(Builder.MESSAGE_PARAMS_PROPERTY)) {
                paramsNode = readTree.get(Builder.MESSAGE_PARAMS_PROPERTY);
                if (paramsNode.hasNonNull(Builder.MESSAGE_SESSION_ID_PROPERTY)) {
                    sessionId = paramsNode.get(Builder.MESSAGE_SESSION_ID_PROPERTY).asText();
                }
            }
            String parentSessionId = null;
            if (readTree.hasNonNull(Builder.MESSAGE_SESSION_ID_PROPERTY)) {
                parentSessionId = readTree.get(Builder.MESSAGE_SESSION_ID_PROPERTY).asText();
            }
            if ("Target.attachedToTarget".equals(method)) {// attached to target -> page attached to browser
                JsonNode typeNode = paramsNode.get(Builder.MESSAGE_TARGETINFO_PROPERTY)
                        .get(Builder.MESSAGE_TYPE_PROPERTY);
                CDPSession cdpSession = new CDPSession(this, typeNode.asText(), sessionId, parentSessionId);
                this.sessions.put(sessionId, cdpSession);
                this.emit(CDPSession.CDPSessionEvent.sessionAttached, cdpSession);
                CDPSession parentSession = this.sessions.get(parentSessionId);
                if (parentSession != null) {
                    parentSession.emit(CDPSession.CDPSessionEvent.sessionAttached, cdpSession);
                }
            } else if ("Target.detachedFromTarget".equals(method)) {// 页面与浏览器脱离关系
                CDPSession cdpSession = this.sessions.get(sessionId);
                if (cdpSession != null) {
                    cdpSession.onClosed();
                    this.sessions.remove(sessionId);
                    this.emit(CDPSession.CDPSessionEvent.sessionDetached, cdpSession);
                    CDPSession parentSession = this.sessions.get(parentSessionId);
                    if (parentSession != null) {
                        parentSession.emit(CDPSession.CDPSessionEvent.sessionDetached, cdpSession);
                    }
                }
            }
            if (StringKit.isNotEmpty(parentSessionId)) {
                CDPSession parentSession = this.sessions.get(parentSessionId);
                if (parentSession != null) {
                    parentSession.onMessage(readTree);
                }
            } else if (readTree.hasNonNull(Builder.MESSAGE_ID_PROPERTY)) {// long类型的id,说明属于这次发送消息后接受的回应
                int id = readTree.get(Builder.MESSAGE_ID_PROPERTY).asInt();
                if (readTree.hasNonNull(Builder.MESSAGE_ERROR_PROPERTY)) {
                    this.callbacks.reject(id, createProtocolErrorMessage(readTree), readTree
                            .get(Builder.MESSAGE_ERROR_PROPERTY).get(Builder.MESSAGE_MESSAGE_PROPERTY).asText());
                } else {
                    this.callbacks.resolve(id, readTree.get(Builder.MESSAGE_RESULT_PROPERTY));
                }
            } else {// 是一个事件，那么响应监听器
                if (events == null) {
                    events = Arrays.stream(CDPSession.CDPSessionEvent.values())
                            .map(CDPSession.CDPSessionEvent::getEventName).collect(Collectors.toList());
                }
                boolean match = events.contains(method);
                if (!match) {// 不匹配就是没有监听该事件
                    return;
                }
                this.emit(CDPSession.CDPSessionEvent.valueOf(method.replace(".", "_")),
                        classes.get(method) == null ? null
                                : Builder.OBJECTMAPPER.treeToValue(paramsNode, classes.get(method)));
            }
        } catch (Exception e) {
            Logger.error("onMessage error:", e);
        }
    }

    /**
     * 创建一个{@link CDPSession}
     *
     * @param targetInfo target info
     * @return CDPSession client
     */
    public CDPSession createSession(TargetInfo targetInfo) {
        return this._createSession(targetInfo, false);
    }

    public CDPSession _createSession(TargetInfo targetInfo, boolean isAutoAttachEmulated) {
        if (!isAutoAttachEmulated) {
            this.manuallyAttached.add(targetInfo.getTargetId());
        }
        Map<String, Object> params = new HashMap<>();
        params.put("targetId", targetInfo.getTargetId());
        params.put("flatten", true);
        JsonNode receivedNode = this.send("Target.attachToTarget", params, null, true);
        this.manuallyAttached.remove(targetInfo.getTargetId());
        if (receivedNode.hasNonNull(Builder.MESSAGE_SESSION_ID_PROPERTY)) {
            CDPSession session = this.sessions.get(receivedNode.get(Builder.MESSAGE_SESSION_ID_PROPERTY).asText());
            if (session == null) {
                throw new InternalException("CDPSession creation failed.");
            }
            return session;
        } else {
            throw new InternalException("CDPSession creation failed.");
        }
    }

    public String url() {
        return this.url;
    }

    public String getUrl() {
        return url;
    }

    public CDPSession session(String sessionId) {
        return sessions.get(sessionId);
    }

    @Override
    public void accept(String t) {
        onMessage(t);
    }

    public void dispose() {
        this.onClose();// 清理Connection资源
        this.transport.close();// 关闭websocket
    }

    public void onClose() {
        if (this.closed)
            return;
        this.closed = true;
        this.transport.setConnection(null);
        this.callbacks.clear();
        for (CDPSession session : this.sessions.values())
            session.onClosed();
        this.sessions.clear();
        this.emit(CDPSession.CDPSessionEvent.CDPSession_Disconnected, null);
    }

    public List<ProtocolException> getPendingProtocolErrors() {
        List<ProtocolException> result = new ArrayList<>(this.callbacks.getPendingProtocolErrors());
        for (CDPSession session : this.sessions.values()) {
            result.addAll(session.getPendingProtocolErrors());
        }
        return result;
    }

}
