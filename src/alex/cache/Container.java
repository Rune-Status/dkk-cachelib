package alex.cache;

public class Container {

	private int version;
	private int crc;
	private int nameHash;
	private boolean updated;
	public Container() {
		nameHash = -1;
		version = -1;
		crc = -1;
	}

	public void setVersion(int version) {
		this.version = version;
	}
	
	public void updateVersion() {
		version++;
		updated = true;
	}

	public int getVersion() {
		return version;
	}
	
	public int getNextVersion() {
		return updated ? version : version+1;
	}

	public void setCrc(int crc) {
		this.crc = crc;
	}

	public int getCrc() {
		return crc;
	}
	

	public void setNameHash(int nameHash) {
		this.nameHash = nameHash;
	}

	public int getNameHash() {
		return nameHash;
	}
	
	public boolean isUpdated() {
		return updated;
	}
}
