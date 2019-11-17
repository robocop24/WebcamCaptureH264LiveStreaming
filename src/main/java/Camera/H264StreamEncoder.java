package Camera;

import com.xuggle.xuggler.*;
import com.xuggle.xuggler.IStreamCoder.Direction;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import com.xuggle.xuggler.IPixelFormat.Type;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelOutboundBuffer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class H264StreamEncoder {
    protected final IStreamCoder iStreamCoder = IStreamCoder.make(Direction.ENCODING, ICodec.ID.CODEC_ID_H264);
    protected Dimension dimension;
    protected final IPacket iPacket = IPacket.make();
    protected long startTime ;

    public H264StreamEncoder(Dimension dimension){
        this.dimension = dimension;
        initialize();
    }
    //config the IstreamCoder.
    private void initialize(){
        //setup
        iStreamCoder.setNumPicturesInGroupOfPictures(25);

        iStreamCoder.setBitRate(200000);
        iStreamCoder.setBitRateTolerance(10000);
        iStreamCoder.setPixelType(IPixelFormat.Type.YUV420P);
        iStreamCoder.setHeight(dimension.height);
        iStreamCoder.setWidth(dimension.width);
        iStreamCoder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, true);
        iStreamCoder.setGlobalQuality(0);
        //rate
        IRational rate = IRational.make(25, 1);
        iStreamCoder.setFrameRate(rate);
        //time base
        //iStreamCoder.setAutomaticallyStampPacketsForStream(true);
        iStreamCoder.setTimeBase(IRational.make(rate.getDenominator(),rate.getNumerator()));
        IMetaData codecOptions = IMetaData.make();
        codecOptions.setValue("tune", "zerolatency");// equals -tune zerolatency in ffmpeg
        //open it
        int revl = iStreamCoder.open(codecOptions, null);
        if (revl < 0) {
            throw new RuntimeException("could not open the coder");
        }
    }

    public Object encode(Object msg) throws Exception{
        if (msg == null) {
            return null;
        }
        if (!(msg instanceof BufferedImage)) {
            throw new IllegalArgumentException("your need to pass into an bufferedimage");
        }
        BufferedImage bufferedImage = (BufferedImage) msg;
        BufferedImage convetedImage = ConverterFactory.convertToType(bufferedImage,BufferedImage.TYPE_3BYTE_BGR);
        IConverter converter = ConverterFactory.createConverter(convetedImage, Type.YUV420P);
        long now = System.currentTimeMillis();
        if (startTime == 0) {
            startTime = now;
        }
        //to frame.
        IVideoPicture pFrame = converter.toPicture(convetedImage, (now - startTime)*1000);
        //pFrame.setQuality(0);
        iStreamCoder.encodeVideo(iPacket, pFrame, 0) ;
        //free the MEM
        pFrame.delete();
        converter.delete();

        if (iPacket.isComplete()) {
            try {
                ByteBuffer byteBuffer = iPacket.getByteBuffer();
                //ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer(byteBuffer.capacity());
                ByteBuf byteBuf = Unpooled.copiedBuffer(byteBuffer.order(ByteOrder.BIG_ENDIAN));
                return byteBuf;
            }finally {
                iPacket.reset();
            }
        }


        return null;
    }
}
