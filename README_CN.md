# Lancia

[English](README.md) | [中文](README_CN.md)

Lancia 是 Java 原生的浏览器自动化库，提供接近 Puppeteer 的公开 API。它可以通过 CDP 启动和连接 Chrome/Chromium，也支持 Firefox 的 WebDriver BiDi 路径，并覆盖浏览器与页面自动化、截图、PDF、网络、输入、模拟、Tracing、Coverage、Accessibility、Extension、浏览器下载与缓存等能力。

本项目以 Puppeteer 的公开行为和协议流程作为兼容目标，并按 Java 习惯实现 API。源码为 Java 独立实现，不复制 Puppeteer TypeScript 源码。

## 环境要求

- Java 21 或更高版本。
- Maven 3.9 或更高版本。
- 本机存在 Chrome/Chromium 或 Firefox 可执行文件，或允许 Lancia 下载托管浏览器。
- Linux 服务器和 CI 镜像可能需要预先安装浏览器运行所需的系统依赖。

## 安装

```xml
<dependency>
    <groupId>org.miaixz</groupId>
    <artifactId>lancia</artifactId>
    <version>8.5.1</version>
</dependency>
```

## 快速开始

### 下载托管浏览器

`Puppeteer.downloadBrowsers()` 会读取 Puppeteer 风格的环境变量，并把配置的浏览器版本安装到本地缓存。Chrome 下载默认使用 Chrome for Testing，当 Google 下载地址不可用时，可以自动回退到 npmmirror。

```java
import org.miaixz.lancia.Puppeteer;

public class InstallBrowsers {

    public static void main(String[] args) {
        Puppeteer.downloadBrowsers().forEach(System.out::println);
    }
}
```

### 启动浏览器

```java
import org.miaixz.lancia.Browser;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.Puppeteer;
import org.miaixz.lancia.options.LaunchOptions;

public class LaunchExample {

    public static void main(String[] args) {
        LaunchOptions options = new LaunchOptions();
        options.setPipe(true);
        options.setHeadless(true);
        options.addArg("--no-sandbox");

        // 不使用托管缓存时，可以指定本机浏览器路径。
        // options.setExecutablePath("/path/to/chrome");

        try (Browser browser = Puppeteer.launch(options)) {
            Page page = browser.newPage();
            page.goTo("https://example.com");
            System.out.println(page.url());
        }
    }
}
```

### 连接已有浏览器

```java
import org.miaixz.lancia.Browser;
import org.miaixz.lancia.Puppeteer;
import org.miaixz.lancia.options.ConnectOptions;

public class ConnectExample {

    public static void main(String[] args) {
        ConnectOptions options = new ConnectOptions();
        options.setBrowserWSEndpoint("ws://127.0.0.1:9222/devtools/browser/<id>");

        try (Browser browser = Puppeteer.connect(options)) {
            System.out.println(browser.version());
        }
    }
}
```

### 截图与 PDF

```java
import java.nio.file.Path;

import org.miaixz.lancia.Browser;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.Puppeteer;
import org.miaixz.lancia.options.LaunchOptions;
import org.miaixz.lancia.options.PDFOptions;
import org.miaixz.lancia.options.ScreenshotOptions;

public class PageOutputExample {

    public static void main(String[] args) {
        LaunchOptions launch = new LaunchOptions();
        launch.setPipe(true);

        try (Browser browser = Puppeteer.launch(launch)) {
            Page page = browser.newPage();
            page.setContent("<!doctype html><title>Lancia</title><h1>Hello Lancia</h1>");

            ScreenshotOptions screenshot = new ScreenshotOptions();
            screenshot.setFullPage(true);
            screenshot.setPath(Path.of("page.png"));
            page.screenshot(screenshot);

            PDFOptions pdf = new PDFOptions();
            pdf.setFormat("A4");
            pdf.setPrintBackground(true);
            pdf.setPath(Path.of("page.pdf"));
            page.pdf(pdf);

            System.out.println(page.evaluate("document.title"));
        }
    }
}
```

## 主要 API

| 范围 | API |
|---|---|
| 入口 | `Puppeteer.launch`、`Puppeteer.connect`、`Puppeteer.downloadBrowsers` |
| 启动与连接 | `LaunchOptions`、`ConnectOptions`、`BrowserVariant` |
| 浏览器模型 | `Browser`、`Context`、`Target`、`Page`、`Frame` |
| 页面输出 | `ScreenshotOptions`、`PDFOptions`、`Page.screenshot`、`Page.pdf` |
| 页面交互 | `Keyboard`、`Mouse`、`Touchscreen`、`Element`、`Locator` |
| 网络与权限 | `Request`、`Response`、Cookie、Download、Permission |
| 高级能力 | `Coverage`、`Tracing`、`Accessibility`、Dialog、Console Message |
| 运行时工具 | `Puppeteer.executablePath`、`Puppeteer.browserVersion`、`Puppeteer.trimCache` |
| 协议 | 默认使用 CDP；配置 BiDi 或启动 Firefox 时使用 WebDriver BiDi |

## 配置

Lancia 启动时读取 Puppeteer 风格的环境变量：

| 环境变量 | 说明 |
|---|---|
| `PUPPETEER_CACHE_DIR` | 浏览器缓存目录，默认是 `~/.cache/puppeteer`。 |
| `PUPPETEER_EXECUTABLE_PATH` | 本机浏览器可执行文件路径，设置后会跳过托管浏览器下载。 |
| `PUPPETEER_BROWSER` | 默认浏览器，例如 `chrome` 或 `firefox`。 |
| `PUPPETEER_TMP_DIR` | 启动和运行时临时目录。 |
| `PUPPETEER_SKIP_DOWNLOAD` | 为真时跳过全部托管浏览器下载。 |
| `PUPPETEER_LOGLEVEL` | 运行时日志级别：`silent`、`error`、`warn`。 |
| `PUPPETEER_CHROME_VERSION` | Chrome 托管下载使用的版本或通道。 |
| `PUPPETEER_CHROME_DOWNLOAD_BASE_URL` | 自定义 Chrome 下载基础地址。 |
| `PUPPETEER_CHROME_SKIP_DOWNLOAD` | 仅跳过 Chrome 下载。 |
| `PUPPETEER_CHROME_HEADLESS_SHELL_SKIP_DOWNLOAD` | 仅跳过 Chrome Headless Shell 下载。 |
| `PUPPETEER_FIREFOX_SKIP_DOWNLOAD` | 启用或跳过 Firefox 托管下载；Firefox 默认跳过。 |

浏览器级环境变量还支持 `_VERSION`、`_DOWNLOAD_BASE_URL`、`_EXPECTED_ARCHIVE_SHA256`、`_ALLOW_UNVERIFIED_DOWNLOAD` 后缀。

## 安全建议

- 本地 Chrome/CDP 启动建议使用 `LaunchOptions#setPipe(true)`，避免暴露 DevTools TCP 端口。
- 不要把浏览器调试端点直接暴露到公网。
- 把 `Page.evaluate` 的入参视为高权限 JavaScript。
- 处理不可信 URL、请求头、下载文件或协议载荷时，建议配合 `SecurityPolicy` 与 `ResourceLimits`。

## 构建

```bash
mvn verify
mvn -DskipTests package
```

## 许可证

Lancia 使用 Apache License 2.0 发布，详情见 [LICENSE](LICENSE)。
