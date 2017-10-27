package shared;

import java.io.Serializable;

public abstract class Operation implements Serializable{
	protected int operand;
	
	public Operation(int operand) {
		this.operand = operand;
	}
	
	public abstract int performOperation() throws CalculatorOccupiedException;
}
