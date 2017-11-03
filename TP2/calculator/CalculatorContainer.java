package calculator;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UID;
import java.rmi.server.UnicastRemoteObject;

import shared.RepartitorRegistering;
import shared.CalculationOperations;

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
	private static final String INVALID_NB_OPERATIONS = "Le nombre d'opérations n'est pas un entier positif.";
	private static final String INVALID_MALICIOUS_PERCENT = "Le pourcentage malicieux doit être entre 0 et 100 inclusivement.";
	private static final int INVALID_PARAM = 1;
	private static final int INVALID_OBJECT = 2;
	private static final int INVALID_ACCESS = 3;
	private static final int INVALID_REGISTER_NAME = 4;
	private static final int REMOTE_ERROR = 5;
	private static final int NUMBER_OF_TRIES = 3;
	
	private static CalculatorContainer calculatorContainer;
	private static boolean cleanRMIRegistry = false;
	private static boolean cleanObjectRegister = false;
	
	private final Calculator calculator;
	private final Remote objectExported;
	private Registry registry;
	private String bindName;
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
	 * Pour s'enregistrer, le calculateur envoie son objet exporté au répartiteur qui l'ajoutera au RMIRegistry.
	 * @throws AccessException Si le registry est local et s'il bloque l'accès pour effectuer l'opération.
	 * @throws AlreadyBoundException Si le nom pour l'enregistrement est déjà utilisé.
	 * @throws RemoteException Si la communication avec le registre échoue.
	 */
	public void registerToRMIRegistry() throws AccessException, NotBoundException, AlreadyBoundException, RemoteException {
		if(!registered) {
			bindName = CalculationOperations.CALCULATOR_PREFIX + new UID().toString();
			RepartitorRegistering repartitor = (RepartitorRegistering)registry.lookup(RepartitorRegistering.REPARTITOR_NAME);
			repartitor.bindSomething(bindName, objectExported);
			registered = true;
		}
	}
	
	/**
	 * Enlève le calculateur du RMIRegistry. Ne fait rien si aucun enregistrement n'a été effectué.
	 * @throws AccessException Si le registry est local et s'il bloque l'accès pour effectuer l'opération
	 * @throws NotBoundException Si l'objet n'était pas dans le registry.
	 * @throws RemoteException Si la communication avec le registre échoue.
	 */
	public void unregisterToRMIRegistry() throws AccessException, NotBoundException, RemoteException {
		if(registered) {
			RepartitorRegistering repartitor = (RepartitorRegistering)registry.lookup(RepartitorRegistering.REPARTITOR_NAME);
			repartitor.unbindSomething(bindName);
			registered = false;
		}
	}
	
	/**
	 * Vérifie auprès du RMIRegistry si l'objet précédement exporté est encore présent. Il s'agit de faire un lookup et de vérifier
	 * si l'ojet retourné est le même que l'objet conservé dans la classe. Dans le cas où le RMIRegistry aurait été arrêté et reparti, pour
	 * pouvoir enregistrer à nouveau l'objet, cette méthode doit être appelée pour remettre l'état de registered à false.
	 * @return Vrai si l'objet précédement exporté est encore présent, faux si l'objet n'est plus dans le RMIRegistry.
	 * @throws AlreadyBoundException Si un objet existe sous le nom auquel il a été attaché, mais que ce n'est pas le même objet que celui
	 * qui a été exporté. 
	 * @throws AccessException Si l'accès au RMIRegistry a été refusé.
	 * @throws RemoteException Si une erreur de communication survient.
	 */
	public boolean validateIfObjectStillInRMIRegistry() throws AlreadyBoundException, AccessException, RemoteException {
		if(registered) {
			try {
				Remote calculatorPreviouslyExported = registry.lookup(bindName);
				if(calculatorPreviouslyExported.toString().equals(objectExported.toString())) {
					throw new AlreadyBoundException();
				}
			}
			catch(NotBoundException e) {
				registered = false;
			}
		}
		return registered;
	}
	
	/**
	 * Le point d'entrée du programme pour démarrer le calculateur.
	 * @param args 3 arguments sont attendus. Le premier doit être le nombre d'opérations qu'il accepte toujours,
	 * le deuxième est le pourcentage de temps (entre 0 et 100 inclusivement) que le calculateur retourne un
	 * mauvais résultat et le troisième est l'hôte où il faut s'enregistrer.
	 */
	public static void main(String[] args) throws InterruptedException {
		/*
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}
		System.getSecurityManager().checkConnect("132.207.213.114", Registry.REGISTRY_PORT);
		*/
		
		int nbOfOperationsToAccept = 0;
		float maliciousPercent = 0;
		String hostName = null;
		
		if(args.length != 3) {
			System.out.println("Nombre invalide de paramètres entrés.");
			showUsage();
			System.exit(INVALID_PARAM);
		}
		else {
			try {
				nbOfOperationsToAccept = Integer.parseInt(args[0]);
				if(nbOfOperationsToAccept <= 0) {
					System.out.println(INVALID_NB_OPERATIONS);
					showUsage();
					System.exit(INVALID_PARAM);
				}
			}
			catch(NumberFormatException e) {
				System.out.println(INVALID_NB_OPERATIONS);
				System.exit(INVALID_PARAM);
			}
			
			try {
				maliciousPercent = Float.parseFloat(args[1]);
				if(maliciousPercent < 0 || maliciousPercent > 100) {
					System.out.println(INVALID_MALICIOUS_PERCENT);
					System.exit(INVALID_PARAM);
				}
			}
			catch(NumberFormatException e) {
				System.out.println(INVALID_MALICIOUS_PERCENT);
				System.exit(INVALID_PARAM);
			}
			hostName = args[2];
		}
		
		try {
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					cleanup();
				}
			});
			calculatorContainer = new CalculatorContainer(nbOfOperationsToAccept, maliciousPercent, hostName);
			cleanObjectRegister = true;
		}
		catch (RemoteException e) {
			System.out.println("Impossible de créer le CalculatorContainer. " + e.getMessage());
			e.printStackTrace();
			System.exit(REMOTE_ERROR);
		}
		
		try {
			tryToRegister(NUMBER_OF_TRIES);
			cleanRMIRegistry = true;
			System.out.println("Calculateur prêt!");
			
			while(true) {
				Thread.sleep(5000);
				verifyCalculatorStillRegistered();
			}
		}
		catch(AccessException e) {
			System.out.println("Accès refusé pour communiquer avec le registre. " + e.getMessage());
			System.exit(INVALID_ACCESS);
		}
		catch(RemoteException e) {
			cleanRMIRegistry = false;
			System.out.println("Impossible de communiquer avec le RMIRegistry. " + e.getMessage());
			System.exit(REMOTE_ERROR);
		}
	}
	
	/**
	 * Enlève l'enregistrement du RMIRegistry et enlève l'exportation du Calculator.
	 */
	private static void cleanup() {
		if(cleanRMIRegistry) {
			try {
				calculatorContainer.unregisterToRMIRegistry();
			} catch (AccessException e) {
				System.out.println("L'opération pour enlever l'objet du registre n'a pas été permise. " + e.getMessage());
			} catch (RemoteException e) {
				System.out.println("Impossible de communiquer avec le registre pour le nettoyage. " + e.getMessage());
			} catch (NotBoundException e) {
				System.out.println("L'objet avait déjà été enlevé du registry. " + e.getMessage());
			}
		}

		if(cleanObjectRegister) {
			try {
				calculatorContainer.doCleanup();
			} catch(NoSuchObjectException e) {
				System.out.println("La désexportation du calculateur a déjà eu lieu. " + e.getMessage());
			}
		}
	}
	
	/**
	 * Affiche comment utiliser le calculateur avec les arguments qu'il faut lui passer.
	 */
	private static void showUsage() {
		System.out.println("Usage : calculatorContainer nbOperationsToAccept maliciousPercent hostname");
	}
	
	/**
	 * Essaie de s'enregistrer auprès du RMIRegistry ou quitte le programme en cas d'échec.
	 * @param numberOfTries Le nombre d'essaies à effectuer si jamais le nom qui est essayé est déjà pris.
	 * @throws AccessException Si l'accès au RMIRegistry a été refusé.
	 * @throws RemoteException Si une erreur de communication avec le RMIRegistry survient.
	 */
	private static void tryToRegister(int numberOfTries) throws AccessException, RemoteException {
		boolean registerNameFound = false;
		int tryNumber = 0;
		while(!registerNameFound) {
			try {
				calculatorContainer.registerToRMIRegistry();
				registerNameFound = true;
			}
			catch(NotBoundException e) {
				System.out.println("Le répartiteur n'existe pas dans le RMIRegistry.");
				System.exit(INVALID_REGISTER_NAME);
			}
			catch(AlreadyBoundException e) {
				if(tryNumber < numberOfTries) {
					System.out.println("Le nom pour l'enregistrement a déjà été choisi. Essai d'un autre nom.");
					++tryNumber;
				}
				else {
					System.out.println("Impossible de trouver un nom non utilisé pour l'enregistrement.");
					System.exit(INVALID_REGISTER_NAME);
				}
			}
		}
	}
	
	/**
	 * Vérifie si le calculateur est toujours enregistré au RMIRegistry. Si le calculateur n'est plus enregistré, alors il tente de
	 * s'enregistrer à nouveau. Si un objet du même nom a été enregistré dans le RMIRegistry entre temps ou s'il n'arrive pas à
	 * communiquer avec le RMIRegistry, alors le calculateur se termine.
	 * @throws AccessException Si l'accès au RMIRegistry a été refusé.
	 * @throws RemoteException Si une erreur de communication avec le RMIRegistry survient.
	 */
	private static void verifyCalculatorStillRegistered() throws AccessException, RemoteException {
		try {
			if(!calculatorContainer.validateIfObjectStillInRMIRegistry()) {
				tryToRegister(NUMBER_OF_TRIES);
			}
		}
		catch(AlreadyBoundException e) {
			System.out.println("Un autre objet du même nom a remplacé le calculateur.");
			cleanRMIRegistry = false;
			System.exit(INVALID_OBJECT);
		}
	}
}
