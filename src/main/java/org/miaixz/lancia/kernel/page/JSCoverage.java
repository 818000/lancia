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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.xyz.CollKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.nimble.css.Range;
import org.miaixz.lancia.nimble.debugger.ScriptParsedEvent;
import org.miaixz.lancia.nimble.profiler.*;
import org.miaixz.lancia.socket.CDPSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.reactivex.rxjava3.disposables.Disposable;

public class JSCoverage {

    private final List<Disposable> disposables = new ArrayList<>();
    private CDPSession client;
    private boolean enabled;
    private Map<String, String> scriptSources;
    private Map<String, String> scriptURLs;
    private boolean resetOnNavigation;

    private boolean reportAnonymousScripts;

    public JSCoverage(CDPSession client) {
        this.client = client;
        this.enabled = false;
        this.scriptURLs = new HashMap<>();
        this.scriptSources = new HashMap<>();
        this.resetOnNavigation = false;
    }

    public void start(boolean resetOnNavigation, boolean reportAnonymousScripts) {
        Assert.isTrue(!this.enabled, "JSCoverage is already enabled");

        this.resetOnNavigation = resetOnNavigation;
        this.reportAnonymousScripts = reportAnonymousScripts;
        this.enabled = true;
        this.scriptURLs.clear();
        this.scriptSources.clear();
        this.disposables.add(Builder.fromEmitterEvent(this.client, CDPSession.CDPSessionEvent.Debugger_scriptparsed)
                .subscribe((event) -> this.onScriptParsed((ScriptParsedEvent) event)));
        this.disposables
                .add(Builder.fromEmitterEvent(this.client, CDPSession.CDPSessionEvent.Runtime_executionContextsCleared)
                        .subscribe((ignore) -> this.onExecutionContextsCleared()));
        this.client.send("Profiler.enable", null, null, false);
        Map<String, Object> params = new HashMap<>();
        params.put("callCount", false);
        params.put("detailed", true);
        this.client.send("Profiler.startPreciseCoverage", params, null, false);
        this.client.send("Debugger.enable", null, null, false);
        params.clear();
        params.put("skip", true);
        this.client.send("Debugger.setSkipAllPauses", params);

    }

    private void onExecutionContextsCleared() {
        if (!this.resetOnNavigation)
            return;
        this.scriptURLs.clear();
        this.scriptSources.clear();
    }

    private void onScriptParsed(ScriptParsedEvent event) {
        // Ignore puppeteer-injected scripts
        if (ExecutionContext.EVALUATION_SCRIPT_URL.equals(event.getUrl()))
            return;
        // Ignore other anonymous scripts unless the reportAnonymousScripts option is true.
        if (StringKit.isEmpty(event.getUrl()) && !this.reportAnonymousScripts)
            return;
        ForkJoinPool.commonPool().submit(() -> {
            Map<String, Object> params = new HashMap<>();
            params.put("scriptId", event.getScriptId());
            JsonNode response = client.send("Debugger.getScriptSource", params);
            scriptURLs.put(event.getScriptId(), event.getUrl());
            scriptSources.put(event.getScriptId(), response.get("scriptSource").asText());
        });
    }

    public List<CoverageEntry> stop() throws JsonProcessingException {
        Assert.isTrue(this.enabled, "JSCoverage is not enabled");
        this.enabled = false;
        JsonNode result = this.client.send("Profiler.takePreciseCoverage");
        this.client.send("Profiler.stopPreciseCoverage");
        this.client.send("Profiler.disable");
        this.client.send("Debugger.disable");
        this.disposables.forEach(Disposable::dispose);
        List<CoverageEntry> coverage = new ArrayList<>();
        TakePreciseCoverageReturnValue profileResponse = Builder.OBJECTMAPPER.treeToValue(result,
                TakePreciseCoverageReturnValue.class);
        if (CollKit.isEmpty(profileResponse.getResult())) {
            return coverage;
        }
        for (ScriptCoverage entry : profileResponse.getResult()) {
            String url = this.scriptURLs.get(entry.getScriptId());
            if (StringKit.isEmpty(url) && this.reportAnonymousScripts)
                url = "debugger://VM" + entry.getScriptId();
            String text = this.scriptSources.get(entry.getScriptId());
            if (StringKit.isEmpty(url) || StringKit.isEmpty(text))
                continue;
            List<CoverageRange> flattenRanges = new ArrayList<>();
            for (FunctionCoverage func : entry.getFunctions())
                flattenRanges.addAll(func.getRanges());
            List<Range> ranges = Coverage.convertToDisjointRanges(flattenRanges);
            coverage.add(createCoverageEntry(url, ranges, text));
        }
        return coverage;
    }

    private CoverageEntry createCoverageEntry(String url, List<Range> ranges, String text) {
        CoverageEntry coverageEntity = new CoverageEntry();
        coverageEntity.setUrl(url);
        coverageEntity.setRanges(ranges);
        coverageEntity.setText(text);
        return coverageEntity;
    }

}
