package repartitor;

import java.rmi.RemoteException;
import java.util.Collection;

import shared.CalculationOperations;
import shared.CalculatorOccupiedException;
import shared.Operation;
import threadNotifier.IThreadNotifier;

public class CalculatorThread extends Thread implements IThreadNotifier {
	
	private final Operation[] operations; 
	private Integer resultats;
	private final CalculationOperations calculatorCaller;
	private boolean calculatorDead = false;
	private Collection<IThreadNotifier> finishedThreads;
	
	public CalculatorThread(Operation[] ops, CalculationOperations calculatorCaller) {
		if(ops == null || calculatorCaller == null) {
			throw new NullPointerException("A parameter for the CalculatorThread is null.");
		}
		operations = ops;
		this.calculatorCaller = calculatorCaller;
	}
	
	public void setFinishedCollection(Collection<IThreadNotifier> finishedThreads) {
		this.finishedThreads = finishedThreads;
	}
	
	@Override
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
