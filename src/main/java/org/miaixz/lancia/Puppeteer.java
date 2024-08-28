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
package org.miaixz.lancia;

import static org.miaixz.lancia.Builder.PRODUCT_ENV;

import java.io.IOException;
import java.util.List;

import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.kernel.Variables;
import org.miaixz.lancia.kernel.browser.Fetcher;
import org.miaixz.lancia.launch.ChromeLauncher;
import org.miaixz.lancia.options.ConnectOptions;
import org.miaixz.lancia.options.FetcherOptions;
import org.miaixz.lancia.options.LaunchOptions;
import org.miaixz.lancia.options.LaunchOptionsBuilder;
import org.miaixz.lancia.socket.ConnectionTransport;

/**
 * Puppeteer 也可以用来控制 Chrome 浏览器， 但它与绑定的 Chromium 版本在一起使用效果最好。不能保证它可以与任何其他版本一起使用。谨慎地使用 executablePath 选项。 如果 Google
 * Chrome（而不是Chromium）是首选，一个 Chrome Canary 或 Dev Channel 版本是建议的
 */
public class Puppeteer {

    private String productName = null;

    private Launcher launcher;

    private Variables env = null;

    private String projectRoot = System.getProperty("user.dir");

    private String preferredRevision = Builder.VERSION;

    private boolean isPuppeteerCore;

    public Puppeteer() {

    }

    public Puppeteer(String projectRoot, String preferredRevision, boolean isPuppeteerCore, String productName) {
        this.projectRoot = projectRoot;
        this.preferredRevision = StringKit.isEmpty(preferredRevision) ? Builder.VERSION : preferredRevision;
        this.isPuppeteerCore = isPuppeteerCore;
        this.productName = productName;
    }

    /**
     * 以默认参数启动浏览器 launch Browser by default options
     *
     * @return 浏览器
     */
    public static Browser launch() {
        return rawLaunch();
    }

    public static Browser launch(boolean headless) {
        return rawLaunch(headless);
    }

    public static Browser launch(LaunchOptions options) {
        return rawLaunch(options, new Puppeteer());
    }

    private static Browser rawLaunch() {
        return rawLaunch(true);
    }

    private static Browser rawLaunch(boolean headless) {
        return rawLaunch(new LaunchOptionsBuilder().withHeadless(headless).build(), new Puppeteer());
    }

    /**
     * 连接一个已经存在的浏览器实例 browserWSEndpoint、browserURL、transport有其中一个就行了 browserWSEndpoint:类似 UUID
     * 的字符串，可通过{@link Browser#wsEndpoint()}获取 browserURL: 类似 localhost:8080 这个地址 transport: 之前已经创建好的 ConnectionTransport
     * 
     * @param options 连接的浏览器选项
     * @return 浏览器实例
     */
    private static Browser connect(ConnectOptions options) {
        Puppeteer puppeteer = new Puppeteer();
        adapterLauncher(puppeteer);
        return puppeteer.getLauncher().connect(options);
    }

    /**
     * 连接一个已经存在的 Browser 实例 browserWSEndpoint:类似 UUID 的字符串，可通过{@link Browser#wsEndpoint()}获取 browserURL: 类似
     * localhost:8080 这个地址
     * 
     * @param browserWSEndpointOrURL 一个Browser实例对应一个browserWSEndpoint
     * @return 浏览器实例
     */
    public static Browser connect(String browserWSEndpointOrURL) {
        ConnectOptions options = new ConnectOptions();
        if (browserWSEndpointOrURL.contains(":")) {
            options.setBrowserURL(browserWSEndpointOrURL);
            return connect(options);
        } else {
            options.setBrowserWSEndpoint(browserWSEndpointOrURL);
            return connect(options);
        }
    }

    /**
     * 连接一个已经存在的 Browser 实例 transport: 之前已经创建好的 ConnectionTransport
     * 
     * @param transport websocket http transport 三选一
     * @return 浏览器实例
     */
    public static Browser connect(ConnectionTransport transport) {
        ConnectOptions options = new ConnectOptions();
        options.setTransport(transport);
        return connect(options);
    }

    /**
     * The method launches a browser instance with given arguments. The browser will be closed when the parent java
     * process is closed.
     */
    private static Browser rawLaunch(LaunchOptions options, Puppeteer puppeteer) {
        if (StringKit.isNotBlank(options.getProduct())) {
            puppeteer.setProductName(options.getProduct());
        }
        adapterLauncher(puppeteer);
        return puppeteer.getLauncher().launch(options);
    }

    /**
     * 适配chrome or firefox 浏览器
     */
    private static void adapterLauncher(Puppeteer puppeteer) {
        String productName;
        Launcher launcher;
        Variables env;
        if (StringKit.isEmpty(productName = puppeteer.getProductName()) && !puppeteer.getIsPuppeteerCore()) {

            if ((env = puppeteer.getEnv()) == null) {
                puppeteer.setEnv(env = System::getenv);
            }
            for (int i = 0; i < PRODUCT_ENV.length; i++) {
                String envProductName = PRODUCT_ENV[i];
                productName = env.getEnv(envProductName);
                if (StringKit.isNotEmpty(productName)) {
                    puppeteer.setProductName(productName);
                    break;
                }
            }
        }
        if (StringKit.isEmpty(productName)) {
            productName = "chrome";
            puppeteer.setProductName(productName);
        }
        switch (productName) {
        case "firefox":
        case "chrome":
        default:
            launcher = new ChromeLauncher(System.getProperty("user.dir"), puppeteer.getPreferredRevision());
        }
        puppeteer.setLauncher(launcher);
    }

    /**
     * 指定启动版本，开启浏览器
     * 
     * @param options 启动参数
     * @param version 浏览器版本
     * @return 浏览器实例
     */
    public static Browser launch(LaunchOptions options, String version) {
        Puppeteer puppeteer = new Puppeteer();
        if (StringKit.isNotEmpty(version)) {
            puppeteer.setPreferredRevision(version);
        }
        return Puppeteer.rawLaunch(options, puppeteer);
    }

    /**
     * 返回默认的运行的参数
     * 
     * @param options 可自己添加的选项
     * @return 默认参数集合
     */
    public List<String> defaultArgs(LaunchOptions options) {
        return this.getLauncher().defaultArgs(options);
    }

    public String executablePath() throws IOException {
        return this.getLauncher().executablePath();
    }

    public Fetcher createBrowserFetcher() {
        return new Fetcher(this.projectRoot, new FetcherOptions());
    }

    public Fetcher createBrowserFetcher(FetcherOptions options) {
        return new Fetcher(this.projectRoot, options);
    }

    private String getProductName() {
        return productName;
    }

    private void setProductName(String productName) {
        this.productName = productName;
    }

    private boolean getIsPuppeteerCore() {
        return isPuppeteerCore;
    }

    private Launcher getLauncher() {
        return launcher;
    }

    private void setLauncher(Launcher launcher) {
        this.launcher = launcher;
    }

    private Variables getEnv() {
        return env;
    }

    private void setEnv(Variables env) {
        this.env = env;
    }

    public String getProjectRoot() {
        return projectRoot;
    }

    public void setProjectRoot(String projectRoot) {
        this.projectRoot = projectRoot;
    }

    public String getPreferredRevision() {
        return preferredRevision;
    }

    public void setPreferredRevision(String preferredRevision) {
        this.preferredRevision = preferredRevision;
    }

}
