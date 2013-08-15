package com.hbaspecto.pecas.sd.estimation;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

/**
 * A restriction on a single parameter, enforcing an inequality of the parameter
 * and some fixed value. The penalty function for this constraint is <i>a</i>/<i>h</i>,
 * where <i>a</i> is the looseness parameter and <i>h</i> is the distance between the
 * parameter value and the boundary.
 * @author Graham
 *
 */
public class SimpleBoundary implements Constraint {
    
    private int paramIndex;
    private double boundingValue;
    // An integer that encodes the type of bound. Should be 1 for a lower bound, -1 for an upper bound.
    private int boundmod;
    
    /**
     * Constructs a <code>SimpleBoundary</code> with the given properties.
     * @param param The index of the parameter that this constraint will apply to.
     * @param value The bounding value for the parameter.
     * @param greaterThan True if the parameter must be greater than the bounding value;
     *        false if the parameter must be less than the bounding value.
     */
    public SimpleBoundary(int param, double value, boolean greaterThan) {
        paramIndex = param;
        boundingValue = value;
        boundmod = greaterThan? 1 : -1;
    }
    
    @Override
    public double getPenaltyFunction(Vector params, double looseness) {
        double param = params.get(paramIndex);
        double diff = boundmod * (param - boundingValue);
        if(diff <= 0)
            return Double.POSITIVE_INFINITY;
        else
            return looseness / diff;
    }

    @Override
    public Vector getPenaltyFunctionGradient(Vector params, double looseness) {
        double param = params.get(paramIndex);
        double diff = boundmod * (param - boundingValue);
        Vector result = new DenseVector(params.size());
        if(diff > 0)
            result.set(paramIndex, -boundmod * looseness / (diff * diff));
        return result;
    }

    @Override
    public Matrix getPenaltyFunctionHessian(Vector params, double looseness) {
        double param = params.get(paramIndex);
        double diff = boundmod * (param - boundingValue);
        Matrix result = new DenseMatrix(params.size(), params.size());
        if(diff > 0)
            result.set(paramIndex, paramIndex, 2 * boundmod * boundmod * looseness / (diff * diff * diff));
        return result;
    }

}
