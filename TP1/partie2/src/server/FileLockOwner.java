package server;

import java.nio.channels.FileLock;
import java.util.Arrays;

/**
 * Class used to keep in memory the lock applied on the different files by the users.
 * @author Dominik Courcelles, Dan Vatnik
 *
 */
public class FileLockOwner {
	
	byte[] clientIdentifier;
	FileLock fileLock;
	
	/**
	 * Store the clientIdentifier that own a lock and store the different information associated with the lock.
	 * @param clientIdentifier The client identifier that own the lock.
	 * @param fileLock The information of the lock.
	 */
	public FileLockOwner(byte[] clientIdentifier, FileLock fileLock) {
		this.clientIdentifier = Arrays.copyOf(clientIdentifier, clientIdentifier.length);
		this.fileLock = fileLock;
	}
	
	/**
	 * Get the client identifier that own the lock.
	 * @return The client identifier.
	 */
	public byte[] getClientIdentifier() {
		return Arrays.copyOf(clientIdentifier, clientIdentifier.length);
	}
	
	/**
	 * Determine if the identifier that own the lock is the one passed in parameter.
	 * @param clientIdentifierToVerify The client identifier to verify.
	 * @return True if it is this client identifier that own the lock, false otherwise.
	 */
	public boolean fileLockedByClient(byte[] clientIdentifierToVerify) {
		return Arrays.equals(this.clientIdentifier, clientIdentifierToVerify);
	}
	
	/**
	 * Get the information associated with the lock.
	 * @return The lock information.
	 */
	public FileLock getFileLock() {
		return fileLock;
	}
}
