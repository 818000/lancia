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
package org.miaixz.lancia.shared.page;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.lancia.Payload;
import org.miaixz.lancia.kernel.Element;
import org.miaixz.lancia.kernel.FileChooser;

/**
 * Represents a file chooser request from the page.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class FileChooserRequest implements FileChooser {

    /**
     * Current element.
     */
    private final Element element;
    /**
     * Whether multiple is enabled.
     */
    private final boolean multiple;
    /**
     * Thread-safe handled state.
     */
    private final AtomicBoolean handled = new AtomicBoolean();

    /**
     * Creates a file chooser.
     *
     * @param element  element handle
     * @param multiple multiple
     */
    public FileChooserRequest(Element element, boolean multiple) {
        this.element = Assert.notNull(element, "element");
        this.multiple = multiple;
    }

    /**
     * Returns whether multiple files are accepted.
     *
     * @return {@code true} when the condition matches
     */
    public boolean isMultiple() {
        return multiple;
    }

    /**
     * Handles d.
     *
     * @return handled state
     */
    public boolean handled() {
        return handled.get();
    }

    /**
     * Returns the accept.
     *
     * @param paths paths value
     * @return completion future
     */
    public CompletableFuture<? extends Payload> accept(List<String> paths) {
        List<String> actualPaths = paths == null ? List.of() : List.copyOf(paths);
        return accept(actualPaths.toArray(String[]::new));
    }

    /**
     * Returns the accept.
     *
     * @param paths paths value
     * @return completion future
     */
    public CompletableFuture<? extends Payload> accept(String... paths) {
        markHandled("Cannot accept FileChooserRequest which is already handled!");
        return element.uploadFile(paths);
    }

    /**
     * Returns whether cel is available.
     *
     * @return {@code true} when the condition matches
     */
    public CompletableFuture<Void> cancel() {
        markHandled("Cannot cancel FileChooserRequest which is already handled!");
        element.evaluate("function(){this.dispatchEvent(new Event('cancel',{bubbles:true}));}");
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Handles mark handled.
     *
     * @param message message text
     */
    private void markHandled(String message) {
        if (!handled.compareAndSet(false, true)) {
            throw new InternalException(message);
        }
    }

}
