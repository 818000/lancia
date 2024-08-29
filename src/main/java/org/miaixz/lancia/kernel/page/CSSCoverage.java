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
package org.miaixz.lancia.kernel.page;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.nimble.css.CSSStyleSheetHeader;
import org.miaixz.lancia.nimble.css.Range;
import org.miaixz.lancia.nimble.profiler.CoverageEntry;
import org.miaixz.lancia.nimble.profiler.CoverageRange;
import org.miaixz.lancia.socket.CDPSession;
import org.miaixz.lancia.worker.enums.CDPSessionEvent;
import org.miaixz.lancia.worker.events.StyleSheetAddedEvent;

import com.fasterxml.jackson.databind.JsonNode;

import io.reactivex.rxjava3.disposables.Disposable;

public class CSSCoverage {

    private final CDPSession client;
    private final List<Disposable> disposables = new ArrayList<>();
    private boolean enabled;
    private HashMap<String, String> stylesheetURLs;
    private HashMap<String, String> stylesheetSources;
    private boolean resetOnNavigation;

    public CSSCoverage(CDPSession client) {
        this.client = client;
        this.enabled = false;
        this.stylesheetURLs = new HashMap<>();
        this.stylesheetSources = new HashMap();
        this.resetOnNavigation = false;
    }

    public void start(boolean resetOnNavigation) {
        Assert.isTrue(!this.enabled, "CSSCoverage is already enabled");

        this.resetOnNavigation = resetOnNavigation;
        this.enabled = true;
        this.stylesheetURLs.clear();
        this.stylesheetSources.clear();
        this.disposables.add(Builder.fromEmitterEvent(this.client, CDPSessionEvent.CSS_styleSheetAdded)
                .subscribe((event) -> this.onStyleSheet((StyleSheetAddedEvent) event)));
        this.disposables.add(Builder.fromEmitterEvent(this.client, CDPSessionEvent.Runtime_executionContextsCleared)
                .subscribe((ignore) -> this.onExecutionContextsCleared()));
        this.client.send("DOM.enable");
        this.client.send("CSS.enable");
        this.client.send("CSS.startRuleUsageTracking");
    }

    private void onExecutionContextsCleared() {
        if (!this.resetOnNavigation)
            return;
        this.stylesheetURLs.clear();
        this.stylesheetSources.clear();
    }

    private void onStyleSheet(StyleSheetAddedEvent event) {
        CSSStyleSheetHeader header = event.getHeader();
        // Ignore anonymous scripts
        if (StringKit.isEmpty(header.getSourceURL()))
            return;

        ForkJoinPool.commonPool().submit(() -> {
            Map<String, Object> params = new HashMap<>();
            params.put("styleSheetId", header.getStyleSheetId());
            JsonNode response = client.send("CSS.getStyleSheetText", params);
            stylesheetURLs.put(header.getStyleSheetId(), header.getSourceURL());
            stylesheetSources.put(header.getStyleSheetId(), response.get("text").asText());
        });

    }

    public List<CoverageEntry> stop() {
        Assert.isTrue(this.enabled, "CSSCoverage is not enabled");
        this.enabled = false;

        JsonNode ruleTrackingResponse = this.client.send("CSS.stopRuleUsageTracking");

        this.client.send("CSS.disable", null, null, false);
        this.client.send("DOM.disable", null, null, false);

        // aggregate by styleSheetId
        Map<String, List<CoverageRange>> styleSheetIdToCoverage = new HashMap<>();
        JsonNode ruleUsageNode = ruleTrackingResponse.get("ruleUsage");
        Iterator<JsonNode> elements = ruleUsageNode.elements();
        while (elements.hasNext()) {
            JsonNode entry = elements.next();
            List<CoverageRange> ranges = styleSheetIdToCoverage.get(entry.get("styleSheetId").asText());
            if (ranges == null) {
                ranges = new ArrayList<>();
                styleSheetIdToCoverage.put(entry.get("styleSheetId").asText(), ranges);
            }
            boolean used = entry.get("used").asBoolean();
            if (used)
                ranges.add(new CoverageRange(entry.get("startOffset").asInt(), entry.get("endOffset").asInt(), 1));
            else
                ranges.add(new CoverageRange(entry.get("startOffset").asInt(), entry.get("endOffset").asInt(), 0));
        }

        List<CoverageEntry> coverage = new ArrayList<>();
        for (String styleSheetId : this.stylesheetURLs.keySet()) {
            String url = this.stylesheetURLs.get(styleSheetId);
            String text = this.stylesheetSources.get(styleSheetId);
            List<Range> ranges = Coverage.convertToDisjointRanges(styleSheetIdToCoverage.get(styleSheetId));
            coverage.add(new CoverageEntry(url, ranges, text));
        }
        return coverage;
    }

}
