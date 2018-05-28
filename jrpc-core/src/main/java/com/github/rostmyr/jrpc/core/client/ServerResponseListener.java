package com.github.rostmyr.jrpc.core.client;

import io.netty.buffer.ByteBuf;

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

    public ResponseFuture waitFor(int requestId, ResponseType responseType) {
        ResponseFuture listener = new ResponseFuture(responseType);
        responseListenerByRequestId.put(requestId, listener);
        return listener;
    }

    public void setResponse(int requestId, ByteBuf buffer) {
        ResponseFuture listener = responseListenerByRequestId.remove(requestId);
        if (listener != null) {
            listener.complete(buffer);
        }
    }

    public void setExceptional(int requestId, Throwable cause) {
        ResponseFuture listener = responseListenerByRequestId.remove(requestId);
        if (listener != null) {
            listener.completeExceptionally(cause);
        }
    }

    public static class ResponseFuture extends CompletableFuture {
        private final ResponseType responseType;

        public ResponseFuture(ResponseType responseType) {
            this.responseType = responseType;
        }

        @SuppressWarnings("unchecked")
        private void complete(ByteBuf buffer) {
            complete(responseType.read(buffer));
        }
    }
}
