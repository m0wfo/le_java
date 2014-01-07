package com.logentries.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;

import java.util.UUID;

/**
 * Wraps a log event in an HTTP request envelope.
 */
class HttpHandler extends ChannelOutboundHandlerAdapter {

    public static final String API_LEVEL = "2";

    private final String path;

    public HttpHandler(UUID token) {
        this.path = "/api/v" + API_LEVEL + "/logs/" + token;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            // TODO- wrap payload in JSON envelope
            ByteBuf payload = (ByteBuf) msg;
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, path, payload);
            ctx.write(request, promise);
        }
    }
}
