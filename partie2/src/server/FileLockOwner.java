package server;

import java.nio.channels.FileLock;
import java.util.Arrays;

public class FileLockOwner {
	
	byte[] clientIdentifier;
	FileLock fileLock;
	
	public FileLockOwner(byte[] clientIdentifier, FileLock fileLock) {
		this.clientIdentifier = Arrays.copyOf(clientIdentifier, clientIdentifier.length);
	}
	
	public byte[] getClientIdentifier() {
		return Arrays.copyOf(clientIdentifier, clientIdentifier.length);
	}
	
	public boolean fileLockedByClient(byte[] clientIdentifierToVerify) {
		return Arrays.equals(this.clientIdentifier, clientIdentifierToVerify);
	}
	
	public FileLock getFileLock() {
		return fileLock;
	}
}
