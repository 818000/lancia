# Lancia

Lancia 是 Java 原生的 Puppeteer 兼容库，面向 Chromium CDP、pipe/WebSocket 连接、页面自动化、截图、PDF、网络、输入、模拟、Tracing、Coverage、Accessibility、Extension、BiDi 与 Firefox 启动场景。

本项目以 Puppeteer 的公开行为和协议流程为兼容目标，源码使用 Java 独立实现，不复制 Puppeteer TypeScript 源码。

## 环境要求

- Java 21。
- Maven 3.9 及以上。
- 本机或服务器存在 Chrome/Chromium 可执行文件，或允许 Lancia 使用内置脚本安装浏览器。
- 生产环境默认使用 `--remote-debugging-pipe`，避免开放 DevTools TCP 端口。

## Maven

```xml
<dependency>
    <groupId>org.miaixz</groupId>
    <artifactId>lancia</artifactId>
    <version>8.0.6</version>
</dependency>
```

## 快速开始

### 启动浏览器

```java
import java.nio.file.Path;

import org.miaixz.lancia.Browser;
import org.miaixz.lancia.LaunchOptions;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.Puppeteer;

public class LaunchExample {

    public static void main(String[] args) {
        LaunchOptions options = new LaunchOptions();
        options.setExecutablePath(Path.of("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"));
        options.setPipe(true);
        options.setHeadless(true);
        options.addArg("--no-sandbox");

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
import java.net.URI;

import org.miaixz.lancia.Browser;
import org.miaixz.lancia.ConnectOptions;
import org.miaixz.lancia.Puppeteer;

public class ConnectExample {

    public static void main(String[] args) {
        ConnectOptions options = new ConnectOptions();
        options.setBrowserWSEndpoint(URI.create("ws://127.0.0.1:9222/devtools/browser/<id>"));

        try (Browser browser = Puppeteer.connect(options)) {
            System.out.println(browser.version());
        }
    }
}
```

### 页面操作、截图与 PDF

```java
import java.nio.file.Path;

import org.miaixz.lancia.Browser;
import org.miaixz.lancia.LaunchOptions;
import org.miaixz.lancia.PDFOptions;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.Puppeteer;
import org.miaixz.lancia.ScreenshotOptions;

public class PageExample {

    public static void main(String[] args) {
        LaunchOptions launch = new LaunchOptions();
        launch.setPipe(true);

        try (Browser browser = Puppeteer.launch(launch)) {
            Page page = browser.newPage();
            page.setContent("<!doctype html><title>Lancia</title><h1>Hello</h1>");
            Object title = page.evaluate("document.title");

            ScreenshotOptions screenshot = new ScreenshotOptions();
            screenshot.setFullPage(true);
            screenshot.setPath(Path.of("page.png"));
            page.screenshot(screenshot);

            PDFOptions pdf = new PDFOptions();
            pdf.setFormat("A4");
            pdf.setPrintBackground(true);
            pdf.setPath(Path.of("page.pdf"));
            page.pdf(pdf);

            System.out.println(title);
        }
    }
}
```

## 核心能力

| 能力 | 入口 |
|---|---|
| 启动与连接 | `Puppeteer.launch`、`Puppeteer.connect`、`ChromeLauncher`、`FirefoxLauncher` |
| 浏览器与上下文 | `Browser`、`Context`、`ContextOptions` |
| 页面与 Frame | `Page`、`FrameManager`、`Frame`、`ExecutionContext` |
| 网络 | `NetworkManager`、`Request`、`Response`、Cookie、Permission、Download |
| 输入与元素 | `Keyboard`、`Mouse`、`Touchscreen`、`ElementHandle`、`Locator` |
| 输出 | `ScreenshotOptions`、`PDFOptions`、`Page.screenshot`、`Page.pdf` |
| 辅助域 | `Coverage`、`Tracing`、`Accessibility`、`Dialog`、`ConsoleMessage` |
| 高级能力 | `DeviceRequestPrompt`、`WebAuthn`、`BluetoothEmulation`、`WebMCP`、`Extension`、`Screen` |
| 协议与传输 | `Session`、`Payload`、`Connection`、`CDPSession`、`PipeTransport`、`SocketTransport` |
| BiDi 与 Firefox | `BidiConnection`、`BidiBrowser`、`BidiBrowserContext`、`BidiPage`、`FirefoxLauncher` |

## 文档

- [API 文档](docs/API.md)
- [Puppeteer 兼容范围](docs/PUPPETEER.md)
- [测试矩阵](docs/TEST.md)

## 安全约束

- 生产默认使用 pipe，不直接暴露 Chrome DevTools HTTP 端口。
- URL、资源大小和并发限制由 `SecurityPolicy` 与 `ResourceLimits` 承担。
- 不允许把可执行任意 JavaScript 的浏览器调试能力直接暴露到公网。

## 验收

```bash
mvn test
mvn -DskipTests package
```
