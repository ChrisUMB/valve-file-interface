package blue.sparse.vfi.files.vtf.image.impl;

import blue.sparse.vfi.files.vtf.image.ImageFormatReader;
import blue.sparse.vfi.files.vtf.image.ImageUtil;
import org.joml.Vector3f;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ImageReaderDXT1 implements ImageFormatReader {

	@Override
	public BufferedImage read(int width, int height, ByteBuffer buffer) {
		var originalOrder = buffer.order();
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		var widthInBlocks = width / 4;
		var heightInBlocks = height / 4;

		if (width % 4 != 0) {
			widthInBlocks++;
		}

		if (height % 4 != 0) {
			heightInBlocks++;
		}

		var bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		for (int blockY = 0; blockY < heightInBlocks; blockY++) {
			for (int blockX = 0; blockX < widthInBlocks; blockX++) {

				var color0int = (int) buffer.getShort() & 0xFFFF;
				var color1int = (int) buffer.getShort() & 0xFFFF;
				var color0 = ImageUtil.decodeRGB565(color0int);
				var color1 = ImageUtil.decodeRGB565(color1int);
				var codes = buffer.getInt();

				var array = interpolateColors(color0, color1);

				for (int y = 0; y <= 3; y++) {
					for (int x = 0; x <= 3; x++) {
						var imageX = x + blockX * 4;
						var imageY = y + blockY * 4;

						if (imageX >= width || imageY >= height) {
							continue;
						}

						var index = x + y * 4;
						var code = (codes >> (index * 2)) & 0b11;
						var color = array[code];

						bufferedImage.setRGB(imageX, imageY, ImageUtil.encodeRGB888(color));
					}
				}
			}
		}

		buffer.order(originalOrder);
		return bufferedImage;
	}

	private Vector3f[] interpolateColors(Vector3f color0, Vector3f color1) {
		var color0int = (int) ImageUtil.encodeRGB565(color0) & 0xFFFF;
		var color1int = (int) ImageUtil.encodeRGB565(color1) & 0xFFFF;

		var array = new Vector3f[4];
		for (int i = 0; i < array.length; i++) {
			if (color0int > color1int) {
				if (i == 0) {
					array[i] = color0;
				} else if (i == 1) {
					array[i] = color1;
				} else if (i == 2) {
					array[i] = new Vector3f(color0).mul(2f).add(color1).div(3f);
				} else {
					array[i] = new Vector3f(color0).add(new Vector3f(color1).mul(2f)).div(3f);
				}
			} else {
				if (i == 0) {
					array[i] = color0;
				} else if (i == 1) {
					array[i] = color1;
				} else if (i == 2) {
					array[i] = new Vector3f(color0).add(color1).div(2f);
				} else {
					array[i] = new Vector3f(0f);
				}
			}
		}
		return array;
	}

}
