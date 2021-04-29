package blue.sparse.vfi.files.vtf.image.impl;

import blue.sparse.vfi.files.vtf.image.ImageFormatReader;
import blue.sparse.vfi.files.vtf.image.ImageUtil;
import org.joml.Vector3f;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ImageReaderDXT5 implements ImageFormatReader {

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
				var alphaLong = buffer.getLong();
				var color0int = (int) buffer.getShort() & 0xFFFF;
				var color1int = (int) buffer.getShort() & 0xFFFF;
				var colorCodes = buffer.getInt();

				var color0 = ImageUtil.decodeRGB565(color0int);
				var color1 = ImageUtil.decodeRGB565(color1int);

				var alpha0 = (alphaLong & 0xFF) / (float) (1 << 8);
				var alpha1 = (alphaLong >> 8 & 0xFF) / (float) (1 << 8);

				var alphaCodes = (alphaLong >> 16 & ((1L << 48) - 1));

				var alphaArray = new float[8];

				for (int i = 0; i < alphaArray.length; i++) {
					if (alpha0 > alpha1) {
						switch (i) {
							case 0 -> alphaArray[i] = alpha0;
							case 1 -> alphaArray[i] = alpha1;
							case 2 -> alphaArray[i] = (6f * alpha0 + 1f * alpha1) / 7f;
							case 3 -> alphaArray[i] = (5f * alpha0 + 2f * alpha1) / 7f;
							case 4 -> alphaArray[i] = (4f * alpha0 + 3f * alpha1) / 7f;
							case 5 -> alphaArray[i] = (3f * alpha0 + 4f * alpha1) / 7f;
							case 6 -> alphaArray[i] = (2f * alpha0 + 5f * alpha1) / 7f;
							case 7 -> alphaArray[i] = (1f * alpha0 + 6f * alpha1) / 7f;
						}
					} else {
						switch (i) {
							case 0 -> alphaArray[i] = alpha0;
							case 1 -> alphaArray[i] = alpha1;
							case 2 -> alphaArray[i] = (4f * alpha0 + 1f * alpha1) / 5f;
							case 3 -> alphaArray[i] = (3f * alpha0 + 2f * alpha1) / 5f;
							case 4 -> alphaArray[i] = (2f * alpha0 + 3f * alpha1) / 5f;
							case 5 -> alphaArray[i] = (1f * alpha0 + 4f * alpha1) / 5f;
							case 6 -> alphaArray[i] = 0.0f;
							case 7 -> alphaArray[i] = 1.0f;
						}
					}

				}

				var colorArray = new Vector3f[4];
				for (int i = 0; i < colorArray.length; i++) {
					switch (i) {
						case 0 -> colorArray[i] = color0;
						case 1 -> colorArray[i] = color1;
						case 2 -> colorArray[i] = (new Vector3f(color0).mul(2f).add(color1)).div(3f);
						case 3 -> colorArray[i] = (new Vector3f(color0).add(new Vector3f(color1).mul(2f))).div(3f);
					}
				}

				for (int y = 0; y <= 3; y++) {
					for (int x = 0; x <= 3; x++) {
						var imageX = x + blockX * 4;
						var imageY = y + blockY * 4;

						if (imageX >= width || imageY >= height) {
							continue;
						}

						var index = x + y * 4;

						var colorCode = (colorCodes >> (index * 2) & 0b11);
						var alphaCode = (int) (alphaCodes >> (index * 3) & 0b111);

						var color = colorArray[colorCode];
						var alpha = alphaArray[alphaCode];

						bufferedImage.setRGB(imageX, imageY, new Color(color.x, color.y, color.z, alpha).getRGB());
					}
				}
			}
		}

		buffer.order(originalOrder);
		return bufferedImage;
	}

}
