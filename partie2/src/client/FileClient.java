package client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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
		
		//TODO: Read args and determine which action the client must do.
	}
	
	public FileClient(String hostname, String rmiRegistryServerName) throws NotBoundException, AccessException, RemoteException {
		Registry registry = LocateRegistry.getRegistry(hostname);
		stub = (FileServerInterface) registry.lookup(rmiRegistryServerName);
	}
}
