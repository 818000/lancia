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
package org.miaixz.lancia.kernel.browser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Browser;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Emitter;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.kernel.page.Target;
import org.miaixz.lancia.socket.Connection;
import org.miaixz.lancia.worker.enums.BrowserContextEvent;
import org.miaixz.lancia.worker.enums.TargetType;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;

/**
 * 浏览器上下文
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class Context extends Emitter<BrowserContextEvent> {

    private static final Map<String, String> WEB_PERMISSION_TO_PROTOCOL_PERMISSION = new HashMap<>(32);

    static {
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("geolocation", "geolocation");
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("midi", "midi");
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("notifications", "notifications");
//		webPermissionToProtocol.put("push","push");
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("camera", "videoCapture");
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("microphone", "audioCapture");
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("background-sync", "backgroundSync");
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("ambient-light-sensor", "sensors");
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("accelerometer", "sensors");
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("gyroscope", "sensors");
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("magnetometer", "sensors");
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("accessibility-events", "accessibilityEvents");
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("clipboard-read", "clipboardReadWrite");
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("clipboard-write", "clipboardReadWrite");
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("clipboard-sanitized-write", "clipboardSanitizedWrite");
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("payment-handler", "paymentHandler");
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("persistent-storage", "durableStorage");
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("idle-detection", "idleDetection");
        WEB_PERMISSION_TO_PROTOCOL_PERMISSION.put("midi-sysex", "midiSysex");
    }

    /**
     * 浏览器对应的websocket client包装类，用于发送和接受消息
     */
    private Connection connection;
    /**
     * 浏览器上下文对应的浏览器，一个上下文只有一个浏览器，但是一个浏览器可能有多个上下文
     */
    private Browser browser;
    /**
     * 浏览器上下文id
     */
    private String id;

    public Context() {
        super();
    }

    public Context(Connection connection, Browser browser, String contextId) {
        super();
        this.connection = connection;
        this.browser = browser;
        this.id = contextId;
    }

    public List<Target> targets() {
        return this.browser.targets().stream().filter(target -> target.browserContext() == this)
                .collect(Collectors.toList());
    }

    public List<Page> pages() {
        return this.targets().stream()
                .filter(target -> TargetType.PAGE.equals(target.type())
                        || (TargetType.OTHER.equals(target.type()) && this.browser.getIsPageTargetCallback() != null
                                ? this.browser.getIsPageTargetCallback().apply(target)
                                : true))
                .map(Target::page).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public void overridePermissions(String origin, List<String> permissions) {
        permissions.replaceAll(item -> {
            String protocolPermission = WEB_PERMISSION_TO_PROTOCOL_PERMISSION.get(item);
            Assert.isTrue(protocolPermission != null, "Unknown permission: " + item);
            return protocolPermission;
        });
        Map<String, Object> params = new HashMap<>();
        params.put("origin", origin);
        params.put("browserContextId", this.id);
        params.put("permissions", permissions);
        this.connection.send("Browser.grantPermissions", params);
    }

    public void clearPermissionOverrides() {
        Map<String, Object> params = new HashMap<>();
        params.put("browserContextId", this.id);
        this.connection.send("Browser.resetPermissions", params);
    }

    public Page newPage() {
        synchronized (this) {
            return this.browser.createPageInContext(this.id);
        }
    }

    public void close() {
        Assert.isTrue(StringKit.isNotEmpty(this.id), "Non-incognito profiles cannot be closed!");
        this.browser.disposeContext(this.id);
    }

    public boolean closed() {
        return !this.browser.browserContexts().contains(this);
    }

    public Target waitForTarget(Predicate<Target> predicate, int timeout) {
        Observable<Target> targetCreateObservable = Builder.fromEmitterEvent(this, BrowserContextEvent.TargetCreated);
        Observable<Target> TargetChangeObservable = Builder.fromEmitterEvent(this, BrowserContextEvent.TargetChanged);
        @NonNull
        Observable<@NonNull Target> targetsObservable = Observable.fromIterable(this.targets());
        return Observable.mergeArray(targetCreateObservable, TargetChangeObservable, targetsObservable)
                .filter(predicate::test).timeout(timeout, TimeUnit.MILLISECONDS).blockingFirst();
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Browser browser() {
        return browser;
    }

    public String getId() {
        return this.id;
    }

}
