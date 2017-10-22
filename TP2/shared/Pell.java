package shared;

public class Pell extends Operation {

	public Pell(int operand) {
		super(operand);
	}
	
	public int performOperation() {
		return pell(operand);
	}
	
	private int pell(int number) {
		if (operand == 0)
			return 0;
		if (operand == 1)
			return 1;
		return 2 * pell(number - 1) + pell(number - 2);
	}
}
