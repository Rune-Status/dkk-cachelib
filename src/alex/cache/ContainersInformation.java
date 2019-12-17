package alex.cache;

import alex.io.Stream;
import alex.util.Utils;

public class ContainersInformation {

	private Container informationContainer;
	private int protocol;
	private int revision;
	private int[] containersIndexes;
	private FilesContainer[] containers;
	private boolean updated;
	private boolean containersAndFilesNamed;
	private boolean filesNamed;
	
	public ContainersInformation(byte[] informationContainerPackedData) {
		informationContainer = new Container();
		informationContainer.setVersion((informationContainerPackedData[informationContainerPackedData.length - 2] << 8 & 0xff00) + (informationContainerPackedData[-1 + informationContainerPackedData.length] & 0xff));
		Utils.CRC32Instance.reset();
		Utils.CRC32Instance.update(informationContainerPackedData);
		informationContainer.setCrc((int) Utils.CRC32Instance.getValue());
		decodeContainersInformation(Utils.unpackContainer(informationContainerPackedData));
	}
	
	public int[] getContainersIndexes() {
		return containersIndexes;
	}
	
	public FilesContainer[] getContainers() {
		return containers;
	}
	public Container getInformationContainer() {
		return informationContainer;
	}
	
	public int getRevision() {
		return revision;
	}
	
	public void updateRevision() {
		revision++;
		updated = true;
	}
	
	public int addContainer(int containerId) {
		if(containerId < 0)
			return -1;
		int containerIndex = -1;
		for (int index = 0; index < containersIndexes.length; index++) {
			if(containersIndexes[index] == containerId) {
				containerIndex = index;
				break;
			}
		}
		if(containerIndex == -1) {
			containerIndex = containersIndexes.length;
			int[] newContainersIndexes = new int[containerIndex+1];
			System.arraycopy(containersIndexes, 0, newContainersIndexes, 0, containerIndex);
			newContainersIndexes[containerIndex] = containerId;
			containersIndexes = newContainersIndexes;
		}
		if(containers.length <= containerId) {
			FilesContainer[] newContainers = new FilesContainer[containerId+1];
			System.arraycopy(containers, 0, newContainers, 0, containers.length);
			containers = newContainers;
		}
		if(containers[containerId] == null)
			containers[containerId] = new FilesContainer();
		return containerIndex;
	}
	
	public int addFile(int containerId, int fileId) {
		if(containerId < 0 || containers[containerId] == null)
			return -1;
		int fileIndex = -1;
		if(containers[containerId].getFilesIndexes() == null) {
			containers[containerId].setFilesIndexes(new int[1]);
			containers[containerId].getFilesIndexes()[0] = fileId;
			fileIndex = 0;
		}else{
			for (int index = 0; index < containersIndexes.length; index++) {
				if(containers[containerId].getFilesIndexes()[index] == fileId) {
					fileIndex = index;
					break;
				}
			}
			if(fileIndex == -1) {
				fileIndex = containers[containerId].getFilesIndexes().length;
				int[] newFilesIndexes = new int[fileIndex+1];
				System.arraycopy(containers[containerId].getFilesIndexes(), 0, newFilesIndexes, 0, fileIndex);
				newFilesIndexes[fileIndex] = fileId;
				containers[containerId].setFilesIndexes(newFilesIndexes);
			}
		}
		if(containers[containerId].getFiles() == null)
			containers[containerId].setFiles(new Container[fileId+1]);
		else if (containers[containerId].getFiles().length <= fileId){
			Container[] newFiles = new Container[fileId+1];
			System.arraycopy(containers[containerId].getFiles(), 0, newFiles, 0, containers[containerId].getFiles().length);
			containers[containerId].setFiles(newFiles);
		}
		if(containers[containerId].getFiles()[fileId] == null)
			containers[containerId].getFiles()[fileId] = new Container();
		return fileIndex;
	}

	
	public byte[] encodeContainersInformation() {
		Stream stream = new Stream(500000);
		stream.putByte(protocol);
		if(protocol >= 6)
			stream.putInt(revision);
		byte namedHash = 0;
		if(containersAndFilesNamed)
			namedHash |= 0x1;
		if(filesNamed)
			namedHash |= 0x2;
		stream.putByte(namedHash);
		
		
		stream.putShort(containersIndexes.length);
		for (int index = 0; index < containersIndexes.length; index++)
			stream.putShort(containersIndexes[index] - (index == 0 ? 0 : containersIndexes[index-1]));
		if(filesNamed)
			for(int index = 0; index < containersIndexes.length; index++)
				for(int fileIndex = 0; fileIndex < 64; fileIndex++)
					stream.putByte(containers[containersIndexes[index]].getFiles()[containers[containersIndexes[index]].getFilesIndexes()[fileIndex]].getVersion());
		if(containersAndFilesNamed)
			for(int index = 0; index < containersIndexes.length; index++)
				stream.putInt(containers[containersIndexes[index]].getNameHash());	
		for(int index = 0; index < containersIndexes.length; index++)
			stream.putInt(containers[containersIndexes[index]].getCrc());
		for(int index = 0; index < containersIndexes.length; index++)
			stream.putInt(containers[containersIndexes[index]].getVersion());
		for(int index = 0; index < containersIndexes.length; index++)
			stream.putShort(containers[containersIndexes[index]].getFilesIndexes().length);
		for (int index = 0; index < containersIndexes.length; index++)
			for(int fileIndex = 0; fileIndex < containers[containersIndexes[index]].getFilesIndexes().length; fileIndex++)
				stream.putShort(containers[containersIndexes[index]].getFilesIndexes()[fileIndex] - (fileIndex == 0 ? 0 : containers[containersIndexes[index]].getFilesIndexes()[fileIndex-1]));
		if(containersAndFilesNamed)
			for(int index = 0; index < containersIndexes.length; index++)
				for(int fileIndex = 0; fileIndex < containers[containersIndexes[index]].getFilesIndexes().length; fileIndex++)
					stream.putInt(containers[containersIndexes[index]].getFiles()[containers[containersIndexes[index]].getFilesIndexes()[fileIndex]].getNameHash());	
		byte[] encodedContainer = new byte[stream.offset];
		stream.offset = 0;
		stream.getBytes(encodedContainer, 0, encodedContainer.length);
		return encodedContainer;
	}
	
	public void decodeContainersInformation(byte[] data) {
		Stream stream = new Stream(data);
		protocol = stream.getUByte();
		if (protocol != 5 && protocol != 6)
			throw new RuntimeException();
		revision = protocol < 6 ? 0 : stream.getInt();
		
		int nameHash = stream.getByte();
		containersAndFilesNamed = (0x1 & nameHash) != 0;
		filesNamed = (0x2 & nameHash) != 0;
		containersIndexes = new int[stream.getUShort()];
		int lastIndex = -1;
		for (int index = 0; index < containersIndexes.length; index++) {
			containersIndexes[index] = stream.getUShort() + (index == 0 ? 0 : containersIndexes[index-1]);
			if (containersIndexes[index] > lastIndex)
				lastIndex = containersIndexes[index];
		}
		containers = new FilesContainer[lastIndex+1];
		for(int index = 0; index < containersIndexes.length; index++)
			containers[containersIndexes[index]] = new FilesContainer();
		if(containersAndFilesNamed)
			for(int index = 0; index < containersIndexes.length; index++)
				containers[containersIndexes[index]].setNameHash(stream.getInt());
		byte[][] filesNameHashes = null;
		if(filesNamed) {
			filesNameHashes = new byte[containers.length][];
			for(int index = 0; index < containersIndexes.length; index++) {
				filesNameHashes[containersIndexes[index]] = new byte[64];	
				stream.getBytes(filesNameHashes[containersIndexes[index]], 0, 64);
			}
		}
		for(int index = 0; index < containersIndexes.length; index++)
			containers[containersIndexes[index]].setCrc(stream.getInt());
		for(int index = 0; index < containersIndexes.length; index++)
			containers[containersIndexes[index]].setVersion(stream.getInt());
		for(int index = 0; index < containersIndexes.length; index++)
			containers[containersIndexes[index]].setFilesIndexes(new int[stream.getUShort()]);
		for (int index = 0; index < containersIndexes.length; index++) {
			int lastFileIndex = -1;
			for(int fileIndex = 0; fileIndex < containers[containersIndexes[index]].getFilesIndexes().length; fileIndex++) {
				containers[containersIndexes[index]].getFilesIndexes()[fileIndex] = stream.getUShort() + (fileIndex == 0 ? 0 : containers[containersIndexes[index]].getFilesIndexes()[fileIndex-1]);
				if (containers[containersIndexes[index]].getFilesIndexes()[fileIndex] > lastFileIndex)
					lastFileIndex = containers[containersIndexes[index]].getFilesIndexes()[fileIndex];
			}
			containers[containersIndexes[index]].setFiles(new Container[lastFileIndex+1]);
			for(int fileIndex = 0; fileIndex < containers[containersIndexes[index]].getFilesIndexes().length; fileIndex++)
				containers[containersIndexes[index]].getFiles()[containers[containersIndexes[index]].getFilesIndexes()[fileIndex]] = new Container();
		}
		if(filesNamed)
			for(int index = 0; index < containersIndexes.length; index++)
				for(int fileIndex = 0; fileIndex < containers[containersIndexes[index]].getFilesIndexes().length; fileIndex++)
					containers[containersIndexes[index]].getFiles()[containers[containersIndexes[index]].getFilesIndexes()[fileIndex]].setVersion(filesNameHashes[containersIndexes[index]][containers[containersIndexes[index]].getFilesIndexes()[fileIndex]]);
		if(containersAndFilesNamed)
			for (int index = 0; index < containersIndexes.length; index++)
				for(int fileIndex = 0; fileIndex < containers[containersIndexes[index]].getFilesIndexes().length; fileIndex++)
					containers[containersIndexes[index]].getFiles()[containers[containersIndexes[index]].getFilesIndexes()[fileIndex]].setNameHash(stream.getInt());
	}

	public boolean isUpdated() {
		return updated;
	}
	
}
