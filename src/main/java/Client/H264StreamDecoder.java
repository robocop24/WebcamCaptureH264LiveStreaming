package Client;

import com.xuggle.ferry.IBuffer;
import com.xuggle.xuggler.*;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.Charset;

class H264StreamDecoder extends SimpleChannelInboundHandler<ByteBuf> {
    protected static Dimension dimension = new Dimension(640,480 );
    private final IStreamCoder iStreamCoder = IStreamCoder.make(IStreamCoder.Direction.DECODING, ICodec.ID.CODEC_ID_H264);
    protected final ConverterFactory.Type type = ConverterFactory.findRegisteredConverter(ConverterFactory.XUGGLER_BGR_24);
    private final static SingleVideoDisplayWindow displayWindow = new SingleVideoDisplayWindow("Stream 1234",dimension);
    public H264StreamDecoder(){
        iStreamCoder.open(null,null);
    }
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {

        int size = byteBuf.readableBytes();
        // start to decode
        IBuffer iBuffer = IBuffer.make(null, size);
        IPacket iPacket = IPacket.make(iBuffer);
        iPacket.getByteBuffer().put(byteBuf.nioBuffer());
        // decode the packet
        if (!iPacket.isComplete()) {
            System.out.println("packet not complete"); ;
        }

        IVideoPicture picture = IVideoPicture.make(IPixelFormat.Type.YUV420P, dimension.width, dimension.height);

        try{
            // decode the packet into the video picture
            int position = 0;
            int packageSize = iPacket.getSize();
            //System.out.println(packageSize);
            while (position < packageSize){

                position = position + iStreamCoder.decodeVideo(picture, iPacket, position);
                //System.out.println(position);
                if (position < 0) {
                    System.out.println("error decoding video");
                }
                if (picture.isComplete()){
                    IConverter converter = ConverterFactory.createConverter(type.getDescriptor(), picture);
                    BufferedImage image = converter.toImage(picture);
                    //Image i =  SwingFXUtils.toFXImage(image,null);

                    displayWindow.updateImage(image);

                    converter.delete();
                }else {
                    picture.delete();
                    iPacket.delete();
                }
                // clean the picture and reuse it
                picture.getByteBuffer().clear();
            }
        }finally {
            if (picture != null) {
                picture.delete();
            }
            iPacket.delete();
            // ByteBufferUtil.destroy(data);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        displayWindow.setVisible(true);
        System.out.println("Connect");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        displayWindow.close();
        System.out.println("Disconnect");
    }
}
