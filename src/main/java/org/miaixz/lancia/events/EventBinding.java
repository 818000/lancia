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
package org.miaixz.lancia.events;

import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.lancia.Binding;

/**
 * Provides a one-shot event binding handle.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class EventBinding implements Binding {

    /**
     * No-op unbind action.
     */
    private static final Runnable NOOP = () -> {
    };

    /**
     * Action invoked when the binding is removed.
     */
    private final Runnable unbindAction;

    /**
     * Tracks whether the unbind action has already run.
     */
    private final AtomicBoolean unbound = new AtomicBoolean(false);

    /**
     * Creates a no-op event binding.
     */
    public EventBinding() {
        this(NOOP);
    }

    /**
     * Creates an event binding with the provided removal action.
     *
     * @param unbindAction action invoked once during unbind
     */
    public EventBinding(Runnable unbindAction) {
        this.unbindAction = Assert.notNull(unbindAction, "unbindAction");
    }

    /**
     * Removes the attached listener once.
     */
    @Override
    public void unbind() {
        if (unbound.compareAndSet(false, true)) {
            unbindAction.run();
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

}
