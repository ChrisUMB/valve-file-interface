package blue.sparse.vfi.files.vtf.image.impl.dxt;

import blue.sparse.vfi.files.vtf.image.ImageFormatWriter;
import blue.sparse.vfi.files.vtf.image.ImageUtil;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ImageWriterDXT1BitAlpha implements ImageFormatWriter {

	private static final int[] SCORES = new int[4];

	@Override
	public byte[] write(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();

		var widthInBlocks = width / 4;
		var heightInBlocks = height / 4;

		if (width % 4 != 0) {
			widthInBlocks++;
		}

		if (height % 4 != 0) {
			heightInBlocks++;
		}

		byte[] resultBytes = new byte[widthInBlocks * heightInBlocks * 12];
		ByteBuffer buffer = ByteBuffer.wrap(resultBytes);

		buffer.order(ByteOrder.LITTLE_ENDIAN);
		for (int by = 0; by < heightInBlocks; by++) {
			int rby = by * 4;
			for (int bx = 0; bx < widthInBlocks; bx++) {
				int rbx = bx * 4;

				Vector4f[] originalColors = new Vector4f[16];

				for (int x = 0; x < 4; x++) {
					int rx = Math.min(width - 1, rbx + x);
					for (int y = 0; y < 4; y++) {
						int ry = Math.min(height - 1, rby + y);

						int index = y * 4 + x;

						Vector4f color = ImageUtil.decodeARGB8888(image.getRGB(rx, ry));
						if (color.w == 0f) {
							color.set(0f);
						}

						originalColors[index] = color;
					}
				}

				BlockData best = best(
						run(originalColors, ImageWriterDXT1BitAlpha::getMostDifferentAverageColors),
						run(originalColors, ImageWriterDXT1BitAlpha::getTwoMostDifferentColors),
						run(originalColors, ImageWriterDXT1BitAlpha::getMinMaxColors),
						run(originalColors, ImageWriterDXT1BitAlpha::getMostCommonUniqueColors)
				);

				buffer.putShort(ImageUtil.encodeRGB565(best.min));
				buffer.putShort(ImageUtil.encodeRGB565(best.max));
//				buffer.putint((short) 0xFFFF);
				buffer.putInt(best.code);
			}
		}

		return resultBytes;
	}

	private static BlockData run(Vector4f[] originalColors, Algorithm algorithm) {
		Vector4f min = new Vector4f(), max = new Vector4f();
		algorithm.getMinMax(originalColors, min, max);
		rgb565ify(min, max);
		return findBestOfSwapped(originalColors, min, max);
	}

	private static BlockData best(BlockData... blockData) {
		BlockData best = null;
		int bestIndex = -1;

		for (int i = 0, blockDataLength = blockData.length; i < blockDataLength; i++) {
			BlockData datum = blockData[i];
			if (best == null || datum.distance < best.distance) {
				best = datum;
				bestIndex = i;
			}
		}

		SCORES[bestIndex]++;
		return best;
	}

	private static BlockData findBestOfSwapped(Vector4f[] originalColors, Vector4f min, Vector4f max) {
//        if(min.distanceSquared(max) <= 0.05 * 0.05) {
//            Vector3f[] colorOptions = interpolateColors(max, min);
//            int code = 0;
//            float distance = 0f;
//
//            ThreadLocalRandom random = ThreadLocalRandom.current();
//            for (int i = 0; i < originalColors.length; i++) {
//                int selected = random.nextInt(colorOptions.length);
//                distance += colorOptions[selected].distance(originalColors[i]);
//
//                code |= selected << (i * 2);
//            }
//
//            return new BlockData(max, min, code, distance);
//        } else {
		Vector4f[] colorOptions1 = interpolateColors(min, max);
		Vector4f[] colorOptions2 = interpolateColors(max, min);
		int code1 = 0;
		int code2 = 0;

		float[] distance1 = new float[1];
		float[] distance2 = new float[1];
		for (int i = 0; i < originalColors.length; i++) {
			code1 |= getClosestColor(distance1, colorOptions1, originalColors[i]) << (i * 2);
			code2 |= getClosestColor(distance2, colorOptions2, originalColors[i]) << (i * 2);
		}

		if (distance1[0] < distance2[0]) {
			return new BlockData(min, max, code1, distance1[0]);
		} else {
			return new BlockData(max, min, code2, distance2[0]);
		}
//        }
	}

	private static record BlockData(Vector4f min, Vector4f max, int code, float distance) {

	}

	private static int getClosestColor(float[] distanceTarget, Vector4f[] options, Vector4f color) {
		float distance = Float.POSITIVE_INFINITY;
		int index = -1;

		for (int i = 0; i < options.length; i++) {
			float currentDistance = options[i].distanceSquared(color);
			if (currentDistance < distance) {
				distance = currentDistance;
				index = i;
			}
		}

		distanceTarget[0] += distance;
		return index;
	}

	private static void getMostCommonUniqueColors(Vector4f[] originalColors, Vector4f min, Vector4f max) {
		int[] counts = new int[originalColors.length];

		for (int i = 0; i < originalColors.length; i++) {
			if (counts[i] == -1)
				continue;

			Vector4f color = originalColors[i];
			for (int j = 0; j < originalColors.length; j++) {
				Vector4f otherColor = originalColors[j];

				if (color.equals(otherColor)) {
					counts[i]++;
					counts[j] = -1;
				}
			}
		}

		int bestIndex1 = 0;
		int bestIndex2 = 1;
		for (int i = 0; i < counts.length; i++) {
			if (counts[i] >= counts[bestIndex1]) {
				bestIndex2 = bestIndex1;
				bestIndex1 = i;
			}
		}

		min.set(originalColors[bestIndex1]);
		max.set(originalColors[bestIndex2]);
	}

	private static void getTwoMostDifferentColors(Vector4f[] originalColors, Vector4f min, Vector4f max) {
		float distance = Float.NEGATIVE_INFINITY;

		for (int i = 0, originalColorsLength = originalColors.length; i < originalColorsLength; i++) {
			Vector4f color1 = originalColors[i];
			for (int j = i + 1, colorsLength = originalColors.length; j < colorsLength; j++) {
				Vector4f color2 = originalColors[j];
				float currentDistance = color1.distanceSquared(color2);
				if (currentDistance > distance) {
					distance = currentDistance;
					min.set(color1);
					max.set(color2);
				}
			}
		}
	}

	private static void getMinMaxColors(Vector4f[] originalColors, Vector4f min, Vector4f max) {
		float minLength = Float.POSITIVE_INFINITY;
		float maxLength = Float.NEGATIVE_INFINITY;

		for (Vector4f color : originalColors) {
			float length = color.lengthSquared();
			if (length < minLength) {
				min.set(color);
				minLength = length;
			}

			if (length > maxLength) {
				max.set(color);
				maxLength = length;
			}
		}
	}

	private static void getMostDifferentAverageColors(Vector4f[] originalColors, Vector4f min, Vector4f max) {
		Vector4f average = new Vector4f();

		for (Vector4f color : originalColors) {
			average.add(color);
		}

		average.div(originalColors.length);

		float distance = Float.NEGATIVE_INFINITY;
		for (Vector4f color : originalColors) {
			float currentDistance = color.distanceSquared(average);
			if (currentDistance > distance) {
				distance = currentDistance;
				max.set(color);
			}
		}

		distance = Float.NEGATIVE_INFINITY;
		for (Vector4f color : originalColors) {
			float currentDistance = color.distanceSquared(max);
			if (currentDistance > distance) {
				distance = currentDistance;
				min.set(color);
			}
		}
	}

	public static Vector4f[] interpolateColors(Vector4f color0, Vector4f color1) {
		var color0int = (int) ImageUtil.encodeRGB565(new Vector3f(color0.x, color0.y, color0.z)) & 0xFFFF;
		var color1int = (int) ImageUtil.encodeRGB565(new Vector3f(color1.x, color1.y, color1.z)) & 0xFFFF;

		var array = new Vector4f[4];
		for (int i = 0; i < array.length; i++) {
			if (color0int > color1int) {
				switch (i) {
					case 0 -> array[i] = new Vector4f(color0);
					case 1 -> array[i] = new Vector4f(color1);
					case 2 -> array[i] = new Vector4f(color0).mul(2f).add(color1).div(3f);
					case 3 -> array[i] = new Vector4f(color0).add(new Vector4f(color1).mul(2f)).div(3f);
				}

				array[i].w = 1f;
			} else {
				switch (i) {
					case 0 -> array[i] = new Vector4f(color0);
					case 1 -> array[i] = new Vector4f(color1);
					case 2 -> array[i] = new Vector4f(color0).add(color1).div(2f);
					case 3 -> array[i] = new Vector4f(color0.x, color0.y, color0.z, 0f);
				}

				if (i != 3) {
					array[i].w = 1f;
				}
			}
		}

		return array;
	}

	private static void rgb565ify(Vector4f vector1, Vector4f vector2) {
		Vector3f vector3f = ImageUtil.decodeRGB565(ImageUtil.encodeRGB565(new Vector3f(vector1.x, vector1.y, vector1.z)));
		vector1.set(vector3f.x, vector3f.y, vector3f.z, vector1.w);

		Vector3f vector3f1 = ImageUtil.decodeRGB565(ImageUtil.encodeRGB565(new Vector3f(vector2.x, vector2.y, vector2.z)));
		vector2.set(vector3f1.x, vector3f1.y, vector3f1.z, vector2.w);

		if (vector1.equals(vector2)) {
			vector2.add(1f / ImageUtil.BITS_5, -1f / ImageUtil.BITS_6, 1f / ImageUtil.BITS_5, 0f);
			vector2.x = Math.min(Math.max(vector2.x, 0f), 1f);
			vector2.y = Math.min(Math.max(vector2.y, 0f), 1f);
			vector2.z = Math.min(Math.max(vector2.z, 0f), 1f);
		}
	}

	private interface Algorithm {
		void getMinMax(Vector4f[] originalColors, Vector4f min, Vector4f max);
	}
}
