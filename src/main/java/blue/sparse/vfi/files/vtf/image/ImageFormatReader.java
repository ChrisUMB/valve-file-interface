package blue.sparse.vfi.files.vtf.image;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public interface ImageFormatReader {

	BufferedImage read(int width, int height, ByteBuffer buffer);

}
