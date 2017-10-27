package repartitor;

import shared.CalculationOperations;

public class InvalidCalculator extends Exception {
	
	private CalculationOperations invalidCalculator;
	
	public InvalidCalculator(CalculationOperations calculator) {
		invalidCalculator = calculator;
	}
	
	public CalculationOperations getInvalidCalculator() {
		return invalidCalculator;
	}
}
