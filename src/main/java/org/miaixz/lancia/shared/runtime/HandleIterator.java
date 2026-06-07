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
package org.miaixz.lancia.shared.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.kernel.Handle;

/**
 * Represents handle iterator.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class HandleIterator implements Iterator<Handle>, Iterable<Handle>, AutoCloseable {

    /**
     * Default batch size.
     */
    public static final int DEFAULT_BATCH_SIZE = 20;
    /**
     * Current iterator handle.
     */
    private final Handle iteratorHandle;
    /**
     * Whether close iterator handle is enabled.
     */
    private final boolean closeIteratorHandle;
    /**
     * Current batch size.
     */
    private int batchSize;
    /**
     * Registered current batch values.
     */
    private List<Handle> currentBatch = List.of();
    /**
     * Current current index.
     */
    private int currentIndex;
    /**
     * Whether exhausted is enabled.
     */
    private boolean exhausted;
    /**
     * Whether closed is enabled.
     */
    private boolean closed;

    /**
     * Creates a handle iterator.
     *
     * @param iteratorHandle      iterator handle
     * @param batchSize           batch size
     * @param closeIteratorHandle close iterator handle
     */
    private HandleIterator(Handle iteratorHandle, int batchSize, boolean closeIteratorHandle) {
        this.iteratorHandle = requireHandle(iteratorHandle);
        this.batchSize = Math.max(1, batchSize);
        this.closeIteratorHandle = closeIteratorHandle;
    }

    /**
     * Returns the fast transpose iterator handle.
     *
     * @param iteratorHandle iterator handle value
     * @param size           size value
     * @return fast transpose iterator handle value
     */
    public static Batch fastTransposeIteratorHandle(Handle iteratorHandle, int size) {
        Handle actualIterator = requireHandle(iteratorHandle);
        int actualSize = Math.max(1, size);
        Handle arrayHandle = actualIterator.evaluateHandle(fastTransposeFunction(actualSize));
        try {
            Map<String, ? extends Handle> properties = arrayHandle.getProperties();
            List<Handle> handles = new ArrayList<>();
            properties.entrySet().stream()
                    .sorted(Comparator.comparingInt(entry -> numericPropertyIndex(entry.getKey())))
                    .forEach(entry -> handles.add(entry.getValue()));
            return new Batch(handles, properties.isEmpty());
        } finally {
            arrayHandle.dispose();
        }
    }

    /**
     * Returns the transpose iterator handle.
     *
     * @param iteratorHandle iterator handle value
     * @return transpose iterator handle value
     */
    public static HandleIterator transposeIteratorHandle(Handle iteratorHandle) {
        return new HandleIterator(iteratorHandle, DEFAULT_BATCH_SIZE, true);
    }

    /**
     * Returns the transpose iterable handle.
     *
     * @param handle handle value
     * @return transpose iterable handle value
     */
    public static HandleIterator transposeIterableHandle(Handle handle) {
        Handle actualHandle = requireHandle(handle);
        Handle generatorHandle = actualHandle
                .evaluateHandle("function(){const iterable=this;return (async function*(){yield* iterable;})();}");
        return new HandleIterator(generatorHandle, DEFAULT_BATCH_SIZE, true);
    }

    /**
     * Converts this value to list.
     *
     * @param handle handle value
     * @return values
     */
    public static List<Handle> toList(Handle handle) {
        try (HandleIterator iterator = transposeIterableHandle(handle)) {
            List<Handle> handles = new ArrayList<>();
            while (iterator.hasNext()) {
                handles.add(iterator.next());
            }
            return List.copyOf(handles);
        }
    }

    /**
     * Returns whether next is available.
     *
     * @return {@code true} when the condition matches
     */
    @Override
    public boolean hasNext() {
        if (closed) {
            return false;
        }
        if (currentIndex < currentBatch.size()) {
            return true;
        }
        loadNextBatch();
        return currentIndex < currentBatch.size();
    }

    /**
     * Returns the next.
     *
     * @return next value
     */
    @Override
    public Handle next() {
        if (!hasNext()) {
            throw new NoSuchElementException("Handle iterator is exhausted.");
        }
        return currentBatch.get(currentIndex++);
    }

    /**
     * Returns the iterator.
     *
     * @return iterator value
     */
    @Override
    public Iterator<Handle> iterator() {
        return this;
    }

    /**
     * Closes this object and releases its resources.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        disposeRemainingHandles();
        if (closeIteratorHandle) {
            iteratorHandle.dispose();
        }
    }

    /**
     * Returns whether this object is closed.
     *
     * @return whether this object is closed
     */
    public boolean closed() {
        return closed;
    }

    /**
     * Handles load next batch.
     */
    private void loadNextBatch() {
        if (exhausted) {
            close();
            return;
        }
        Batch batch = fastTransposeIteratorHandle(iteratorHandle, batchSize);
        currentBatch = batch.handles();
        currentIndex = Normal._0;
        exhausted = batch.exhausted();
        if (!exhausted) {
            batchSize = Math.max(batchSize << 1, batchSize);
        }
        if (currentBatch.isEmpty() && exhausted) {
            close();
        }
    }

    /**
     * Handles dispose remaining handles.
     */
    private void disposeRemainingHandles() {
        for (int index = currentIndex; index < currentBatch.size(); index++) {
            currentBatch.get(index).dispose();
        }
        currentBatch = List.of();
        currentIndex = Normal._0;
    }

    /**
     * Returns the fast transpose function.
     *
     * @param size size value
     * @return fast transpose function value
     */
    private static String fastTransposeFunction(int size) {
        return "async function(){const iterator=this;const results=[];while(results.length<" + size
                + "){const result=await iterator.next();if(result.done){break;}results.push(result.value);}return results;}";
    }

    /**
     * Returns the require handle.
     *
     * @param handle handle value
     * @return require handle value
     */
    private static Handle requireHandle(Handle handle) {
        if (handle == null) {
            throw new InternalException("Handle cannot be null.");
        }
        return handle;
    }

    /**
     * Returns the numeric property index.
     *
     * @param propertyName property name value
     * @return numeric property index value
     */
    private static int numericPropertyIndex(String propertyName) {
        if (StringKit.isBlank(propertyName)) {
            return Integer.MAX_VALUE;
        }
        for (int i = Normal._0; i < propertyName.length(); i++) {
            char current = propertyName.charAt(i);
            if (current < Symbol.C_ZERO || current > Symbol.C_NINE) {
                return Integer.MAX_VALUE;
            }
        }
        return Integer.parseInt(propertyName);
    }

    /**
     * Represents batch.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class Batch {

        /**
         * Registered handles values.
         */
        private final List<Handle> handles;
        /**
         * Whether exhausted is enabled.
         */
        private final boolean exhausted;

        /**
         * Creates an instance.
         *
         * @param handles   handles value
         * @param exhausted exhausted value
         */
        public Batch(List<Handle> handles, boolean exhausted) {
            this.handles = handles == null ? List.of() : List.copyOf(handles);
            this.exhausted = exhausted;
        }

        /**
         * Handles s.
         *
         * @return handles value
         */
        public List<Handle> handles() {
            return handles;
        }

        /**
         * Returns the exhausted.
         *
         * @return {@code true} when the condition matches
         */
        public boolean exhausted() {
            return exhausted;
        }
    }

}
