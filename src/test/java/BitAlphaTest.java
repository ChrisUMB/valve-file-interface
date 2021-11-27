import blue.sparse.vfi.assets.ValveTexture;
import blue.sparse.vfi.files.vtf.VTFTextureFlag;
import blue.sparse.vfi.files.vtf.image.ImageDataFormat;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public final class BitAlphaTest {

	public static void main(String[] args) throws IOException {
		File file = new File("source/1ba.png");
		ValveTexture load = ValveTexture.create(ImageIO.read(file));
		load.setVersion(7, 0);
		load.setHighFormat(ImageDataFormat.DXT1_ONEBITALPHA);
		load.setFlag(VTFTextureFlag.ONEBITALPHA, true);
		load.save(new File("source/concrete_modular_wall001a.vtf"));
	}

}
