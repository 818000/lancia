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
package org.miaixz.lancia.kernel.cdp.network;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.lancia.kernel.cdp.protocol.CdpPayload;

/**
 * Manages CDP network event state and protocol events.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpNetworkEventManager {

    /**
     * Creates a CDP network event manager.
     */
    public CdpNetworkEventManager() {
        // No initialization required.
    }

    /**
     * Mapped requests values.
     */
    private final Map<String, CdpRequest> requests = new ConcurrentHashMap<>();
    /**
     * Mapped request will be sent events values.
     */
    private final Map<String, CdpPayload> requestWillBeSentEvents = new ConcurrentHashMap<>();
    /**
     * Mapped request paused events values.
     */
    private final Map<String, CdpPayload> requestPausedEvents = new ConcurrentHashMap<>();
    /**
     * Mapped responses values.
     */
    private final Map<String, CdpResponse> responses = new ConcurrentHashMap<>();
    /**
     * Mapped request extra infos values.
     */
    private final Map<String, Deque<CdpPayload>> requestExtraInfos = new ConcurrentHashMap<>();
    /**
     * Mapped response extra infos values.
     */
    private final Map<String, Deque<CdpPayload>> responseExtraInfos = new ConcurrentHashMap<>();
    /**
     * Mapped queued redirect infos values.
     */
    private final Map<String, Deque<RedirectInfo>> queuedRedirectInfos = new ConcurrentHashMap<>();
    /**
     * Mapped queued event groups values.
     */
    private final Map<String, QueuedEventGroup> queuedEventGroups = new ConcurrentHashMap<>();
    /**
     * Mapped queued events values.
     */
    private final Map<String, CdpPayload> queuedEvents = new ConcurrentHashMap<>();

    /**
     * Handles forget.
     *
     * @param networkRequestId network request ID value
     */
    public void forget(String networkRequestId) {
        requests.remove(networkRequestId);
        requestWillBeSentEvents.remove(networkRequestId);
        requestPausedEvents.remove(networkRequestId);
        responses.remove(networkRequestId);
        requestExtraInfos.remove(networkRequestId);
        responseExtraInfos.remove(networkRequestId);
        queuedRedirectInfos.remove(networkRequestId);
        queuedEventGroups.remove(networkRequestId);
        queuedEvents.remove(networkRequestId);
    }

    /**
     * Handles record request.
     *
     * @param request request object
     */
    public void recordRequest(CdpRequest request) {
        storeRequest(request.requestId(), request);
    }

    /**
     * Returns the request.
     *
     * @param requestId request ID value
     * @return optional value
     */
    public Optional<CdpRequest> request(String requestId) {
        return getRequest(requestId);
    }

    /**
     * Removes request.
     *
     * @param requestId request id
     * @return remove request value
     */
    public Optional<CdpRequest> removeRequest(String requestId) {
        Optional<CdpRequest> removed = forgetRequest(requestId);
        responses.remove(requestId);
        return removed;
    }

    /**
     * Handles record response.
     *
     * @param requestId request ID value
     * @param response  response object
     */
    public void recordResponse(String requestId, CdpResponse response) {
        responses.put(requestId, response);
    }

    /**
     * Returns the response.
     *
     * @param requestId request ID value
     * @return optional value
     */
    public Optional<CdpResponse> response(String requestId) {
        return Optional.ofNullable(responses.get(requestId));
    }

    /**
     * Handles record request extra info.
     *
     * @param requestId request ID value
     * @param payload   protocol payload
     */
    public void recordRequestExtraInfo(String requestId, CdpPayload payload) {
        requestExtraInfoQueue(requestId).add(payload);
    }

    /**
     * Returns the request extra info.
     *
     * @param requestId request ID value
     * @return values
     */
    public List<CdpPayload> requestExtraInfo(String requestId) {
        return List.copyOf(requestExtraInfoQueue(requestId));
    }

    /**
     * Returns the take request extra info.
     *
     * @param requestId request ID value
     * @return optional value
     */
    public Optional<CdpPayload> takeRequestExtraInfo(String requestId) {
        return Optional.ofNullable(requestExtraInfoQueue(requestId).poll());
    }

    /**
     * Handles record response extra info.
     *
     * @param requestId request ID value
     * @param payload   protocol payload
     */
    public void recordResponseExtraInfo(String requestId, CdpPayload payload) {
        responseExtraInfoQueue(requestId).add(payload);
    }

    /**
     * Returns the response extra info.
     *
     * @param requestId request ID value
     * @return values
     */
    public List<CdpPayload> responseExtraInfo(String requestId) {
        return List.copyOf(responseExtraInfoQueue(requestId));
    }

    /**
     * Returns the take response extra info.
     *
     * @param requestId request ID value
     * @return optional value
     */
    public Optional<CdpPayload> takeResponseExtraInfo(String requestId) {
        return Optional.ofNullable(responseExtraInfoQueue(requestId).poll());
    }

    /**
     * Returns the latest response extra info.
     *
     * @param requestId request ID value
     * @return optional value
     */
    public Optional<CdpPayload> latestResponseExtraInfo(String requestId) {
        return Optional.ofNullable(responseExtraInfoQueue(requestId).peekLast());
    }

    /**
     * Handles queue event.
     *
     * @param requestId request ID value
     * @param payload   protocol payload
     */
    public void queueEvent(String requestId, CdpPayload payload) {
        queuedEvents.put(requestId, payload);
    }

    /**
     * Returns the poll queued event.
     *
     * @param requestId request ID value
     * @return optional value
     */
    public Optional<CdpPayload> pollQueuedEvent(String requestId) {
        return Optional.ofNullable(queuedEvents.remove(requestId));
    }

    /**
     * Returns the in flight request count.
     *
     * @return in flight request count value
     */
    public int inFlightRequestCount() {
        int count = 0;
        for (CdpRequest request : requests.values()) {
            if (request.response().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Handles store request will be sent.
     *
     * @param networkRequestId network request ID value
     * @param event            event type
     */
    public void storeRequestWillBeSent(String networkRequestId, CdpPayload event) {
        requestWillBeSentEvents.put(networkRequestId, event);
    }

    /**
     * Returns the request will be sent.
     *
     * @param networkRequestId network request id
     * @return request will be sent
     */
    public Optional<CdpPayload> getRequestWillBeSent(String networkRequestId) {
        return Optional.ofNullable(requestWillBeSentEvents.get(networkRequestId));
    }

    /**
     * Handles forget request will be sent.
     *
     * @param networkRequestId network request ID value
     */
    public void forgetRequestWillBeSent(String networkRequestId) {
        requestWillBeSentEvents.remove(networkRequestId);
    }

    /**
     * Handles store request paused.
     *
     * @param networkRequestId network request ID value
     * @param event            event type
     */
    public void storeRequestPaused(String networkRequestId, CdpPayload event) {
        requestPausedEvents.put(networkRequestId, event);
    }

    /**
     * Returns the request paused.
     *
     * @param networkRequestId network request id
     * @return request paused
     */
    public Optional<CdpPayload> getRequestPaused(String networkRequestId) {
        return Optional.ofNullable(requestPausedEvents.get(networkRequestId));
    }

    /**
     * Handles forget request paused.
     *
     * @param networkRequestId network request ID value
     */
    public void forgetRequestPaused(String networkRequestId) {
        requestPausedEvents.remove(networkRequestId);
    }

    /**
     * Handles store request.
     *
     * @param networkRequestId network request ID value
     * @param request          request object
     */
    public void storeRequest(String networkRequestId, CdpRequest request) {
        requests.put(networkRequestId, request);
    }

    /**
     * Returns the request.
     *
     * @param networkRequestId network request id
     * @return request
     */
    public Optional<CdpRequest> getRequest(String networkRequestId) {
        return Optional.ofNullable(requests.get(networkRequestId));
    }

    /**
     * Returns the forget request.
     *
     * @param networkRequestId network request ID value
     * @return optional value
     */
    public Optional<CdpRequest> forgetRequest(String networkRequestId) {
        responses.remove(networkRequestId);
        return Optional.ofNullable(requests.remove(networkRequestId));
    }

    /**
     * Handles queue redirect info.
     *
     * @param fetchRequestId fetch request ID value
     * @param redirectInfo   redirect info value
     */
    public void queueRedirectInfo(String fetchRequestId, RedirectInfo redirectInfo) {
        Assert.notNull(redirectInfo, "redirectInfo");
        queuedRedirectInfo(fetchRequestId).add(redirectInfo);
    }

    /**
     * Returns the take queued redirect info.
     *
     * @param fetchRequestId fetch request ID value
     * @return optional value
     */
    public Optional<RedirectInfo> takeQueuedRedirectInfo(String fetchRequestId) {
        return Optional.ofNullable(queuedRedirectInfo(fetchRequestId).poll());
    }

    /**
     * Returns the queued event group.
     *
     * @param networkRequestId network request id
     * @return queued event group
     */
    public Optional<QueuedEventGroup> getQueuedEventGroup(String networkRequestId) {
        return Optional.ofNullable(queuedEventGroups.get(networkRequestId));
    }

    /**
     * Handles queue event group.
     *
     * @param networkRequestId network request ID value
     * @param event            event type
     */
    public void queueEventGroup(String networkRequestId, QueuedEventGroup event) {
        Assert.notNull(event, "event");
        queuedEventGroups.put(networkRequestId, event);
    }

    /**
     * Handles forget queued event group.
     *
     * @param networkRequestId network request ID value
     */
    public void forgetQueuedEventGroup(String networkRequestId) {
        queuedEventGroups.remove(networkRequestId);
    }

    /**
     * Returns the debug state.
     *
     * @return debug state value
     */
    public String debugState() {
        return "CdpNetworkEventManager{requests=" + requests.size() + ", requestWillBeSent="
                + requestWillBeSentEvents.size() + ", requestPaused=" + requestPausedEvents.size()
                + ", responseExtraInfo=" + responseExtraInfos.size() + ", queuedRedirectInfo="
                + queuedRedirectInfos.size() + ", queuedEventGroup=" + queuedEventGroups.size() + Symbol.C_BRACE_RIGHT;
    }

    /**
     * Returns the request extra info queue.
     *
     * @param requestId request ID value
     * @return request extra info queue value
     */
    private Deque<CdpPayload> requestExtraInfoQueue(String requestId) {
        return requestExtraInfos.computeIfAbsent(requestId, ignored -> new ConcurrentLinkedDeque<>());
    }

    /**
     * Returns the response extra info queue.
     *
     * @param requestId request ID value
     * @return response extra info queue value
     */
    private Deque<CdpPayload> responseExtraInfoQueue(String requestId) {
        return responseExtraInfos.computeIfAbsent(requestId, ignored -> new ConcurrentLinkedDeque<>());
    }

    /**
     * Returns the queued redirect info.
     *
     * @param fetchRequestId fetch request ID value
     * @return queued redirect info value
     */
    private Deque<RedirectInfo> queuedRedirectInfo(String fetchRequestId) {
        return queuedRedirectInfos.computeIfAbsent(fetchRequestId, ignored -> new ConcurrentLinkedDeque<>());
    }

    /**
     * Represents queued event group.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class QueuedEventGroup {

        /**
         * Current response received event.
         */
        private final CdpPayload responseReceivedEvent;
        /**
         * Current loading finished event.
         */
        private CdpPayload loadingFinishedEvent;
        /**
         * Current loading failed event.
         */
        private CdpPayload loadingFailedEvent;

        /**
         * Creates an instance.
         *
         * @param responseReceivedEvent response received event value
         */
        public QueuedEventGroup(CdpPayload responseReceivedEvent) {
            this.responseReceivedEvent = Assert.notNull(responseReceivedEvent, "responseReceivedEvent");
        }

        /**
         * Returns the response received event.
         *
         * @return response received event value
         */
        public CdpPayload responseReceivedEvent() {
            return responseReceivedEvent;
        }

        /**
         * Returns the loading finished event.
         *
         * @return optional value
         */
        public Optional<CdpPayload> loadingFinishedEvent() {
            return Optional.ofNullable(loadingFinishedEvent);
        }

        /**
         * Updates loading finished event.
         *
         * @param loadingFinishedEvent loading finished event value
         */
        public void setLoadingFinishedEvent(CdpPayload loadingFinishedEvent) {
            this.loadingFinishedEvent = loadingFinishedEvent;
        }

        /**
         * Returns the loading failed event.
         *
         * @return optional value
         */
        public Optional<CdpPayload> loadingFailedEvent() {
            return Optional.ofNullable(loadingFailedEvent);
        }

        /**
         * Updates loading failed event.
         *
         * @param loadingFailedEvent loading failed event value
         */
        public void setLoadingFailedEvent(CdpPayload loadingFailedEvent) {
            this.loadingFailedEvent = loadingFailedEvent;
        }
    }

    /**
     * Represents redirect info.
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    public static final class RedirectInfo {

        /**
         * Current event.
         */
        private final CdpPayload event;

        /**
         * Fetch requestId.
         */
        private final String fetchRequestId;

        /**
         * Creates an instance.
         *
         * @param event          event type
         * @param fetchRequestId fetch request ID value
         */
        public RedirectInfo(CdpPayload event, String fetchRequestId) {
            this.event = Assert.notNull(event, "event");
            this.fetchRequestId = fetchRequestId;
        }

        /**
         * Returns the event.
         *
         * @return event value
         */
        public CdpPayload event() {
            return event;
        }

        /**
         * Returns the fetch request ID.
         *
         * @return optional value
         */
        public Optional<String> fetchRequestId() {
            return Optional.ofNullable(fetchRequestId);
        }
    }

}
