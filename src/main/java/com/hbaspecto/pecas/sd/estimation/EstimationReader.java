package com.hbaspecto.pecas.sd.estimation;

import java.util.List;
import com.hbaspecto.discreteChoiceModelling.Coefficient;

public interface EstimationReader
{
    List<EstimationTarget> readTargets();

    double[][] readTargetVariance(List<EstimationTarget> targets);

    List<Coefficient> readCoeffs();

    double[] readPriorMeans(List<Coefficient> coeffs);

    double[][] readPriorVariance(List<Coefficient> coeffs);

    double[] readStartingValues(List<Coefficient> coeffs);
}
