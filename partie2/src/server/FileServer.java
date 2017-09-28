package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import server.clientGeneration.ClientIdGenerator;
import shared.FileContent;
import shared.FileLockedInfo;
import shared.FileServerInterface;

/**
 * A simple shared file system. Users can create, get, lock and push files to the server.
 * @author Dominik Courcelles, Dan Vatnik
 *
 */
public class FileServer implements FileServerInterface {
	
	private static class VisitContentOfARepository implements FileVisitor<Path> {
		private FileLockStructure fileLockStructure;
		private final ArrayList<FileLockedInfo> retrievedLockedInfo;
		
		public VisitContentOfARepository(FileLockStructure fileLockStructure) {
			this.fileLockStructure = fileLockStructure;
			retrievedLockedInfo = new ArrayList<>();
		}
		
		@Override
		public FileVisitResult postVisitDirectory(Path arg0, IOException arg1) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path arg0, BasicFileAttributes arg1) throws IOException {
			return FileVisitResult.SKIP_SUBTREE;
		}

		@Override
		public FileVisitResult visitFile(Path pathToFile, BasicFileAttributes fileAttributes) throws IOException {
			retrievedLockedInfo.add(fileLockStructure.createInfoOnLockedFile(pathToFile.toString()));
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path arg0, IOException exceptionOccured) throws IOException {
			retrievedLockedInfo.clear();
			throw exceptionOccured;
		}
		
		public FileLockedInfo[] getFileLockInfo() {
			FileLockedInfo[] infoRetrieved = retrievedLockedInfo.toArray(new FileLockedInfo[0]);
			retrievedLockedInfo.clear();
			return infoRetrieved;
		}
	}
	
	private final static String FOLDER_NAME_FOR_SHARED_FILES = "sharedFiles";
	private final static String RMI_REGISTRY_SERVER_NAME = "FileServer";
	
	private final FileSystem fileSystem;
	private final String rootFolderName;
	private final VisitContentOfARepository folderVisitor;
	private MessageDigest md5Calculator;
	private final FileLockStructure filesLocked;
	
	public static void main(String[] args) {
		try {
			FileServer server = new FileServer(FOLDER_NAME_FOR_SHARED_FILES);
			server.start(RMI_REGISTRY_SERVER_NAME);
		}
		catch(IllegalAccessException e) {
			System.out.println(e.getMessage());
		}
		catch(Exception e) {
			System.out.println("Unhandled exception." + e.getMessage());
			System.out.println(e.getStackTrace());
		}
	}
	
	public FileServer(String rootFolderName) throws IllegalAccessException {
		fileSystem = FileSystems.getDefault();
		this.rootFolderName = rootFolderName;
		filesLocked = new FileLockStructure();
		folderVisitor = new VisitContentOfARepository(filesLocked);
		try {
			md5Calculator = MessageDigest.getInstance("MD5");
		}
		catch(NoSuchAlgorithmException e) {
			System.out.println("The algorithm provided for the hash calculation is not valid.");
		}
		File folderToCreate = new File(rootFolderName);
		if(folderToCreate.exists()) {
			if(!folderToCreate.isDirectory() || folderToCreate.canRead() || !folderToCreate.canWrite()) {
				throw new IllegalAccessException("A folder at the location " + folderToCreate + " already exists, but it cannot be read or written.");
			}
		}
		else if(!folderToCreate.mkdir()) {
			throw new IllegalAccessException("Cannot create a directory at the location " + folderToCreate);
		}
	}
	
	@Override
	public byte[] generateClientId() {
		return ClientIdGenerator.getInstance().generateNewClientId();
	}

	@Override
	public boolean create(String fileName) throws IOException {
		return fileSystem.getPath(rootFolderName, fileName).toFile().createNewFile();
	}

	@Override
	public FileLockedInfo[] list() throws IOException {
		Files.walkFileTree(fileSystem.getPath(rootFolderName), folderVisitor);
		return folderVisitor.getFileLockInfo();
	}

	@Override
	public FileContent[] syncLocalDir() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public byte[] get(String fileName, byte[] checksum) throws IOException {
		byte[] fileContent = filesLocked.readFile(fileSystem.getPath(rootFolderName, fileName).toString());
		md5Calculator.update(fileContent);
		if(Arrays.equals(md5Calculator.digest(), checksum)) {
			fileContent = null;
		}
		return fileContent;
	}

	@Override
	public byte[] lock(String fileName, byte[] clientId, byte[] checksum) throws FileNotFoundException, IllegalStateException, IOException {
		filesLocked.addLockToFile(fileSystem.getPath(rootFolderName, fileName).toString(), clientId);
		return get(fileName, checksum);
	}

	@Override
	public void push(String fileName, byte[] fileContent, byte[] clientId) throws FileNotFoundException, IllegalStateException, IllegalAccessException, IOException {
		String filePath = fileSystem.getPath(rootFolderName, fileName).toString();
		filesLocked.replaceFileContent(filePath, fileContent, clientId);
		filesLocked.releaseLockFromFile(filePath, clientId);
	}
	
	public void start(String rmiRegistryServerName) {
		FileServerInterface stub = null;
		try {
			stub = (FileServerInterface) UnicastRemoteObject.exportObject(this, 0);
		}
		catch(RemoteException e) {
			System.out.println("Cannot create the server stub. " + e.getMessage());
		}
		
		if(stub != null) {
			try {
				Registry registry = LocateRegistry.getRegistry();
				registry.rebind(rmiRegistryServerName, stub);
				System.out.println("Server ready.");
			}
			catch(RemoteException e) {
				System.out.println("Cannot get the rmiregistry. Is the rmiregistry started? Error message : " + e.getMessage());
				unexportRemoteObject(this);
			}
		}
	}
	
	private void unexportRemoteObject(Remote remoteObject) {
		try {
			UnicastRemoteObject.unexportObject(remoteObject, true);
		}
		catch(NoSuchObjectException e) {
			System.out.println("Cannot unbind the server object. The process will stay blocked." + e.getMessage());
		}
	}
}
