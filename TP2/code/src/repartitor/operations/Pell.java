package repartitor.operations;

import shared.CalculatorOccupiedException;

public class Pell extends Operation {

	public Pell(int operand) {
		super(operand);
	}
	
	public int performOperation() throws CalculatorOccupiedException {
		return pell(operand);
	}
	
	private int pell(int number) {
		if (number == 0)
			return 0;
		if (number == 1)
			return 1;
		return 2 * pell(number - 1) + pell(number - 2);
	}
}
