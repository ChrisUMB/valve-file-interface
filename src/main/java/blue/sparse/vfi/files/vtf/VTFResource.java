package blue.sparse.vfi.files.vtf;

import java.nio.charset.StandardCharsets;

public abstract class VTFResource {

	private final byte tag0, tag1, tag2;

	private VTFResource(byte tag0, byte tag1, byte tag2) {
		this.tag0 = tag0;
		this.tag1 = tag1;
		this.tag2 = tag2;
	}

	private VTFResource(byte[] tag) {
		this(tag[0], tag[1], tag[2]);
	}

	private VTFResource(String tag) {
		this(tag.getBytes(StandardCharsets.UTF_8));
	}

	public abstract byte getFlag();

	public boolean isHighResImageData() {
		return this instanceof BinData && tag0 == (byte) 0x30 && tag1 == 0 && tag2 == 0;
	}

	public boolean isLowResImageData() {
		return this instanceof BinData && tag0 == (byte) 0x01 && tag1 == 0 && tag2 == 0;
	}

	public byte[] getTag() {
		return new byte[]{tag0, tag1, tag2};
	}

	public String getStringTag() {
		return new String(getTag());
	}

	public byte getTag0() {
		return tag0;
	}

	public byte getTag1() {
		return tag1;
	}

	public byte getTag2() {
		return tag2;
	}

	public static class IntData extends VTFResource {

		private int data;

		public IntData(byte tag0, byte tag1, byte tag2, int data) {
			super(tag0, tag1, tag2);
			this.data = data;
		}

		public IntData(byte[] tag, int data) {
			super(tag);
			this.data = data;
		}

		public IntData(String tag, int data) {
			super(tag);
			this.data = data;
		}

		public int getData() {
			return data;
		}

		public void setData(int data) {
			this.data = data;
		}

		@Override
		public byte getFlag() {
			return (byte) 0x2;
		}
	}

	public static class BinData extends VTFResource {

		private byte[] data;

		public BinData(byte tag0, byte tag1, byte tag2, byte[] data) {
			super(tag0, tag1, tag2);
			this.data = data;
		}

		public BinData(byte[] tag, byte[] data) {
			super(tag);
			this.data = data;
		}

		public BinData(String tag, byte[] data) {
			super(tag);
			this.data = data;
		}

		public byte[] getData() {
			return data;
		}

		public void setData(byte[] data) {
			this.data = data;
		}

		@Override
		public byte getFlag() {
			return (byte) 0x0;
		}

		public String getDataAsString() {
			return new String(data);
		}
	}
}
