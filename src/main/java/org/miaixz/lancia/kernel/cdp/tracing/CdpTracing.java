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
package org.miaixz.lancia.kernel.cdp.tracing;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Tracing;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.options.TracingOptions;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * CDP tracing implementation.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpTracing implements Tracing {

    /**
     * Current session.
     */
    private CDPSession session;
    /**
     * Current stream future.
     */
    private CompletableFuture<String> streamFuture = new CompletableFuture<>();
    /**
     * Current path.
     */
    private Path path;
    /**
     * Whether recording is enabled.
     */
    private boolean recording;

    /**
     * Creates a CDP tracing.
     *
     * @param session protocol session
     */
    public CdpTracing(CDPSession session) {
        updateClient(session);
    }

    /**
     * Updates client.
     *
     * @param session protocol session
     */
    public void updateClient(CDPSession session) {
        this.session = session;
        if (session != null) {
            session.on("Tracing.tracingComplete", this::onTracingComplete);
        }
        Logger.debug(false, "Page", "CDP tracing session updated: hasSession={}", session != null);
    }

    /**
     * Handles start.
     *
     * @param categories categories value
     * @param path       file path
     */
    public void start(List<String> categories, Path path) {
        start(new TracingOptions(path, false, categories));
    }

    /**
     * Handles start.
     *
     * @param options operation options
     */
    public void start(TracingOptions options) {
        if (recording) {
            throw new InternalException("Cannot start recording trace while already recording trace.");
        }
        TracingOptions safeOptions = options == null ? new TracingOptions() : options;
        this.path = safeOptions.path();
        this.streamFuture = new CompletableFuture<>();
        this.recording = true;
        List<String> categories = new ArrayList<>(
                safeOptions.categories().isEmpty() ? defaultCategories() : safeOptions.categories());
        if (safeOptions.screenshots()) {
            categories.add("disabled-by-default-devtools.screenshot");
        }
        Logger.debug(
                true,
                "Page",
                "CDP tracing start requested: categoryCount={}, screenshots={}, hasPath={}",
                categories.size(),
                safeOptions.screenshots(),
                path != null);
        List<String> includedCategories = categories.stream().filter(category -> !category.startsWith(Symbol.MINUS))
                .toList();
        List<String> excludedCategories = categories.stream().filter(category -> category.startsWith(Symbol.MINUS))
                .map(category -> category.substring(1)).toList();
        send(
                "Tracing.start",
                Map.of(
                        "transferMode",
                        "ReturnAsStream",
                        "traceConfig",
                        Map.of("includedCategories", includedCategories, "excludedCategories", excludedCategories)));
    }

    /**
     * Returns the stop.
     *
     * @return stop value
     */
    public byte[] stop() {
        if (!recording) {
            Logger.warn(true, "Page", "CDP tracing stop rejected because tracing is not active");
            throw new InternalException("Tracing has not been started.");
        }
        Logger.debug(true, "Page", "CDP tracing stop requested");
        send("Tracing.end", Map.of());
        recording = false;
        try {
            String stream = streamFuture.get(5, TimeUnit.SECONDS);
            Assert.notBlank(stream, "stream");
            byte[] bytes = Builder.readProtocolStream(session, stream);
            if (path != null) {
                FileKit.writeBytes(bytes, path.toFile());
            }
            Logger.debug(false, "Page", "CDP tracing stopped: bytes={}, written={}", bytes.length, path != null);
            return bytes;
        } catch (Exception ex) {
            Logger.error(false, "Page", "CDP tracing stop failed: message={}", ex.getMessage());
            throw new IllegalStateException("Failed to stop tracing.", ex);
        }
    }

    /**
     * Handles on tracing complete.
     *
     * @param params protocol parameters
     */
    private void onTracingComplete(CdpPayload params) {
        streamFuture.complete(PayloadReader.text(params.get("stream")));
        Logger.debug(false, "Page", "CDP tracing stream received");
    }

    /**
     * Returns the recording.
     *
     * @return {@code true} when the condition matches
     */
    public boolean recording() {
        return recording;
    }

    /**
     * Returns the path.
     *
     * @return path value
     */
    public Path path() {
        return path;
    }

    /**
     * Returns the default categories.
     *
     * @return values
     */
    private List<String> defaultCategories() {
        return List.of(
                "-*",
                "devtools.timeline",
                "v8.execute",
                "disabled-by-default-devtools.timeline",
                "disabled-by-default-devtools.timeline.frame",
                "toplevel",
                "blink.console",
                "blink.user_timing",
                "latencyInfo",
                "disabled-by-default-devtools.timeline.stack",
                "disabled-by-default-v8.cpu_profiler");
    }

    /**
     * Sends a protocol command.
     *
     * @param method protocol method
     * @param params protocol parameters
     */
    private void send(String method, Map<String, Object> params) {
        if (session != null) {
            Logger.debug(true, "Protocol", "CDP tracing command requested: method={}", method);
            session.send(method, params);
        } else {
            Logger.debug(true, "Protocol", "CDP tracing command skipped: method={}", method);
        }
    }

}
