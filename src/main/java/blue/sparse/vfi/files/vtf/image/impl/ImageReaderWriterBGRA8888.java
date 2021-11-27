package blue.sparse.vfi.files.vtf.image.impl;

import blue.sparse.vfi.files.vtf.image.ImageFormatReader;
import blue.sparse.vfi.files.vtf.image.ImageFormatWriter;
import blue.sparse.vfi.files.vtf.image.ImageUtil;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class ImageReaderWriterBGRA8888 implements ImageFormatReader, ImageFormatWriter {

	@Override
	public BufferedImage read(int width, int height, ByteBuffer buffer) {
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int b = buffer.get() & 0xFF;
				int g = buffer.get() & 0xFF;
				int r = buffer.get() & 0xFF;
				int a = buffer.get() & 0xFF;
				result.setRGB(x, y, ImageUtil.encodeABGR8888(a, b, g, r));
			}
		}

		return result;
	}

	@Override
	public byte[] write(BufferedImage image) {
		byte[] bytes = new byte[(image.getWidth() * image.getHeight()) * 3];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int argb = image.getRGB(x, y);
				int b = argb & 0xFF;
				int g = argb >> 8 & 0xFF;
				int r = argb >> 16 & 0xFF;
				int a = argb >> 24 & 0xFF;
				buffer.put((byte) b);
				buffer.put((byte) g);
				buffer.put((byte) r);
				buffer.put((byte) a);
			}
		}

		return bytes;
	}
}
