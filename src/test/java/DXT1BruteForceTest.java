import blue.sparse.vfi.files.vtf.image.impl.ImageWriterDXT1BruteForceAll;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class DXT1BruteForceTest {

    public static void main(String[] args) throws IOException {
        var writer = new ImageWriterDXT1BruteForceAll();

        File folder = new File("M:\\source-resources\\materials\\metal");

        File[] files = folder.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            System.out.println(file.getName());
            writer.write(ImageIO.read(file));
            ImageWriterDXT1BruteForceAll.saveCache();
        }


    }

}
