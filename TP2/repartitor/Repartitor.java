package repartitor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import shared.Operation;

public class Repartitor {
	
	private final Registry registryCreated;
	
	public Repartitor() throws RemoteException {
		registryCreated = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
	}
	
	public void calculateOperations(BufferedReader operationsToDo) {
		String[] calculatorList;
		try {
			calculatorList = registryCreated.list();
			for(String calculatorName : calculatorList) {
				System.out.println(calculatorName);
			}
		}
		catch(AccessException e) {
			System.out.println("Impossible d'obtenir la liste des éléments enregistrés dans le RMIRegistry. Accès refusé. " + e.getMessage());
			System.exit(2);
		}
		catch(RemoteException e) {
			System.out.println("Erreur de communication avec le RMIRegistry. " + e.getMessage());
			System.exit(3);
		}
	}
	
	private Operation[] transformInputToOperations() {
		return null;
	}
	
	public static void main(String[] args) {
		Repartitor repartitor = null;
		try {
			repartitor = new Repartitor();
			System.out.println("Répartiteur créé. Prêt pour commencer la répartition de calculs.");
		}
		catch(RemoteException e) {
			System.out.println("Impossible de créer le Répartiteur. " + e.getMessage());
			System.exit(1);
		}
		
		BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
		try {
			askForOperationsToExecute(consoleReader, System.out, repartitor);
		}
		catch(FileNotFoundException e) {
			System.out.println("Le fichier entré n'a pas été trouvé.");
		}
		catch(IOException e) {
			System.out.println("Une erreur est survenue lors de la lecture d'un flux d'entrée (console ou fichier). Fin du programme. " + e.getMessage());
			System.exit(2);
		}
	}
	
	private static void askForOperationsToExecute(BufferedReader consoleReader, PrintStream output, Repartitor repartitor) throws FileNotFoundException, IOException {
		output.print("Entrez le nom du fichier contenant les opérations à exécuter : ");
		String line;
		line = consoleReader.readLine();
		BufferedReader operationsToRead = new BufferedReader(new InputStreamReader(new FileInputStream(line)));
		repartitor.calculateOperations(operationsToRead);
	}
}
