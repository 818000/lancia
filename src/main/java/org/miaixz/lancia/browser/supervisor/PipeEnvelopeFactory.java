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

import java.io.IOException;
import java.util.List;

import org.miaixz.bus.core.lang.Assert;

/**
 * Defines the pipe envelope factory contract.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface PipeEnvelopeFactory {

    /**
     * Chromium flag enabling remote debugging over inherited pipes.
     */
    String REMOTE_DEBUGGING_PIPE = "--remote-debugging-pipe";

    /**
     * Chromium flag that would expose DevTools over TCP.
     */
    String REMOTE_DEBUGGING_PORT = "--remote-debugging-port";

    /**
     * Creates a pipe envelope for a browser process.
     *
     * @param command command name
     * @return pipe envelope
     * @throws IOException if the operation fails
     */
    PipeEnvelope create(List<String> command) throws IOException;

    /**
     * Validates pipe command.
     *
     * @param command command name
     */
    static void validatePipeCommand(List<String> command) {
        Assert.notNull(command, "command");
        if (!command.contains(REMOTE_DEBUGGING_PIPE)) {
            throw new IllegalArgumentException("Pipe mode requires --remote-debugging-pipe.");
        }
        for (String arg : command) {
            if (arg != null && arg.startsWith(REMOTE_DEBUGGING_PORT)) {
                throw new IllegalArgumentException("Pipe mode must not expose the DevTools TCP port.");
            }
        }
    }

}
