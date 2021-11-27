import blue.sparse.vfi.assets.ValveTexture;
import blue.sparse.vfi.files.vtf.VTFFace;
import blue.sparse.vfi.files.vtf.VTFFrame;
import blue.sparse.vfi.files.vtf.VTFMipmap;
import blue.sparse.vfi.files.vtf.image.ImageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class FaceTextureTest {

	public static void main(String[] args) throws IOException {

		File file = new File("source/gallery.vtf");
		ValveTexture texture = ValveTexture.load(file);
		VTFMipmap mipmap = texture.getMipmaps().get(0);
		VTFFrame frame = mipmap.getFrames().get(0);
		List<VTFFace> faces = frame.getFaces();
		int i = 0;
		for (VTFFace face : faces) {
			File out = new File("source/gallery" + i + ".png");
			BufferedImage image = face.getImage();
			ImageIO.write(image, "PNG", out);
			i++;
		}
//		texture.export("PNG", out);

	}

}
