package shared;

public class Prime extends Operation {
	
	public Prime(int operand) {
		super(operand);
	}
	
	public int performOperation() {
		return prime(operand);
	}
	
	public int prime(int number) throws CalculatorOccupiedException {
		int highestPrime = 0;
		
		for (int i = 1; i <= number; ++i)
		{
			if (isPrime(i) && number % i == 0 && i > highestPrime)
				highestPrime = i;
		}
		
		return highestPrime;
	}
	
	private boolean isPrime(int x) {
		if (x <= 1)
			return false;

		for (int i = 2; i < x; ++i)
		{
			if (x % i == 0)
				return false;
		}
		
		return true;		
	}
}
