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
 * Collects CSS coverage ranges through the browser protocol.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpCSSCoverage {

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
     * Mapped stylesheet URLs.
     */
    private final Map<String, String> stylesheetURLs = new LinkedHashMap<>();
    /**
     * Mapped stylesheet sources values.
     */
    private final Map<String, String> stylesheetSources = new LinkedHashMap<>();
    /**
     * Current binding.
     */
    private Binding binding = new EventBinding();

    /**
     * Creates a CSS coverage.
     *
     * @param session protocol session
     */
    public CdpCSSCoverage(CDPSession session) {
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
        start(new CdpCoverage.CSSCoverageOptions());
    }

    /**
     * Handles start.
     *
     * @param options operation options
     */
    public void start(CdpCoverage.CSSCoverageOptions options) {
        if (enabled) {
            throw new InternalException("CdpCSSCoverage is already enabled");
        }
        CdpCoverage.CSSCoverageOptions actualOptions = options == null ? new CdpCoverage.CSSCoverageOptions() : options;
        resetOnNavigation = actualOptions.isResetOnNavigation();
        enabled = true;
        stylesheetURLs.clear();
        stylesheetSources.clear();
        clearBindings();
        if (session != null) {
            binding = binding.combine(session.on("CSS.styleSheetAdded", this::onStyleSheet));
            binding = binding.combine(session.on("Runtime.executionContextsCleared", this::onExecutionContextsCleared));
        }
        CDPSession.sendIfPresent(session, "DOM.enable", Map.of());
        CDPSession.sendIfPresent(session, "CSS.enable", Map.of());
        CDPSession.sendIfPresent(session, "CSS.startRuleUsageTracking", Map.of());
    }

    /**
     * Returns the stop.
     *
     * @return stop value
     */
    public CdpPayload stop() {
        if (!enabled) {
            throw new InternalException("CdpCSSCoverage is not enabled");
        }
        enabled = false;
        CdpPayload result = Awaitable.await(
                CDPSession.sendIfPresent(session, "CSS.stopRuleUsageTracking", Map.of()),
                "Failed to read CSS coverage.");
        CDPSession.sendIfPresent(session, "CSS.disable", Map.of());
        CDPSession.sendIfPresent(session, "DOM.disable", Map.of());
        clearBindings();
        return result;
    }

    /**
     * Returns the stop entries.
     *
     * @return values
     */
    public List<CdpCoverage.CoverageEntry> stopEntries() {
        CdpPayload result = stop();
        Map<String, List<CdpCoverage.ProtocolRange>> styleSheetRanges = new LinkedHashMap<>();
        CdpPayload ruleUsage = result.get("ruleUsage");
        if (ruleUsage.isArray()) {
            for (CdpPayload entry : ruleUsage.elements()) {
                String styleSheetId = PayloadReader.text(entry.get("styleSheetId"));
                styleSheetRanges.computeIfAbsent(styleSheetId, key -> new ArrayList<>()).add(
                        new CdpCoverage.ProtocolRange(entry.get("startOffset").asInt(), entry.get("endOffset").asInt(),
                                entry.get("used").asBoolean() ? 1 : 0));
            }
        }
        List<CdpCoverage.CoverageEntry> entries = new ArrayList<>();
        for (String styleSheetId : stylesheetURLs.keySet()) {
            String url = stylesheetURLs.get(styleSheetId);
            String text = stylesheetSources.get(styleSheetId);
            if (url == null || text == null) {
                continue;
            }
            entries.add(
                    new CdpCoverage.CoverageEntry(url, text, CdpCoverage
                            .convertToDisjointRanges(styleSheetRanges.getOrDefault(styleSheetId, List.of()))));
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
            stylesheetURLs.clear();
            stylesheetSources.clear();
        }
    }

    /**
     * Handles on style sheet.
     *
     * @param event event type
     */
    private void onStyleSheet(CdpPayload event) {
        CdpPayload header = event.get("header");
        String sourceURL = PayloadReader.text(header.get("sourceURL"));
        if (StringKit.isBlank(sourceURL)) {
            return;
        }
        String styleSheetId = PayloadReader.text(header.get("styleSheetId"));
        try {
            CdpPayload response = Awaitable.await(
                    CDPSession.sendIfPresent(session, "CSS.getStyleSheetText", Map.of("styleSheetId", styleSheetId)),
                    "Failed to read stylesheet source.");
            stylesheetURLs.put(styleSheetId, sourceURL);
            stylesheetSources.put(styleSheetId, PayloadReader.text(response.get("text")));
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

}
