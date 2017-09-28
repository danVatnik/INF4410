package server.clientGeneration;

import java.util.HashMap;

/**
 * Singleton class to generate and memorize the ClientId generated and their client number.
 * @author Dominik Courcelles, Dan Vatnik
 *
 */
public class ClientIdGenerator extends HashMap<ClientId, Integer> {
	
	private static ClientIdGenerator instance;
	private int nextClientNumber;
	
	/**
	 * Get the instance of the ClientIdGenerator.
	 * @return The ClientIdGenerator.
	 */
	public static ClientIdGenerator getInstance() {
		if(instance == null) {
			instance = new ClientIdGenerator();
		}
		return instance;
	}
	
	/**
	 * Can only be instantiated once. Initialize the singleton.
	 */
	private ClientIdGenerator() {
		nextClientNumber = 1;
	}
	
	/**
	 * Generate a new client id for a client that want to make modifications on the files of the server.
	 * @return An array of bytes representing the client identifier.
	 */
	public byte[] generateNewClientId() {
		ClientId clientToAdd = new ClientId();
		this.put(clientToAdd, nextClientNumber);
		++nextClientNumber;
		return clientToAdd.getClientId();
	}
	
	/**
	 * Get the client number associated with the client identifier provided.
	 * @param clientId The client id to get the client number.
	 * @return The client number associated or -1 if the client identifier doesn't exist.
	 */
	public int getClientNumberFromId(byte[] clientId) {
		return this.get(new ClientId(clientId));
	}
}
