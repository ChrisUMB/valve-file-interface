package blue.sparse.vfi.files.vtf;

import blue.sparse.vfi.files.vtf.image.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class VTFMipmap {
	private final int index;
	private final int width;
	private final int height;
	private final List<VTFFrame> frames;

	public VTFMipmap(int index, int width, int height, List<VTFFrame> frames) {
		this.index = index;
		this.width = width;
		this.height = height;
		this.frames = frames;
	}

	public static List<VTFMipmap> generate(List<BufferedImage> frameImages) {
		int width = frameImages.get(0).getWidth();
		int height = frameImages.get(0).getHeight();
		List<VTFMipmap> results = new ArrayList<>();

		int index = 0;

		while(true) {
			int w = width >> index;
			int h = height >> index;

			if(w < 1 || h < 1) {
				break;
			}

			VTFMipmap mipmap = new VTFMipmap(index, w, h, new ArrayList<>());
			results.add(mipmap);

			for (int i = 0; i < frameImages.size(); i++) {
				BufferedImage image = frameImages.get(i);

				if(index > 0) {
					image = ImageUtil.scaleImage(image, w, h);
				}

				List<VTFFrame> frames = mipmap.frames;
				VTFFrame frame = new VTFFrame(i, new ArrayList<>());
				VTFFace face = new VTFFace(0, image);
				frame.getFaces().add(face);
				frames.add(frame);
			}

			index++;
		}

		return results;
	}

	public static List<VTFMipmap> generate(BufferedImage image) {
		return generate(List.of(image));
	}

	public VTFFrame getFrame(int frame) {
		return frames.get(frame);
	}

	public VTFFace getFace(int frame, int face) {
		return getFrame(frame).getFaces().get(face);
	}

	public BufferedImage getImage() {
		return getImage(0, 0);
	}

	public BufferedImage getImage(int frame) {
		return getImage(frame, 0);
	}

	public BufferedImage getImage(int frame, int face) {
		return getFace(frame, face).getImage();
	}

	public void setImage(BufferedImage image) {
		setImage(0, 0, image);
	}

	public void setImage(int frame, BufferedImage image) {
		setImage(frame, 0, image);
	}

	public void setImage(int frame, int face, BufferedImage image) {
		getFace(frame, face).setImage(image);
	}

	public int getIndex() {
		return index;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public List<VTFFrame> getFrames() {
		return frames;
	}

	public void clearImages() {
		frames.clear();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (VTFMipmap) obj;
		return this.index == that.index &&
				this.width == that.width &&
				this.height == that.height &&
				Objects.equals(this.frames, that.frames);
	}

	@Override
	public int hashCode() {
		return Objects.hash(index, width, height, frames);
	}

	@Override
	public String toString() {
		return "VTFMipmap[" +
				"index=" + index + ", " +
				"width=" + width + ", " +
				"height=" + height + ", " +
				"frames=" + frames + ']';
	}

}
