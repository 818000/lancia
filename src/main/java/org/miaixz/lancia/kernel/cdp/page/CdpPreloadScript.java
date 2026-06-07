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
package org.miaixz.lancia.kernel.cdp.page;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;

/**
 * Represents CDP preload script.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class CdpPreloadScript {

    /**
     * Current identifier.
     */
    private final String id;
    /**
     * Current source.
     */
    private final String source;
    /**
     * Mapped frame to ID values.
     */
    private final Map<CdpFrame, String> frameToId = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Creates a CDP preload script.
     *
     * @param mainFrame main frame
     * @param id        identifier
     * @param source    source value
     */
    public CdpPreloadScript(CdpFrame mainFrame, String id, String source) {
        this.id = Assert.notBlank(id, "id");
        this.source = source == null ? Normal.EMPTY : source;
        setIdForFrame(Assert.notNull(mainFrame, "mainFrame"), id);
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        return id;
    }

    /**
     * Returns the ID.
     *
     * @return ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the source.
     *
     * @return source value
     */
    public String source() {
        return source;
    }

    /**
     * Returns the source.
     *
     * @return source
     */
    public String getSource() {
        return source;
    }

    /**
     * Returns the ID for frame.
     *
     * @param frame frame instance
     * @return ID for frame
     */
    public String getIdForFrame(CdpFrame frame) {
        if (frame == null) {
            return null;
        }
        return frameToId.get(frame);
    }

    /**
     * Updates ID for frame.
     *
     * @param frame      frame instance
     * @param identifier identifier
     */
    public void setIdForFrame(CdpFrame frame, String identifier) {
        frameToId.put(Assert.notNull(frame, "frame"), Assert.notBlank(identifier, "identifier"));
    }

}
