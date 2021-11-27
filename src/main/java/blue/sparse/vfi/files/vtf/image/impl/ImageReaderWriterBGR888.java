package blue.sparse.vfi.files.vtf.image.impl;

import blue.sparse.vfi.files.vtf.image.ImageFormatReader;
import blue.sparse.vfi.files.vtf.image.ImageFormatWriter;
import blue.sparse.vfi.files.vtf.image.ImageUtil;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public final class ImageReaderWriterBGR888 implements ImageFormatReader, ImageFormatWriter {

	@Override
	public BufferedImage read(int width, int height, ByteBuffer buffer) {
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int b = buffer.get() & 0xFF;
				int g = buffer.get() & 0xFF;
				int r = buffer.get() & 0xFF;
				result.setRGB(x, y, ImageUtil.encodeRGB888(r, g, b));
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
				int rgb = image.getRGB(x, y);
				int b = rgb & 0xFF;
				int g = rgb >> 8 & 0xFF;
				int r = rgb >> 16 & 0xFF;
				buffer.put((byte) b);
				buffer.put((byte) g);
				buffer.put((byte) r);
			}
		}

		return bytes;
	}
}
