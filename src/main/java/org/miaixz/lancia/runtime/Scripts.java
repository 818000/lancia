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
package org.miaixz.lancia.runtime;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.shared.protocol.TextWriter;

/**
 * Provides JavaScript source helpers for public utility delegates.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class Scripts {

    /**
     * Fixed classpath root for bundled injected scripts.
     */
    public static final String INJECTED_RESOURCE_ROOT = "META-INF/lancia/injected";

    /**
     * JavaScript value writer used by script builders.
     */
    private static final TextWriter WRITER = new TextWriter();

    /**
     * Default maximum scroll passes used before visual export.
     */
    private static final int DEFAULT_VISUAL_EXPORT_SCROLL_PASSES = 120;

    /**
     * Default stable page height rounds required before automatic scroll stops.
     */
    private static final int DEFAULT_VISUAL_EXPORT_STABLE_HEIGHT_ROUNDS = 4;

    /**
     * Default image wait timeout used before visual export.
     */
    private static final int DEFAULT_VISUAL_EXPORT_IMAGE_TIMEOUT_MILLIS = 6_000;

    /**
     * Waits for page images before visual export.
     */
    private static final String LOAD_IMAGES_TEMPLATE = """
            (async () => {
              const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
              const images = Array.from(document.images);
              await Promise.race([
                Promise.all(images.filter(image => !image.complete).map(image => new Promise(resolve => {
                  image.addEventListener('load', resolve, {once: true});
                  image.addEventListener('error', resolve, {once: true});
                }))),
                sleep(%d)
              ]);
              return JSON.stringify({
                timeoutMillis: %d,
                images: images.length,
                completeImages: images.filter(image => image.complete).length,
                pendingImages: images.filter(image => !image.complete).length
              });
            })()
            """;

    /**
     * Automatically scrolls the page to trigger lazy rendering.
     */
    private static final String SCROLL_PAGE_TEMPLATE = """
            (async () => {
              const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
              const scroller = document.scrollingElement || document.documentElement;
              const pageHeight = () => Math.max(
                scroller.scrollHeight,
                document.documentElement.scrollHeight,
                document.body ? document.body.scrollHeight : 0
              );
              const viewport = Math.max(window.innerHeight || 900, 900);
              const step = Math.max(500, Math.floor(viewport * 0.65));
              let y = 0;
              let passes = 0;
              let stableHeightRounds = 0;
              let previousHeight = 0;
              while (passes < %d && stableHeightRounds < %d) {
                const height = pageHeight();
                for (; y < height; y += step) {
                  scroller.scrollTo(0, y);
                  window.dispatchEvent(new Event('scroll'));
                  await sleep(380);
                }
                scroller.scrollTo(0, pageHeight());
                window.dispatchEvent(new Event('scroll'));
                await sleep(900);
                const nextHeight = pageHeight();
                stableHeightRounds = nextHeight === previousHeight ? stableHeightRounds + 1 : 0;
                previousHeight = nextHeight;
                passes++;
                y = Math.max(0, nextHeight - viewport);
              }
              scroller.scrollTo(0, 0);
              window.dispatchEvent(new Event('scroll'));
              await sleep(1200);
              return JSON.stringify({
                maxScrollPasses: %d,
                requiredStableHeightRounds: %d,
                passes,
                height: pageHeight(),
                stableHeightRounds
              });
            })()
            """;

    /**
     * Prepares a page for visual export.
     */
    private static final String EXPORT_PAGE_TEMPLATE = """
            (async () => {
              const exportTarget = %s;
              const printPageSize = %s;
              if (printPageSize) {
                const styleId = 'lancia-pdf-page-size-' + String(printPageSize).toLowerCase();
                let style = document.getElementById(styleId);
                if (!style) {
                  style = document.createElement('style');
                  style.id = styleId;
                  (document.head || document.documentElement).appendChild(style);
                }
                style.textContent = '@page { size: ' + printPageSize + '; }';
              }
              const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));
              if (document.fonts && document.fonts.ready) {
                await document.fonts.ready;
              }
              const scroller = document.scrollingElement || document.documentElement;
              const pageHeight = () => Math.max(
                scroller.scrollHeight,
                document.documentElement.scrollHeight,
                document.body ? document.body.scrollHeight : 0
              );
              const viewport = Math.max(window.innerHeight || 900, 900);
              const step = Math.max(500, Math.floor(viewport * 0.65));
              let y = 0;
              let passes = 0;
              let stableHeightRounds = 0;
              let previousHeight = 0;
              while (passes < %d && stableHeightRounds < %d) {
                const height = pageHeight();
                for (; y < height; y += step) {
                  scroller.scrollTo(0, y);
                  window.dispatchEvent(new Event('scroll'));
                  await sleep(380);
                }
                scroller.scrollTo(0, pageHeight());
                window.dispatchEvent(new Event('scroll'));
                await sleep(900);
                const nextHeight = pageHeight();
                stableHeightRounds = nextHeight === previousHeight ? stableHeightRounds + 1 : 0;
                previousHeight = nextHeight;
                passes++;
                y = Math.max(0, nextHeight - viewport);
              }
              scroller.scrollTo(0, 0);
              window.dispatchEvent(new Event('scroll'));
              await sleep(1200);
              const images = Array.from(document.images);
              await Promise.race([
                Promise.all(images.filter(image => !image.complete).map(image => new Promise(resolve => {
                  image.addEventListener('load', resolve, {once: true});
                  image.addEventListener('error', resolve, {once: true});
                }))),
                sleep(%d)
              ]);
              const skeletonLike = document.querySelectorAll(
                '[class*="skeleton"],[class*="loading"],[class*="placeholder"]'
              ).length;
              return JSON.stringify({
                exportTarget,
                printPageSize,
                maxScrollPasses: %d,
                requiredStableHeightRounds: %d,
                imageTimeoutMillis: %d,
                passes,
                height: pageHeight(),
                images: images.length,
                completeImages: images.filter(image => image.complete).length,
                pendingImages: images.filter(image => !image.complete).length,
                skeletonLike
              });
            })()
            """;

    /**
     * Applies the default print style.
     */
    public static final String APPLY_PRINT_STYLE = """
            (() => {
              const styleId = 'lancia-pdf-page-size-a4';
              let style = document.getElementById(styleId);
              if (!style) {
                style = document.createElement('style');
                style.id = styleId;
                (document.head || document.documentElement).appendChild(style);
              }
              style.textContent = '@page { size: A4; }';
              return JSON.stringify({applied: true, pageSize: 'A4', styleId});
            })()
            """;

    /**
     * Prepares a page for PNG screenshot export by scrolling lazy content and waiting for render resources.
     */
    public static final String CAPTURE_PAGE = buildCapturePage(
            DEFAULT_VISUAL_EXPORT_SCROLL_PASSES,
            DEFAULT_VISUAL_EXPORT_STABLE_HEIGHT_ROUNDS,
            DEFAULT_VISUAL_EXPORT_IMAGE_TIMEOUT_MILLIS);

    /**
     * Prepares a page for PDF export by applying print size, scrolling lazy content, and waiting for resources.
     */
    public static final String PRINT_PAGE = buildPrintPage(
            "A4",
            DEFAULT_VISUAL_EXPORT_SCROLL_PASSES,
            DEFAULT_VISUAL_EXPORT_STABLE_HEIGHT_ROUNDS,
            DEFAULT_VISUAL_EXPORT_IMAGE_TIMEOUT_MILLIS);

    /**
     * Automatically scrolls the page to trigger lazy rendering.
     */
    public static final String SCROLL_PAGE = buildScrollPage(
            DEFAULT_VISUAL_EXPORT_SCROLL_PASSES,
            DEFAULT_VISUAL_EXPORT_STABLE_HEIGHT_ROUNDS);

    /**
     * Waits for page images with the default timeout.
     */
    public static final String LOAD_IMAGES = buildLoadImages(DEFAULT_VISUAL_EXPORT_IMAGE_TIMEOUT_MILLIS);

    /**
     * Waits for web fonts.
     */
    public static final String LOAD_FONTS = """
            (async () => {
              if (!document.fonts || !document.fonts.ready) {
                return JSON.stringify({supported: false, ready: true});
              }
              await document.fonts.ready;
              return JSON.stringify({supported: true, ready: true});
            })()
            """;

    /**
     * Reads common page metrics.
     */
    public static final String PAGE_METRICS = """
            (() => JSON.stringify({
              width: Math.max(
                document.documentElement.scrollWidth,
                document.body ? document.body.scrollWidth : 0
              ),
              height: Math.max(
                document.documentElement.scrollHeight,
                document.body ? document.body.scrollHeight : 0
              ),
              viewportWidth: window.innerWidth,
              viewportHeight: window.innerHeight,
              body: !!document.body
            }))()
            """;

    /**
     * Hides the scripts constructor.
     */
    private Scripts() {
        // No initialization required.
    }

    /**
     * Builds a page image wait script.
     *
     * @param timeoutMillis image wait timeout in milliseconds
     * @return JavaScript evaluation string
     */
    public static String buildLoadImages(int timeoutMillis) {
        int timeout = positive(timeoutMillis, "timeoutMillis");
        return LOAD_IMAGES_TEMPLATE.formatted(timeout, timeout);
    }

    /**
     * Builds an automatic scroll script for triggering lazy rendering.
     *
     * @param maxScrollPasses    maximum scroll passes
     * @param stableHeightRounds stable page height rounds required before stopping
     * @return JavaScript evaluation string
     */
    public static String buildScrollPage(int maxScrollPasses, int stableHeightRounds) {
        int passes = positive(maxScrollPasses, "maxScrollPasses");
        int rounds = positive(stableHeightRounds, "stableHeightRounds");
        return SCROLL_PAGE_TEMPLATE.formatted(passes, rounds, passes, rounds);
    }

    /**
     * Builds a script that prepares a page for PNG screenshot export.
     *
     * @param maxScrollPasses    maximum scroll passes
     * @param stableHeightRounds stable page height rounds required before stopping
     * @param imageTimeoutMillis image wait timeout in milliseconds
     * @return JavaScript evaluation string
     */
    public static String buildCapturePage(int maxScrollPasses, int stableHeightRounds, int imageTimeoutMillis) {
        return buildVisualExport("PNG_SCREENSHOT", null, maxScrollPasses, stableHeightRounds, imageTimeoutMillis);
    }

    /**
     * Builds a script that prepares a page for A4 PDF export.
     *
     * @param maxScrollPasses    maximum scroll passes
     * @param stableHeightRounds stable page height rounds required before stopping
     * @param imageTimeoutMillis image wait timeout in milliseconds
     * @return JavaScript evaluation string
     */
    public static String buildPrintPage(
            String printPageSize,
            int maxScrollPasses,
            int stableHeightRounds,
            int imageTimeoutMillis) {
        return buildVisualExport("A4_PDF", printPageSize, maxScrollPasses, stableHeightRounds, imageTimeoutMillis);
    }

    /**
     * Sanitizes a sourceURL value.
     *
     * @param path path
     * @return sourceURL
     */
    public static String sourceUrl(String path) {
        return (path == null ? Normal.EMPTY : path).replace("\n", Normal.EMPTY).replace("\r", Normal.EMPTY);
    }

    /**
     * Returns a checked page function declaration.
     *
     * @param pageFunction page function
     * @return checked function
     */
    public static String checkedFunction(String pageFunction) {
        if (StringKit.isBlank(pageFunction)) {
            throw new InternalException("pageFunction must not be blank.");
        }
        return pageFunction;
    }

    /**
     * Builds a script that prepares a page for visual export.
     *
     * @param exportTarget       export target label
     * @param printPageSize      print page size, or {@code null}
     * @param maxScrollPasses    maximum scroll passes
     * @param stableHeightRounds stable page height rounds required before stopping
     * @param imageTimeoutMillis image wait timeout in milliseconds
     * @return JavaScript evaluation string
     */
    private static String buildVisualExport(
            String exportTarget,
            String printPageSize,
            int maxScrollPasses,
            int stableHeightRounds,
            int imageTimeoutMillis) {
        int passes = positive(maxScrollPasses, "maxScrollPasses");
        int rounds = positive(stableHeightRounds, "stableHeightRounds");
        int imageTimeout = positive(imageTimeoutMillis, "imageTimeoutMillis");
        return EXPORT_PAGE_TEMPLATE.formatted(
                literal(exportTarget),
                literal(printPageSize),
                passes,
                rounds,
                imageTimeout,
                passes,
                rounds,
                imageTimeout);
    }

    /**
     * Returns a JavaScript literal.
     *
     * @param value object to encode
     * @return JavaScript literal
     */
    private static String literal(Object value) {
        return WRITER.writeValue(value);
    }

    /**
     * Validates a positive integer script parameter.
     *
     * @param value parameter value
     * @param name  parameter name
     * @return validated value
     */
    private static int positive(int value, String name) {
        if (value <= Normal._0) {
            throw new IllegalArgumentException(name + " must be greater than 0.");
        }
        return value;
    }

}
