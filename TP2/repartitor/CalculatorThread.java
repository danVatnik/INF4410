package repartitor;

import java.rmi.RemoteException;
import java.util.Collection;

import shared.CalculationOperations;
import shared.Operation;
import threadNotifier.IThreadNotifier;

public class CalculatorThread extends Thread implements IThreadNotifier {
	
	private final Operation[] operations; 
	private int resultats;
	private final CalculationOperations calculatorCaller;
	private Throwable exceptionReceived = null;
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
			exceptionReceived = e.getCause();
		}
		
		synchronized (finishedThreads) {
			finishedThreads.add(this);
			finishedThreads.notify();
		}
	}
	
	public int getResults(){
		return resultats;
	}
	
	public Operation[] getOperations(){
		return operations;
	}
	
	public Throwable getExceptionThrown(){
		return exceptionReceived;
	}
	
	public CalculationOperations getCalculatorCaller(){
		return calculatorCaller;
	}
}
