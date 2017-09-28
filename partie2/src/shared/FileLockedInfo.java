package shared;

public class FileLockedInfo extends FileCommonInfo {
	private final int clientNumberFileLocked;
	
	public FileLockedInfo(String fileName, int clientNumberFileLocked) {
		super(fileName);
		this.clientNumberFileLocked = clientNumberFileLocked;
	}
	
	public int getClientNumberThatLockedFile() {
		return clientNumberFileLocked;
	}
}
