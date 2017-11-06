package repartitor.operations;

import shared.IOperation;

public abstract class Operation implements IOperation {
	protected int operand;
	
	public Operation(int operand) {
		this.operand = operand;
	}
}
