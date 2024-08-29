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
import org.miaixz.lancia.kernel.page.*;

public enum PageEvent {
    /**
     * Emitted when the page closes.
     */
    CLOSE("close"),
    /**
     * Emitted when JavaScript within the page calls one of console API methods, e.g. `console.log` or `console.dir`.
     * Also emitted if the page throws an error or a warning.
     *
     * @remarks A `console` event provides a {@link ConsoleMessage} representing the console message that was logged.
     * @example An example of handling `console` event:
     *          <p>
     *          ```ts page.on('console', msg => { for (let i = 0; i < msg.args().length; ++i) console.log(`${i}:
     *          ${msg.args()[i]}`); }); page.evaluate(() => console.log('hello', 5, {foo: 'bar'})); ```
     */
    CONSOLE("console"),
    /**
     * Emitted when a JavaScript dialog appears, such as `alert`, `prompt`, `confirm` or `beforeunload`. Puppeteer can
     * respond to the dialog via {@link Dialog#accept} or {@link Dialog#dismiss}.
     */
    DIALOG("dialog"),
    /**
     * Emitted when the JavaScript
     * <a href="https://developer.mozilla.org/en-US/docs/Web/Events/DOMContentLoaded">DOMContentLoaded</a> event is
     * dispatched.
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
     * Emitted when the JavaScript <a href="https://developer.mozilla.org/en-US/docs/Web/Events/load">load</a> event is
     * dispatched.
     */
    LOAD("load"),
    /**
     * Emitted when the JavaScript code makes a call to `console.timeStamp`. For the list of metrics see
     * {@link Page#metrics | page.metrics}.
     *
     * @remarks Contains an object with two properties:
     *          <p>
     *          - `title`: the title passed to `console.timeStamp` - `metrics`: object containing metrics as key/value
     *          pairs. The values will be `number`s.
     */
    METRICS("metrics"),
    /**
     * Emitted when an uncaught exception happens within the page. Contains an `Error`.
     */
    PAGEERROR("pageerror"),
    /**
     * Emitted when the page opens a new tab or window.
     * <p>
     * Contains a {@link Page} corresponding to the popup window.
     *
     * @example ```ts const [popup] = await Promise.all([ new Promise(resolve => page.once('popup', resolve)),
     *          page.click('a[target=_blank]'), ]); ```
     *          <p>
     *          ```ts const [popup] = await Promise.all([ new Promise(resolve => page.once('popup', resolve)),
     *          page.evaluate(() => window.open('https://example.com')), ]); ```
     */
    POPUP("popup"),
    /**
     * Emitted when a page issues a request and contains a {@link Request}.
     *
     * @remarks The object is readonly. See {@link Page#setRequestInterception} for intercepting and mutating requests.
     */
    REQUEST("request"),
    /**
     * Emitted when a request ended up loading from cache. Contains a {@link Request}.
     *
     * @remarks For certain requests, might contain undefined. {@link <a href="https://crbug.com/750469">crbug</a>}
     */
    REQUESTSERVEDFROMCACHE("requestservedfromcache"),
    /**
     * Emitted when a request fails, for example by timing out.
     * <p>
     * Contains a {@link Request}.
     *
     * @remarks HTTP Error responses, such as 404 or 503, are still successful responses from HTTP standpoint, so
     *          request will complete with `requestfinished` event and not with `requestfailed`.
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
     * Emitted when a dedicated
     * {@link <a href="https://developer.mozilla.org/en-US/docs/Web/API/Web_Workers_API">WebWorker</a>} is spawned by
     * the page.
     */
    WORKERCREATED("workercreated"),
    /**
     * Emitted when a dedicated
     * {@link <a href="https://developer.mozilla.org/en-US/docs/Web/API/Web_Workers_API">WebWorker</a>} is destroyed by
     * the page.
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
