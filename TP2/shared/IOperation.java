package shared;

import java.io.Serializable;

public interface IOperation extends Serializable {
	/**
	 * Effectue l'opération demandée.
	 * @return Le résultat de l'opération.
	 * @throws CalculatorOccupiedException Exception lancée si le calculateur n'est pas en mesure d'effectuer le calcul.
	 */
	int performOperation() throws CalculatorOccupiedException;
}
