package com.logentries.core;

import com.google.common.base.Charsets;

import com.logentries.core.format.Formatters;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

import java.util.UUID;

/**
 * Turns an incoming string into a valid token-based log entry.
 */
class SimpleEntryHandler extends ChannelOutboundHandlerAdapter {

	private final String token;

	public SimpleEntryHandler(UUID token) {
		this.token = token.toString();
	}

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (msg instanceof String) {
			String formatted = Formatters.formatLogEntry((String) msg);
			ByteBuf buf = Unpooled.copiedBuffer(token + " " + formatted, Charsets.UTF_8);
			ctx.writeAndFlush(buf, promise);
		} else {
			ctx.write(msg, promise);
		}
    }
}
