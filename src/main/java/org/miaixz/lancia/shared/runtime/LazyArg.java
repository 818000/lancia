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

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.lancia.kernel.Handle;

/**
 * Defines the LazyArg class.
 *
 * @param <T> the generic type handled by this member
 * @param <C> the generic type handled by this member
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class LazyArg<T, C> {

    /**
     * Current resolver.
     */
    private final Resolver<T, C> resolver;

    /**
     * Creates a lazy arg.
     *
     * @param resolver the resolver value
     */
    private LazyArg(Resolver<T, C> resolver) {
        this.resolver = Assert.notNull(resolver, "resolver");
    }

    /**
     * Returns the create.
     *
     * @param resolver resolver value
     * @param <T>      resolved value type
     * @param <C>      execution context type
     * @param <H>      handle type
     * @return create value
     */
    public static <T, C, H extends Handle> LazyArg<T, InjectedRuntime<C, H>> create(
            Resolver<T, InjectedRuntime<C, H>> resolver) {
        return new LazyArg<>(resolver);
    }

    /**
     * Creates with context.
     *
     * @param resolver the resolver value
     * @param <T>      the generic type handled by this member
     * @param <C>      the generic type handled by this member
     * @return the result
     */
    public static <T, C> LazyArg<T, C> createWithContext(Resolver<T, C> resolver) {
        return new LazyArg<>(resolver);
    }

    /**
     * Returns the get.
     *
     * @param context browser context
     * @return get value
     */
    public T get(C context) {
        try {
            return resolver.resolve(context);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalException("Failed to resolve LazyArg.", ex);
        }
    }

    /**
     * Returns the context.
     *
     * @param executionContext execution context value
     * @param <C>              execution context type
     * @param <H>              handle type
     * @return context value
     */
    public static <C, H extends Handle> InjectedRuntime<C, H> context(C executionContext) {
        return new InjectedRuntime<>(executionContext, null);
    }

    /**
     * Returns the context.
     *
     * @param executionContext execution context value
     * @param puppeteerUtil    puppeteer util value
     * @param <C>              execution context type
     * @param <H>              handle type
     * @return context value
     */
    public static <C, H extends Handle> InjectedRuntime<C, H> context(C executionContext, H puppeteerUtil) {
        return new InjectedRuntime<>(executionContext, puppeteerUtil);
    }

    /**
     * Defines the Resolver interface.
     *
     * @param <T> resolved value type
     * @param <C> context type
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    @FunctionalInterface
    public interface Resolver<T, C> {

        /**
         * Returns the resolve.
         *
         * @param context browser context
         * @return resolve value
         * @throws Exception if the operation fails
         */
        T resolve(C context) throws Exception;
    }

    /**
     * Defines the InjectedRuntime class.
     *
     * @param <C> execution context type
     * @param <H> handle type
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class InjectedRuntime<C, H extends Handle> {

        /**
         * Current execution context.
         */
        private final C executionContext;
        /**
         * Current puppeteer util.
         */
        private final H puppeteerUtil;

        /**
         * Creates an instance.
         *
         * @param executionContext execution context value
         * @param puppeteerUtil    puppeteer util value
         */
        private InjectedRuntime(C executionContext, H puppeteerUtil) {
            this.executionContext = executionContext;
            this.puppeteerUtil = puppeteerUtil;
        }

        /**
         * Returns the execution context.
         *
         * @return execution context value
         */
        public C executionContext() {
            return executionContext;
        }

        /**
         * Returns the puppeteer util.
         *
         * @return puppeteer util value
         */
        public H puppeteerUtil() {
            return puppeteerUtil;
        }
    }

}
