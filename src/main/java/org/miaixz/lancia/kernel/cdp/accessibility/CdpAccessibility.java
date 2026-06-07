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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.kernel.Accessibility;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Reads accessibility tree snapshots from a page.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpAccessibility implements Accessibility {

    /**
     * Current session.
     */
    private final CDPSession session;

    /**
     * Creates an CdpAccessibility instance.
     *
     * @param session session
     */
    public CdpAccessibility(CDPSession session) {
        this.session = session;
    }

    /**
     * Returns the snapshot.
     *
     * @return values
     */
    public List<CdpAXNode> snapshot() {
        CdpPayload result = Awaitable.await(
                CDPSession.sendIfPresent(session, "CdpAccessibility.getFullAXTree", Map.of()),
                "Failed to read accessibility tree.");
        return result.get("nodes").elements().stream().map(CdpAXNode::from).toList();
    }

    /**
     * Returns the snapshot.
     *
     * @param options operation options
     * @return snapshot value
     */
    public SerializedAXNode snapshot(SnapshotOptions options) {
        SnapshotOptions actualOptions = options == null ? new SnapshotOptions() : options;
        CdpPayload result = Awaitable.await(
                CDPSession.sendIfPresent(
                        session,
                        "CdpAccessibility.getFullAXTree",
                        Map.of("frameId", actualOptions.frameId())),
                "Failed to read accessibility tree.");
        TreeNode defaultRoot = TreeNode.createTree(result.get("nodes"));
        if (defaultRoot == null) {
            return null;
        }
        if (actualOptions.includeIframes()) {
            populateIframes(defaultRoot, actualOptions);
        }
        Integer backendNodeId = backendNodeId(actualOptions.rootObjectId());
        TreeNode needle = defaultRoot;
        if (backendNodeId != null) {
            needle = defaultRoot.find(node -> backendNodeId.equals(node.backendNodeId()));
        }
        if (needle == null) {
            return null;
        }
        if (!actualOptions.interestingOnly()) {
            List<SerializedAXNode> serialized = serializeTree(needle, null);
            return serialized.isEmpty() ? null : serialized.get(0);
        }
        java.util.Set<TreeNode> interestingNodes = new java.util.LinkedHashSet<>();
        collectInterestingNodes(interestingNodes, defaultRoot, false);
        List<SerializedAXNode> serialized = serializeTree(needle, interestingNodes);
        return serialized.isEmpty() ? null : serialized.get(0);
    }

    /**
     * Populates iframe accessibility snapshots in parallel.
     *
     * @param root    root tree node
     * @param options snapshot options
     */
    private void populateIframes(TreeNode root, SnapshotOptions options) {
        List<CompletableFuture<Void>> tasks = new ArrayList<>();
        collectIframeTasks(root, options, tasks);
        if (!tasks.isEmpty()) {
            CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();
        }
    }

    /**
     * Collects iframe snapshot tasks for a tree.
     *
     * @param node    tree node
     * @param options snapshot options
     * @param tasks   task collector
     */
    private void collectIframeTasks(TreeNode node, SnapshotOptions options, List<CompletableFuture<Void>> tasks) {
        if (node.isIframe() && node.backendNodeId() != null) {
            tasks.add(CompletableFuture.runAsync(() -> populateIframeSnapshot(node, options)).exceptionally(error -> {
                Logger.debug(false, "Page", "Accessibility iframe snapshot skipped: message={}", error.getMessage());
                return null;
            }));
        }
        for (TreeNode child : node.children()) {
            collectIframeTasks(child, options, tasks);
        }
    }

    /**
     * Populates one iframe snapshot.
     *
     * @param node    iframe tree node
     * @param options snapshot options
     */
    private void populateIframeSnapshot(TreeNode node, SnapshotOptions options) {
        String frameId = iframeFrameId(node.backendNodeId());
        if (StringKit.isBlank(frameId) || frameId.equals(options.frameId())) {
            return;
        }
        SnapshotOptions iframeOptions = new SnapshotOptions().setInterestingOnly(options.interestingOnly())
                .setIncludeIframes(options.includeIframes()).setFrameId(frameId);
        node.setIframeSnapshot(snapshot(iframeOptions));
    }

    /**
     * Resolves the frame id for an iframe backend DOM node.
     *
     * @param backendNodeId iframe backend DOM node id
     * @return frame id
     */
    private String iframeFrameId(Integer backendNodeId) {
        if (backendNodeId == null) {
            return Normal.EMPTY;
        }
        CdpPayload result = Awaitable.await(
                CDPSession.sendIfPresent(session, "DOM.describeNode", Map.of("backendNodeId", backendNodeId)),
                "Failed to read iframe node.");
        CdpPayload node = result.get("node");
        String frameId = PayloadReader.text(node.get("frameId"));
        if (StringKit.isBlank(frameId)) {
            frameId = PayloadReader.text(node.get("contentDocument").get("frameId"));
        }
        return frameId;
    }

    /**
     * Returns the serialize tree.
     *
     * @param node             node value
     * @param interestingNodes interesting nodes value
     * @return values
     */
    private List<SerializedAXNode> serializeTree(TreeNode node, java.util.Set<TreeNode> interestingNodes) {
        List<SerializedAXNode> children = new ArrayList<>();
        for (TreeNode child : node.children()) {
            children.addAll(serializeTree(child, interestingNodes));
        }
        if (interestingNodes != null && !interestingNodes.contains(node)) {
            return children;
        }
        SerializedAXNode serialized = node.serialize();
        for (SerializedAXNode child : children) {
            serialized.addChild(child);
        }
        if (node.iframeSnapshot() != null) {
            serialized.addChild(node.iframeSnapshot());
        }
        return List.of(serialized);
    }

    /**
     * Handles collect interesting nodes.
     *
     * @param collection    collection value
     * @param node          node value
     * @param insideControl inside control value
     */
    private void collectInterestingNodes(java.util.Set<TreeNode> collection, TreeNode node, boolean insideControl) {
        if (node.isInteresting(insideControl) || node.iframeSnapshot() != null) {
            collection.add(node);
        }
        if (node.isLeafNode()) {
            return;
        }
        boolean childInsideControl = insideControl || node.isControl();
        for (TreeNode child : node.children()) {
            collectInterestingNodes(collection, child, childInsideControl);
        }
    }

    /**
     * Returns the backend node ID.
     *
     * @param rootObjectId root object ID value
     * @return backend node ID value
     */
    private Integer backendNodeId(String rootObjectId) {
        if (StringKit.isBlank(rootObjectId)) {
            return null;
        }
        CdpPayload result = Awaitable.await(
                CDPSession.sendIfPresent(session, "DOM.describeNode", Map.of("objectId", rootObjectId)),
                "Failed to read root node.");
        CdpPayload backendNodeId = result.get("node").get("backendNodeId");
        return backendNodeId.isNull() ? null : backendNodeId.asInt();
    }

    /**
     * Defines options for snapshot operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class SnapshotOptions {

        /**
         * Whether interesting only is enabled.
         */
        private boolean interestingOnly = true;
        /**
         * Whether include iframes is enabled.
         */
        private boolean includeIframes;
        /**
         * Current root object ID.
         */
        private String rootObjectId;
        /**
         * Current frame ID.
         */
        private String frameId = Normal.EMPTY;

        /**
         * Returns the interesting only.
         *
         * @return {@code true} when the condition matches
         */
        public boolean interestingOnly() {
            return interestingOnly;
        }

        /**
         * Updates interesting only.
         *
         * @param interestingOnly interesting only
         * @return set interesting only value
         */
        public SnapshotOptions setInterestingOnly(boolean interestingOnly) {
            this.interestingOnly = interestingOnly;
            return this;
        }

        /**
         * Returns the include iframes.
         *
         * @return {@code true} when the condition matches
         */
        public boolean includeIframes() {
            return includeIframes;
        }

        /**
         * Updates include iframes.
         *
         * @param includeIframes include iframes
         * @return set include iframes value
         */
        public SnapshotOptions setIncludeIframes(boolean includeIframes) {
            this.includeIframes = includeIframes;
            return this;
        }

        /**
         * Returns the root object ID.
         *
         * @return root object ID value
         */
        public String rootObjectId() {
            return rootObjectId;
        }

        /**
         * Updates root object ID.
         *
         * @param rootObjectId root object id
         * @return set root object ID value
         */
        public SnapshotOptions setRootObjectId(String rootObjectId) {
            this.rootObjectId = rootObjectId;
            return this;
        }

        /**
         * Returns the frame ID.
         *
         * @return frame ID value
         */
        public String frameId() {
            return frameId;
        }

        /**
         * Updates frame ID.
         *
         * @param frameId frame id
         * @return set frame ID value
         */
        public SnapshotOptions setFrameId(String frameId) {
            this.frameId = frameId == null ? Normal.EMPTY : frameId;
            return this;
        }
    }

    /**
     * Represents a serialized AX tree node.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class SerializedAXNode {

        /**
         * Current role.
         */
        private final String role;
        /**
         * Mapped properties values.
         */
        private final Map<String, Object> properties;
        /**
         * Registered children values.
         */
        private final List<SerializedAXNode> children = new ArrayList<>();
        /**
         * Current backend node ID.
         */
        private final Integer backendNodeId;

        /**
         * Creates an instance.
         *
         * @param role          role value
         * @param properties    properties value
         * @param backendNodeId backend node ID value
         */
        private SerializedAXNode(String role, Map<String, Object> properties, Integer backendNodeId) {
            this.role = role;
            this.properties = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(properties));
            this.backendNodeId = backendNodeId;
        }

        /**
         * Handles add child.
         *
         * @param child child value
         */
        private void addChild(SerializedAXNode child) {
            children.add(child);
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
         * @return optional value
         */
        public Optional<String> name() {
            Object value = properties.get("name");
            return value == null ? Optional.empty() : Optional.of(String.valueOf(value));
        }

        /**
         * Returns the properties.
         *
         * @return mapped values
         */
        public Map<String, Object> properties() {
            return properties;
        }

        /**
         * Returns the children.
         *
         * @return values
         */
        public List<SerializedAXNode> children() {
            return List.copyOf(children);
        }

        /**
         * Returns the backend node ID.
         *
         * @return optional value
         */
        public Optional<Integer> backendNodeId() {
            return Optional.ofNullable(backendNodeId);
        }
    }

    /**
     * Represents a tree tree node.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class TreeNode {

        /**
         * Current payload.
         */
        private final CdpPayload payload;
        /**
         * Registered children values.
         */
        private final List<TreeNode> children = new ArrayList<>();
        /**
         * Current role.
         */
        private final String role;
        /**
         * Current name.
         */
        private final String name;
        /**
         * Current description.
         */
        private final String description;
        /**
         * Whether ignored is enabled.
         */
        private final boolean ignored;
        /**
         * Mapped properties values.
         */
        private final Map<String, Object> properties;
        /**
         * Current iframe accessibility snapshot.
         */
        private volatile SerializedAXNode iframeSnapshot;

        /**
         * Creates an instance.
         *
         * @param payload protocol payload
         */
        private TreeNode(CdpPayload payload) {
            this.payload = payload;
            this.role = PayloadReader.text(payload.get("role").get("value"), "Unknown");
            this.name = PayloadReader.text(payload.get("name").get("value"), Normal.EMPTY);
            this.description = PayloadReader.text(payload.get("description").get("value"), Normal.EMPTY);
            this.ignored = PayloadReader.bool(payload.get("ignored"));
            this.properties = properties(payload);
        }

        /**
         * Creates tree.
         *
         * @param payloads payloads value
         * @return create tree value
         */
        private static TreeNode createTree(CdpPayload payloads) {
            if (payloads == null || !payloads.isArray()) {
                return null;
            }
            Map<String, TreeNode> nodeById = new LinkedHashMap<>();
            for (CdpPayload payload : payloads.elements()) {
                nodeById.put(PayloadReader.text(payload.get("nodeId"), Normal.EMPTY), new TreeNode(payload));
            }
            for (TreeNode node : nodeById.values()) {
                CdpPayload childIds = node.payload.get("childIds");
                if (!childIds.isArray()) {
                    continue;
                }
                for (CdpPayload childId : childIds.elements()) {
                    TreeNode child = nodeById.get(PayloadReader.text(childId, Normal.EMPTY));
                    if (child != null) {
                        node.children.add(child);
                    }
                }
            }
            return nodeById.values().stream().findFirst().orElse(null);
        }

        /**
         * Returns the find.
         *
         * @param predicate predicate value
         * @return find value
         */
        private TreeNode find(java.util.function.Predicate<TreeNode> predicate) {
            if (predicate.test(this)) {
                return this;
            }
            for (TreeNode child : children) {
                TreeNode result = child.find(predicate);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }

        /**
         * Returns the children.
         *
         * @return values
         */
        private List<TreeNode> children() {
            return children;
        }

        /**
         * Returns whether this node represents an iframe.
         *
         * @return {@code true} when this node represents an iframe
         */
        private boolean isIframe() {
            return "Iframe".equals(role) || "iframe".equals(role);
        }

        /**
         * Returns the iframe accessibility snapshot.
         *
         * @return iframe accessibility snapshot
         */
        private SerializedAXNode iframeSnapshot() {
            return iframeSnapshot;
        }

        /**
         * Updates the iframe accessibility snapshot.
         *
         * @param iframeSnapshot iframe accessibility snapshot
         */
        private void setIframeSnapshot(SerializedAXNode iframeSnapshot) {
            this.iframeSnapshot = iframeSnapshot;
        }

        /**
         * Returns the backend node ID.
         *
         * @return backend node ID value
         */
        private Integer backendNodeId() {
            CdpPayload id = payload.get("backendDOMNodeId");
            return id.isNull() ? null : id.asInt();
        }

        /**
         * Returns whether leaf node is enabled.
         *
         * @return {@code true} when the condition matches
         */
        private boolean isLeafNode() {
            return children.isEmpty() || isPlainTextField() || isTextOnlyObject() || isLeafRole()
                    || hasFocusableChild() && "heading".equals(role) && StringKit.isNotBlank(name);
        }

        /**
         * Returns whether control is enabled.
         *
         * @return {@code true} when the condition matches
         */
        private boolean isControl() {
            return switch (role) {
                case "button", "checkbox", "ColorWell", "combobox", "DisclosureTriangle", "listbox", "menu", "menubar", "menuitem", "menuitemcheckbox", "menuitemradio", "radio", "scrollbar", "searchbox", "slider", "spinbutton", "switch", "tab", "textbox", "tree", "treeitem" -> true;
                default -> false;
            };
        }

        /**
         * Returns whether interesting is enabled.
         *
         * @param insideControl inside control value
         * @return {@code true} when the condition matches
         */
        private boolean isInteresting(boolean insideControl) {
            if ("Ignored".equals(role) || boolProperty("hidden") || ignored) {
                return false;
            }
            if (isLandmark() || boolProperty("focusable") || boolProperty("busy") || boolProperty("modal")
                    || properties.containsKey("errormessage") || properties.containsKey("details")
                    || properties.containsKey("roledescription")) {
                return true;
            }
            if (isControl()) {
                return true;
            }
            return !insideControl && isLeafNode() && (StringKit.isNotBlank(name) || StringKit.isNotBlank(description));
        }

        /**
         * Returns the serialize.
         *
         * @return serialize value
         */
        private SerializedAXNode serialize() {
            Map<String, Object> values = new LinkedHashMap<>(properties);
            if (StringKit.isNotBlank(name)) {
                values.put("name", name);
            }
            if (StringKit.isNotBlank(description)) {
                values.put("description", description);
            }
            CdpPayload value = payload.get("value").get("value");
            if (!value.isNull()) {
                values.put("value", value.raw());
            }
            return new SerializedAXNode(role, values, backendNodeId());
        }

        /**
         * Returns whether plain text field is enabled.
         *
         * @return {@code true} when the condition matches
         */
        private boolean isPlainTextField() {
            return ("textbox".equals(role) || "searchbox".equals(role) || properties.containsKey("editable"))
                    && !"richtext".equals(properties.get("editable"));
        }

        /**
         * Returns whether text only object is enabled.
         *
         * @return {@code true} when the condition matches
         */
        private boolean isTextOnlyObject() {
            return switch (role) {
                case "LineBreak", "text", "InlineTextBox", "StaticText" -> true;
                default -> false;
            };
        }

        /**
         * Returns whether leaf role is enabled.
         *
         * @return {@code true} when the condition matches
         */
        private boolean isLeafRole() {
            return switch (role) {
                case "doc-cover", "graphics-symbol", "img", "image", "Meter", "scrollbar", "slider", "separator", "progressbar" -> true;
                default -> false;
            };
        }

        /**
         * Returns whether focusable child is available.
         *
         * @return {@code true} when the condition matches
         */
        private boolean hasFocusableChild() {
            for (TreeNode child : children) {
                if (child.boolProperty("focusable") || child.hasFocusableChild()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns whether landmark is enabled.
         *
         * @return {@code true} when the condition matches
         */
        private boolean isLandmark() {
            return switch (role) {
                case "banner", "complementary", "contentinfo", "form", "main", "navigation", "region", "search" -> true;
                default -> false;
            };
        }

        /**
         * Returns the bool property.
         *
         * @param name name to use
         * @return {@code true} when the condition matches
         */
        private boolean boolProperty(String name) {
            Object value = properties.get(name);
            return Boolean.TRUE.equals(value) || "true".equals(String.valueOf(value));
        }

        /**
         * Returns the properties.
         *
         * @param payload protocol payload
         * @return mapped values
         */
        private static Map<String, Object> properties(CdpPayload payload) {
            Map<String, Object> values = new LinkedHashMap<>();
            CdpPayload properties = payload.get("properties");
            if (properties.isArray()) {
                for (CdpPayload property : properties.elements()) {
                    values.put(
                            PayloadReader.text(property.get("name"), Normal.EMPTY).toLowerCase(),
                            property.get("value").get("value").raw());
                }
            }
            return values;
        }

    }

}
