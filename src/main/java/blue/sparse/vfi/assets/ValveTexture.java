package blue.sparse.vfi.assets;

import blue.sparse.vfi.files.vtf.*;
import blue.sparse.vfi.files.vtf.image.ImageDataFormat;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Set;

public final class ValveTexture implements ValveAsset {

	private final VTFFile vtf;

	private ValveTexture(VTFFile vtf) {
		this.vtf = vtf;
	}

	public static ValveTexture load(File file) throws IOException {
		return new ValveTexture(VTFFile.read(file));
	}

	public static ValveTexture create(BufferedImage image) {
		return new ValveTexture(VTFFile.create(image));
	}

	public void save(File file) throws IOException {
		SeekableByteChannel channel = Files.newByteChannel(
				file.toPath(),
				StandardOpenOption.WRITE,
				StandardOpenOption.CREATE
		);

		VTFFile.write(vtf, channel);
	}

	public void export(String format, File file) throws IOException {
		BufferedImage image = vtf.getImage();
		ImageIO.write(image, format, file);
	}

	public void export(String format, File directory, String name) throws IOException {
		List<VTFMipmap> mipmaps = vtf.getMipmaps();
		if (mipmaps.isEmpty()) {
			throw new IllegalStateException("Tried to export VTF, but mipmaps were empty. [\"" + name + "\"]");
		}

		directory.mkdirs();

		VTFMipmap mipmap = mipmaps.get(0);
		List<VTFFrame> frames = mipmap.getFrames();
		int i = 0;
		for (VTFFrame frame : frames) {
			List<VTFFace> faces = frame.getFaces();
			if (faces.isEmpty()) {
				continue;
			}

			VTFFace face = faces.get(0);

			File out = new File(directory, name + "." + i + "." + format.toLowerCase());
			ImageIO.write(face.getImage(), format, out);
			i++;
		}
	}

	public int getVersion() {
		return vtf.getVersion();
	}

	public int getVersionMajor() {
		return vtf.getVersionMajor();
	}

	public int getVersionMinor() {
		return vtf.getVersionMinor();
	}

	public void setVersion(int major, int minor) {
		vtf.setVersion(major, minor);
	}

	public void setVersionMajor(int major) {
		vtf.setVersionMajor(major);
	}

	public void setVersionMinor(int minor) {
		vtf.setVersionMinor(minor);
	}

	public ImageDataFormat getHighFormat() {
		return vtf.getHighFormat();
	}

	public void setHighFormat(ImageDataFormat format) {
		vtf.setHighFormat(format);
	}

	public ImageDataFormat getLowFormat() {
		return vtf.getLowFormat();
	}

	public void setLowFormat(ImageDataFormat format) {
		vtf.setLowFormat(format);
	}

	public void setFormats(ImageDataFormat high, ImageDataFormat low) {
		vtf.setFormats(high, low);
	}

	public BufferedImage getThumbnail() {
		return vtf.getThumbnail();
	}

	public List<VTFMipmap> getMipmaps() {
		return vtf.getMipmaps();
	}

	public BufferedImage getImage() {
		return vtf.getImage();
	}

	public void setImage(BufferedImage image) {
		vtf.setImage(0, image);
	}

	public void setImage(int frameIndex, BufferedImage image) {
		vtf.setImage(frameIndex, image);
	}

	public void setImage(int frameIndex, int faceIndex, BufferedImage image) {
		vtf.setImage(frameIndex, faceIndex, image);
	}

	public List<VTFResource> getResources() {
		return vtf.getResources();
	}

	public VTFResource getResourceByTag(String tag) {
		return vtf.getResourceByTag(tag);
	}

	public VTFResource getResourceByTag(byte a, byte b, byte c) {
		return vtf.getResourceByTag(a, b, c);
	}

	public Set<VTFTextureFlag> getFlags() {
		return vtf.getFlags();
	}

	public boolean isFlagSet(VTFTextureFlag flag) {
		return vtf.isFlagSet(flag);
	}

	public boolean setFlag(VTFTextureFlag flag, boolean state) {
		return vtf.setFlag(flag, state);
	}

	public Vector3f getReflectivity() {
		return vtf.getReflectivity();
	}

	public void setReflectivity(Vector3f reflectivity) {
		vtf.setReflectivity(reflectivity);
	}

	public VTFFile getVTF() {
		return vtf;
	}
}
