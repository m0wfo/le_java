package com.logentries.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Handles connection retry behaviour.
 * @author chris
 */
public class ReconnectHandler extends ChannelInboundHandlerAdapter {

    private final FailureHandler handler;

    public ReconnectHandler(FailureHandler handler) {
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
