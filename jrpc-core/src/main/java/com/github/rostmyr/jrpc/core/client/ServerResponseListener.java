package com.github.rostmyr.jrpc.core.client;

import io.netty.buffer.ByteBuf;

import com.github.rostmyr.jrpc.core.server.registry.ResourceRegistry;
import com.github.rostmyr.jrpc.core.service.ResponseType;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rostyslav Myroshnychenko
 * on 27.05.2018.
 */
public class ServerResponseListener {
    private final Map<Integer, ResponseFuture> responseListenerByRequestId = new ConcurrentHashMap<>();
    private final ResourceRegistry resourceRegistry;

    public ServerResponseListener(ResourceRegistry resourceRegistry) {
        this.resourceRegistry = resourceRegistry;
    }

    /**
     * Adds a new listener for the server response
     *
     * @param requestId    client's request id
     * @param responseType expected response type
     * @return a future to wait for result
     */
    public ResponseFuture waitFor(int requestId, ResponseType responseType) {
        ResponseFuture listener = new ResponseFuture(responseType);
        responseListenerByRequestId.put(requestId, listener);
        return listener;
    }

    /**
     * Sets a response for the client's request
     *
     * @param requestId client's request id
     * @param buffer    buffer to extract data from
     */
    public void setResponse(int requestId, ByteBuf buffer) {
        ResponseFuture listener = responseListenerByRequestId.remove(requestId);
        if (listener != null) {
            listener.complete(buffer);
        }
    }

    /**
     * Completes response listener exceptionally
     *
     * @param requestId client's request id
     * @param cause     cause
     */
    public void setExceptional(int requestId, Throwable cause) {
        ResponseFuture listener = responseListenerByRequestId.remove(requestId);
        if (listener != null) {
            listener.completeExceptionally(cause);
        }
    }

    /**
     * Removes all listeners which are currently waiting for response.
     * It should be called by {@link ClientChannelImpl#shutdown()} method only
     */
    public void destroy() {
        responseListenerByRequestId.clear();
    }

    public class ResponseFuture extends CompletableFuture {
        private final ResponseType responseType;

        public ResponseFuture(ResponseType responseType) {
            this.responseType = responseType;
        }

        @SuppressWarnings("unchecked")
        private void complete(ByteBuf buffer) {
            complete(responseType.read(buffer, resourceRegistry));
        }
    }
}
