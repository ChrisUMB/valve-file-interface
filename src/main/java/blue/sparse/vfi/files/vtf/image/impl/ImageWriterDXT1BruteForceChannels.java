package blue.sparse.vfi.files.vtf.image.impl;

import blue.sparse.vfi.files.vtf.image.ImageFormatWriter;
import blue.sparse.vfi.files.vtf.image.ImageUtil;
import org.joml.Vector3f;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

// Doesn't work well at all.
@Deprecated
public final class ImageWriterDXT1BruteForceChannels implements ImageFormatWriter {

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

        byte[] resultBytes = new byte[widthInBlocks * heightInBlocks * 8];
        ByteBuffer buffer = ByteBuffer.wrap(resultBytes);

        float[] originalRed = new float[16];
        float[] originalGreen = new float[16];
        float[] originalBlue = new float[16];
        Vector3f[] originalColors = new Vector3f[16];

        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int by = 0; by < heightInBlocks; by++) {
            System.out.printf("%,d/%,d%n", by, heightInBlocks);
            int rby = by * 4;
            for (int bx = 0; bx < widthInBlocks; bx++) {
                int rbx = bx * 4;

                getOriginalColors(image, width, height, rby, rbx, originalRed, originalGreen, originalBlue);

                for (int i = 0; i < 16; i++) {
                    originalColors[i] = new Vector3f(originalRed[i], originalGreen[i], originalBlue[i]);
                }

                int[] bestRed = findBestColorsForChannel(originalRed, ImageUtil.BITS_5, new int[4]);
                int[] bestGreen = findBestColorsForChannel(originalGreen, ImageUtil.BITS_6, new int[4]);
                int[] bestBlue = findBestColorsForChannel(originalBlue, ImageUtil.BITS_5, new int[4]);
                System.out.println(Arrays.toString(bestRed));
                System.out.println(Arrays.toString(bestGreen));
                System.out.println(Arrays.toString(bestBlue));
                System.out.println("--");

                short[] bestPositive = new short[2];
                encodeToPositive(
                        bestRed[0], bestGreen[0], bestBlue[0],
                        bestRed[1], bestGreen[1], bestBlue[1],
                        bestPositive
                );

                short[] bestNegative = new short[2];
                encodeToNegative(
                        bestRed[2], bestGreen[2], bestBlue[2],
                        bestRed[3], bestGreen[3], bestBlue[3],
                        bestNegative
                );

                BlockData best = getBest(bestPositive, bestNegative, originalColors);
                buffer.putShort(best.min);
                buffer.putShort(best.max);
                buffer.putInt(best.code);
            }
        }

        return resultBytes;
    }

    private static BlockData getBest(short[] baseA, short[] baseB, Vector3f[] originalColors) {
        BlockData a = compute(baseA[0], baseA[1], originalColors);
        BlockData b = compute(baseB[0], baseB[1], originalColors);

        return a.distance < b.distance ? a : b;
    }

    private static BlockData compute(short min, short max, Vector3f[] originalColors) {
        Vector3f[] options = ImageWriterDXT1.interpolateColors(
                ImageUtil.decodeRGB565(min & 0xFFFF),
                ImageUtil.decodeRGB565(max & 0xFFFF)
        );

        float[] distance = new float[1];
        int code = 0;
        for (int i = 0; i < originalColors.length; i++) {
            code |= ImageWriterDXT1.getClosestColor(distance, options, originalColors[i]) << (i * 2);
        }

        return new BlockData(min, max, code, distance[0]);
    }

    private static record BlockData(short min, short max, int code, float distance) {
    }

    private static void encodeToPositive(
            int red0, int green0, int blue0,
            int red1, int green1, int blue1,
            short[] target
    ) {
        int temp;
        if (red1 > red0) {
            temp = red1;
            red1 = red0;
            red0 = temp;
        }

        if (green1 > green0) {
            temp = green1;
            green1 = green0;
            green0 = temp;
        }

        if (blue1 > blue0) {
            temp = blue1;
            blue1 = blue0;
            blue0 = temp;
        }

        target[0] = ImageUtil.encodeRGB565(red0, green0, blue0);
        target[1] = ImageUtil.encodeRGB565(red1, green1, blue1);
    }

    private static void encodeToNegative(
            int red0, int green0, int blue0,
            int red1, int green1, int blue1,
            short[] target
    ) {
//        int temp;
//        if (red1 < red0) {
//            temp = red1;
//            red1 = red0;
//            red0 = temp;
//        }
//
//        if (green1 < green0) {
//            temp = green1;
//            green1 = green0;
//            green0 = temp;
//        }
//
//        if (blue1 < blue0) {
//            temp = blue1;
//            blue1 = blue0;
//            blue0 = temp;
//        }

        target[0] = ImageUtil.encodeRGB565(red0, green0, blue0);
        target[1] = ImageUtil.encodeRGB565(red1, green1, blue1);
    }

    private static int[] findBestColorsForChannel(
            float[] original,
            int max,
            int[] target
    ) {
        float[] interpolateTarget = new float[4];
        float positiveDistance = Float.POSITIVE_INFINITY;
        float negativeDistance = Float.POSITIVE_INFINITY;

        for (int a = 0; a < max; a++) {
            for (int b = 0; b < max; b++) {
                float fa = roundTo(a / (float) max, max);
                float fb = roundTo(b / (float) max, max);

                float distance0 = getChannelDistance(fa, fb, true, original, interpolateTarget);
                float distance1 = getChannelDistance(fa, fb, false, original, interpolateTarget);

                if (distance0 < positiveDistance) {
                    positiveDistance = distance0;
                    target[0] = a;
                    target[1] = b;
                }

                if (distance1 < negativeDistance) {
                    negativeDistance = distance1;
                    target[2] = a;
                    target[3] = b;
                }
            }
        }

        return target;
    }

    private static float getChannelDistance(
            float fa, float fb, boolean positive,
            float[] original, float[] interpolateTarget
    ) {
        interpolateChannel(fa, fb, positive, interpolateTarget);

        float distance = 0f;
        for (float o : original) {
            float iDistance = Float.POSITIVE_INFINITY;
            for (float i : interpolateTarget) {
                float cDistance = Math.abs(o - i);
                if (cDistance < iDistance) {
                    iDistance = cDistance;
                }
            }

            distance += iDistance;
        }

        return distance;
    }

    private static void getOriginalColors(
            BufferedImage image, int width, int height, int rby, int rbx,
            float[] originalRed, float[] originalGreen, float[] originalBlue
    ) {
        for (int x = 0; x < 4; x++) {
            int rx = Math.min(width - 1, rbx + x);
            for (int y = 0; y < 4; y++) {
                int ry = Math.min(height - 1, rby + y);

                int index = y * 4 + x;

                Vector3f color = ImageUtil.decodeRGB888(image.getRGB(rx, ry));
                originalRed[index] = color.x;
                originalGreen[index] = color.y;
                originalBlue[index] = color.z;
            }
        }
    }

    private static float roundTo(float value, int max) {
        return Math.round(value * max) / (float) max;
    }

    private static void interpolateChannelAll(float channel0, float channel1, float[] target) {
        target[0] = channel0;
        target[1] = channel1;
        target[2] = Math.fma(channel0, 2f, channel1) / 3f;
        target[3] = Math.fma(channel1, 2f, channel0) / 3f;
        target[4] = channel0;
        target[5] = channel1;
        target[6] = (channel0 + channel1) / 2f;
        target[7] = 0f;
    }

    private static void interpolateChannel(float channel0, float channel1, boolean positive, float[] target) {
        target[0] = channel0;
        target[1] = channel1;
        if (positive) {
            target[2] = Math.fma(channel0, 2f, channel1) / 3f;
            target[3] = Math.fma(channel1, 2f, channel0) / 3f;
        } else {
            target[2] = (channel0 + channel1) / 2f;
            target[3] = 0f;
        }
    }
}
