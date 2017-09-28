package server.clientGeneration;

import java.util.HashMap;

public class ClientIdGenerator extends HashMap<ClientId, Integer> {
	
	private static ClientIdGenerator instance;
	private int nextClientNumber;
	
	public static ClientIdGenerator getInstance() {
		if(instance == null) {
			instance = new ClientIdGenerator();
		}
		return instance;
	}
	
	private ClientIdGenerator() {
		nextClientNumber = 1;
	}
	
	public byte[] generateNewClientId() {
		ClientId clientToAdd = new ClientId();
		this.put(clientToAdd, nextClientNumber);
		++nextClientNumber;
		return clientToAdd.getClientId();
	}
	
	public int getClientNumberFromId(byte[] clientId) {
		return this.get(new ClientId(clientId));
	}
}
