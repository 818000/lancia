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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.CollKit;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.worker.enums.FrameEvent;
import org.miaixz.lancia.worker.enums.FrameManagerType;
import org.miaixz.lancia.worker.enums.NetworkManagerType;
import org.miaixz.lancia.worker.enums.PuppeteerLifeCycle;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.SingleSubject;

public class LifecycleWatcher {

    private final List<String> expectedLifecycle = new ArrayList<>();
    private final List<Disposable> disposables = new ArrayList<>();
    private final SingleSubject<Boolean> lifecycleSubject = SingleSubject.create();
    private final SingleSubject<Boolean> sameDocumentNavigationSubject = SingleSubject.create();
    private final SingleSubject<Boolean> newDocumentNavigationSubject = SingleSubject.create();
    public SingleSubject<Exception> terminationSubject = SingleSubject.create();
    public SingleSubject<Boolean> navigationResponseReceived = SingleSubject.create();
    private Frame frame;
    private int timeout;
    private Request navigationRequest;
    private String initialLoaderId;
    private boolean hasSameDocumentNavigation;
    private boolean swapped = false;

    public LifecycleWatcher() {
        super();
    }

    public LifecycleWatcher(NetworkManager networkManager, Frame frame, List<PuppeteerLifeCycle> waitUntil,
            int timeout) {
        super();
        this.frame = frame;
        this.initialLoaderId = frame.getLoaderId();
        this.timeout = timeout;
        waitUntil.forEach(value -> {
            if (PuppeteerLifeCycle.DOMCONTENTLOADED.equals(value)) {
                this.expectedLifecycle.add("DOMContentLoaded");
            } else if (PuppeteerLifeCycle.NETWORKIDLE.equals(value)) {
                this.expectedLifecycle.add("networkIdle");
            } else if (PuppeteerLifeCycle.NETWORKIDLE2.equals(value)) {
                this.expectedLifecycle.add("networkAlmostIdle");
            } else if (PuppeteerLifeCycle.LOAD.equals(value)) {
                this.expectedLifecycle.add("load");
            } else {
                throw new IllegalArgumentException("Unknown value for options.waitUntil: " + value);
            }
        });
        this.disposables.add(Builder.fromEmitterEvent(frame.getFrameManager(), FrameManagerType.LifecycleEvent)
                .subscribe((ignore) -> this.checkLifecycleComplete()));
        this.disposables.add(Builder.fromEmitterEvent(frame, FrameEvent.FrameNavigatedWithinDocument)
                .subscribe((ignore) -> this.navigatedWithinDocument()));
        this.disposables.add(Builder.fromEmitterEvent(frame, FrameEvent.FrameNavigated)
                .subscribe((type) -> this.navigated((String) type)));
        this.disposables.add(
                Builder.fromEmitterEvent(frame, FrameEvent.FrameSwapped).subscribe((ignore) -> this.frameSwapped()));
        this.disposables.add(Builder.fromEmitterEvent(frame, FrameEvent.FrameSwappedByActivation)
                .subscribe((ignore) -> this.frameSwapped()));
        this.disposables.add(Builder.fromEmitterEvent(frame, FrameEvent.FrameDetached)
                .subscribe((frameParam) -> this.frameDetached((Frame) frameParam)));
        this.disposables.add(Builder.fromEmitterEvent(networkManager, NetworkManagerType.Request)
                .subscribe((request) -> this.onRequest((Request) request)));
        this.disposables.add(Builder.fromEmitterEvent(networkManager, NetworkManagerType.RequestFailed)
                .subscribe((request) -> this.onRequestFailed((Request) request)));
        this.disposables.add(Builder.fromEmitterEvent(networkManager, NetworkManagerType.Response)
                .subscribe((request) -> this.onResponse((Response) request)));
        this.checkLifecycleComplete();
    }

    private void onRequestFailed(Request request) {
        if (this.navigationRequest != null) {
            if (!this.navigationRequest.requestId().equals(request.requestId())) {
                return;
            }
        }
        if (this.navigationResponseReceived != null) {
            this.navigationResponseReceived.onSuccess(true);
        }
    }

    private void onResponse(Response response) {
        if (this.navigationRequest != null) {
            if (!this.navigationRequest.requestId().equals(response.request().requestId())) {
                return;
            }
        }
        if (this.navigationResponseReceived != null) {
            this.navigationResponseReceived.onSuccess(true);
        }
    }

    private void frameDetached(Frame frame) {
        if (this.frame.equals(frame)) {
            terminationSubject.onError(new InternalException("Navigating frame was detached'"));
            return;
        }
        this.checkLifecycleComplete();
    }

    /**
     *
     * @param navigationType ('Navigation' | 'BackForwardCacheRestore');
     */
    private void navigated(String navigationType) {
        if ("BackForwardCacheRestore".equals(navigationType)) {
            this.frameSwapped();
            return;
        }
        this.checkLifecycleComplete();
    }

    private void frameSwapped() {
        this.swapped = true;
        this.checkLifecycleComplete();
    }

    public boolean waitForSameDocumentNavigation() {
        return this.sameDocumentNavigationSubject.blockingGet();
    }

    public boolean waitForNewDocumentNavigation() {
        return this.newDocumentNavigationSubject.blockingGet();
    }

    private void onRequest(Request request) {
        if (!request.frame().equals(this.frame) || !request.isNavigationRequest())
            return;
        this.navigationRequest = request;
        if (this.navigationResponseReceived != null) {
            this.navigationResponseReceived.onSuccess(true);
        }
        this.navigationResponseReceived = SingleSubject.create();
        if (request.response() != null) {
            if (this.navigationResponseReceived != null) {
                this.navigationResponseReceived.onSuccess(true);
            }
        }
    }

    public void navigatedWithinDocument() {
        this.hasSameDocumentNavigation = true;
        this.checkLifecycleComplete();
    }

    private void checkLifecycleComplete() {
        // We expect navigation to commit.
        if (!checkLifecycle(this.frame, this.expectedLifecycle))
            return;
        this.lifecycleSubject.onSuccess(true);
        if (this.hasSameDocumentNavigation)
            this.sameDocumentNavigationSubject.onSuccess(true);
        if (this.swapped || !this.frame.getLoaderId().equals(this.initialLoaderId))
            this.newDocumentNavigationSubject.onSuccess(true);
    }

    /**
     * @param frame             frame
     * @param expectedLifecycle 生命周期集合
     * @return boolean 结果
     */
    private boolean checkLifecycle(Frame frame, List<String> expectedLifecycle) {
        if (CollKit.isNotEmpty(expectedLifecycle)) {
            for (String event : expectedLifecycle) {
                if (!frame.getLifecycleEvents().contains(event))
                    return false;
            }
        }
        if (CollKit.isNotEmpty(frame.childFrames())) {
            for (Frame child : frame.childFrames()) {
                if (!checkLifecycle(child, expectedLifecycle))
                    return false;
            }
        }
        return true;
    }

    public void waitForLifecycle() {
        this.lifecycleSubject.blockingSubscribe();
    }

    public void waitForTermination() {
        this.terminationSubject.timeout(this.timeout, TimeUnit.MILLISECONDS).blockingSubscribe();
    }

    public void dispose() {
        this.disposables.forEach(Disposable::dispose);
        this.terminationSubject.onSuccess(new InternalException("LifecycleWatcher disposed"));
        if (!this.lifecycleSubject.hasValue()) {
            this.lifecycleSubject.onSuccess(true);
        }
        if (!this.newDocumentNavigationSubject.hasValue()) {
            this.newDocumentNavigationSubject.onSuccess(true);
        }
        if (!this.sameDocumentNavigationSubject.hasValue()) {
            this.sameDocumentNavigationSubject.onSuccess(true);
        }
        if (!this.navigationResponseReceived.hasValue()) {
            this.navigationResponseReceived.onSuccess(true);
        }

    }

    public Response navigationResponse() {
        // TODO 暂时关闭
        // Optional.ofNullable(this.navigationResponseReceived).ifPresent(Single::blockingSubscribe);
        return this.navigationRequest != null ? this.navigationRequest.response() : null;
    }

}
