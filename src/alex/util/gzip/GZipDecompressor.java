package alex.util.gzip;

import java.util.zip.Inflater;

import alex.io.Stream;

public class GZipDecompressor {

	private static final Inflater inflaterInstance = new Inflater(true);
	
	public static final void decompress(Stream stream, byte data[]) {
		if (~stream.payload[stream.offset] != -32 || stream.payload[stream.offset + 1] != -117)
			throw new RuntimeException("Invalid GZIP header!");
		try {
			inflaterInstance.setInput(stream.payload, stream.offset + 10, -stream.offset - 18 + stream.payload.length);
			inflaterInstance.inflate(data);
		} catch (Exception e) {
			inflaterInstance.reset();
			throw new RuntimeException("Invalid GZIP compressed data!");
		}
		inflaterInstance.reset();
	}
	
}
