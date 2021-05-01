package blue.sparse.vfi.files.vtf;

import blue.sparse.vfi.files.vtf.image.ImageDataFormat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class VTFFile {

	private final Header header;
	private final BufferedImage thumbnail;
	private final List<VTFMipmap> mipmaps;
	private final List<VTFResource> resources;

	public VTFFile(Header header, BufferedImage thumbnail, List<VTFMipmap> mipmaps, List<VTFResource> resources) {
		this.header = header;
		this.thumbnail = thumbnail;
		this.mipmaps = mipmaps;
		this.resources = resources;
	}

	public Header getHeader() {
		return header;
	}

	public BufferedImage getThumbnail() {
		return thumbnail;
	}

	public List<VTFMipmap> getMipmaps() {
		return mipmaps;
	}

	public List<VTFResource> getResources() {
		return resources;
	}

	public static final class Header {

		//7.0, 7.1
		public int versionMajor;
		public int versionMinor;
		public int headerSize;
		public short width;
		public short height;
		public int flags;
		public short frameCount;
		public short firstFrame;
		public byte[] padding0;
		public float[] reflectivity;
		public byte[] padding1;
		public float bumpmapScale;
		public ImageDataFormat highResImageFormat;
		public byte mipmapCount;
		public ImageDataFormat lowResImageFormat;
		public byte lowResImageWidth;
		public byte lowResImageHeight;

		//7.2
		public short depth;

		//7.3, 7.4, 7.5
		public byte[] padding2;
		public int resourceCount;
		public byte[] padding3;

		@Override
		public String toString() {
			return "Header{" +
					"versionMajor=" + versionMajor +
					", versionMinor=" + versionMinor +
					", headerSize=" + headerSize +
					", width=" + width +
					", height=" + height +
					", flags=" + flags +
					", frameCount=" + frameCount +
					", firstFrame=" + firstFrame +
					", padding0=" + Arrays.toString(padding0) +
					", reflectivity=" + Arrays.toString(reflectivity) +
					", padding1=" + Arrays.toString(padding1) +
					", bumpmapScale=" + bumpmapScale +
					", highResImageFormat=" + highResImageFormat +
					", mipmapCount=" + mipmapCount +
					", lowResImageFormat=" + lowResImageFormat +
					", lowResImageWidth=" + lowResImageWidth +
					", lowResImageHeight=" + lowResImageHeight +
					", depth=" + depth +
					", padding2=" + Arrays.toString(padding2) +
					", resourceCount=" + resourceCount +
					'}';
		}
	}

	public static VTFFile read(File file) throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(file.toPath()));
		return read(buffer);
	}

	public static VTFFile read(ByteBuffer buffer) {
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		VTFFile.Header header = new VTFFile.Header();

		byte[] signature = new byte[4];
		buffer.get(signature);

		if (signature[0] != 'V' || signature[1] != 'T' || signature[2] != 'F' || signature[3] != 0) {
			throw new IllegalStateException("Tried to read data that had invalid VTF signature. %s".formatted(
					Arrays.toString(signature))
			);
		}

		header.versionMajor = buffer.getInt();
		header.versionMinor = buffer.getInt();
		header.headerSize = buffer.getInt();
		header.width = buffer.getShort();
		header.height = buffer.getShort();
		header.flags = buffer.getInt();
		header.frameCount = buffer.getShort();
		header.firstFrame = buffer.getShort();

		buffer.get(header.padding0 = new byte[4]);

		float[] reflectivity = new float[3];
		reflectivity[0] = buffer.getFloat();
		reflectivity[1] = buffer.getFloat();
		reflectivity[2] = buffer.getFloat();
		header.reflectivity = reflectivity;

		buffer.get(header.padding1 = new byte[4]);

		header.bumpmapScale = buffer.getFloat();

		ImageDataFormat highResFormat = ImageDataFormat.values()[buffer.getInt()];
		header.highResImageFormat = highResFormat;

		header.mipmapCount = buffer.get();

		ImageDataFormat lowResFormat = ImageDataFormat.values()[buffer.getInt()];
		header.lowResImageFormat = lowResFormat;

		header.lowResImageWidth = buffer.get();
		header.lowResImageHeight = buffer.get();

		int version = header.versionMajor * 10 + header.versionMinor;

		if (version >= 72) {
			header.depth = buffer.getShort();
		}

		List<VTFResource> resources = new ArrayList<>();

		if (version >= 73) {
			System.out.println("Handling resources...");
			List<VTFMipmap> mipmaps = null;
			BufferedImage thumbnail = null;

			buffer.get(header.padding2 = new byte[3]);
			header.resourceCount = buffer.getInt();
			buffer.get(header.padding3 = new byte[8]);

			for (int i = 0; i < header.resourceCount; i++) {
				byte[] tag = new byte[3];
				buffer.get(tag);

				byte flags = buffer.get();
				int offset = buffer.getInt();

				resources.add(new VTFResource(tag, flags, offset));

				if (tag[0] == (byte) 0x30 && tag[1] == 0 && tag[2] == 0) {
					System.out.println("Found high resolution resource data tag.");
					int start = buffer.position();
					buffer.position(offset);
					mipmaps = readMipmaps(buffer, header, highResFormat);
					buffer.position(start);
					continue;
				}

				if (tag[0] == (byte) 0x01 && tag[1] == 0 && tag[2] == 0) {
					System.out.println("Found low resolution resource data tag.");
					int start = buffer.position();
					buffer.position(offset);
					thumbnail = lowResFormat.read(header.lowResImageWidth, header.lowResImageHeight, buffer);
					buffer.position(start);
				}
			}

			return new VTFFile(header, thumbnail, mipmaps, resources);
		}

		buffer.position(header.headerSize);
		BufferedImage thumbnail = lowResFormat.read(header.lowResImageWidth, header.lowResImageHeight, buffer);

		List<VTFMipmap> mipmaps = readMipmaps(buffer, header, highResFormat);
		return new VTFFile(header, thumbnail, mipmaps, resources);
	}

	public static void write(VTFFile file, WritableByteChannel channel) throws IOException {
		Header header = file.getHeader();
		int version = header.versionMajor * 10 + header.versionMinor;

		//7.3+ supports resources, < 7.2 does not.
		List<VTFResource> resources = file.getResources();

		if (resources.size() > 0 && version < 73) {
			header.versionMajor = 7;
			header.versionMinor = 3;
			version = 73;
		}

		ImageDataFormat lowResFormat = header.lowResImageFormat;
		ImageDataFormat highResFormat = header.highResImageFormat;

		byte[] lowResImageData = lowResFormat.write(file.getThumbnail());

		if (version >= 73 && resources.isEmpty()) {
			int lowResOffset = header.headerSize + 16;
			int highResOffset = lowResOffset + lowResImageData.length;
			resources.add(new VTFResource(new byte[]{0x01, 0, 0}, (byte) 0, lowResOffset));
			resources.add(new VTFResource(new byte[]{0x30, 0, 0}, (byte) 0, highResOffset));
		}

		header.headerSize = 80 + (header.resourceCount * 8);

		ByteBuffer headerBuffer = ByteBuffer.allocate(header.headerSize);
		headerBuffer.order(ByteOrder.LITTLE_ENDIAN);

		headerBuffer.put(new byte[]{'V', 'T', 'F', 0});
		headerBuffer.putInt(header.versionMajor);
		headerBuffer.putInt(header.versionMinor);
		headerBuffer.putInt(header.headerSize);
		headerBuffer.putShort(header.width);
		headerBuffer.putShort(header.height);
		headerBuffer.putInt(header.flags);
		headerBuffer.putShort(header.frameCount);
		headerBuffer.putShort(header.firstFrame);
		headerBuffer.put(header.padding0);
		headerBuffer.putFloat(header.reflectivity[0]);
		headerBuffer.putFloat(header.reflectivity[1]);
		headerBuffer.putFloat(header.reflectivity[2]);
		headerBuffer.put(header.padding1);
		headerBuffer.putFloat(header.bumpmapScale);
		headerBuffer.putInt(header.highResImageFormat.ordinal());
		headerBuffer.put(header.mipmapCount);
		headerBuffer.putInt(header.lowResImageFormat.ordinal());
		headerBuffer.put(header.lowResImageWidth);
		headerBuffer.put(header.lowResImageHeight);

		if (version >= 72) {
			headerBuffer.putShort(header.depth);
		}

		if (version >= 73) {
			headerBuffer.put(header.padding2);
			headerBuffer.putInt(header.resourceCount);
			headerBuffer.put(header.padding3);

			for (VTFResource resource : resources) {
				headerBuffer.put(resource.tag());
				headerBuffer.put(resource.flags());
				headerBuffer.putInt(resource.offset());
			}
		}

		channel.write(headerBuffer.clear());
		channel.write(ByteBuffer.wrap(lowResImageData));

		List<VTFMipmap> mipmaps = file.getMipmaps();
		for (int i = mipmaps.size() - 1; i >= 0; i--) {
			VTFMipmap mipmap = mipmaps.get(i);
			for (VTFFrame frame : mipmap.frames()) {
				for (VTFFace face : frame.faces()) {
					channel.write(ByteBuffer.wrap(highResFormat.write(face.image())));
				}
			}
		}
	}

	

	private static List<VTFMipmap> readMipmaps(ByteBuffer data, VTFFile.Header header, ImageDataFormat format) {
		List<VTFMipmap> mipmaps = new ArrayList<>();

		int mipmapCount = header.mipmapCount;
		int frameCount = header.frameCount;
		int faceCount = 1;

		//TODO
//		if (instance.isFlagSet(TextureFlags.ENVMAP)) {
//			faceCount = 6;
//		}

		for (int mipmapIndex = mipmapCount; mipmapIndex >= 0; mipmapIndex--) {
			var width = (int) header.width >> mipmapIndex;
			var height = (int) header.height >> mipmapIndex;

			if (width <= 0 || height <= 0) {
				continue;
			}

			VTFMipmap mipmap = new VTFMipmap(mipmapIndex, width, height, new ArrayList<>());
			mipmaps.add(0, mipmap);

			for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
				VTFFrame frame = new VTFFrame(frameIndex, new ArrayList<>());
				mipmap.frames().add(frame);

				for (int faceIndex = 0; faceIndex < faceCount; faceIndex++) {
					BufferedImage image = format.read(width, height, data);
					VTFFace face = new VTFFace(faceIndex, image);
					frame.faces().add(face);
				}
			}
		}

		return mipmaps;
	}
}
