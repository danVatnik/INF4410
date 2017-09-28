package server.clientGeneration;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Class to generate an identifier for a client. Note that it is not a public class. It is only used by
 * the ClientIdGenerator class.
 * @author Dominik Courcelles, Dan Vatnik
 *
 */
class ClientId
{
	private final static int NB_BYTES_FOR_CLIENT_ID = 16;
	private final static SecureRandom idGenerator = new SecureRandom();
	
	private byte[] clientId;
	
	/**
	 * Get a copy of the client identifier
	 * @return The identifier in an array of bytes.
	 */
	public byte[] getClientId()
	{
		return clientId.clone();
	}
	
	/**
	 * Create a new client id.
	 */
	public ClientId()
	{
		clientId = new byte[NB_BYTES_FOR_CLIENT_ID];
		idGenerator.nextBytes(clientId);
	}
	
	/**
	 * Recreate a ClientId object from an existing clientId.
	 * @param clientId The client id to use.
	 */
	public ClientId(byte[] clientId) {
		this.clientId = clientId;
	}

	@Override
	public boolean equals(Object element)
	{
		boolean equals = false;
		if((element instanceof ClientId))
		{
			equals = Arrays.equals(clientId, ((ClientId)element).clientId);
		}
		return equals;
	}
}
