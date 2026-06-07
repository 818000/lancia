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
package org.miaixz.lancia.browser.bundle;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.LongConsumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;

/**
 * Represents a progress bar.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class ProgressBar {

    /**
     * Current active progress.
     */
    private static ProgressBar activeProgress;
    /**
     * Shared constant for bar width.
     */
    private static final int BAR_WIDTH = 20;
    /**
     * Shared constant for render interval millis.
     */
    private static final long RENDER_INTERVAL_MILLIS = 100L;
    /**
     * Current stream.
     */
    private final ProgressStream stream;
    /**
     * Current total rows.
     */
    private int totalRows;
    /**
     * Current done count.
     */
    private int doneCount;
    /**
     * Registered rows values.
     */
    private final List<ProgressRow> rows = new ArrayList<>();
    /**
     * Current last render time.
     */
    private long lastRenderTime;
    /**
     * Whether all done is enabled.
     */
    private boolean allDone;

    /**
     * Creates a progress bar.
     *
     * @param stream stream
     */
    private ProgressBar(ProgressStream stream) {
        this.stream = Assert.notNull(stream, "stream");
    }

    /**
     * Creates bar ticker.
     *
     * @param title title
     * @param total total
     * @return created bar ticker
     */
    public static LongConsumer createBarTicker(String title, long total) {
        return createBarTicker(title, total, ProgressStream.systemErr());
    }

    /**
     * Creates bar ticker.
     *
     * @param title  title
     * @param total  total
     * @param stream stream
     * @return created bar ticker
     */
    public static synchronized LongConsumer createBarTicker(String title, long total, ProgressStream stream) {
        ProgressStream actualStream = Assert.notNull(stream, "stream");
        if (activeProgress == null || !activeProgress.stream.sameTarget(actualStream)) {
            activeProgress = new ProgressBar(actualStream);
        }
        return activeProgress.addBar(title, total);
    }

    /**
     * Returns the claim row.
     *
     * @return claim row value
     */
    private int claimRow() {
        int row = totalRows++;
        if (row > 0 && stream.isTty()) {
            stream.write(Symbol.LF);
        }
        return row;
    }

    /**
     * Returns the add bar.
     *
     * @param title title value
     * @param total total value
     * @return add bar value
     */
    private LongConsumer addBar(String title, long total) {
        int row = claimRow();
        rows.add(new ProgressRow(title, Math.max(0L, total)));
        return new ProgressTicker(this, row);
    }

    /**
     * Handles tick.
     *
     * @param rowIndex row index value
     * @param delta    delta value
     */
    private synchronized void tick(int rowIndex, long delta) {
        ProgressRow row = rows.get(rowIndex);
        if (row.startTime == 0L) {
            row.startTime = System.currentTimeMillis();
        }
        row.downloaded += delta;

        boolean done = row.downloaded >= row.total;
        if (done && !row.doneNotified) {
            row.doneNotified = true;
            onBarDone();
        }
        long now = System.currentTimeMillis();
        if (done || now - lastRenderTime >= RENDER_INTERVAL_MILLIS) {
            lastRenderTime = now;
            renderAll();
        }
    }

    /**
     * Returns the compute rendered.
     *
     * @param rowIndex row index value
     * @return compute rendered value
     */
    private String computeRendered(int rowIndex) {
        ProgressRow row = rows.get(rowIndex);
        double ratio = row.total <= 0L ? 1D : Math.min(1D, Math.max(0D, (double) row.downloaded / row.total));
        int percent = (int) Math.round(ratio * 100D);
        long elapsedMs = row.startTime > 0L ? System.currentTimeMillis() - row.startTime : 0L;
        double rate = elapsedMs > 0L ? (double) row.downloaded / elapsedMs : 0D;
        double etaSec = rate > 0D ? (row.total - row.downloaded) / rate / 1000D : 0D;
        int completeCount = (int) Math.round(BAR_WIDTH * ratio);
        String bar = Symbol.EQUAL.repeat(completeCount) + Symbol.SPACE.repeat(BAR_WIDTH - completeCount);
        String status = ratio >= 1D ? "unpacking" : percent + "% " + String.format(Locale.ROOT, "%.1fs", etaSec);
        return row.title + " [" + bar + "] " + status + Symbol.SPACE;
    }

    /**
     * Handles render all.
     */
    private void renderAll() {
        if (!stream.isTty() || totalRows == 0) {
            return;
        }
        if (totalRows > 1) {
            stream.moveCursor(0, -(totalRows - 1));
        }
        for (int rowIndex = 0; rowIndex < totalRows; rowIndex++) {
            ProgressRow row = rows.get(rowIndex);
            String rendered = computeRendered(rowIndex);
            if (!Objects.equals(rendered, row.lastRendered)) {
                stream.cursorTo(0);
                stream.write(rendered);
                stream.clearLine(1);
                row.lastRendered = rendered;
            }
            if (rowIndex < totalRows - 1) {
                stream.moveCursor(0, 1);
            }
        }
        stream.cursorTo(0);
        if (allDone) {
            activeProgress = null;
            stream.write(Symbol.LF);
        }
    }

    /**
     * Handles on bar done.
     */
    private void onBarDone() {
        doneCount++;
        if (doneCount >= totalRows && totalRows > 0) {
            allDone = true;
        }
    }

    /**
     * Represents progress ticker.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class ProgressTicker implements LongConsumer {

        /**
         * Current progress.
         */
        private final ProgressBar progress;
        /**
         * Current row index.
         */
        private final int rowIndex;

        /**
         * Creates an instance.
         *
         * @param progress progress value
         * @param rowIndex row index value
         */
        private ProgressTicker(ProgressBar progress, int rowIndex) {
            this.progress = progress;
            this.rowIndex = rowIndex;
        }

        /**
         * Handles accept.
         *
         * @param value to use
         */
        @Override
        public void accept(long value) {
            progress.tick(rowIndex, value);
        }
    }

    /**
     * Represents progress row.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class ProgressRow {

        /**
         * Current title.
         */
        private final String title;
        /**
         * Current total.
         */
        private final long total;
        /**
         * Current downloaded.
         */
        private long downloaded;
        /**
         * Current start time.
         */
        private long startTime;
        /**
         * Current last rendered.
         */
        private String lastRendered = Normal.EMPTY;
        /**
         * Whether done notified is enabled.
         */
        private boolean doneNotified;

        /**
         * Creates an instance.
         *
         * @param title title value
         * @param total total value
         */
        private ProgressRow(String title, long total) {
            this.title = title == null ? Normal.EMPTY : title;
            this.total = total;
        }
    }

    /**
     * Defines the progress stream contract.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public interface ProgressStream {

        /**
         * Returns the system err.
         *
         * @return system err value
         */
        static ProgressStream systemErr() {
            return new PrintProgressStream(System.err);
        }

        /**
         * Returns whether tty is enabled.
         *
         * @return {@code true} when the condition matches
         */
        boolean isTty();

        /**
         * Handles write.
         *
         * @param value to use
         */
        void write(String value);

        /**
         * Handles move cursor.
         *
         * @param columns columns value
         * @param rows    rows value
         */
        void moveCursor(int columns, int rows);

        /**
         * Handles cursor to.
         *
         * @param column column value
         */
        void cursorTo(int column);

        /**
         * Handles clear line.
         *
         * @param direction direction value
         */
        void clearLine(int direction);

        /**
         * Returns the same target.
         *
         * @param other other value
         * @return {@code true} when the condition matches
         */
        default boolean sameTarget(ProgressStream other) {
            return this == other;
        }
    }

    /**
     * Represents print progress stream.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class PrintProgressStream implements ProgressStream {

        /**
         * Current delegate.
         */
        private final PrintStream delegate;

        /**
         * Creates an instance.
         *
         * @param delegate delegate value
         */
        private PrintProgressStream(PrintStream delegate) {
            this.delegate = Assert.notNull(delegate, "delegate");
        }

        /**
         * Returns whether tty is enabled.
         *
         * @return {@code true} when the condition matches
         */
        @Override
        public boolean isTty() {
            return System.console() != null && delegate == System.err;
        }

        /**
         * Handles write.
         *
         * @param value to use
         */
        @Override
        public void write(String value) {
            delegate.print(value);
        }

        /**
         * Handles move cursor.
         *
         * @param columns columns value
         * @param rows    rows value
         */
        @Override
        public void moveCursor(int columns, int rows) {
            if (rows < 0) {
                delegate.print("\u001B[" + -rows + "A");
            } else if (rows > 0) {
                delegate.print("\u001B[" + rows + "B");
            }
            if (columns < 0) {
                delegate.print("\u001B[" + -columns + "D");
            } else if (columns > 0) {
                delegate.print("\u001B[" + columns + "C");
            }
        }

        /**
         * Handles cursor to.
         *
         * @param column column value
         */
        @Override
        public void cursorTo(int column) {
            delegate.print(Symbol.CR);
            if (column > 0) {
                delegate.print("\u001B[" + column + "C");
            }
        }

        /**
         * Handles clear line.
         *
         * @param direction direction value
         */
        @Override
        public void clearLine(int direction) {
            if (direction >= 0) {
                delegate.print("\u001B[K");
            }
        }

        /**
         * Returns the same target.
         *
         * @param other other value
         * @return {@code true} when the condition matches
         */
        @Override
        public boolean sameTarget(ProgressStream other) {
            return other instanceof PrintProgressStream that && that.delegate == delegate;
        }
    }

}
