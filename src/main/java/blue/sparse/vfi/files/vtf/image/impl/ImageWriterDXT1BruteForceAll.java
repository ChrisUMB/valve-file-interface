package blue.sparse.vfi.files.vtf.image.impl;

import blue.sparse.pixel.PixelDisplay;
import blue.sparse.vfi.files.vtf.image.ImageFormatWriter;
import blue.sparse.vfi.files.vtf.image.ImageUtil;
import blue.sparse.vfi.files.vtf.image.impl.dxt.ImageWriterDXT1;
import org.joml.Vector3f;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class ImageWriterDXT1BruteForceAll implements ImageFormatWriter {

    private static final Map<ColorSetSignature, BlockRange> CACHE = new ConcurrentHashMap<>();
    private static final AtomicInteger CACHE_CHANGES = new AtomicInteger();
    private static final ExecutorService SERVICE = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setPriority(Thread.NORM_PRIORITY - 2);
        t.setDaemon(true);
        return t;
    });

    public static void saveCache() throws IOException {
        int currentChangeCount = CACHE_CHANGES.get();
        if (currentChangeCount == 0)
            return;

        File file = new File("dxt1_cache.bin");
        DataOutputStream out = new DataOutputStream(
                new GZIPOutputStream(
                        new BufferedOutputStream(new FileOutputStream(file))
                )
        );
        out.writeInt(CACHE.size());
        for (Map.Entry<ColorSetSignature, BlockRange> entry : CACHE.entrySet()) {
            out.write(entry.getKey().bytes);
            out.writeShort(entry.getValue().min);
            out.writeShort(entry.getValue().max);
        }
        out.close();
        CACHE_CHANGES.getAndUpdate(it -> it - currentChangeCount);
    }

    public static void loadCache() throws IOException, ClassNotFoundException {
        System.out.println("Loading DXT1 cache");
        File file = new File("dxt1_cache.bin");
        if (!file.exists())
            return;

        DataInputStream in = new DataInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(file))));
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            byte[] bytes = new byte[16 * 3];
            in.readFully(bytes);
            CACHE.put(new ColorSetSignature(bytes), new BlockRange(in.readShort(), in.readShort()));
        }
        in.close();
    }

    static {
        try {
            loadCache();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static volatile PixelDisplay display = null;

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

        int finalWidthInBlocks = widthInBlocks;

        byte[] resultBytes = new byte[widthInBlocks * heightInBlocks * 8];
        ByteBuffer buffer = ByteBuffer.wrap(resultBytes);

        AtomicInteger cacheHits = new AtomicInteger();
        AtomicInteger cacheMisses = new AtomicInteger();

        ExecutorCompletionService<Void> service = new ExecutorCompletionService<>(SERVICE);

        if (display == null || display.getWidth() != width || display.getHeight() != height) {
            if (display != null) {
                display.close();
            }
            display = new PixelDisplay("DXT1", width, height, 1);
        }
        display.clear();

        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int by = 0; by < heightInBlocks; by++) {
            int finalBY = by;
//            System.out.printf("%,d / %,d || %,d : %,d : %,d%n", by, heightInBlocks, cacheHits, cacheMisses, CACHE.size());
            int rby = by * 4;
            int rowBytePosition = by * widthInBlocks * 8;

            service.submit(() -> {
                ByteBuffer safeBuffer = buffer.slice();
                safeBuffer.order(ByteOrder.LITTLE_ENDIAN);
                for (int bx = 0; bx < finalWidthInBlocks; bx++) {
                    int rbx = bx * 4;

                    Vector3f[] originalColors = new Vector3f[16];
                    ColorSetSignature signature = getOriginalColors(image, width, height, rby, rbx, originalColors);

                    boolean wasCached = false;
                    BlockData data;
                    BlockRange range = CACHE.get(signature);
                    if (range == null) {
                        data = findBestMinMaxBruteForce(originalColors);

                        CACHE.put(signature, new BlockRange(data.min, data.max));
                        CACHE_CHANGES.getAndIncrement();
                        cacheMisses.getAndIncrement();
                    } else {
                        wasCached = true;
                        data = new BlockData();
                        compute(data, range.min, range.max, originalColors);
                        cacheHits.getAndIncrement();
                    }

                    display(width, height, display, finalBY, bx, data, wasCached);

                    safeBuffer.position(rowBytePosition + bx * 8);
                    safeBuffer.putShort(data.min);
                    safeBuffer.putShort(data.max);
                    safeBuffer.putInt(data.code);
                    Thread.yield();
                }
            }, null);
        }

        for (int i = 0; i < heightInBlocks; i++) {
            try {
                service.take().get();
                int hits = cacheHits.get();
                int misses = cacheMisses.get();
                int total = hits + misses;
                float percent = hits / (float) total;
                System.out.printf(
                        "%,d / %,d || %,d : %,d : %,d (%.2f%%)%n",
                        i, heightInBlocks, hits, misses, CACHE.size(), percent * 100.0f
                );
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        return resultBytes;
    }

    private void display(int width, int height, PixelDisplay display, int finalBY, int bx, BlockData data, boolean wasCached) {
        var array = ImageWriterDXT1.interpolateColors(
                ImageUtil.decodeRGB565(data.min),
                ImageUtil.decodeRGB565(data.max)
        );

        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                var imageX = x + bx * 4;
                var imageY = y + finalBY * 4;

                if (imageX >= width || imageY >= height) {
                    continue;
                }

//                if (wasCached) {
//                    display.setPixel(imageX, imageY, 0x00FF00);
//                } else {
                    var index = x + y * 4;
                    var code = (data.code >> (index * 2)) & 0b11;
                    var color = ImageUtil.encodeRGB888(array[code]);
                    display.setPixel(imageX, imageY, color);
//                }
            }
        }
    }

    private BlockData findBestMinMaxBruteForce(Vector3f[] originalColors) {
        ImageWriterDXT1.BlockData base = ImageWriterDXT1.evaluate(originalColors);
        short bestMin = ImageUtil.encodeRGB565(base.min());
        short bestMax = ImageUtil.encodeRGB565(base.max());

        BlockData temp = new BlockData();

        BlockData a;
        BlockData b;

        for (int i = 0; i < 2; i++) {
            a = findBestMin(temp, bestMax, originalColors);
            bestMin = a.min;
            Thread.yield();

            b = findBestMax(temp, bestMin, originalColors);
            bestMax = b.max;
            Thread.yield();
        }

        compute(temp, bestMin, bestMax, originalColors);
        return temp;
    }

    private BlockData findBestMin(BlockData temp, short bestMax, Vector3f[] originalColors) {
        BlockData best = null;
        for (int min = 0; min <= ImageUtil.BITS_16; min++) {
            compute(temp, (short) min, bestMax, originalColors);
            if (best == null || temp.distance <= best.distance) {
                best = temp;
                temp = new BlockData();
            }
        }

        return best;
    }

    private BlockData findBestMax(BlockData temp, short bestMin, Vector3f[] originalColors) {
        BlockData best = null;

        for (int max = 0; max <= ImageUtil.BITS_16; max++) {
            compute(temp, bestMin, (short) max, originalColors);
            if (best == null || temp.distance <= best.distance) {
                best = temp;
                temp = new BlockData();
            }
        }

        return best;
    }

    private static final class BlockRange implements Serializable {
        private final short min, max;

        public BlockRange(short min, short max) {
            this.min = min;
            this.max = max;
        }
    }

    private static final class BlockData implements Serializable {
        private short min;
        private short max;
        private int code;
        private float distance;

        @Override
        public String toString() {
            return "BlockData{min=%04X, max=%04X, code=%d, distance=%s}".formatted(min, max, code, distance);
        }
    }

    private static void compute(BlockData target, short min, short max, Vector3f[] originalColors) {
        Vector3f[] options = ImageWriterDXT1.interpolateColors(
                ImageUtil.decodeRGB565(min & 0xFFFF),
                ImageUtil.decodeRGB565(max & 0xFFFF)
        );

        float[] distance = new float[1];
        int code = 0;
        for (int i = 0; i < originalColors.length; i++) {
            code |= getClosestColor(distance, options, originalColors[i]) << (i * 2);
        }

        target.min = min;
        target.max = max;
        target.code = code;
        target.distance = distance[0];
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

    private static ColorSetSignature getOriginalColors(
            BufferedImage image,
            int width, int height, int rby, int rbx,
            Vector3f[] originalColors
    ) {
        int[] intColors = new int[16];

        for (int x = 0; x < 4; x++) {
            int rx = Math.min(width - 1, rbx + x);
            for (int y = 0; y < 4; y++) {
                int ry = Math.min(height - 1, rby + y);

                int index = y * 4 + x;

                int rgb = image.getRGB(rx, ry);
                intColors[index] = rgb;

                originalColors[index] = ImageUtil.decodeRGB888(rgb);
            }
        }

        byte[] bytes = new byte[16 * 3];
        Arrays.sort(intColors);

        for (int i = 0; i < intColors.length; i++) {
            int rgb = intColors[i];

            bytes[i * 3] = (byte) (rgb >> 16 & ImageUtil.BITS_8);
            bytes[i * 3 + 1] = (byte) (rgb >> 8 & ImageUtil.BITS_8);
            bytes[i * 3 + 2] = (byte) (rgb & ImageUtil.BITS_8);
        }

        return new ColorSetSignature(bytes);
//        return Base64.getEncoder().encodeToString(bytes);
    }

    private static final class ColorSetSignature {
        private final byte[] bytes;
        private final int hashCode;

        private ColorSetSignature(byte[] bytes) {
            this.bytes = bytes;
            this.hashCode = Arrays.hashCode(bytes);
        }

        public byte[] bytes() {
            return bytes;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (ColorSetSignature) obj;
            return Arrays.equals(this.bytes, that.bytes);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            return "ColorSetSignature[" +
                    "bytes=" + Base64.getEncoder().encodeToString(bytes) + ']';
        }
    }

    //    private static String getOriginalColors(
    //            BufferedImage image,
    //            int width, int height, int rby, int rbx,
    //            Vector3f[] originalColors
    //    ) {
    //        byte[] bytes = new byte[16 * 3];
    //
    //        for (int x = 0; x < 4; x++) {
    //            int rx = Math.min(width - 1, rbx + x);
    //            for (int y = 0; y < 4; y++) {
    //                int ry = Math.min(height - 1, rby + y);
    //
    //                int index = y * 4 + x;
    //
    //                int rgb = image.getRGB(rx, ry);
    //                int r = (rgb >> 16 & ImageUtil.BITS_8);
    //                int g = (rgb >> 8 & ImageUtil.BITS_8);
    //                int b = (rgb & ImageUtil.BITS_8);
    //
    //                bytes[index * 3] = (byte) r;
    //                bytes[index * 3 + 1] = (byte) g;
    //                bytes[index * 3 + 2] = (byte) b;
    //
    //                originalColors[index] = new Vector3f(
    //                        r / (float) ImageUtil.BITS_8,
    //                        g / (float) ImageUtil.BITS_8,
    //                        b / (float) ImageUtil.BITS_8
    //                );
    //            }
    //        }
    //
    //        return Base64.getEncoder().encodeToString(bytes);
    //    }
}