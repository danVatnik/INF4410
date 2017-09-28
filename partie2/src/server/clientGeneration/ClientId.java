package server.clientGeneration;

import java.security.SecureRandom;
import java.util.Arrays;

class ClientId
{
	private final static int NB_BYTES_FOR_CLIENT_ID = 16;
	private final static SecureRandom idGenerator = new SecureRandom();
	
	private byte[] clientId;
	
	public byte[] getClientId()
	{
		return clientId.clone();
	}
	
	public ClientId()
	{
		clientId = new byte[NB_BYTES_FOR_CLIENT_ID];
		idGenerator.nextBytes(clientId);
	}
	
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
