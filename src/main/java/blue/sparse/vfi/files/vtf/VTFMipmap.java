package blue.sparse.vfi.files.vtf;

import java.awt.image.BufferedImage;
import java.util.List;

public record VTFMipmap(int index, int width, int height, List<VTFFrame> frames) {

	public VTFFrame getFrame(int frame) {
		return frames.get(frame);
	}
	
	public VTFFace getFace(int frame, int face) {
		return getFrame(frame).faces().get(face);
	}

	public BufferedImage getImage() {
		return getImage(0, 0);
	}

	public BufferedImage getImage(int frame) {
		return getImage(frame, 0);
	}

	public BufferedImage getImage(int frame, int face) {
		return getFace(frame, face).image();
	}

}
