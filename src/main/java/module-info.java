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
/**
 * Defines the Lancia browser automation module.
 *
 * @author Kimi Liu
 * @since Java 21+
 */
module lancia {

    requires java.desktop;
    requires java.management;
    requires java.net.http;
    requires java.sql;

    requires bus.all;

    requires static lombok;
    requires static com.sun.jna;
    requires static com.sun.jna.platform;

    exports org.miaixz.lancia;
    exports org.miaixz.lancia.browser;
    exports org.miaixz.lancia.browser.bundle;
    exports org.miaixz.lancia.browser.launch;
    exports org.miaixz.lancia.browser.metadata;
    exports org.miaixz.lancia.browser.supervisor;
    exports org.miaixz.lancia.events;
    exports org.miaixz.lancia.kernel;
    exports org.miaixz.lancia.kernel.bidi;
    exports org.miaixz.lancia.kernel.bidi.accessor;
    exports org.miaixz.lancia.kernel.bidi.browser;
    exports org.miaixz.lancia.kernel.bidi.device;
    exports org.miaixz.lancia.kernel.bidi.emulation;
    exports org.miaixz.lancia.kernel.bidi.input;
    exports org.miaixz.lancia.kernel.bidi.network;
    exports org.miaixz.lancia.kernel.bidi.page;
    exports org.miaixz.lancia.kernel.bidi.protocol;
    exports org.miaixz.lancia.kernel.bidi.protocol.message;
    exports org.miaixz.lancia.kernel.bidi.runtime;
    exports org.miaixz.lancia.kernel.bidi.session;
    exports org.miaixz.lancia.kernel.bidi.targets;
    exports org.miaixz.lancia.kernel.bidi.transport;
    exports org.miaixz.lancia.kernel.bidi.worker;
    exports org.miaixz.lancia.kernel.cdp;
    exports org.miaixz.lancia.kernel.cdp.accessibility;
    exports org.miaixz.lancia.kernel.cdp.auth;
    exports org.miaixz.lancia.kernel.cdp.browser;
    exports org.miaixz.lancia.kernel.cdp.coverage;
    exports org.miaixz.lancia.kernel.cdp.device;
    exports org.miaixz.lancia.kernel.cdp.emulation;
    exports org.miaixz.lancia.kernel.cdp.input;
    exports org.miaixz.lancia.kernel.cdp.mcp;
    exports org.miaixz.lancia.kernel.cdp.network;
    exports org.miaixz.lancia.kernel.cdp.page;
    exports org.miaixz.lancia.kernel.cdp.protocol;
    exports org.miaixz.lancia.kernel.cdp.runtime;
    exports org.miaixz.lancia.kernel.cdp.screen;
    exports org.miaixz.lancia.kernel.cdp.session;
    exports org.miaixz.lancia.kernel.cdp.targets;
    exports org.miaixz.lancia.kernel.cdp.tracing;
    exports org.miaixz.lancia.kernel.cdp.transport;
    exports org.miaixz.lancia.kernel.cdp.worker;
    exports org.miaixz.lancia.nimble;
    exports org.miaixz.lancia.nimble.browser;
    exports org.miaixz.lancia.nimble.device;
    exports org.miaixz.lancia.nimble.emulation;
    exports org.miaixz.lancia.nimble.input;
    exports org.miaixz.lancia.nimble.mcp;
    exports org.miaixz.lancia.nimble.network;
    exports org.miaixz.lancia.nimble.screen;
    exports org.miaixz.lancia.options;
    exports org.miaixz.lancia.runtime;
    exports org.miaixz.lancia.shared;
    exports org.miaixz.lancia.shared.async;
    exports org.miaixz.lancia.shared.frame;
    exports org.miaixz.lancia.shared.input;
    exports org.miaixz.lancia.shared.locator;
    exports org.miaixz.lancia.shared.page;
    exports org.miaixz.lancia.shared.payload;
    exports org.miaixz.lancia.shared.protocol;
    exports org.miaixz.lancia.shared.query;
    exports org.miaixz.lancia.shared.resource;
    exports org.miaixz.lancia.shared.runtime;

}
