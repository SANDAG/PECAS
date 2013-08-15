package com.hbaspecto.pecas.sd.estimation;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

/**
 * A constraint on the parameter values. Constraints provide a penalty function,
 * which takes small values for parameters that meet the constraint but increases
 * rapidly as the parameters start to approach values that do not meet the constraint.
 * The penalty function depends on a <i>looseness</i> parameter, which determines how
 * smooth the penalty function is; high values indicate a smooth penalty function,
 * low values make the penalty function sharper. A looseness of 0 imposes an ideal
 * penalty function, equal to 0 wherever the constraint is met and positive infinity elsewhere.
 * Each iteration should decrease this parameter.
 * @author Graham
 *
 */
public interface Constraint {
    
    /**
     * Returns the value of the penalty function at the given parameter values.
     * @param params The parameter values.
     * @param looseness The looseness parameter.
     * @return The value of the penalty function.
     */
    public double getPenaltyFunction(Vector params, double looseness);
    
    /**
     * Returns the gradient of the penalty function with respect to the parameters,
     * at the given parameter values.
     * @param params The parameter values.
     * @param looseness The looseness parameter.
     * @return The gradient of the penalty function.
     */
    public Vector getPenaltyFunctionGradient(Vector params, double looseness);
    
    /**
     * Returns the Hessian of the penalty function with respect to the parameters,
     * at the given parameter values.
     * @param params The parameter values.
     * @param looseness The looseness parameter.
     * @return The Hessian matrix.
     */
    public Matrix getPenaltyFunctionHessian(Vector params, double looseness);
}
