package client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

public class FileClient {
	private final static String RMI_REGISTRY_SERVER_NAME = "FileServer";
	
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
		
		client.executeCommand(args);
		
		
	}
	
	private void executeCommand(String[] args){
		
		String notLocked = "\tnon verrouillé";
		String lockedByClient = "\tverrouillé par client ";
		String alreadyLockedByClient = " est déjà verrouillé par client ";
		String locked = " verrouillé";
		String files = " fichier(s)";
		String fileAdded = " ajouté.";
		String alredyExists = " existe déjà.";
		String fileSentToServer = " a été envoyé au serveur";
		String commandRefused = "opération refusée:";
		String errorPushLock = " vous devez d'abord verrouiller le fichier";
		String missingArg = "Argument manquant";

		String sync = " synchronisé";
		String upToDate = " est déjà à jour";
		
		String command = "empty";
		if(args.length > 0)
			command = args[0];
		
		
		switch(command){
		case "list":
			try {
				FileLockedInfo[] fileLockInfos = stub.list();
				for(FileLockedInfo lockInfo : fileLockInfos){
					if(lockInfo.getClientNumberThatLockedFile() == -1){
						 System.out.println("* " + lockInfo.getFileName() + notLocked);
					}
					else{
						System.out.println("* " + lockInfo.getFileName() + lockedByClient + lockInfo.getClientNumberThatLockedFile());
					}
				}
				System.out.println(fileLockInfos.length + files);
	
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		case "create":
			if(args.length == 2){
				String file = args[1];
				try {
					
					boolean success = stub.create(file);
					
					if(success)
						System.out.println(file + fileAdded);
					else
						System.out.println(file + alredyExists);
					
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else{
				System.out.println(missingArg);
			}
			break;
		case "get":
			if(args.length == 2){
				String file = args[1];
				
				try{
						
				    byte[] fileContent = stub.get(file, getChecksum(file));
				    
				    if(fileContent != null){
						Files.write(Paths.get(file), fileContent);
						System.out.println(file + sync);
				    }
				    else{
				    	System.out.println(file + upToDate);
				    }
				    
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
			else{
				System.out.println(missingArg);
			}
			break;
		case "push":
			if(args.length == 2){
				String file = args[1];
				try {
					stub.push(file, readFile(file), getOrCreateClientId());
					System.out.println(file + fileSentToServer);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			else{
				System.out.println(missingArg);
			}
			break;
		case "lock":
			if(args.length == 2){
				String file = args[1];
				
				try{				
					
					byte[] fileContent = stub.lock(file, getOrCreateClientId(), getChecksum(file));
					
				    if(fileContent != null){
						Files.write(Paths.get(file), fileContent);				
				    }
				    System.out.println(file + locked);
				    
				} catch (IllegalStateException e){
					System.out.println(file + alreadyLockedByClient + "NEED TO GET CLIENT");
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else{
				System.out.println(missingArg);
			}
			break;
		case "syncLocalDir":
			try {
				FileContent[] filesContent = stub.syncLocalDir();
				for(FileContent fileContent : filesContent){
					Files.write(Paths.get(fileContent.getFileName()), fileContent.getFileContentBytes());
				}
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		default:
			System.out.println("Help");
		}
	}
	
	private byte[] readFile(String file){
		byte[] data = null;
		File f = new File(file + "txt");
		try {
			if(f.exists() && !f.isDirectory()) { 
				data = Files.readAllBytes(Paths.get("clientId.txt"));
				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data;
	}
	
	private byte[] getOrCreateClientId(){
		byte[] clientId = null;
		File f = new File("clientId.txt");
		try {
			if(f.exists() && !f.isDirectory()) { 
					clientId = Files.readAllBytes(Paths.get("clientId.txt"));
				
			}
			else{
				clientId = stub.generateClientId();
				Files.write(Paths.get("clientId.txt"), clientId);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return clientId;
	}
	
	private byte[] getChecksum(String file){
		byte[] checksum = null;
		try{
			File f = new File(file);
			MessageDigest md = MessageDigest.getInstance("MD5");
			
			if(f.exists() && !f.isDirectory()) {
					
				FileInputStream fileStream = new FileInputStream(f);
		        byte[] bytesRead = new byte[(int)f.length()];
		        fileStream.read(bytesRead);
		        fileStream.close();
		        md.update(bytesRead);
			    	        	
			    checksum = md.digest();
			}
			
			return checksum;
			
		} catch (NoSuchAlgorithmException e){
			e.printStackTrace();
		} catch(FileNotFoundException e){
			e.printStackTrace();
		} catch(IOException e){
			e.printStackTrace();
		}
		
		return null;
	}
	
	public FileClient(String hostname, String rmiRegistryServerName) throws NotBoundException, AccessException, RemoteException {
		Registry registry = LocateRegistry.getRegistry(hostname);
		stub = (FileServerInterface) registry.lookup(rmiRegistryServerName);
	}
}
