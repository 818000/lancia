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

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.exception.ProtocolException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;

import com.fasterxml.jackson.databind.JsonNode;

public class CallbackRegistry {

    private final Map<Integer, Callback> callbacks = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    public JsonNode create(String label, Integer timeout, Consumer<Integer> request, boolean isBlocking) {
        Callback callback = new Callback(idGenerator.incrementAndGet(), label);
        this.callbacks.put(callback.id(), callback);
        // 完成时移除回调
        callback.getSubject().doAfterTerminate(() -> this.callbacks.remove(callback.id())).subscribe();
        try {
            request.accept(callback.id());
            // 不阻塞时，不关心结果
            if (!isBlocking)
                return null;
        } catch (Exception e) {
            try {
                if (timeout > 0) {
                    return callback.getSubject().timeout(timeout, TimeUnit.MILLISECONDS).blockingGet();
                } else if (timeout == 0) {
                    return callback.getSubject().blockingGet();
                } else {
                    throw new InternalException("Timeout < 0,It shouldn't happen");
                }
            } catch (Exception ex) {
                Logger.error("Callback waiting Error:", e);
            }
            callback.reject(e);
            Logger.error("There was an error sending the request:", e);
        }
        if (timeout > 0) {
            return callback.getSubject().timeout(timeout, TimeUnit.MILLISECONDS).blockingGet();
        } else if (timeout == 0) {
            return callback.getSubject().blockingGet();
        } else {
            throw new InternalException("Timeout < 0");
        }
    }

    public void reject(int id, String message, String originalMessage) {
        Callback callback = this.callbacks.get(id);
        if (callback != null) {
            this._reject(callback, message, originalMessage);
        }
    }

    private void _reject(Callback callback, String errorMessage, String originalMessage) {
        ProtocolException protocolException = new ProtocolException(
                "Protocol error (" + callback.label() + "): " + errorMessage);
        protocolException.setErrcode(callback.error().getErrcode());
        if (StringKit.isNotEmpty(originalMessage)) {
            protocolException.setErrmsg(originalMessage);
        }
        callback.setError(protocolException);
        callback.reject(protocolException);
    }

    private void _reject(Callback callback, ProtocolException error, String originalMessage) {// todo 用法
        String message = error.getMessage();
        ProtocolException protocolException = new ProtocolException(
                "Protocol error (" + callback.label() + "): " + message, callback.error());
        protocolException.setErrcode(error.getErrcode());
        if (StringKit.isNotEmpty(originalMessage)) {
            protocolException.setErrmsg(originalMessage);
        }
        callback.setError(protocolException);
        callback.reject(protocolException);
    }

    public void resolve(int id, JsonNode value) {
        Callback callback = this.callbacks.get(id);
        if (callback != null) {
            callback.resolve(value);
        }
    }

    // 这里会释放线程等待，避免死锁
    public void clear() {
        this.callbacks.forEach((key, callback) -> {
            this._reject(callback, "Target closed", "");
        });
    }

    public List<ProtocolException> getPendingProtocolErrors() {
        List<ProtocolException> results = new ArrayList<>();
        this.callbacks.forEach((key, callback) -> {
            ProtocolException error = callback.error();
            if (error != null) {
                results.add(new ProtocolException(
                        callback.error() + " timed out. Trace: " + Arrays.toString(callback.error().getStackTrace())));
            }
        });
        return results;
    }

}
