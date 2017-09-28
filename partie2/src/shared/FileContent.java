package shared;

import java.io.UnsupportedEncodingException;

/**
 * Class to transfer the content of a file beetween the client and the server.
 * @author Dominik Courcelles, Dan Vatnik
 *
 */
public class FileContent extends FileCommonInfo {
	private byte[] fileContent;
	
	/**
	 * Create a FileContent to store the content of a file to send it.
	 * @param fileName The name of the file to send.
	 * @param fileContent The content of the file to send in a UTF-8 format.
	 */
	public FileContent(String fileName, byte[] fileContent)
	{
		super(fileName);
		this.fileContent = fileContent;
	}
	
	/**
	 * Retrieve the content of the file in a String.
	 * @return The content of the file or null if the UTF-8 format is not a valid name.
	 */
	public String getFileContent() {
		try {
			return new String(fileContent, "UTF-8");
		}
		catch(UnsupportedEncodingException e) {
			return null;
		}
	}
}
