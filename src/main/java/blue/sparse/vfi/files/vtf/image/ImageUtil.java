package blue.sparse.vfi.files.vtf.image;

import blue.sparse.vfi.files.vtf.VTFMipmap;
import org.joml.Vector3f;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public final class ImageUtil {

    public static final int BITS_5 = ((1 << 5) - 1); // 0x1F
    public static final int BITS_6 = ((1 << 6) - 1); // 0x3F
    public static final int BITS_7 = ((1 << 7) - 1); // 0x7F
    public static final int BITS_8 = ((1 << 8) - 1); // 0xFF

    private ImageUtil() {
    }

    public static short encodeRGB565(int r, int g, int b) {
        return (short) ((r << 6 | g) << 5 | b);
    }

//	public static short encodeRGB565(Vector3f v) {
//		float fr = v.x * BITS_5;
//		float fg = v.y * BITS_6;
//		float fb = v.z * BITS_5;
//		int r = (int) fr;
//		if(Math.random() < fr - r) r++;
//
//		int g = (int) fg;
//		if(Math.random() < fg - g) g++;
//
//		int b = (int) fb;
//		if(Math.random() < fb - b) b++;
//
//		return encodeRGB565(r, g, b);
//	}

    public static short encodeRGB565(Vector3f v) {
        int r = Math.round(v.x * BITS_5);
        int g = Math.round(v.y * BITS_6);
        int b = Math.round(v.z * BITS_5);
        return encodeRGB565(r, g, b);
    }

//    public static short encodeRGB565(Vector3f v) {
//        int r = Math.round(v.x * BITS_8);
//        int g = Math.round(v.y * BITS_8);
//        int b = Math.round(v.z * BITS_8);
//        return encodeRGB565(r >> 3, g >> 2, b >> 3);
//    }

    public static Vector3f decodeRGB565(int rgb) {
        var r = (rgb >> 11 & BITS_5) / (float) BITS_5;
        var g = (rgb >> 5 & BITS_6) / (float) BITS_6;
        var b = (rgb & BITS_5) / (float) BITS_5;

        return new Vector3f(r, g, b);
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

	public static BufferedImage scaleImage(BufferedImage image, int newWidth, int newHeight) {
		BufferedImage bufferedImage = new BufferedImage(newWidth, newHeight, image.getType());
		Graphics2D graphics = bufferedImage.createGraphics();
		graphics.drawImage(image, 0, 0, newWidth, newHeight, null);
		graphics.dispose();
		return bufferedImage;
	}

	public static BufferedImage createThumbnail(BufferedImage image) {
		int w = image.getWidth();
		int h = image.getHeight();

		float ar = (float) w / (float) h;
		if (ar > 1.0f) {
			w = 16;
			h = (int) (16 / ar);
		} else {
			h = 16;
			w = (int) (16 * ar);
		}

		return scaleImage(image, w, h);
	}
    public static Vector3f decodeRGB888(int rgb) {
        var r = (rgb >> 16 & BITS_8) / (float) BITS_8;
        var g = (rgb >> 8 & BITS_8) / (float) BITS_8;
        var b = (rgb & BITS_8) / (float) BITS_8;

        return new Vector3f(r, g, b);
    }

}
