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
package org.miaixz.lancia.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
/**
 * @author Kimi Liu
 * @since Java 17+
 */
public class AsyncDisposableStack implements AutoCloseable {

    private boolean disposed = false;

    private List<DisposableStackConsumer> stack = new ArrayList<>();

    /**
     * Returns a value indicating whether this stack has been disposed.
     */
    public boolean getDisposed() {
        return disposed;
    }

    public void setDisposed(boolean disposed) {
        this.disposed = disposed;
    }

    /**
     * Disposes each resource in the stack in the reverse order that they were added.
     */
    void dispose() {
        if (this.disposed) {
            return;
        }
        this.disposed = true;
        ListIterator<DisposableStackConsumer> consumerListIterator = this.stack.listIterator(this.stack.size());
        while (consumerListIterator.hasPrevious()) {
            DisposableStackConsumer consumer = consumerListIterator.previous();
            consumer.execute();
        }
    }

    /**
     * Adds a callback to be invoked when the stack is disposed.
     */
    public void defer(DisposableStackConsumer consumer) {
        this.stack.add(consumer);
    }

    @Override
    public void close() throws Exception {
        CompletableFuture.runAsync(this::dispose);
    }

}
