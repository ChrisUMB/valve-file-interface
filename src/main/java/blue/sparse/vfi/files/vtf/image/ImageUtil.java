package blue.sparse.vfi.files.vtf.image;

import org.joml.Vector3f;

public final class ImageUtil {

	public static final int BITS_5 = ((1 << 5) - 1);
	public static final int BITS_6 = ((1 << 6) - 1);

	public static final int BITS_8 = ((1 << 8) - 1);

	private ImageUtil() {
	}

	public static short encodeRGB565(int r, int g, int b) {
		return (short) ((r << 5 | g) << 6 | b);
	}

	public static short encodeRGB565(Vector3f v) {
		int r = Math.round(v.x * BITS_5);
		int g = Math.round(v.y * BITS_6);
		int b = Math.round(v.z * BITS_5);
		return encodeRGB565(r, g, b);
	}

	public static Vector3f decodeRGB565(int rgb) {

		var red = (rgb >> 11 & BITS_5) / (float) BITS_5;
		var green = (rgb >> 5 & BITS_6) / (float) BITS_6;
		var blue = (rgb & BITS_5) / (float) BITS_5;

		return new Vector3f(red, green, blue);
	}

	public static int encodeRGB888(int r, int g, int b) {
		return ((r << 8 | g) << 8 | b);
	}

	public static int encodeRGB888(Vector3f v) {
		int r = Math.round(v.x * BITS_8);
		int g = Math.round(v.y * BITS_8);
		int b = Math.round(v.z * BITS_8);
		return encodeRGB888(r, g, b);
	}


}
