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

public class WaitForOptions {

    private boolean ignoreSameDocumentNavigation;

    /**
     * 什么时候要考虑等待成功。给定一组事件字符串，在所有事件被触发后，等待被认为是成功的。
     */
    private List<PuppeteerLifeCycle> waitUntil;
    /**
     * 最长等待时间（以毫秒为单位）。传递 0 以禁用超时。 可以使用 Page.setDefaultTimeout() 或 Page.setDefaultNavigationTimeout() 方法更改默认值。
     */
    private Integer timeout;

    public boolean getIgnoreSameDocumentNavigation() {
        return ignoreSameDocumentNavigation;
    }

    public void setIgnoreSameDocumentNavigation(boolean ignoreSameDocumentNavigation) {
        this.ignoreSameDocumentNavigation = ignoreSameDocumentNavigation;
    }

    public List<PuppeteerLifeCycle> getWaitUntil() {
        return waitUntil;
    }

    public void setWaitUntil(List<PuppeteerLifeCycle> waitUntil) {
        this.waitUntil = waitUntil;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return "WaitForOptions{" + "ignoreSameDocumentNavigation=" + ignoreSameDocumentNavigation + ", waitUntil="
                + waitUntil + ", timeout=" + timeout + '}';
    }

}
