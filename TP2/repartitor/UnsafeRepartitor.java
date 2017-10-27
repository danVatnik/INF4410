package repartitor;

import java.rmi.RemoteException;
import java.util.LinkedList;
import java.util.ListIterator;

import shared.CalculationOperations;
import shared.Operation;

public class UnsafeRepartitor extends Repartitor {

	LinkedList<CalculatorThread[]> threadPairs = new LinkedList<>();
	
	public UnsafeRepartitor() throws RemoteException {
		super();
	}

	@Override
	protected boolean haveEnoughtCalculators(int numberOfCalculators) {
		return numberOfCalculators > 1;
	}

	@Override
	protected void launchACalculation() throws InvalidCalculator {
		CalculationOperations currentCalculator1 = getACalculator();
		int nbOperationsSupported1;
		try {
			nbOperationsSupported1 = currentCalculator1.getNumberOfOperationsSupported();
		}
		catch(RemoteException e) {
			throw new InvalidCalculator(currentCalculator1);
		}
		CalculationOperations currentCalculator2;
		do
		{
			currentCalculator2 = getACalculator();
		}
		while(currentCalculator1 == currentCalculator2);
		int nbOperationsSupported2;
		try {
			nbOperationsSupported2 = currentCalculator2.getNumberOfOperationsSupported();
		}
		catch(RemoteException e) {
			throw new InvalidCalculator(currentCalculator2);
		}
		Operation[] currentOps = retrieveSomeOperationsFromStack(Math.min(nbOperationsSupported1, nbOperationsSupported2) + 1);
		CalculatorThread[] calculatorPair = new CalculatorThread[2]; 
		CalculatorThread calculatorThread = new CalculatorThread(currentOps, currentCalculator1);
		calculatorPair[0] = calculatorThread;
		calculatorThread.start();
		
		calculatorThread = new CalculatorThread(currentOps, currentCalculator2);
		calculatorPair[1] = calculatorThread;
		calculatorThread.start();
		
		threadPairs.add(calculatorPair);
	}

	@Override
	protected boolean haveWaitingResults() {
		return threadPairs.size() != 0;
	}

	@Override
	protected int getResult() throws InvalidCalculator, ResultError {
		Integer threadResult = null;
		while(threadResult == null) {
			ListIterator<CalculatorThread[]> iter = threadPairs.listIterator();
			while(iter.hasNext() && threadResult == null) {
				CalculatorThread[] calculatorPair = iter.next();
				if(calculatorPair[0].getState() == Thread.State.TERMINATED && calculatorPair[1].getState() == Thread.State.TERMINATED) {
					iter.remove();
					if(calculatorPair[0].getCalculatorDead()) {
						putSomeOperationsOnStack(calculatorPair[0].getOperations());
						throw new InvalidCalculator(calculatorPair[0].getCalculatorCaller());
					}
					else if(calculatorPair[1].getCalculatorDead()) {
						putSomeOperationsOnStack(calculatorPair[1].getOperations());
						throw new InvalidCalculator(calculatorPair[1].getCalculatorCaller());
					}
					
					Integer result1 = calculatorPair[0].getResults();
					if(result1 == null) {
						putSomeOperationsOnStack(calculatorPair[0].getOperations());
						throw new ResultError();
					}
					
					Integer result2 = calculatorPair[1].getResults();
					if(result2 == null) {
						putSomeOperationsOnStack(calculatorPair[1].getOperations());
						throw new ResultError();
					}
					
					if(!result1.equals(result2)) {
						putSomeOperationsOnStack(calculatorPair[1].getOperations());
						throw new ResultError();
					}
					threadResult = result1;
				}
			}
		}
		return threadResult.intValue();
	}

}
