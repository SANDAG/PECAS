package com.hbaspecto.pecas.sd;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

import org.junit.BeforeClass;
import org.junit.Test;

import com.hbaspecto.discreteChoiceModelling.Alternative;
import com.hbaspecto.discreteChoiceModelling.Coefficient;
import com.hbaspecto.discreteChoiceModelling.LogitModel;
import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.NoAlternativeAvailable;
import com.hbaspecto.pecas.sd.estimation.DifferentiableModel;
import com.hbaspecto.pecas.sd.estimation.EstimationTarget;
import com.hbaspecto.pecas.sd.estimation.ExpectedValue;
import com.hbaspecto.pecas.sd.estimation.GaussBayesianObjective;
import com.hbaspecto.pecas.sd.estimation.OptimizationException;

import drasys.or.NotImplementedError;

public class TestGaussBayesianObjective {
	private static GaussBayesianObjective gauss;
	private static List<Coefficient> theCoeffs;
	private static TestTarget target1;
	private static TestTarget target2;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		theCoeffs = new ArrayList<Coefficient>();
		final List<EstimationTarget> theTargets = new ArrayList<EstimationTarget>();

		final LogitModel logit = new LogitModel();
		final Coefficient disp1 = new TestCoefficient(
				"Spacetype 1 intensity dispersion");
		final Coefficient disp2 = new TestCoefficient(
				"Spacetype 2 intensity dispersion");
		final Coefficient disp3 = new TestCoefficient("Top-level dispersion");
		disp1.setValue(0.2);
		disp2.setValue(0.5);
		disp3.setValue(0.1);
		theCoeffs.add(disp1);
		theCoeffs.add(disp2);
		theCoeffs.add(disp3);

		final Alternative alt1 = new TestAlternative(1, 2, 3, 1, 4, disp1);
		final Alternative alt2 = new TestAlternative(2, 1, 5, 1.5, 6, disp2);
		logit.addAlternative(alt1);
		logit.addAlternative(alt2);
		logit.setDispersionParameterAsCoeff(disp3);

		target1 = new TestTarget(1);
		target2 = new TestTarget(2);
		theTargets.add(target1);
		theTargets.add(target2);

		final DifferentiableModel model = new TestModel(logit);
		final Matrix targetVariance = new DenseMatrix(new double[][] { { 0.5, 0 },
				{ 0, 0.3 } });
		final Vector means = new DenseVector(new double[] { disp1.getValue(),
				disp2.getValue(), disp3.getValue() });
		final Matrix priorVariance = new DenseMatrix(new double[][] {
				{ 0.09, -0.07, -0.05 }, { -0.07, 0.09, 0.03 }, { -0.05, 0.03, 0.05 } });
		gauss = new GaussBayesianObjective(model, theCoeffs, theTargets,
				targetVariance, means, priorVariance);
	}

	@Test
	public void testGetValue() throws OptimizationException {
		target1.setTheTargetValue(1);
		target2.setTheTargetValue(5);

		Vector params = new DenseVector(new double[] { 0.2, 0.5, 0.1 });
		gauss.setParameterValues(params);
		double value = gauss.getValue();
		assertEquals(1.33012, value, 0.00001);
		params = new DenseVector(new double[] { 0.9, 0.6, 0.2 });
		gauss.setParameterValues(params);
		value = gauss.getValue();
		assertEquals(40.4401, value, 0.0001);

		target1.setTheTargetValue(5);
		target2.setTheTargetValue(1);

		params = new DenseVector(new double[] { 0.2, 0.5, 0.1 });
		gauss.setParameterValues(params);
		value = gauss.getValue();
		assertEquals(78.3321, value, 0.0001);
		params = new DenseVector(new double[] { 0.9, 0.6, 0.2 });
		gauss.setParameterValues(params);
		value = gauss.getValue();
		assertEquals(151.777, value, 0.001);
	}

	@Test
	public void testGetGradient() throws OptimizationException {
		target1.setTheTargetValue(1);
		target2.setTheTargetValue(5);

		Vector params = new DenseVector(new double[] { 0.2, 0.5, 0.1 });
		Vector gradient = gauss.getGradient(params);
		assertEquals(-6.96056, gradient.get(0), 0.00001);
		assertEquals(-2.24502, gradient.get(1), 0.00001);
		assertEquals(-35.1461, gradient.get(2), 0.0001);
		params = new DenseVector(new double[] { 0.9, 0.6, 0.2 });
		gradient = gauss.getGradient(params);
		assertEquals(92.4492, gradient.get(0), 0.0001);
		assertEquals(54.1844, gradient.get(1), 0.0001);
		assertEquals(82.3474, gradient.get(2), 0.0001);

		target1.setTheTargetValue(5);
		target2.setTheTargetValue(1);

		params = new DenseVector(new double[] { 0.2, 0.5, 0.1 });
		gradient = gauss.getGradient(params);
		assertEquals(64.6920, gradient.get(0), 0.0001);
		assertEquals(13.6035, gradient.get(1), 0.0001);
		assertEquals(397.936, gradient.get(2), 0.001);
		params = new DenseVector(new double[] { 0.9, 0.6, 0.2 });
		gradient = gauss.getGradient(params);
		assertEquals(92.2167, gradient.get(0), 0.0001);
		assertEquals(68.8553, gradient.get(1), 0.0001);
		assertEquals(204.158, gradient.get(2), 0.001);
	}

	@Test
	public void testGetHessian() throws OptimizationException {
		target1.setTheTargetValue(1);
		target2.setTheTargetValue(5);

		Vector params = new DenseVector(new double[] { 0.2, 0.5, 0.1 });
		Matrix gradient = gauss.getHessian(params);
		assertEquals(147.903, gradient.get(0, 0), 0.001);
		assertEquals(71.4147, gradient.get(0, 1), 0.0001);
		assertEquals(278.344, gradient.get(0, 2), 0.001);
		assertEquals(71.4147, gradient.get(1, 0), 0.0001);
		assertEquals(64.9324, gradient.get(1, 1), 0.0001);
		assertEquals(74.3608, gradient.get(1, 2), 0.0001);
		assertEquals(278.344, gradient.get(2, 0), 0.001);
		assertEquals(74.3608, gradient.get(2, 1), 0.0001);
		assertEquals(1286.02, gradient.get(2, 2), 0.01);
		params = new DenseVector(new double[] { 0.9, 0.6, 0.2 });
		gradient = gauss.getHessian(params);
		assertEquals(112.501, gradient.get(0, 0), 0.001);
		assertEquals(62.5003, gradient.get(0, 1), 0.0001);
		assertEquals(74.8787, gradient.get(0, 2), 0.0001);
		assertEquals(62.5003, gradient.get(1, 0), 0.0001);
		assertEquals(64.4868, gradient.get(1, 1), 0.0001);
		assertEquals(37.0680, gradient.get(1, 2), 0.0001);
		assertEquals(74.8787, gradient.get(2, 0), 0.0001);
		assertEquals(37.0680, gradient.get(2, 1), 0.0001);
		assertEquals(190.454, gradient.get(2, 2), 0.001);

		target1.setTheTargetValue(5);
		target2.setTheTargetValue(1);

		params = new DenseVector(new double[] { 0.2, 0.5, 0.1 });
		gradient = gauss.getHessian(params);
		assertEquals(147.903, gradient.get(0, 0), 0.001);
		assertEquals(71.4147, gradient.get(0, 1), 0.0001);
		assertEquals(278.344, gradient.get(0, 2), 0.001);
		assertEquals(71.4147, gradient.get(1, 0), 0.0001);
		assertEquals(64.9324, gradient.get(1, 1), 0.0001);
		assertEquals(74.3608, gradient.get(1, 2), 0.0001);
		assertEquals(278.344, gradient.get(2, 0), 0.001);
		assertEquals(74.3608, gradient.get(2, 1), 0.0001);
		assertEquals(1286.02, gradient.get(2, 2), 0.01);
		params = new DenseVector(new double[] { 0.9, 0.6, 0.2 });
		gradient = gauss.getHessian(params);
		assertEquals(112.501, gradient.get(0, 0), 0.001);
		assertEquals(62.5003, gradient.get(0, 1), 0.0001);
		assertEquals(74.8787, gradient.get(0, 2), 0.0001);
		assertEquals(62.5003, gradient.get(1, 0), 0.0001);
		assertEquals(64.4868, gradient.get(1, 1), 0.0001);
		assertEquals(37.0680, gradient.get(1, 2), 0.0001);
		assertEquals(74.8787, gradient.get(2, 0), 0.0001);
		assertEquals(37.0680, gradient.get(2, 1), 0.0001);
		assertEquals(190.454, gradient.get(2, 2), 0.001);
	}

	private static class TestModel implements DifferentiableModel {
		List<Coefficient> myCoeffs;
		LogitModel myModel;

		private TestModel(LogitModel model) {
			myModel = model;
			myCoeffs = new ArrayList<Coefficient>();
			for (final Alternative alt : myModel.getAlternatives()) {
				myCoeffs.add(((TestAlternative) alt).getIntensityDispersion());
			}
			myCoeffs.add(myModel.getDispersionParameterAsCoeff());
		}

		@Override
		public Vector getTargetValues(List<EstimationTarget> targets, Vector params)
				throws OptimizationException {
			int i = 0;
			for (final Coefficient coeff : myCoeffs) {
				coeff.setValue(params.get(i));
				i++;
			}
			Vector result;
			try {
				final List<ExpectedValue> expvalues = new ArrayList<ExpectedValue>();
				for (final EstimationTarget t : targets) {
					expvalues.addAll(t.getAssociatedExpectedValues());
				}
				result = myModel.getExpectedTargetValues(expvalues);
			}
			catch (final ChoiceModelOverflowException e) {
				throw new OptimizationException(e);
			}
			catch (final NoAlternativeAvailable e) {
				throw new OptimizationException(e);
			}
			return result;
		}

		@Override
		public Matrix getJacobian(List<EstimationTarget> targets, Vector params)
				throws OptimizationException {
			int i = 0;
			for (final Coefficient coeff : myCoeffs) {
				coeff.setValue(params.get(i));
				i++;
			}
			Matrix result;
			try {
				final List<ExpectedValue> expvalues = new ArrayList<ExpectedValue>();
				for (final EstimationTarget t : targets) {
					expvalues.addAll(t.getAssociatedExpectedValues());
				}
				result = myModel.getExpectedTargetDerivativesWRTParameters(expvalues,
						myCoeffs);
			}
			catch (final ChoiceModelOverflowException e) {
				throw new OptimizationException(e);
			}
			catch (final NoAlternativeAvailable e) {
				throw new OptimizationException(e);
			}
			return result;
		}
	}

	private static class TestAlternative extends DevelopmentAlternative {
		private final Coefficient intensityDispersion;

		private final double utilPerLand;
		private final double utilPerSpace;
		private final double[] intensityPoints;
		private final double[] perSpaceAdjustments;
		private final double[] perLandAdjustments;
		private final int mySpacetype;

		private static final double landArea = 43560;

		private TestAlternative(int spacetype, double utilityPerLand,
				double utilityPerSpace, double minFAR, double maxFAR,
				Coefficient dispersion) {
			mySpacetype = spacetype;
			utilPerLand = utilityPerLand;
			utilPerSpace = utilityPerSpace;
			intensityDispersion = dispersion;

			intensityPoints = new double[] { minFAR, maxFAR };
			perSpaceAdjustments = new double[] { 0 };
			perLandAdjustments = new double[] { 0 };
		}

		@Override
		public Vector getExpectedTargetValues(List<ExpectedValue> ts)
				throws NoAlternativeAvailable, ChoiceModelOverflowException {
			final double expvalue = getExpectedFAR(intensityDispersion.getValue(),
					landArea, utilPerSpace, utilPerLand, intensityPoints,
					perSpaceAdjustments, perLandAdjustments);

			final Vector result = new DenseVector(ts.size());

			int i = 0;
			for (final ExpectedValue target : ts) {
				result.set(i,
						target.getModelledTotalNewValueForParcel(mySpacetype, 0, expvalue));
				i++;
			}

			return result;
		}

		@Override
		public Vector getUtilityDerivativesWRTParameters(List<Coefficient> cs)
				throws NoAlternativeAvailable, ChoiceModelOverflowException {
			final Vector derivs = getUtilityDerivativesWRTParameters(
					intensityDispersion.getValue(), landArea, utilPerSpace, utilPerLand,
					intensityPoints, perSpaceAdjustments, perLandAdjustments);

			final Vector result = new DenseVector(cs.size());
			result
					.set(cs.indexOf(intensityDispersion), derivs.get(derivs.size() - 1));

			return result;
		}

		@Override
		public Matrix getExpectedTargetDerivativesWRTParameters(
				List<ExpectedValue> ts, List<Coefficient> cs)
				throws NoAlternativeAvailable, ChoiceModelOverflowException {
			final Vector derivs = getExpectedFARDerivativesWRTParameters(
					intensityDispersion.getValue(), landArea, utilPerSpace, utilPerLand,
					intensityPoints, perSpaceAdjustments, perLandAdjustments);

			final double expvalue = getExpectedFAR(intensityDispersion.getValue(),
					landArea, utilPerSpace, utilPerLand, intensityPoints,
					perSpaceAdjustments, perLandAdjustments);

			final Matrix result = new DenseMatrix(ts.size(), cs.size());

			final int dispIndex = cs.indexOf(intensityDispersion);
			int i = 0;
			for (final ExpectedValue target : ts) {
				double deriv = derivs.get(derivs.size() - 1);
				deriv = deriv
						* target.getModelledTotalNewDerivativeWRTNewSpace(mySpacetype, 0,
								expvalue);
				result.set(i, dispIndex, deriv);
				i++;
			}
			return result;
		}

		@Override
		public double getUtility(double dispersionParameterForSizeTermCalculation)
				throws ChoiceModelOverflowException {
			return getCompositeUtility(intensityDispersion.getValue(), landArea,
					utilPerSpace, utilPerLand, intensityPoints, perSpaceAdjustments,
					perLandAdjustments);
		}

		//@Override
		public double getUtilityNoSizeEffect() throws ChoiceModelOverflowException {
			throw new NotImplementedError();

		}

		@Override
		public void doDevelopment() {
		}

		public Coefficient getIntensityDispersion() {
			return intensityDispersion;
		}

		@Override
		public void startCaching() {
		}

		@Override
		public void endCaching() {
		}

	}

	private static class TestCoefficient implements Coefficient {
		private final String myName;
		private double myValue;

		private TestCoefficient(String name) {
			myName = name;
		}

		@Override
		public double getValue() {
			return myValue;
		}

		@Override
		public void setValue(double v) {
			myValue = v;
		}

		@Override
		public String getName() {
			return myName;
		}

		@Override
		public double getTransformedValue() {
			return getValue();
		}

		@Override
		public void setTransformedValue(double v) {
			setValue(v);
		}

		@Override
		public double getTransformationDerivative() {
			return 1;
		}

		@Override
		public double getInverseTransformationDerivative() {
			return 1;
		}

	}

	private static class TestTarget extends EstimationTarget implements
			ExpectedValue {
		private final int mySpacetype;
		private double modelledValue;
		private double[] derivs;

		private TestTarget(int spacetype) {
			mySpacetype = spacetype;
		}

		private void setTheTargetValue(double target) {
			super.setTargetValue(target);
		}

		@Override
		public double getModelledTotalNewValueForParcel(int spacetype,
				double expectedAddedSpace, double expectedNewSpace) {
			if (spacetype == mySpacetype) {
				return expectedNewSpace + expectedAddedSpace;
			}
			else {
				return 0;
			}
		}

		@Override
		public double getModelledTotalNewDerivativeWRTAddedSpace(int spacetype,
				double expectedAddedSpace, double expectedNewSpace) {
			if (spacetype == mySpacetype) {
				return 1;
			}
			else {
				return 0;
			}
		}

		@Override
		public double getModelledTotalNewDerivativeWRTNewSpace(int spacetype,
				double expectedAddedSpace, double expectedNewSpace) {
			if (spacetype == mySpacetype) {
				return 1;
			}
			else {
				return 0;
			}
		}

		@Override
		public boolean appliesToCurrentParcel() {
			return true;
		}

		@Override
		public String getName() {
			return "Test target " + mySpacetype;
		}

		@Override
		public void setModelledValue(double value) {
			modelledValue = value;
		}

		@Override
		public double getModelledValue() {
			return modelledValue;
		}

		@Override
		public List<ExpectedValue> getAssociatedExpectedValues() {
			return Collections.<ExpectedValue> singletonList(this);
		}

		@Override
		public void setDerivatives(double[] derivatives) {
			derivs = Arrays.copyOf(derivatives, derivatives.length);
		}

		@Override
		public double[] getDerivatives() {
			return Arrays.copyOf(derivs, derivs.length);
		}
	}
}
