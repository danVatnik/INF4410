package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import shared.FileContent;
import shared.FileLockedInfo;
import shared.FileServerInterface;
import shared.exceptions.AlreadyLockedByClient;
import shared.exceptions.InvalidClientIdentifier;

public class FileClient {
	private final static String RMI_REGISTRY_SERVER_NAME = "FileServer";
	private final static String NOT_LOCKED = "\tnon verrouillé";
	private final static String LOCKED_BY_CLIENT = "\tverrouillé par client ";
	private final static String ALREADY_LOCKED = " est déjà verrouillé par client ";
	private final static String LOCKED = " verrouillé";
	private final String FILES = " fichier(s)";
	private final static String FILE_ADDED = " ajouté.";
	private final static String ALREADY_EXISTS = " existe déjà.";
	private final static String FILE_SENT_TO_SERVER = " a été envoyé au serveur";
	private final static String COMMAND_REFUSED = "opération refusée : ";
	private final static String ERROR_NOT_CREATED = " n'existe pas.";
	private final static String ERROR_PUSH_LOCK = "vous devez d'abord verrouiller le fichier";
	private final static String MISSING_ARG = "Argument manquant";
	private final static String SYNC = " synchronisé";
	private final static String UP_TO_DATE = " est déjà à jour";
	
	private final static String CLIENT_ID_FILENAME = ".CLIENT";
	
	private final FileServerInterface stub;
	
	public static void main(String[] args) {
		FileClient client = null;
		try {
			client = new FileClient("127.0.0.1", RMI_REGISTRY_SERVER_NAME);
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas défini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
		
		if(client == null) {
			System.exit(1);
		}
		
		try {
			client.executeCommand(args);
		} catch(IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private void executeCommand(String[] args) throws IOException {
		String command = "empty";
		if(args.length > 0)
			command = args[0];
		
		
		switch(command){
		case "list":
			FileLockedInfo[] fileLockInfos = stub.list();
			for(FileLockedInfo lockInfo : fileLockInfos){
				if(lockInfo.getClientNumberThatLockedFile() == -1){
					 System.out.println("* " + lockInfo.getFileName() + NOT_LOCKED);
				}
				else{
					System.out.println("* " + lockInfo.getFileName() + LOCKED_BY_CLIENT + lockInfo.getClientNumberThatLockedFile());
				}
			}
			System.out.println(fileLockInfos.length + FILES);
			break;

		case "create":
			if(args.length == 2){
				String file = args[1];
				boolean success = stub.create(file);
				
				if(success)
					System.out.println(file + FILE_ADDED);
				else
					System.out.println(file + ALREADY_EXISTS);
			}
			else{
				System.out.println(MISSING_ARG);
				printHelp();
			}
			break;

		case "get":
			if(args.length == 2) {
				String file = args[1];
				try{
				    byte[] fileContent = stub.get(file, getChecksum(file));
				    
				    if(fileContent != null){
						Files.write(Paths.get(file), fileContent);
						System.out.println(file + SYNC);
				    }
				    else{
				    	System.out.println(file + UP_TO_DATE);
				    }
				    
				} catch (FileNotFoundException e) {
					System.out.println(COMMAND_REFUSED + file + ERROR_NOT_CREATED);
				}
			}
			else{
				System.out.println(MISSING_ARG);
				printHelp();
			}
			break;

		case "push":
			if(args.length == 2) {
				push(args[1]);
			}
			else{
				System.out.println(MISSING_ARG);
				printHelp();
			}
			break;

		case "lock":
			if(args.length == 2){		
				lock(args[1]);
			}
			else {
				System.out.println(MISSING_ARG);
				printHelp();
			}
			break;

		case "syncLocalDir":
			FileContent[] filesContent = stub.syncLocalDir();
			for(FileContent fileContent : filesContent){
				Files.write(Paths.get(fileContent.getFileName()), fileContent.getFileContentBytes());
			}
			break;
			
		default:
			System.out.println("Commande invalide");
			printHelp();
		}
	}
	
	private void printHelp(){
		System.out.println();
		System.out.println("Commandes: ");
		System.out.println("==========");
		System.out.println("list: list");
		System.out.println("create: create [nom_fichier]");
		System.out.println("get: get [nom_fichier]");
		System.out.println("lock: lock [nom_fichier]");
		System.out.println("push: push [nom_fichier]");
		System.out.println("syncLocalDir: syncLocalDir");
	}
	
	private void lock(String file) throws IOException{
		
		boolean renewId = false;
		try {
			performLock(file);
		} catch(InvalidClientIdentifier e) {
			renewId = true;
		} catch(AlreadyLockedByClient e){
			System.out.println(COMMAND_REFUSED + file + ALREADY_LOCKED  + e.getClientNumber());
		} catch(FileNotFoundException e){
			System.out.println(COMMAND_REFUSED + file + ERROR_NOT_CREATED);
		}
		
		if(renewId) {
			createClientId();
			try {
				performLock(file);
			} catch(AlreadyLockedByClient e){
				System.out.println(COMMAND_REFUSED + file + ALREADY_LOCKED  + e.getClientNumber());
			} catch(FileNotFoundException e){
				System.out.println(COMMAND_REFUSED + file + ERROR_NOT_CREATED);
			}
		}
	}
	
private void push(String file) throws IOException{
		
		boolean renewId = false;
		try {
			performPush(file);
		} catch(InvalidClientIdentifier e) {
			renewId = true;
		} catch(IllegalStateException e){
			System.out.println(COMMAND_REFUSED + ERROR_PUSH_LOCK);
		} catch(AlreadyLockedByClient e){
			System.out.println(COMMAND_REFUSED + file + ALREADY_LOCKED  + e.getClientNumber());
		} catch(FileNotFoundException e){
			System.out.println(COMMAND_REFUSED + file + ERROR_NOT_CREATED);
		}
		
		if(renewId) {
			createClientId();
			try {
				performPush(file);
			} catch(IllegalStateException e){
				System.out.println(COMMAND_REFUSED + ERROR_PUSH_LOCK);
			} catch(AlreadyLockedByClient e){
				System.out.println(COMMAND_REFUSED + file + ALREADY_LOCKED  + e.getClientNumber());
			} catch(FileNotFoundException e){
				System.out.println(COMMAND_REFUSED + file + ERROR_NOT_CREATED);
			}
		}
	}
	
	/**
	 * Read a file and return all its content in an array of bytes.
	 * @param file The file to read.
	 * @return The content of the file or null if the file doesn't exist.
	 * @throws IOException Exception thrown if an error occur while reading the file.
	 */
	private byte[] readFile(String file) throws IOException {
		byte[] data = null;
		File f = new File(file);
		if(f.exists() && !f.isDirectory()) { 
			data = Files.readAllBytes(Paths.get(file));
		}
		return data;
	}
	
	/**
	 * Look for the file CLIENT_ID_FILENAME on the disk and get it's content if it exists. Otherwise, ask the server
	 * for an identifier and write it to CLIENT_ID_FILENAME.
	 * @return The identifier on the disk or generated by the server.
	 * @throws IOException Exception thrown if an error occur while reading or writing on the disk.
	 * @throws RemoteException Exception thrown if an error occur while communicating with the server.
	 */
	private byte[] getOrCreateClientId() throws IOException, RemoteException {
		byte[] clientId = null;
		try {
			clientId = readFile(CLIENT_ID_FILENAME);
		} catch(IOException e) {
			System.out.println("Error while reading the client id from disk.");
			throw e;
		}
		
		if(clientId == null) {
			createClientId();
		}
		return clientId;
	}
	
	/**
	 * Ask the server for an identifier and write it to CLIENT_ID_FILENAME.
	 * @return The identifier returned by the server.
	 * @throws IOException Exception thrown if an error occur while writing on the disk.
	 * @throws RemoteException Exception thrown if an error occur while communicating with the server.
	 */
	private byte[] createClientId() throws IOException, RemoteException {
		byte[] clientId = stub.generateClientId();
		try {
			Files.write(Paths.get(CLIENT_ID_FILENAME), clientId);
		} catch(IOException e) {
			System.out.println("Cannot write the client identifier to disk.");
			throw e;
		}
		return clientId;
	}
	
	/**
	 * Calculate the checksum of a file.
	 * @param file The file to calculate the checksum.
	 * @return The checksum or null if the file doesn't exist.
	 */
	private byte[] getChecksum(String file) throws IOException {
		byte[] checksum = null;
		try {
			byte[] bytesRead = readFile(file);
			if(bytesRead != null) {
				MessageDigest md = MessageDigest.getInstance("MD5");
				md.update(bytesRead);
			    checksum = md.digest();
			}
		} catch (NoSuchAlgorithmException e){
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Cannot read the file to calculate the checksum.");
		}
		
		return checksum;
	}
	
	/**
	 * Perform the lock request to the server.
	 * @param file The file to lock.
	 * @throws InvalidClientIdentifier The identifier is no longer valid. It needs to be recreated.
	 */
	private void performLock(String file) throws InvalidClientIdentifier, IOException {
		try {
			byte[] fileContent = stub.lock(file, getOrCreateClientId(), getChecksum(file));
		    if(fileContent != null) {
				Files.write(Paths.get(file), fileContent);				
		    }
		    System.out.println(file + LOCKED);
		}
		catch (AlreadyLockedByClient e){
			System.out.println(file + ALREADY_LOCKED + e.getClientNumber());
		} catch(FileNotFoundException e) {
			System.out.println(COMMAND_REFUSED + file + ERROR_NOT_CREATED);
		}
	}
	
	/**
	 * Perform the push request to the server.
	 * @param file The file to replace content.
	 * @throws InvalidClientIdentifier The identifier is no longer valid. It needs to be recreated.
	 */
	private void performPush(String file) throws InvalidClientIdentifier, IOException {
		try {
			stub.push(file, readFile(file), getOrCreateClientId());
			System.out.println(file + FILE_SENT_TO_SERVER);
		} catch (FileNotFoundException e) {
			System.out.println(COMMAND_REFUSED + file + ERROR_NOT_CREATED);
		} catch (IllegalStateException e) {
			System.out.println(COMMAND_REFUSED + ERROR_PUSH_LOCK);
			e.printStackTrace();
		} catch (AlreadyLockedByClient e) {
			System.out.println(COMMAND_REFUSED + file + ALREADY_LOCKED + e.getClientNumber());
		}
	}
	
	/**
	 * Create a file client to interact with the server.
	 * @param hostname The hostname of the server to interact.
	 * @param rmiRegistryServerName The registry of the server to get the server stub.
	 * @throws NotBoundException
	 * @throws AccessException
	 * @throws RemoteException
	 */
	public FileClient(String hostname, String rmiRegistryServerName) throws NotBoundException, AccessException, RemoteException {
		Registry registry = LocateRegistry.getRegistry(hostname);
		stub = (FileServerInterface) registry.lookup(rmiRegistryServerName);
	}
}
