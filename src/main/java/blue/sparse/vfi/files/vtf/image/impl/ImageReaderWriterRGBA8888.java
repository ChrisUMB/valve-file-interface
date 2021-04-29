package blue.sparse.vfi.files.vtf.image.impl;

import blue.sparse.vfi.files.vtf.image.ImageFormatReader;
import blue.sparse.vfi.files.vtf.image.ImageFormatWriter;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public final class ImageReaderWriterRGBA8888 implements ImageFormatReader, ImageFormatWriter {

	@Override
	public BufferedImage read(int width, int height, ByteBuffer buffer) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int rgba = buffer.getInt();
				int argb = rgba >> 8 | ((rgba & 0xFF) << 24);
				image.setRGB(x, y, argb);
			}
		}

		return image;
	}

	@Override
	public byte[] write(BufferedImage image) {
		byte[] bytes = new byte[(image.getWidth() * image.getHeight()) * 4];
		ByteBuffer buffer = ByteBuffer.wrap(bytes);

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {

				int argb = image.getRGB(x, y);
				int rgba = argb << 8 | argb >> 24 & 0xFF;
				buffer.putInt(rgba);

			}
		}

		return bytes;
	}
}
