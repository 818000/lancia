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
package org.miaixz.lancia.option;

import java.util.List;

import org.miaixz.lancia.Builder;
import org.miaixz.lancia.kernel.Variables;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * 浏览器启动选项
 *
 * @author Kimi Liu
 * @since Java 17+
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LaunchOptions extends BrowserConnectOptions {

    /**
     * 设置chrome浏览器的路径 Path to a Chromium executable to run instead of bundled Chromium. If executablePath is a relative
     * path, then it is resolved relative to current working directory.
     */
    private String executablePath;
    /**
     * 如果是true，代表忽略所有默认的启动参数，默认的启动参数见{@link Builder#DEFAULT_ARGS}，默认是false
     */
    private boolean ignoreAllDefaultArgs;
    /**
     * 忽略指定的默认启动参数
     */
    private List<String> ignoreDefaultArgs;
    /**
     * Close chrome process on Ctrl-C. 默认是true
     */
    @lombok.Builder.Default
    private boolean handleSIGINT = true;
    /**
     * Close chrome process on SIGTERM. 默认是 true
     */
    @lombok.Builder.Default
    private boolean handleSIGTERM = true;
    /**
     * Close chrome process on SIGHUP. 默认是 true
     */
    @lombok.Builder.Default
    private boolean handleSIGHUP = true;
    /**
     * 将cheome的标准输出流输入流转换到java程序的标准输入输出,java默认已经将子进程的输入和错误流通过管道重定向了，现在这个参数暂时用不上 <br/>
     * Whether to pipe browser process stdout and stderr into process.stdout and process.stderr. 默认是 false
     */
    private boolean dumpio;
    /**
     * ָSystem.getEnv() Specify environment variables that will be visible to Chromium. 默认是 `process.env`.
     */
    private Variables env;
    /**
     * ͨfalse代表使用websocket通讯，true代表使用websocket通讯 Connects to the browser over a pipe instead of a WebSocket. 默认是 false
     */
    private boolean pipe;
    /**
     * chrome or firefox
     */
    private String product;
    /**
     * Whether to wait for the initial page to be ready. Useful when a user explicitly disables that (e.g.
     * `--no-startup-window` for Chrome).
     */
    @lombok.Builder.Default
    private boolean waitForInitialPage = true;

}
