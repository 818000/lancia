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
package org.miaixz.lancia.kernel;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.miaixz.bus.core.lang.Optional;
import org.miaixz.lancia.Payload;

/**
 * Represents a remote JavaScript object handle.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public interface Handle {

    /**
     * Returns the remote object id.
     *
     * @return object id
     */
    String id();

    /**
     * Returns the raw remote object payload.
     *
     * @return remote object
     */
    Payload remoteObject();

    /**
     * Returns the disposed.
     *
     * @return {@code true} when the condition matches
     */
    boolean disposed();

    /**
     * Returns the remote JavaScript object as a local representation.
     *
     * @return local representation of the remote JavaScript object
     */
    Object jsonValue();

    /**
     * Returns this handle as an element handle when possible.
     *
     * @return element handle or {@code null}
     */
    Element asElement();

    /**
     * Releases this handle.
     *
     * @return release command future
     */
    CompletableFuture<? extends Payload> dispose();

    /**
     * Returns the property.
     *
     * @param name name to use
     * @return optional value
     */
    Optional<? extends Handle> getProperty(String name);

    /**
     * Returns the properties.
     *
     * @return mapped values
     */
    Map<String, ? extends Handle> getProperties();

    /**
     * Evaluates a function on this handle.
     *
     * @param functionDeclaration function declaration
     * @return evaluation result
     */
    Object evaluate(String functionDeclaration);

    /**
     * Evaluates a function on this handle and returns a handle.
     *
     * @param functionDeclaration function declaration
     * @return handle result
     */
    Handle evaluateHandle(String functionDeclaration);

}
