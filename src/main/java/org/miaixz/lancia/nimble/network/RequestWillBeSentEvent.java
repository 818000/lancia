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
package org.miaixz.lancia.nimble.network;

import org.miaixz.lancia.kernel.page.Request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Fired when page is about to send HTTP request.
 * @author Kimi Liu
 * @since Java 17+
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RequestWillBeSentEvent {

    /**
     * Request identifier.
     */
    private String requestId;
    /**
     * Loader identifier. Empty string if the request is fetched from worker.
     */
    private String loaderId;
    /**
     * URL of the document this request is loaded for.
     */
    private String documentURL;
    /**
     * Request data.
     */
    private Request request;
    /**
     * Timestamp.
     */
    private long timestamp;
    /**
     * Timestamp.
     */
    private long wallTime;
    /**
     * Request initiator.
     */
    private Initiator initiator;
    /**
     * Redirect response data.
     */
    private ResponsePayload redirectResponse;
    /**
     * Type of this resource.
     * "Document"|"Stylesheet"|"Image"|"Media"|"Font"|"Script"|"TextTrack"|"XHR"|"Fetch"|"EventSource"|"WebSocket"|"Manifest"|"SignedExchange"|"Ping"|"CSPViolationReport"|"Other";
     */
    private String type;
    /**
     * Frame identifier.
     */
    private String frameId;
    /**
     * Whether the request is initiated by a user gesture. Defaults to false.
     */
    private boolean hasUserGesture;

}
