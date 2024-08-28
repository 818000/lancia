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
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.miaixz.bus.core.xyz.CollKit;
import org.miaixz.lancia.nimble.css.Point;
import org.miaixz.lancia.nimble.css.Range;
import org.miaixz.lancia.nimble.profiler.CoverageEntry;
import org.miaixz.lancia.nimble.profiler.CoverageRange;
import org.miaixz.lancia.socket.CDPSession;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Coverage gathers information about parts of JavaScript and CSS that were used by the page.
 */
public class Coverage {

    private CSSCoverage cssCoverage;

    private JSCoverage jsCoverage;

    public Coverage(CDPSession client) {
        this.cssCoverage = new CSSCoverage(client);
        this.jsCoverage = new JSCoverage(client);
    }

    public static List<Range> convertToDisjointRanges(List<CoverageRange> nestedRanges) {
        List<Point> points = new ArrayList<>();
        if (CollKit.isNotEmpty(nestedRanges)) {
            for (CoverageRange range : nestedRanges) {
                points.add(createPoint(range.getStartOffset(), 0, range));
                points.add(createPoint(range.getStartOffset(), 1, range));
            }
        }
        // Sort points to form a valid parenthesis sequence.
        points.sort((a, b) -> {

            // Sort with increasing offsets.
            if (a.getOffset() != b.getOffset())
                return a.getOffset() - b.getOffset();
            // All "end" points should go before "start" points.
            if (a.getType() != b.getType())
                return b.getType() - a.getType();
            int aLength = a.getRange().getEndOffset() - a.getRange().getStartOffset();
            int bLength = b.getRange().getEndOffset() - b.getRange().getStartOffset();
            // For two "start" points, the one with longer range goes first.
            if (a.getType() == 0)
                return bLength - aLength;
            // For two "end" points, the one with shorter range goes first.
            return aLength - bLength;
        });

        LinkedList<Integer> hitCountStack = new LinkedList<>();

        List<Range> results = new ArrayList<>();
        int lastOffset = 0;
        // Run scanning line to intersect all ranges.
        for (Point point : points) {
            if (hitCountStack.size() > 0 && lastOffset < point.getOffset()
                    && hitCountStack.get(hitCountStack.size() - 1) > 0) {
                Range lastResult = results.size() > 0 ? results.get(results.size() - 1) : null;
                if (lastResult != null && lastResult.getEnd() == lastOffset)
                    lastResult.setEnd(point.getOffset());
                else
                    results.add(createRange(lastOffset, point.getOffset()));
            }
            lastOffset = point.getOffset();
            if (point.getType() == 0)
                hitCountStack.addLast(point.getRange().getCount());
            else
                hitCountStack.poll();
        }
        // Filter out empty ranges.
        return results.stream().filter(range -> range.getEnd() - range.getStart() > 1).collect(Collectors.toList());
    }

    private static Point createPoint(int startOffset, int type, CoverageRange range) {
        return new Point(startOffset, type, range);
    }

    private static Range createRange(int start, int end) {
        return new Range(start, end);
    }

    public void startJSCoverage() {
        this.jsCoverage.start(true, false);
    }

    public void startJSCoverage(boolean resetOnNavigation, boolean reportAnonymousScripts) {
        this.jsCoverage.start(resetOnNavigation, reportAnonymousScripts);
    }

    public List<CoverageEntry> stopJSCoverage() throws JsonProcessingException {
        return this.jsCoverage.stop();
    }

    public void startCSSCoverage() {
        this.cssCoverage.start(true);
    }

    public void startCSSCoverage(boolean resetOnNavigation) {
        this.cssCoverage.start(resetOnNavigation);
    }

    public List<CoverageEntry> stopCSSCoverage() {
        return this.cssCoverage.stop();
    }

}
