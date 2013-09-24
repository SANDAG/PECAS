package com.hbaspecto.pecas.sd.estimation;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;
import org.apache.log4j.Logger;

public interface ObjectiveFunction
{
    /**
     * Computes the value of the objective function at the given parameter
     * values.
     * 
     * @param params
     *            The parameter values.
     * @return The value of the objective function.
     */
    double getValue() throws OptimizationException;

    /**
     * Computes the gradient of the objective function at the given parameter
     * values, with respect to those parameter values.
     * 
     * @param params
     *            The parameter values.
     * @return The gradient of the objective function.
     */
    Vector getGradient(Vector params) throws OptimizationException;

    /**
     * Computes the Hessian (or an approximation to it) of the objective
     * function at the given parameter values, with respect to those parameter
     * values.
     * 
     * @param params
     *            The parameter values.
     * @return The Hessian of the objective function.
     */
    Matrix getHessian(Vector params) throws OptimizationException;

    void logCurrentValues(Logger logger);

    void logParameters(Logger logger);

    void logTargetAndObjective(Logger logger);

    void setParameterValues(Vector params);

}
