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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.socket.CDPSession;
import org.miaixz.lancia.worker.enums.CDPSessionEvent;
import org.miaixz.lancia.worker.events.TracingCompleteEvent;

/**
 * You can use [`tracing.start`](#tracingstartoptions) and [`tracing.stop`](#tracingstop) to create a trace file which
 * can be opened in Chrome DevTools or [timeline viewer](https://chromedevtools.github.io/timeline-viewer/)
 * @author Kimi Liu
 * @since Java 17+
 */
public class Tracing {

    /**
     * 当前要trace的 chrome devtools protocol session
     */
    private CDPSession client;

    /**
     * 判断是否已经在追踪中
     */
    private boolean recording;

    /**
     * 追踪到的信息要保存的文件路径
     */
    private String path;

    public Tracing(CDPSession client) {
        this.client = client;
        this.recording = false;
        this.path = "";
    }

    public void start(String path) {
        this.start(path, false, null);
    }

    /**
     * 每个浏览器一次只能激活一条跟踪
     * 
     * @param path        A path to write the trace file to. 跟踪文件写入的路径
     * @param screenshots captures screenshots in the trace 捕获跟踪中的屏幕截图
     * @param categories  specify custom categories to use instead of default. 指定要使用的自定义类别替换默认值
     */
    public void start(String path, boolean screenshots, Set<String> categories) {
        Assert.isTrue(!this.recording, "Cannot start recording trace while already recording trace.");
        if (categories == null)
            categories = new HashSet<>(Builder.DEFAULTCATEGORIES);
        if (screenshots)
            categories.add("disabled-by-default-devtools.screenshot");
        this.path = path;
        this.recording = true;
        Map<String, Object> params = new HashMap<>();
        params.put("transferMode", "ReturnAsStream");
        params.put("categories", String.join(",", categories));
        this.client.send("Tracing.start", params);
    }

    /**
     * stop tracing
     */
    public void stop() {
        this.client.once(CDPSessionEvent.Tracing_tracingComplete, (Consumer<TracingCompleteEvent>) event -> {
            try {
                Builder.readProtocolStream(Tracing.this.getClient(), event.getStream(), Tracing.this.getPath(), true);
            } catch (IOException e) {
                Logger.error("Error reading trace", e);
            }
        });
        this.client.send("Tracing.end");
        this.recording = false;
    }

    public CDPSession getClient() {
        return client;
    }

    public void setClient(CDPSession client) {
        this.client = client;
    }

    public boolean isRecording() {
        return recording;
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
