package repartitor;

import shared.CalculationOperations;

public class ResultError extends Exception {
	private CalculationOperations invalidCalculator;
	
	public ResultError(CalculationOperations calculator) {
		invalidCalculator = calculator;
	}
	
	public ResultError(CalculationOperations calculator, Throwable cause) {
		super(cause);
		invalidCalculator = calculator;
	}
	
	public CalculationOperations getInvalidCalculator() {
		return invalidCalculator;
	}
}
