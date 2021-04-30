import blue.sparse.vfi.files.vtf.VTFFile;
import blue.sparse.vfi.files.vtf.image.ImageDataFormat;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class VTFFileTest {

	public static void main(String[] args) throws IOException {

		VTFFile read = VTFFile.read(new File("source/7-2.vtf"));
		read.setHighFormat(ImageDataFormat.RGBA8888);
		read.setLowFormat(ImageDataFormat.RGBA8888);
//		read.getHeader().lowResImageFormat = ImageDataFormat.RGBA8888;
//		read.getHeader().highResImageFormat = ImageDataFormat.RGBA8888;

		File out = new File("source/7-2-out.vtf");
		SeekableByteChannel channel = Files.newByteChannel(out.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		VTFFile.write(read, channel);
		channel.close();

//		VTFFile.Header header = read.getHeader();
//
//		System.out.println(header);
//
//		BufferedImage thumbnail = read.getThumbnail();
//		ImageIO.write(thumbnail, "PNG", new File("source/7-0-thumbnail.png"));
//		BufferedImage image = read.getMipmaps().get(0).getImage();
//		ImageIO.write(image, "PNG", new File("source/7-0.png"));
	}

}
