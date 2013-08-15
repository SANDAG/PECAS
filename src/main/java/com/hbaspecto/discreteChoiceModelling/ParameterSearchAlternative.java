package com.hbaspecto.discreteChoiceModelling;

import java.util.List;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.NoAlternativeAvailable;
import com.hbaspecto.pecas.sd.estimation.ExpectedValue;

public interface ParameterSearchAlternative extends Alternative {
    /**
     * Returns a vector giving the expected value of each target measure
     * if this alternative is selected, in the same order as the
     * list of target measures given.
     * @param ts The list of target measures.
     * @return The vector of expected values.
     */
    public Vector getExpectedTargetValues(List<ExpectedValue> ts)
        throws NoAlternativeAvailable, ChoiceModelOverflowException;
    
    /**
     * Returns a vector giving the derivative of this alternative's
     * utility with respect to each parameter, in the same order as the
     * list of parameters given. If the utility does not depend on a
     * parameter in the list (for example, a constant for a different
     * alternative), then zero will be returned for the corresponding
     * derivative.
     * @param cs The list of parameters.
     * @return The vector of derivatives.
     */
	public Vector getUtilityDerivativesWRTParameters(List<Coefficient> cs)
	    throws NoAlternativeAvailable, ChoiceModelOverflowException;

	/**
	 * Returns a matrix giving the derivative of the expected
	 * value of each target measure with respect to each parameter, if this
	 * alternative is selected. The rows of the array represent the targets,
	 * and appear in the same order as the given list of target measures; the
	 * columns represent the parameters, and appear in the same order as the
	 * given list of measures. If any of the targets do not depend on a parameter
	 * in the list, then zero will be returned for the corresponding derivative.
	 * @param ts The list of target measures.
	 * @param cs The list of parameters.
	 * @return The matrix of derivatives.
	 */
	public Matrix getExpectedTargetDerivativesWRTParameters(List<ExpectedValue> ts, List<Coefficient> cs)
	    throws NoAlternativeAvailable, ChoiceModelOverflowException;
	
	/**
	 * Instructs the alternative to not recalculate values it has already calculated.
	 * The alternative will assume that no underlying properties that affect
	 * the calculations will change until <code>endCaching()</code> is called.
	 */
	public void startCaching();
	
	/**
	 * Instructs the alternative to resume recalculating values.
	 */
	public void endCaching();
}
