# Lancia

[English](README.md) | [中文](README_CN.md)

Lancia is a native Java browser automation library with a Puppeteer-compatible public surface. It launches and connects to Chrome/Chromium through CDP, supports WebDriver BiDi routes for Firefox, and provides browser/page automation, screenshots, PDF output, network control, input, emulation, tracing, coverage, accessibility, extension, and browser download/cache features.

The project follows Puppeteer's public behavior and protocol flow where they fit Java. The implementation is written in Java and does not copy Puppeteer's TypeScript source code.

## Requirements

- Java 21 or later.
- Maven 3.9 or later.
- A Chrome/Chromium or Firefox executable, or permission for Lancia to download managed browser builds.
- Linux servers and CI images may need browser system libraries installed before launching a downloaded browser.

## Installation

```xml
<dependency>
    <groupId>org.miaixz</groupId>
    <artifactId>lancia</artifactId>
    <version>8.5.1</version>
</dependency>
```

## Quick Start

### Download Managed Browsers

`Puppeteer.downloadBrowsers()` reads Puppeteer-style environment variables and installs configured browser builds into the local cache. Chrome downloads use Chrome for Testing and can fall back to npmmirror when the Google download endpoint is unavailable.

```java
import org.miaixz.lancia.Puppeteer;

public class InstallBrowsers {

    public static void main(String[] args) {
        Puppeteer.downloadBrowsers().forEach(System.out::println);
    }
}
```

### Launch a Browser

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

        // Use a system browser when you do not want to use the managed cache.
        // options.setExecutablePath("/path/to/chrome");

        try (Browser browser = Puppeteer.launch(options)) {
            Page page = browser.newPage();
            page.goTo("https://example.com");
            System.out.println(page.url());
        }
    }
}
```

### Connect to an Existing Browser

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

### Capture Screenshot and PDF

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

## Main APIs

| Area | APIs |
|---|---|
| Entry point | `Puppeteer.launch`, `Puppeteer.connect`, `Puppeteer.downloadBrowsers` |
| Launch and connect | `LaunchOptions`, `ConnectOptions`, `BrowserVariant` |
| Browser model | `Browser`, `Context`, `Target`, `Page`, `Frame` |
| Page output | `ScreenshotOptions`, `PDFOptions`, `Page.screenshot`, `Page.pdf` |
| Page interaction | `Keyboard`, `Mouse`, `Touchscreen`, `Element`, `Locator` |
| Network and permissions | `Request`, `Response`, cookies, downloads, permissions |
| Advanced domains | `Coverage`, `Tracing`, `Accessibility`, dialogs, console messages |
| Runtime utilities | `Puppeteer.executablePath`, `Puppeteer.browserVersion`, `Puppeteer.trimCache` |
| Protocols | CDP by default, WebDriver BiDi when configured or when launching Firefox |

## Configuration

Lancia reads Puppeteer-style environment variables at startup:

| Variable | Purpose |
|---|---|
| `PUPPETEER_CACHE_DIR` | Browser cache directory. Defaults to `~/.cache/puppeteer`. |
| `PUPPETEER_EXECUTABLE_PATH` | System browser executable path. Also implies download skipping. |
| `PUPPETEER_BROWSER` | Default browser, such as `chrome` or `firefox`. |
| `PUPPETEER_TMP_DIR` | Temporary directory for launch/runtime files. |
| `PUPPETEER_SKIP_DOWNLOAD` | Skips all managed browser downloads when truthy. |
| `PUPPETEER_LOGLEVEL` | Runtime log level: `silent`, `error`, or `warn`. |
| `PUPPETEER_CHROME_VERSION` | Chrome build or channel used for managed downloads. |
| `PUPPETEER_CHROME_DOWNLOAD_BASE_URL` | Custom Chrome download base URL. |
| `PUPPETEER_CHROME_SKIP_DOWNLOAD` | Skips only Chrome downloads. |
| `PUPPETEER_CHROME_HEADLESS_SHELL_SKIP_DOWNLOAD` | Skips only Chrome Headless Shell downloads. |
| `PUPPETEER_FIREFOX_SKIP_DOWNLOAD` | Enables or skips managed Firefox downloads. Firefox is skipped by default. |

Browser-specific variables also support `_VERSION`, `_DOWNLOAD_BASE_URL`, `_EXPECTED_ARCHIVE_SHA256`, and `_ALLOW_UNVERIFIED_DOWNLOAD` suffixes.

## Security Notes

- Prefer `LaunchOptions#setPipe(true)` for local Chrome/CDP launches so DevTools is not exposed through a TCP port.
- Do not expose a browser debugging endpoint directly to the public internet.
- Treat `Page.evaluate` input as privileged JavaScript.
- Use `SecurityPolicy` and `ResourceLimits` when handling untrusted URLs, headers, downloads, or protocol payloads.

## Build

```bash
mvn verify
mvn -DskipTests package
```

## License

Lancia is released under the Apache License 2.0. See [LICENSE](LICENSE) for details.
