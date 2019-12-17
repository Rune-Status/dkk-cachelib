package alex.util;

import java.util.zip.CRC32;

import alex.cache.Cache;
import alex.cache.Container;
import alex.io.Stream;
import alex.util.bzip2.BZip2Decompressor;
import alex.util.gzip.GZipCompressor;
import alex.util.gzip.GZipDecompressor;

public class Utils {

	
	public static final CRC32 CRC32Instance = new CRC32();
	
	public static char aCharArray5916[] = { '\u20AC', '\0', '\u201A', '\u0192',
		'\u201E', '\u2026', '\u2020', '\u2021', '\u02C6', '\u2030',
		'\u0160', '\u2039', '\u0152', '\0', '\u017D', '\0', '\0', '\u2018',
		'\u2019', '\u201C', '\u201D', '\u2022', '\u2013', '\u2014',
		'\u02DC', '\u2122', '\u0161', '\u203A', '\u0153', '\0', '\u017E',
		'\u0178' };
	
	public static final Container[] copyArray(Container[] array) {
		Container[] copyArray = new Container[array.length];
		System.arraycopy(array, 0, copyArray, 0, array.length);
		return copyArray;
	}
	
	public static final int[] copyArray(int array[]) {
		int copyArray[] = new int[array.length];
		System.arraycopy(array, 0, copyArray, 0, array.length);
		return copyArray;
	}
	
	public static final int getAmountOfItems(Cache cache) {
			int lastContainerId = cache.getCacheFileManagers()[19].getContainersSize() -1;
			return (256 * lastContainerId) +cache.getCacheFileManagers()[19].getFilesSize(lastContainerId);
	}
	
	public static final int getNameHash(String name) {
		name = name.toLowerCase();
		int hash = 0;
		for (int index = 0; index < name.length(); index++)
			hash = method1258(name.charAt(index)) + ((hash << 5) - hash);
		return hash;
	}

	static final byte method1258(char c) {
		byte byte0;
		if (c > 0 && c < '\200' || c >= '\240' && c <= '\377') {
			byte0 = (byte) c;
		} else if (c != '\u20AC') {
			if (c != '\u201A') {
				if (c != '\u0192') {
					if (c == '\u201E') {
						byte0 = -124;
					} else if (c != '\u2026') {
						if (c != '\u2020') {
							if (c == '\u2021') {
								byte0 = -121;
							} else if (c == '\u02C6') {
								byte0 = -120;
							} else if (c == '\u2030') {
								byte0 = -119;
							} else if (c == '\u0160') {
								byte0 = -118;
							} else if (c == '\u2039') {
								byte0 = -117;
							} else if (c == '\u0152') {
								byte0 = -116;
							} else if (c != '\u017D') {
								if (c == '\u2018') {
									byte0 = -111;
								} else if (c != '\u2019') {
									if (c != '\u201C') {
										if (c == '\u201D') {
											byte0 = -108;
										} else if (c != '\u2022') {
											if (c == '\u2013') {
												byte0 = -106;
											} else if (c == '\u2014') {
												byte0 = -105;
											} else if (c == '\u02DC') {
												byte0 = -104;
											} else if (c == '\u2122') {
												byte0 = -103;
											} else if (c != '\u0161') {
												if (c == '\u203A') {
													byte0 = -101;
												} else if (c != '\u0153') {
													if (c == '\u017E') {
														byte0 = -98;
													} else if (c != '\u0178') {
														byte0 = 63;
													} else {
														byte0 = -97;
													}
												} else {
													byte0 = -100;
												}
											} else {
												byte0 = -102;
											}
										} else {
											byte0 = -107;
										}
									} else {
										byte0 = -109;
									}
								} else {
									byte0 = -110;
								}
							} else {
								byte0 = -114;
							}
						} else {
							byte0 = -122;
						}
					} else {
						byte0 = -123;
					}
				} else {
					byte0 = -125;
				}
			} else {
				byte0 = -126;
			}
		} else {
			byte0 = -128;
		}
		return byte0;
	}
	
	public static final int getStringBytes(String s, int strOff, int strLen,
			byte buffer[], int bufOff) {
		int l = -strOff + strLen;
		for (int i1 = 0; i1 < l; i1++) {
			char c = s.charAt(strOff + i1);
			if (c > '\0' && c < '\200' || c >= '\240' && c <= '\377') {
				buffer[i1 + bufOff] = (byte) c;
			} else if (c == '\u20AC') {
				buffer[i1 + bufOff] = -128;
			} else if (c != '\u201A') {
				if (c == '\u0192') {
					buffer[bufOff + i1] = -125;
				} else if (c != '\u201E') {
					if (c == '\u2026') {
						buffer[bufOff + i1] = -123;
					} else if (c != '\u2020') {
						if (c == '\u2021') {
							buffer[bufOff + i1] = -121;
						} else if (c != '\u02C6') {
							if (c == '\u2030') {
								buffer[i1 + bufOff] = -119;
							} else if (c == '\u0160') {
								buffer[bufOff + i1] = -118;
							} else if (c == '\u2039') {
								buffer[i1 + bufOff] = -117;
							} else if (c == '\u0152') {
								buffer[i1 + bufOff] = -116;
							} else if (c == '\u017D') {
								buffer[i1 + bufOff] = -114;
							} else if (c == '\u2018') {
								buffer[i1 + bufOff] = -111;
							} else if (c == '\u2019') {
								buffer[i1 + bufOff] = -110;
							} else if (c == '\u201C') {
								buffer[bufOff + i1] = -109;
							} else if (c == '\u201D') {
								buffer[i1 + bufOff] = -108;
							} else if (c == '\u2022') {
								buffer[i1 + bufOff] = -107;
							} else if (c != '\u2013') {
								if (c == '\u2014') {
									buffer[i1 + bufOff] = -105;
								} else if (c != '\u02DC') {
									if (c != '\u2122') {
										if (c == '\u0161') {
											buffer[i1 + bufOff] = -102;
										} else if (c == '\u203A') {
											buffer[bufOff + i1] = -101;
										} else if (c == '\u0153') {
											buffer[bufOff + i1] = -100;
										} else if (c == '\u017E') {
											buffer[bufOff + i1] = -98;
										} else if (c == '\u0178') {
											buffer[i1 + bufOff] = -97;
										} else {
											buffer[i1 + bufOff] = 63;
										}
									} else {
										buffer[bufOff + i1] = -103;
									}
								} else {
									buffer[i1 + bufOff] = -104;
								}
							} else {
								buffer[i1 + bufOff] = -106;
							}
						} else {
							buffer[i1 + bufOff] = -120;
						}
					} else {
						buffer[bufOff + i1] = -122;
					}
				} else {
					buffer[i1 + bufOff] = -124;
				}
			} else {
				buffer[bufOff + i1] = -126;
			}
		}
		return l;
	}
	
	public static final String getStringFromBytes(byte buffer[], int off,
			int len) {
		char ac[] = new char[len];
		int l = 0;
		for (int i1 = 0; len > i1; i1++) {
			int j1 = 0xff & buffer[off + i1];
			if (j1 != 0) {
				if (j1 >= 128 && j1 < 160) {
					char c = aCharArray5916[-128 + j1];
					if (c == 0) {
						c = '?';
					}
					j1 = c;
				}
				ac[l++] = (char) j1;
			}
		}
		return new String(ac, 0, l);
	}
	
	
	public static final byte[] packContainer(byte[] unpackedData, int compression) {
		Stream stream = new Stream(unpackedData.length+100);
		stream.putByte(compression);
		byte[] compressedData = null;
		if(compression == 0)
			compressedData = unpackedData;
		if (compression == 1)
			compressedData = null;
		else
			compressedData = GZipCompressor.compress(unpackedData);
		stream.putInt(compressedData.length);
		if(compression > 0)
			stream.putInt(unpackedData.length);
		for(int index = 0; index < compressedData.length; index++)
			stream.putByte(compressedData[index]);
		byte[] packedData = new byte[stream.offset];
		stream.offset = 0;
		stream.getBytes(packedData, 0, packedData.length);
		return packedData;
	}
	public static final byte[] unpackContainer(byte[] packedData) {
		Stream stream = new Stream(packedData);
		int compression = stream.getUByte();
		int containerSize = stream.getInt();
		if (containerSize < 0)
			throw new RuntimeException();
		if (compression == 0) {
			byte unpacked[] = new byte[containerSize];
			stream.getBytes(unpacked, 0, containerSize);
			return unpacked;
		}
		int decompressedSize = stream.getInt();
		if (decompressedSize < 0)
			throw new RuntimeException();
		byte decompressedData[] = new byte[decompressedSize];
		if(compression == 1) {
			BZip2Decompressor.decompress(decompressedData, packedData, containerSize, 9);
		}else{
			GZipDecompressor.decompress(stream, decompressedData);
		}
		return decompressedData;
		
	}
	
}
