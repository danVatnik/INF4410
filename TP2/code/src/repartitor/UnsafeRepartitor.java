package repartitor;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;

import shared.CalculationOperations;
import shared.IOperation;
import threadNotifier.NotifierHandler;

public class UnsafeRepartitor extends Repartitor {

	private final NotifierHandler<CalculatorThread> threadNotifier = new NotifierHandler<>();
	
	public UnsafeRepartitor() throws AlreadyBoundException, RemoteException {
		super();
	}

	@Override
	protected boolean haveEnoughtCalculators(int numberOfCalculators) {
		return numberOfCalculators > 1;
	}

	@Override
	protected void launchACalculation() throws ResultError {
		CalculationOperations currentCalculator1 = getACalculator();
		int nbOperationsSupported1;
		try {
			nbOperationsSupported1 = currentCalculator1.getNumberOfOperationsSupported();
		}
		catch(RemoteException e) {
			throw new ResultError(currentCalculator1, e.getCause());
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
			throw new ResultError(currentCalculator2, e.getCause());
		}
		IOperation[] currentOps = retrieveSomeOperationsFromStack(Math.min(nbOperationsSupported1, nbOperationsSupported2) + 1);
		CalculatorThread[] calculatorPair = new CalculatorThread[] {
				new CalculatorThread(currentOps, currentCalculator1),
				new CalculatorThread(currentOps, currentCalculator2)
		};
		threadNotifier.startNewThreads(calculatorPair);
	}

	@Override
	protected boolean haveWaitingResults() {
		return threadNotifier.haveUngetResults();
	}

	@Override
	protected int getResult() throws ResultError, InterruptedException {
		int result;
		CalculatorThread[] threadsFinished = threadNotifier.getFinishedThreadsPool();
		result = retrieveAndValidateResult(threadsFinished[0]);
		int i = 1;
		while(i < threadsFinished.length) {
			if(retrieveAndValidateResult(threadsFinished[i]) != result) {
				putSomeOperationsOnStack(threadsFinished[i].getOperations());
				throw new ResultError(threadsFinished[i].getCalculatorCaller());
			}
			++i;
		}
		return result;
	}

	private int retrieveAndValidateResult(CalculatorThread thread) throws ResultError {
		if(thread.getExceptionThrown() != null) {
			putSomeOperationsOnStack(thread.getOperations());
			throw new ResultError(thread.getCalculatorCaller(), thread.getExceptionThrown());
		}
		Integer result = thread.getResults();
		if(result == null) {
			putSomeOperationsOnStack(thread.getOperations());
			throw new ResultError(thread.getCalculatorCaller());
		}
		return result.intValue();
	}

}
