package shared;

import java.io.Serializable;

/**
 * Class to transfer the information about the person who locked a file.
 * @author Dominik Courcelles, Dan Vatnik
 *
 */
public class FileLockedInfo extends FileCommonInfo implements Serializable{
	private final int clientNumberFileLocked;

	public FileLockedInfo(){
		super();
		clientNumberFileLocked = -1;
	}
	
	/**
	 * Store the information of the client number who own a file.
	 * @param fileName The name of the file.
	 * @param clientNumberFileLocked The client number who locked the file. It should be -1 if no client locked the file.
	 */
	public FileLockedInfo(String fileName, int clientNumberFileLocked) {
		super(fileName);
		this.clientNumberFileLocked = clientNumberFileLocked;
	}

	/**
	 * Get the client number who locked the file.
	 * @return The client number who locked the file.
	 */
	public int getClientNumberThatLockedFile() {
		return clientNumberFileLocked;
	}
}
