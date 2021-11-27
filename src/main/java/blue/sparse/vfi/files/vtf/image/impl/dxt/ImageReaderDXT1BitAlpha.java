package blue.sparse.vfi.files.vtf.image.impl.dxt;

import blue.sparse.vfi.files.vtf.image.ImageFormatReader;
import blue.sparse.vfi.files.vtf.image.ImageUtil;
import org.joml.Vector4f;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ImageReaderDXT1BitAlpha implements ImageFormatReader {

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
				var color0int = (int) buffer.getShort() & 0xFFFF;
				var color1int = (int) buffer.getShort() & 0xFFFF;
				var color0 = ImageUtil.decodeRGB565(color0int);
				var color1 = ImageUtil.decodeRGB565(color1int);
				var codes = buffer.getInt();

				var array = ImageWriterDXT1BitAlpha.interpolateColors(
						new Vector4f(color0.x, color0.y, color0.z, 1f),
						new Vector4f(color1.x, color1.y, color1.z, 1f)
				);

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

						bufferedImage.setRGB(imageX, imageY, ImageUtil.encodeARGB8888(color));
					}
				}
			}
		}

		buffer.order(originalOrder);
		return bufferedImage;
	}

}
