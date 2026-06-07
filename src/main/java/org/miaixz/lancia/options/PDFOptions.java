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
package org.miaixz.lancia.options;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;

/**
 * Defines options for PDF operations.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class PDFOptions {

    /**
     * Default timeout millis.
     */
    public static final long DEFAULT_TIMEOUT_MILLIS = 30_000L;

    /**
     * Default scale.
     */
    public static final double DEFAULT_SCALE = 1.0d;
    /**
     * Default width inches.
     */
    private static final double DEFAULT_WIDTH_INCHES = 8.5d;
    /**
     * Default height inches.
     */
    private static final double DEFAULT_HEIGHT_INCHES = 11.0d;
    /**
     * Shared constant for unit to pixels.
     */
    private static final Map<String, Double> UNIT_TO_PIXELS = Map.of("px", 1.0d, "in", 96.0d, "cm", 37.8d, "mm", 3.78d);
    /**
     * Shared constant for paper formats.
     */
    private static final Map<String, PaperFormatDimensions> PAPER_FORMATS = paperFormatMap();
    /**
     * Current scale.
     */
    private double scale = DEFAULT_SCALE;
    /**
     * Whether header and footer templates are displayed.
     */
    private boolean displayHeaderFooter;
    /**
     * Current header template.
     */
    private String headerTemplate = Normal.EMPTY;
    /**
     * Current footer template.
     */
    private String footerTemplate = Normal.EMPTY;
    /**
     * Whether print background is enabled.
     */
    private boolean printBackground;
    /**
     * Whether landscape is enabled.
     */
    private boolean landscape;
    /**
     * Current page ranges.
     */
    private String pageRanges = Normal.EMPTY;
    /**
     * Current format.
     */
    private String format = "letter";
    /**
     * Current width.
     */
    private Object width;
    /**
     * Current height.
     */
    private Object height;
    /**
     * Whether CSS page size should be preferred.
     */
    private boolean preferCSSPageSize;
    /**
     * Current margin.
     */
    private PDFMargin margin;
    /**
     * Current path.
     */
    private Path path;
    /**
     * Whether omit background is enabled.
     */
    private boolean omitBackground;
    /**
     * Whether tagged is enabled.
     */
    private boolean tagged = true;
    /**
     * Whether outline is enabled.
     */
    private boolean outline;
    /**
     * Current timeout.
     */
    private long timeout = DEFAULT_TIMEOUT_MILLIS;
    /**
     * Whether wait for fonts is enabled.
     */
    private boolean waitForFonts = true;
    /**
     * Whether stream is enabled.
     */
    private boolean stream;

    /**
     * Creates PDF options.
     */
    public PDFOptions() {
        // No initialization required.
    }

    /**
     * Converts this value to protocol parameters.
     *
     * @return protocol parameters
     */
    public Map<String, Object> toMap() {
        ParsedPDFOptions parsed = parse();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("landscape", parsed.landscape());
        result.put("displayHeaderFooter", parsed.displayHeaderFooter());
        result.put("headerTemplate", parsed.headerTemplate());
        result.put("footerTemplate", parsed.footerTemplate());
        result.put("printBackground", parsed.printBackground());
        result.put("scale", parsed.scale());
        result.put("paperWidth", parsed.width());
        result.put("paperHeight", parsed.height());
        result.put("marginTop", parsed.margin().top());
        result.put("marginBottom", parsed.margin().bottom());
        result.put("marginLeft", parsed.margin().left());
        result.put("marginRight", parsed.margin().right());
        result.put("pageRanges", parsed.pageRanges());
        result.put("preferCSSPageSize", parsed.preferCSSPageSize());
        result.put("generateTaggedPDF", parsed.tagged());
        result.put("generateDocumentOutline", parsed.outline());
        if (stream) {
            result.put("transferMode", "ReturnAsStream");
        }
        return result;
    }

    /**
     * Returns the parse.
     *
     * @return parse value
     */
    public ParsedPDFOptions parse() {
        validateScale();
        double paperWidth = DEFAULT_WIDTH_INCHES;
        double paperHeight = DEFAULT_HEIGHT_INCHES;
        if (StringKit.isNotBlank(format)) {
            PaperFormatDimensions dimensions = PAPER_FORMATS.get(format.toLowerCase(Locale.ROOT));
            if (dimensions == null) {
                throw new InternalException("Unknown paper format: " + format);
            }
            paperWidth = dimensions.width();
            paperHeight = dimensions.height();
        } else {
            Double parsedWidth = convertPrintParameterToInches(width);
            Double parsedHeight = convertPrintParameterToInches(height);
            paperWidth = parsedWidth == null ? paperWidth : parsedWidth;
            paperHeight = parsedHeight == null ? paperHeight : parsedHeight;
        }
        PDFMargin actualMargin = margin == null ? new PDFMargin() : margin;
        ParsedPDFMargin parsedMargin = new ParsedPDFMargin(
                zeroIfNull(convertPrintParameterToInches(actualMargin.getTop())),
                zeroIfNull(convertPrintParameterToInches(actualMargin.getBottom())),
                zeroIfNull(convertPrintParameterToInches(actualMargin.getLeft())),
                zeroIfNull(convertPrintParameterToInches(actualMargin.getRight())));
        return new ParsedPDFOptions(scale, displayHeaderFooter, blankToEmpty(headerTemplate),
                blankToEmpty(footerTemplate), printBackground, landscape, blankToEmpty(pageRanges), paperWidth,
                paperHeight, preferCSSPageSize, parsedMargin, omitBackground, outline || tagged, outline, timeout,
                waitForFonts);
    }

    /**
     * Returns the paper formats.
     *
     * @return mapped values
     */
    public static Map<String, PaperFormatDimensions> paperFormats() {
        return PAPER_FORMATS;
    }

    /**
     * Converts print parameter to inches.
     *
     * @param parameter parameter
     * @return converted print parameter to inches
     */
    public static Double convertPrintParameterToInches(Object parameter) {
        return convertPrintParameter(parameter, "in");
    }

    /**
     * Converts print parameter to centimeters.
     *
     * @param parameter parameter
     * @return converted print parameter to centimeters
     */
    public static Double convertPrintParameterToCentimeters(Object parameter) {
        return convertPrintParameter(parameter, "cm");
    }

    /**
     * Returns the scale.
     *
     * @return scale
     */
    public double getScale() {
        return scale;
    }

    /**
     * Updates scale.
     *
     * @param scale scale value
     */
    public void setScale(double scale) {
        this.scale = scale;
    }

    /**
     * Returns whether display header footer is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isDisplayHeaderFooter() {
        return displayHeaderFooter;
    }

    /**
     * Updates display header footer.
     *
     * @param displayHeaderFooter display header footer value
     */
    public void setDisplayHeaderFooter(boolean displayHeaderFooter) {
        this.displayHeaderFooter = displayHeaderFooter;
    }

    /**
     * Returns the header template.
     *
     * @return header template
     */
    public String getHeaderTemplate() {
        return headerTemplate;
    }

    /**
     * Updates header template.
     *
     * @param headerTemplate header template value
     */
    public void setHeaderTemplate(String headerTemplate) {
        this.headerTemplate = headerTemplate;
    }

    /**
     * Returns the footer template.
     *
     * @return footer template
     */
    public String getFooterTemplate() {
        return footerTemplate;
    }

    /**
     * Updates footer template.
     *
     * @param footerTemplate footer template value
     */
    public void setFooterTemplate(String footerTemplate) {
        this.footerTemplate = footerTemplate;
    }

    /**
     * Returns whether print background is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isPrintBackground() {
        return printBackground;
    }

    /**
     * Updates print background.
     *
     * @param printBackground print background value
     */
    public void setPrintBackground(boolean printBackground) {
        this.printBackground = printBackground;
    }

    /**
     * Returns whether landscape is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isLandscape() {
        return landscape;
    }

    /**
     * Updates landscape.
     *
     * @param landscape landscape value
     */
    public void setLandscape(boolean landscape) {
        this.landscape = landscape;
    }

    /**
     * Returns the page ranges.
     *
     * @return page ranges
     */
    public String getPageRanges() {
        return pageRanges;
    }

    /**
     * Updates page ranges.
     *
     * @param pageRanges page ranges value
     */
    public void setPageRanges(String pageRanges) {
        this.pageRanges = pageRanges;
    }

    /**
     * Returns the format.
     *
     * @return format
     */
    public String getFormat() {
        return format;
    }

    /**
     * Updates format.
     *
     * @param format format value
     */
    public void setFormat(String format) {
        this.format = StringKit.isBlank(format) ? null : format;
    }

    /**
     * Returns the width.
     *
     * @return width
     */
    public Object getWidth() {
        return width;
    }

    /**
     * Updates width.
     *
     * @param width width value
     */
    public void setWidth(Object width) {
        this.width = width;
    }

    /**
     * Returns the height.
     *
     * @return height
     */
    public Object getHeight() {
        return height;
    }

    /**
     * Updates height.
     *
     * @param height height value
     */
    public void setHeight(Object height) {
        this.height = height;
    }

    /**
     * Returns whether prefer CSS page size is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isPreferCSSPageSize() {
        return preferCSSPageSize;
    }

    /**
     * Updates prefer CSS page size.
     *
     * @param preferCSSPageSize prefer css page size
     */
    public void setPreferCSSPageSize(boolean preferCSSPageSize) {
        this.preferCSSPageSize = preferCSSPageSize;
    }

    /**
     * Returns the margin.
     *
     * @return margin
     */
    public PDFMargin getMargin() {
        return margin;
    }

    /**
     * Updates margin.
     *
     * @param margin margin value
     */
    public void setMargin(PDFMargin margin) {
        this.margin = margin;
    }

    /**
     * Returns the path.
     *
     * @return path
     */
    public Path getPath() {
        return path;
    }

    /**
     * Updates path.
     *
     * @param path file path
     */
    public void setPath(Path path) {
        this.path = path;
    }

    /**
     * Updates path.
     *
     * @param path file path
     */
    public void setPath(String path) {
        this.path = StringKit.isBlank(path) ? null : Path.of(path);
    }

    /**
     * Returns whether omit background is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isOmitBackground() {
        return omitBackground;
    }

    /**
     * Updates omit background.
     *
     * @param omitBackground omit background value
     */
    public void setOmitBackground(boolean omitBackground) {
        this.omitBackground = omitBackground;
    }

    /**
     * Returns whether tagged is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isTagged() {
        return tagged;
    }

    /**
     * Updates tagged.
     *
     * @param tagged tagged value
     */
    public void setTagged(boolean tagged) {
        this.tagged = tagged;
    }

    /**
     * Returns whether outline is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isOutline() {
        return outline;
    }

    /**
     * Updates outline.
     *
     * @param outline outline value
     */
    public void setOutline(boolean outline) {
        this.outline = outline;
    }

    /**
     * Returns the timeout.
     *
     * @return timeout
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * Updates timeout.
     *
     * @param timeout timeout value
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * Returns whether wait for fonts is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isWaitForFonts() {
        return waitForFonts;
    }

    /**
     * Updates wait for fonts.
     *
     * @param waitForFonts wait for fonts value
     */
    public void setWaitForFonts(boolean waitForFonts) {
        this.waitForFonts = waitForFonts;
    }

    /**
     * Returns whether stream is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isStream() {
        return stream;
    }

    /**
     * Updates stream.
     *
     * @param stream stream value
     */
    public void setStream(boolean stream) {
        this.stream = stream;
    }

    /**
     * Validates scale.
     */
    private void validateScale() {
        if (scale < 0.1d || scale > 2.0d) {
            throw new InternalException("PDF scale must be between 0.1 and 2.");
        }
    }

    /**
     * Returns the blank to empty.
     *
     * @param value to use
     * @return blank to empty value
     */
    private static String blankToEmpty(String value) {
        return StringKit.isBlank(value) ? Normal.EMPTY : value;
    }

    /**
     * Returns the zero if null.
     *
     * @param value to use
     * @return zero if null value
     */
    private static double zeroIfNull(Double value) {
        return value == null ? 0.0d : value;
    }

    /**
     * Returns the convert print parameter.
     *
     * @param parameter  parameter value
     * @param lengthUnit length unit value
     * @return convert print parameter value
     */
    private static Double convertPrintParameter(Object parameter, String lengthUnit) {
        if (parameter == null) {
            return null;
        }
        double pixels;
        if (parameter instanceof Number number) {
            pixels = number.doubleValue();
        } else if (parameter instanceof String text) {
            pixels = parseLengthText(text);
        } else {
            throw new InternalException("page.pdf() Cannot handle parameter type: " + parameter.getClass().getName());
        }
        return pixels / UNIT_TO_PIXELS.get(lengthUnit);
    }

    /**
     * Parses length text.
     *
     * @param text text to use
     * @return parse length text value
     */
    private static double parseLengthText(String text) {
        String actualText = Assert.notBlank(text, "text").trim();
        String unit = actualText.length() >= 2 ? actualText.substring(actualText.length() - 2).toLowerCase(Locale.ROOT)
                : "px";
        String valueText;
        if (UNIT_TO_PIXELS.containsKey(unit)) {
            valueText = actualText.substring(0, actualText.length() - 2).trim();
        } else {
            unit = "px";
            valueText = actualText;
        }
        try {
            return Double.parseDouble(valueText) * UNIT_TO_PIXELS.get(unit);
        } catch (NumberFormatException ex) {
            throw new InternalException("Failed to parse parameter value: " + text, ex);
        }
    }

    /**
     * Returns the paper format map.
     *
     * @return mapped values
     */
    private static Map<String, PaperFormatDimensions> paperFormatMap() {
        Map<String, PaperFormatDimensions> formats = new LinkedHashMap<>();
        formats.put("letter", new PaperFormatDimensions(8.5d, 11.0d));
        formats.put("legal", new PaperFormatDimensions(8.5d, 14.0d));
        formats.put("tabloid", new PaperFormatDimensions(11.0d, 17.0d));
        formats.put("ledger", new PaperFormatDimensions(17.0d, 11.0d));
        formats.put("a0", new PaperFormatDimensions(33.1102d, 46.811d));
        formats.put("a1", new PaperFormatDimensions(23.3858d, 33.1102d));
        formats.put("a2", new PaperFormatDimensions(16.5354d, 23.3858d));
        formats.put("a3", new PaperFormatDimensions(11.6929d, 16.5354d));
        formats.put("a4", new PaperFormatDimensions(8.2677d, 11.6929d));
        formats.put("a5", new PaperFormatDimensions(5.8268d, 8.2677d));
        formats.put("a6", new PaperFormatDimensions(4.1339d, 5.8268d));
        return Map.copyOf(formats);
    }

    /**
     * Represents PDF margin.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static class PDFMargin {

        /**
         * Current top.
         */
        private Object top;
        /**
         * Current bottom.
         */
        private Object bottom;
        /**
         * Current left.
         */
        private Object left;
        /**
         * Current right.
         */
        private Object right;

        /**
         * Creates an instance.
         */
        public PDFMargin() {
            // No initialization required.
        }

        /**
         * Returns the top.
         *
         * @return top
         */
        public Object getTop() {
            return top;
        }

        /**
         * Updates top.
         *
         * @param top top value
         */
        public void setTop(Object top) {
            this.top = top;
        }

        /**
         * Returns the bottom.
         *
         * @return bottom
         */
        public Object getBottom() {
            return bottom;
        }

        /**
         * Updates bottom.
         *
         * @param bottom bottom value
         */
        public void setBottom(Object bottom) {
            this.bottom = bottom;
        }

        /**
         * Returns the left.
         *
         * @return left
         */
        public Object getLeft() {
            return left;
        }

        /**
         * Updates left.
         *
         * @param left left value
         */
        public void setLeft(Object left) {
            this.left = left;
        }

        /**
         * Returns the right.
         *
         * @return right
         */
        public Object getRight() {
            return right;
        }

        /**
         * Updates right.
         *
         * @param right right value
         */
        public void setRight(Object right) {
            this.right = right;
        }
    }

    /**
     * Represents paper format dimensions.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class PaperFormatDimensions {

        /**
         * Current width.
         */
        private final double width;
        /**
         * Current height.
         */
        private final double height;

        /**
         * Creates an instance.
         *
         * @param width  width value
         * @param height height value
         */
        public PaperFormatDimensions(double width, double height) {
            this.width = width;
            this.height = height;
        }

        /**
         * Returns the width.
         *
         * @return width value
         */
        public double width() {
            return width;
        }

        /**
         * Returns the height.
         *
         * @return height value
         */
        public double height() {
            return height;
        }
    }

    /**
     * Represents parsed PDF margin.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class ParsedPDFMargin {

        /**
         * Current top.
         */
        private final double top;
        /**
         * Current bottom.
         */
        private final double bottom;
        /**
         * Current left.
         */
        private final double left;
        /**
         * Current right.
         */
        private final double right;

        /**
         * Creates an instance.
         *
         * @param top    top value
         * @param bottom bottom value
         * @param left   left value
         * @param right  right value
         */
        public ParsedPDFMargin(double top, double bottom, double left, double right) {
            this.top = top;
            this.bottom = bottom;
            this.left = left;
            this.right = right;
        }

        /**
         * Converts this value to p.
         *
         * @return p
         */
        public double top() {
            return top;
        }

        /**
         * Returns the bottom.
         *
         * @return bottom value
         */
        public double bottom() {
            return bottom;
        }

        /**
         * Returns the left.
         *
         * @return left value
         */
        public double left() {
            return left;
        }

        /**
         * Returns the right.
         *
         * @return right value
         */
        public double right() {
            return right;
        }
    }

    /**
     * Defines options for parsed PDF operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class ParsedPDFOptions {

        /**
         * Current scale.
         */
        private final double scale;
        /**
         * Whether header and footer templates are displayed.
         */
        private final boolean displayHeaderFooter;
        /**
         * Current header template.
         */
        private final String headerTemplate;
        /**
         * Current footer template.
         */
        private final String footerTemplate;
        /**
         * Whether print background is enabled.
         */
        private final boolean printBackground;
        /**
         * Whether landscape is enabled.
         */
        private final boolean landscape;
        /**
         * Current page ranges.
         */
        private final String pageRanges;
        /**
         * Current width.
         */
        private final double width;
        /**
         * Current height.
         */
        private final double height;
        /**
         * Whether CSS page size should be preferred.
         */
        private final boolean preferCSSPageSize;
        /**
         * Current margin.
         */
        private final ParsedPDFMargin margin;
        /**
         * Whether omit background is enabled.
         */
        private final boolean omitBackground;
        /**
         * Whether tagged is enabled.
         */
        private final boolean tagged;
        /**
         * Whether outline is enabled.
         */
        private final boolean outline;
        /**
         * Current timeout.
         */
        private final long timeout;
        /**
         * Whether wait for fonts is enabled.
         */
        private final boolean waitForFonts;

        /**
         * Creates an instance.
         *
         * @param scale               scale value
         * @param displayHeaderFooter display header footer value
         * @param headerTemplate      header template value
         * @param footerTemplate      footer template value
         * @param printBackground     print background value
         * @param landscape           landscape value
         * @param pageRanges          page ranges value
         * @param width               width value
         * @param height              height value
         * @param preferCSSPageSize   prefer CSS page size value
         * @param margin              margin value
         * @param omitBackground      omit background value
         * @param tagged              tagged value
         * @param outline             outline value
         * @param timeout             timeout value
         * @param waitForFonts        wait for fonts value
         */
        public ParsedPDFOptions(double scale, boolean displayHeaderFooter, String headerTemplate, String footerTemplate,
                boolean printBackground, boolean landscape, String pageRanges, double width, double height,
                boolean preferCSSPageSize, ParsedPDFMargin margin, boolean omitBackground, boolean tagged,
                boolean outline, long timeout, boolean waitForFonts) {
            this.scale = scale;
            this.displayHeaderFooter = displayHeaderFooter;
            this.headerTemplate = headerTemplate;
            this.footerTemplate = footerTemplate;
            this.printBackground = printBackground;
            this.landscape = landscape;
            this.pageRanges = pageRanges;
            this.width = width;
            this.height = height;
            this.preferCSSPageSize = preferCSSPageSize;
            this.margin = margin == null ? new ParsedPDFMargin(0.0d, 0.0d, 0.0d, 0.0d) : margin;
            this.omitBackground = omitBackground;
            this.tagged = tagged;
            this.outline = outline;
            this.timeout = timeout;
            this.waitForFonts = waitForFonts;
        }

        /**
         * Returns the scale.
         *
         * @return scale value
         */
        public double scale() {
            return scale;
        }

        /**
         * Returns the display header footer.
         *
         * @return {@code true} when the condition matches
         */
        public boolean displayHeaderFooter() {
            return displayHeaderFooter;
        }

        /**
         * Returns the header template.
         *
         * @return header template value
         */
        public String headerTemplate() {
            return headerTemplate;
        }

        /**
         * Returns the footer template.
         *
         * @return footer template value
         */
        public String footerTemplate() {
            return footerTemplate;
        }

        /**
         * Returns the print background.
         *
         * @return {@code true} when the condition matches
         */
        public boolean printBackground() {
            return printBackground;
        }

        /**
         * Returns the landscape.
         *
         * @return {@code true} when the condition matches
         */
        public boolean landscape() {
            return landscape;
        }

        /**
         * Returns the page ranges.
         *
         * @return page ranges value
         */
        public String pageRanges() {
            return pageRanges;
        }

        /**
         * Returns the width.
         *
         * @return width value
         */
        public double width() {
            return width;
        }

        /**
         * Returns the height.
         *
         * @return height value
         */
        public double height() {
            return height;
        }

        /**
         * Returns the prefer CSS page size.
         *
         * @return {@code true} when the condition matches
         */
        public boolean preferCSSPageSize() {
            return preferCSSPageSize;
        }

        /**
         * Returns the margin.
         *
         * @return margin value
         */
        public ParsedPDFMargin margin() {
            return margin;
        }

        /**
         * Returns the omit background.
         *
         * @return {@code true} when the condition matches
         */
        public boolean omitBackground() {
            return omitBackground;
        }

        /**
         * Returns the tagged.
         *
         * @return {@code true} when the condition matches
         */
        public boolean tagged() {
            return tagged;
        }

        /**
         * Returns the outline.
         *
         * @return {@code true} when the condition matches
         */
        public boolean outline() {
            return outline;
        }

        /**
         * Returns the timeout.
         *
         * @return timeout value
         */
        public long timeout() {
            return timeout;
        }

        /**
         * Waits for fonts.
         *
         * @return wait for fonts value
         */
        public boolean waitForFonts() {
            return waitForFonts;
        }
    }

}
