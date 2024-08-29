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
package org.miaixz.lancia.worker.enums;

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
    Debugger_scriptparsed("Debugger.scriptParsed"), Runtime_executionContextCreated("Runtime.executionContextCreated"),
    Runtime_executionContextDestroyed("Runtime.executionContextDestroyed"),
    Runtime_executionContextsCleared("Runtime.executionContextsCleared"), CSS_styleSheetAdded("CSS.styleSheetAdded"),
    // 先暂时放这里吧
    DeviceAccess_deviceRequestPrompted("DeviceAccess.deviceRequestPrompted"), Page_frameAttached("Page.frameAttached"),
    Page_frameNavigated("Page.frameNavigated"), Page_navigatedWithinDocument("Page.navigatedWithinDocument"),
    Page_frameDetached("Page.frameDetached"), Page_frameStoppedLoading("Page.frameStoppedLoading"),
    Page_lifecycleEvent("Page.lifecycleEvent"), targetcreated("targetcreated"), targetdestroyed("targetdestroyed"),
    targetchanged("targetchanged"), disconnected("disconnected"),

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
