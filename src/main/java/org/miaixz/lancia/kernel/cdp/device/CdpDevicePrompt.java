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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.core.lang.thread.NamedThreadFactory;
import org.miaixz.lancia.Binding;
import org.miaixz.lancia.kernel.Prompts;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.nimble.device.DeviceRequest;
import org.miaixz.lancia.shared.payload.PayloadDeviceRequest;
import org.miaixz.lancia.shared.payload.PayloadReader;

/**
 * Represents a Web Bluetooth device request prompt.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpDevicePrompt implements Prompts {

    /**
     * Default wait for device timeout.
     */
    private static final java.time.Duration DEFAULT_WAIT_FOR_DEVICE_TIMEOUT = java.time.Duration.ofSeconds(30L);
    /**
     * Shared constant for device wait timer.
     */
    private static final ScheduledExecutorService DEVICE_WAIT_TIMER = Executors
            .newSingleThreadScheduledExecutor(new NamedThreadFactory("lancia-device-waiter-", true));
    /**
     * Current session.
     */
    private final CDPSession session;
    /**
     * Current identifier.
     */
    private final String id;
    /**
     * Registered devices values.
     */
    private final List<DeviceRequest> devices = new CopyOnWriteArrayList<>();
    /**
     * Registered waiters values.
     */
    private final List<DeviceWaiter> waiters = new CopyOnWriteArrayList<>();
    /**
     * Current update binding.
     */
    private final Binding updateBinding;
    /**
     * Current detach binding.
     */
    private final Binding detachBinding;
    /**
     * Whether handled is enabled.
     */
    private volatile boolean handled;
    /**
     * Whether detached is enabled.
     */
    private volatile boolean detached;

    /**
     * Creates a device request prompt.
     *
     * @param session protocol session
     * @param id      identifier
     * @param devices devices
     */
    public CdpDevicePrompt(CDPSession session, String id, List<DeviceRequest> devices) {
        this.session = Assert.notNull(session, "session");
        this.id = Assert.notBlank(id, "id");
        if (devices != null) {
            devices.forEach(this::addDevice);
        }
        this.updateBinding = session.on("DeviceAccess.deviceRequestPrompted", this::updateDevices);
        this.detachBinding = session.on("Target.detachedFromTarget", ignored -> detached = true);
    }

    /**
     * Waits for device.
     *
     * @param filter  filter
     * @param timeout timeout value
     * @return wait for device value
     */
    public CompletableFuture<DeviceRequest> waitForDevice(Predicate<DeviceRequest> filter, java.time.Duration timeout) {
        Predicate<DeviceRequest> actualFilter = Assert.notNull(filter, "filter");
        for (DeviceRequest device : devices) {
            if (actualFilter.test(device)) {
                return CompletableFuture.completedFuture(device);
            }
        }
        CompletableFuture<DeviceRequest> future = new CompletableFuture<>();
        DeviceWaiter waiter = new DeviceWaiter(actualFilter, future);
        waiters.add(waiter);
        java.time.Duration actualTimeout = timeout == null ? DEFAULT_WAIT_FOR_DEVICE_TIMEOUT : timeout;
        if (!actualTimeout.isZero() && !actualTimeout.isNegative()) {
            DEVICE_WAIT_TIMER.schedule(() -> {
                if (future.completeExceptionally(new TimeoutException("Timed out waiting for requested device."))) {
                    waiters.remove(waiter);
                }
            }, actualTimeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        future.whenComplete((value, error) -> waiters.remove(waiter));
        return future;
    }

    /**
     * Waits for device.
     *
     * @param filter filter
     * @return wait for device value
     */
    public CompletableFuture<DeviceRequest> waitForDevice(Predicate<DeviceRequest> filter) {
        return waitForDevice(filter, DEFAULT_WAIT_FOR_DEVICE_TIMEOUT);
    }

    /**
     * Adds device.
     *
     * @param device device
     */
    public void addDevice(DeviceRequest device) {
        DeviceRequest actualDevice = Assert.notNull(device, "device");
        for (DeviceRequest existing : devices) {
            if (existing.id().equals(actualDevice.id())) {
                return;
            }
        }
        devices.add(actualDevice);
        for (DeviceWaiter waiter : waiters) {
            if (waiter.filter.test(actualDevice) && waiter.future.complete(actualDevice)) {
                waiters.remove(waiter);
            }
        }
    }

    /**
     * Returns the select.
     *
     * @param device device value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> select(DeviceRequest device) {
        ensureSelectable(device);
        handled = true;
        cleanupBindings();
        return session
                .send("DeviceAccess.selectPrompt", Map.of("id", id, "deviceId", Assert.notNull(device, "device").id()));
    }

    /**
     * Returns whether cel is available.
     *
     * @return {@code true} when the condition matches
     */
    public CompletableFuture<CdpPayload> cancel() {
        ensureNotHandled("Cannot cancel CdpDevicePrompt which is already handled!");
        ensureAttached("Cannot cancel prompt through detached session!");
        handled = true;
        cleanupBindings();
        return session.send("DeviceAccess.cancelPrompt", Map.of("id", id));
    }

    /**
     * Returns the ID.
     *
     * @return ID value
     */
    public String id() {
        return id;
    }

    /**
     * Returns the devices.
     *
     * @return values
     */
    public List<DeviceRequest> devices() {
        return List.copyOf(devices);
    }

    /**
     * Handles d.
     *
     * @return handled state
     */
    public boolean handled() {
        return handled;
    }

    /**
     * Updates devices.
     *
     * @param event event name
     */
    public void updateDevices(CdpPayload event) {
        if (event == null || event.isNull() || !id.equals(PayloadReader.text(event.get("id")))) {
            return;
        }
        CdpPayload rawDevices = event.get("devices");
        if (!rawDevices.isArray()) {
            return;
        }
        rawDevices.elements().stream().map(PayloadDeviceRequest::from).forEach(this::addDevice);
    }

    /**
     * Handles ensure selectable.
     *
     * @param device device value
     */
    private void ensureSelectable(DeviceRequest device) {
        DeviceRequest actualDevice = Assert.notNull(device, "device");
        ensureAttached("Cannot select device through detached session!");
        ensureNotHandled("Cannot select CdpDevicePrompt which is already handled!");
        if (!devices.contains(actualDevice)) {
            throw new InternalException("Cannot select unknown device!");
        }
    }

    /**
     * Handles ensure attached.
     *
     * @param message message text
     */
    private void ensureAttached(String message) {
        if (detached || session.detached()) {
            throw new InternalException(message);
        }
    }

    /**
     * Handles ensure not handled.
     *
     * @param message message text
     */
    private void ensureNotHandled(String message) {
        if (handled) {
            throw new InternalException(message);
        }
    }

    /**
     * Handles cleanup bindings.
     */
    private void cleanupBindings() {
        updateBinding.unbind();
        detachBinding.unbind();
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
         * Creates an instance.
         *
         * @param filter filter value
         * @param future future value
         */
        private DeviceWaiter(Predicate<DeviceRequest> filter, CompletableFuture<DeviceRequest> future) {
            this.filter = filter;
            this.future = future;
        }
    }

}
