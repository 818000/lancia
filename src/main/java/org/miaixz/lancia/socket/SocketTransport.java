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

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.xyz.UrlKit;
import org.miaixz.bus.logger.Logger;

/**
 * websocket client
 * @author Kimi Liu
 * @since Java 17+
 */
public class SocketTransport extends WebSocketClient implements Transport {

    private Connection connection = null;

    public SocketTransport(String serverURI) {
        super(UrlKit.toURI(serverURI));
    }

    @Override
    public void onMessage(String message) {
        Assert.notNull(this.connection, "MessageConsumer must be initialized");
        this.connection.accept(message);
    }

    /**
     *
     * @param code   NORMAL = 1000; GOING_AWAY = 1001; PROTOCOL_ERROR = 1002; REFUSE = 1003; NOCODE = 1005;
     *               ABNORMAL_CLOSE = 1006; NO_UTF8 = 1007; POLICY_VALIDATION = 1008; TOOBIG = 1009; EXTENSION = 1010;
     *               UNEXPECTED_CONDITION = 1011; SERVICE_RESTART = 1012; TRY_AGAIN_LATER = 1013; BAD_GATEWAY = 1014;
     *               TLS_ERROR = 1015; NEVER_CONNECTED = -1; BUGGYCLOSE = -2; FLASHPOLICY = -3;
     * @param reason 原因
     * @param remote 远程
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        // 这里是WebSocketClient的实现方法,当websocket
        // closed的时候会调用onClose
        Logger.info("Connection closed by {} Code: {} Reason: {}", remote ? "remote peer" : "us", code, reason);
        if (this.connection != null) {// 浏览器以外关闭时候，connection不为空
            this.connection.dispose();
        }
    }

    @Override
    public void onError(Exception e) {
        Logger.error("Websocket error:", e);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        Logger.info("Websocket serverHandshake status: {}", serverHandshake.getHttpStatus());
    }

    @Override
    public void setConnection(Connection connection) {
        this.connection = connection;
    }

}
