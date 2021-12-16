package blue.sparse.vfi.files.vtf.image.impl;

import blue.sparse.pixel.Color;
import blue.sparse.pixel.PixelDisplay;
import blue.sparse.vfi.files.vtf.image.ImageFormatWriter;
import blue.sparse.vfi.files.vtf.image.ImageUtil;
import blue.sparse.vfi.files.vtf.image.impl.dxt.ImageWriterDXT1;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class ImageWriterDXT1BruteForceAll implements ImageFormatWriter {

    private static final Map<ColorSetSignature, BlockRange> CACHE = new ConcurrentHashMap<>();
    private static final AtomicInteger CACHE_CHANGES = new AtomicInteger();
    private static final AtomicBoolean SAVING_CACHE = new AtomicBoolean();
//    private static final ExecutorService SERVICE = Executors.newCachedThreadPool(r -> {
//        Thread t = new Thread(r);
//        t.setPriority(Thread.NORM_PRIORITY - 2);
//        t.setDaemon(true);
//        return t;
//    });
//    private static final ExecutorService SAVE_SERVICE = Executors.newSingleThreadExecutor();

    private static final ExecutorService SERVICE = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1, r -> {
        Thread t = new Thread(r);
//        t.setPriority(Thread.NORM_PRIORITY - 2);
        t.setDaemon(true);
        return t;
    });

    public static final int BRUTE_FORCE_OFFSET_RANGE = 4096;

    public static boolean saveCache() throws IOException {
        int currentChangeCount = CACHE_CHANGES.get();
        if (currentChangeCount == 0)
            return true;

        if (SAVING_CACHE.compareAndExchange(false, true)) {
            return false;
        }

        System.out.println("Saving cache...");
        Map<ColorSetSignature, BlockRange> cacheCopy = new HashMap<>(CACHE);
        File file = new File("dxt1_cache.bin");
        DataOutputStream out = new DataOutputStream(
                new GZIPOutputStream(
                        new BufferedOutputStream(new FileOutputStream(file))
                )
        );
        out.writeInt(cacheCopy.size());
        for (Map.Entry<ColorSetSignature, BlockRange> entry : cacheCopy.entrySet()) {
            out.write(entry.getKey().bytes);
            out.writeShort(entry.getValue().min);
            out.writeShort(entry.getValue().max);
        }
        out.close();
        System.out.println("Done saving cache");
        CACHE_CHANGES.getAndUpdate(it -> it - currentChangeCount);
        SAVING_CACHE.set(false);
        return true;
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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                while(!saveCache()) {
                    Thread.onSpinWait();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
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
        int finalHeightInBlocks = heightInBlocks;

        int totalBlocks = widthInBlocks * heightInBlocks;

        byte[] resultBytes = new byte[totalBlocks * 8];
        ByteBuffer buffer = ByteBuffer.wrap(resultBytes);

        AtomicInteger cacheHits = new AtomicInteger();
        AtomicInteger cacheMisses = new AtomicInteger();
        DoubleAdder inverseQuality = new DoubleAdder();

        ExecutorCompletionService<Void> service = new ExecutorCompletionService<>(SERVICE);

        int minW = Math.min(width, 512);
        int minH = Math.min(height, 512);
        int displayDivX = width / minW;
        int displayDivY = height / minH;
//        if (display == null || display.getWidth() < minW || display.getHeight() < minH) {
//            if (display != null) {
//                display.close();
//            }
//            display = new PixelDisplay("DXT1", minW, minH, 1);
//        }
//        display.clear();

        buffer.order(ByteOrder.LITTLE_ENDIAN);
        for (int by = 0; by < heightInBlocks; by++) {
            int finalBY = by;
//            System.out.printf("%,d / %,d || %,d : %,d : %,d%n", by, heightInBlocks, cacheHits, cacheMisses, CACHE.size());
//            int rby = by * 4;
//            int rowBytePosition = by * widthInBlocks * 8;

            service.submit(() -> {
                ByteBuffer safeBuffer = buffer.slice();
                safeBuffer.order(ByteOrder.LITTLE_ENDIAN);
                Vector3f[] originalColors = new Vector3f[16];
                int[] intColors = new int[16];
                byte[] byteColors = new byte[16 * 3];
                Vector2i position = new Vector2i();
                for (int bx = 0; bx < finalWidthInBlocks; bx++) {
//                    int rbx = bx * 4;

//                    int si = totalBlocks - (finalBY * finalWidthInBlocks + bx) - 1;
                    int sbx, sby, rbx, rby;
//                    if(finalWidthInBlocks == finalHeightInBlocks) {
//                        int si = finalBY * finalWidthInBlocks + bx;
//                        getSpiral(position, si).add((finalWidthInBlocks / 2) - 1, finalHeightInBlocks / 2);
//
//                        sbx = position.x;
//                        sby = position.y;
//
//                    } else {
                        sbx = bx;
                        sby = finalBY;
//                    }
                    rbx = sbx * 4;
                    rby = sby * 4;

                    ColorSetSignature signature = getOriginalColors(image, width, height, rby, rbx, intColors, originalColors, byteColors);

                    boolean wasCached = false;
                    BlockData data;
                    BlockRange range = CACHE.get(signature);
                    if (range == null) {
                        data = findBestMinMaxBruteForce(originalColors);

                        CACHE.put(signature.copy(), new BlockRange(data.min, data.max));
                        CACHE_CHANGES.getAndIncrement();
                        cacheMisses.getAndIncrement();
                    } else {
                        wasCached = true;
                        data = new BlockData();
                        compute(data, range.min, range.max, originalColors);
                        cacheHits.getAndIncrement();
                    }

                    inverseQuality.add(data.distance);
//                    display(width, height, display, finalBY, bx, data, wasCached);
                    display(width, height, displayDivX, displayDivY, sby, sbx, data, wasCached);

//                    safeBuffer.position(rowBytePosition + bx * 8);
                    safeBuffer.position((sby * finalWidthInBlocks + sbx) * 8);
                    safeBuffer.putShort(data.min);
                    safeBuffer.putShort(data.max);
                    safeBuffer.putInt(data.code);
                }
            }, null);
        }

        //255 / 256 || 2,894 : 62,642 : 62,501 (4.42%) || 6758.09
        //255 / 256 || 2,859 : 62,677 : 62,501 (4.36%) || 6753.44
        //255 / 256 || 3,023 : 62,513 : 62,500 (4.61%) || 6804.14
        //255 / 256 || 3,022 : 62,514 : 62,500 (4.61%) || 6814.95 //
        //255 / 256 || 3,025 : 62,511 : 62,500 (4.62%) || 6817.19

        for (int i = 0; i < heightInBlocks; i++) {
            try {
                service.take().get();
                int hits = cacheHits.get();
                int misses = cacheMisses.get();
                int total = hits + misses;
                float percent = hits / (float) total;
                System.out.printf(
                        "%,d / %,d || %,d : %,d : %,d (%.2f%%) || %.2f%n",
                        i, heightInBlocks, hits, misses, CACHE.size(), percent * 100.0f, inverseQuality.sum()
                );
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        //TODO: Good?
        ForkJoinPool.commonPool().submit(() -> {
            try {
                saveCache();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return resultBytes;
    }

    private void display(int width, int height, int divX, int divY, int finalBY, int bx, BlockData data, boolean wasCached) {
        if(display == null)
            return;
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
//                    display.setPixel(imageX / divX, imageY / divY, 0x00FF00);
//                } else {
                    var index = x + y * 4;
                    var code = (data.code >> (index * 2)) & 0b11;
//                System.out.println(array[code]);
                    var color = ImageUtil.encodeRGB888(array[code]);
                    display.setPixel(imageX / divX, imageY / divY, color);
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

        for (int i = 0; i < 1; i++) {
            a = findBestMin(temp, bestMin, bestMax, originalColors);
            bestMin = a.min;
//            Thread.yield();

            b = findBestMax(temp, bestMin, bestMax, originalColors);
            bestMax = b.max;
//            Thread.yield();
        }

        compute(temp, bestMin, bestMax, originalColors);
        return temp;
    }

    private BlockData findBestMin(BlockData temp, short bestMin, short bestMax, Vector3f[] originalColors) {
        BlockData best = null;
        int bestMinInt = bestMin & 0xFFFF;
//        for (int min = 0; min <= ImageUtil.BITS_16; min++) {
        for (int min = bestMinInt - BRUTE_FORCE_OFFSET_RANGE; min <= bestMinInt + BRUTE_FORCE_OFFSET_RANGE; min++) {
            compute(temp, (short) min, bestMax, originalColors);
            if (best == null || temp.distance <= best.distance) {
                BlockData old = best == null ? new BlockData() : best;
                best = temp;
                temp = old;
//                temp = new BlockData();
            }
        }

        return best;
    }

    private BlockData findBestMax(BlockData temp, short bestMin, short bestMax, Vector3f[] originalColors) {
        BlockData best = null;

        int bestMaxInt = bestMax & 0xFFFF;

//        for (int max = 0; max <= ImageUtil.BITS_16; max++) {
        for (int max = bestMaxInt - BRUTE_FORCE_OFFSET_RANGE; max <= bestMaxInt + BRUTE_FORCE_OFFSET_RANGE; max++) {
            compute(temp, bestMin, (short) max, originalColors);
            if (best == null || temp.distance <= best.distance) {
                BlockData old = best == null ? new BlockData() : best;
                best = temp;
                temp = old;
//                best = temp;
//                temp = new BlockData();
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

    private static final ThreadLocal<Vector3f[]> COMPUTE_TARGET = ThreadLocal.withInitial(() -> {
        Vector3f[] target = new Vector3f[4];
        for (int i = 0; i < target.length; i++) {
            target[i] = new Vector3f();
        }
        return target;
    });

    private static final ThreadLocal<float[]> DISTANCE_TARGET = ThreadLocal.withInitial(() -> new float[1]);

    private static void compute(BlockData target, short min, short max, Vector3f[] originalColors) {
        Vector3f[] options = ImageWriterDXT1.interpolateColorsPreEncoded(
                COMPUTE_TARGET.get(),
                min & 0xFFFF,
                ImageUtil.decodeRGB565(min & 0xFFFF),
                max & 0xFFFF,
                ImageUtil.decodeRGB565(max & 0xFFFF)
        );

        float[] distance = DISTANCE_TARGET.get();
        distance[0] = 0f;

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
        float d0 = ImageUtil.distanceSquared(options[0], color);
        float d1 = ImageUtil.distanceSquared(options[1], color);
        float d2 = ImageUtil.distanceSquared(options[2], color);
        float d3 = ImageUtil.distanceSquared(options[3], color);

        float d;
        int i;

        if(d0 < d1) {
            i = 0;
            d = d0;
        } else {
            i = 1;
            d = d1;
        }

        if(d2 < d) {
            i = 2;
            d = d2;
        }

        if(d3 < d) {
            i = 3;
            d = d3;
        }

//        if(d0 < d1 && d0 < d2 && d0 < d3) {
//            i = 0;
//            d = d0;
//        } else if(d1 < d0 && d1 < d2 && d1 < d3) {
//            i = 1;
//            d = d1;
//        } else if(d2 < d0 && d2 < d1 && d2 < d3) {
//            i = 2;
//            d = d2;
//        } else {
//            i = 3;
//            d = d3;
//        }

        distanceTarget[0] += Math.sqrt(d);
        return i;
//        float distance = Float.POSITIVE_INFINITY;
//        int index = -1;
//
//        for (int i = 0; i < 4; i++) {
////            float currentDistance = options[i].distanceSquared(color);
//            float currentDistance = ImageUtil.distanceSquared(options[i], color);
//            if (currentDistance < distance) {
//                distance = currentDistance;
//                index = i;
//            }
//        }
//
//        distanceTarget[0] += Math.sqrt(distance);
//        return index;
    }

//    private static final ThreadLocal<int[]> INT_COLORS_TARGET = ThreadLocal.withInitial(() -> new int[16]);

    private static ColorSetSignature getOriginalColors(
            BufferedImage image,
            int width, int height, int rby, int rbx,
            int[] intColors,
            Vector3f[] originalColors,
            byte[] bytes
    ) {
//        int[] intColors = INT_COLORS_TARGET.get();//new int[16];

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

//        byte[] bytes = new byte[16 * 3];
        Arrays.sort(intColors);

        for (int i = 0; i < intColors.length; i++) {
            int rgb = intColors[i];

            bytes[i * 3] = (byte) (rgb >> 16 & 0xFF);
            bytes[i * 3 + 1] = (byte) (rgb >> 8 & 0xFF);
            bytes[i * 3 + 2] = (byte) (rgb & 0xFF);
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

        private ColorSetSignature(byte[] bytes, int hashCode) {
            this.bytes = bytes;
            this.hashCode = hashCode;
        }

        public ColorSetSignature copy() {
            return new ColorSetSignature(Arrays.copyOf(bytes, bytes.length), hashCode);
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

    private static Vector2i vec2i(Vector2i target, double x, double y) {
        return target.set((int) x, (int) y);
    }

    public static Vector2i getSpiral(Vector2i target, int index) {
        if (index == 0) return new Vector2i(0);

        var radius = Math.floor((Math.sqrt(index) - 1) / 2) + 1;
        var p = (8 * radius * (radius - 1)) / 2;
        var a = (index - p) % (radius * 8);
        var en = radius * 2;
        var side = (int) Math.floor(a / en);

        return switch (side) {
            case 0 -> vec2i(target, a - radius, -radius);
            case 1 -> vec2i(target, radius, (a % en) - radius);
            case 2 -> vec2i(target, radius - (a % en), radius);
            case 3 -> vec2i(target, -radius, radius - (a % en));
            default -> throw new IllegalStateException("Unexpected state!");
        };
    }

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        BufferedImage input = ImageIO.read(new File("tests-in/skybox1024/back.png"));
        int[] intColors = new int[16];
        Vector3f[] originalColors = new Vector3f[16];
        for (int i = 0; i < originalColors.length; i++) {
            originalColors[i] = new Vector3f();
        }
        byte[] byteColors = new byte[16 * 3];
        getOriginalColors(input, input.getWidth(), input.getHeight(), 157, 120, intColors, originalColors, byteColors);


        int rgb565Values = 65536;
        int divisor = 64;
        int visSize = rgb565Values / divisor;
        PixelDisplay display = new PixelDisplay("DXT1 Visualization", visSize, visSize, 1);
        float[] values = new float[visSize * visSize];

        ExecutorCompletionService<Void> service = new ExecutorCompletionService<>(SERVICE);

        float maxDistance = (float) (Math.sqrt(3) * divisor * divisor);

        for (int min = 0; min < rgb565Values; min++) {
            int finalMin = min;
            service.submit(() -> {
                BlockData target = new BlockData();
                for (int max = 0; max < rgb565Values; max++) {
                    compute(target, (short) finalMin, (short) max, originalColors);
                    float distance = target.distance / 16f;
                    int vx = finalMin / divisor;
                    int vy = max / divisor;
                    float v = values[vx * visSize + vy] += distance;
                    display.setPixel(vx, vy, new Color(v / maxDistance).getRGB());
                }
            }, null);
        }

        for (int i = 0; i < rgb565Values; i++) {
            service.take().get();
            System.out.println(i);
        }

        BufferedImage image = new BufferedImage(visSize, visSize, BufferedImage.TYPE_BYTE_GRAY);
        for (int x = 0; x < visSize; x++) {
            for (int y = 0; y < visSize; y++) {
                float v = values[x * visSize + y];
                image.setRGB(x, y, new Color(v / maxDistance).getRGB());
            }
        }

        ImageIO.write(image, "PNG", new File("visualize_dxt1.png"));

//        Set<Vector2i> positions = new HashSet<>();
//
//        int size = 256;
//        int area = size * size;
//        for (int i = 0; i < area; i++) {
//            Vector2i position = getSpiral(new Vector2i(), area - i - 1).add((size / 2) - 1, size / 2);
//            if(position.x < 0 || position.y < 0 || position.x >= size || position.y >= size)
//                System.out.println(position);
//            positions.add(position);
//        }
//
//        System.out.println(positions.size());
//        System.out.println(area);
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