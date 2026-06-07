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
package org.miaixz.lancia.kernel.bidi.device;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.core.lang.thread.NamedThreadFactory;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.kernel.Prompts;
import org.miaixz.lancia.kernel.bidi.accessor.BidiSession;
import org.miaixz.lancia.kernel.bidi.session.BidiProtocolSession;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.nimble.device.DeviceRequest;
import org.miaixz.lancia.options.WaitForOptions;
import org.miaixz.lancia.shared.async.Awaitable;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents a BiDi device request prompt.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class BidiDeviceRequestPrompt implements Prompts {

    /**
     * Coordinates device prompt waiting for a browsing context.
     */
    public interface Manager {

        /**
         * Waits for the next device request prompt.
         *
         * @param timeout timeout value
         * @return completion future
         */
        CompletableFuture<BidiDeviceRequestPrompt> waitForDevicePrompt(Duration timeout);

    }

    /**
     * Creates a device request prompt manager.
     *
     * @param contextId context identifier
     * @param session   protocol session
     * @return device request prompt manager
     */
    public static Manager manager(String contextId, BidiSession session) {
        return new BidiDeviceRequestPromptManager(contextId, session);
    }

    /**
     * Creates a device request prompt manager.
     *
     * @param contextId context identifier
     * @param session   protocol session
     * @return device request prompt manager
     */
    public static Manager manager(String contextId, BidiProtocolSession session) {
        return new BidiDeviceRequestPromptManager(contextId, session);
    }

    /**
     * Shared constant for request device prompt updated.
     */
    static final String REQUEST_DEVICE_PROMPT_UPDATED = "bluetooth.requestDevicePromptUpdated";
    /**
     * Shared constant for handle request device prompt.
     */
    private static final String HANDLE_REQUEST_DEVICE_PROMPT = "bluetooth.handleRequestDevicePrompt";
    /**
     * Shared constant for unknown device name.
     */
    private static final String UNKNOWN_DEVICE_NAME = "UNKNOWN";
    /**
     * Default wait for device timeout.
     */
    private static final Duration DEFAULT_WAIT_FOR_DEVICE_TIMEOUT = Duration.ofSeconds(30L);
    /**
     * Shared constant for device waiter timer.
     */
    private static final ScheduledExecutorService DEVICE_WAITER_TIMER = Executors
            .newSingleThreadScheduledExecutor(new NamedThreadFactory("lancia-bidi-device-waiter-", true));
    /**
     * Current session.
     */
    private final BidiSession session;
    /**
     * Current context ID.
     */
    private final String contextId;
    /**
     * Current prompt ID.
     */
    private final String promptId;
    /**
     * Registered devices values.
     */
    private final List<DeviceRequest> devices = new ArrayList<>();
    /**
     * Registered waiters values.
     */
    private final Set<DeviceWaiter> waiters = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /**
     * Thread-safe settled state.
     */
    private final AtomicBoolean settled = new AtomicBoolean(false);
    /**
     * Current binding.
     */
    private final Binding binding;

    /**
     * Creates a bidi device request prompt.
     *
     * @param contextId context id
     * @param promptId  prompt id
     * @param session   protocol session
     * @param devices   devices
     */
    public BidiDeviceRequestPrompt(String contextId, String promptId, BidiSession session,
            List<DeviceRequest> devices) {
        this.contextId = Assert.notBlank(contextId, "contextId");
        this.promptId = Assert.notBlank(promptId, "promptId");
        this.session = Assert.notNull(session, "session");
        if (devices != null) {
            this.devices.addAll(devices);
        }
        this.binding = this.session.connection().on(REQUEST_DEVICE_PROMPT_UPDATED, this::onPromptUpdated);
    }

    /**
     * Creates a bidi device request prompt.
     *
     * @param contextId context id
     * @param promptId  prompt id
     * @param session   protocol session
     * @param devices   devices
     */
    public BidiDeviceRequestPrompt(String contextId, String promptId, BidiProtocolSession session,
            List<DeviceRequest> devices) {
        this(contextId, promptId, BidiSession.wrap(session), devices);
    }

    /**
     * Returns whether cel is available.
     *
     * @return {@code true} when the condition matches
     */
    public CompletableFuture<CdpPayload> cancel() {
        settleWaiters(new IllegalStateException("Device prompt has been cancelled."));
        return session
                .send(HANDLE_REQUEST_DEVICE_PROMPT, Map.of("context", contextId, "prompt", promptId, "accept", false));
    }

    /**
     * Returns the select.
     *
     * @param device device value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> select(DeviceRequest device) {
        DeviceRequest actualDevice = Assert.notNull(device, "device");
        settleWaiters(new IllegalStateException("Device prompt has been handled."));
        return session.send(
                HANDLE_REQUEST_DEVICE_PROMPT,
                Map.of("context", contextId, "prompt", promptId, "accept", true, "device", actualDevice.id()));
    }

    /**
     * Waits for device.
     *
     * @param filter  filter
     * @param timeout timeout value
     * @return wait for device value
     */
    public CompletableFuture<DeviceRequest> waitForDevice(Predicate<DeviceRequest> filter, Duration timeout) {
        return waitForDevice(filter, new WaitForOptions(timeout));
    }

    /**
     * Waits for device.
     *
     * @param filter  filter
     * @param options operation options
     * @return wait for device value
     */
    public CompletableFuture<DeviceRequest> waitForDevice(Predicate<DeviceRequest> filter, WaitForOptions options) {
        Predicate<DeviceRequest> actualFilter = filter == null ? device -> true : filter;
        if (settled.get()) {
            return Awaitable.failed(new IllegalStateException("Device prompt has been handled."));
        }
        synchronized (devices) {
            for (DeviceRequest device : devices) {
                if (actualFilter.test(device)) {
                    return CompletableFuture.completedFuture(device);
                }
            }
        }
        CompletableFuture<DeviceRequest> future = new CompletableFuture<>();
        DeviceWaiter waiter = new DeviceWaiter(actualFilter, future);
        waiters.add(waiter);
        waiter.schedule(timeoutOf(options));
        future.whenComplete((value, throwable) -> {
            waiters.remove(waiter);
            waiter.cancelTimeout();
        });
        return future;
    }

    /**
     * Waits for device.
     *
     * @param filter filter
     * @return wait for device value
     */
    public CompletableFuture<DeviceRequest> waitForDevice(Predicate<DeviceRequest> filter) {
        return waitForDevice(filter, (WaitForOptions) null);
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        return promptId;
    }

    /**
     * Returns the prompt ID.
     *
     * @return prompt ID value
     */
    public String promptId() {
        return promptId;
    }

    /**
     * Returns the context ID.
     *
     * @return context ID value
     */
    public String contextId() {
        return contextId;
    }

    /**
     * Returns the devices.
     *
     * @return values
     */
    public List<DeviceRequest> devices() {
        synchronized (devices) {
            return List.copyOf(devices);
        }
    }

    /**
     * Returns handled state.
     *
     * @return handled state
     */
    public boolean handled() {
        return settled.get();
    }

    /**
     * Handles on prompt updated.
     *
     * @param params protocol parameters
     */
    void onPromptUpdated(CdpPayload params) {
        if (!contextId.equals(PayloadReader.text(params.get("context")))
                || !promptId.equals(PayloadReader.text(params.get("prompt")))) {
            return;
        }
        List<DeviceRequest> updates = devices(params);
        synchronized (devices) {
            for (DeviceRequest update : updates) {
                if (devices.stream().noneMatch(device -> device.id().equals(update.id()))) {
                    devices.add(update);
                }
            }
        }
        notifyWaiters();
    }

    /**
     * Returns the devices.
     *
     * @param params protocol parameters
     * @return values
     */
    static List<DeviceRequest> devices(CdpPayload params) {
        CdpPayload payload = params.get("devices");
        if (!payload.isArray()) {
            return List.of();
        }
        List<DeviceRequest> result = new ArrayList<>();
        for (CdpPayload item : payload.elements()) {
            String name = PayloadReader.text(item.get("name"));
            result.add(
                    new DeviceRequest(PayloadReader.text(item.get("id")),
                            StringKit.isBlank(name) ? UNKNOWN_DEVICE_NAME : name));
        }
        return List.copyOf(result);
    }

    /**
     * Handles notify waiters.
     */
    private void notifyWaiters() {
        for (DeviceWaiter waiter : List.copyOf(waiters)) {
            synchronized (devices) {
                for (DeviceRequest device : devices) {
                    if (waiter.matches(device)) {
                        waiter.complete(device);
                    }
                }
            }
        }
    }

    /**
     * Updates tle waiters.
     *
     * @param throwable throwable value
     */
    private void settleWaiters(Throwable throwable) {
        if (!settled.compareAndSet(false, true)) {
            return;
        }
        binding.unbind();
        for (DeviceWaiter waiter : List.copyOf(waiters)) {
            waiter.completeExceptionally(throwable);
        }
        waiters.clear();
    }

    /**
     * Returns the timeout of.
     *
     * @param options operation options
     * @return timeout of value
     */
    private Duration timeoutOf(WaitForOptions options) {
        return options == null || options.timeout() == null ? DEFAULT_WAIT_FOR_DEVICE_TIMEOUT : options.timeout();
    }

    /**
     * Defines options for wait for operations.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class WaitForOptions {

        /**
         * Current timeout.
         */
        private Duration timeout;

        /**
         * Creates an instance.
         */
        public WaitForOptions() {
            // No initialization required.
        }

        /**
         * Creates an instance.
         *
         * @param timeout timeout value
         */
        public WaitForOptions(Duration timeout) {
            this.timeout = timeout;
        }

        /**
         * Returns the timeout.
         *
         * @return timeout value
         */
        public Duration timeout() {
            return timeout;
        }

        /**
         * Updates timeout.
         *
         * @param timeout timeout value
         */
        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

    }

    /**
     * Waits for device completion.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private static final class DeviceWaiter {

        /**
         * Current filter.
         */
        private final Predicate<DeviceRequest> filter;
        /**
         * Current future.
         */
        private final CompletableFuture<DeviceRequest> future;
        /**
         * Current timeout task.
         */
        private ScheduledFuture<?> timeoutTask;

        /**
         * Creates an instance.
         *
         * @param filter filter value
         * @param future future value
         */
        private DeviceWaiter(Predicate<DeviceRequest> filter, CompletableFuture<DeviceRequest> future) {
            this.filter = filter;
            this.future = future;
        }

        /**
         * Returns the matches.
         *
         * @param device device value
         * @return {@code true} when the condition matches
         */
        private boolean matches(DeviceRequest device) {
            return filter.test(device);
        }

        /**
         * Handles complete.
         *
         * @param device device value
         */
        private void complete(DeviceRequest device) {
            future.complete(device);
        }

        /**
         * Handles complete exceptionally.
         *
         * @param throwable throwable value
         */
        private void completeExceptionally(Throwable throwable) {
            future.completeExceptionally(throwable);
        }

        /**
         * Handles schedule.
         *
         * @param timeout timeout value
         */
        private void schedule(Duration timeout) {
            if (timeout == null || timeout.isZero() || timeout.isNegative()) {
                return;
            }
            timeoutTask = DEVICE_WAITER_TIMER.schedule(
                    () -> future.completeExceptionally(
                            new TimeoutException("Wait for BiDi device timed out: " + timeout.toMillis() + "ms")),
                    timeout.toMillis(),
                    TimeUnit.MILLISECONDS);
        }

        /**
         * Handles cancel timeout.
         */
        private void cancelTimeout() {
            if (timeoutTask != null) {
                timeoutTask.cancel(false);
            }
        }
    }

}

/**
 * Manages BiDi device request prompt state and protocol events.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
final class BidiDeviceRequestPromptManager implements BidiDeviceRequestPrompt.Manager {

    /**
     * Default wait for prompt timeout.
     */
    private static final Duration DEFAULT_WAIT_FOR_PROMPT_TIMEOUT = Duration.ofSeconds(30L);
    /**
     * Shared constant for device prompt timer.
     */
    private static final ScheduledExecutorService DEVICE_PROMPT_TIMER = Executors
            .newSingleThreadScheduledExecutor(new NamedThreadFactory("lancia-bidi-device-prompt-", true));
    /**
     * Current context ID.
     */
    private final String contextId;

    /**
     * BiDi session.
     */
    private final BidiSession session;
    /**
     * Thread-safe enabled state.
     */
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    /**
     * Creates an instance.
     *
     * @param contextId context ID value
     * @param session   protocol session
     */
    BidiDeviceRequestPromptManager(String contextId, BidiSession session) {
        this.contextId = Assert.notBlank(contextId, "contextId");
        this.session = Assert.notNull(session, "session");
    }

    /**
     * Creates an instance.
     *
     * @param contextId context ID value
     * @param session   protocol session
     */
    BidiDeviceRequestPromptManager(String contextId, BidiProtocolSession session) {
        this(contextId, BidiSession.wrap(session));
    }

    /**
     * Waits for device prompt.
     *
     * @param timeout timeout value
     * @return completion future
     */
    public CompletableFuture<BidiDeviceRequestPrompt> waitForDevicePrompt(Duration timeout) {
        CompletableFuture<BidiDeviceRequestPrompt> future = new CompletableFuture<>();
        Binding binding = session.connection().on(
                BidiDeviceRequestPrompt.REQUEST_DEVICE_PROMPT_UPDATED,
                params -> onRequestDevicePromptUpdated(params, future));
        future.whenComplete((value, throwable) -> binding.unbind());
        scheduleTimeout(future, timeout);
        enableIfNeeded().whenComplete((value, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    /**
     * Returns the enable if needed.
     *
     * @return completion future
     */
    private CompletableFuture<CdpPayload> enableIfNeeded() {
        if (enabled.compareAndSet(false, true)) {
            return session.subscribe(List.of(BidiDeviceRequestPrompt.REQUEST_DEVICE_PROMPT_UPDATED), List.of(contextId))
                    .thenApply(value -> CdpPayload.NULL).whenComplete((value, throwable) -> {
                        if (throwable != null) {
                            enabled.set(false);
                        }
                    });
        }
        return CompletableFuture.completedFuture(CdpPayload.NULL);
    }

    /**
     * Handles on request device prompt updated.
     *
     * @param params protocol parameters
     * @param future future value
     */
    private void onRequestDevicePromptUpdated(CdpPayload params, CompletableFuture<BidiDeviceRequestPrompt> future) {
        if (!contextId.equals(PayloadReader.text(params.get("context")))) {
            return;
        }
        future.complete(
                new BidiDeviceRequestPrompt(contextId, PayloadReader.text(params.get("prompt")), session,
                        BidiDeviceRequestPrompt.devices(params)));
    }

    /**
     * Handles schedule timeout.
     *
     * @param future  future value
     * @param timeout timeout value
     */
    private void scheduleTimeout(CompletableFuture<BidiDeviceRequestPrompt> future, Duration timeout) {
        Duration actualTimeout = timeout == null ? DEFAULT_WAIT_FOR_PROMPT_TIMEOUT : timeout;
        if (!actualTimeout.isZero() && !actualTimeout.isNegative()) {
            DEVICE_PROMPT_TIMER.schedule(
                    () -> future.completeExceptionally(
                            new TimeoutException("Timed out waiting for BiDi DeviceRequestPrompt: "
                                    + actualTimeout.toMillis() + "ms")),
                    actualTimeout.toMillis(),
                    TimeUnit.MILLISECONDS);
        }
    }

}
