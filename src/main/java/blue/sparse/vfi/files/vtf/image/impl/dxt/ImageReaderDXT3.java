package blue.sparse.vfi.files.vtf.image.impl.dxt;

import blue.sparse.vfi.files.vtf.image.ImageFormatReader;
import blue.sparse.vfi.files.vtf.image.ImageUtil;
import org.joml.Vector3f;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ImageReaderDXT3 implements ImageFormatReader {

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

		var bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		for (int blockY = 0; blockY < heightInBlocks; blockY++) {
			for (int blockX = 0; blockX < widthInBlocks; blockX++) {
				var alphaLong = buffer.getLong();
				var color0int = (int) buffer.getShort() & 0xFFFF;
				var color1int = (int) buffer.getShort() & 0xFFFF;
				var color0 = ImageUtil.decodeRGB565(color0int);
				var color1 = ImageUtil.decodeRGB565(color1int);
				var codes = buffer.getInt();

				var array = new Vector3f[4];
				for (int i = 0; i < array.length; i++) {
					if (color0int > color1int) {
						switch (i) {
							case 0 -> array[i] = color0;
							case 1 -> array[i] = color1;
							case 2 -> array[i] = (new Vector3f(color0).mul(2f).add(color1)).div(3f);
							case 3 -> array[i] = (new Vector3f(color0).add(new Vector3f(color1).mul(2f))).div(3f);
						}
					} else {
						switch (i) {
							case 0 -> array[i] = color0;
							case 1 -> array[i] = color1;
							case 2 -> array[i] = new Vector3f(color0).add(color1).div(2f);
							case 3 -> array[i] = new Vector3f(0f);
						}
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
//						var code = Bits.getBits32(codes, index * 2, 2);
						var code = codes >> (index * 2) & 0b11;
//						long bits64 = Bits.getBits64(alphaLong, index * 4, 4);
						long bits64 = alphaLong >> (index * 4) & 0b1111;
						var alpha = bits64 / ((float) (1 << 4) - 1);
						var color = array[code];
						bufferedImage.setRGB(imageX, imageY, new Color(color.x, color.y, color.z, alpha).getRGB());
					}
				}
			}
		}

		buffer.order(originalOrder);
		return bufferedImage;
	}
}
