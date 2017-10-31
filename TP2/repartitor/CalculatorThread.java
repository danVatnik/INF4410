package repartitor;

import java.rmi.RemoteException;
import java.util.Collection;

import shared.CalculationOperations;
import shared.CalculatorOccupiedException;
import shared.Operation;

public class CalculatorThread extends Thread {
	
	private final Operation[] operations; 
	private Integer resultats;
	private final CalculationOperations calculatorCaller;
	private final Collection<CalculatorThread> finishedThreads;
	private boolean calculatorDead = false;
	
	public CalculatorThread(Operation[] ops, CalculationOperations calculatorCaller, Collection<CalculatorThread> finishedThreads) {
		if(ops == null || calculatorCaller == null || finishedThreads == null) {
			throw new NullPointerException("A parameter for the CalculatorThread is null.");
		}
		operations = ops;
		this.calculatorCaller = calculatorCaller;
		this.finishedThreads = finishedThreads;
	}
	
	public void run(){
		try {
			resultats = calculatorCaller.calculate(operations);
		}
		catch (RemoteException e) {
			Throwable cause = e.getCause();
			if(!(cause instanceof CalculatorOccupiedException)) {
				calculatorDead = true;
			}
		}

		synchronized (finishedThreads) {
			finishedThreads.add(this);
			finishedThreads.notify();
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
