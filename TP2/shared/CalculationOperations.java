package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CalculationOperations extends Remote {
	
	int pell(int number) throws CalculatorOccupiedException, RemoteException;
	
	int prime(int number) throws CalculatorOccupiedException, RemoteException;
}
