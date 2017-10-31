package repartitor;

import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.LinkedList;

import shared.CalculationOperations;
import shared.Operation;

public class SafeRepartitor extends Repartitor {
	
	private HashSet<CalculatorThread> threads = new HashSet<>();
	private LinkedList<CalculatorThread> finishedThreads = new LinkedList<>();
	
	public SafeRepartitor() throws RemoteException {
		super();
	}
	
	@Override
	protected boolean haveEnoughtCalculators(int numberOfCalculators) {
		return numberOfCalculators > 0;
	}
	
	@Override
	protected void launchACalculation() throws InvalidCalculator {
		CalculationOperations currentCalculator = getACalculator();
		int currentCalculatorSupportedOps;
		try {
			currentCalculatorSupportedOps = currentCalculator.getNumberOfOperationsSupported();
		}
		catch(RemoteException e) {
			throw new InvalidCalculator(currentCalculator);
		}
		Operation[] currentOps = retrieveSomeOperationsFromStack(currentCalculatorSupportedOps + 1);
		
		CalculatorThread calculatorThread = new CalculatorThread(currentOps, currentCalculator, finishedThreads);
		threads.add(calculatorThread);
		calculatorThread.start();
	}
	
	@Override
	protected boolean haveWaitingResults() {
		return threads.size() != 0;
	}
	
	@Override
	protected int getResult() throws InvalidCalculator, ResultError, InterruptedException {
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
	
	private Integer treatFinishedThreads() throws InvalidCalculator, ResultError {
		Integer threadResult = null;
		CalculatorThread thread = finishedThreads.getFirst();
		finishedThreads.removeFirst();
		threads.remove(thread);
		if(thread.getCalculatorDead()) {
			putSomeOperationsOnStack(thread.getOperations());
			throw new InvalidCalculator(thread.getCalculatorCaller());
		}

		threadResult = thread.getResults();
		if(threadResult == null) {
			putSomeOperationsOnStack(thread.getOperations());
			throw new ResultError();
		}
		return threadResult;
	}
}
