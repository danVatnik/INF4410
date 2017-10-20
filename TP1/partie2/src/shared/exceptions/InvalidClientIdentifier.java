package shared.exceptions;

public class InvalidClientIdentifier extends RuntimeException {
	public InvalidClientIdentifier(String message) {
		super(message);
	}
}
