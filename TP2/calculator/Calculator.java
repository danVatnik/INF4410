package calculator;

import java.util.Random;

import shared.CalculationOperations;
import shared.CalculatorOccupiedException;
import shared.Operation;

class Calculator implements CalculationOperations {

	private final int nbOperationsToAccept;
	private final float maliciousPercent;
	
	public Calculator(int nbOperationsToAccept, float maliciousPercent) {
		this.nbOperationsToAccept = nbOperationsToAccept;
		this.maliciousPercent = maliciousPercent;
	}

	@Override
	public int calculate(Operation[] operations) throws CalculatorOccupiedException {
		System.out.println("Demande de calculs reçue.");
		Random random = new Random();
		if(operations.length > nbOperationsToAccept) {
			float occupiedPercent = (float)(operations.length - nbOperationsToAccept) / (5 * nbOperationsToAccept) * 100;
			if (occupiedPercent > random.nextFloat() * 100) {
				System.out.println("Demande de calculs refusée.");
				throw new CalculatorOccupiedException();
			}
		}
		
		int result = 0;
		for(int i = 0; i < operations.length; ++i) {
			result += operations[i].performOperation() % 4000;
		}
		
		if (maliciousPercent > random.nextFloat() * 100) {
			result += random.nextInt(1000) - 500;
		}
		
		return result;
	}

	@Override
	public int getNumberOfOperationsSupported() {
		return nbOperationsToAccept;
	}
}
