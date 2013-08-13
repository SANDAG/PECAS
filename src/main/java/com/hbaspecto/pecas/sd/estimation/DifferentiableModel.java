package com.hbaspecto.pecas.sd.estimation;

import java.util.List;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

public interface DifferentiableModel {
    /**
     * Finds the values of the specified list of targets at the given parameter values.
     * @param targets The list of targets, in the order in which their values should be returned.
     * @param params The parameter values.
     * @return The values of the targets.
     */
    public Vector getTargetValues(List<EstimationTarget> targets, Vector params) throws OptimizationException;
    
    /**
     * Returns the Jacobian matrix of the derivatives of each target value with respect to each
     * parameter. The rows of this matrix correspond to the targets, and are returned in
     * the same order as the given list of targets.
     * @param targets The list of targets.
     * @param params The parameter values.
     * @return The derivatives of the targets with respect to the parameter values.
     */
    public Matrix getJacobian(List<EstimationTarget> targets, Vector params) throws OptimizationException;
}
