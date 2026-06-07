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

import org.miaixz.lancia.Payload;

/**
 * Resolves CDP remote object values.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class CdpRemoteObjectValue {

    /**
     * Prevents instantiation.
     */
    private CdpRemoteObjectValue() {
        // No initialization required.
    }

    /**
     * Resolves a remote object value.
     *
     * @param remoteObject remote object
     * @return value
     */
    public static Object from(Payload remoteObject) {
        if (remoteObject == null || remoteObject.isNull()) {
            return null;
        }
        Payload value = remoteObject.get("value");
        if (!value.isNull()) {
            return value.raw();
        }
        Payload unserializableValue = remoteObject.get("unserializableValue");
        if (!unserializableValue.isNull()) {
            return unserializableValue.raw();
        }
        return remoteObject.get("description").raw();
    }

}
