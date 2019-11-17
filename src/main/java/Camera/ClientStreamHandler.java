package Camera;

import com.github.sarxos.webcam.Webcam;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.*;

public class ClientStreamHandler extends SimpleChannelInboundHandler<ByteBuf> {

    protected final Webcam webcam;
    protected final Dimension dimension;
    protected ScheduledExecutorService timeWorker;
    protected ExecutorService encodeWorker;
    protected ScheduledFuture<?> imageGrabTaskFuture;
    protected final H264StreamEncoder h264StreamEncoder;
    Channel channel;

    public ClientStreamHandler(Webcam webcam, Dimension dimension){
        this.webcam = webcam;
        this.dimension = dimension;
        this.timeWorker = new ScheduledThreadPoolExecutor(1);
        this.encodeWorker = Executors.newSingleThreadExecutor();
        this.h264StreamEncoder = new H264StreamEncoder(dimension);
    }
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        channel=ctx.channel();
        System.out.println("Cam1 Connect !!");
        Runnable imageGrabTask = new ImageGrabTask();
        ScheduledFuture<?> imageGrabFuture =
                timeWorker.scheduleWithFixedDelay(imageGrabTask,
                        0,
                        1000 / 25,
                        TimeUnit.MILLISECONDS);
        imageGrabTaskFuture = imageGrabFuture;
    }

    private class ImageGrabTask implements Runnable {

        @Override
        public void run() {
            BufferedImage bufferedImage = webcam.getImage();
            encodeWorker.execute(new EncodeTask(bufferedImage));
        }

    }

    private class EncodeTask implements Runnable {
        private final BufferedImage image;

        public EncodeTask(BufferedImage image) {
            super();
            this.image = image;
        }

        @Override
        public void run() {
            try {
                Object msg = h264StreamEncoder.encode(image);
                if (msg != null) {
                    channel.writeAndFlush(msg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        System.out.println("Disconnect");
        webcam.close();
        timeWorker.shutdown();
        encodeWorker.shutdown();
    }
}
