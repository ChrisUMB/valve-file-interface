package blue.sparse.vfi.files.vtf.image;

import java.awt.image.BufferedImage;

public interface ImageFormatWriter {

	byte[] write(BufferedImage image);

}
