/*
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
 ~                                                                               ~
 ~ The MIT License (MIT)                                                         ~
 ~                                                                               ~
 ~ Copyright (c) 2015-2024 miaixz.org and other contributors.                    ~
 ~                                                                               ~
 ~ Permission is hereby granted, free of charge, to any person obtaining a copy  ~
 ~ of this software and associated documentation files (the "Software"), to deal ~
 ~ in the Software without restriction, including without limitation the rights  ~
 ~ to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     ~
 ~ copies of the Software, and to permit persons to whom the Software is         ~
 ~ furnished to do so, subject to the following conditions:                      ~
 ~                                                                               ~
 ~ The above copyright notice and this permission notice shall be included in    ~
 ~ all copies or substantial portions of the Software.                           ~
 ~                                                                               ~
 ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    ~
 ~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      ~
 ~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   ~
 ~ AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        ~
 ~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, ~
 ~ OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     ~
 ~ THE SOFTWARE.                                                                 ~
 ~                                                                               ~
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
*/
package org.miaixz.lancia.kernel.page;

import java.util.*;
import java.util.regex.Pattern;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.xyz.CollKit;
import org.miaixz.bus.core.xyz.ListKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.nimble.emulation.ScreenOrientation;
import org.miaixz.lancia.option.GeolocationOptions;
import org.miaixz.lancia.option.data.MediaFeature;
import org.miaixz.lancia.option.data.RGBA;
import org.miaixz.lancia.option.data.Viewport;
import org.miaixz.lancia.option.state.*;
import org.miaixz.lancia.socket.CDPSession;
import org.miaixz.lancia.worker.ClientProvider;
import org.miaixz.lancia.worker.Updater;
import org.miaixz.lancia.worker.enums.CDPSessionEvent;
import org.miaixz.lancia.worker.enums.VisionDeficiency;

public class EmulationManager implements ClientProvider {

    private static final Updater<ViewportState> applyViewport = (client, viewportState) -> {
        if (viewportState.getViewport() == null) {
            client.send("Emulation.setDeviceMetricsOverride");
            client.send("Emulation.setTouchEmulationEnabled", new HashMap<>() {
                {
                    put("enabled", false);
                }
            });
            return;
        }
        Viewport viewport = viewportState.getViewport();
        boolean mobile = viewport.isMobile();
        int width = viewport.getWidth();
        int height = viewport.getHeight();
        double deviceScaleFactor = viewport.getDeviceScaleFactor() == null ? (double) viewport.getDeviceScaleFactor()
                : 1;
        ScreenOrientation screenOrientation;
        if (viewport.isLandscape()) {
            screenOrientation = ScreenOrientation.builder().angle(90).type("landscapePrimary").build();
        } else {
            screenOrientation = ScreenOrientation.builder().angle(0).type("portraitPrimary").build();
        }
        boolean hasTouch = viewport.isHasTouch();
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("mobile", mobile);
            params.put("width", width);
            params.put("height", height);
            params.put("deviceScaleFactor", deviceScaleFactor);
            params.put("screenOrientation", screenOrientation);
            client.send("Emulation.setDeviceMetricsOverride", params);
        } catch (Exception err) {
            if (err.getMessage().contains("Target does not support metrics override")) {
                Logger.error("lancia:error", err);
            }
            throw err;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("enabled", hasTouch);
        client.send("Emulation.setTouchEmulationEnabled", params);
    };
    private static final Updater<IdleOverridesState> emulateIdleState = (client, idleStateState) -> {
        if (!idleStateState.isActive()) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        if (idleStateState.getOverrides() != null) {
            params.put("isUserActive", idleStateState.overrides.isUserActive);
            params.put("isScreenUnlocked", idleStateState.overrides.isScreenUnlocked);
            client.send("Emulation.setIdleOverride", params);
        } else {
            client.send("Emulation.clearIdleOverride");
        }
    };
    private static final Updater<TimezoneState> emulateTimezone = (client, timezoneState) -> {
        if (!timezoneState.isActive()) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("timezoneId", StringKit.isEmpty(timezoneState.timezoneId) ? "" : timezoneState.timezoneId);
        try {
            client.send("Emulation.setTimezoneOverride", params);
        } catch (Exception error) {
            if (error.getMessage().contains("Invalid timezone")) {
                throw new IllegalArgumentException("Invalid timezone ID : " + timezoneState.timezoneId);
            }
            throw error;
        }
    };
    private static final Updater<VisionDeficiencyState> emulateVisionDeficiency = (client, visionDeficiency) -> {
        if (!visionDeficiency.isActive()) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("type", visionDeficiency.visionDeficiency.getValue());
        client.send("Emulation.setEmulatedVisionDeficiency", params);
    };
    private static final Updater<CpuThrottlingState> emulateCpuThrottling = (client, state) -> {
        if (!state.isActive()) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("rate", state.getFactor() == null ? 1 : state.getFactor());
        client.send("Emulation.setCPUThrottlingRate", params);
    };
    private static final Updater<MediaFeaturesState> emulateMediaFeatures = (client, state) -> {
        if (!state.isActive()) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("features", state.mediaFeatures);
        client.send("Emulation.setEmulatedMedia", params);
    };
    private static final Updater<MediaTypeState> emulateMediaType = (client, state) -> {
        if (!state.isActive()) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("media", state.getType() == null ? "" : state.getType());
        client.send("Emulation.setEmulatedMedia", params);
    };
    private static final Updater<GeoLocationState> setGeolocation = (client, state) -> {
        if (!state.active) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        if (state.geoLocation != null) {
            params.put("longitude", state.getGeoLocation().getLongitude());
            params.put("latitude", state.getGeoLocation().getLatitude());
            params.put("accuracy", state.getGeoLocation().getAccuracy());
            client.send("Emulation.setGeolocationOverride", params);
        } else {
            client.send("Emulation.setGeolocationOverride");
        }

    };
    private static final Updater<DefaultBackgroundColorState> setDefaultBackgroundColor = (client, state) -> {
        if (!state.isActive()) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("color", state.getColor());
        client.send("Emulation.setDefaultBackgroundColorOverride", params);
    };
    private static final Updater<JavascriptEnabledState> setJavaScriptEnabled = (client, state) -> {
        if (!state.active) {
            return;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("value", !state.isJavaScriptEnabled());
        client.send("Emulation.setScriptExecutionDisabled", params);
    };
    private final EmulatedState<ViewportState> viewportState = new EmulatedState<>(
            ViewportState.builder().active(false).build(), this, this.applyViewport);
    private final EmulatedState<IdleOverridesState> idleOverridesState = new EmulatedState<>(
            IdleOverridesState.builder().active(false).build(), this, this.emulateIdleState);
    private final EmulatedState<TimezoneState> timezoneState = new EmulatedState<>(
            TimezoneState.builder().active(false).build(), this, this.emulateTimezone);
    private final EmulatedState<VisionDeficiencyState> visionDeficiencyState = new EmulatedState<>(
            VisionDeficiencyState.builder().active(false).build(), this, this.emulateVisionDeficiency);
    private final EmulatedState<CpuThrottlingState> cpuThrottlingState = new EmulatedState<>(
            CpuThrottlingState.builder().active(false).build(), this, this.emulateCpuThrottling);

    private final EmulatedState<MediaFeaturesState> mediaFeaturesState = new EmulatedState<>(
            MediaFeaturesState.builder().active(false).build(), this, this.emulateMediaFeatures);
    private final EmulatedState<MediaTypeState> mediaTypeState = new EmulatedState<>(
            MediaTypeState.builder().active(false).build(), this, this.emulateMediaType);
    private final EmulatedState<GeoLocationState> geoLocationState = new EmulatedState<>(
            GeoLocationState.builder().active(false).build(), this, this.setGeolocation);
    private final EmulatedState<DefaultBackgroundColorState> defaultBackgroundColorState = new EmulatedState<>(
            DefaultBackgroundColorState.builder().active(false).build(), this, this.setDefaultBackgroundColor);

    private final EmulatedState<JavascriptEnabledState> javascriptEnabledState = new EmulatedState<>(
            JavascriptEnabledState.builder().active(false).javaScriptEnabled(true).build(), this,
            this.setJavaScriptEnabled);
    List<EmulatedState<?>> states = new ArrayList<>();
    Set<CDPSession> secondaryClients = new HashSet<>();
    private CDPSession client;
    private boolean emulatingMobile = false;
    private boolean hasTouch = false;

    public EmulationManager(CDPSession client) {
        this.client = client;
    }

    public void updateClient(CDPSession client) {
        this.client = client;
        this.secondaryClients.remove(client);

    }

    @Override
    public void registerState(EmulatedState<?> state) {
        if (CollKit.isEmpty(this.states)) {
            this.states = ListKit.of();
        }
        this.states.add(state);
    }

    @Override
    public List<CDPSession> clients() {
        List<CDPSession> cdpSessionList = new ArrayList<>();
        cdpSessionList.add(this.client);
        cdpSessionList.addAll(this.secondaryClients);
        return cdpSessionList;
    }

    public void registerSpeculativeSession(CDPSession _client) {
        this.secondaryClients.add(_client);
        client.once(CDPSessionEvent.CDPSession_Disconnected, event -> this.secondaryClients.remove(_client));
        // We don't await here because we want to register all state changes before
        // the target is unpaused.
        this.states.forEach(EmulatedState::send);
    }

    public boolean getJavascriptEnabled() {
        return this.javascriptEnabledState.state.javaScriptEnabled;
    }

    public boolean emulateViewport(Viewport viewport) {
        if (viewport == null && !this.viewportState.getState().isActive()) {
            return false;
        }
        if (viewport != null) {
            this.viewportState.setState(ViewportState.builder().active(true).viewport(viewport).build());
        } else {
            this.viewportState.setState(ViewportState.builder().active(false).build());
        }
        boolean mobile = false;
        boolean hasTouch = false;
        if (viewport != null) {
            mobile = viewport.isMobile();
            hasTouch = viewport.isHasTouch();
        }
        boolean reloadNeeded = this.emulatingMobile != mobile || this.hasTouch != hasTouch;
        this.emulatingMobile = mobile;
        this.hasTouch = hasTouch;
        return reloadNeeded;
    }

    public void emulateIdleState(IdleOverridesState.Overrides overrides) {
        this.idleOverridesState.setState(IdleOverridesState.builder().active(true).overrides(overrides).build());
    }

    public void emulateTimezone(String timezoneId) {
        this.timezoneState.setState(TimezoneState.builder().active(true).timezoneId(timezoneId).build());
    }

    public void emulateVisionDeficiency(VisionDeficiency visionDeficiency) {
        this.visionDeficiencyState
                .setState(VisionDeficiencyState.builder().active(true).visionDeficiency(visionDeficiency).build());
    }

    public void emulateCPUThrottling(double factor) {
        Assert.isTrue(factor >= 1, "Throttling rate should be greater or equal to 1");
        this.cpuThrottlingState.setState(CpuThrottlingState.builder().active(true).factor(factor).build());
    }

    public void emulateMediaFeatures(List<MediaFeature> features) {
        if (features != null && !features.isEmpty()) {
            for (MediaFeature mediaFeature : features) {
                String name = mediaFeature.getName();
                Pattern pattern = Pattern.compile("^(?:prefers-(?:color-scheme|reduced-motion)|color-gamut)$");
                Assert.isTrue(pattern.matcher(name).find(), "Unsupported media feature: " + name);
            }
        }
        this.mediaFeaturesState.setState(MediaFeaturesState.builder().active(true).mediaFeatures(features).build());
    }

    public void emulateMediaType(String type) {
        Assert.isTrue("screen".equals(type) || "print".equals(type) || type == null, "Unsupported media type: " + type);
        this.mediaTypeState.setState(MediaTypeState.builder().active(true).type(type).build());
    }

    public void setGeolocation(GeolocationOptions options) {
        if (options.getLongitude() < -180 || options.getLongitude() > 180) {
            throw new IllegalArgumentException(
                    "Invalid longitude " + options.getLongitude() + ": precondition -180 <= LONGITUDE <= 180 failed.");
        }
        if (options.getLatitude() < -90 || options.getLatitude() > 90) {
            throw new IllegalArgumentException(
                    "Invalid latitude " + options.getLatitude() + ": precondition -90 <= LATITUDE <= 90 failed.");
        }
        if (options.getAccuracy() < 0) {
            throw new IllegalArgumentException(
                    "Invalid accuracy " + options.getAccuracy() + ": precondition 0 <= ACCURACY failed.");
        }
        this.geoLocationState
                .setState(
                        GeoLocationState.builder().active(true)
                                .geoLocation(GeolocationOptions.builder().longitude(options.getLongitude())
                                        .latitude(options.getLatitude()).accuracy(options.getAccuracy()).build())
                                .build());
    }

    /**
     * Resets default white background
     */
    public void resetDefaultBackgroundColor() {
        this.defaultBackgroundColorState.setState(DefaultBackgroundColorState.builder().active(true).build());
    }

    /**
     * Hides default white background
     */
    public void setTransparentBackgroundColor() {
        this.defaultBackgroundColorState.setState(DefaultBackgroundColorState.builder().active(true)
                .color(RGBA.builder().r(0).g(0).b(0).a(0).build()).build());
    }

    public void setJavaScriptEnabled(boolean enabled) {
        this.javascriptEnabledState
                .setState(JavascriptEnabledState.builder().active(true).javaScriptEnabled(enabled).build());
    }

}