package shared;

public class FileLockedInfo extends FileCommonInfo {
	private final byte[] clientIdFileLocked;
	
	public FileLockedInfo(String fileName, byte[] clientIdFileLocked) {
		super(fileName);
		this.clientIdFileLocked = clientIdFileLocked;
	}
	
	public byte[] getClientIdThatLockedFile() {
		return clientIdFileLocked;
	}
}
