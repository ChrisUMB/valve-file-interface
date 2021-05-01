import blue.sparse.vfi.assets.ValveTexture;

import java.io.File;
import java.io.IOException;

public class ValveTextureTest {

	public static void main(String[] args) throws IOException {
		File input = new File("source/7-2.vtf");
		ValveTexture texture = ValveTexture.load(input);

		File out = new File("source/7-2.png");
		texture.export("PNG", out);
	}

}
