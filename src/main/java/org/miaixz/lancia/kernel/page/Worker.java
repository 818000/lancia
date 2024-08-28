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
package org.miaixz.lancia.kernel.page;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.events.ExceptionThrownEvent;
import org.miaixz.lancia.nimble.runtime.ConsoleAPICalledEvent;
import org.miaixz.lancia.nimble.runtime.ExecutionContextCreatedEvent;
import org.miaixz.lancia.nimble.runtime.ExecutionContextDescription;
import org.miaixz.lancia.nimble.runtime.RemoteObject;
import org.miaixz.lancia.options.TargetType;
import org.miaixz.lancia.socket.CDPSession;

/**
 * The events `workercreated` and `workerdestroyed` are emitted on the page object to signal the worker lifecycle.
 */
public class Worker {

    private final CDPSession client;
    private final String url;
    private final Consumer<ExceptionThrownEvent> exceptionThrown;
    private final ConsoleAPI consoleAPICalled;
    private ExecutionContext context;
    private CountDownLatch contextLatch;

    public Worker(CDPSession client, String url, String targetId, TargetType targetType, ConsoleAPI consoleAPICalled,
            Consumer<ExceptionThrownEvent> exceptionThrown) {
        super();
        this.client = client;
        this.url = url;
        this.exceptionThrown = exceptionThrown;
        this.consoleAPICalled = consoleAPICalled;
        this.client.once(CDPSession.CDPSessionEvent.Runtime_executionContextCreated,
                (Consumer<ExecutionContextCreatedEvent>) this::onExecutionContextCreated);
        this.client.send("Runtime.enable");
        this.client.on(CDPSession.CDPSessionEvent.Runtime_consoleAPICalled,
                (Consumer<ConsoleAPICalledEvent>) this::onConsoleAPICalled);
        this.client.on(CDPSession.CDPSessionEvent.Runtime_exceptionThrown,
                (Consumer<ExceptionThrownEvent>) this::onExceptionThrown);
    }

    private void onExceptionThrown(ExceptionThrownEvent event) {
        exceptionThrown.accept(event);
    }

    private void onConsoleAPICalled(ConsoleAPICalledEvent event) {
        this.consoleAPICalled.call(event.getType(),
                event.getArgs().stream().map(this::jsHandleFactory).collect(Collectors.toList()),
                event.getStackTrace());
    }

    private void onExecutionContextCreated(ExecutionContextCreatedEvent event) {
        ExecutionContextDescription contextDescription = event.getContext();
        ExecutionContext executionContext = new ExecutionContext(client, contextDescription, null);
        this.executionContextCallback(executionContext);
    }

    public JSHandle jsHandleFactory(RemoteObject remoteObject) {
        return new JSHandle(this.context, client, remoteObject);
    }

    protected void executionContextCallback(ExecutionContext executionContext) {
        this.setContext(executionContext);
    }

    private ExecutionContext executionContextPromise() throws InterruptedException {
        if (context == null) {
            this.setContextLatch(new CountDownLatch(1));
            boolean await = this.getContextLatch().await(Builder.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
            if (!await) {
                throw new TimeoutException("Wait for ExecutionContext timeout");
            }
        }
        return context;
    }

    private CountDownLatch getContextLatch() {
        return contextLatch;
    }

    private void setContextLatch(CountDownLatch contextLatch) {
        this.contextLatch = contextLatch;
    }

    public void setContext(ExecutionContext context) {
        this.context = context;
    }

    public String url() {
        return this.url;
    }

    public ExecutionContext executionContext() throws InterruptedException {
        return this.executionContextPromise();
    }

    public Object evaluate(String pageFunction, List<Object> args) throws InterruptedException {
        return this.executionContextPromise().evaluate(pageFunction, args);
    }

    public Object evaluateHandle(String pageFunction, List<Object> args) throws InterruptedException {
        return this.executionContextPromise().evaluateHandle(pageFunction, args);
    }

}
