package com.hbaspecto.pecas.sd.estimation;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

/**
 * A constraint that fixes a parameter at a given value. The penalty function is
 * <i>h</i><sup>2</sup>/sqrt(<i>a</i>), where <i>a</i> is the looseness parameter
 * and <i>h</i> is the distance between the parameter and the fixed value.
 * @author Graham
 *
 */
public class FixedParameter implements Constraint {
    
    private int paramIndex;
    private double fixedValue;
    
    public FixedParameter(int param, double value) {
        paramIndex = param;
        fixedValue = value;
    }
    
    @Override
    public double getPenaltyFunction(Vector params, double looseness) {
        double param = params.get(paramIndex);
        double diff = param - fixedValue;
        return diff * diff / Math.sqrt(looseness);
    }

    @Override
    public Vector getPenaltyFunctionGradient(Vector params, double looseness) {
        double param = params.get(paramIndex);
        double diff = param - fixedValue;
        Vector result = new DenseVector(params.size());
        result.set(paramIndex, 2 * diff / Math.sqrt(looseness));
        return result;
    }

    @Override
    public Matrix getPenaltyFunctionHessian(Vector params, double looseness) {
        Matrix result = new DenseMatrix(params.size(), params.size());
        result.set(paramIndex, paramIndex, 2 / Math.sqrt(looseness));
        return result;
    }

}
