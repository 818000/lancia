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
package org.miaixz.lancia.browser.supervisor;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.xyz.IoKit;

/**
 * Holds a browser process and its CDP pipe streams.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class PipeEnvelope implements AutoCloseable {

    /**
     * Browser process using the pipe streams.
     */
    private final Process process;
    /**
     * Stream that reads CDP messages from the browser.
     */
    private final InputStream cdpReader;
    /**
     * Stream that writes CDP messages to the browser.
     */
    private final OutputStream cdpWriter;
    /**
     * Temporary directory that holds platform pipe files when needed.
     */
    private final Path temporaryDirectory;

    /**
     * Creates a pipe envelope.
     *
     * @param process   process
     * @param cdpReader cdp reader
     * @param cdpWriter cdp writer
     */
    public PipeEnvelope(Process process, InputStream cdpReader, OutputStream cdpWriter) {
        this(process, cdpReader, cdpWriter, null);
    }

    /**
     * Creates a pipe envelope.
     *
     * @param process            process
     * @param cdpReader          cdp reader
     * @param cdpWriter          cdp writer
     * @param temporaryDirectory temporary directory
     */
    public PipeEnvelope(Process process, InputStream cdpReader, OutputStream cdpWriter, Path temporaryDirectory) {
        this.process = Assert.notNull(process, "process");
        this.cdpReader = Assert.notNull(cdpReader, "cdpReader");
        this.cdpWriter = Assert.notNull(cdpWriter, "cdpWriter");
        this.temporaryDirectory = temporaryDirectory;
    }

    /**
     * Returns the process.
     *
     * @return process
     */
    public Process getProcess() {
        return process;
    }

    /**
     * Returns the CDP reader.
     *
     * @return CDP reader
     */
    public InputStream getCdpReader() {
        return cdpReader;
    }

    /**
     * Returns the CDP writer.
     *
     * @return CDP writer
     */
    public OutputStream getCdpWriter() {
        return cdpWriter;
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        process.destroy();
        try {
            if (!process.waitFor(500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
        IoKit.closeQuietly(cdpReader, cdpWriter);
        BrowserProcess.cleanupTemporaryDirectory(temporaryDirectory);
    }

}
