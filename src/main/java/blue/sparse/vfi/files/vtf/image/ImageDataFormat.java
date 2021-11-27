package blue.sparse.vfi.files.vtf.image;

import blue.sparse.vfi.files.vtf.image.impl.*;
import blue.sparse.vfi.files.vtf.image.impl.dxt.*;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public enum ImageDataFormat implements ImageFormatReader, ImageFormatWriter {
	UNKNOWN(new ImageReaderUnknown(), null),
	RGBA8888(new ImageReaderWriterRGBA8888()),
	ABGR8888(new ImageReaderWriterABGR8888()),
	RGB888(new ImageReaderWriterRGB888()),
	BGR888(new ImageReaderWriterBGR888()),
	RGB565,
	I8,
	IA88,
	P8,
	A8,
	RGB888_BLUESCREEN,
	BGR888_BLUESCREEN,
	ARGB8888,
	BGRA8888(new ImageReaderWriterBGRA8888()),
	DXT1(new ImageReaderDXT1(), new ImageWriterDXT1()),
	DXT3(new ImageReaderDXT3(), null),
	DXT5(new ImageReaderDXT5(), null),
	BGRX8888,
	BGR565(new ImageReaderWriterBGR565()),
	BGRX5551,
	BGRA4444,
	DXT1_ONEBITALPHA(new ImageReaderDXT1BitAlpha(), new ImageWriterDXT1BitAlpha()),
	BGRA5551,
	UV88(new ImageReaderWriterUV88()),
	UVWQ8888,
	RGBA16161616F,
	RGBA16161616,
	UVLX8888;

	private final ImageFormatReader reader;
	private final ImageFormatWriter writer;

	ImageDataFormat(ImageFormatReader reader, ImageFormatWriter writer) {
		this.reader = reader;
		this.writer = writer;
	}

	<T extends ImageFormatReader & ImageFormatWriter> ImageDataFormat(T readerWriter) {
		this.reader = readerWriter;
		this.writer = readerWriter;
	}

	ImageDataFormat() {
		this(null, null);
	}

	public ImageFormatReader getReader() {
		return reader;
	}

	public ImageFormatWriter getWriter() {
		return writer;
	}

	@Override
	public BufferedImage read(int width, int height, ByteBuffer buffer) {
		return getReader().read(width, height, buffer);
	}

	@Override
	public byte[] write(BufferedImage image) {
		return getWriter().write(image);
	}

	public int getIndex() {
		return ordinal() - 1;
	}

	public static ImageDataFormat get(int index) {
		return values()[index + 1];
	}

}
