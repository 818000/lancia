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
package org.miaixz.lancia.kernel.cdp.accessibility;

import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents a AX tree node.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpAXNode {

    /**
     * Current node ID.
     */
    private final String nodeId;
    /**
     * Current role.
     */
    private final String role;
    /**
     * Current name.
     */
    private final String name;

    /**
     * Creates an CdpAXNode instance.
     *
     * @param nodeId node id
     * @param role   role
     * @param name   name
     */
    public CdpAXNode(String nodeId, String role, String name) {
        this.nodeId = nodeId;
        this.role = role;
        this.name = name;
    }

    /**
     * Returns the from.
     *
     * @param payload protocol payload
     * @return from value
     */
    public static CdpAXNode from(CdpPayload payload) {
        return new CdpAXNode(PayloadReader.text(payload.get("nodeId")),
                PayloadReader.text(payload.get("role").get("value")),
                PayloadReader.text(payload.get("name").get("value")));
    }

    /**
     * Returns the node ID.
     *
     * @return node ID value
     */
    public String nodeId() {
        return nodeId;
    }

    /**
     * Returns the role.
     *
     * @return role value
     */
    public String role() {
        return role;
    }

    /**
     * Returns the name.
     *
     * @return name value
     */
    public String name() {
        return name;
    }

}
