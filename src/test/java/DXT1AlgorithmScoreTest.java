import blue.sparse.vfi.files.vtf.image.ImageDataFormat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

public class DXT1AlgorithmScoreTest {

    public static void main(String[] args) throws IOException {
        //[3919340, 91022, 7774]
        //[3264818, 42846, 15994]
        //[1525854, 24799, 4546]
        //[1858680, 16531, 12431]
        //[2230247, 71606, 34038]
        //[2179782, 63011, 48165]
        //[43933, 448, 1139]
        //[20074, 303, 167]
        //[292098, 4444, 4514]
        //[5984322, 206975, 67023]

        //   List[3919340, 91022, 7774], List[3264818, 42846, 15994], List[1525854, 24799, 4546], List[1858680, 16531, 12431], List[2230247, 71606, 34038], List[2179782, 63011, 48165], List[43933, 448, 1139], List[20074, 303, 167], List[292098, 4444, 4514], List[5984322, 206975, 67023]

        File folder = new File("E:\\Users\\Tom\\AppData\\Roaming\\.minecraft\\.instances\\Normal (1.15)\\screenshots");
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            String name = file.getName().toLowerCase();
            if(!name.endsWith(".png") && !name.endsWith(".jpg"))
                continue;

            long start = System.currentTimeMillis();
            BufferedImage image = ImageIO.read(file);
            ImageDataFormat.DXT1.write(image);
            long diff = System.currentTimeMillis() - start;
            System.out.printf("%s -> %,d * %,d -> %,dms%n", file.getName(), image.getWidth(), image.getHeight(), diff);
        }
    }

}
