package com.logentries.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Handles connection retry behaviour.
 */
class ReconnectHandler extends ChannelInboundHandlerAdapter {

    private final FailureManager handler;

    public ReconnectHandler(FailureManager handler) {
        this.handler = handler;
    }

    /**
     * Callback invoked when connection is lost.
     * @param ctx channel context
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        handler.handleFailure();
    }
}
