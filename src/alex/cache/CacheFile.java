package alex.cache;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

import alex.io.Stream;
import alex.util.Utils;

public class CacheFile {

	private int indexFileId;
	private byte[] cacheFileBuffer;
	private int maxContainerSize;
	private RandomAccessFile indexFile;
	private RandomAccessFile dataFile;
	
	public CacheFile(int indexFileId, RandomAccessFile indexFile, RandomAccessFile dataFile, int maxContainerSize, byte[] cacheFileBuffer) {
		this.cacheFileBuffer = cacheFileBuffer;
		this.indexFileId = indexFileId;
		this.maxContainerSize = maxContainerSize;
		this.indexFile = indexFile;
		this.dataFile = dataFile;
	}
	
	public int getIndexFileId() {
		return indexFileId;
	}
			
	public final byte[] getContainerUnpackedData(int containerId) {
		return getContainerUnpackedData(containerId, null);
	}
	
	
	
	public final byte[] getContainerUnpackedData(int containerId, int[] container_keys) {
		byte[] packedData = getContainerData(containerId);
		if(packedData == null)
			return null;
		if (container_keys != null && (container_keys[0] != 0 || container_keys[1] != 0 || container_keys[2] != 0 || container_keys[3] != 0)) {
			Stream stream = new Stream(packedData);
			stream.decodeXTEA(container_keys, 5, stream.payload.length);
		}
		return Utils.unpackContainer(packedData);
	}
	
	public final byte[] getContainerData(int containerId) {
		try {
			if (indexFile.length() < (6 * containerId + 6))
				return null;
			indexFile.seek(6 * containerId);
			indexFile.read(cacheFileBuffer, 0, 6);
			int containerSize = (cacheFileBuffer[2] & 0xff) + (((0xff & cacheFileBuffer[0]) << 16) + (cacheFileBuffer[1] << 8 & 0xff00));
			int sector = ((cacheFileBuffer[3] & 0xff) << 16)- (-(0xff00 & cacheFileBuffer[4] << 8) - (cacheFileBuffer[5] & 0xff));
			if (containerSize < 0 || containerSize > maxContainerSize)
				return null;
			if (sector <= 0 || dataFile.length() / 520L < sector)
				return null;
			byte data[] = new byte[containerSize];
			int dataReadCount = 0;
			int part = 0;
			while (containerSize > dataReadCount) {
				if (sector == 0)
					return null;
				dataFile.seek(520 * sector);
				int dataToReadCount = containerSize - dataReadCount;
				if (dataToReadCount > 512)
					dataToReadCount = 512;
				dataFile.read(cacheFileBuffer, 0, 8 + dataToReadCount);
				int currentContainerId = (0xff & cacheFileBuffer[1]) + (0xff00 & cacheFileBuffer[0] << 8);
				int currentPart = ((cacheFileBuffer[2] & 0xff) << 8) + (0xff & cacheFileBuffer[3]);
				int nextSector = (cacheFileBuffer[6] & 0xff) + (0xff00 & cacheFileBuffer[5] << 8) + ((0xff & cacheFileBuffer[4]) << 16);
				int currentIndexFileId = cacheFileBuffer[7] & 0xff;
				if (containerId != currentContainerId || currentPart != part|| indexFileId != currentIndexFileId)
					return null;
				if (nextSector < 0 || (dataFile.length() / 520L) < nextSector) {
					return null;
				}
				for (int index = 0; dataToReadCount > index; index++)
					data[dataReadCount++] = cacheFileBuffer[8 + index];
				part++;
				sector = nextSector;
			}
			return data;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public final boolean putContainerUnpackedData(int containerId, int compression, byte[] data, int version) {
		return putContainerUnpackedData(containerId, compression, data, version, null);
	}
	
	public final boolean putContainerUnpackedData(int containerId, int compression, byte[] data, int version, int[] container_keys) {
		byte[] packedData = Utils.packContainer(data, compression);
		if(packedData == null)
			return false;
		if (container_keys != null && (container_keys[0] != 0 || container_keys[1] != 0 || container_keys[2] != 0 || container_keys[3] != 0)) {
			Stream stream = new Stream(packedData);
			stream.encodeXTEA(container_keys);
		}
		packedData[packedData.length - 2] = (byte) (version >>> 8);
		packedData[packedData.length - 1] = (byte) version;
		return putContainerData(containerId, packedData);
	}
	
	public final boolean putContainerData(int containerId, byte[] data) {
		if (data.length > maxContainerSize)
			throw new IllegalArgumentException();
		boolean done = putContainerData(containerId, data, true);
		if(!done)
			done = putContainerData(containerId, data, false);
		return done;
		
	}
	
	private final boolean putContainerData(int containerId, byte[] data, boolean exists) {
		try {
			int sector;
			if (!exists) {
				sector = (int) ((dataFile.length() + 519L) / 520L);
				if (sector == 0)
					sector = 1;
			} else {
				if ((6 * containerId + 6) > indexFile.length())
					return false;
				indexFile.seek(containerId * 6);
				indexFile.read(cacheFileBuffer, 0, 6);
				sector = (cacheFileBuffer[5] & 0xff)+ (((cacheFileBuffer[4] & 0xff) << 8) + (cacheFileBuffer[3] << 16 & 0xff0000));
				if (sector <= 0 || sector > dataFile.length() / 520L)
					return false;
			}
			cacheFileBuffer[1] = (byte) (data.length >> 8);
			cacheFileBuffer[3] = (byte) (sector >> 16);
			cacheFileBuffer[2] = (byte) data.length;
			cacheFileBuffer[0] = (byte) (data.length >> 16);
			cacheFileBuffer[4] = (byte) (sector >> 8);
			cacheFileBuffer[5] = (byte) sector;
			indexFile.seek(containerId * 6);
			indexFile.write(cacheFileBuffer, 0, 6);
			int dataWritten = 0;
			for (int part = 0; dataWritten < data.length; part++) {
				int nextSector = 0;
				if (exists) {
					dataFile.seek(sector * 520);
					try {
						dataFile.read(cacheFileBuffer, 0, 8);
					} catch (EOFException e) {
						e.printStackTrace();
						break;
					}
					int currentContainerId = (0xff & cacheFileBuffer[1]) + (0xff00 & cacheFileBuffer[0] << 8);
					int currentPart = (0xff & cacheFileBuffer[3]) + (0xff00 & cacheFileBuffer[2] << 8);
					nextSector = ((0xff & cacheFileBuffer[4]) << 16) + (((0xff & cacheFileBuffer[5]) << 8) + (0xff & cacheFileBuffer[6]));
					int currentIndexFileId = cacheFileBuffer[7] & 0xff;
					if (currentContainerId != containerId || part != currentPart || indexFileId  != currentIndexFileId)
						return false;
					if (nextSector < 0 || dataFile.length() / 520L < nextSector)
						return false;
				}
				if (nextSector == 0) {
					exists = false;
					nextSector = (int) ((dataFile.length() + 519L) / 520L);
					if (nextSector == 0)
						nextSector++;
					if (nextSector == sector)
						nextSector++;
				}
				cacheFileBuffer[3] = (byte) part;
				if (data.length - dataWritten <= 512)
					nextSector = 0;
				cacheFileBuffer[0] = (byte) (containerId >> 8);
				cacheFileBuffer[1] = (byte) containerId;
				cacheFileBuffer[2] = (byte) (part >> 8);
				cacheFileBuffer[7] = (byte) indexFileId;
				cacheFileBuffer[4] = (byte) (nextSector >> 16);
				cacheFileBuffer[5] = (byte) (nextSector >> 8);
				cacheFileBuffer[6] = (byte) nextSector;
				dataFile.seek(sector * 520);
				dataFile.write(cacheFileBuffer, 0, 8);
				int dataToWrite = data.length - dataWritten;
				if (dataToWrite > 512)
					dataToWrite = 512;
				dataFile.write(data, dataWritten, dataToWrite);
				dataWritten += dataToWrite;
				sector = nextSector;
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
		
	}
	
}

