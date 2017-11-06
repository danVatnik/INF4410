package repartitor;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.LinkedList;

import shared.CalculationOperations;
import shared.IOperation;
import threadNotifier.IThreadNotifier;

public class SafeRepartitor extends Repartitor {
	
	private HashSet<CalculatorThread> threads = new HashSet<>();
	private LinkedList<IThreadNotifier> finishedThreads = new LinkedList<>();
	
	public SafeRepartitor() throws AlreadyBoundException, RemoteException {
		super();
	}
	
	@Override
	protected boolean haveEnoughtCalculators(int numberOfCalculators) {
		return numberOfCalculators > 0;
	}
	
	@Override
	protected void launchACalculation() throws ResultError {
		CalculationOperations currentCalculator = getACalculator();
		int currentCalculatorSupportedOps;
		try {
			currentCalculatorSupportedOps = currentCalculator.getNumberOfOperationsSupported();
		}
		catch(RemoteException e) {
			throw new ResultError(currentCalculator, e);
		}
		IOperation[] currentOps = retrieveSomeOperationsFromStack(currentCalculatorSupportedOps + 1);
		
		CalculatorThread calculatorThread = new CalculatorThread(currentOps, currentCalculator);
		calculatorThread.setFinishedCollection(finishedThreads);
		threads.add(calculatorThread);
		calculatorThread.start();
	}
	
	@Override
	protected boolean haveWaitingResults() {
		return threads.size() != 0;
	}
	
	@Override
	protected int getResult() throws ResultError, InterruptedException {
		Integer threadResult = null;
		synchronized(finishedThreads) {
			if(finishedThreads.size() > 0) {
				threadResult = treatFinishedThreads();
			}
			else {
				finishedThreads.wait();
				threadResult = treatFinishedThreads();
			}
		}
		return threadResult.intValue();
	}
	
	private Integer treatFinishedThreads() throws ResultError {
		CalculatorThread thread = (CalculatorThread)finishedThreads.getFirst();
		finishedThreads.removeFirst();
		threads.remove(thread);
		if(thread.getExceptionThrown() != null) {
			putSomeOperationsOnStack(thread.getOperations());
			throw new ResultError(thread.getCalculatorCaller(), thread.getExceptionThrown());
		}
		return thread.getResults();
	}
}
