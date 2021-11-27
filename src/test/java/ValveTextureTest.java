import blue.sparse.vfi.assets.ValveTexture;
import blue.sparse.vfi.files.vtf.VTFResource;
import blue.sparse.vfi.files.vtf.image.ImageDataFormat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

public class ValveTextureTest {

	public static void main(String[] args) throws IOException {
		File in = new File("source/eutaxy-pack");

		File out = new File("source/eutaxy-pack-dxt1");
		out.mkdirs();

		Files.walk(in.toPath()).forEach(path -> {
			File file = path.toFile();
			String name = file.getAbsolutePath();
			if (!name.endsWith("png")) {
				return;
			}
			try {
				BufferedImage image = ImageIO.read(file);

				String assetName = name.substring(in.getAbsolutePath().length());

				File write = new File(out, assetName);

				ImageDataFormat format = ImageDataFormat.DXT1_ONEBITALPHA;
				byte[] bytes = format.write(image);
				ByteBuffer buffer = ByteBuffer.wrap(bytes);

				BufferedImage read = format.read(image.getWidth(), image.getHeight(), buffer);
				write.getParentFile().mkdirs();
				ImageIO.write(read, "PNG", write);
			} catch (IOException e) {
				e.printStackTrace();
			}


//			write.getParentFile().mkdirs();
//			try {
//				System.out.println("Creating VTF for " + assetName);
//				ValveTexture texture = ValveTexture.create(ImageIO.read(file));
//				texture.export("PNG", write);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		});
	}
}
