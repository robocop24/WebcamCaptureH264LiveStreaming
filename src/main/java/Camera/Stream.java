package Camera;

import com.github.sarxos.webcam.Webcam;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.awt.Dimension;

public class Stream {

    protected final Webcam webcam;
    protected final Dimension dimension;

    public Stream(Webcam webcam, Dimension dimension) {
        this.webcam = webcam;
        this.dimension = dimension;

    }

    public void start(String host, int port) throws InterruptedException {
        //run your client.
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            //ch.pipeline().addLast("frame", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                            //ch.pipeline().addLast("decoder", new StringDecoder());
                            //ch.pipeline().addLast("encoder", new StringEncoder());
                            //ch.pipeline().addLast("message", new Message(webcam,dimension));
                            //outbound -- add size as header on packet.
                            ch.pipeline().addLast("frame decoder",new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4,0,4));
                            ch.pipeline().addLast("frame encoder", new LengthFieldPrepender(4));
                            ch.pipeline().addLast("handler",new ClientStreamHandler(webcam,dimension));
                        }
                    });
            // connect client to the Server.
            ChannelFuture channelFuture =  b.connect(host, port);
            channelFuture.channel().closeFuture().sync();

        } finally {
            workerGroup.shutdownGracefully();
        }
    }


}
