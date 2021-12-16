import blue.sparse.vfi.files.vtf.image.impl.ImageWriterDXT1BruteForceAll;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class DXT1BruteForceTest {

    public static void main(String[] args) throws IOException {
        var writer = new ImageWriterDXT1BruteForceAll();

//        File folder = new File("M:\\source-resources\\materials\\metal");
//        File folder = new File("M:\\source-resources\\materials\\concrete");
        File folder = new File("C:\\Users\\eutax\\IdeaProjects\\valve-file-interface\\tests-in\\skybox");

        File[] files = folder.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            if(!name.endsWith(".png") && !name.endsWith(".jpg"))
                continue;

            System.out.println(name);
            writer.write(ImageIO.read(file));
//            ImageWriterDXT1BruteForceAll.saveCache();
        }


    }

}
