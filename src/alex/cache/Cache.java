package alex.cache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.HashMap;

import alex.io.Stream;
import alex.util.UpdateServer;

public class Cache {

	private CacheFileManager[] cacheFileManagers;
	private CacheFile containersInformCacheFile;
	
	public CacheFileManager[] getCacheFileManagers() {
		return cacheFileManagers;
	}
	public Cache(String path) {
		HashMap<Integer, RandomAccessFile> indexesFiles = new HashMap <Integer, RandomAccessFile>();
		RandomAccessFile containersInformFile = null;
		RandomAccessFile dataFile = null;
		File[] files = new File(path).listFiles();
		int lastIndexFileId = -1;
		for (File file : files) {
			if (file.length() == 0)
				continue;
			if (file.getName().startsWith("main_file_cache.idx")) {
				int id = Integer.parseInt(file.getName().split(".idx")[1]);
				if (id != 255) {

					try {
						indexesFiles.put(id, new RandomAccessFile(file, "rw"));
						if(id > lastIndexFileId)
							lastIndexFileId = id;
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				} else
					try {
						containersInformFile = new RandomAccessFile(file, "rw");
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					
			} else if (file.getName().equals("main_file_cache.dat2"))
				try {
					dataFile = new RandomAccessFile(file, "rw");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
		}
		if(lastIndexFileId == -1 || containersInformFile ==  null || dataFile == null)
			throw new Error("Missing one or more cache files.");
		byte[] cacheFileBuffer = new byte[520];
		containersInformCacheFile = new CacheFile(255, containersInformFile, dataFile, 500000, cacheFileBuffer);
		cacheFileManagers = new CacheFileManager[lastIndexFileId+1];
		for(int indexFileId : indexesFiles.keySet()) {
			cacheFileManagers[indexFileId] = new CacheFileManager(this, new CacheFile(indexFileId, indexesFiles.get(indexFileId), dataFile, 1000000, cacheFileBuffer), true);
		}
	}
	
	public byte[] generateUkeys() {
		return UpdateServer.getContainerPacketData(255, 255, 0, generateUkeysContainer());
	}
	
	public byte[] generateUkeysContainer() {
		Stream stream = new Stream(cacheFileManagers.length * 8);
		for(int index = 0; index < cacheFileManagers.length; index++) {
			if(cacheFileManagers[index] == null) {
				stream.putInt(0);
				stream.putInt(0);
				continue;
			}
			stream.putInt(cacheFileManagers[index].getInformation().getInformationContainer().getCrc());
			stream.putInt(cacheFileManagers[index].getInformation().getRevision());
		}
		byte[] ukeysContainer = new byte[stream.offset];
		stream.offset = 0;
		stream.getBytes(ukeysContainer, 0, ukeysContainer.length);
		return ukeysContainer;
	}
	
	public CacheFile getConstainersInformCacheFile() {
		return containersInformCacheFile;
	}
}
