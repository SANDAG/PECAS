package com.hbaspecto.pecas.sd.estimation;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

/**
 * A constraint on the ordering of two parameters, enforcing an inequality between the two.
 * The penalty function for this constraint is <i>a</i>/<i>h</i>, where <i>a</i> is the
 * looseness parameter and <i>h</i> is the distance between the two parameters.
 * @author Graham
 *
 */
public class OrderingConstraint implements Constraint {
    
    private int paramIndex1;
    private int paramIndex2;
    // An integer that encodes the type of ordering.
    // Should be 1 for param1 > param2, -1 for param1 < param2.
    private int boundmod;
    
    /**
     * Constructs an <code>OrderingConstraint</code> with the given properties.
     * @param firstParam The index of the first parameter that the constraint will apply to.
     * @param secondParam The index of the second parameter that the constraint will apply to.
     * @param greaterThan True if the first parameter must be greater than the second;
     *        false if the first parameter must be less than the second.
     */
    public OrderingConstraint(int firstParam, int secondParam, boolean greaterThan) {
        paramIndex1 = firstParam;
        paramIndex2 = secondParam;
        boundmod = greaterThan? 1 : -1;
    }
    
    @Override
    public double getPenaltyFunction(Vector params, double looseness) {
        double param1 = params.get(paramIndex1);
        double param2 = params.get(paramIndex2);
        double diff = boundmod * (param1 - param2);
        if(diff <= 0)
            return Double.POSITIVE_INFINITY;
        else
            return looseness / diff;
    }

    @Override
    public Vector getPenaltyFunctionGradient(Vector params, double looseness) {
        double param1 = params.get(paramIndex1);
        double param2 = params.get(paramIndex2);
        double diff = boundmod * (param1 - param2);
        Vector result = new DenseVector(params.size());
        if(diff > 0) {
            double deriv = boundmod * looseness / (diff * diff);
            result.set(paramIndex1, -deriv);
            result.set(paramIndex2, deriv);
        }
        return result;
    }

    @Override
    public Matrix getPenaltyFunctionHessian(Vector params, double looseness) {
        double param1 = params.get(paramIndex1);
        double param2 = params.get(paramIndex2);
        double diff = boundmod * (param1 - param2);
        Matrix result = new DenseMatrix(params.size(), params.size());
        if(diff > 0) {
            double deriv = 2 * boundmod * boundmod * looseness / (diff * diff * diff);
            result.set(paramIndex1, paramIndex1, deriv);
            result.set(paramIndex1, paramIndex2, -deriv);
            result.set(paramIndex2, paramIndex1, -deriv);
            result.set(paramIndex2, paramIndex2, deriv);
        }
        return result;
    }

}
