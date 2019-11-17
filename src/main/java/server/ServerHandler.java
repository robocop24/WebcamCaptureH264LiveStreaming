package server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

public class ServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private final static ChannelGroup g = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        g.add(ctx.channel());
    }
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        /*int l = byteBuf.readableBytes();
        System.out.println(l);*/
        Channel in = channelHandlerContext.channel();
        for (Channel channel:g){
            if(channel!=in){
                ReferenceCountUtil.retain(byteBuf);
                channel.writeAndFlush(byteBuf);
            }
        }
    }

}
