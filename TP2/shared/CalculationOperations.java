package shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CalculationOperations extends Remote {
	
	public final static String CALCULATOR_PREFIX = "Calculator";
	
	public int[] calculate(Operation[] operations) throws CalculatorOccupiedException, RemoteException;
	
	public int getNumberOfOperationsSupported() throws RemoteException;
}
