package calculator;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import shared.CalculationOperations;
import shared.CalculatorOccupiedException;

class Calculator implements CalculationOperations {

	private final int nbOperationsToAccept;
	private final float maliciousPercent;
	
	public Calculator(int nbOperationsToAccept, float maliciousPercent) {
		this.nbOperationsToAccept = nbOperationsToAccept;
		this.maliciousPercent = maliciousPercent;
	}
	
	public int pell(int number) throws CalculatorOccupiedException {
		return 0;
	}
	
	public int prime(int number) throws CalculatorOccupiedException {
		return 0;
	}
}
