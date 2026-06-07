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
package org.miaixz.lancia.shared.frame;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.kernel.Frame;
import org.miaixz.lancia.shared.async.LanciaOnce;

/**
 * Represents frame tree.
 *
 * @param <T> the generic type handled by this member
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class FrameTree<T extends Frame> {

    /**
     * Mapped frames values.
     */
    private final Map<String, T> frames = new LinkedHashMap<>();
    /**
     * Mapped parent ids values.
     */
    private final Map<String, String> parentIds = new LinkedHashMap<>();
    /**
     * Mapped child ids values.
     */
    private final Map<String, Set<String>> childIds = new LinkedHashMap<>();
    /**
     * Current main frame.
     */
    private T mainFrame;
    /**
     * Whether main frame stale is enabled.
     */
    private boolean mainFrameStale;
    /**
     * Mapped wait requests values.
     */
    private final Map<String, Set<LanciaOnce<T>>> waitRequests = new LinkedHashMap<>();

    /**
     * Creates a frame tree.
     */
    public FrameTree() {
        // No initialization required.
    }

    /**
     * Creates a frame tree.
     *
     * @param mainFrame main frame
     */
    public FrameTree(T mainFrame) {
        this.mainFrame = mainFrame;
        if (mainFrame != null && StringKit.isNotBlank(mainFrame.id())) {
            addFrame(mainFrame);
        }
    }

    /**
     * Returns the main frame.
     *
     * @return main frame
     */
    public T getMainFrame() {
        return mainFrame;
    }

    /**
     * Returns the main frame.
     *
     * @return main frame value
     */
    public T mainFrame() {
        return getMainFrame();
    }

    /**
     * Returns the by ID.
     *
     * @param frameId frame id
     * @return by ID
     */
    public T getById(String frameId) {
        if (StringKit.isBlank(frameId)) {
            return null;
        }
        return frames.get(frameId);
    }

    /**
     * Returns the frame.
     *
     * @param frameId frame ID value
     * @return frame value
     */
    public T frame(String frameId) {
        return getById(frameId);
    }

    /**
     * Waits for frame.
     *
     * @param frameId frame id
     * @return wait for frame value
     */
    public CompletableFuture<T> waitForFrame(String frameId) {
        Assert.notBlank(frameId, "frameId");
        T frame = getById(frameId);
        if (frame != null) {
            return CompletableFuture.completedFuture(frame);
        }
        LanciaOnce<T> once = new LanciaOnce<>();
        waitRequests.computeIfAbsent(frameId, ignored -> new LinkedHashSet<>()).add(once);
        return once.future();
    }

    /**
     * Returns the frames.
     *
     * @return values
     */
    public List<T> frames() {
        List<T> result = new ArrayList<>(frames.values());
        if (mainFrame != null && (StringKit.isBlank(mainFrame.id()) || !frames.containsKey(mainFrame.id()))) {
            result.add(0, mainFrame);
        }
        return List.copyOf(result);
    }

    /**
     * Adds frame.
     *
     * @param frame frame instance
     */
    public void addFrame(T frame) {
        Assert.notNull(frame, "frame");
        String frameId = frame.id();
        if (frame.parentFrame() == null && (mainFrame == null || mainFrameStale || mainFrame == frame)) {
            mainFrame = frame;
            mainFrameStale = false;
        }
        if (StringKit.isBlank(frameId)) {
            return;
        }
        frames.put(frameId, frame);
        Frame parent = frame.parentFrame();
        if (parent != null && StringKit.isNotBlank(parent.id())) {
            parentIds.put(frameId, parent.id());
            childIds.computeIfAbsent(parent.id(), ignored -> new LinkedHashSet<>()).add(frameId);
        }
        resolveWaitRequests(frameId, frame);
    }

    /**
     * Removes frame.
     *
     * @param frame frame instance
     */
    public void removeFrame(T frame) {
        if (frame == null) {
            return;
        }
        String frameId = frame.id();
        if (frame == mainFrame || frame.parentFrame() == null) {
            mainFrameStale = true;
        }
        if (StringKit.isBlank(frameId)) {
            return;
        }
        frames.remove(frameId);
        String parentId = parentIds.remove(frameId);
        if (StringKit.isNotBlank(parentId)) {
            Set<String> children = childIds.get(parentId);
            if (children != null) {
                children.remove(frameId);
                if (children.isEmpty()) {
                    childIds.remove(parentId);
                }
            }
        }
        childIds.remove(frameId);
    }

    /**
     * Returns the child frames.
     *
     * @param frameId frame ID value
     * @return values
     */
    public List<T> childFrames(String frameId) {
        if (StringKit.isBlank(frameId)) {
            return List.of();
        }
        Set<String> ids = childIds.get(frameId);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<T> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            T frame = getById(id);
            if (frame != null) {
                result.add(frame);
            }
        }
        return List.copyOf(result);
    }

    /**
     * Returns the parent frame.
     *
     * @param frameId frame ID value
     * @return parent frame value
     */
    public T parentFrame(String frameId) {
        if (StringKit.isBlank(frameId)) {
            return null;
        }
        String parentId = parentIds.get(frameId);
        return StringKit.isBlank(parentId) ? null : getById(parentId);
    }

    /**
     * Returns whether main frame stale is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isMainFrameStale() {
        return mainFrameStale;
    }

    /**
     * Returns the wait request count.
     *
     * @param frameId frame ID value
     * @return wait request count value
     */
    public int waitRequestCount(String frameId) {
        Set<LanciaOnce<T>> requests = waitRequests.get(frameId);
        return requests == null ? 0 : requests.size();
    }

    /**
     * Handles resolve wait requests.
     *
     * @param frameId frame ID value
     * @param frame   frame instance
     */
    private void resolveWaitRequests(String frameId, T frame) {
        Set<LanciaOnce<T>> requests = waitRequests.remove(frameId);
        if (requests == null || requests.isEmpty()) {
            return;
        }
        for (LanciaOnce<T> request : requests) {
            request.success(frame);
        }
    }

}
