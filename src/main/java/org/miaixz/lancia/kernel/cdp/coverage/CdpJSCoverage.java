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
package org.miaixz.lancia.kernel.cdp.coverage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.events.EventBinding;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Collects JavaScript coverage ranges through the browser protocol.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpJSCoverage {

    /**
     * Current session.
     */
    private CDPSession session;
    /**
     * Whether this feature is enabled.
     */
    private boolean enabled;
    /**
     * Whether reset on navigation is enabled.
     */
    private boolean resetOnNavigation;
    /**
     * Whether report anonymous scripts is enabled.
     */
    private boolean reportAnonymousScripts;
    /**
     * Whether include raw script coverage is enabled.
     */
    private boolean includeRawScriptCoverage;
    /**
     * Mapped script URLs.
     */
    private final Map<String, String> scriptURLs = new LinkedHashMap<>();
    /**
     * Mapped script sources values.
     */
    private final Map<String, String> scriptSources = new LinkedHashMap<>();
    /**
     * Current binding.
     */
    private Binding binding = new EventBinding();

    /**
     * Creates a JS coverage.
     *
     * @param session protocol session
     */
    public CdpJSCoverage(CDPSession session) {
        this.session = session;
    }

    /**
     * Updates client.
     *
     * @param session protocol session
     */
    public void updateClient(CDPSession session) {
        this.session = session;
    }

    /**
     * Handles start.
     */
    public void start() {
        start(new CdpCoverage.JSCoverageOptions());
    }

    /**
     * Handles start.
     *
     * @param options operation options
     */
    public void start(CdpCoverage.JSCoverageOptions options) {
        if (enabled) {
            throw new InternalException("CdpJSCoverage is already enabled");
        }
        CdpCoverage.JSCoverageOptions actualOptions = options == null ? new CdpCoverage.JSCoverageOptions() : options;
        resetOnNavigation = actualOptions.isResetOnNavigation();
        reportAnonymousScripts = actualOptions.isReportAnonymousScripts();
        includeRawScriptCoverage = actualOptions.isIncludeRawScriptCoverage();
        enabled = true;
        scriptURLs.clear();
        scriptSources.clear();
        clearBindings();
        if (session != null) {
            binding = binding.combine(session.on("Debugger.scriptParsed", this::onScriptParsed));
            binding = binding.combine(session.on("Runtime.executionContextsCleared", this::onExecutionContextsCleared));
        }
        CDPSession.sendIfPresent(session, "Profiler.enable", Map.of());
        CDPSession.sendIfPresent(
                session,
                "Profiler.startPreciseCoverage",
                Map.of("callCount", includeRawScriptCoverage, "detailed", actualOptions.isUseBlockCoverage()));
        CDPSession.sendIfPresent(session, "Debugger.enable", Map.of());
        CDPSession.sendIfPresent(session, "Debugger.setSkipAllPauses", Map.of("skip", true));
    }

    /**
     * Returns the stop.
     *
     * @return stop value
     */
    public CdpPayload stop() {
        if (!enabled) {
            throw new InternalException("CdpJSCoverage is not enabled");
        }
        enabled = false;
        CdpPayload result = Awaitable.await(
                CDPSession.sendIfPresent(session, "Profiler.takePreciseCoverage", Map.of()),
                "Failed to read JS coverage.");
        CDPSession.sendIfPresent(session, "Profiler.stopPreciseCoverage", Map.of());
        CDPSession.sendIfPresent(session, "Profiler.disable", Map.of());
        CDPSession.sendIfPresent(session, "Debugger.disable", Map.of());
        clearBindings();
        return result;
    }

    /**
     * Returns the stop entries.
     *
     * @return values
     */
    public List<CdpCoverage.JSCoverageEntry> stopEntries() {
        CdpPayload result = stop();
        List<CdpCoverage.JSCoverageEntry> entries = new ArrayList<>();
        CdpPayload coverage = result.get("result");
        if (!coverage.isArray()) {
            return List.of();
        }
        for (CdpPayload script : coverage.elements()) {
            String scriptId = PayloadReader.text(script.get("scriptId"));
            String url = scriptURLs.get(scriptId);
            if (StringKit.isBlank(url) && reportAnonymousScripts) {
                url = "debugger://VM" + scriptId;
            }
            String source = scriptSources.get(scriptId);
            if (url == null || source == null) {
                continue;
            }
            List<CdpCoverage.ProtocolRange> ranges = new ArrayList<>();
            CdpPayload functions = script.get("functions");
            if (functions.isArray()) {
                for (CdpPayload function : functions.elements()) {
                    CdpPayload functionRanges = function.get("ranges");
                    if (functionRanges.isArray()) {
                        for (CdpPayload range : functionRanges.elements()) {
                            ranges.add(
                                    new CdpCoverage.ProtocolRange(range.get("startOffset").asInt(),
                                            range.get("endOffset").asInt(), range.get("count").asInt()));
                        }
                    }
                }
            }
            Map<String, Object> raw = includeRawScriptCoverage ? rawObject(script) : null;
            entries.add(new CdpCoverage.JSCoverageEntry(url, source, CdpCoverage.convertToDisjointRanges(ranges), raw));
        }
        return List.copyOf(entries);
    }

    /**
     * Handles on execution contexts cleared.
     *
     * @param ignored ignored value
     */
    private void onExecutionContextsCleared(CdpPayload ignored) {
        if (resetOnNavigation) {
            scriptURLs.clear();
            scriptSources.clear();
        }
    }

    /**
     * Handles on script parsed.
     *
     * @param event event type
     */
    private void onScriptParsed(CdpPayload event) {
        String scriptId = PayloadReader.text(event.get("scriptId"));
        String url = PayloadReader.text(event.get("url"));
        if (url.startsWith("pptr:")) {
            return;
        }
        if (StringKit.isBlank(url) && !reportAnonymousScripts) {
            return;
        }
        try {
            CdpPayload response = Awaitable.await(
                    CDPSession.sendIfPresent(session, "Debugger.getScriptSource", Map.of("scriptId", scriptId)),
                    "Failed to read script source.");
            scriptURLs.put(scriptId, url);
            scriptSources.put(scriptId, PayloadReader.text(response.get("scriptSource")));
        } catch (RuntimeException ignored) {
            // Implementation note.
        }
    }

    /**
     * Handles clear bindings.
     */
    private void clearBindings() {
        binding.unbind();
        binding = new EventBinding();
    }

    /**
     * Returns the raw object.
     *
     * @param payload protocol payload
     * @return mapped values
     */
    private Map<String, Object> rawObject(CdpPayload payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (payload == null || !payload.isObject()) {
            return result;
        }
        for (Map.Entry<String, CdpPayload> entry : payload.fields().entrySet()) {
            result.put(entry.getKey(), entry.getValue().raw());
        }
        return result;
    }

}
