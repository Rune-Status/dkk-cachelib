package alex.util;

import alex.cache.Cache;
import alex.io.Stream;

public class UpdateServer {
	
	public static byte[] getContainerPacketData(Cache cache, int indexFileId, int containerId) {
		if(indexFileId != 255 && (cache.getCacheFileManagers().length <= containerId || cache.getCacheFileManagers()[containerId] == null))
			return null;
		byte[] container = indexFileId == 255 ? cache.getConstainersInformCacheFile().getContainerData(containerId) : cache.getCacheFileManagers()[indexFileId].getCacheFile().getContainerData(containerId);
		if(container == null)
			return null;
		Stream stream = new Stream(container.length+3);
		stream.putByte(indexFileId);
		stream.putShort(containerId);
		for(int index = 0; index < container.length; index++)
			stream.putByte(container[index]);
		byte[] packet = new byte[stream.offset];
		stream.offset = 0;
		stream.getBytes(packet, 0, packet.length);
		return packet;
	}
	
	/*
	* only using for ukeys atm, doesnt allow keys encode
	*/
	public static byte[] getContainerPacketData(int indexFileId, int containerId, int compression, byte[] unpackedContainer) {
		Stream stream = new Stream(unpackedContainer.length+4);
		stream.putByte(indexFileId);
		stream.putShort(containerId);
		byte[] compressedData = Utils.packContainer(unpackedContainer, compression);
		for(int index = 0; index < compressedData.length; index++)
			stream.putByte(compressedData[index]);
		byte[] packet = new byte[stream.offset];
		stream.offset = 0;
		stream.getBytes(packet, 0, packet.length);
		return packet;
	}

}
