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
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.miaixz.lancia.kernel.Coverage;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;

/**
 * Coordinates JavaScript and CSS coverage collection.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpCoverage implements Coverage {

    /**
     * Current JS coverage.
     */
    private final CdpJSCoverage jsCoverage;
    /**
     * Current CSS coverage.
     */
    private final CdpCSSCoverage cssCoverage;

    /**
     * Creates a coverage.
     *
     * @param session protocol session
     */
    public CdpCoverage(CDPSession session) {
        this.jsCoverage = new CdpJSCoverage(session);
        this.cssCoverage = new CdpCSSCoverage(session);
    }

    /**
     * Updates client.
     *
     * @param session protocol session
     */
    public void updateClient(CDPSession session) {
        jsCoverage.updateClient(session);
        cssCoverage.updateClient(session);
    }

    /**
     * Handles start JS coverage.
     *
     * @param options operation options
     */
    public void startJSCoverage(JSCoverageOptions options) {
        jsCoverage.start(options);
    }

    /**
     * Handles start JS coverage.
     */
    public void startJSCoverage() {
        startJSCoverage(new JSCoverageOptions());
    }

    /**
     * Returns the stop JS coverage.
     *
     * @return values
     */
    public List<JSCoverageEntry> stopJSCoverage() {
        return jsCoverage.stopEntries();
    }

    /**
     * Handles start CSS coverage.
     *
     * @param options operation options
     */
    public void startCSSCoverage(CSSCoverageOptions options) {
        cssCoverage.start(options);
    }

    /**
     * Handles start CSS coverage.
     */
    public void startCSSCoverage() {
        startCSSCoverage(new CSSCoverageOptions());
    }

    /**
     * Returns the stop CSS coverage.
     *
     * @return values
     */
    public List<CoverageEntry> stopCSSCoverage() {
        return cssCoverage.stopEntries();
    }

    /**
     * Returns the JS coverage.
     *
     * @return JS coverage value
     */
    public CdpJSCoverage jsCoverage() {
        return jsCoverage;
    }

    /**
     * Returns the CSS coverage.
     *
     * @return CSS coverage value
     */
    public CdpCSSCoverage cssCoverage() {
        return cssCoverage;
    }

    /**
     * Returns the convert to disjoint ranges.
     *
     * @param nestedRanges nested ranges value
     * @return values
     */
    static List<CoverageRange> convertToDisjointRanges(List<ProtocolRange> nestedRanges) {
        List<RangePoint> points = new ArrayList<>();
        for (ProtocolRange range : nestedRanges) {
            points.add(new RangePoint(range.startOffset(), 0, range));
            points.add(new RangePoint(range.endOffset(), 1, range));
        }
        points.sort(
                Comparator.comparingInt(RangePoint::offset)
                        .thenComparing((left, right) -> Integer.compare(right.type(), left.type()))
                        .thenComparing((left, right) -> {
                            int leftLength = left.range().endOffset() - left.range().startOffset();
                            int rightLength = right.range().endOffset() - right.range().startOffset();
                            return left.type() == 0 ? Integer.compare(rightLength, leftLength)
                                    : Integer.compare(leftLength, rightLength);
                        }));
        List<Integer> hitCountStack = new ArrayList<>();
        List<CoverageRange> results = new ArrayList<>();
        int lastOffset = 0;
        for (RangePoint point : points) {
            if (!hitCountStack.isEmpty() && lastOffset < point.offset()
                    && hitCountStack.get(hitCountStack.size() - 1) > 0) {
                if (!results.isEmpty() && results.get(results.size() - 1).end() == lastOffset) {
                    CoverageRange previous = results.remove(results.size() - 1);
                    results.add(new CoverageRange(previous.start(), point.offset()));
                } else {
                    results.add(new CoverageRange(lastOffset, point.offset()));
                }
            }
            lastOffset = point.offset();
            if (point.type() == 0) {
                hitCountStack.add(point.range().count());
            } else if (!hitCountStack.isEmpty()) {
                hitCountStack.remove(hitCountStack.size() - 1);
            }
        }
        return results.stream().filter(range -> range.end() - range.start() > 0).toList();
    }

    /**
     * Carries the CoverageEntry data.
     *
     * @param url    url
     * @param text   text
     * @param ranges ranges
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public record CoverageEntry(String url, String text, List<CoverageRange> ranges) {
    }

    /**
     * Carries the JSCoverageEntry data.
     *
     * @param url               url
     * @param text              text
     * @param ranges            ranges
     * @param rawScriptCoverage raw script coverage
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public record JSCoverageEntry(String url, String text, List<CoverageRange> ranges,
            Map<String, Object> rawScriptCoverage) {
    }

    /**
     * Carries the CoverageRange data.
     *
     * @param start start
     * @param end   end
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public record CoverageRange(int start, int end) {
    }

    /**
     * Carries the ProtocolRange data.
     *
     * @param startOffset start offset
     * @param endOffset   end offset
     * @param count       count
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    record ProtocolRange(int startOffset, int endOffset, int count) {
    }

    /**
     * Carries the RangePoint data.
     *
     * @param offset offset
     * @param type   type
     * @param range  range
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private record RangePoint(int offset, int type, ProtocolRange range) {
    }

    /**
     * Defines options for JS coverage operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class JSCoverageOptions {

        /**
         * Whether reset on navigation is enabled.
         */
        private boolean resetOnNavigation = true;
        /**
         * Whether report anonymous scripts is enabled.
         */
        private boolean reportAnonymousScripts;
        /**
         * Whether include raw script coverage is enabled.
         */
        private boolean includeRawScriptCoverage;
        /**
         * Whether use block coverage is enabled.
         */
        private boolean useBlockCoverage = true;

        /**
         * Creates an instance.
         */
        public JSCoverageOptions() {
            // No initialization required.
        }

        /**
         * Returns whether reset on navigation is enabled.
         *
         * @return {@code true} when the condition matches
         */
        public boolean isResetOnNavigation() {
            return resetOnNavigation;
        }

        /**
         * Updates reset on navigation.
         *
         * @param resetOnNavigation reset on navigation value
         */
        public void setResetOnNavigation(boolean resetOnNavigation) {
            this.resetOnNavigation = resetOnNavigation;
        }

        /**
         * Returns whether report anonymous scripts is enabled.
         *
         * @return {@code true} when the condition matches
         */
        public boolean isReportAnonymousScripts() {
            return reportAnonymousScripts;
        }

        /**
         * Updates report anonymous scripts.
         *
         * @param reportAnonymousScripts report anonymous scripts value
         */
        public void setReportAnonymousScripts(boolean reportAnonymousScripts) {
            this.reportAnonymousScripts = reportAnonymousScripts;
        }

        /**
         * Returns whether include raw script coverage is enabled.
         *
         * @return {@code true} when the condition matches
         */
        public boolean isIncludeRawScriptCoverage() {
            return includeRawScriptCoverage;
        }

        /**
         * Updates include raw script coverage.
         *
         * @param includeRawScriptCoverage include raw script coverage value
         */
        public void setIncludeRawScriptCoverage(boolean includeRawScriptCoverage) {
            this.includeRawScriptCoverage = includeRawScriptCoverage;
        }

        /**
         * Returns whether use block coverage is enabled.
         *
         * @return {@code true} when the condition matches
         */
        public boolean isUseBlockCoverage() {
            return useBlockCoverage;
        }

        /**
         * Updates use block coverage.
         *
         * @param useBlockCoverage use block coverage value
         */
        public void setUseBlockCoverage(boolean useBlockCoverage) {
            this.useBlockCoverage = useBlockCoverage;
        }
    }

    /**
     * Defines options for CSS coverage operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class CSSCoverageOptions {

        /**
         * Whether reset on navigation is enabled.
         */
        private boolean resetOnNavigation = true;

        /**
         * Creates an instance.
         */
        public CSSCoverageOptions() {
            // No initialization required.
        }

        /**
         * Returns whether reset on navigation is enabled.
         *
         * @return {@code true} when the condition matches
         */
        public boolean isResetOnNavigation() {
            return resetOnNavigation;
        }

        /**
         * Updates reset on navigation.
         *
         * @param resetOnNavigation reset on navigation value
         */
        public void setResetOnNavigation(boolean resetOnNavigation) {
            this.resetOnNavigation = resetOnNavigation;
        }
    }

}
