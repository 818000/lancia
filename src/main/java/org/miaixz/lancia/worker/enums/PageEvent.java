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

import org.miaixz.lancia.Page;
import org.miaixz.lancia.kernel.page.Dialog;
import org.miaixz.lancia.kernel.page.Frame;
import org.miaixz.lancia.kernel.page.Request;
import org.miaixz.lancia.kernel.page.Response;
/**
 * @author Kimi Liu
 * @since Java 17+
 */
public enum PageEvent {

    /**
     * Emitted when the page closes.
     */
    CLOSE("close"),
    /**
     * Emitted when JavaScript within the page calls one of console API methods, e.g. `console.log` or `console.dir`.
     * Also emitted if the page throws an error or a warning.
     */
    CONSOLE("console"),
    /**
     * Emitted when a JavaScript dialog appears, such as `alert`, `prompt`, `confirm` or `beforeunload`. Puppeteer can
     * respond to the dialog via {@link Dialog#accept} or {@link Dialog#dismiss}.
     */
    DIALOG("dialog"),
    /**
     * Emitted when the JavaScript DOMContentLoaded event is dispatched.
     */
    DOMCONTENTLOADED("domcontentloaded"),
    /**
     * Emitted when the page crashes. Will contain an `Error`.
     */
    ERROR("error"),
    /**
     * Emitted when a frame is attached. Will contain a {@link Frame}.
     */
    FRAMEATTACHED("frameattached"),
    /**
     * Emitted when a frame is detached. Will contain a {@link Frame}.
     */
    FRAMEDETACHED("framedetached"),
    /**
     * Emitted when a frame is navigated to a new URL. Will contain a {@link Frame}.
     */
    FRAMENAVIGATED("framenavigated"),
    /**
     * Emitted when the JavaScript load event is dispatched.
     */
    LOAD("load"),
    /**
     * Emitted when the JavaScript code makes a call to `console.timeStamp`. For the list of metrics see
     * {@link Page#metrics | page.metrics}.
     */
    METRICS("metrics"),
    /**
     * Emitted when an uncaught exception happens within the page. Contains an `Error`.
     */
    PAGEERROR("pageerror"),
    /**
     * Emitted when the page opens a new tab or window. Contains a {@link Page} corresponding to the popup window.
     */
    POPUP("popup"),
    /**
     * Emitted when a page issues a request and contains a {@link Request}.
     */
    REQUEST("request"),
    /**
     * Emitted when a request ended up loading from cache. Contains a {@link Request}.
     */
    REQUESTSERVEDFROMCACHE("requestservedfromcache"),
    /**
     * Emitted when a request fails, for example by timing out. Contains a {@link Request}.
     */
    REQUESTFAILED("requestfailed"),
    /**
     * Emitted when a request finishes successfully. Contains a {@link Request}.
     */
    REQUESTFINISHED("requestfinished"),
    /**
     * Emitted when a response is received. Contains a {@link Response}.
     */
    RESPONSE("response"),
    /**
     * Emitted when a dedicated WebWorker is spawned by the page.
     */
    WORKERCREATED("workercreated"),
    /**
     * Emitted when a dedicated WebWorker is destroyed by the page.
     */
    WORKERDESTROYED("workerdestroyed");

    private String eventName;

    PageEvent(String eventName) {
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

}
