package repartitor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

import shared.CalculationOperations;
import shared.Operation;
import shared.Pell;
import shared.Prime;

/**
 * Un répartiteur qui crée un RMIRegistry et qui demande à l'utilisateur quel fichier contenant des opérations il faut calculer.
 * Le répartiteur tente d'utiliser les différents Calculateur qui se sont enregistrés auprès du RMIRegistry pour effectuer les
 * différentes opérations. Le répartiteur tient compte de différents facteurs pour l'assignation des opérations aux calculateurs.
 * Chaque calculateur a un nombre maximal d'opérations après lequel le calculateur se mettra à refuser de faire le calcul.
 * Les calculateurs peuvent se terminer abruptement sans avoir retourné un résultat.
 * En mode non sécurisé, les calculateurs peuvent retourner des résultats erronés.
 * @author dcourcel
 *
 */
public abstract class Repartitor {
	private static final int REGISTRY_CREATE_FAILED = 1;
	private static final int ACCESS_EXCEPTION_CODE = 2;
	private static final int REMOTE_EXCEPTION_CODE = 3;
	private static final int READ_FILE_EXCEPTION = 4;
	private static final int INVALID_PARAM = 5;
	private static final String INVALID_LINE = "Ligne invalide.";
	
	private final Registry registryCreated;
	private final Random randomCalculator = new Random();
	private ArrayList<CalculationOperations> calculatorList;
	private Stack<Operation> operations;
	private int lastCalculatorGiven;
	
	/**
	 * Crée un répartiteur et un RMIRegistry.
	 * @param secureMode Indique si le répartiteur peut faire confiance aux calculateurs (vrai) ou non (faux).
	 * @throws RemoteException S'il est impossible de créer le RMIRegistry.
	 */
	public Repartitor() throws RemoteException {
		registryCreated = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
	}
	
	/**
	 * Effectue le calcul des opérations lues dans operationsToDo. Après chaque opération, un modulo 4000 est appliqué pour éviter
	 * un débordement d'entier.
	 * @param operationsToDo Un buffer contenant les différentes opérations à effectuer.
	 * @return Le résultat du calcul.
	 * @throws IllegalStateException S'il n'y a aucun calculateur enregistré dans le RMIRegistry.
	 * @throws AccessException Si l'accès au RMIRegistry a été refusé.
	 * @throws RemoteException Si une erreur de communication survient avec le RMIRegistry.
	 */
	public int calculateOperations(BufferedReader operationsToDo) throws IllegalStateException, IOException {

		int resultat = 0;
		operations = transformInputToOperations(operationsToDo);
		calculatorList = findAvailableCalculators();
		
		while(!operations.isEmpty()){
			do {
				if(!haveEnoughtCalculators(calculatorList.size())) {
					throw new IllegalStateException("Plus assez de calculateurs.");
				}
				try {
					launchACalculation();
				}
				catch(InvalidCalculator e) {
					calculatorList.remove(e.getInvalidCalculator());
				}
			}
			while(!operations.isEmpty());
			
			boolean threadGotError = false;
			while(!threadGotError && haveWaitingResults()) {
				try {
					int threadResult = getResult();
					resultat = (resultat + threadResult) % 4000 ;
				}
				catch(ResultError e) {
					threadGotError = true;
				}
				catch(InvalidCalculator e) {
					calculatorList.remove(e.getInvalidCalculator());
					threadGotError = true;
				}
				catch(InterruptedException e) {
					System.out.println("Interruption catched.");
				}
			}
		}
		
		return resultat;
	}
	
	protected CalculationOperations getACalculator() {
		lastCalculatorGiven = randomCalculator.nextInt(calculatorList.size());
		return calculatorList.get(lastCalculatorGiven);
	}
	
	/**
	 * Dépile une partie des opérations de la pile d'opérations et retourne un tableau contenant ces opérations.
	 * @param numberOfOperationsToGet Le nombre d'opérations à dépiler.
	 * @return Un tableau d'opérations provenant de la pile de la taille du paramètre numberOfOperationsToGet ou de taille inférieure
	 * s'il restait moins d'opérations dans la pile à extraire.
	 */
	protected Operation[] retrieveSomeOperationsFromStack(int numberOfOperationsToGet) {
		Operation[] extractedOPs = new Operation[Math.min(numberOfOperationsToGet, operations.size())];
		
		for(int i = 0; i < extractedOPs.length; i++){
			extractedOPs[i] = operations.pop();
		}
		
		return extractedOPs;
	}
	
	/**
	 * Met des opérations de type Operation sur la pile pour qu'elles soient exécutées.
	 * @param objectsToPut Les objets à mettre sur la pile.
	 */
	protected void putSomeOperationsOnStack(Operation[] objectsToPut) {
		for(Operation operation : objectsToPut) {
			operations.push(operation);
		}
	}
	
	/**
	 * Vérifie s'il y a encore assez de calculateurs pour effectuer des calculs.
	 * @param numberOfCalculators Le nombre de calculateurs qu'il y a.
	 * @return Vrai s'il y a assez de calculateurs, faux sinon.
	 */
	protected abstract boolean haveEnoughtCalculators(int numberOfCalculators);
	
	/**
	 * Démarre un calcul. Cette méthode ne devrait pas être bloquante. Elle peut utiliser les méthodes protégées getACalculator et
	 * retrieveSomeOperationsFromStack.
	 * @throws RemoteException Cette méthode devrait lancer cette exception si le dernier calculateur qui a été récupéré n'est plus valide.
	 */
	protected abstract void launchACalculation() throws InvalidCalculator;
	
	/**
	 * Vérifie s'il y a des résultats qui ont été lancés avec launchACalculation, mais dont le résultat n'a pas encore été récupéré avec
	 * getResult.
	 * @return Vrai s'il y a des résultats non récupérés, faux sinon.
	 */
	protected abstract boolean haveWaitingResults();
	
	/**
	 * Récupère le résultat d'un calcul qui a été lancé. Cette méthode peut être bloquante. Si le résultat ne peut être récupéré, alors il
	 * est de la responsabilité de cette méthode de remettre les opérations non exécutées sur le stack avec putSomeOperationsOnStack.
	 * @return Le résultat d'un calcul.
	 * @throws InvalidCalculator Si un calculateur est rendu invalide et ne doit plus être utilisé.
	 * @throws ResultError S'il n'a pas été possible de calculer un résultat cette fois-ci, mais qu'il sera possible d'en obtenir un si on recommence.
	 * @throws Exception lancée si l'attente est interrompue par une interruption.
	 */
	protected abstract int getResult() throws InvalidCalculator, ResultError, InterruptedException;
	
	/**
	 * Lit le RMIRegistry pour trouver les calculateurs qui seront utilisés durant le calcul des opérations.
	 * @return Une liste des calculateurs qui sont actuellement disponibles.
	 * @throws AccessException Si l'accès au RMIRegistry a été refusé.
	 * @throws RemoteException Si une erreur de communication survient.
	 */
	private ArrayList<CalculationOperations> findAvailableCalculators() throws AccessException, RemoteException {
		ArrayList<CalculationOperations> calculatorList = new ArrayList<>();
		String[] calculatorString = registryCreated.list();
		for(int i = 0; i < calculatorString.length; i++) {
			try {
				Remote calculator =  registryCreated.lookup(calculatorString[i]);
				if(calculator instanceof CalculationOperations) {
					calculatorList.add((CalculationOperations)calculator);
				}
				else {
					System.out.println("Calculateur invalide.");
				}
			}
			catch(NotBoundException e){
				System.out.println("Le calculateur est inexistant. " + e.getMessage());
			}
		}
		return calculatorList;
	}
	
	/**
	 * Lit le buffer passé et extrait les opérations à faire.
	 * @param operationsToDo Le buffer à partir duquel il faut lire les opérations à faire.
	 * @return Une pile contenant les différentes opérations à faire extraires du buffer.
	 * @throws IOException Si un erreur survient lors de la lecture du buffer.
	 */
	private Stack<Operation> transformInputToOperations(BufferedReader operationsToDo) throws IOException{
		Stack<Operation> ops = new Stack<>();
		
		String line = operationsToDo.readLine();
		
		while(line != null){
			
			String[] args = line.split(" ");
			
			if(args.length == 2){
				try{
					int operande = Integer.parseInt(args[1]);
					if(operande >= 0){
						if(args[0].equals("prime")){
							ops.push(new Prime(operande));
						}else if(args[0].equals("pell")){
							ops.push(new Pell(operande));
						}else{
							System.out.println(INVALID_LINE);
						}
					}else{
						System.out.println(INVALID_LINE);
					}
				}catch( NumberFormatException e){
					System.out.println(INVALID_LINE);
				}
			}else{
				System.out.println(INVALID_LINE);
			}
			
			line = operationsToDo.readLine();
		}
		return ops;
	}
	
	/**
	 * Début d'exécution du répartiteur.
	 * @param args Contient l'argument 0 si le répartiteur fonctionne en mode sécurisé ou aucun argument si le répartiteur fonctionne en mode
	 * non sécurisé.
	 */
	public static void main(String[] args) {
		boolean isSecure = false;
		if(args.length == 1) {
			if(args[0].equals("0")) {
				isSecure = true;
			}
			else {
				System.out.println("Paramètre invalide. Usage : repartitor | repartitor 0");
				System.exit(INVALID_PARAM);
			}
		}
		else if(args.length == 0) {
			isSecure = false;
		}
		else {
			System.out.println("Paramètre invalide. Usage : repartitor | repartitor 0");
			System.exit(INVALID_PARAM);
		}
		
		Repartitor repartitor = null;
		try {
			if(isSecure) {
				repartitor = new SafeRepartitor();
			}
			else {
				repartitor = new UnsafeRepartitor();
			}
			System.out.println("Répartiteur créé. Prêt pour commencer la répartition de calculs.");
		}
		catch(RemoteException e) {
			System.out.println("Impossible de créer le Répartiteur. " + e.getMessage());
			System.exit(REGISTRY_CREATE_FAILED);
		}
		
		while(true) {
			BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
			try {
				askForOperationsToExecute(consoleReader, System.out, repartitor);
			}
			catch(FileNotFoundException e) {
				System.out.println("Le fichier entré n'a pas été trouvé.");
			}
			catch(IllegalStateException e) {
				System.out.println(e.getMessage());
			}
			catch(AccessException e) {
				System.out.println("L'accès au RMIRegistry a été refusé. " + e.getMessage());
				System.exit(ACCESS_EXCEPTION_CODE);
			}
			catch(RemoteException e) {
				System.out.println("Erreur de communication avec le RMIRegistry. " + e.getMessage());
				System.exit(REMOTE_EXCEPTION_CODE);
			}
			catch(IOException e) {
				System.out.println("Une erreur est survenue lors de la lecture d'un flux d'entrée (console ou fichier). " + e.getMessage());
				System.exit(READ_FILE_EXCEPTION);
			}
		}
	}
	
	/**
	 * Demande à l'utilisateur à la console le nom du fichier à lire pour y extraire les différentes opérations et lance le calcul des opérations.
	 * @param consoleReader Le lecteur à partir duquel l'utilisateur répondra.
	 * @param output Le flux de sortie pour afficher du texte.
	 * @param repartitor Le répartiteur à utiliser pour le calcul des opérations.
	 * @throws FileNotFoundException Si le fichier entré n'a pas été trouvé.
	 * @throws IOException Si une erreur survient lors de la lecture du fichier.
	 * @throws IllegalStateException S'il n'y a plus suffisament de calculateurs pour effectuer les calculs.
	 */
	private static void askForOperationsToExecute(BufferedReader consoleReader, PrintStream output, Repartitor repartitor) throws IllegalStateException, FileNotFoundException, IOException, AccessException, RemoteException {
		output.print("Entrez le nom du fichier contenant les opérations à exécuter : ");
		String line = consoleReader.readLine();
		BufferedReader operationsToRead = new BufferedReader(new InputStreamReader(new FileInputStream(line)));
		output.println(repartitor.calculateOperations(operationsToRead));
	}
}
