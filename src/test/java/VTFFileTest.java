import blue.sparse.vfi.assets.ValveTexture;
import blue.sparse.vfi.files.vtf.VTFFile;
import blue.sparse.vfi.files.vtf.image.ImageDataFormat;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class VTFFileTest {

    public static void main(String[] args) throws IOException {

        BufferedImage image = ImageIO.read(new File("tests-in/sb-4096.png"));
        ValveTexture valveTexture = ValveTexture.create(image);
        valveTexture.setHighFormat(ImageDataFormat.DXT1);
        valveTexture.save(new File("tests-out/sb-4096-DXT1.vtf"));

//		VTFFile read = VTFFile.read(new File("tests-in/sb-4096.png"));
//		System.out.println("lowResImageFormat = "+read.getHeader().lowResImageFormat);
//		System.out.println("highResImageFormat = "+read.getHeader().highResImageFormat);
////		read.getHeader().lowResImageFormat = ImageDataFormat.RGBA8888;
////		read.getHeader().highResImageFormat = ImageDataFormat.RGBA8888;
//
////		File out = new File("source/metal_box-out.vtf");
////		SeekableByteChannel channel = Files.newByteChannel(out.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
////		VTFFile.write(read, channel);
////		channel.close();
//
////		VTFFile.Header header = read.getHeader();
////
////		System.out.println(header);
//
//		BufferedImage thumbnail = read.getThumbnail();
//		ImageIO.write(thumbnail, "PNG", new File("tests-out/metal_box-thumbnail.png"));
//		BufferedImage image = read.getMipmaps().get(0).getImage();
//		ImageIO.write(image, "PNG", new File("tests-out/metal_box.vtf.png"));
    }

}
