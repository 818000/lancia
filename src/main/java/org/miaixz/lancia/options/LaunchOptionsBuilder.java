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
package org.miaixz.lancia.options;

import java.util.List;

import org.miaixz.lancia.Builder;
import org.miaixz.lancia.kernel.Variables;

public class LaunchOptionsBuilder {

    private LaunchOptions options;

    public LaunchOptionsBuilder() {
        options = new LaunchOptions();
    }

    public LaunchOptionsBuilder withExecutablePath(String executablePath) {
        options.setExecutablePath(executablePath);
        return this;
    }

    /**
     * 是否忽略所欲的默认启动参数，默认是fasle
     * 
     * @param ignoreAllDefaultArgs true为忽略所有启动参数
     * @return LaunchOptionsBuilder
     */
    public LaunchOptionsBuilder withIgnoreDefaultArgs(boolean ignoreAllDefaultArgs) {
        options.setIgnoreAllDefaultArgs(ignoreAllDefaultArgs);
        return this;
    }

    /**
     * 忽略指定的默认启动参数，默认的启动参数见 {@link Builder#DEFAULT_ARGS}
     * 
     * @param ignoreDefaultArgs 要忽略的启动参数
     * @return LaunchOptionsBuilder
     */
    public LaunchOptionsBuilder withIgnoreDefaultArgs(List<String> ignoreDefaultArgs) {
        options.setIgnoreDefaultArgs(ignoreDefaultArgs);
        return this;
    }

    public LaunchOptionsBuilder withHandleSIGINT(boolean handleSIGINT) {
        options.setHandleSIGINT(handleSIGINT);
        return this;
    }

    public LaunchOptionsBuilder withHandleSIGTERM(boolean handleSIGTERM) {
        options.setHandleSIGTERM(handleSIGTERM);
        return this;
    }

    public LaunchOptionsBuilder withHandleSIGHUP(boolean handleSIGHUP) {
        options.setHandleSIGHUP(handleSIGHUP);
        return this;
    }

    public LaunchOptionsBuilder withDumpio(boolean dumpio) {
        options.setDumpio(dumpio);
        return this;
    }

    public LaunchOptionsBuilder withEnv(Variables env) {
        options.setEnv(env);
        return this;
    }

    public LaunchOptionsBuilder withPipe(boolean pipe) {
        options.setPipe(pipe);
        return this;
    }

    public LaunchOptionsBuilder withProduct(String product) {
        options.setProduct(product);
        return this;
    }

    public LaunchOptionsBuilder setAcceptInsecureCerts(boolean acceptInsecureCerts) {
        options.setAcceptInsecureCerts(acceptInsecureCerts);
        return this;
    }

    public LaunchOptionsBuilder withViewport(Viewport viewport) {
        options.setDefaultViewport(viewport);
        return this;
    }

    public LaunchOptionsBuilder withSlowMo(int slowMo) {
        options.setSlowMo(slowMo);
        return this;
    }

    public LaunchOptionsBuilder withHeadless(boolean headless) {
        options.setHeadless(headless);
        return this;
    }

    public LaunchOptionsBuilder withArgs(List<String> args) {
        options.setArgs(args);
        return this;
    }

    public LaunchOptionsBuilder withUserDataDir(String userDataDir) {
        options.setUserDataDir(userDataDir);
        return this;
    }

    public LaunchOptionsBuilder withDevtools(boolean devtools) {
        options.setDevtools(devtools);
        return this;
    }

    public LaunchOptions build() {
        return options;
    }
}
