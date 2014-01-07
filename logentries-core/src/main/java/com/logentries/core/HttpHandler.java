package com.logentries.core;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;

/**
 * Wraps a log event in an HTTP request envelope.
 */
class HttpHandler extends ChannelOutboundHandlerAdapter {

    private final String token, host, key, endpoint;

    public HttpHandler(String token, String host, String key) {
        this.token = token;
        this.host = host;
        this.key = key;
        this.endpoint = "/" + key + "/hosts/" + host + "/" + token + "/realtime=1";
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf payload = (ByteBuf) msg;
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, endpoint, payload);
            ctx.write(request, promise);
        }
    }
}
