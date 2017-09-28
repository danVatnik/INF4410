package shared;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import com.sun.istack.internal.Nullable;


public interface FileServerInterface extends Remote {
	/**
	 * Generate a new client id. It is composed of an array of bytes.
	 * @return The new client id generated.
	 * @throws RemoteException Exception thrown if an error with the connection occur.
	 */
	byte[] generateClientId() throws RemoteException;
	
	/**
	 * Create a new file.
	 * @param fileName The name of the file to create.
	 * @return True if the file was successfully created, false if the file already exists.
	 * @throws IOException Exception thrown if an error occur during the creation of the file.
	 * @throws RemoteException Exception thrown if an error with the connection occur.
	 */
	boolean create(String fileName) throws IOException, RemoteException;
	
	/**
	 * Get all the existing file names in the server and which files are currently locked by who. The identifier of the person
	 * who locked the file is not transmitted. It is only a number that represent the client that is sent.
	 * @return All the file names with the people who locked those files.
	 * @throws IOException Exception thrown if an error occur during reading the files on the disk.
	 * @throws RemoteException Exception thrown if an error with the connection occur.
	 */
	FileLockedInfo[] list() throws IOException, RemoteException;
	
	/**
	 * Retrieve the content of all the files.
	 * @return The file names and all their content.
	 * @throws IOException Exception thrown if an error occur during reading the files on the disk.
	 * @throws RemoteException Exception thrown if an error with the connection occur.
	 */
	FileContent[] syncLocalDir() throws IOException, RemoteException;
	
	/**
	 * Get the content of a file in an array of bytes.
	 * @param fileName The name of the file to get the content.
	 * @param checksum The checksum of the last version of the file or null to force the server to return the content of the file.
	 * @return The content of the file or null if the checksum passed in parameter is identical as the checksum of the file of the server.
	 * @throws IOException Exception thrown if an error occur during reading the file.
	 * @throws RemoteException Exception thrown if an error with the connection occur.
	 */
	@Nullable
	byte[] get(String fileName, @Nullable byte[] checksum) throws IOException, RemoteException;
	
	/**
	 * Lock a file to write some content inside.
	 * @param fileName The name of the file to lock.
	 * @param clientId The clientId the lock must be assigned to.
	 * @param checksum The checksum of the file the client own or null to force the server to return the content of the file.
	 * @return The content of the file or null if the checksum provided is the same as the checksum of the server file.
	 * @throws FileNotFoundException The file asked to lock doesn't exist.
	 * @throws IllegalStateException A lock already exists on the file.
	 * @throws IOException Exception thrown if an error occur during reading the file or during locking the file.
	 * @throws RemoteException Exception thrown if an error with the connection occur.
	 */
	@Nullable
	byte[] lock(String fileName, byte[] clientId, @Nullable byte[] checksum) throws FileNotFoundException, IllegalStateException, IOException, RemoteException;
	
	/**
	 * Replace the content of a file. The file must be locked to be pushed.
	 * @param fileName The name of the file to replace.
	 * @param fileContent The content of the file to substitute.
	 * @param clientId The clientId to verify that the file is locked by the right client.
	 * @throws FileNotFoundException The file doesn't exist.
	 * @throws IllegalStateException The file is not locked.
	 * @throws IllegalAccessException The file is locked by another user.
	 * @throws IOException An error occur while writing to the file or while removing the lock.
	 * @throws RemoteException Exception thrown if an error with the connection occur.
	 */
	void push(String fileName, byte[] fileContent, byte[] clientId) throws FileNotFoundException, IllegalStateException, IllegalAccessException, IOException, RemoteException;
}
