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
package org.miaixz.lancia.kernel.cdp.runtime;

import java.util.ArrayList;
import java.util.List;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.protocol.CdpRuntime;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.shared.page.ConsoleMessage;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Provides CDP helpers that need kernel runtime objects.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class CdpRuntimeValues {

    /**
     * Creates a CDP runtime values.
     */
    private CdpRuntimeValues() {
        // No initialization required.
    }

    /**
     * Creates a console message from a CDP runtime event.
     *
     * @param session the CDP session
     * @param event   the CDP event payload
     * @param values  JavaScript handles
     * @return console message
     */
    public static ConsoleMessage createConsoleMessage(CDPSession session, CdpPayload event, List<CdpJSHandle> values) {
        return createConsoleMessage(session, event, values, Normal.EMPTY);
    }

    /**
     * Creates a console message from a CDP runtime event.
     *
     * @param session  the CDP session
     * @param event    the CDP event payload
     * @param values   JavaScript handles
     * @param targetId the target id
     * @return console message
     */
    public static ConsoleMessage createConsoleMessage(
            CDPSession session,
            CdpPayload event,
            List<CdpJSHandle> values,
            String targetId) {
        CdpPayload actualEvent = event == null ? CdpPayload.NULL : event;
        List<CdpJSHandle> actualValues = values == null ? List.of() : List.copyOf(values);
        List<String> textTokens = new ArrayList<>();
        for (CdpJSHandle value : actualValues) {
            Object token = valueFromJSHandle(value);
            textTokens.add(token == null ? Normal.EMPTY : String.valueOf(token));
        }
        CdpPayload stackTrace = actualEvent.get("stackTrace");
        return new ConsoleMessage(CdpRuntime.convertConsoleMessageLevel(PayloadReader.text(actualEvent.get("type"))),
                String.join(Symbol.SPACE, textTokens), actualValues, ConsoleMessage.locationsFromStackTrace(stackTrace),
                null, stackTrace, targetId);
    }

    /**
     * Extracts a printable value from a JavaScript handle.
     *
     * @param handle the JavaScript handle
     * @return extracted value
     */
    public static Object valueFromJSHandle(CdpJSHandle handle) {
        CdpPayload remoteObject = Assert.notNull(handle, "handle").remoteObject();
        if (StringKit.isNotBlank(PayloadReader.text(remoteObject.get("objectId")))) {
            return valueFromRemoteObjectReference(handle);
        }
        return CdpRuntime.valueFromPrimitiveRemoteObject(remoteObject);
    }

    /**
     * Extracts a printable value from a remote object reference.
     *
     * @param handle the JavaScript handle
     * @return printable reference text
     */
    public static String valueFromRemoteObjectReference(CdpJSHandle handle) {
        CdpPayload remoteObject = Assert.notNull(handle, "handle").remoteObject();
        if (StringKit.isBlank(PayloadReader.text(remoteObject.get("objectId")))) {
            throw new InternalException("Cannot extract value when no objectId is given");
        }
        String description = PayloadReader.text(remoteObject.get("description"));
        if ("error".equals(PayloadReader.text(remoteObject.get("subtype"))) && StringKit.isNotBlank(description)) {
            int newline = description.indexOf(Symbol.C_LF);
            return newline < Normal._0 ? description : description.substring(Normal._0, newline);
        }
        String type = StringKit.isNotBlank(PayloadReader.text(remoteObject.get("subtype")))
                ? PayloadReader.text(remoteObject.get("subtype"))
                : PayloadReader.text(remoteObject.get("type"));
        String className = PayloadReader.text(remoteObject.get("className"));
        return Symbol.BRACKET_LEFT + type + (StringKit.isBlank(className) ? Normal.EMPTY : Symbol.SPACE + className)
                + Symbol.BRACKET_RIGHT;
    }

}
