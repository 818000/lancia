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
package org.miaixz.lancia.browser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;

import org.miaixz.bus.core.codec.binary.Base64;
import org.miaixz.bus.core.io.buffer.Buffer;
import org.miaixz.bus.core.io.source.Source;
import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.net.HTTP;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.core.xyz.IoKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.fabric.Context;
import org.miaixz.bus.fabric.Fabric;
import org.miaixz.bus.fabric.protocol.http.HttpResponse;
import org.miaixz.bus.fabric.protocol.http.HttpX;
import org.miaixz.bus.fabric.protocol.http.body.PayloadBody;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.runtime.ResourceLimits;
import org.miaixz.lancia.runtime.SecurityPolicy;

/**
 * Handles browser network requests, downloads, redirects, and metadata reads.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BrowserNetwork {

    /**
     * Default request timeout.
     */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(Normal._30);

    /**
     * Maximum redirect count.
     */
    private static final int MAX_REDIRECTS = Normal._20;

    /**
     * Download buffer size.
     */
    private static final int BUFFER_SIZE = Normal._8192;

    /**
     * Header value that disables connection reuse for one request.
     */
    private static final String CONNECTION_CLOSE = "close";

    /**
     * Shared Fabric context.
     */
    private static final Context FABRIC = Context.create();

    /**
     * Creates a browser network.
     */
    private BrowserNetwork() {
        // No initialization required.
    }

    /**
     * Sends a HEAD request and returns whether the target responds with HTTP 200.
     *
     * @param url request URL
     * @return {@code true} when the target responds with HTTP 200
     */
    public static boolean headHttpRequest(URI url) {
        URI actualUrl = Assert.notNull(url, "url");
        HttpResponse response = null;
        Logger.debug(
                true,
                "Browser",
                "HEAD request started: url={}",
                requestUri(actualUrl).toString().replaceAll("[?#].*$", "?<redacted>"));
        try {
            response = httpRequest(actualUrl, HTTP.HEAD, false);
            boolean result = response.code() == HTTP.HTTP_OK;
            Logger.debug(
                    false,
                    "Browser",
                    "HEAD request completed: url={}, status={}, available={}",
                    requestUri(actualUrl).toString().replaceAll("[?#].*$", "?<redacted>"),
                    response.code(),
                    result);
            return result;
        } catch (RuntimeException ex) {
            Logger.warn(
                    false,
                    "Browser",
                    ex,
                    "HEAD request failed: {}",
                    requestUri(actualUrl).toString().replaceAll("[?#].*$", "?<redacted>"));
            return false;
        } finally {
            closeResponseBody(response);
        }
    }

    /**
     * Sends an HTTP request and returns an input stream response.
     *
     * @param url    request URL
     * @param method request method
     * @return response
     */
    public static HttpResponse httpRequest(URI url, String method) {
        return httpRequest(url, method, true);
    }

    /**
     * Sends an HTTP request and returns an input stream response.
     *
     * <p>
     * Fabric manages connection reuse. The {@code keepAlive} flag preserves Puppeteer API call semantics.
     * </p>
     *
     * @param url       request URL
     * @param method    request method
     * @param keepAlive whether to preserve keep-alive semantics
     * @return response
     */
    public static HttpResponse httpRequest(URI url, String method, boolean keepAlive) {
        return send(url, method, keepAlive, Normal._0);
    }

    /**
     * Sends an HTTP request and passes the response to a handler.
     *
     * @param url      request URL
     * @param method   request method
     * @param response response handler
     */
    public static void httpRequest(URI url, String method, ResponseHandler response) {
        httpRequest(url, method, response, true);
    }

    /**
     * Sends an HTTP request and passes the response to a handler.
     *
     * @param url       request URL
     * @param method    request method
     * @param response  response handler
     * @param keepAlive whether to preserve keep-alive semantics
     */
    public static void httpRequest(URI url, String method, ResponseHandler response, boolean keepAlive) {
        Assert.notNull(response, "response");
        HttpResponse httpResponse = httpRequest(url, method, keepAlive);
        try {
            response.accept(httpResponse);
        } catch (IOException ex) {
            throw new InternalException("Failed to handle HTTP response: " + url, ex);
        }
    }

    /**
     * Downloads a file to a target path.
     *
     * @param url             download URL
     * @param destinationPath target file path
     */
    public static void downloadFile(URI url, Path destinationPath) {
        downloadFile(url, destinationPath, null);
    }

    /**
     * Downloads a file to a target path.
     *
     * @param url              download URL
     * @param destinationPath  target file path
     * @param progressCallback progress callback
     */
    public static void downloadFile(URI url, Path destinationPath, ProgressCallback progressCallback) {
        downloadFile(url, destinationPath, progressCallback, SecurityPolicy.defaultPolicy(), ResourceLimits.defaults());
    }

    /**
     * Downloads a file to a target path.
     *
     * @param url              download URL
     * @param destinationPath  target file path
     * @param progressCallback progress callback
     * @param securityPolicy   security policy
     * @param resourceLimits   resource limits
     */
    public static void downloadFile(
            URI url,
            Path destinationPath,
            ProgressCallback progressCallback,
            SecurityPolicy securityPolicy,
            ResourceLimits resourceLimits) {
        URI actualUrl = Assert.notNull(url, "url");
        Path actualDestinationPath = Assert.notNull(destinationPath, "destinationPath").toAbsolutePath().normalize();
        SecurityPolicy actualSecurityPolicy = policy(securityPolicy);
        ResourceLimits actualResourceLimits = limits(resourceLimits);
        Logger.debug(
                true,
                "Browser",
                "Download started: url={}, destination={}",
                requestUri(actualUrl).toString().replaceAll("[?#].*$", "?<redacted>"),
                actualDestinationPath);
        HttpResponse response = send(actualUrl, HTTP.GET, true, Normal._0, actualSecurityPolicy, actualResourceLimits);
        try {
            if (response.code() != HTTP.HTTP_OK) {
                throw new InternalException(
                        "Download failed: server returned code " + response.code() + ". URL: " + actualUrl);
            }
            Path parent = actualDestinationPath.getParent();
            if (parent != null) {
                FileKit.mkdir(parent.toFile());
            }
            long totalBytes = contentLength(response);
            if (totalBytes > Normal.LONG_ZERO) {
                actualResourceLimits.validateDownloadBytes(totalBytes);
            }
            long downloadedBytes = copyToFile(
                    bodyStream(response),
                    actualDestinationPath,
                    totalBytes,
                    progressCallback,
                    actualResourceLimits);
            Logger.debug(
                    false,
                    "Browser",
                    "Download completed: url={}, destination={}, bytes={}",
                    requestUri(actualUrl).toString().replaceAll("[?#].*$", "?<redacted>"),
                    actualDestinationPath,
                    downloadedBytes);
        } catch (IOException ex) {
            deletePartial(actualDestinationPath);
            Logger.error(
                    false,
                    "Browser",
                    ex,
                    "Download failed: url={}, destination={}",
                    requestUri(actualUrl).toString().replaceAll("[?#].*$", "?<redacted>"),
                    actualDestinationPath);
            throw new InternalException("Failed to download file: " + actualUrl + " -> " + actualDestinationPath, ex);
        } catch (RuntimeException ex) {
            deletePartial(actualDestinationPath);
            Logger.error(
                    false,
                    "Browser",
                    ex,
                    "Download failed: url={}, destination={}",
                    requestUri(actualUrl).toString().replaceAll("[?#].*$", "?<redacted>"),
                    actualDestinationPath);
            throw ex;
        } finally {
            closeResponseBody(response);
        }
    }

    /**
     * Reads a JSON payload from a URL.
     *
     * @param url request URL
     * @return JSON payload
     */
    public static CdpPayload getJSON(URI url) {
        return getJSON(url, SecurityPolicy.defaultPolicy(), ResourceLimits.defaults());
    }

    /**
     * Reads a JSON payload from a URL.
     *
     * @param url            request URL
     * @param securityPolicy security policy
     * @param resourceLimits resource limits
     * @return JSON payload
     */
    public static CdpPayload getJSON(URI url, SecurityPolicy securityPolicy, ResourceLimits resourceLimits) {
        URI actualUrl = Assert.notNull(url, "url");
        Logger.debug(
                true,
                "Browser",
                "JSON read started: url={}",
                requestUri(actualUrl).toString().replaceAll("[?#].*$", "?<redacted>"));
        String text = getText(actualUrl, securityPolicy, resourceLimits);
        try {
            CdpPayload payload = CdpPayload.parse(text);
            Logger.debug(
                    false,
                    "Browser",
                    "JSON read completed: url={}, chars={}",
                    requestUri(actualUrl).toString().replaceAll("[?#].*$", "?<redacted>"),
                    text.length());
            return payload;
        } catch (RuntimeException ex) {
            Logger.error(
                    false,
                    "Browser",
                    ex,
                    "JSON parse failed: url={}, chars={}",
                    requestUri(actualUrl).toString().replaceAll("[?#].*$", "?<redacted>"),
                    text.length());
            throw new InternalException("Could not parse JSON from " + actualUrl, ex);
        }
    }

    /**
     * Reads text from a URL.
     *
     * @param url request URL
     * @return response text
     */
    public static String getText(URI url) {
        return getText(url, SecurityPolicy.defaultPolicy(), ResourceLimits.defaults());
    }

    /**
     * Reads text from a URL.
     *
     * @param url            request URL
     * @param securityPolicy security policy
     * @param resourceLimits resource limits
     * @return response text
     */
    public static String getText(URI url, SecurityPolicy securityPolicy, ResourceLimits resourceLimits) {
        URI actualUrl = Assert.notNull(url, "url");
        SecurityPolicy actualSecurityPolicy = policy(securityPolicy);
        ResourceLimits actualResourceLimits = limits(resourceLimits);
        Logger.debug(
                true,
                "Browser",
                "Text read started: url={}",
                requestUri(actualUrl).toString().replaceAll("[?#].*$", "?<redacted>"));
        HttpResponse response = send(actualUrl, HTTP.GET, false, Normal._0, actualSecurityPolicy, actualResourceLimits);
        try {
            if (response.code() >= HTTP.HTTP_BAD_REQUEST) {
                throw new InternalException("Got status code " + response.code());
            }
            long totalBytes = contentLength(response);
            if (totalBytes > Normal.LONG_ZERO) {
                actualResourceLimits.validateMetadataBytes(totalBytes);
            }
            String result = bodyString(response, actualResourceLimits);
            Logger.debug(
                    false,
                    "Browser",
                    "Text read completed: url={}, status={}, chars={}",
                    requestUri(actualUrl).toString().replaceAll("[?#].*$", "?<redacted>"),
                    response.code(),
                    result.length());
            return result;
        } catch (IOException ex) {
            Logger.error(
                    false,
                    "Browser",
                    ex,
                    "Text read failed: url={}",
                    requestUri(actualUrl).toString().replaceAll("[?#].*$", "?<redacted>"));
            throw new InternalException("Failed to read HTTP response: " + actualUrl, ex);
        } catch (RuntimeException ex) {
            Logger.error(
                    false,
                    "Browser",
                    ex,
                    "Text read failed: url={}, status={}",
                    requestUri(actualUrl).toString().replaceAll("[?#].*$", "?<redacted>"),
                    response.code());
            throw ex;
        } finally {
            closeResponseBody(response);
        }
    }

    /**
     * Normalizes an HTTP header value.
     *
     * @param header HTTP header value
     * @return normalized HTTP header value
     */
    public static String normalizeHeaderValue(String header) {
        if (header == null || !header.contains(String.valueOf(Symbol.C_LF))) {
            return header;
        }
        return String.join(
                Symbol.COMMA + Symbol.SPACE,
                String.valueOf(header).lines().map(String::trim).filter(StringKit::isNotBlank).toList());
    }

    /**
     * Sends a request and handles redirects.
     *
     * @param url           request URL
     * @param method        request method
     * @param keepAlive     whether to preserve keep-alive semantics
     * @param redirectCount current redirect count
     * @return response
     */
    private static HttpResponse send(URI url, String method, boolean keepAlive, int redirectCount) {
        return send(url, method, keepAlive, redirectCount, SecurityPolicy.defaultPolicy(), ResourceLimits.defaults());
    }

    /**
     * Sends a request and handles redirects.
     *
     * @param url            request URL
     * @param method         request method
     * @param keepAlive      whether to preserve keep-alive semantics
     * @param redirectCount  current redirect count
     * @param securityPolicy security policy
     * @param resourceLimits resource limits
     * @return response
     */
    private static HttpResponse send(
            URI url,
            String method,
            boolean keepAlive,
            int redirectCount,
            SecurityPolicy securityPolicy,
            ResourceLimits resourceLimits) {
        URI actualUrl = Assert.notNull(url, "url");
        SecurityPolicy actualSecurityPolicy = policy(securityPolicy);
        ResourceLimits actualResourceLimits = limits(resourceLimits);
        actualSecurityPolicy.validateHttpUrl(actualUrl);
        if (redirectCount > MAX_REDIRECTS) {
            throw new InternalException("HTTP redirect count exceeded the limit: " + actualUrl);
        }
        try {
            String actualMethod = normalizeMethod(method);
            HttpResponse response = requestBuilder(actualUrl, actualMethod, keepAlive).method(actualMethod).execute();
            Optional<String> location = redirectLocation(response);
            if (location.isPresent()) {
                closeResponseBody(response);
                URI redirectUrl = actualUrl.resolve(location.getOrThrow());
                actualSecurityPolicy.validateHttpUrl(redirectUrl);
                return send(
                        redirectUrl,
                        method,
                        keepAlive,
                        redirectCount + Normal._1,
                        actualSecurityPolicy,
                        actualResourceLimits);
            }
            return response;
        } catch (RuntimeException ex) {
            if (ex instanceof InternalException) {
                throw ex;
            }
            throw new InternalException("HTTP request failed: " + actualUrl, ex);
        }
    }

    /**
     * Creates an HTTP request builder.
     *
     * @param url       request URL
     * @param method    request method
     * @param keepAlive whether to preserve keep-alive semantics
     * @return HTTP request builder
     */
    private static HttpX.Builder requestBuilder(URI url, String method, boolean keepAlive) {
        HttpX.Builder builder = Fabric.http(FABRIC).url(requestUri(url).toString()).method(method)
                .timeout(REQUEST_TIMEOUT);
        authHeader(url).ifPresent(value -> builder.header(HTTP.AUTHORIZATION, value));
        if (keepAlive) {
            Logger.debug(
                    false,
                    "Browser",
                    "HTTP request uses keep-alive semantics: {} {}",
                    normalizeMethod(method),
                    requestUri(url).toString().replaceAll("[?#].*$", "?<redacted>"));
        } else {
            builder.header(HTTP.CONNECTION, CONNECTION_CLOSE);
        }
        return builder;
    }

    /**
     * Normalizes an HTTP method.
     *
     * @param method request method
     * @return uppercase request method
     */
    private static String normalizeMethod(String method) {
        if (StringKit.isBlank(method)) {
            throw new InternalException("HTTP request method must not be blank.");
        }
        return method.toUpperCase(Locale.ROOT);
    }

    /**
     * Builds a request URI without user-info credentials.
     *
     * @param url original URI
     * @return request URI
     */
    private static URI requestUri(URI url) {
        if (StringKit.isBlank(url.getUserInfo())) {
            return url;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(url.getScheme()).append(Symbol.COLON).append(Symbol.SLASH).append(Symbol.SLASH);
        builder.append(formatHost(url));
        if (url.getPort() >= Normal._0) {
            builder.append(Symbol.COLON).append(url.getPort());
        }
        builder.append(StringKit.isBlank(url.getRawPath()) ? Symbol.SLASH : url.getRawPath());
        if (StringKit.isNotBlank(url.getRawQuery())) {
            builder.append(Symbol.QUESTION_MARK).append(url.getRawQuery());
        }
        if (StringKit.isNotBlank(url.getRawFragment())) {
            builder.append(Symbol.C_HASH).append(url.getRawFragment());
        }
        return URI.create(builder.toString());
    }

    /**
     * Formats a host name.
     *
     * @param url request URI
     * @return host name
     */
    private static String formatHost(URI url) {
        String host = url.getHost();
        if (host != null && host.contains(Symbol.COLON) && !host.startsWith(Symbol.BRACKET_LEFT)) {
            return Symbol.BRACKET_LEFT + host + Symbol.BRACKET_RIGHT;
        }
        return String.valueOf(host);
    }

    /**
     * Returns a Basic authentication header.
     *
     * @param url request URI
     * @return authentication header
     */
    private static Optional<String> authHeader(URI url) {
        String userInfo = url.getUserInfo();
        if (StringKit.isBlank(userInfo)) {
            return Optional.empty();
        }
        return Optional.of("Basic" + Symbol.SPACE + Base64.encode(userInfo, Charset.UTF_8));
    }

    /**
     * Returns a redirect location.
     *
     * @param response response
     * @return redirect location
     */
    private static Optional<String> redirectLocation(HttpResponse response) {
        int statusCode = response.code();
        if (statusCode >= HTTP.HTTP_MULT_CHOICE && statusCode < HTTP.HTTP_BAD_REQUEST) {
            return Optional.ofBlankAble(response.headers().get(HTTP.LOCATION));
        }
        return Optional.empty();
    }

    /**
     * Closes a response body when possible.
     *
     * @param response response
     */
    private static void closeResponseBody(HttpResponse response) {
        IoKit.close(response);
    }

    /**
     * Reads the response body length.
     *
     * @param response response
     * @return response body length, or {@code 0} when unknown
     */
    private static long contentLength(HttpResponse response) {
        long headerLength = response.headers().contentLength();
        if (headerLength >= Normal.LONG_ZERO) {
            return headerLength;
        }
        PayloadBody body = response.body();
        return body == null || body.length() < Normal.LONG_ZERO ? Normal.LONG_ZERO : body.length();
    }

    /**
     * Copies a response body to a file.
     *
     * @param input            response input stream
     * @param destinationPath  target file path
     * @param totalBytes       total bytes
     * @param progressCallback progress callback
     * @throws IOException when copying fails
     */
    private static long copyToFile(
            InputStream input,
            Path destinationPath,
            long totalBytes,
            ProgressCallback progressCallback,
            ResourceLimits resourceLimits) throws IOException {
        ResourceLimits actualResourceLimits = limits(resourceLimits);
        long downloadedBytes = Normal.LONG_ZERO;
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream source = input; OutputStream target = Files.newOutputStream(destinationPath)) {
            int read;
            while ((read = source.read(buffer)) >= Normal._0) {
                if (read == Normal._0) {
                    continue;
                }
                downloadedBytes += read;
                actualResourceLimits.validateDownloadBytes(downloadedBytes);
                target.write(buffer, Normal._0, read);
                if (progressCallback != null) {
                    progressCallback.onProgress(downloadedBytes, totalBytes);
                }
            }
            return downloadedBytes;
        } catch (InternalException ex) {
            if (ex.getCause() instanceof IOException io) {
                throw io;
            }
            throw ex;
        }
    }

    /**
     * Returns a response body stream.
     *
     * @param response response
     * @return response body stream
     */
    private static InputStream bodyStream(HttpResponse response) {
        PayloadBody body = response.body();
        return body == null ? InputStream.nullInputStream() : new SourceInputStream(body.source());
    }

    /**
     * Reads a response body as text.
     *
     * @param response response
     * @return response body text
     * @throws IOException when reading fails
     */
    private static String bodyString(HttpResponse response, ResourceLimits resourceLimits) throws IOException {
        ResourceLimits actualResourceLimits = limits(resourceLimits);
        byte[] buffer = new byte[BUFFER_SIZE];
        int size = Normal._0;
        try (InputStream source = bodyStream(response);
                java.io.ByteArrayOutputStream target = new java.io.ByteArrayOutputStream()) {
            int read;
            while ((read = source.read(buffer)) >= Normal._0) {
                if (read == Normal._0) {
                    continue;
                }
                size += read;
                actualResourceLimits.validateMetadataBytes(size);
                target.write(buffer, Normal._0, read);
            }
            return target.size() == Normal._0 ? Normal.EMPTY
                    : new String(target.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * Returns a security policy.
     *
     * @param securityPolicy security policy
     * @return security policy
     */
    private static SecurityPolicy policy(SecurityPolicy securityPolicy) {
        return securityPolicy == null ? SecurityPolicy.defaultPolicy() : securityPolicy;
    }

    /**
     * Returns resource limits.
     *
     * @param resourceLimits resource limits
     * @return resource limits
     */
    private static ResourceLimits limits(ResourceLimits resourceLimits) {
        return resourceLimits == null ? ResourceLimits.defaults() : resourceLimits;
    }

    /**
     * Deletes a partial download.
     *
     * @param destinationPath target file path
     */
    private static void deletePartial(Path destinationPath) {
        try {
            if (destinationPath != null && Files.exists(destinationPath)) {
                FileKit.remove(destinationPath.toFile());
            }
        } catch (RuntimeException ex) {
            Logger.warn(false, "Browser", ex, "Partial download cleanup failed: {}", destinationPath);
        }
    }

    /**
     * Defines the response handler contract.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    @FunctionalInterface
    public interface ResponseHandler {

        /**
         * Handles a response.
         *
         * @param response response
         * @throws IOException when handling fails
         */
        void accept(HttpResponse response) throws IOException;
    }

    /**
     * Defines the progress callback contract.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    @FunctionalInterface
    public interface ProgressCallback {

        /**
         * Handles download progress.
         *
         * @param downloadedBytes downloaded bytes
         * @param totalBytes      total bytes, or {@code 0} when unknown
         */
        void onProgress(long downloadedBytes, long totalBytes);
    }

    /**
     * Adapts a Fabric source to the stream contract used by download and metadata reads.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class SourceInputStream extends InputStream {

        /**
         * Source buffer size.
         */
        private static final int SOURCE_BUFFER_SIZE = BUFFER_SIZE;

        /**
         * Single-byte read buffer.
         */
        private final byte[] single = new byte[Normal._1];

        /**
         * Fabric source.
         */
        private final Source source;

        /**
         * Read buffer.
         */
        private final Buffer buffer = new Buffer();

        /**
         * Closed flag.
         */
        private boolean closed;

        /**
         * Creates a stream adapter.
         *
         * @param source Fabric source
         */
        private SourceInputStream(Source source) {
            this.source = Assert.notNull(source, "source");
        }

        @Override
        public int read() throws IOException {
            int read = read(single, Normal._0, Normal._1);
            return read < Normal._0 ? Normal.__1 : single[Normal._0] & 0xff;
        }

        @Override
        public int read(byte[] target, int offset, int length) throws IOException {
            if (closed) {
                throw new IOException("Source is closed.");
            }
            Assert.notNull(target, "target");
            if (offset < Normal._0 || length < Normal._0 || length > target.length - offset) {
                throw new IndexOutOfBoundsException();
            }
            if (length == Normal._0) {
                return Normal._0;
            }
            if (buffer.size() == Normal.LONG_ZERO) {
                long read = source.read(buffer, Math.min(length, SOURCE_BUFFER_SIZE));
                if (read < Normal.LONG_ZERO) {
                    return Normal.__1;
                }
            }
            return buffer.read(target, offset, (int) Math.min(length, buffer.size()));
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                source.close();
            }
        }
    }

}
