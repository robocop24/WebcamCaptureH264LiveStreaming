package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.concurrent.GlobalEventExecutor;

public class Server {
    public static void main(String[] args) throws InterruptedException {
        new Server(8000,8080).run();
    }

    private final  int port1;
    private final int port2;

    public Server(int port1,int port2){
        this.port1 = port1;
        this.port2 = port2;
    }

    public void run() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();
        ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast("Frame Decoder",new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4,0,4));
                            pipeline.addLast("Server Handler",new ServerHandler());
                            pipeline.addLast("Frame Encoder",new LengthFieldPrepender(4));
                        }
                    });
            //for camera.
            Channel channel1 = bootstrap.bind(port1).channel();
            group.add(channel1);
            //for client.
            Channel channel2 = bootstrap.bind(port2).channel();
            group.add(channel2);
            group.newCloseFuture().sync();
            System.out.println("Server port: 8000 for camera & 8080 for client");

        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

}
