package blue.sparse.vfi.files.vtf.image.impl.dxt;

import blue.sparse.vfi.files.vtf.image.ImageFormatWriter;
import blue.sparse.vfi.files.vtf.image.ImageUtil;
import org.joml.Vector3f;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class ImageWriterDXT1 implements ImageFormatWriter {

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

        byte[] resultBytes = new byte[widthInBlocks * heightInBlocks * 8];
        ByteBuffer buffer = ByteBuffer.wrap(resultBytes);

        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int by = 0; by < heightInBlocks; by++) {
            int rby = by * 4;
            for (int bx = 0; bx < widthInBlocks; bx++) {
                int rbx = bx * 4;

                Vector3f[] originalColors = new Vector3f[16];

                for (int x = 0; x < 4; x++) {
                    int rx = Math.min(width - 1, rbx + x);
                    for (int y = 0; y < 4; y++) {
                        int ry = Math.min(height - 1, rby + y);

                        int index = y * 4 + x;

                        originalColors[index] = ImageUtil.decodeRGB888(image.getRGB(rx, ry));
                    }
                }

                BlockData best = evaluate(originalColors);

                buffer.putShort(ImageUtil.encodeRGB565(best.min));
                buffer.putShort(ImageUtil.encodeRGB565(best.max));
                buffer.putInt(best.code);
            }
        }

        System.out.println("SCORES = " + Arrays.toString(SCORES));
        return resultBytes;
    }

    public static BlockData evaluate(Vector3f[] originalColors) {
        return best(
                run(originalColors, ImageWriterDXT1::getMostDifferentAverageColors),
                run(originalColors, ImageWriterDXT1::getTwoMostDifferentColors),
                run(originalColors, ImageWriterDXT1::getMinMaxColors),
                run(originalColors, ImageWriterDXT1::getMostCommonUniqueColors)
        );
    }

    public static BlockData evaluate2(Vector3f[] originalColors) {
        return run(originalColors, ImageWriterDXT1::getMinMaxColors);
    }

    private static BlockData run(Vector3f[] originalColors, Algorithm algorithm) {
        Vector3f min = new Vector3f(), max = new Vector3f();
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

    private static BlockData findBestOfSwapped(Vector3f[] originalColors, Vector3f min, Vector3f max) {
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
        Vector3f[] colorOptions1 = interpolateColors(min, max);
        Vector3f[] colorOptions2 = interpolateColors(max, min);
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

    public static record BlockData(Vector3f min, Vector3f max, int code, float distance) {

    }

    public static int getClosestColor(float[] distanceTarget, Vector3f[] options, Vector3f color) {
        float distance = Float.POSITIVE_INFINITY;
        int index = -1;

        for (int i = 0; i < options.length; i++) {
            float currentDistance = options[i].distance(color);
            if (currentDistance < distance) {
                distance = currentDistance;
                index = i;
            }
        }

        distanceTarget[0] += distance;
        return index;
    }

    private static void getMostCommonUniqueColors(Vector3f[] originalColors, Vector3f min, Vector3f max) {
        int[] counts = new int[originalColors.length];

        for (int i = 0; i < originalColors.length; i++) {
            if (counts[i] == -1)
                continue;

            Vector3f color = originalColors[i];
            for (int j = 0; j < originalColors.length; j++) {
                Vector3f otherColor = originalColors[j];

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

    private static void getTwoMostDifferentColors(Vector3f[] originalColors, Vector3f min, Vector3f max) {
        float distance = Float.NEGATIVE_INFINITY;

        for (int i = 0, originalColorsLength = originalColors.length; i < originalColorsLength; i++) {
            Vector3f color1 = originalColors[i];
            for (int j = i + 1, colorsLength = originalColors.length; j < colorsLength; j++) {
                Vector3f color2 = originalColors[j];
                float currentDistance = color1.distanceSquared(color2);
                if (currentDistance > distance) {
                    distance = currentDistance;
                    min.set(color1);
                    max.set(color2);
                }
            }
        }
    }

    private static void getMinMaxColors(Vector3f[] originalColors, Vector3f min, Vector3f max) {
        float minLength = Float.POSITIVE_INFINITY;
        float maxLength = Float.NEGATIVE_INFINITY;

        for (Vector3f color : originalColors) {
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

    private static void getMostDifferentAverageColors(Vector3f[] originalColors, Vector3f min, Vector3f max) {
        Vector3f average = new Vector3f();

        for (Vector3f color : originalColors) {
            average.add(color);
        }

        average.div(originalColors.length);

        float distance = Float.NEGATIVE_INFINITY;
        for (Vector3f color : originalColors) {
            float currentDistance = color.distanceSquared(average);
            if (currentDistance > distance) {
                distance = currentDistance;
                max.set(color);
            }
        }

        distance = Float.NEGATIVE_INFINITY;
        for (Vector3f color : originalColors) {
            float currentDistance = color.distanceSquared(max);
            if (currentDistance > distance) {
                distance = currentDistance;
                min.set(color);
            }
        }
    }

    public static Vector3f[] interpolateColorsPreEncoded(Vector3f[] target, int color0int, Vector3f color0, int color1int, Vector3f color1) {
        target[0].set(color0);
        target[1].set(color1);

        if(color0int > color1int) {
//            target[3].set(color0).add(new Vector3f(color1).mul(2f)).div(3f);
//            target[2].set(color0).mul(2f).add(color1).div(3f);
            target[2].set(
                    (color0.x * 2f + color1.x) / 3f,
                    (color0.y * 2f + color1.y) / 3f,
                    (color0.z * 2f + color1.z) / 3f
            );
            target[3].set(
                    (color0.x + color1.x * 2f) / 3f,
                    (color0.y + color1.y * 2f) / 3f,
                    (color0.z + color1.z * 2f) / 3f
            );
        } else {
//            target[2].set(color0).add(color1).div(2f);
            target[2].set(
                    (color0.x + color1.x) / 2f,
                    (color0.y + color1.y) / 2f,
                    (color0.z + color1.z) / 2f
            );
            target[3].set(0f);
        }

        return target;
    }

    public static Vector3f[] interpolateColors(Vector3f[] target, Vector3f color0, Vector3f color1) {
        var color0int = (int) ImageUtil.encodeRGB565(color0) & 0xFFFF;
        var color1int = (int) ImageUtil.encodeRGB565(color1) & 0xFFFF;
        return interpolateColorsPreEncoded(target, color0int, color0, color1int, color1);
    }

    public static Vector3f[] interpolateColors(Vector3f color0, Vector3f color1) {
        Vector3f[] target = new Vector3f[4];
        for (int i = 0; i < target.length; i++) {
            target[i] = new Vector3f();
        }

        return interpolateColors(target, color0, color1);
    }

    private static void rgb565ify(Vector3f vector1, Vector3f vector2) {
        vector1.set(ImageUtil.decodeRGB565(ImageUtil.encodeRGB565(vector1)));
        vector2.set(ImageUtil.decodeRGB565(ImageUtil.encodeRGB565(vector2)));

        if (vector1.equals(vector2)) {
            vector2.add(1f / ImageUtil.BITS_5, -1f / ImageUtil.BITS_6, 1f / ImageUtil.BITS_5);
            vector2.x = Math.min(Math.max(vector2.x, 0f), 1f);
            vector2.y = Math.min(Math.max(vector2.y, 0f), 1f);
            vector2.z = Math.min(Math.max(vector2.z, 0f), 1f);
        }
    }

    private interface Algorithm {
        void getMinMax(Vector3f[] originalColors, Vector3f min, Vector3f max);
    }
}
