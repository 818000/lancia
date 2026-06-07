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
package org.miaixz.lancia;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.miaixz.bus.core.lang.Optional;
import org.miaixz.lancia.kernel.Frame;

/**
 * Public request API.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public abstract class Request {

    /**
     * Creates a request.
     */
    protected Request() {
        // No initialization required.
    }

    /**
     * Continues an intercepted request.
     *
     * @param overrides request overrides
     * @return protocol result
     */
    public abstract CompletableFuture<? extends Payload> continueRequest(Map<String, Object> overrides);

    /**
     * Aborts an intercepted request.
     *
     * @param errorCode error code
     * @return protocol result
     */
    public abstract CompletableFuture<? extends Payload> abort(String errorCode);

    /**
     * Responds to an intercepted request.
     *
     * @param response response data
     * @return protocol result
     */
    public abstract CompletableFuture<? extends Payload> respond(Map<String, Object> response);

    /**
     * Finalizes queued interceptions.
     *
     * @return protocol result
     */
    public abstract CompletableFuture<? extends Payload> finalizeInterceptions();

    /**
     * Returns queued continue request overrides.
     *
     * @return continue request overrides
     */
    public abstract Map<String, Object> continueRequestOverrides();

    /**
     * Returns queued response data for this request.
     *
     * @return response data
     */
    public abstract Optional<Map<String, Object>> responseForRequest();

    /**
     * Returns queued abort error reason.
     *
     * @return abort error reason
     */
    public abstract Optional<String> abortErrorReason();

    /**
     * Returns current interception resolution state.
     *
     * @return interception resolution state
     */
    public abstract Map<String, Object> interceptResolutionState();

    /**
     * Returns whether request interception has already been resolved.
     *
     * @return handled state
     */
    public abstract boolean isInterceptResolutionHandled();

    /**
     * Enqueues an interception action.
     *
     * @param action interception action
     */
    public abstract void enqueueInterceptAction(Supplier<CompletableFuture<?>> action);

    /**
     * Returns the CDP session.
     *
     * @return CDP session
     */
    public abstract Session client();

    /**
     * Returns the request id.
     *
     * @return request id
     */
    public abstract String id();

    /**
     * Returns request URL.
     *
     * @return URL
     */
    public abstract String url();

    /**
     * Returns request method.
     *
     * @return method
     */
    public abstract String method();

    /**
     * Returns request headers.
     *
     * @return headers
     */
    public abstract Map<String, String> headers();

    /**
     * Returns request post data.
     *
     * @return post data
     */
    public abstract Optional<String> postData();

    /**
     * Returns whether post data is available.
     *
     * @return post data flag
     */
    public abstract boolean hasPostData();

    /**
     * Fetches request post data.
     *
     * @return post data
     */
    public abstract Optional<String> fetchPostData();

    /**
     * Returns request resource type.
     *
     * @return resource type
     */
    public abstract String resourceType();

    /**
     * Returns request frame.
     *
     * @return frame
     */
    public abstract Optional<? extends Frame> frame();

    /**
     * Returns whether this request is a navigation request.
     *
     * @return navigation request flag
     */
    public abstract boolean isNavigationRequest();

    /**
     * Returns request initiator data.
     *
     * @return initiator data
     */
    public abstract Map<String, Object> initiator();

    /**
     * Returns the redirect chain.
     *
     * @return redirect chain
     */
    public abstract List<? extends Request> redirectChain();

    /**
     * Returns the response associated with this request.
     *
     * @return response
     */
    public abstract Optional<? extends Response> response();

    /**
     * Returns request failure data.
     *
     * @return failure data
     */
    public abstract Optional<Map<String, String>> failure();

}
