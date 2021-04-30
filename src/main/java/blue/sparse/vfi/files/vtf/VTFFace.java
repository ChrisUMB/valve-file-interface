package blue.sparse.vfi.files.vtf;

import java.awt.image.BufferedImage;
import java.util.Objects;

public final class VTFFace {
	private final int index;
	private BufferedImage image;

	public VTFFace(int index, BufferedImage image) {
		this.index = index;
		this.image = image;
	}

	public int getIndex() {
		return index;
	}

	public BufferedImage getImage() {
		return image;
	}

	public void setImage(BufferedImage image) {
		this.image = image;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (VTFFace) obj;
		return this.index == that.index &&
				Objects.equals(this.image, that.image);
	}

	@Override
	public int hashCode() {
		return Objects.hash(index, image);
	}

	@Override
	public String toString() {
		return "VTFFace[" +
				"index=" + index + ", " +
				"image=" + image + ']';
	}
}
