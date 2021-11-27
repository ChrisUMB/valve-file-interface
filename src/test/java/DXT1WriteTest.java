import blue.sparse.vfi.files.vtf.image.ImageDataFormat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class DXT1WriteTest {

    public static void main(String[] args) throws IOException {
        BufferedImage image = ImageIO.read(new File("tests-in/metal_box.png"));

//        for (int i = 0; i < 10; i++) {
        long start = System.currentTimeMillis();
        byte[] data = ImageDataFormat.DXT1.write(image);
        long diff1 = System.currentTimeMillis() - start;
        start = System.currentTimeMillis();
        image = ImageDataFormat.DXT1.read(image.getWidth(), image.getHeight(), ByteBuffer.wrap(data));
        long diff2 = System.currentTimeMillis() - start;
        System.out.printf("Write: %,dms, Read: %,dms%n", diff1, diff2);
//        }

        ImageIO.write(image, "PNG", new File("tests-out/metal_box.dxt1.png"));
    }

}
