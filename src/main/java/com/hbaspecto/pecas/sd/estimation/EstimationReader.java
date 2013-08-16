package com.hbaspecto.pecas.sd.estimation;

import java.util.List;

import com.hbaspecto.discreteChoiceModelling.Coefficient;

public interface EstimationReader {
	public List<EstimationTarget> readTargets();

	public double[][] readTargetVariance(List<EstimationTarget> targets);

	public List<Coefficient> readCoeffs();

	public double[] readPriorMeans(List<Coefficient> coeffs);

	public double[][] readPriorVariance(List<Coefficient> coeffs);

	public double[] readStartingValues(List<Coefficient> coeffs);
}
