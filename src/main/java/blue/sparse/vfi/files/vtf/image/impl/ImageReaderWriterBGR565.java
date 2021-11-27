package blue.sparse.vfi.files.vtf.image.impl;

import blue.sparse.vfi.files.vtf.image.ImageFormatReader;
import blue.sparse.vfi.files.vtf.image.ImageFormatWriter;
import blue.sparse.vfi.files.vtf.image.ImageUtil;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public final class ImageReaderWriterBGR565 implements ImageFormatReader, ImageFormatWriter {

	@Override
	public BufferedImage read(int width, int height, ByteBuffer buffer) {
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				result.setRGB(x, y, ImageUtil.encodeRGB888(ImageUtil.decodeBGR565(buffer.getShort())));
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
				int b = rgb & 0xFF;
				int g = rgb >> 8 & 0xFF;
				int r = rgb >> 16 & 0xFF;

				buffer.putShort(ImageUtil.encodeRGB565(r, g, b));
			}
		}

		return bytes;
	}
}
