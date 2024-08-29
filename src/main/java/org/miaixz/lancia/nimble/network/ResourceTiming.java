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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Timing information for the request.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceTiming {

    /**
     * Timing's requestTime is a baseline in seconds, while the other numbers are ticks in milliseconds relatively to
     * this requestTime.
     */
    private int requestTime;
    /**
     * Started resolving proxy.
     */
    private int proxyStart;
    /**
     * Finished resolving proxy.
     */
    private int proxyEnd;
    /**
     * Started DNS address resolve.
     */
    private int dnsStart;
    /**
     * Finished DNS address resolve.
     */
    private int dnsEnd;
    /**
     * Started connecting to the remote host.
     */
    private int connectStart;
    /**
     * Connected to the remote host.
     */
    private int connectEnd;
    /**
     * Started SSL handshake.
     */
    private int sslStart;
    /**
     * Finished SSL handshake.
     */
    private int sslEnd;
    /**
     * Started running ServiceWorker.
     */
    private int workerStart;
    /**
     * Finished Starting ServiceWorker.
     */
    private int workerReady;
    /**
     * Started sending request.
     */
    private int sendStart;
    /**
     * Finished sending request.
     */
    private int sendEnd;
    /**
     * Time the server started pushing request.
     */
    private int pushStart;
    /**
     * Time the server finished pushing request.
     */
    private int pushEnd;
    /**
     * Finished receiving response headers.
     */
    private int receiveHeadersEnd;

}
