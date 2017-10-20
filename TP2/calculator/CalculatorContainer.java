package calculator;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Un conteneur pour le calculateur qui récupère un RMIRegistry et qui s'enregistre auprès de ce registry.
 * Une fois enregistré, il écoute par RMI les demandes de calculs pour les opérations Pell et Prime.
 * Il possède 3 paramètres configuratbles : Le nombre d'opérations qu'il accepte toujours avant d'avoir un
 * taux de refus, le pourcentage de temps auquel le calculateur fourni un mauvais résultat et l'hôte où il
 * faut s'enregistrer.
 * @author dcourcel
 *
 */
public class CalculatorContainer {
	private static CalculatorContainer calculatorContainer;
	
	private final Calculator calculator;
	private final Remote objectExported;
	private Registry registry;
	private boolean registered;
	
	/**
	 * Crée un nouveau conteneur pour le calculateur. Lors de la création, il essaie d'exporter l'objet Calculator pour qu'il
	 * puisse être ajouté au registry et essaie de récupérer le registry associé au hostname.
	 * @param nbOperationsToAccept Le nombre d'opérations que le calculateur acceptera toujours.
	 * @param maliciousPercent Le pourcentage de temps (entre 0 et 100 inclusivement) que le calculateur retourne un mauvais résultat.
	 * @param hostname Le nom d'hôte auquel il faut récupérer le registry.
	 * @throws RemoteException
	 */
	public CalculatorContainer(int nbOperationsToAccept, float maliciousPercent, String hostname) throws RemoteException {
		calculator = new Calculator(nbOperationsToAccept, maliciousPercent);
		objectExported = UnicastRemoteObject.exportObject(calculator, 0);
		registry = LocateRegistry.getRegistry(hostname, Registry.REGISTRY_PORT);
		registered = false;
	}

	/**
	 * Enlève l'exportation du calculateur. Une fois cette méthode appelée, le register et unregister au registry ne
	 * fonctionneront plus.
	 * @throws NoSuchObjectException Si l'objet n'était pas exporté.
	 */
	public void doCleanup() throws NoSuchObjectException {
		UnicastRemoteObject.unexportObject(calculator, true);
	}
	
	/**
	 * Enregistre l'objet au RMIRegistry. Le calculateur pourra être appelé à partir de l'extérieur à partir de ce moment.
	 * @throws AccessException Si le registry est local et s'il bloque l'accès pour effectuer l'opération.
	 * @throws AlreadyBoundException Si le nom pour l'enregistrement est déjà utilisé.
	 * @throws RemoteException Si la communication avec le registre échoue.
	 */
	public void registerToRMIRegistry() throws AccessException, AlreadyBoundException, RemoteException {
		registry.bind("Calculator", objectExported);
		registered = true;
	}
	
	/**
	 * Enlève le calculateur du RMIRegistry. Ne fait rien si aucun enregistrement n'a été effectué.
	 * @throws AccessException Si le registry est local et s'il bloque l'accès pour effectuer l'opération
	 * @throws NotBoundException Si l'objet n'était pas dans le registry.
	 * @throws RemoteException Si la communication avec le registre échoue.
	 */
	public void unregisterToRMIRegistry() throws AccessException, NotBoundException, RemoteException {
		if(registered) {
			registry.unbind("Calculator");
			registered = false;
		}
	}
	
	/**
	 * Le point d'entrée du programme pour démarrer le calculateur.
	 * @param args 3 arguments sont attendus. Le premier doit être le nombre d'opérations qu'il accepte toujours,
	 * le deuxième est le pourcentage de temps (entre 0 et 100 inclusivement) que le calculateur retourne un
	 * mauvais résultat et le troisième est l'hôte où il faut s'enregistrer.
	 */
	public static void main(String[] args) {
		try {
			calculatorContainer = new CalculatorContainer(4, 0, "127.0.0.1");
		}
		catch (RemoteException e) {
			System.out.println("Impossible de créer le CalculatorContainer. " + e.getMessage());
			e.printStackTrace();
			System.exit(2);
		}
		
		boolean registerNameFound = false;
		int tryNumber = 0;
		while(!registerNameFound && tryNumber < 3) {
			try {
				calculatorContainer.registerToRMIRegistry();
				registerNameFound = true;
			}
			catch(AccessException e) {
				System.out.println("Accès refusé pour communiquer avec l'hôte. " + e.getMessage());
				e.printStackTrace();
				cleanupAndExit(3);
			}
			catch(AlreadyBoundException e) {
				System.out.println("Le nom pour l'enregistrement a déjà été choisi. Essai d'un autre nom.");
			}
			catch(RemoteException e) {
				System.out.println("Impossible de communiquer avec le serveur. " + e.getMessage());
				e.printStackTrace();
				cleanupAndExit(4);
			}
			++tryNumber;
		}
		if(!registerNameFound) {
			System.out.println("Impossible de trouver un nom non utilisé pour l'enregistrement.");
			cleanupAndExit(5);
		}
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				cleanup();
			}
		});
		System.out.println("Calculateur prêt!");
	}
	
	/**
	 * Effectue le cleanup et quitte le programme. Si le cleanup s'est bien effectué, le exitcode est
	 * utilisé pour quitter le programme. Si le cleanup ne s'est pas bien effectué, 1 est utilisé pour
	 * quitter le programme.
	 * @param exitCode Le code de retour du programme.
	 */
	private static void cleanupAndExit(int exitCode) {
		if(cleanup()) {
			System.exit(exitCode);
		}
		else {
			System.exit(1);
		}
	}
	
	/**
	 * Enlève l'enregistrement du RMIRegistry et enlève l'exportation du Calculator.
	 * @return Vrai si le cleanup s'est effectué correctement, faux si une erreur est survenue.
	 */
	private static boolean cleanup() {
		boolean cleanupSucceded;
		try {
			calculatorContainer.unregisterToRMIRegistry();
			calculatorContainer.doCleanup();
			cleanupSucceded = true;
		}
		catch(NoSuchObjectException e) {
			System.out.println("Impossible d'enlever l'exportation du calculateur. Fin du programme. " + e.getMessage());
			cleanupSucceded = false;
		} catch (AccessException e) {
			System.out.println("L'opération pour enlever l'objet du registre n'a pas été permise. Fin du programme. " + e.getMessage());
			cleanupSucceded = false;
		} catch (RemoteException e) {
			System.out.println("Impossible de communiquer avec le registre pour le nettoyage. Fin du programme. " + e.getMessage());
			cleanupSucceded = false;
		} catch (NotBoundException e) {
			System.out.println("L'objet avait déjà été enlevé du registry. Fin du programme. " + e.getMessage());
			cleanupSucceded = false;
		}
		return cleanupSucceded;
	}
}
