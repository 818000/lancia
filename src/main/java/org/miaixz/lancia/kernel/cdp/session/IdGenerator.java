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
package org.miaixz.lancia.kernel.cdp.session;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import org.miaixz.bus.core.lang.Assert;

/**
 * Thread-safe incremental ID generator aligned with Puppeteer's incremental-id-generator.ts.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class IdGenerator implements LongSupplier, IntSupplier {

    /**
     * JavaScript maximum safe integer.
     */
    public static final long MAX_SAFE_ID = 9_007_199_254_740_991L;

    /**
     * Maximum ID before wrapping to {@code 1}.
     */
    private final long maxId;

    /**
     * Current identifier.
     */
    private final AtomicLong id = new AtomicLong();

    /**
     * Creates a generator that wraps at JavaScript's maximum safe integer.
     */
    public IdGenerator() {
        this(MAX_SAFE_ID);
    }

    /**
     * Creates a generator with a custom maximum ID.
     *
     * @param maxId maximum ID before wrapping to {@code 1}
     */
    public IdGenerator(long maxId) {
        Assert.isTrue(maxId > 0, "maxId must be positive.");
        this.maxId = maxId;
    }

    /**
     * Creates a generator that wraps at JavaScript's maximum safe integer.
     *
     * @return ID generator
     */
    public static IdGenerator createIncrementalIdGenerator() {
        return new IdGenerator();
    }

    /**
     * Creates a generator that is safe for Java int-based protocol IDs.
     *
     * @return int-range ID generator
     */
    public static IdGenerator intRange() {
        return new IdGenerator(Integer.MAX_VALUE);
    }

    /**
     * Returns the next ID.
     *
     * @return next ID
     */
    public long next() {
        return id.updateAndGet(current -> current >= maxId ? 1L : current + 1L);
    }

    /**
     * Returns the next ID as an int.
     *
     * @return next int ID
     */
    public int nextInt() {
        return Math.toIntExact(next());
    }

    /**
     * Returns the next ID.
     *
     * @return next ID
     */
    @Override
    public long getAsLong() {
        return next();
    }

    /**
     * Returns the next ID as an int.
     *
     * @return next int ID
     */
    @Override
    public int getAsInt() {
        return nextInt();
    }

}
