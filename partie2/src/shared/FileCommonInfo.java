package shared;

/**
 * Base class for file exchange information. This class contains only the fileName and children can
 * add other information that should be exchanged between the client and the server.
 * @author Dominik Courcelles, Dan Vatnik
 *
 */
public class FileCommonInfo {
	private String fileName;
	
	/**
	 * Create a class to handle base information on a file.
	 * @param fileName The name of the file to store.
	 */
	public FileCommonInfo(String fileName) {
		this.fileName = fileName;
	}
	
	/**
	 * Get the fileName stored.
	 * @return The fileName
	 */
	public final String getFileName() {
		return fileName;
	}
}
