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

import java.util.Optional;
import java.util.Timer;

import org.miaixz.bus.core.lang.exception.ProtocolException;

import com.fasterxml.jackson.databind.JsonNode;

import io.reactivex.rxjava3.subjects.SingleSubject;
/**
 * @author Kimi Liu
 * @since Java 17+
 */
public class Callback {

    public String label;
    SingleSubject<JsonNode> subject = SingleSubject.create();
    private int id;
    private Timer timer;
    private ProtocolException error = new ProtocolException();

    public Callback(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public void resolve(JsonNode value) {
        Optional.ofNullable(this.timer).ifPresent(Timer::cancel);
        this.subject.onSuccess(value);
    }

    public void reject(Exception error) {
        Optional.ofNullable(this.timer).ifPresent(Timer::cancel);
        this.subject.onError(error);
    }

    public int id() {
        return this.id;
    }

    public ProtocolException error() {
        return this.error;
    }

    public String label() {
        return this.label;
    }

    public SingleSubject<JsonNode> getSubject() {
        return this.subject;
    }

    public void setError(ProtocolException error) {
        this.error = error;
    }

}
