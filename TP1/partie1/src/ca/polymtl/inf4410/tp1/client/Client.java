package ca.polymtl.inf4410.tp1.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import ca.polymtl.inf4410.tp1.shared.ServerInterface;

public class Client {
	private static final int DEFAULT_POWER_OF_BYTES = 1;
	private static final int MIN_POWER_OF_BYTES = 1;
	private static final int MAX_POWER_OF_BYTES = 7;
	
	private static void printInvalidPowerMessage(String parameter)
	{
		System.out.println("Invalid parameter " + parameter + " for the number of bytes to send. It should a number between "
				+ String.valueOf(MIN_POWER_OF_BYTES) + " and " + String.valueOf(MAX_POWER_OF_BYTES) + ". " +
				"The default value " + String.valueOf(DEFAULT_POWER_OF_BYTES + " will be used."));
	}
	
	public static void main(String[] args) {
		String distantHostname = null;

		if (args.length > 0) {
			distantHostname = args[0];
		}
		
		int puissanceNbBytes = DEFAULT_POWER_OF_BYTES;
		if(args.length > 1) {
			try
			{
				int parameter = Integer.parseInt(args[1]);
				if(MIN_POWER_OF_BYTES <= parameter && parameter <= MAX_POWER_OF_BYTES)
				{
					puissanceNbBytes = parameter;
				}
				else
				{
					printInvalidPowerMessage(args[1]);
				}
			}
			catch(NumberFormatException e)
			{
				printInvalidPowerMessage(args[1]);
			}
		}

		Client client = new Client(distantHostname);
		client.run(puissanceNbBytes);
	}

	FakeServer localServer = null; // Pour tester la latence d'un appel de
									// fonction normal.
	private ServerInterface localServerStub = null;
	private ServerInterface distantServerStub = null;

	public Client(String distantServerHostname) {
		super();
		
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		
		localServer = new FakeServer();
		localServerStub = loadServerStub("127.0.0.1");

		if (distantServerHostname != null) {
			distantServerStub = loadServerStub(distantServerHostname);
		}
	}

	private void run(int puissanceNbBytes) {
		byte[] bytes = new byte[(int)Math.pow(10, puissanceNbBytes)];
		System.out.println(String.valueOf(bytes.length) + " bytes will be sent.");

		appelNormal(bytes);

		if (localServerStub != null) {
			appelRMILocal(bytes);
		}

		if (distantServerStub != null) {
			appelRMIDistant(bytes);
		}
	}

	private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (ServerInterface) registry.lookup("server");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas défini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}

		return stub;
	}

	private void appelNormal(byte[] bytesToSend) {
		long start = System.nanoTime();
		localServer.execute(bytesToSend);
		long end = System.nanoTime();

		System.out.println("Temps écoulé appel normal: " + (end - start)
				+ " ns");
		//System.out.println("Résultat appel normal: " + result);
	}

	private void appelRMILocal(byte[] bytesToSend) {
		try {
			long start = System.nanoTime();
			localServerStub.execute(bytesToSend);
			long end = System.nanoTime();

			System.out.println("Temps écoulé appel RMI local: " + (end - start)
					+ " ns");
			//System.out.println("Résultat appel RMI local: " + result);
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
	}

	private void appelRMIDistant(byte[] bytesToSend) {
		try {
			long start = System.nanoTime();
			distantServerStub.execute(bytesToSend);
			long end = System.nanoTime();

			System.out.println("Temps écoulé appel RMI distant: "
					+ (end - start) + " ns");
			//System.out.println("Résultat appel RMI distant: " + result);
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
	}
}
