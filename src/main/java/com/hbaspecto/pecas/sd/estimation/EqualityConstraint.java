package com.hbaspecto.pecas.sd.estimation;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

/**
 * A constraint that forces two parameters to be equal. The penalty function is
 * <i>h</i><sup>2</sup>/sqrt(<i>a</i>), where <i>a</i> is the looseness parameter
 * and <i>h</i> is the distance between the two parameters.
 * @author Graham
 *
 */
public class EqualityConstraint implements Constraint {
    
    private int paramIndex1;
    private int paramIndex2;
    
    public EqualityConstraint(int param1, int param2) {
        paramIndex1 = param1;
        paramIndex2 = param2;
    }
    
    @Override
    public double getPenaltyFunction(Vector params, double looseness) {
        double param1 = params.get(paramIndex1);
        double param2 = params.get(paramIndex2);
        double diff = param1 - param2;
        return diff * diff / Math.sqrt(looseness);
    }

    @Override
    public Vector getPenaltyFunctionGradient(Vector params, double looseness) {
        double param1 = params.get(paramIndex1);
        double param2 = params.get(paramIndex2);
        double diff = param1 - param2;
        Vector result = new DenseVector(params.size());
        double deriv = 2 * diff / Math.sqrt(looseness);
        result.set(paramIndex1, deriv);
        result.set(paramIndex2, -deriv);
        return result;
    }

    @Override
    public Matrix getPenaltyFunctionHessian(Vector params, double looseness) {
        Matrix result = new DenseMatrix(params.size(), params.size());
        double deriv = 2 / Math.sqrt(looseness);
        result.set(paramIndex1, paramIndex1, deriv);
        result.set(paramIndex1, paramIndex2, -deriv);
        result.set(paramIndex2, paramIndex1, -deriv);
        result.set(paramIndex2, paramIndex2, deriv);
        return result;
    }

}
