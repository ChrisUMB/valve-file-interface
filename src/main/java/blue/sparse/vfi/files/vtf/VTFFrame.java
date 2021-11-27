package blue.sparse.vfi.files.vtf;

import java.util.List;
import java.util.Objects;

public final class VTFFrame {
	private final int index;
	private final List<VTFFace> faces;

	public VTFFrame(int index, List<VTFFace> faces) {
		this.index = index;
		this.faces = faces;
	}

	public int getIndex() {
		return index;
	}

	public List<VTFFace> getFaces() {
		return faces;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (VTFFrame) obj;
		return this.index == that.index &&
				Objects.equals(this.faces, that.faces);
	}

	@Override
	public int hashCode() {
		return Objects.hash(index, faces);
	}

	@Override
	public String toString() {
		return "VTFFrame[" +
				"index=" + index + ", " +
				"faces=" + faces + ']';
	}

}
