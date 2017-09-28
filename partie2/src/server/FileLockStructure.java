package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.ByteBuffer;
import java.util.HashMap;

import server.clientGeneration.ClientIdGenerator;
import shared.FileLockedInfo;

/**
 * Class to perform operation on files and to acquire locks on files.
 * @author Dominik Courcelles, Dan Vatnik
 *
 */
public class FileLockStructure {
	HashMap<String, FileLockOwner> fileLocks;
	
	/**
	 * Create a new FileLockStructure to handle the locks on the files, to read files and to replace the content of the files.
	 */
	public FileLockStructure() {
		fileLocks = new HashMap<>();
	}
	
	/**
	 * Lock the fileName specified for a specific user.
	 * Note : A SuppressWarnings was added because we do not want to close the FileInputStream immediately. The corresponding channel is keep to be able to
	 * close it later.
	 * @param fileName The name of the file to lock.
	 * @param clientIdentifier The identifier of the client to assign the lock.
	 * @throws FileNotFoundException Exception thrown if the file doesn't exist.
	 * @throws IllegalStateException Exception thrown if the file is already locked.
	 * @throws IOException Exception thrown if there is an error getting the channel of the file or when acquiring the lock.
	 */
	@SuppressWarnings("resource")
	public void addLockToFile(String fileName, byte[] clientIdentifier) throws FileNotFoundException, IllegalStateException, IOException {
		
		if(fileLocks.containsKey(fileName)) {
			throw new IllegalStateException("The file " + fileName + " is already locked.");
		}
		FileLock fileLock = new FileInputStream(fileName).getChannel().lock();
		fileLocks.put(fileName, new FileLockOwner(clientIdentifier, fileLock));
	}
	
	/**
	 * Get information object for the lock owner that can be passed to the client.
	 * @param fileName The name of the file to look if there is a lock.
	 * @return An object containing the fileName and the client identifier that own the lock.
	 */
	public FileLockedInfo createInfoOnLockedFile(String fileName) {
		int clientNumber = -1;
		if(fileLocks.containsKey(fileName)) {
			clientNumber = ClientIdGenerator.getInstance().getClientNumberFromId(fileLocks.get(fileName).getClientIdentifier());
		}
		return new FileLockedInfo(fileName, clientNumber);
	}
	
	/**
	 * Get the client identifier owner of the lock.
	 * @param fileName The name of the file.
	 * @return The client identifier in a byte array.
	 * @throws IllegalStateException The file is not locked.
	 */
	/*
	public byte[] getClientOwnerOfLock(String fileName) throws IllegalStateException {
		if(!fileLocks.containsKey(fileName)) {
			throw new IllegalStateException("The file " + fileName + " is not locked.");
		}
		
		return fileLocks.get(fileName).getClientIdentifier();
	}
	*/
	
	/**
	 * Replace the content of a file. A lock must have been placed on the file before.
	 * @param fileName The name of the file to replace the content.
	 * @param newContent The new content to put in the file.
	 * @param clientIdentifier The identifier of the client. It will be used to verify if it is him who own the lock.
	 * @throws FileNotFoundException Exception thrown if the file doesn't exist.
	 * @throws IllegalAccessException Exception thrown if the file was not locked by the clientIdentifier.
	 * @throws IllegalStateException Exception thrown if the file is not locked.
	 * @throws IOException Exception thrown if there is an error while replacing the content of the file.
	 */
	public void replaceFileContent(String fileName, byte[] newContent, byte[] clientIdentifier) throws FileNotFoundException, IllegalAccessException, IllegalStateException, IOException {
		if(!fileLocks.containsKey(fileName)) {
			throw new IllegalStateException("The file " + fileName + " is not locked.");
		}
		
		FileLockOwner fileLockOwner = fileLocks.get(fileName);
		if(!fileLockOwner.fileLockedByClient(clientIdentifier)) {
			throw new IllegalAccessException("The file " + fileName + " cannot be unlocked." +
					"The clientIdentifier " + clientIdentifier + " is not the identifier who locked the file.");
		}
		
		FileChannel fileChannel = fileLockOwner.getFileLock().channel();
		fileChannel.write(ByteBuffer.wrap(newContent));
	}
	
	/**
	 * Return the content of a file in a byte array. A FileChannel is used to read the file if there is an existing one.
	 * @param fileName The name of the file to read the content.
	 * @return The content of the file read.
	 * @throws FileNotFoundException Exception thrown if the file doesn't exist.
	 * @throws IOException Exception thrown if an IO error occur when reading the file size or the file content.
	 */
	public byte[] readFile(String fileName) throws FileNotFoundException, IOException {
		byte[] bytesRead;
		if(fileLocks.containsKey(fileName)) {
			FileLockOwner fileLock = fileLocks.get(fileName);
			bytesRead = readFileFromExistingChannel(fileLock.getFileLock().channel());
		}
		else {
			bytesRead = readFileFromInputFileStream(fileName);
		}
		return bytesRead;
	}
	
	private byte[] readFileFromExistingChannel(FileChannel fileChannel) throws IOException {
		int fileSize = (int)fileChannel.size();
		ByteBuffer bufferToReadFile = ByteBuffer.allocateDirect(fileSize);
		fileChannel.read(bufferToReadFile);
		return bufferToReadFile.array();
	}
	
	private byte[] readFileFromInputFileStream(String fileName) throws IOException {
		File file = new File(fileName);
		FileInputStream fileStream = new FileInputStream(file);
		byte[] bytesRead = new byte[(int)file.length()];
		fileStream.read(bytesRead);
		fileStream.close();
		return bytesRead;
	}
	
	/**
	 * Release the lock from a file.
	 * @param fileName The name of the file to release the lock.
	 * @param clientIdentifier The identifier of the client to verify if it is the current lock is owned by this client.
	 * @throws IllegalStateException Exception thrown if the file is not locked.
	 * @throws IllegalAccessException Exception thrown if the file lock is not owned by the clientIdentifier.
	 * @throws IOException Exception thrown if an error occur while removing the lock.
	 */
	public void releaseLockFromFile(String fileName, byte[] clientIdentifier) throws IllegalStateException, IllegalAccessException, IOException {
		if(!fileLocks.containsKey(fileName)) {
			throw new IllegalStateException("The file " + fileName + " is not locked.");
		}
		
		FileLockOwner fileLockOwner = fileLocks.get(fileName);
		if(!fileLockOwner.fileLockedByClient(clientIdentifier)) {
			throw new IllegalAccessException("The file " + fileName + " cannot be unlocked." +
					"The clientIdentifier " + clientIdentifier + " is not the identifier who locked the file.");
		}
		
		fileLockOwner.getFileLock().release();
		fileLocks.remove(fileName);
	}
}
