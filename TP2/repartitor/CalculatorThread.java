package repartitor;

import java.rmi.RemoteException;

import shared.CalculationOperations;
import shared.CalculatorOccupiedException;
import shared.Operation;

public class CalculatorThread extends Thread {
	
	private final Operation[] operations; 
	private Integer resultats;
	private final CalculationOperations calculatorCaller;
	private boolean calculatorDead = false;
	
	public CalculatorThread(Operation[] ops, CalculationOperations calculatorCaller){
		operations = ops;
		this.calculatorCaller = calculatorCaller;
	}
	
	public void run(){
		try {
			resultats = calculatorCaller.calculate(operations);
		}
		catch (RemoteException e) {
			Throwable cause = e.getCause();
			if(!(cause instanceof CalculatorOccupiedException)) {
				calculatorDead = true;
				e.printStackTrace();
			}
		}
	}
	
	public Integer getResults(){
		return resultats;
	}
	
	public Operation[] getOperations(){
		return operations;
	}
	
	public boolean getCalculatorDead(){
		return calculatorDead;
	}
	
	public CalculationOperations getCalculatorCaller(){
		return calculatorCaller;
	}
}
