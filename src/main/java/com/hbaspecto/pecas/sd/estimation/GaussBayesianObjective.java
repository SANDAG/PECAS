package com.hbaspecto.pecas.sd.estimation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.hbaspecto.discreteChoiceModelling.Coefficient;

import no.uib.cipr.matrix.DenseCholesky;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.Matrices;

public class GaussBayesianObjective implements ObjectiveFunction {
    private DifferentiableModel myModel;
    private List<EstimationTarget> myTargets;
    private List<Coefficient> myCoeffs;
    private Matrix myInverseTargetVariance;
    private Vector myMean;
    private Matrix myInversePriorVariance;
    
    private int numParams;
    
    // Values for logging.
    private double currentObjectiveValue;
    private double currentParameterError;
    private double currentTargetError;
    private Vector currentParameterValues;
    private double[] currentModelledValues;
    
    private boolean previousValuesExist = false;
    private double previousObjectiveValue;
    private Vector previousParameterValues;
    private double[] previousModelledValues;

    public GaussBayesianObjective(DifferentiableModel model, List<Coefficient> coeffs,
            List<EstimationTarget> targets, Matrix targetVariance, Vector priorMean, Matrix priorVariance) {
        numParams = priorMean.size();
        
        myModel = model;
        myTargets = new ArrayList<EstimationTarget>(targets);
        myCoeffs = new ArrayList<Coefficient>(coeffs);
        
        myMean = priorMean.copy();
        
        // Invert the target variance.
        DenseMatrix identity = Matrices.identity(targets.size());
        DenseCholesky cholesky = DenseCholesky.factorize(targetVariance);
        myInverseTargetVariance = cholesky.solve(identity);
        
        // Invert the prior variance.
        identity = Matrices.identity(numParams);
        cholesky = DenseCholesky.factorize(priorVariance);
        myInversePriorVariance = cholesky.solve(identity);
    }
    
    @Override
	public void setParameterValues(Vector params)
    {
    	currentParameterValues = params;
    };

    @Override
    public double getValue() throws OptimizationException {
        Vector modelledValues = myModel.getTargetValues(myTargets, currentParameterValues);
        currentModelledValues = Matrices.getArray(modelledValues);
        Vector targetErrors = modelledValues.copy();
        targetErrors.add(-1, getMyTargetValues());
        Vector paramErrors = currentParameterValues.copy();
        paramErrors.add(-1, myMean);
        
        currentTargetError = quadraticForm(targetErrors, myInverseTargetVariance);
        currentParameterError = quadraticForm(paramErrors, myInversePriorVariance);
        currentObjectiveValue = currentTargetError + currentParameterError;
        return currentObjectiveValue;
    }

    @Override
    public Vector getGradient(Vector params) throws OptimizationException {
        Vector modelledValues = myModel.getTargetValues(myTargets, params);
        Vector targetErrors = modelledValues.copy();
        targetErrors.add(-1, getMyTargetValues());
        targetErrors = myInverseTargetVariance.mult(targetErrors, targetErrors.copy());
        Vector paramErrors = params.copy();
        paramErrors.add(-1, myMean);
        
        Matrix jacobian = myModel.getJacobian(myTargets, params);
        
        Vector gradient = new DenseVector(numParams);
        gradient = jacobian.transMult(targetErrors, gradient);
        gradient = myInversePriorVariance.multAdd(paramErrors, gradient);
        gradient.scale(2);
        
        return gradient;
    }

    @Override
    public Matrix getHessian(Vector params) throws OptimizationException {
        Matrix jacobian = myModel.getJacobian(myTargets, params);
        Matrix varianceTimesJacobian = new DenseMatrix(jacobian.numRows(), jacobian.numColumns());
        varianceTimesJacobian = myInverseTargetVariance.mult(jacobian, varianceTimesJacobian);
        
        Matrix hessian = new DenseMatrix(numParams, numParams);
        hessian = jacobian.transAmult(varianceTimesJacobian, hessian);
        hessian.add(myInversePriorVariance);
        hessian.scale(2);
        
        return hessian;
    }
    
    private double quadraticForm(Vector x, Matrix a) {
        Matrix xm = new DenseMatrix(x);
        Matrix xmt = new DenseMatrix(1, x.size());
        xmt = xm.transpose(xmt);
        xmt = xmt.mult(a, xmt.copy());
        Matrix result = new DenseMatrix(1, 1);
        result = xmt.mult(xm, result);
        return result.get(0, 0);
    }
    
    private Vector getMyTargetValues() {
        Vector myTargetValues = new DenseVector(myTargets.size());
        int i = 0;
        for(EstimationTarget target : myTargets) {
            myTargetValues.set(i, target.getTargetValue());
            i++;
        }
        
        return myTargetValues;
    }


	@Override
	public void logParameters(Logger logger) {
	    
        logger.info("Parameter values:");
        int i = 0;
        for(Coefficient coeff : myCoeffs) {
            String line = "Parameter " + coeff.getName()
                    + ": prior mean = " + myMean.get(i)
                    + ", current value = " + currentParameterValues.get(i);
            if(previousValuesExist)
                line = line + ", previous value = " + previousParameterValues.get(i);
            logger.info(line);
            i++;
        }
        
        previousParameterValues = currentParameterValues;
 	}

    
    @Override
	public void logCurrentValues(Logger logger) {
    	logParameters(logger);
    	logTargetAndObjective(logger);
    }
	    

    @Override
	public void logTargetAndObjective(Logger logger) {
	    
        logger.info("Current value of objective function = " + currentObjectiveValue);
        if(previousValuesExist)
            logger.info("Previous value of objective function = " + previousObjectiveValue);
        logger.info("Contribution from parameters = " + currentParameterError);
        logger.info("Contribution from targets = " + currentTargetError);
        logger.info("Parameter values:");
        logger.info("Target values:");
        int i = 0;
        for(EstimationTarget target : myTargets) {
            String line = "Target " + target.getName()
                    + ": target value = " + target.getTargetValue()
                    + ", modelled value = " + currentModelledValues[i];
            if(previousValuesExist)
                line = line + ", previous value = " + previousModelledValues[i];
            logger.info(line);
            i++;
        }
        
        previousObjectiveValue = currentObjectiveValue;
        previousModelledValues = currentModelledValues;
        previousValuesExist = true;
 	}
	
	// DEBUG
	public void printCurrentDerivatives(BufferedWriter writer) throws IOException {
		((ExpectedTargetModel) myModel).printCurrentDerivatives(writer);
	}
	
    public void printHessian(BufferedWriter writer, Matrix hessian) throws IOException {
    	// Prints the Jacobian matrix in a nice table format
    	// First, the parameter names across the top.
    	writer.write(",");
    	for(int j = 0; j < myCoeffs.size(); j++)
    		writer.write("," + myCoeffs.get(j).getName());
    	// The current parameter values.
    	writer.newLine();
    	for(int i = 0; i < hessian.numRows(); i++)
    	{
    		writer.write(myCoeffs.get(i).getName());
    		for(int j = 0; j < myCoeffs.size(); j++)
    			writer.write("," + hessian.get(i, j));
    		writer.newLine();
    	}
    }

}
