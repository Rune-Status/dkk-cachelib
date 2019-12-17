package alex.cache;

import alex.io.Stream;
import alex.util.Utils;

public class CacheFileManager {

	private Cache cache;
	private CacheFile cacheFile;
	private ContainersInformation information;
	private boolean discardFilesData;
	
	private byte[][][] filesData;
	
	public CacheFileManager(Cache cache, CacheFile cacheFile, boolean discardFilesData) {
		this.cache = cache;
		this.cacheFile = cacheFile;
		this.discardFilesData = discardFilesData;
		byte[] informContainerPackedData = cache.getConstainersInformCacheFile().getContainerData(cacheFile.getIndexFileId());
		if(informContainerPackedData == null) {
			cache.getCacheFileManagers()[cacheFile.getIndexFileId()] = null;
			return;
		}
		information = new ContainersInformation(informContainerPackedData);
		resetFilesData();
	}
	
	public CacheFile getCacheFile() {
		return cacheFile;
	}
	
	public int getContainersSize() {
		return information.getContainers().length;
	}
	
	public int getFilesSize(int containerId) {
		if(!validContainer(containerId))
			return -1;
		return information.getContainers()[containerId].getFiles().length;
	}
	public void resetFilesData() {
		filesData = new byte[information.getContainers().length][][];
	}
	
	
	public boolean validFile(int containerId, int fileId) {
		if(!validContainer(containerId))
			return false;
		if(fileId < 0 || information.getContainers()[containerId].getFiles().length <= fileId)
			return false;
		return true;
		
	}
	
	public boolean validContainer(int containerId) {
		if(containerId < 0 || information.getContainers().length <= containerId)
			return false;
		return true;
	}
	
	public byte[] getFileData(int containerId, int fileId) {
		return getFileData(containerId, fileId, null);
	}
	
	
	
	public boolean putAllContainers(Cache sourceCache) {
		if(sourceCache.getCacheFileManagers().length <= cacheFile.getIndexFileId() || sourceCache.getCacheFileManagers()[cacheFile.getIndexFileId()] == null)
			return false;
		for(int containerIndex = 0; containerIndex < sourceCache.getCacheFileManagers()[cacheFile.getIndexFileId()].getInformation().getContainersIndexes().length; containerIndex++) {
			if(!putContainer(sourceCache.getCacheFileManagers()[cacheFile.getIndexFileId()].getInformation().getContainersIndexes()[containerIndex], sourceCache))
				return false;
		}
		return true;
	}
	
	public boolean putContainer(int containerId, Cache sourceCache) {
		if(sourceCache.getCacheFileManagers().length <= cacheFile.getIndexFileId() || sourceCache.getCacheFileManagers()[cacheFile.getIndexFileId()] == null)
			return false;
		byte[] packedContainer = sourceCache.getCacheFileManagers()[cacheFile.getIndexFileId()].getCacheFile().getContainerData(containerId);
		if(packedContainer == null)
			return false;
		information.addContainer(containerId);
		information.getContainers()[containerId].setFiles(Utils.copyArray(sourceCache.getCacheFileManagers()[cacheFile.getIndexFileId()].getInformation().getContainers()[containerId].getFiles()));
		information.getContainers()[containerId].setFilesIndexes(Utils.copyArray(sourceCache.getCacheFileManagers()[cacheFile.getIndexFileId()].getInformation().getContainers()[containerId].getFilesIndexes()));
		information.getContainers()[containerId].setNameHash(sourceCache.getCacheFileManagers()[cacheFile.getIndexFileId()].getInformation().getContainers()[containerId].getNameHash());
		if(!cacheFile.putContainerData(containerId, packedContainer))
			return false;
		Utils.CRC32Instance.reset();
		Utils.CRC32Instance.update(packedContainer, 0, packedContainer.length-2);
		information.getContainers()[containerId].setCrc((int) Utils.CRC32Instance.getValue());
		if(!information.getContainers()[containerId].isUpdated())
			information.getContainers()[containerId].updateVersion();
		if(!information.isUpdated())
			information.updateRevision();
		if(!information.getInformationContainer().isUpdated())
			information.getInformationContainer().updateVersion();
		byte[] informationContainer = information.encodeContainersInformation();
		if(cache.getConstainersInformCacheFile().putContainerUnpackedData(cacheFile.getIndexFileId(), 2, informationContainer, information.getInformationContainer().getVersion())) {
			Utils.CRC32Instance.reset();
			Utils.CRC32Instance.update(informationContainer, 0, informationContainer.length);
			information.getInformationContainer().setCrc((int) Utils.CRC32Instance.getValue());
			resetFilesData();
			return true;
		}
		return false;
	}
	
	public boolean putFileData(int containerId, int fileId, byte[] data) {
		return putFileData(containerId, fileId, 2, data);
	}
	public boolean putFileData(int containerId, int fileId, int compression, byte[] data) {
		return putFileData(containerId, fileId, compression, data, null);
	}
	
	public boolean putFileData(int containerId, int fileId, int compression, byte[] data, int[] container_keys) {
		return putFileData(containerId, fileId, compression, data, container_keys, null, null);
	}
	
	public boolean putFileData(int containerId, int fileId, int compression, byte[] data, int[] container_keys, String containerName, String fileName) {
		int oldFileIndexesLength = 0;
		if(validContainer(containerId) && information.getContainers()[containerId].getFilesIndexes() != null)
			oldFileIndexesLength = information.getContainers()[containerId].getFilesIndexes().length;
		int containerIndex = information.addContainer(containerId);
		if(containerIndex == -1)
			return false;
		int fileIndex = information.addFile(containerId, fileId);
		if(fileIndex == -1)
			return false;
		byte[] containerData;
		if (information.getContainers()[containerId].getFilesIndexes().length == 1) {
			containerData = data;
		}else{
			int filesSize[] = new int[information.getContainers()[containerId].getFilesIndexes().length];
			byte[][] filesBufferData = new byte[information.getContainers()[containerId].getFilesIndexes().length][];
			if(oldFileIndexesLength > 0) {
				byte[] oldContainer = cacheFile.getContainerUnpackedData(containerId, container_keys);
				if(oldFileIndexesLength == 1) {
					filesSize[0] = oldContainer.length;
					filesBufferData[0] = oldContainer;
				}else{
					int readPosition = oldContainer.length;
					int amtOfLoops = oldContainer[--readPosition] & 0xff;
					readPosition -= amtOfLoops * (oldFileIndexesLength * 4);
					Stream stream = new Stream(oldContainer);
					stream.offset = readPosition;
					for (int loop = 0; loop < amtOfLoops; loop++) {
						int offset = 0;
						for (int thisFileIndex = 0; thisFileIndex < oldFileIndexesLength; thisFileIndex++)
							filesSize[thisFileIndex] += offset += stream.getInt();
					}
					for (int thisFileIndex = 0; thisFileIndex < oldFileIndexesLength; thisFileIndex++) {
						filesBufferData[thisFileIndex] = new byte[filesSize[thisFileIndex]];
						filesSize[thisFileIndex] = 0;
					}
					stream.offset = readPosition;
					int sourceOffset = 0;
					for (int loop = 0; loop < amtOfLoops; loop++) {
						int dataRead = 0;
						for (int thisFileIndex = 0; thisFileIndex < oldFileIndexesLength; thisFileIndex++) {
							dataRead += stream.getInt();
							System.arraycopy(oldContainer, sourceOffset, filesBufferData[thisFileIndex], filesSize[thisFileIndex],dataRead);
							sourceOffset += dataRead;
							filesSize[thisFileIndex] += dataRead;
						}
					}	
				}
			}
			filesSize[fileIndex] = data.length;
			filesBufferData[fileIndex] = data;
			Stream stream = new Stream(1000000);
			for (int thisFileIndex = 0; thisFileIndex < information.getContainers()[containerId].getFilesIndexes().length; thisFileIndex++)
				for(int index = 0; index < filesBufferData[thisFileIndex].length; index++)
					stream.putByte(filesBufferData[thisFileIndex][index]);
			for (int thisFileIndex = 0; thisFileIndex < information.getContainers()[containerId].getFilesIndexes().length; thisFileIndex++)
				stream.putInt(filesSize[thisFileIndex] - (thisFileIndex == 0 ? 0 : filesSize[thisFileIndex-1]));
			stream.putByte(1);
			containerData = new byte[stream.offset];
			stream.offset = 0;
			stream.getBytes(containerData, 0, containerData.length);
		}
		int version = information.getContainers()[containerId].getNextVersion();
		if(!cacheFile.putContainerUnpackedData(containerId, compression, containerData, version, container_keys))
			return false;
		byte[] packedContainer = cacheFile.getContainerData(containerId);
		if(packedContainer == null)
			return false;
		Utils.CRC32Instance.reset();
		Utils.CRC32Instance.update(packedContainer, 0, packedContainer.length-2);
		information.getContainers()[containerId].setCrc((int) Utils.CRC32Instance.getValue());
		if(containerName != null)
			information.getContainers()[containerId].setNameHash(Utils.getNameHash(containerName));
		if(fileName != null)
			information.getContainers()[containerId].getFiles()[fileId].setNameHash(Utils.getNameHash(fileName));
		if(!information.getContainers()[containerId].isUpdated())
			information.getContainers()[containerId].updateVersion();
		if(!information.isUpdated())
			information.updateRevision();
		if(!information.getInformationContainer().isUpdated())
			information.getInformationContainer().updateVersion();
		byte[] informationContainer = information.encodeContainersInformation();
		if(cache.getConstainersInformCacheFile().putContainerUnpackedData(cacheFile.getIndexFileId(), 2, informationContainer, information.getInformationContainer().getVersion())) {
			Utils.CRC32Instance.reset();
			Utils.CRC32Instance.update(informationContainer, 0, informationContainer.length);
			information.getInformationContainer().setCrc((int) Utils.CRC32Instance.getValue());
			resetFilesData();
			return true;
		}
		return false;
	}
	
	public boolean loadFilesData(int containerId, int[] container_keys) {
		byte[] data = cacheFile.getContainerUnpackedData(containerId, container_keys);
		if(data == null)
			return false;
		if(filesData[containerId] == null) {
			if(information.getContainers()[containerId] == null) 
				return false; //container inform doesnt exist anymore
			filesData[containerId] = new byte[information.getContainers()[containerId].getFiles().length][];
		}
		if (information.getContainers()[containerId].getFilesIndexes().length == 1) {
			int fileId = information.getContainers()[containerId].getFilesIndexes()[0];
			filesData[containerId][fileId] = data;
		}else{
			int readPosition = data.length;
			int amtOfLoops = data[--readPosition] & 0xff;
			readPosition -= amtOfLoops * (information.getContainers()[containerId].getFilesIndexes().length * 4);
			Stream stream = new Stream(data);
			int filesSize[] = new int[information.getContainers()[containerId].getFilesIndexes().length];
			stream.offset = readPosition;
			for (int loop = 0; loop < amtOfLoops; loop++) {
				int offset = 0;
				for (int fileIndex = 0; fileIndex < information.getContainers()[containerId].getFilesIndexes().length; fileIndex++)
					filesSize[fileIndex] += offset += stream.getInt();
			}
			byte[][] filesBufferData = new byte[information.getContainers()[containerId].getFilesIndexes().length][];
			for (int fileIndex = 0; fileIndex < information.getContainers()[containerId].getFilesIndexes().length; fileIndex++) {
				filesBufferData[fileIndex] = new byte[filesSize[fileIndex]];
				filesSize[fileIndex] = 0;
			}
			stream.offset = readPosition;
			int sourceOffset = 0;
			for (int loop = 0; loop < amtOfLoops; loop++) {
				int dataRead = 0;
				for (int fileIndex = 0; fileIndex < information.getContainers()[containerId].getFilesIndexes().length; fileIndex++) {
					dataRead += stream.getInt();
					System.arraycopy(data, sourceOffset, filesBufferData[fileIndex], filesSize[fileIndex],dataRead);
					sourceOffset += dataRead;
					filesSize[fileIndex] += dataRead;
				}
			}
			for (int fileIndex = 0; fileIndex < information.getContainers()[containerId].getFilesIndexes().length; fileIndex++)
				filesData[containerId][information.getContainers()[containerId].getFilesIndexes()[fileIndex]] = filesBufferData[fileIndex];
		}
		return true;
		
	}
	
	public byte[] getFileData(int containerId, int fileId, int[] container_keys) {
		if(!validFile(containerId, fileId))
			return null;
		if(filesData[containerId] == null || filesData[containerId][fileId] == null) {
			if(!loadFilesData(containerId, container_keys))
				return null;
		}
		byte[] data = filesData[containerId][fileId];
		if(discardFilesData) {
			if(filesData[containerId].length == 1)
				filesData[containerId] = null;
			else
				filesData[containerId][fileId] = null;
			
		}
		return data;
		
	}
	
	
	public ContainersInformation getInformation() {
		return information;
	}
}
