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
package org.miaixz.lancia;

import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;

/**
 * Represents a removable event listener binding.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Binding {

    /**
     * Returns whether this object is unbound.
     *
     * @return {@code true} when the condition matches
     */
    boolean isUnbound();

    /**
     * Removes the listener attached by this binding.
     */
    void unbind();

    /**
     * Combines this binding with another binding.
     *
     * @param binding binding
     * @return combined binding
     */
    default Binding combine(Binding binding) {
        if (binding == null) {
            return this;
        }
        Binding current = this;
        return new Binding() {

            /**
             * Tracks whether the combined binding has already run.
             */
            private final AtomicBoolean unbound = new AtomicBoolean(false);

            /**
             * Removes both bindings once.
             */
            @Override
            public void unbind() {
                if (unbound.compareAndSet(false, true)) {
                    current.unbind();
                    binding.unbind();
                }
            }

            /**
             * Returns whether this binding has already been removed.
             *
             * @return {@code true} when unbind has already run
             */
            @Override
            public boolean isUnbound() {
                return unbound.get();
            }
        };
    }

    /**
     * Removes this binding once.
     *
     * @param disposed disposed marker
     * @return {@code true} when unbind ran
     */
    default boolean unbind(AtomicBoolean disposed) {
        if (Assert.notNull(disposed, "disposed").compareAndSet(false, true)) {
            unbind();
            return true;
        }
        return false;
    }

}
