import blue.sparse.vfi.assets.ValveTexture;
import blue.sparse.vfi.files.vtf.*;
import blue.sparse.vfi.files.vtf.image.ImageDataFormat;
import blue.sparse.vfi.files.vtf.image.ImageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

public class VTFEnvMapTest {

    public enum Face {
        FRONT,
        BACK,
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

    public static void main(String[] args) throws IOException {
        DebugEnvMapGenerator.main(args);
//        ValveTexture loaded = ValveTexture.load(new File("tests-out/fortnitetom_envmap-4.vtf"));
//        loaded.export("PNG", new File("tests-out/export"), "test_export");
//        VTFFile vtf = loaded.getVTF();
//        List<VTFMipmap> mipmaps = vtf.getMipmaps();
//        System.out.println(mipmaps.size());
//        System.out.println(vtf);
//        System.out.println(vtf.getFlags());
//        for (VTFMipmap mipmap : mipmaps) {
//            System.out.println(mipmap);
//        }
//        loaded.save(new File("tests-out/test.vtf"));

        BufferedImage[] images = new BufferedImage[6];
        for (Face face : Face.values()) {
            images[face.ordinal()] = ImageIO.read(new File("tests-out/export/" + face.name().toLowerCase(Locale.ROOT) + ".png"));
        }

        File outFile = new File("tests-out/debug_envmap.vtf");
        outFile.getParentFile().mkdirs();

        VTFFile envMap = VTFFile.createEnvMap(images);
        envMap.setHighFormat(ImageDataFormat.DXT1);
        envMap.setVersionMajor(7);
        envMap.setVersionMinor(0);
        envMap.setFlag(VTFTextureFlag.CLAMPS, true);
        envMap.setFlag(VTFTextureFlag.CLAMPT, true);
        envMap.setFlag(VTFTextureFlag.NOLOD, true);
        envMap.setFlag(VTFTextureFlag.NOMIP, true);
        envMap.setFlag(VTFTextureFlag.ENVMAP, true);
        SeekableByteChannel channel = Files.newByteChannel(
                outFile.toPath(),
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE
        );

        VTFFile.write(envMap, channel);


//        BufferedImage image = ImageIO.read(new File("tests-in/skybox/front.png"));
//        ValveTexture valveTexture = ValveTexture.create(image);
//        valveTexture.setHighFormat(ImageDataFormat.DXT1);
//        valveTexture.save(new File("tests-out/sb-4096-DXT1.vtf"));
    }

}
