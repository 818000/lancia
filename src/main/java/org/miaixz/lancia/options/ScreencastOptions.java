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

import lombok.Getter;
import lombok.Setter;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.lancia.nimble.screen.ScreenshotClip;
import org.miaixz.lancia.nimble.screen.VideoFormat;

/**
 * Public screencast options matching Puppeteer's ScreencastOptions name.
 */
@Getter
@Setter
/**
 * Defines options for screencast operations.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class ScreencastOptions {

    /**
     * Default recording frame rate.
     */
    public static final int DEFAULT_FPS = 30;

    /**
     * Default output quality.
     */
    public static final int DEFAULT_QUALITY = 30;

    /**
     * FFmpeg executable path.
     */
    private String ffmpegPath = "ffmpeg";

    /**
     * Playback speed multiplier.
     */
    private Double speed;

    /**
     * Crop rectangle.
     */
    private ScreenshotClip crop;

    /**
     * Output video format.
     */
    private VideoFormat format = VideoFormat.WEBM;

    /**
     * Output frame rate.
     */
    private int fps = DEFAULT_FPS;

    /**
     * GIF loop count.
     */
    private int loop = -1;

    /**
     * GIF final delay in milliseconds.
     */
    private int delay = -1;

    /**
     * Output quality.
     */
    private int quality = DEFAULT_QUALITY;

    /**
     * GIF palette color count.
     */
    private int colors = 256;

    /**
     * Scale multiplier.
     */
    private Double scale;

    /**
     * Output path.
     */
    private Path path;

    /**
     * Whether output should be overwritten.
     */
    private boolean overwrite = true;

    /**
     * Creates screencast options.
     */
    public ScreencastOptions() {
        // No initialization required.
    }

    /**
     * Updates path.
     *
     * @param path output path text
     */
    public void setPath(String path) {
        this.path = path == null ? null : Path.of(path);
    }

    /**
     * Updates path.
     *
     * @param path output path
     */
    public void setPath(Path path) {
        this.path = path;
    }

    /**
     * Updates format.
     *
     * @param format output format text
     */
    public void setFormat(String format) {
        this.format = VideoFormat.fromValue(format);
    }

    /**
     * Updates format.
     *
     * @param format output format
     */
    public void setFormat(VideoFormat format) {
        this.format = format;
    }

    /**
     * Returns normalized options.
     *
     * @return normalized copy
     */
    public ScreencastOptions normalized() {
        ScreencastOptions copy = new ScreencastOptions();
        copy.ffmpegPath = String.valueOf(Assert.notBlank(ffmpegPath, "ffmpegPath"));
        copy.speed = speed;
        copy.crop = crop;
        copy.format = format == null ? VideoFormat.WEBM : format;
        copy.fps = fps <= 0 ? DEFAULT_FPS : fps;
        copy.loop = loop == 0 ? -1 : loop;
        copy.delay = delay;
        copy.quality = quality <= 0 ? DEFAULT_QUALITY : quality;
        copy.colors = colors <= 0 ? 256 : colors;
        copy.scale = scale;
        copy.path = path;
        copy.overwrite = overwrite;
        return copy;
    }

}
