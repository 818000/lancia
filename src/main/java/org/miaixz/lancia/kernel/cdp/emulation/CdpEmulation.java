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
package org.miaixz.lancia.kernel.cdp.emulation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.exception.InternalException;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.kernel.Emulation;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;
import org.miaixz.lancia.kernel.cdp.session.CDPSession;
import org.miaixz.lancia.nimble.emulation.Device;
import org.miaixz.lancia.nimble.emulation.Geolocation;
import org.miaixz.lancia.nimble.emulation.IdleState;
import org.miaixz.lancia.nimble.emulation.MediaFeature;
import org.miaixz.lancia.nimble.emulation.Viewport;
import org.miaixz.lancia.shared.async.Awaitable;

/**
 * Manages emulation state and protocol events.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpEmulation implements Emulation {

    /**
     * Shared constant for supported media feature.
     */
    private static final Pattern SUPPORTED_MEDIA_FEATURE = Pattern.compile(
            "^(?:prefers-(?:color-scheme|reduced-motion|reduced-transparency|contrast)|color-gamut|forced-colors|inverted-colors|hover|pointer|any-hover|any-pointer)$");
    /**
     * Shared constant for supported media types.
     */
    private static final Set<String> SUPPORTED_MEDIA_TYPES = Set.of("screen", "print");
    /**
     * Shared constant for supported vision deficiencies.
     */
    private static final Set<String> SUPPORTED_VISION_DEFICIENCIES = Set.of(
            "none",
            "achromatopsia",
            "blurredVision",
            "deuteranopia",
            "protanopia",
            "reducedContrast",
            "tritanopia");
    /**
     * Current session.
     */
    private CDPSession session;
    /**
     * Registered secondary clients values.
     */
    private final Set<CDPSession> secondaryClients = new LinkedHashSet<>();
    /**
     * Current viewport.
     */
    private Viewport viewport;
    /**
     * Whether viewport active is enabled.
     */
    private boolean viewportActive;
    /**
     * Whether emulating mobile is enabled.
     */
    private boolean emulatingMobile;
    /**
     * Whether has touch is enabled.
     */
    private boolean hasTouch;
    /**
     * Current user agent.
     */
    private String userAgent;
    /**
     * Whether user agent active is enabled.
     */
    private boolean userAgentActive;
    /**
     * Current media type.
     */
    private String mediaType;
    /**
     * Whether media type active is enabled.
     */
    private boolean mediaTypeActive;
    /**
     * Registered media features values.
     */
    private List<MediaFeature> mediaFeatures = List.of();
    /**
     * Whether media features active is enabled.
     */
    private boolean mediaFeaturesActive;
    /**
     * Current cpu rate.
     */
    private Double cpuRate;
    /**
     * Whether cpu rate active is enabled.
     */
    private boolean cpuRateActive;
    /**
     * Current idle state.
     */
    private IdleState idleState;
    /**
     * Whether idle state active is enabled.
     */
    private boolean idleStateActive;
    /**
     * Current timezone.
     */
    private String timezone;
    /**
     * Whether timezone active is enabled.
     */
    private boolean timezoneActive;
    /**
     * Current vision deficiency.
     */
    private String visionDeficiency;
    /**
     * Whether vision deficiency active is enabled.
     */
    private boolean visionDeficiencyActive;
    /**
     * Current geolocation.
     */
    private Geolocation geolocation;
    /**
     * Whether geolocation active is enabled.
     */
    private boolean geolocationActive;
    /**
     * Mapped default background color values.
     */
    private Map<String, Object> defaultBackgroundColor;
    /**
     * Whether default background color active is enabled.
     */
    private boolean defaultBackgroundColorActive;
    /**
     * Whether JavaScript execution is enabled.
     */
    private boolean javaScriptEnabled = true;
    /**
     * Whether the JavaScript enabled state has been applied.
     */
    private boolean javaScriptEnabledActive;
    /**
     * Whether focus is enabled.
     */
    private boolean focusEnabled = true;
    /**
     * Whether focus active is enabled.
     */
    private boolean focusActive;

    /**
     * Creates an CdpEmulation instance.
     *
     * @param session session
     */
    public CdpEmulation(CDPSession session) {
        this.session = session;
    }

    /**
     * Updates client.
     *
     * @param client protocol client
     */
    public synchronized void updateClient(CDPSession client) {
        this.session = client;
        secondaryClients.remove(client);
    }

    /**
     * Returns the clients.
     *
     * @return values
     */
    public synchronized List<CDPSession> clients() {
        List<CDPSession> clients = new ArrayList<>();
        if (session != null) {
            clients.add(session);
        }
        clients.addAll(secondaryClients);
        return List.copyOf(clients);
    }

    /**
     * Returns the register speculative session.
     *
     * @param client protocol client
     * @return completion future
     */
    public CompletableFuture<Void> registerSpeculativeSession(CDPSession client) {
        if (client == null) {
            return CompletableFuture.completedFuture(null);
        }
        synchronized (this) {
            secondaryClients.add(client);
        }
        client.once(CDPSession.Events.DISCONNECTED, payload -> {
            synchronized (this) {
                secondaryClients.remove(client);
            }
        });
        return replayState(client);
    }

    /**
     * Updates viewport.
     *
     * @param viewport viewport
     * @return set viewport value
     */
    public CompletableFuture<Void> setViewport(Viewport viewport) {
        return emulateViewport(viewport).thenApply(reloadNeeded -> null);
    }

    /**
     * Returns the emulate viewport.
     *
     * @param viewport viewport value
     * @return completion future
     */
    public CompletableFuture<Boolean> emulateViewport(Viewport viewport) {
        boolean previousMobile = emulatingMobile;
        boolean previousTouch = hasTouch;
        this.viewport = viewport;
        this.viewportActive = viewport != null;
        this.emulatingMobile = viewport != null && viewport.isMobile();
        this.hasTouch = viewport != null && viewport.isHasTouch();
        boolean reloadNeeded = previousMobile != emulatingMobile || previousTouch != hasTouch;
        return applyViewport().thenApply(value -> reloadNeeded);
    }

    /**
     * Returns the emulate.
     *
     * @param device device value
     * @return completion future
     */
    public CompletableFuture<Void> emulate(Device device) {
        if (device == null) {
            userAgentActive = false;
            userAgent = null;
            return setViewport(null);
        }
        userAgent = device.userAgent();
        userAgentActive = true;
        viewport = device.viewport();
        viewportActive = true;
        emulatingMobile = viewport.isMobile();
        hasTouch = viewport.isHasTouch();
        return Awaitable.all(applyUserAgent(), applyViewport());
    }

    /**
     * Returns the emulate media type.
     *
     * @param mediaType media type value
     * @return completion future
     */
    public CompletableFuture<Void> emulateMediaType(String mediaType) {
        String normalized = StringKit.isBlank(mediaType) ? null : mediaType;
        if (normalized != null && !SUPPORTED_MEDIA_TYPES.contains(normalized)) {
            throw new InternalException("Invalid media type: " + mediaType);
        }
        this.mediaType = normalized;
        this.mediaTypeActive = true;
        return applyMediaType();
    }

    /**
     * Returns the emulate media features.
     *
     * @param mediaFeatures media features value
     * @return completion future
     */
    public CompletableFuture<Void> emulateMediaFeatures(List<MediaFeature> mediaFeatures) {
        List<MediaFeature> actualFeatures = mediaFeatures == null ? List.of() : List.copyOf(mediaFeatures);
        for (MediaFeature mediaFeature : actualFeatures) {
            if (mediaFeature == null || !SUPPORTED_MEDIA_FEATURE.matcher(mediaFeature.name()).matches()) {
                throw new InternalException(
                        "Invalid media feature: " + (mediaFeature == null ? Normal.EMPTY : mediaFeature.name()));
            }
        }
        this.mediaFeatures = actualFeatures;
        this.mediaFeaturesActive = true;
        return applyMediaFeatures();
    }

    /**
     * Returns the emulate cpu throttling.
     *
     * @param rate rate value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateCPUThrottling(double rate) {
        if (rate < 1) {
            throw new InternalException("Throttling rate should be greater or equal to 1");
        }
        this.cpuRate = rate;
        this.cpuRateActive = true;
        return toPayload(applyCpuThrottling());
    }

    /**
     * Returns the clear cpu throttling.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> clearCPUThrottling() {
        this.cpuRate = null;
        this.cpuRateActive = true;
        return toPayload(applyCpuThrottling());
    }

    /**
     * Returns the emulate idle state.
     *
     * @param idleState idle state value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateIdleState(IdleState idleState) {
        this.idleState = idleState;
        this.idleStateActive = true;
        return toPayload(applyIdleState());
    }

    /**
     * Returns the emulate timezone.
     *
     * @param timezone timezone value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateTimezone(String timezone) {
        this.timezone = timezone;
        this.timezoneActive = true;
        return toPayload(applyTimezone());
    }

    /**
     * Returns the emulate vision deficiency.
     *
     * @param visionDeficiency vision deficiency value
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateVisionDeficiency(String visionDeficiency) {
        String normalized = StringKit.isBlank(visionDeficiency) ? "none" : visionDeficiency;
        if (!SUPPORTED_VISION_DEFICIENCIES.contains(normalized)) {
            throw new InternalException("Invalid vision deficiency: " + visionDeficiency);
        }
        this.visionDeficiency = normalized;
        this.visionDeficiencyActive = true;
        return toPayload(applyVisionDeficiency());
    }

    /**
     * Updates geolocation.
     *
     * @param geolocation geolocation
     * @return set geolocation value
     */
    public CompletableFuture<CdpPayload> setGeolocation(Geolocation geolocation) {
        validateGeolocation(geolocation);
        this.geolocation = geolocation;
        this.geolocationActive = true;
        return toPayload(applyGeolocation());
    }

    /**
     * Returns the reset default background color.
     *
     * @return completion future
     */
    public CompletableFuture<CdpPayload> resetDefaultBackgroundColor() {
        this.defaultBackgroundColor = null;
        this.defaultBackgroundColorActive = true;
        return toPayload(applyDefaultBackgroundColor());
    }

    /**
     * Updates transparent background color.
     *
     * @return set transparent background color value
     */
    public CompletableFuture<CdpPayload> setTransparentBackgroundColor() {
        this.defaultBackgroundColor = Map.of("r", 0, "g", 0, "b", 0, "a", 0);
        this.defaultBackgroundColorActive = true;
        return toPayload(applyDefaultBackgroundColor());
    }

    /**
     * Updates whether JavaScript execution is enabled.
     *
     * @param enabled enabled
     * @return protocol response future
     */
    public CompletableFuture<CdpPayload> setJavaScriptEnabled(boolean enabled) {
        this.javaScriptEnabled = enabled;
        this.javaScriptEnabledActive = true;
        return toPayload(applyJavaScriptEnabled());
    }

    /**
     * Returns whether JavaScript execution is enabled.
     *
     * @return {@code true} when the condition matches
     */
    public boolean javaScriptEnabled() {
        return javaScriptEnabled;
    }

    /**
     * Returns the emulate focus.
     *
     * @param enabled whether the feature should be enabled
     * @return completion future
     */
    public CompletableFuture<CdpPayload> emulateFocus(boolean enabled) {
        this.focusEnabled = enabled;
        this.focusActive = true;
        return toPayload(applyFocus());
    }

    /**
     * Returns the replay state.
     *
     * @return completion future
     */
    public CompletableFuture<Void> replayState() {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (CDPSession client : clients()) {
            futures.add(replayState(client));
        }
        return Awaitable.all(futures);
    }

    /**
     * Returns the replay state.
     *
     * @param client protocol client
     * @return completion future
     */
    private CompletableFuture<Void> replayState(CDPSession client) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        if (userAgentActive) {
            futures.add(applyUserAgent(client));
        }
        if (viewportActive || viewport == null) {
            futures.add(applyViewport(client));
        }
        if (mediaTypeActive) {
            futures.add(applyMediaType(client));
        }
        if (mediaFeaturesActive) {
            futures.add(applyMediaFeatures(client));
        }
        if (cpuRateActive) {
            futures.add(applyCpuThrottling(client));
        }
        if (idleStateActive) {
            futures.add(applyIdleState(client));
        }
        if (timezoneActive) {
            futures.add(applyTimezone(client));
        }
        if (visionDeficiencyActive) {
            futures.add(applyVisionDeficiency(client));
        }
        if (geolocationActive) {
            futures.add(applyGeolocation(client));
        }
        if (defaultBackgroundColorActive) {
            futures.add(applyDefaultBackgroundColor(client));
        }
        if (javaScriptEnabledActive) {
            futures.add(applyJavaScriptEnabled(client));
        }
        if (focusActive) {
            futures.add(applyFocus(client));
        }
        return Awaitable.all(futures);
    }

    /**
     * Returns the apply user agent.
     *
     * @return completion future
     */
    private CompletableFuture<Void> applyUserAgent() {
        return applyToClients(this::applyUserAgent);
    }

    /**
     * Returns the apply user agent.
     *
     * @param client protocol client
     * @return completion future
     */
    private CompletableFuture<CdpPayload> applyUserAgent(CDPSession client) {
        return send(
                client,
                "Network.setUserAgentOverride",
                Map.of("userAgent", userAgent == null ? Normal.EMPTY : userAgent));
    }

    /**
     * Returns the apply viewport.
     *
     * @return completion future
     */
    private CompletableFuture<Void> applyViewport() {
        return applyToClients(this::applyViewport);
    }

    /**
     * Returns the apply viewport.
     *
     * @param client protocol client
     * @return completion future
     */
    private CompletableFuture<Void> applyViewport(CDPSession client) {
        if (!viewportActive || viewport == null) {
            return Awaitable.all(
                    send(client, "Emulation.clearDeviceMetricsOverride", Map.of()),
                    send(client, "Emulation.setTouchEmulationEnabled", Map.of("enabled", false)));
        }
        return Awaitable.all(
                send(client, "Emulation.setDeviceMetricsOverride", viewport.toMap()),
                send(client, "Emulation.setTouchEmulationEnabled", Map.of("enabled", viewport.isHasTouch())));
    }

    /**
     * Returns the apply media type.
     *
     * @return completion future
     */
    private CompletableFuture<Void> applyMediaType() {
        return applyToClients(this::applyMediaType);
    }

    /**
     * Returns the apply media type.
     *
     * @param client protocol client
     * @return completion future
     */
    private CompletableFuture<CdpPayload> applyMediaType(CDPSession client) {
        return send(
                client,
                "Emulation.setEmulatedMedia",
                Map.of("media", mediaType == null ? Normal.EMPTY : mediaType));
    }

    /**
     * Returns the apply media features.
     *
     * @return completion future
     */
    private CompletableFuture<Void> applyMediaFeatures() {
        return applyToClients(this::applyMediaFeatures);
    }

    /**
     * Returns the apply media features.
     *
     * @param client protocol client
     * @return completion future
     */
    private CompletableFuture<CdpPayload> applyMediaFeatures(CDPSession client) {
        List<Map<String, Object>> features = mediaFeatures.stream().map(MediaFeature::toMap).toList();
        return send(client, "Emulation.setEmulatedMedia", Map.of("features", features));
    }

    /**
     * Returns the apply cpu throttling.
     *
     * @return completion future
     */
    private CompletableFuture<Void> applyCpuThrottling() {
        return applyToClients(this::applyCpuThrottling);
    }

    /**
     * Returns the apply cpu throttling.
     *
     * @param client protocol client
     * @return completion future
     */
    private CompletableFuture<CdpPayload> applyCpuThrottling(CDPSession client) {
        return send(client, "Emulation.setCPUThrottlingRate", Map.of("rate", cpuRate == null ? 1 : cpuRate));
    }

    /**
     * Returns the apply idle state.
     *
     * @return completion future
     */
    private CompletableFuture<Void> applyIdleState() {
        return applyToClients(this::applyIdleState);
    }

    /**
     * Returns the apply idle state.
     *
     * @param client protocol client
     * @return completion future
     */
    private CompletableFuture<CdpPayload> applyIdleState(CDPSession client) {
        if (idleState == null) {
            return send(client, "Emulation.clearIdleOverride", Map.of());
        }
        return send(client, "Emulation.setIdleOverride", idleState.toMap());
    }

    /**
     * Returns the apply timezone.
     *
     * @return completion future
     */
    private CompletableFuture<Void> applyTimezone() {
        return applyToClients(this::applyTimezone);
    }

    /**
     * Returns the apply timezone.
     *
     * @param client protocol client
     * @return completion future
     */
    private CompletableFuture<CdpPayload> applyTimezone(CDPSession client) {
        return send(
                client,
                "Emulation.setTimezoneOverride",
                Map.of("timezoneId", timezone == null ? Normal.EMPTY : timezone));
    }

    /**
     * Returns the apply vision deficiency.
     *
     * @return completion future
     */
    private CompletableFuture<Void> applyVisionDeficiency() {
        return applyToClients(this::applyVisionDeficiency);
    }

    /**
     * Returns the apply vision deficiency.
     *
     * @param client protocol client
     * @return completion future
     */
    private CompletableFuture<CdpPayload> applyVisionDeficiency(CDPSession client) {
        return send(
                client,
                "Emulation.setEmulatedVisionDeficiency",
                Map.of("type", visionDeficiency == null ? "none" : visionDeficiency));
    }

    /**
     * Returns the apply geolocation.
     *
     * @return completion future
     */
    private CompletableFuture<Void> applyGeolocation() {
        return applyToClients(this::applyGeolocation);
    }

    /**
     * Returns the apply geolocation.
     *
     * @param client protocol client
     * @return completion future
     */
    private CompletableFuture<CdpPayload> applyGeolocation(CDPSession client) {
        return send(client, "Emulation.setGeolocationOverride", geolocation == null ? Map.of() : geolocation.toMap());
    }

    /**
     * Returns the apply default background color.
     *
     * @return completion future
     */
    private CompletableFuture<Void> applyDefaultBackgroundColor() {
        return applyToClients(this::applyDefaultBackgroundColor);
    }

    /**
     * Returns the apply default background color.
     *
     * @param client protocol client
     * @return completion future
     */
    private CompletableFuture<CdpPayload> applyDefaultBackgroundColor(CDPSession client) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (defaultBackgroundColor != null) {
            params.put("color", defaultBackgroundColor);
        }
        return send(client, "Emulation.setDefaultBackgroundColorOverride", params);
    }

    /**
     * Applies the JavaScript enabled state to all clients.
     *
     * @return completion future
     */
    private CompletableFuture<Void> applyJavaScriptEnabled() {
        return applyToClients(this::applyJavaScriptEnabled);
    }

    /**
     * Applies the JavaScript enabled state to a client.
     *
     * @param client protocol client
     * @return completion future
     */
    private CompletableFuture<CdpPayload> applyJavaScriptEnabled(CDPSession client) {
        return send(client, "Emulation.setScriptExecutionDisabled", Map.of("value", !javaScriptEnabled));
    }

    /**
     * Returns the apply focus.
     *
     * @return completion future
     */
    private CompletableFuture<Void> applyFocus() {
        return applyToClients(this::applyFocus);
    }

    /**
     * Returns the apply focus.
     *
     * @param client protocol client
     * @return completion future
     */
    private CompletableFuture<CdpPayload> applyFocus(CDPSession client) {
        return send(client, "Emulation.setFocusEmulationEnabled", Map.of("enabled", focusEnabled));
    }

    /**
     * Returns the apply to clients.
     *
     * @param action action value
     * @return completion future
     */
    private CompletableFuture<Void> applyToClients(ClientAction action) {
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (CDPSession client : clients()) {
            futures.add(action.apply(client));
        }
        return Awaitable.all(futures);
    }

    /**
     * Validates geolocation.
     *
     * @param geolocation geolocation value
     */
    private void validateGeolocation(Geolocation geolocation) {
        if (geolocation == null) {
            return;
        }
        if (geolocation.longitude() < -180 || geolocation.longitude() > 180) {
            throw new InternalException("Invalid longitude \"" + geolocation.longitude()
                    + "\": precondition -180 <= LONGITUDE <= 180 failed.");
        }
        if (geolocation.latitude() < -90 || geolocation.latitude() > 90) {
            throw new InternalException(
                    "Invalid latitude \"" + geolocation.latitude() + "\": precondition -90 <= LATITUDE <= 90 failed.");
        }
        if (geolocation.accuracy() < Normal._0) {
            throw new InternalException(
                    "Invalid accuracy \"" + geolocation.accuracy() + "\": precondition 0 <= ACCURACY failed.");
        }
    }

    /**
     * Converts this value to payload.
     *
     * @param future future value
     * @return completion future
     */
    private CompletableFuture<CdpPayload> toPayload(CompletableFuture<Void> future) {
        return future.thenApply(value -> CdpPayload.NULL);
    }

    /**
     * Sends a protocol command.
     *
     * @param client protocol client
     * @param method protocol method
     * @param params protocol parameters
     * @return completion future
     */
    private CompletableFuture<CdpPayload> send(CDPSession client, String method, Map<String, Object> params) {
        if (client == null) {
            return CompletableFuture.completedFuture(CdpPayload.NULL);
        }
        return client.send(method, params);
    }

    /**
     * Defines the client action contract.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private interface ClientAction {

        /**
         * Returns the apply.
         *
         * @param client protocol client
         * @return completion future
         */
        CompletableFuture<?> apply(CDPSession client);
    }

}
