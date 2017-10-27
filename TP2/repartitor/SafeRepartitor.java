package repartitor;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.ListIterator;

import shared.CalculationOperations;
import shared.Operation;

public class SafeRepartitor extends Repartitor {
	
	public SafeRepartitor() throws RemoteException {
		super();
	}

	LinkedList<CalculatorThread> threads = new LinkedList<>();
	
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
		
		CalculatorThread calculatorThread = new CalculatorThread(currentOps, currentCalculator);
		threads.add(calculatorThread);
		calculatorThread.start();
	}
	
	protected boolean haveWaitingResults() {
		return threads.size() != 0;
	}
	
	protected int getResult() throws InvalidCalculator, ResultError {
		Integer threadResult = null;
		while(threadResult == null) {
			ListIterator<CalculatorThread> iter = threads.listIterator();
			while(iter.hasNext() && threadResult == null) {
				CalculatorThread thread = iter.next();
				if(thread.getState() == Thread.State.TERMINATED) {
					iter.remove();
					if(thread.getCalculatorDead()){
						putSomeOperationsOnStack(thread.getOperations());
						throw new InvalidCalculator(thread.getCalculatorCaller());
					}
					
					threadResult = thread.getResults();
					if(threadResult == null) {
						putSomeOperationsOnStack(thread.getOperations());
						throw new ResultError();
					}
				}
			}
		}
		return threadResult.intValue();
	}
}
