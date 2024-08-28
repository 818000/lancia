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
package org.miaixz.lancia.kernel;

import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.kernel.browser.Context;
import org.miaixz.lancia.kernel.page.Target;
import org.miaixz.lancia.kernel.page.TargetInfo;
import org.miaixz.lancia.options.TargetType;
import org.miaixz.lancia.options.Viewport;
import org.miaixz.lancia.socket.CDPSession;
import org.miaixz.lancia.socket.factory.SessionFactory;

import io.reactivex.rxjava3.subjects.SingleSubject;

public class PageTarget extends Target {

    private final Viewport defaultViewport;
    protected SingleSubject<Page> pageSubject;

    public PageTarget(TargetInfo targetInfo, CDPSession session, Context context, TargetManager targetManager,
            SessionFactory sessionFactory, Viewport defaultViewport) {
        super(targetInfo, session, context, targetManager, sessionFactory);
        this.defaultViewport = defaultViewport;
    }

    public void initialize() {
        this.initializedSubject.doAfterSuccess(result -> {
            try {
                if ("aborted".equals(result.getStatus())) {
                    return;
                }
                Target opener = this.opener();
                if (opener == null) {
                    return;
                }
                if (!(opener instanceof PageTarget)) {
                    return;
                }
                if (((PageTarget) opener).pageSubject == null || !TargetType.PAGE.equals(this.type())) {
                    return;
                }
                Page openerPage = ((PageTarget) opener).pageSubject.blockingGet();
                if (openerPage.listenerCount(Page.PageEvent.POPUP) == 0) {
                    return;
                }
                Page pupopPage = this.page();
                pupopPage.emit(Page.PageEvent.POPUP, pupopPage);
            } catch (Exception e) {
                Logger.error("lancia error:", e);
            }
        }).subscribe();
        this.checkIfInitialized();
    }

    public Page page() {
        if (this.pageSubject == null) {
            pageSubject = SingleSubject.create();
            CDPSession session = this.session();
            if (session == null) {
                session = this.sessionFactory().create(false);
            }
            pageSubject.onSuccess(Page.create(session, this, this.defaultViewport));
        }
        return this.pageSubject.getValue();
    }

    public void checkIfInitialized() {
        if (this.initializedSubject.hasValue()) {
            return;
        }
        if (!"".equals(this.getTargetInfo().getUrl())) {
            this.initializedSubject.onSuccess(InitializationStatus.SUCCESS);
        }
    }
}
