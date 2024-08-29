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
package org.miaixz.lancia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.CollKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.kernel.*;
import org.miaixz.lancia.kernel.browser.Context;
import org.miaixz.lancia.kernel.page.Target;
import org.miaixz.lancia.kernel.page.TargetInfo;
import org.miaixz.lancia.option.BrowserContextOptions;
import org.miaixz.lancia.option.data.Debug;
import org.miaixz.lancia.option.data.GetVersionResponse;
import org.miaixz.lancia.option.data.Viewport;
import org.miaixz.lancia.socket.Connection;
import org.miaixz.lancia.socket.factory.SessionFactory;
import org.miaixz.lancia.worker.enums.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;

/**
 * 浏览器实例
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class Browser extends Emitter<BrowserEvent> {

    private final Viewport defaultViewport;
    private final Process process;
    private final Connection connection;
    private final Runnable closeCallback;
    private final Context defaultContext;
    private final Map<String, Context> contexts = new HashMap<>();
    private final TargetManager targetManager;
    private final Consumer<Target> onAttachedToTarget = (target) -> {
        if (target.isTargetExposed() && target.initializedSubject.blockingGet().equals(InitializationStatus.SUCCESS)) {
            this.emit(BrowserEvent.TargetCreated, target);
            target.browserContext().emit(BrowserContextEvent.TargetCreated, target);
        }
    };
    private final Consumer<Object> emitDisconnected = (ignore) -> this.emit(BrowserEvent.Disconnected, null);
    private final Consumer<Target> onDetachedFromTarget = (target) -> {
        target.initializedSubject.onSuccess(InitializationStatus.ABORTED);
        target.isClosedSubject.onSuccess(true);
        if (target.isTargetExposed() && target.initializedSubject.blockingGet().equals(InitializationStatus.SUCCESS)) {
            this.emit(BrowserEvent.TargetDestroyed, target);
            target.browserContext().emit(BrowserContextEvent.TargetDestroyed, target);
        }
    };
    private final Consumer<Target> onTargetChanged = (target) -> {
        this.emit(BrowserEvent.TargetChanged, target);
        target.browserContext().emit(BrowserContextEvent.TargetChanged, target);
    };
    private final Consumer<TargetInfo> onTargetDiscovered = (target) -> {
        this.emit(BrowserEvent.TargetDiscovered, target);
    };
    Function<Target, Boolean> targetFilterCallback;
    Function<Target, Boolean> isPageTargetCallback;

    public Browser(String product, Connection connection, List<String> contextIds, Viewport viewport, Process process,
            Runnable closeCallback, Function<Target, Boolean> targetFilterCallback,
            Function<Target, Boolean> isPageTargetCallback, boolean waitForInitiallyDiscoveredTargets) {
        super();
        product = StringKit.isEmpty(product) ? "chrome" : product;
        this.defaultViewport = viewport;
        this.process = process;
        this.connection = connection;
        if (closeCallback == null) {
            closeCallback = () -> {
            };
        }
        this.closeCallback = closeCallback;
        if (targetFilterCallback == null) {
            targetFilterCallback = (ignore) -> true;
        }
        this.targetFilterCallback = targetFilterCallback;
        this.setIsPageTargetCallback(isPageTargetCallback);
        if ("firefox".equals(product)) {
            throw new InternalException("Not Support firefox");
        } else {
            this.targetManager = new ChromeTargetManager(connection, this.createTarget(), this.targetFilterCallback,
                    waitForInitiallyDiscoveredTargets);
        }
        this.defaultContext = new Context(connection, this, "");
        if (CollKit.isNotEmpty(contextIds)) {
            for (String contextId : contextIds) {
                this.contexts.putIfAbsent(contextId, new Context(this.connection, this, contextId));
            }
        }
    }

    public static Browser create(String product, Connection connection, List<String> contextIds,
            boolean acceptInsecureCerts, Viewport defaultViewport, Process process, Runnable closeCallback,
            Function<Target, Boolean> targetFilterCallback, Function<Target, Boolean> IsPageTargetCallback,
            boolean waitForInitiallyDiscoveredTargets) {
        Browser browser = new Browser(product, connection, contextIds, defaultViewport, process, closeCallback,
                targetFilterCallback, IsPageTargetCallback, waitForInitiallyDiscoveredTargets);
        if (acceptInsecureCerts) {
            Map<String, Object> params = new HashMap<>();
            params.put("ignore", true);
            connection.send("Security.setIgnoreCertificateErrors", params);
        }
        browser.attach();
        return browser;
    }

    private void attach() {
        this.connection.on(CDPSessionEvent.CDPSession_Disconnected, this.emitDisconnected);
        this.targetManager.on(TargetManagerType.TargetAvailable, this.onAttachedToTarget);
        this.targetManager.on(TargetManagerType.TargetGone, this.onDetachedFromTarget);
        this.targetManager.on(TargetManagerType.TargetChanged, this.onTargetChanged);
        this.targetManager.on(TargetManagerType.TargetDiscovered, this.onTargetDiscovered);
        this.targetManager.initialize();
    }

    private void detach() {
        this.connection.off(CDPSessionEvent.CDPSession_Disconnected, this.emitDisconnected);
        this.targetManager.off(TargetManagerType.TargetAvailable, this.onAttachedToTarget);
        this.targetManager.off(TargetManagerType.TargetGone, this.onDetachedFromTarget);
        this.targetManager.off(TargetManagerType.TargetChanged, this.onTargetChanged);
        this.targetManager.off(TargetManagerType.TargetDiscovered, this.onTargetDiscovered);
    }

    public Process process() {
        return this.process;
    }

    public TargetManager targetManager() {
        return this.targetManager;
    }

    public Function<Target, Boolean> getIsPageTargetCallback() {
        return this.isPageTargetCallback;
    }

    private void setIsPageTargetCallback(Function<Target, Boolean> isPageTargetCallback) {
        if (isPageTargetCallback == null) {
            isPageTargetCallback = (target -> TargetType.PAGE.equals(target.type())
                    || TargetType.BACKGROUND_PAGE.equals(target.type()) || TargetType.WEBVIEW.equals(target.type()));
        }
        this.isPageTargetCallback = isPageTargetCallback;
    }

    public void disposeContext(String contextId) {
        if (StringKit.isEmpty(contextId)) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("browserContextId", contextId);
        this.connection.send("Target.disposeBrowserContext", params);
        this.contexts.remove(contextId);
    }

    public Context createBrowserContext(BrowserContextOptions options) {
        Map<String, Object> params = new HashMap<>();
        params.put("proxyServer", options.getProxyServer());
        if (CollKit.isNotEmpty(options.getProxyBypassList())) {
            params.put("proxyBypassList", String.join(",", options.getProxyBypassList()));
        }
        JsonNode result = this.connection.send("Target.createBrowserContext", params);
        Context context = new Context(this.connection, this, result.get("browserContextId").asText());
        this.contexts.put(result.get("browserContextId").asText(), context);
        return context;
    }

    public List<Context> browserContexts() {
        List<Context> contexts = new ArrayList<>();
        contexts.add(this.defaultBrowserContext());
        contexts.addAll(this.contexts.values());
        return contexts;
    }

    public Context defaultBrowserContext() {
        return this.defaultContext;
    }

    private TargetManager.TargetFactory createTarget() {
        return (targetInfo, session, parentSession) -> {
            String browserContextId = targetInfo.getBrowserContextId();
            Context context;
            if (StringKit.isNotEmpty(browserContextId) && this.contexts.containsKey(browserContextId)) {
                context = this.contexts.get(browserContextId);
            } else {
                context = this.defaultContext;
            }
            if (context == null) {
                throw new InternalException("Missing browser context");
            }
            SessionFactory createSession = (isAutoAttachEmulated) -> this.connection._createSession(targetInfo,
                    isAutoAttachEmulated);
            OtherTarget otherTarget = new OtherTarget(targetInfo, session, context, this.targetManager, createSession);
            if (StringKit.isNotEmpty(targetInfo.getUrl()) && targetInfo.getUrl().startsWith("devtools://")) {
                return new DevToolsTarget(targetInfo, session, context, this.targetManager, createSession,
                        this.defaultViewport);
            }
            if (this.isPageTargetCallback.apply(otherTarget)) {
                return new PageTarget(targetInfo, session, context, this.targetManager, createSession,
                        this.defaultViewport);
            }
            if ("service_worker".equals(targetInfo.getType()) || "shared_worker".equals(targetInfo.getType())) {
                return new WorkerTarget(targetInfo, session, context, this.targetManager, createSession);
            }
            return otherTarget;
        };
    }

    public String wsEndpoint() {
        return this.connection.url();
    }

    public Page newPage() {
        return this.defaultContext.newPage();
    }

    public Page createPageInContext(String contextId) {
        Map<String, Object> params = new HashMap<>();
        params.put("url", "about:blank");
        if (StringKit.isNotEmpty(contextId)) {
            params.put("browserContextId", contextId);
        }
        JsonNode result = this.connection.send("Target.createTarget", params);
        if (result != null) {
            String targetId = result.get(Builder.MESSAGE_TARGETID_PROPERTY).asText();
            Target target = this.waitForTarget(t -> t.getTargetId().equals(targetId), Builder.DEFAULT_TIMEOUT);
            if (target == null) {
                throw new InternalException("Missing target for page (id = " + targetId + ")");
            }
            if (!target.initializedSubject.blockingGet().equals(InitializationStatus.SUCCESS)) {
                throw new InternalException("Failed to create target for page (id =" + targetId + ")");
            }
            Page page = target.page();
            if (page == null) {
                throw new InternalException("Failed to create a page for context (id = " + contextId + ")");
            }
            return page;
        } else {
            throw new InternalException("Failed to create target for page (id =" + contextId + ")");
        }
    }

    public Target target() {
        for (Target target : this.targets()) {
            if (TargetType.BROWSER.equals(target.type())) {
                return target;
            }
        }
        throw new InternalException("Browser target is not found");
    }

    public List<Target> targets() {
        return this.targetManager.getAvailableTargets().values().stream()
                .filter(target -> target.isTargetExposed()
                        && target.initializedSubject.blockingGet().equals(InitializationStatus.SUCCESS))
                .collect(Collectors.toList());
    }

    public String version() {
        GetVersionResponse version = this.getVersion();
        return version.getProduct();
    }

    public String userAgent() {
        GetVersionResponse version = this.getVersion();
        return version.getUserAgent();
    }

    public void close() {
        this.closeCallback.run();
        this.disconnect();
    }

    public void disconnect() {
        this.targetManager.dispose();
        this.connection.dispose();
        this.detach();
    }

    public boolean connected() {
        return !this.connection.closed;
    }

    private GetVersionResponse getVersion() {
        try {
            return Builder.OBJECTMAPPER.treeToValue(this.connection.send("Browser.getVersion"),
                    GetVersionResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Debug debug() {
        return Debug.builder().pendingProtocolErrors(this.connection.getPendingProtocolErrors()).build();
    }

    public Target waitForTarget(Predicate<Target> predicate, int timeout) {
        Observable<Target> targetCreateObservable = Builder.fromEmitterEvent(this, BrowserEvent.TargetCreated);
        Observable<Target> TargetChangeObservable = Builder.fromEmitterEvent(this, BrowserEvent.TargetChanged);
        @NonNull
        Observable<@NonNull Target> targetsObservable = Observable.fromIterable(this.targets());
        return Observable.mergeArray(targetCreateObservable, TargetChangeObservable, targetsObservable)
                .filter(predicate::test).timeout(timeout, TimeUnit.MILLISECONDS).blockingFirst();
    }

}
