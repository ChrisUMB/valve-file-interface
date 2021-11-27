import blue.sparse.vfi.assets.ValveTexture;

import java.io.File;
import java.io.IOException;

public class SLDFJHSEDIOUFH {

	public static void main(String[] args) throws IOException {
		File in = new File("source/metal_box.vtf");
		ValveTexture texture = ValveTexture.load(in);

		System.out.println("texture.getHighFormat() = " + texture.getHighFormat());
		System.out.println("texture.getLowFormat() = " + texture.getLowFormat());

//		texture.save(new File("source/gallerytard.vtf"));
		texture.export("PNG", new File("source/metal_box_2.png"));
	}

}
