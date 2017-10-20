package shared.exceptions;

public class AlreadyLockedByClient extends RuntimeException {
	private final int clientNumber;
	
	public AlreadyLockedByClient(String message, int clientNumberThatLocked) {
		clientNumber = clientNumberThatLocked;
	}
	
	public int getClientNumber() {
		return clientNumber;
	}
}
