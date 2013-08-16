package com.hbaspecto.pecas.sd.estimation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

import com.hbaspecto.discreteChoiceModelling.Coefficient;
import com.hbaspecto.pecas.land.LandInventory;
import com.hbaspecto.pecas.sd.ZoningRulesI;

public class ExpectedTargetModel implements DifferentiableModel {

	private final List<Coefficient> myCoeffs;
	private final LandInventory land;

	private final List<EstimationTarget> lastTargetValues;
	private Vector lastParamValues;

	private Vector targetValues;
	private Matrix jacobian;

	public ExpectedTargetModel(List<Coefficient> coeffs,
			LandInventory landInventory) {
		myCoeffs = new ArrayList<Coefficient>(coeffs);
		lastTargetValues = new ArrayList<EstimationTarget>();
		land = landInventory;
	}

	@Override
	public Vector getTargetValues(List<EstimationTarget> targets, Vector params)
			throws OptimizationException {
		// Do target value and derivative calculations at the same time, only if
		// they
		// have not already been done for this combination of targets and
		// parameters.
		if (!(targets.equals(lastTargetValues) && equals(params, lastParamValues))) {
			calculateAllValues(targets, params);
		}
		lastTargetValues.clear();
		lastTargetValues.addAll(targets);
		lastParamValues = params.copy();
		return targetValues;
	}

	@Override
	public Matrix getJacobian(List<EstimationTarget> targets, Vector params)
			throws OptimizationException {
		if (!(targets.equals(lastTargetValues) && equals(params, lastParamValues))) {
			calculateAllValues(targets, params);
		}
		lastTargetValues.clear();
		lastTargetValues.addAll(targets);
		lastParamValues = params.copy();
		return jacobian;
	}

	private void calculateAllValues(List<EstimationTarget> targets, Vector params) {
		setCoefficients(params);
		final List<ExpectedValue> expValueObjects = convertToExpectedValueObjects(targets);
		final EstimationMatrix matrix = new EstimationMatrix(expValueObjects,
				myCoeffs);
		land.setToBeforeFirst();
		while (land.advanceToNext()) {
			final ZoningRulesI currentZoningRule = ZoningRulesI
					.getZoningRuleByZoningRulesCode(land.getSession(),
							land.getZoningRulesCode());
			if (currentZoningRule == null) {
				land.getDevelopmentLogger().logBadZoning(land);
			}
			else {
				currentZoningRule.startCaching();
				currentZoningRule.addExpectedValuesToMatrix(matrix, land);
				currentZoningRule.addDerivativesToMatrix(matrix, land);
				currentZoningRule.endCaching();
			}
		}

		final Vector expValues = matrix.getExpectedValues();
		// Convert these back to targets.
		for (int i = 0; i < expValues.size(); i++) {
			expValueObjects.get(i).setModelledValue(expValues.get(i));
		}
		targetValues = new DenseVector(targets.size());
		for (int i = 0; i < targets.size(); i++) {
			targetValues.set(i, targets.get(i).getModelledValue());
		}

		final Matrix expValuesJacobian = matrix.getDerivatives();
		// Convert these back to targets.
		// loop over all exepcted values, and internally set their derivatives
		// with respect to coefficients.
		for (int i = 0; i < expValues.size(); i++) {
			final double[] derivatives = new double[myCoeffs.size()];
			for (int j = 0; j < myCoeffs.size(); j++) {
				derivatives[j] = expValuesJacobian.get(i, j);
			}
			expValueObjects.get(i).setDerivatives(derivatives);
		}
		// loop over targets and calculate derivatives with respect to
		// coefficients, note
		// most targets have only one expected value but some don't so that's
		// why
		// it needs to be in this separate loop.
		jacobian = new DenseMatrix(targets.size(), myCoeffs.size());
		for (int i = 0; i < targets.size(); i++) {
			final double[] derivatives = targets.get(i).getDerivatives();
			for (int j = 0; j < myCoeffs.size(); j++) {
				jacobian.set(i, j, derivatives[j]);
			}
		}

		// Apply the chain rule on the coefficient transformations.
		for (int j = 0; j < myCoeffs.size(); j++) {
			final double derivative = myCoeffs.get(j)
					.getInverseTransformationDerivative();
			for (int i = 0; i < targets.size(); i++) {
				jacobian.set(i, j, jacobian.get(i, j) * derivative);
			}
		}
	}

	private List<ExpectedValue> convertToExpectedValueObjects(
			List<EstimationTarget> targets) {
		final List<ExpectedValue> result = new ArrayList<ExpectedValue>();
		for (final EstimationTarget t : targets) {
			result.addAll(t.getAssociatedExpectedValues());
		}
		return result;
	}

	private void setCoefficients(Vector params) {
		int i = 0;
		for (final Coefficient coeff : myCoeffs) {
			coeff.setTransformedValue(params.get(i));
			i++;
		}
	}

	private boolean equals(Vector v1, Vector v2) {
		if (v1 == null || v2 == null) {
			return false;
		}
		if (v1.size() != v2.size()) {
			return false;
		}
		for (int i = 0; i < v1.size(); i++) {
			if (v1.get(i) != v2.get(i)) {
				return false;
			}
		}
		return true;
	}

	// DEBUG
	public void printCurrentDerivatives(BufferedWriter writer) throws IOException {
		// Prints the Jacobian matrix in a nice table format
		// First, the parameter names across the top.
		writer.write(",");
		for (int j = 0; j < myCoeffs.size(); j++) {
			writer.write("," + myCoeffs.get(j).getName());
		}
		// The current parameter values.
		writer.newLine();
		writer.write(",");
		for (int j = 0; j < lastParamValues.size(); j++) {
			writer.write("," + lastParamValues.get(j));
		}
		// The target names, current target values, and derivatives.
		writer.newLine();
		for (int i = 0; i < lastTargetValues.size(); i++) {
			writer.write(lastTargetValues.get(i).getName() + ","
					+ lastTargetValues.get(i).getTargetValue());
			for (int j = 0; j < myCoeffs.size(); j++) {
				writer.write("," + jacobian.get(i, j));
			}
			writer.newLine();
		}
	}
}
