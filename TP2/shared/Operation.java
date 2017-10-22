package shared;

public abstract class Operation {
	protected int operand;
	
	public Operation(int operand) {
		this.operand = operand;
	}
	
	public abstract int performOperation();
}
