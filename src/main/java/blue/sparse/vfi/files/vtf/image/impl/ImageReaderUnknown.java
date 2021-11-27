package blue.sparse.vfi.files.vtf.image.impl;

import blue.sparse.vfi.files.vtf.image.ImageFormatReader;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public final class ImageReaderUnknown implements ImageFormatReader {
	@Override
	public BufferedImage read(int width, int height, ByteBuffer buffer) {
		return null;
	}
}
