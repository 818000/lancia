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
package org.miaixz.lancia.nimble;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

/**
 * Provides lightweight invocation contracts and payloads.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class Invocations {

    /**
     * Creates an invocation contract holder.
     */
    private Invocations() {
        // No initialization required.
    }

    /**
     * Returns the awaitable.
     *
     * @param value to use
     * @return completion future
     */
    public static <T> CompletionStage<T> awaitable(T value) {
        return CompletableFuture.completedFuture(value);
    }

    /**
     * Returns the awaitable.
     *
     * @param value to use
     * @return completion future
     */
    public static <T> CompletionStage<T> awaitable(CompletionStage<T> value) {
        return value == null ? CompletableFuture.completedFuture(null) : value;
    }

    /**
     * Returns immutable invocation parameters.
     *
     * @param params invocation parameters
     * @return values
     */
    public static List<Object> innerParams(Object... params) {
        List<Object> values = new ArrayList<>();
        if (params != null) {
            Collections.addAll(values, params);
        }
        return Collections.unmodifiableList(values);
    }

    /**
     * Defines the AwaitablePredicate interface.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    @FunctionalInterface
    public interface AwaitablePredicate<T> {

        /**
         * Verifies the test behavior.
         *
         * @param value predicate input
         * @return the result
         */
        CompletionStage<Boolean> test(T value);

        /**
         * Returns the of.
         *
         * @param predicate predicate value
         * @return of value
         */
        static <T> AwaitablePredicate<T> of(Predicate<T> predicate) {
            return value -> CompletableFuture.completedFuture(predicate != null && predicate.test(value));
        }
    }

    /**
     * Defines the Moveable interface.
     *
     * @param <T> the generic type handled by this member
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public interface Moveable<T> {

        /**
         * Dispatches a move input action.
         *
         * @return move value
         */
        T move();
    }

    /**
     * Defines the Disposed interface.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public interface Disposed {

        /**
         * Returns the disposed.
         *
         * @return {@code true} when the condition matches
         */
        boolean disposed();
    }

    /**
     * Defines the AwaitableIterable interface.
     *
     * @param <T> the generic type handled by this member
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public interface AwaitableIterable<T> extends Iterable<T> {
    }

    /**
     * Defines the EvaluateFunc interface.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    @FunctionalInterface
    public interface EvaluateFunc {

        /**
         * Returns the evaluate.
         *
         * @param params protocol parameters
         * @return completion future
         */
        CompletionStage<Object> evaluate(List<Object> params);
    }

    /**
     * Defines the EvaluateFuncWith interface.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    @FunctionalInterface
    public interface EvaluateFuncWith<V> {

        /**
         * Returns the evaluate.
         *
         * @param value  value to use
         * @param params protocol parameters
         * @return completion future
         */
        CompletionStage<Object> evaluate(V value, List<Object> params);
    }

    /**
     * Defines the BindingPayload class.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class BindingPayload {

        /**
         * Current type.
         */
        private final String type;
        /**
         * Current name.
         */
        private final String name;
        /**
         * Current seq.
         */
        private final long seq;
        /**
         * Registered args values.
         */
        private final List<Object> args;
        /**
         * Whether trivial is enabled.
         */
        private final boolean trivial;

        /**
         * Creates an instance.
         *
         * @param type      type to use
         * @param name      name to use
         * @param seq       seq value
         * @param args      arguments to pass
         * @param isTrivial is trivial value
         */
        public BindingPayload(String type, String name, long seq, List<?> args, boolean isTrivial) {
            this.type = type;
            this.name = name;
            this.seq = seq;
            this.args = copyArgs(args);
            this.trivial = isTrivial;
        }

        /**
         * Returns the type.
         *
         * @return type value
         */
        public String type() {
            return type;
        }

        /**
         * Returns the name.
         *
         * @return name value
         */
        public String name() {
            return name;
        }

        /**
         * Returns the seq.
         *
         * @return seq value
         */
        public long seq() {
            return seq;
        }

        /**
         * Returns the args.
         *
         * @return values
         */
        public List<Object> args() {
            return args;
        }

        /**
         * Returns whether this value can be passed without a remote handle.
         *
         * @return the result
         */
        public boolean isTrivial() {
            return trivial;
        }

        /**
         * Converts this value to protocol parameters.
         *
         * @return the result
         */
        public Map<String, Object> toMap() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", type);
            result.put("name", name);
            result.put("seq", seq);
            result.put("args", args);
            result.put("isTrivial", trivial);
            return result;
        }

        /**
         * Returns the copy args.
         *
         * @param args arguments to pass
         * @return values
         */
        private static List<Object> copyArgs(List<?> args) {
            if (args == null || args.isEmpty()) {
                return List.of();
            }
            return Collections.unmodifiableList(new ArrayList<>(args));
        }
    }

}
