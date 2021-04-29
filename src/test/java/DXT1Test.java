import blue.sparse.vfi.files.vtf.image.ImageDataFormat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class DXT1Test {

	public static void main(String[] args) throws IOException {
		File in = new File("source/dxt1_512.bin");
		byte[] bytes = Files.readAllBytes(in.toPath());
		ByteBuffer buffer = ByteBuffer.wrap(bytes);

		BufferedImage image = ImageDataFormat.DXT1.read(512, 512, buffer);
		File out = new File("source/dxt1_512.png");
		ImageIO.write(image, "PNG", out);

	}

}
