/*
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
 ~                                                                           ~
 ~ Copyright (c) 2015-2026 miaixz.org and other contributors.                ~
 ~                                                                           ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");           ~
 ~ you may not use this file except in compliance with the License.          ~
 ~ You may obtain a copy of the License at                                   ~
 ~                                                                           ~
 ~      https://www.apache.org/licenses/LICENSE-2.0                          ~
 ~                                                                           ~
 ~ Unless required by applicable law or agreed to in writing, software       ~
 ~ distributed under the License is distributed on an "AS IS" BASIS,         ~
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  ~
 ~ See the License for the specific language governing permissions and       ~
 ~ limitations under the License.                                            ~
 ~                                                                           ~
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
*/
package org.miaixz.lancia.options;

import java.util.List;

/**
 * Public browser context options matching Puppeteer's BrowserContextOptions name.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BrowserContextOptions extends ContextOptions {

    /**
     * Proxy server used by this browser context.
     */
    private String proxyServer;

    /**
     * Proxy bypass rules used by this browser context.
     */
    private List<String> proxyBypassList = List.of();

    /**
     * Creates browser context options.
     */
    public BrowserContextOptions() {
        // No initialization required.
    }

    /**
     * Returns the proxy server.
     *
     * @return proxy server
     */
    public String getProxyServer() {
        return proxyServer;
    }

    /**
     * Updates proxy server.
     *
     * @param proxyServer proxy server value
     */
    public void setProxyServer(String proxyServer) {
        this.proxyServer = proxyServer;
    }

    /**
     * Returns the proxy bypass list.
     *
     * @return proxy bypass list
     */
    public List<String> getProxyBypassList() {
        return proxyBypassList;
    }

    /**
     * Updates proxy bypass list.
     *
     * @param proxyBypassList proxy bypass list value
     */
    public void setProxyBypassList(List<String> proxyBypassList) {
        this.proxyBypassList = proxyBypassList == null ? List.of() : List.copyOf(proxyBypassList);
    }

}
