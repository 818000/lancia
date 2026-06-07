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
package org.miaixz.lancia.kernel.cdp.device;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.core.lang.thread.NamedThreadFactory;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.nimble.device.DeviceRequest;
import org.miaixz.lancia.shared.payload.PayloadDeviceRequest;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Tracks device request prompt waiters for a page.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpDevicePromptManager {

    /**
     * Executor used for lifecycle timeout tasks.
     */
    private static final ScheduledExecutorService TIMEOUT_EXECUTOR = Executors
            .newSingleThreadScheduledExecutor(new NamedThreadFactory("lancia-device-prompt-", true));
    /**
     * Current session.
     */
    private CDPSession session;
    /**
     * Registered pending prompts values.
     */
    private final Set<CompletableFuture<CdpDevicePrompt>> pendingPrompts = ConcurrentHashMap.newKeySet();

    /**
     * Creates a device request prompt manager.
     *
     * @param session protocol session
     */
    public CdpDevicePromptManager(CDPSession session) {
        this.session = session;
        if (session != null) {
            session.on("DeviceAccess.deviceRequestPrompted", this::onPrompted);
            session.on("Target.detachedFromTarget", ignored -> {
                this.session = null;
                failPending(new InternalException("Cannot wait for device prompt through detached session!"));
            });
        }
    }

    /**
     * Waits for device prompt.
     *
     * @param timeout timeout value
     * @return wait for device prompt value
     */
    public CompletableFuture<CdpDevicePrompt> waitForDevicePrompt(Duration timeout) {
        if (session == null) {
            CompletableFuture<CdpDevicePrompt> rejected = new CompletableFuture<>();
            rejected.completeExceptionally(
                    new InternalException("Cannot wait for device prompt through detached session!"));
            return rejected;
        }
        boolean needsEnable = pendingPrompts.isEmpty();
        if (needsEnable) {
            session.send("DeviceAccess.enable", Map.of());
        }
        CompletableFuture<CdpDevicePrompt> future = new CompletableFuture<>();
        pendingPrompts.add(future);
        Duration actualTimeout = timeout == null ? Duration.ofSeconds(30L) : timeout;
        ScheduledFuture<?> timeoutFuture = TIMEOUT_EXECUTOR.schedule(() -> {
            if (future.completeExceptionally(new TimeoutException("Timed out waiting for device request."))) {
                pendingPrompts.remove(future);
            }
        }, actualTimeout.toMillis(), TimeUnit.MILLISECONDS);
        future.whenComplete((value, error) -> {
            timeoutFuture.cancel(false);
            pendingPrompts.remove(future);
        });
        return future;
    }

    /**
     * Handles on prompted.
     *
     * @param params protocol parameters
     */
    private void onPrompted(CdpPayload params) {
        if (pendingPrompts.isEmpty() || session == null) {
            return;
        }
        List<DeviceRequest> devices = params.get("devices").elements().stream().map(PayloadDeviceRequest::from)
                .toList();
        CdpDevicePrompt prompt = new CdpDevicePrompt(session, PayloadReader.text(params.get("id")), devices);
        for (CompletableFuture<CdpDevicePrompt> future : List.copyOf(pendingPrompts)) {
            future.complete(prompt);
        }
        pendingPrompts.clear();
    }

    /**
     * Handles fail pending.
     *
     * @param throwable throwable value
     */
    private void failPending(Throwable throwable) {
        for (CompletableFuture<CdpDevicePrompt> future : List.copyOf(pendingPrompts)) {
            future.completeExceptionally(throwable);
        }
        pendingPrompts.clear();
    }

}
