package blue.sparse.vfi.assets;

import blue.sparse.vfi.files.vtf.VTFFile;
import blue.sparse.vfi.files.vtf.VTFMipmap;
import blue.sparse.vfi.files.vtf.VTFResource;
import blue.sparse.vfi.files.vtf.image.ImageDataFormat;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

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
}
