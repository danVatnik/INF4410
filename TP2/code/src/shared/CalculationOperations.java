package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CalculationOperations extends Remote {
	
	public final static String CALCULATOR_PREFIX = "Calculator";
	
	/**
	 * Effectue les calculs d'un ensemble d'opérations.
	 * @param operations Les opérations à effectuer.
	 * @return Le résultat des opérations.
	 * @throws CalculatorOccupiedException Si le calculateur n'est pas en mesure d'effectuer les calculs.
	 * @throws RemoteException Si une erreur réseau survient lors de l'appel à la méthode.
	 */
	public int calculate(IOperation[] operations) throws CalculatorOccupiedException, RemoteException;
	
	/**
	 * Indique le nombre d'opérations que le calculateur peut supporter avant de commencer à refuser les calculs.
	 * @return Le nombre d'opérations que le calculateur supporte.
	 * @throws RemoteException Si une erreur réseau survient lors de l'appel à la méthode.
	 */
	public int getNumberOfOperationsSupported() throws RemoteException;
}
