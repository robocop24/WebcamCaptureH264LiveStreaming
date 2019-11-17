package Camera;

import com.github.sarxos.webcam.Webcam;

import java.awt.Dimension;

public class Camera {

    public static void main(String[] args) throws InterruptedException {
        //webcam setup.
        Webcam.setAutoOpenMode(true);
        Webcam webcam = Webcam.getDefault();
        //Dimension dimension = new Dimension(320, 240);
        Dimension dimension = new Dimension(640,480);
        webcam.setViewSize(dimension);

        Stream stream = new Stream(webcam,dimension);
        //stream.start("34.93.243.51",8000);
        //stream.start("192.168.0.121",8000);
        stream.start("127.0.0.1",8000);

    }
}
