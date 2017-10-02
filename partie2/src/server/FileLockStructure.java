package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.util.HashMap;

import server.clientGeneration.ClientIdGenerator;
import shared.FileLockedInfo;
import shared.exceptions.AlreadyLockedByClient;
import shared.exceptions.InvalidClientIdentifier;

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
	 * Note : A SuppressWarnings was added because we do not want to close the RandomAccessFile immediately. The corresponding channel is keep to be able to
	 * close it later.
	 * @param fileName The path of the file to lock.
	 * @param clientIdentifier The identifier of the client to assign the lock.
	 * @throws FileNotFoundException Exception thrown if the file doesn't exist.
	 * @throws IllegalStateException Exception thrown if the file is already locked.
	 * @throws InvalidClientIdentifier Exception thrown if the client identifier was not created by the ClientIdGenerator instance.
	 * @throws IOException Exception thrown if there is an error getting the channel of the file or when acquiring the lock.
	 */
	@SuppressWarnings("resource")
	public void addLockToFile(Path fileName, byte[] clientIdentifier) throws FileNotFoundException, IllegalStateException, InvalidClientIdentifier, IOException {
		if(ClientIdGenerator.getInstance().getClientNumberFromId(clientIdentifier) == -1) {
			throw new InvalidClientIdentifier("The client identifier is not valid.");
		}
		File fileToLock = fileName.toFile();
		if(!fileToLock.exists()) {
			throw new FileNotFoundException();
		}
		if(fileLocks.containsKey(fileName.toString())) {
			int number = getClientNumberFromFileAndClientId(fileName.toString());
			throw new AlreadyLockedByClient("The file " + fileName.getFileName().toString() + " is already locked.", number);
		}
		FileLock fileLock = new RandomAccessFile(fileToLock, "rw").getChannel().lock();
		fileLocks.put(fileName.toString(), new FileLockOwner(clientIdentifier, fileLock));
	}

	/**
	 * Get information object for the lock owner that can be passed to the client.
	 * @param fileName The name of the file to look if there is a lock.
	 * @return An object containing the fileName and the client identifier that own the lock.
	 */
	public FileLockedInfo createInfoOnLockedFile(Path fileName) {
		int clientNumber = -1;
		if(fileLocks.containsKey(fileName.toString())) {
			clientNumber = getClientNumberFromFileAndClientId(fileName.toString());
			if(clientNumber == -1) {
				System.out.println("Warning : cannot find the associated client number with the client id that locks the file " + fileName.getFileName().toString());
			}
		}
		return new FileLockedInfo(fileName.getFileName().toString(), clientNumber);
	}
	
	/**
	 * Replace the content of a file. A lock must have been placed on the file before.
	 * @param fileName The path of the file to replace the content.
	 * @param newContent The new content to put in the file.
	 * @param clientIdentifier The identifier of the client. It will be used to verify if it is him who own the lock.
	 * @throws FileNotFoundException Exception thrown if the file doesn't exist.
	 * @throws IllegalAccessException Exception thrown if the file was not locked by the clientIdentifier.
	 * @throws IllegalStateException Exception thrown if the file is not locked.
	 * @throws IOException Exception thrown if there is an error while replacing the content of the file.
	 */
	public void replaceFileContent(Path fileName, byte[] newContent, byte[] clientIdentifier) throws FileNotFoundException, InvalidClientIdentifier, AlreadyLockedByClient, IllegalStateException, IOException {
		if(ClientIdGenerator.getInstance().getClientNumberFromId(clientIdentifier) == -1) {
			throw new InvalidClientIdentifier("The client identifier is not valid.");
		}
		if(!fileName.toFile().exists()) {
			throw new FileNotFoundException();
		}
		if(!fileLocks.containsKey(fileName.toString())) {
			throw new IllegalStateException("The file " + fileName.getFileName().toString() + " is not locked.");
		}
		
		FileLockOwner fileLockOwner = fileLocks.get(fileName.toString());
		if(!fileLockOwner.fileLockedByClient(clientIdentifier)) {
			int clientNumber = ClientIdGenerator.getInstance().getClientNumberFromId(fileLockOwner.getClientIdentifier());
			throw new AlreadyLockedByClient("The file " + fileName.getFileName().toString() + " cannot be unlocked." +
					"The clientIdentifier " + clientIdentifier + " is not the identifier who locked the file.", clientNumber);
		}
		
		FileChannel fileChannel = fileLockOwner.getFileLock().channel();
		fileChannel.write(ByteBuffer.wrap(newContent), 0);
		fileChannel.truncate(newContent.length);
	}
	
	/**
	 * Return the content of a file in a byte array.
	 * @param fileName The path of the file to read the content.
	 * @return The content of the file read.
	 * @throws FileNotFoundException Exception thrown if the file doesn't exist.
	 * @throws IOException Exception thrown if an IO error occur when reading the file size or the file content.
	 */
	public byte[] readFile(Path fileName) throws FileNotFoundException, IOException {
		byte[] bytesRead;
		if(fileLocks.containsKey(fileName.toString())) {
			FileLockOwner fileLock = fileLocks.get(fileName.toString());
			bytesRead = readFileFromExistingChannel(fileLock.getFileLock().channel());
		}
		else {
			bytesRead = readFileFromInputFileStream(fileName);
		}
		return bytesRead;
	}

	private byte[] readFileFromExistingChannel(FileChannel fileChannel) throws IOException {
		fileChannel.position(0);
		int fileSize = (int)fileChannel.size();
		ByteBuffer bufferToReadFile = ByteBuffer.allocate(fileSize);
		fileChannel.read(bufferToReadFile);
		return bufferToReadFile.array();
	}

	private byte[] readFileFromInputFileStream(Path fileName) throws IOException {
		File file = fileName.toFile();
        FileInputStream fileStream = new FileInputStream(file);
        byte[] bytesRead = new byte[(int)file.length()];
        fileStream.read(bytesRead);
        fileStream.close();
        return bytesRead;
	}
	
	/**
	 * Look in the HashMap structure for the information of the lock. The key existence must be verified before calling this function.
	 * @param fileName The fileName to get the fileLockInfo.
	 * @return The client number or -1 if it was not created by this server.
	 */
	private int getClientNumberFromFileAndClientId(String fileName) {
		FileLockOwner fileLock = fileLocks.get(fileName.toString());
		byte[] id = fileLock.getClientIdentifier();
		return ClientIdGenerator.getInstance().getClientNumberFromId(id);
	}

	/**
	 * Release the lock from a file.
	 * @param fileName The path to the file to release the lock.
	 * @param clientIdentifier The identifier of the client to verify if it is the current lock is owned by this client.
	 * @throws IllegalStateException Exception thrown if the file is not locked.
	 * @throws AlreadyLockedByClient Exception thrown if the file lock is not owned by the clientIdentifier.
	 * @throws InvalidClientIdentifier Exception thrown if the client identifier was not created by the ClientIdGenerator instance.
	 * @throws IOException Exception thrown if an error occur while removing the lock.
	 */
	public void releaseLockFromFile(Path fileName, byte[] clientIdentifier) throws IllegalStateException, AlreadyLockedByClient, InvalidClientIdentifier, IOException {
		if(ClientIdGenerator.getInstance().getClientNumberFromId(clientIdentifier) == -1) {
			throw new InvalidClientIdentifier("The client identifier is not valid.");
		}
		if(!fileLocks.containsKey(fileName.toString())) {
			throw new IllegalStateException("The file " + fileName.getFileName().toString() + " is not locked.");
		}
		
		FileLockOwner fileLockOwner = fileLocks.get(fileName.toString());
		if(!fileLockOwner.fileLockedByClient(clientIdentifier)) {
			int clientNumber = ClientIdGenerator.getInstance().getClientNumberFromId(fileLockOwner.getClientIdentifier());
			throw new AlreadyLockedByClient("The file " + fileName.getFileName().toString() + " cannot be unlocked." +
					"The clientIdentifier " + clientIdentifier + " is not the identifier who locked the file.", clientNumber);
		}
		
		fileLockOwner.getFileLock().channel().close();
		fileLocks.remove(fileName.toString());
	}
}
