package shared;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RepartitorRegistering extends Remote {
	String REPARTITOR_NAME = "Repartitor";
	
	/**
	 * Ajoute un calculateur au registre du répartiteur.
	 * @param bindName Le nom du calculateur à ajouter. Le nom doit commencer par la constante CalculationOperations.CALCULATOR_PREFIX.
	 * @param objectToBind The calculator to add.
	 * @throws AlreadyBoundException Si un calculateur du même nom a déjà été ajouté.
	 * @throws RemoteException Si une erreur survient lors de l'ajout au Registry ou si une erreur survient lors de l'appel à distance.
	 */
	void bindSomething(String bindName, Remote objectToBind) throws AlreadyBoundException, RemoteException;
	
	/**
	 * Enlève un calculateur du registre du répartiteur.
	 * @param nameToRemove Le nom du calculateur à enlever.
	 * @throws NotBoundException S'il n'y a pas de calculateur associé au nom.
	 * @throws RemoteException Si une erreur survient lors du retrait au Registry ou si une erreur survient lors de l'appel à distance.
	 */
	void unbindSomething(String nameToRemove) throws NotBoundException, RemoteException;
}
