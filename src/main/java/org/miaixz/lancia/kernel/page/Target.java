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
package org.miaixz.lancia.kernel.page;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Browser;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.kernel.TargetManager;
import org.miaixz.lancia.kernel.browser.Context;
import org.miaixz.lancia.socket.CDPSession;
import org.miaixz.lancia.socket.factory.SessionFactory;
import org.miaixz.lancia.worker.enums.InitializationStatus;
import org.miaixz.lancia.worker.enums.TargetType;

import io.reactivex.rxjava3.subjects.SingleSubject;

public class Target {

    private final Set<Target> childTargets = new HashSet<>();
    public SingleSubject<Boolean> isClosedSubject = SingleSubject.create();
    public SingleSubject<InitializationStatus> initializedSubject = SingleSubject.create();
    protected TargetInfo targetInfo;
    protected SessionFactory sessionFactory;
    protected Worker worker;
    private Context context;
    private CDPSession session;
    private TargetManager targetManager;
    private String targetId;

    public Target() {
        super();
    }

    public Target(TargetInfo targetInfo, CDPSession session, Context context, TargetManager targetManager,
            SessionFactory sessionFactory) {
        this.session = session;
        this.targetManager = targetManager;
        this.targetInfo = targetInfo;
        this.context = context;
        this.targetId = targetInfo.getTargetId();
        this.sessionFactory = sessionFactory;
        if (this.session != null) {
            this.session.setTarget(this);
        }
    }

    public Page asPage() {
        CDPSession session = this.session();
        if (session == null) {
            session = this.createCDPSession();
            return Page.create(session, this, null);
        }
        return Page.create(session, this, null);
    }

    public String subtype() {
        return this.targetInfo.getSubtype();
    }

    public CDPSession session() {
        return this.session;
    }

    public void addChildTarget(Target target) {
        this.childTargets.add(target);
    }

    public void removeChildTarget(Target target) {
        this.childTargets.remove(target);
    }

    public Set<Target> childTargets() {
        return this.childTargets;
    }

    public SessionFactory sessionFactory() {
        if (this.sessionFactory == null) {
            throw new InternalException("sessionFactory is not initialized");
        }
        return this.sessionFactory;
    }

    public CDPSession createCDPSession() {
        if (this.sessionFactory == null) {
            throw new InternalException("sessionFactory is not initialized");
        }
        CDPSession cdpSession = this.sessionFactory.create(false);
        cdpSession.setTarget(this);
        return cdpSession;
    }

    public String url() {
        return this.targetInfo.getUrl();
    }

    public TargetType type() {
        String type = this.targetInfo.getType();
        switch (type) {
        case "page":
            return TargetType.PAGE;
        case "background_page":
            return TargetType.BACKGROUND_PAGE;
        case "service_worker":
            return TargetType.SERVICE_WORKER;
        case "shared_worker":
            return TargetType.SHARED_WORKER;
        case "browser":
            return TargetType.BROWSER;
        case "webview":
            return TargetType.WEBVIEW;
        case "tab":
            return TargetType.TAB;
        default:
            return TargetType.OTHER;
        }
    }

    public TargetManager targetManager() {
        if (this.targetManager == null) {
            throw new InternalException("targetManager is not initialized");
        }
        return this.targetManager;
    }

    public TargetInfo getTargetInfo() {
        return this.targetInfo;
    }

    public Browser browser() {
        if (this.context == null) {
            throw new InternalException("browserContext is not initialized");
        }
        return this.context.browser();
    }

    public Context browserContext() {
        if (this.context == null) {
            throw new InternalException("browserContext is not initialized");
        }
        return this.context;
    }

    public Target opener() {
        String openerId = this.targetInfo.getOpenerId();
        if (StringKit.isEmpty(openerId)) {
            return null;
        }
        for (Target target : this.browser().targets()) {
            if (target.getTargetId().equals(openerId)) {
                return target;
            }
        }
        return null;
    }

    public void targetInfoChanged(TargetInfo targetInfo) {
        this.targetInfo = targetInfo;
        this.checkIfInitialized();
    }

    public void initialize() {
        this.initializedSubject.onSuccess(InitializationStatus.SUCCESS);
    }

    public boolean isTargetExposed() {
        return this.type() != TargetType.TAB && (this.subtype() == null);
    }

    private void checkIfInitialized() {
        if (!this.initializedSubject.hasValue()) {
            this.initializedSubject.onSuccess(InitializationStatus.SUCCESS);
        }
    }

    public Page page() {
        return null;
    }

    public String getTargetId() {
        return this.targetId;
    }

    public void waitForTargetClose() {
        // 使用blockingFirst来阻塞直到接收到关闭信号或超时
        this.isClosedSubject.timeout(Builder.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS).blockingGet();
    }

}
