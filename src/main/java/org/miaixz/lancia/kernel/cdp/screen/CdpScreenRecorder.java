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
package org.miaixz.lancia.kernel.cdp.screen;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.codec.binary.Base64;
import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.kernel.Recorder;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.nimble.screen.ScreenshotClip;
import org.miaixz.lancia.options.ScreencastOptions;
import org.miaixz.lancia.runtime.ResourceLimits;

/**
 * CDP screen recorder that streams page frames into FFmpeg.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpScreenRecorder implements Recorder {

    /**
     * Default CRF value used by Puppeteer.
     */
    public static final int CRF_VALUE = 30;

    /**
     * Default recording frame rate.
     */
    public static final int DEFAULT_FPS = 30;

    /**
     * Recorder page.
     */
    private final Page page;

    /**
     * Recorder options.
     */
    private final ScreencastOptions options;

    /**
     * Immutable FFmpeg command used to encode frames.
     */
    private final List<String> command;

    /**
     * FFmpeg process.
     */
    private final Process process;

    /**
     * FFmpeg stderr drainer.
     */
    private final Thread stderrDrainer;

    /**
     * Bounded stderr diagnostic bytes.
     */
    private final byte[] stderrBytes;

    /**
     * Stderr ring buffer lock.
     */
    private final Object stderrLock = new Object();

    /**
     * Stderr ring buffer start.
     */
    private int stderrStart;

    /**
     * Stderr ring buffer size.
     */
    private int stderrSize;

    /**
     * Whether the recorder has stopped.
     */
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    /**
     * Creates and starts a screen recorder.
     *
     * @param page    page being recorded
     * @param width   output width
     * @param height  output height
     * @param options recorder options
     */
    public CdpScreenRecorder(Page page, int width, int height, ScreencastOptions options) {
        this(page, width, height, options, ProcessBuilderProcessFactory.INSTANCE);
    }

    /**
     * Starts a screen recorder.
     *
     * @param page    page being recorded
     * @param width   output width
     * @param height  output height
     * @param options recorder options
     * @return screen recorder
     */
    public static CdpScreenRecorder start(Page page, int width, int height, ScreencastOptions options) {
        return new CdpScreenRecorder(page, width, height, options);
    }

    /**
     * Creates and starts a screen recorder with a custom process factory.
     *
     * @param page           page being recorded
     * @param width          output width
     * @param height         output height
     * @param options        recorder options
     * @param processFactory process factory
     */
    CdpScreenRecorder(Page page, int width, int height, ScreencastOptions options, ProcessFactory processFactory) {
        this.page = page;
        this.options = options == null ? new ScreencastOptions().normalized() : options.normalized();
        this.command = buildCommand(width, height, this.options);
        ensureOutputDirectory(this.options);
        this.process = Assert.notNull(processFactory, "processFactory").start(command);
        this.stderrBytes = new byte[(int) Math
                .min(Integer.MAX_VALUE, Math.max(0L, ResourceLimits.defaults().getMaxProcessOutputBytes()))];
        this.stderrDrainer = startStderrDrainer();
    }

    /**
     * Returns the page being recorded.
     *
     * @return page being recorded
     */
    public Page page() {
        return page;
    }

    /**
     * Returns recorder options.
     *
     * @return recorder options
     */
    public ScreencastOptions options() {
        return options;
    }

    /**
     * Returns the final FFmpeg command.
     *
     * @return immutable FFmpeg command
     */
    public List<String> command() {
        return command;
    }

    /**
     * Returns the encoded video stream.
     *
     * @return FFmpeg stdout stream
     */
    public InputStream stream() {
        return process.getInputStream();
    }

    /**
     * Writes one PNG frame to FFmpeg stdin.
     *
     * @param frame PNG frame bytes
     */
    public synchronized void writeFrame(byte[] frame) {
        if (stopped.get()) {
            throw new InternalException("CdpScreenRecorder is already stopped.");
        }
        try {
            OutputStream stdin = process.getOutputStream();
            stdin.write(Assert.notNull(frame, "frame"));
            stdin.flush();
        } catch (IOException ex) {
            throw new InternalException("Failed to write screen recorder frame.", ex);
        }
    }

    /**
     * Writes one screencast payload frame to FFmpeg stdin.
     *
     * @param payload screencast payload
     */
    public void writeFrame(CdpPayload payload) {
        CdpPayload actualPayload = Assert.notNull(payload, "payload");
        writeFrame(Base64.decode(actualPayload.get("data").asText()));
    }

    /**
     * Stops the recorder and waits for FFmpeg to exit.
     */
    public void stop() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }
        IOException closeError = null;
        try {
            process.getOutputStream().close();
        } catch (IOException ex) {
            closeError = ex;
        }
        try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    throw new InternalException("FFmpeg process did not exit. stderr: " + stderrText());
                }
            }
            stderrDrainer.join(2_000L);
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new InternalException(
                        "FFmpeg process exited with code " + exitCode + ". stderr: " + stderrText());
            }
            if (closeError != null) {
                throw new InternalException("Failed to close screen recorder input.", closeError);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new InternalException("Interrupted while stopping screen recorder.", ex);
        }
    }

    /**
     * Stops the recorder when the lifecycle is closed.
     */
    @Override
    public void close() {
        stop();
    }

    /**
     * Returns whether the recorder has stopped.
     *
     * @return {@code true} when the condition matches
     */
    public boolean stopped() {
        return stopped.get();
    }

    /**
     * Builds the FFmpeg command.
     *
     * @param width   output width
     * @param height  output height
     * @param options recorder options
     * @return immutable command
     */
    public static List<String> buildCommand(int width, int height, ScreencastOptions options) {
        ScreencastOptions actualOptions = options == null ? new ScreencastOptions().normalized() : options.normalized();
        List<String> filters = new ArrayList<>();
        filters.add("crop='min(" + width + ",iw):min(" + height + ",ih):0:0'");
        filters.add("pad=" + width + Symbol.COLON + height + ":0:0");
        if (actualOptions.getSpeed() != null) {
            filters.add("setpts=" + (1.0D / actualOptions.getSpeed()) + "*PTS");
        }
        if (actualOptions.getCrop() != null) {
            ScreenshotClip crop = actualOptions.getCrop();
            filters.add(
                    "crop=" + crop.getWidth() + Symbol.COLON + crop.getHeight() + Symbol.COLON + crop.getX()
                            + Symbol.COLON + crop.getY());
        }
        if (actualOptions.getScale() != null) {
            filters.add("scale=iw*" + actualOptions.getScale() + ":-1:flags=lanczos");
        }

        List<String> formatArgs = new ArrayList<>(formatArgs(actualOptions));
        int vf = formatArgs.indexOf("-vf");
        if (vf >= 0 && vf + 1 < formatArgs.size()) {
            filters.add(formatArgs.remove(vf + 1));
            formatArgs.remove(vf);
        }

        List<String> command = new ArrayList<>();
        command.add(actualOptions.getFfmpegPath());
        command.addAll(
                List.of(
                        "-loglevel",
                        "error",
                        "-avioflags",
                        "direct",
                        "-fpsprobesize",
                        "0",
                        "-probesize",
                        "32",
                        "-analyzeduration",
                        "0",
                        "-fflags",
                        "nobuffer",
                        "-f",
                        "image2pipe",
                        "-vcodec",
                        "png",
                        "-i",
                        "pipe:0",
                        "-an",
                        "-threads",
                        "1",
                        "-framerate",
                        String.valueOf(actualOptions.getFps()),
                        "-b:v",
                        "0"));
        command.addAll(formatArgs);
        command.addAll(
                List.of(
                        "-vf",
                        String.join(Symbol.COMMA, filters),
                        actualOptions.isOverwrite() ? "-y" : "-n",
                        "pipe:1"));
        return List.copyOf(command);
    }

    /**
     * Builds format-specific FFmpeg arguments.
     *
     * @param options recorder options
     * @return format-specific arguments
     */
    private static List<String> formatArgs(ScreencastOptions options) {
        List<String> libvpx = List.of(
                "-vcodec",
                "vp9",
                "-crf",
                String.valueOf(options.getQuality()),
                "-deadline",
                "realtime",
                "-cpu-used",
                String.valueOf(Math.min(Math.max(1, Runtime.getRuntime().availableProcessors() / 2), 8)));
        return switch (options.getFormat()) {
            case WEBM -> combine(libvpx, List.of("-f", "webm"));
            case MP4 -> combine(libvpx, List.of("-movflags", "hybrid_fragmented", "-f", "mp4"));
            case GIF -> List.of(
                    "-vf",
                    "fps=" + gifFps(options) + ",split[s0][s1];[s0]palettegen=stats_mode=diff:max_colors="
                            + options.getColors() + "[p];[s1][p]paletteuse=dither=bayer",
                    "-loop",
                    String.valueOf(options.getLoop()),
                    "-final_delay",
                    String.valueOf(options.getDelay() == -1 ? -1 : options.getDelay() / 10),
                    "-f",
                    "gif");
        };
    }

    /**
     * Combines two argument lists.
     *
     * @param left  first list
     * @param right second list
     * @return combined list
     */
    private static List<String> combine(List<String> left, List<String> right) {
        List<String> result = new ArrayList<>(left);
        result.addAll(right);
        return result;
    }

    /**
     * Returns GIF frame-rate argument.
     *
     * @param options recorder options
     * @return GIF frame-rate argument
     */
    private static String gifFps(ScreencastOptions options) {
        return options.getFps() == DEFAULT_FPS ? "20" : "source_fps";
    }

    /**
     * Ensures the output directory exists when a path is provided.
     *
     * @param options recorder options
     */
    private void ensureOutputDirectory(ScreencastOptions options) {
        if (options.getPath() == null || options.getPath().getParent() == null) {
            return;
        }
        try {
            FileKit.mkdir(options.getPath().getParent().toFile());
        } catch (RuntimeException ex) {
            throw new InternalException("Failed to create screen recorder output directory.", ex);
        }
    }

    /**
     * Starts the FFmpeg stderr drainer.
     *
     * @return stderr drainer thread
     */
    private Thread startStderrDrainer() {
        Thread thread = new Thread(this::drainStderr, "lancia-screen-recorder-stderr");
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * Drains FFmpeg stderr into a bounded diagnostic buffer.
     */
    private void drainStderr() {
        byte[] buffer = new byte[8192];
        try (InputStream stderr = process.getErrorStream()) {
            int read;
            while ((read = stderr.read(buffer)) >= 0) {
                appendStderr(buffer, read);
            }
        } catch (IOException ignored) {
            // The process may close stderr while stop is shutting the recorder down.
        }
    }

    /**
     * Appends bytes to the bounded stderr ring buffer.
     *
     * @param bytes  source bytes
     * @param length number of bytes to append
     */
    private void appendStderr(byte[] bytes, int length) {
        if (stderrBytes.length == 0 || length <= 0) {
            return;
        }
        synchronized (stderrLock) {
            for (int index = 0; index < length; index++) {
                int writeIndex;
                if (stderrSize < stderrBytes.length) {
                    writeIndex = (stderrStart + stderrSize) % stderrBytes.length;
                    stderrSize++;
                } else {
                    writeIndex = stderrStart;
                    stderrStart = (stderrStart + 1) % stderrBytes.length;
                }
                stderrBytes[writeIndex] = bytes[index];
            }
        }
    }

    /**
     * Returns bounded stderr diagnostics.
     *
     * @return stderr text
     */
    private String stderrText() {
        synchronized (stderrLock) {
            byte[] output = new byte[stderrSize];
            for (int index = 0; index < stderrSize; index++) {
                output[index] = stderrBytes[(stderrStart + index) % stderrBytes.length];
            }
            return new String(output, Charset.UTF_8);
        }
    }

    /**
     * Process factory used by tests and production.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    interface ProcessFactory {

        /**
         * Starts the process.
         *
         * @param command process command
         * @return started process
         */
        Process start(List<String> command);
    }

    /**
     * Process factory backed by {@link ProcessBuilder}.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private enum ProcessBuilderProcessFactory implements ProcessFactory {

        /**
         * Singleton instance.
         */
        INSTANCE;

        /**
         * Starts the FFmpeg process.
         *
         * @param command process command
         * @return started process
         */
        @Override
        public Process start(List<String> command) {
            try {
                return new ProcessBuilder(command).start();
            } catch (IOException ex) {
                throw new InternalException("Failed to start FFmpeg process.", ex);
            }
        }
    }

}
