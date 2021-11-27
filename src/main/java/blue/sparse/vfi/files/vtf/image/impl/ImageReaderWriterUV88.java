package blue.sparse.vfi.files.vtf.image.impl;

import blue.sparse.vfi.files.vtf.image.ImageFormatReader;
import blue.sparse.vfi.files.vtf.image.ImageFormatWriter;
import blue.sparse.vfi.files.vtf.image.ImageUtil;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public final class ImageReaderWriterUV88 implements ImageFormatReader, ImageFormatWriter {

	@Override
	public BufferedImage read(int width, int height, ByteBuffer buffer) {
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int u = buffer.get() & 0xFF;
				int v = buffer.get() & 0xFF;
				result.setRGB(x, y, ImageUtil.encodeRGB888(u, v, 0));
			}
		}

		return result;
	}

	@Override
	public byte[] write(BufferedImage image) {
		byte[] bytes = new byte[(image.getWidth() * image.getHeight()) * 2];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int rgb = image.getRGB(x, y);
				int u = rgb & 0xFF;
				int v = rgb >> 8 & 0xFF;
				buffer.put((byte) u);
				buffer.put((byte) v);
			}
		}

		return bytes;
	}
}
