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
package org.miaixz.lancia.shared.query;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.exception.InternalException;

/**
 * Represents script injector.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class ScriptInjector {

    /**
     * Classpath root for injected runtime resources.
     */
    private static final String RESOURCE_BASE = "META-INF/lancia/injected/";
    /**
     * Injected resource load order.
     */
    private static final List<String> RUNTIME_RESOURCES = List.of(
            "TextContent.js",
            "TextQuerySelector.js",
            "CSSQuerySelector.js",
            "XPathQuerySelector.js",
            "PierceQuerySelector.js",
            "ARIAQuerySelector.js",
            "CustomQuerySelector.js",
            "PQuerySelector.js",
            "Poller.js",
            "injected.js");
    /**
     * Full injected runtime source.
     */
    private static final String RUNTIME_SOURCE = loadRuntimeSource();
    /**
     * Shared constant for shared.
     */
    private static final ScriptInjector SHARED = new ScriptInjector();
    /**
     * Whether updated is enabled.
     */
    private boolean updated;
    /**
     * Registered amendments values.
     */
    private final Set<String> amendments = new LinkedHashSet<>();

    /**
     * Creates a script injector.
     */
    public ScriptInjector() {
        // No initialization required.
    }

    /**
     * Returns the shared.
     *
     * @return shared value
     */
    public static ScriptInjector shared() {
        return SHARED;
    }

    /**
     * Returns the injected runtime source.
     *
     * @return runtime source
     */
    public static String runtimeSource() {
        return RUNTIME_SOURCE;
    }

    /**
     * Wraps a JavaScript expression with the injected runtime.
     *
     * @param expression expression to evaluate
     * @return wrapped expression
     */
    public static String expression(String expression) {
        return block("return (" + expression + ");");
    }

    /**
     * Wraps a JavaScript expression with the injected runtime and awaits it.
     *
     * @param expression expression to evaluate
     * @return wrapped async expression
     */
    public static String asyncExpression(String expression) {
        return asyncBlock("return await (" + expression + ");");
    }

    /**
     * Wraps a JavaScript block with the injected runtime.
     *
     * @param body JavaScript statements
     * @return wrapped expression
     */
    public static String block(String body) {
        return "(()=>{" + installRuntimeStatement() + "const Lancia=globalThis.__lanciaRuntime;" + body + "})()";
    }

    /**
     * Wraps an async JavaScript block with the injected runtime.
     *
     * @param body JavaScript statements
     * @return wrapped async expression
     */
    public static String asyncBlock(String body) {
        return "(async()=>{" + installRuntimeStatement() + "const Lancia=globalThis.__lanciaRuntime;" + body + "})()";
    }

    /**
     * Handles append.
     *
     * @param statement statement value
     */
    public synchronized void append(String statement) {
        update(() -> amendments.add(Assert.notBlank(statement, "statement")));
    }

    /**
     * Handles pop.
     *
     * @param statement statement value
     */
    public synchronized void pop(String statement) {
        update(() -> amendments.remove(statement));
    }

    /**
     * Handles clear.
     */
    public synchronized void clear() {
        update(amendments::clear);
    }

    /**
     * Handles inject.
     *
     * @param inject inject value
     */
    public void inject(Consumer<String> inject) {
        inject(inject, false);
    }

    /**
     * Handles inject.
     *
     * @param inject inject value
     * @param force  force value
     */
    public synchronized void inject(Consumer<String> inject, boolean force) {
        Consumer<String> actualInject = Assert.notNull(inject, "inject");
        if (updated || force) {
            actualInject.accept(source());
        }
        updated = false;
    }

    /**
     * Returns the source.
     *
     * @return source value
     */
    public synchronized String source() {
        StringBuilder builder = new StringBuilder();
        builder.append("(()=>{").append(RUNTIME_SOURCE).append("const Lancia=globalThis.__lanciaRuntime;");
        for (String amendment : amendments) {
            builder.append("(").append(amendment).append(")(Lancia);");
        }
        builder.append("return Lancia;})()");
        return builder.toString();
    }

    /**
     * Updates d.
     *
     * @return updated value
     */
    public synchronized boolean updated() {
        return updated;
    }

    /**
     * Returns the amendments.
     *
     * @return values
     */
    public synchronized List<String> amendments() {
        return List.copyOf(amendments);
    }

    /**
     * Handles update.
     *
     * @param callback callback to invoke
     */
    private void update(Runnable callback) {
        callback.run();
        updated = true;
    }

    /**
     * Builds the runtime installation guard.
     *
     * @return runtime installation statement
     */
    private static String installRuntimeStatement() {
        return "if(!globalThis.__lanciaRuntime){" + RUNTIME_SOURCE + "}";
    }

    /**
     * Loads all injected runtime resources.
     *
     * @return runtime source
     */
    private static String loadRuntimeSource() {
        StringBuilder builder = new StringBuilder();
        for (String resource : RUNTIME_RESOURCES) {
            builder.append(loadResource(resource)).append('\n');
        }
        return builder.toString();
    }

    /**
     * Loads one injected runtime resource.
     *
     * @param resource resource file name
     * @return resource source
     */
    private static String loadResource(String resource) {
        String path = RESOURCE_BASE + resource;
        ClassLoader loader = ScriptInjector.class.getClassLoader();
        try (InputStream input = loader.getResourceAsStream(path)) {
            if (input == null) {
                throw new InternalException("Missing injected runtime resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new InternalException("Failed to load injected runtime resource: " + path, ex);
        }
    }

}
