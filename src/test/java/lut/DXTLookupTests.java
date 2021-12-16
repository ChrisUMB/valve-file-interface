package lut;

import blue.sparse.pixel.PixelDisplay;
import blue.sparse.vfi.files.vtf.image.ImageUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DXTLookupTests {

    public static void main(String[] args) throws InterruptedException {
        PixelDisplay triangleTest = new PixelDisplay("Triangle Test", 256, 256, 3);
        Thread.sleep(2000);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int x = 0; x <= ImageUtil.BITS_16; x++) {
            int fx = x;
            pool.submit(() -> {
                for (int y = 0; y <= fx; y++) {
                    triangleTest.setPixel(fx / 256, y / 256, fx > y ? 0xFF0000 : 0x0000FF);
                }
            });
        }
    }

}
