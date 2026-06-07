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
package org.miaixz.lancia.kernel.bidi.protocol;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.exception.ProtocolException;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.lancia.kernel.bidi.protocol.BidiLogMapper.StackTraceLocation;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Maps WebDriver BiDi error payloads to runtime exceptions.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BidiExceptionMapper {

    /**
     * Creates a bidi exception mapper.
     */
    private BidiExceptionMapper() {
        // No initialization required.
    }

    /**
     * Creates evaluation error.
     *
     * @param details exception details
     * @return created evaluation error
     */
    public static RuntimeException createEvaluationError(CdpPayload details) {
        CdpPayload exception = details.get("exception");
        String type = PayloadReader.text(exception.get("type"));
        if ("object".equals(type) && exception.get("value").isNull()) {
            return new InternalException(PayloadReader.text(details.get("text")));
        }
        if (!"error".equals(type)) {
            return new InternalException(String.valueOf(BidiDeserializer.deserialize(exception)));
        }
        String text = PayloadReader.text(details.get("text"));
        String message = text.contains(": ") ? text.substring(text.indexOf(": ") + 2) : text;
        StringBuilder stack = new StringBuilder(text);
        for (StackTraceLocation location : BidiLogMapper.stackTraceLocations(details.get("stackTrace"))) {
            stack.append(Symbol.C_LF).append("    at ").append(location.url()).append(Symbol.C_COLON)
                    .append(location.lineNumber()).append(Symbol.C_COLON).append(location.columnNumber());
        }
        return new InternalException(message + Symbol.C_LF + stack);
    }

    /**
     * Returns the rewrite navigation error.
     *
     * @param message message text
     * @param ms      ms value
     * @param error   error to propagate
     * @return rewrite navigation error value
     */
    public static RuntimeException rewriteNavigationError(String message, long ms, Throwable error) {
        if (error instanceof TimeoutException) {
            return new TimeoutException("Navigation timeout of " + ms + " ms exceeded");
        }
        if (error instanceof ProtocolException) {
            return new ProtocolException(error.getMessage() + " at " + message);
        }
        if (error instanceof RuntimeException runtime) {
            return runtime;
        }
        return new InternalException(error == null ? Normal.EMPTY : error.getMessage(), error);
    }

    /**
     * Returns the rewrite evaluation error.
     *
     * @param error error to propagate
     * @return rewrite evaluation error value
     */
    public static RuntimeException rewriteEvaluationError(Throwable error) {
        String message = error == null ? Normal.EMPTY : error.getMessage();
        if (message.contains("ExecutionContext was destroyed")
                || message.contains("Inspected target navigated or closed")) {
            return new InternalException("Execution context was destroyed, most likely because of a navigation.");
        }
        if (error instanceof RuntimeException runtime) {
            return runtime;
        }
        return new InternalException(message, error);
    }

}
