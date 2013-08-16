package com.hbaspecto.pecas.sd;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

import org.junit.Before;
import org.junit.Test;

import com.hbaspecto.discreteChoiceModelling.Coefficient;
import com.hbaspecto.discreteChoiceModelling.LogitModel;
import com.hbaspecto.discreteChoiceModelling.ParameterSearchAlternative;
import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.NoAlternativeAvailable;
import com.hbaspecto.pecas.sd.estimation.ExpectedValue;

public class TestExpectedValueFormula {
	private Coefficient cr;
	private Coefficient cb;
	private Coefficient cy;
	private Coefficient k;
	private Coefficient \u03bb1;
	private Coefficient \u03bb2;
	private List<Coefficient> coeffs;
	private List<ExpectedValue> targets;

	@Before
	public void setUp() {
		cr = new DummyCoefficient("cr");
		cb = new DummyCoefficient("cb");
		cy = new DummyCoefficient("cy");
		k = new DummyCoefficient("k");
		\u03bb1 = new DummyCoefficient("\u03bb1");
		\u03bb1.setValue(1);
		\u03bb2 = new DummyCoefficient("\u03bb2");
		\u03bb2.setValue(2);
		coeffs = new ArrayList<Coefficient>();
		coeffs.add(cr);
		coeffs.add(cb);
		coeffs.add(cy);
		coeffs.add(k);
		coeffs.add(\u03bb1);
		coeffs.add(\u03bb2);
		targets = new ArrayList<ExpectedValue>();
		targets.add(new DummyTarget());
		targets.add(new DummyTarget());
		targets.add(new DummyTarget());
	}

	@Test
	public void testExpectedValueSingleLevel() throws NoAlternativeAvailable,
			ChoiceModelOverflowException {

		final LogitModel m = new LogitModel();
		m.addAlternative(new DummyRedAlternative());
		m.addAlternative(new DummyBlueYellowAlternative());
		m.setDispersionParameterAsCoeff(\u03bb1);

		final Matrix d = m.getExpectedTargetDerivativesWRTParameters(targets,
				coeffs);

		// Check values.
		assertEquals(2.0546, d.get(0, 0), 0.001);
		assertEquals(-1.2901, d.get(0, 1), 0.001);
		assertEquals(-0.76452, d.get(0, 2), 0.0001);
		assertEquals(12.454, d.get(0, 3), 0.01);
		assertEquals(-0.32175, d.get(0, 4), 0.0001);
		assertEquals(0.35201, d.get(0, 5), 0.0001);
		assertEquals(-1.1379, d.get(1, 0), 0.001);
		assertEquals(2.7396, d.get(1, 1), 0.001);
		assertEquals(-1.6017, d.get(1, 2), 0.001);
		assertEquals(-5.1532, d.get(1, 3), 0.001);
		assertEquals(0.17819, d.get(1, 4), 0.0001);
		assertEquals(0.31133, d.get(1, 5), 0.0001);
		assertEquals(-0.91671, d.get(2, 0), 0.0001);
		assertEquals(-1.4496, d.get(2, 1), 0.001);
		assertEquals(2.3663, d.get(2, 2), 0.001);
		assertEquals(0.69940, d.get(2, 3), 0.0001);
		assertEquals(0.14356, d.get(2, 4), 0.0001);
		assertEquals(-0.66335, d.get(2, 5), 0.0001);
	}

	@Test
	public void testExpectedValueTwoLevel() throws NoAlternativeAvailable,
			ChoiceModelOverflowException {
		final LogitModel m1 = new LogitModel();
		final LogitModel m2 = new LogitModel();
		m1.addAlternative(new DummyRedAlternative());
		m1.addAlternative(m2);
		m1.setDispersionParameterAsCoeff(\u03bb1);
		m2.addAlternative(new DummyBlueAlternative());
		m2.addAlternative(new DummyYellowAlternative());
		m2.setDispersionParameterAsCoeff(\u03bb2);

		final Matrix d = m1.getExpectedTargetDerivativesWRTParameters(targets,
				coeffs);

		// Check values.
		assertEquals(2.0546, d.get(0, 0), 0.001);
		assertEquals(-1.2901, d.get(0, 1), 0.001);
		assertEquals(-0.76452, d.get(0, 2), 0.0001);
		assertEquals(12.454, d.get(0, 3), 0.01);
		assertEquals(-0.32175, d.get(0, 4), 0.0001);
		assertEquals(0.35201, d.get(0, 5), 0.0001);
		assertEquals(-1.1379, d.get(1, 0), 0.001);
		assertEquals(2.7396, d.get(1, 1), 0.001);
		assertEquals(-1.6017, d.get(1, 2), 0.001);
		assertEquals(-5.1532, d.get(1, 3), 0.001);
		assertEquals(0.17819, d.get(1, 4), 0.0001);
		assertEquals(0.31133, d.get(1, 5), 0.0001);
		assertEquals(-0.91671, d.get(2, 0), 0.0001);
		assertEquals(-1.4496, d.get(2, 1), 0.001);
		assertEquals(2.3663, d.get(2, 2), 0.001);
		assertEquals(0.69940, d.get(2, 3), 0.0001);
		assertEquals(0.14356, d.get(2, 4), 0.0001);
		assertEquals(-0.66335, d.get(2, 5), 0.0001);
	}

	@Test
	public void testExpectedValueMoreAlts() throws NoAlternativeAvailable,
			ChoiceModelOverflowException {
		final LogitModel m1 = new LogitModel();
		final LogitModel m2 = new LogitModel();
		m1.addAlternative(new DummyRedAlternative());
		m1.addAlternative(m2);
		m1.addAlternative(new DummyWhiteAlternative());
		m1.setDispersionParameterAsCoeff(\u03bb1);
		m2.addAlternative(new DummyBlueAlternative());
		m2.addAlternative(new DummyYellowAlternative());
		m2.addAlternative(new DummyGreenAlternative());
		m2.setDispersionParameterAsCoeff(\u03bb2);

		final Matrix d = m1.getExpectedTargetDerivativesWRTParameters(targets,
				coeffs);

		// Check values.
		assertEquals(0.97488, d.get(0, 0), 0.0001);
		assertEquals(-1.1288, d.get(0, 1), 0.001);
		assertEquals(-1.1646, d.get(0, 2), 0.001);
		assertEquals(6.8685, d.get(0, 3), 0.001);
		assertEquals(-2.4157, d.get(0, 4), 0.001);
		assertEquals(-0.03612, d.get(0, 5), 0.0001);
		assertEquals(-0.47673, d.get(1, 0), 0.0001);
		assertEquals(0.72546, d.get(1, 1), 0.0001);
		assertEquals(0.57969, d.get(1, 2), 0.0001);
		assertEquals(-0.02485, d.get(1, 3), 0.0001);
		assertEquals(1.4875, d.get(1, 4), 0.001);
		assertEquals(0.01533, d.get(1, 5), 0.0001);
		assertEquals(-0.49815, d.get(2, 0), 0.0001);
		assertEquals(0.403345, d.get(2, 1), 0.001);
		assertEquals(0.58490, d.get(2, 2), 0.001);
		assertEquals(1.1563, d.get(2, 3), 0.001);
		assertEquals(0.92820, d.get(2, 4), 0.0001);
		assertEquals(0.02080, d.get(2, 5), 0.0001);
	}

	private class DummyCoefficient implements Coefficient {

		private double value;
		private final String name;

		private DummyCoefficient(String n) {
			name = n;
		}

		@Override
		public double getValue() {
			return value;
		}

		@Override
		public void setValue(double v) {
			value = v;
		}

		@Override
		public String getName() {
			return name;
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

	private class DummyTarget implements ExpectedValue {

		@Override
		public double getModelledTotalNewValueForParcel(int spacetype,
				double expectedAddedSpace, double expectedNewSpace) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getModelledTotalNewDerivativeWRTAddedSpace(int spacetype,
				double expectedAddedSpace, double expectedNewSpace) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public double getModelledTotalNewDerivativeWRTNewSpace(int spacetype,
				double expectedAddedSpace, double expectedNewSpace) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean appliesToCurrentParcel() {
			return true;
		}

		@Override
		public void setModelledValue(double value) {
		}

		@Override
		public void setDerivatives(double[] derivatives) {
		}
	}

	private class DummyRedAlternative implements ParameterSearchAlternative {

		@Override
		public double getUtility(double dispersionParameterForSizeTermCalculation)
				throws ChoiceModelOverflowException {
			return getUtilityNoSizeEffect();
		}

		@Override
		public double getUtilityNoSizeEffect() throws ChoiceModelOverflowException {
			return 2.5;
		}

		@Override
		public Vector getExpectedTargetValues(List<ExpectedValue> ts) {
			return new DenseVector(new double[] { 12.5, 3.5, 3 });
		}

		@Override
		public Vector getUtilityDerivativesWRTParameters(List<Coefficient> cs) {
			return new DenseVector(new double[] { 1, 0, 0, 5, 0, 0 });
		}

		@Override
		public Matrix getExpectedTargetDerivativesWRTParameters(
				List<ExpectedValue> ts, List<Coefficient> cs) {
			return new DenseMatrix(new double[][] { { 0, 0, 0, 5, 0, 0 },
					{ 0, 0, 0, 1, 0, 0 }, { 0, 0, 0, 2, 0, 0 } });
		}

		@Override
		public void startCaching() {
		}

		@Override
		public void endCaching() {
		}
	}

	private class DummyBlueAlternative implements ParameterSearchAlternative {

		@Override
		public double getUtility(double dispersionParameterForSizeTermCalculation)
				throws ChoiceModelOverflowException {
			return getUtilityNoSizeEffect();
		}

		@Override
		public double getUtilityNoSizeEffect() throws ChoiceModelOverflowException {
			return 2.5;
		}

		@Override
		public Vector getExpectedTargetValues(List<ExpectedValue> ts) {
			return new DenseVector(new double[] { 4.5, 10.5, 4 });
		}

		@Override
		public Vector getUtilityDerivativesWRTParameters(List<Coefficient> cs) {
			return new DenseVector(new double[] { 0, 1, 0, 1, 0, 0 });
		}

		@Override
		public Matrix getExpectedTargetDerivativesWRTParameters(
				List<ExpectedValue> ts, List<Coefficient> cs) {
			return new DenseMatrix(new double[][] { { 0, 0, 0, 5, 0, 0 },
					{ 0, 0, 0, 1, 0, 0 }, { 0, 0, 0, 2, 0, 0 } });
		}

		@Override
		public void startCaching() {
		}

		@Override
		public void endCaching() {
		}

	}

	private class DummyYellowAlternative implements ParameterSearchAlternative {

		@Override
		public double getUtility(double dispersionParameterForSizeTermCalculation)
				throws ChoiceModelOverflowException {
			return getUtilityNoSizeEffect();
		}

		@Override
		public double getUtilityNoSizeEffect() throws ChoiceModelOverflowException {
			return 2;
		}

		@Override
		public Vector getExpectedTargetValues(List<ExpectedValue> ts) {
			return new DenseVector(new double[] { 3.5, 1.5, 14 });
		}

		@Override
		public Vector getUtilityDerivativesWRTParameters(List<Coefficient> cs) {
			return new DenseVector(new double[] { 0, 0, 1, 2, 0, 0 });
		}

		@Override
		public Matrix getExpectedTargetDerivativesWRTParameters(
				List<ExpectedValue> ts, List<Coefficient> cs) {
			return new DenseMatrix(new double[][] { { 0, 0, 0, 5, 0, 0 },
					{ 0, 0, 0, 1, 0, 0 }, { 0, 0, 0, 2, 0, 0 } });
		}

		@Override
		public void startCaching() {
		}

		@Override
		public void endCaching() {
		}

	}

	private class DummyGreenAlternative implements ParameterSearchAlternative {

		@Override
		public double getUtility(double dispersionParameterForSizeTermCalculation)
				throws ChoiceModelOverflowException {
			return getUtilityNoSizeEffect();
		}

		@Override
		public double getUtilityNoSizeEffect() throws ChoiceModelOverflowException {
			return 4.5;
		}

		@Override
		public Vector getExpectedTargetValues(List<ExpectedValue> ts) {
			return new DenseVector(new double[] { 2.5, 8.5, 8 });
		}

		@Override
		public Vector getUtilityDerivativesWRTParameters(List<Coefficient> cs) {
			return new DenseVector(new double[] { 0, 1, 1, 3, 0, 0 });
		}

		@Override
		public Matrix getExpectedTargetDerivativesWRTParameters(
				List<ExpectedValue> ts, List<Coefficient> cs) {
			return new DenseMatrix(new double[][] { { 0, 0, 0, 5, 0, 0 },
					{ 0, 0, 0, 1, 0, 0 }, { 0, 0, 0, 2, 0, 0 } });
		}

		@Override
		public void startCaching() {
		}

		@Override
		public void endCaching() {
		}

	}

	private class DummyWhiteAlternative implements ParameterSearchAlternative {

		@Override
		public double getUtility(double dispersionParameterForSizeTermCalculation)
				throws ChoiceModelOverflowException {
			return getUtilityNoSizeEffect();
		}

		@Override
		public double getUtilityNoSizeEffect() throws ChoiceModelOverflowException {
			return 1.5;
		}

		@Override
		public Vector getExpectedTargetValues(List<ExpectedValue> ts) {
			return new DenseVector(new double[] { 7.5, 3.5, 8 });
		}

		@Override
		public Vector getUtilityDerivativesWRTParameters(List<Coefficient> cs) {
			return new DenseVector(new double[] { 0, 0, 0, 3, 0, 0 });
		}

		@Override
		public Matrix getExpectedTargetDerivativesWRTParameters(
				List<ExpectedValue> ts, List<Coefficient> cs) {
			return new DenseMatrix(new double[][] { { 0, 0, 0, 5, 0, 0 },
					{ 0, 0, 0, 1, 0, 0 }, { 0, 0, 0, 2, 0, 0 } });
		}

		@Override
		public void startCaching() {
		}

		@Override
		public void endCaching() {
		}

	}

	private class DummyBlueYellowAlternative implements
			ParameterSearchAlternative {

		@Override
		public double getUtility(double dispersionParameterForSizeTermCalculation)
				throws ChoiceModelOverflowException {
			return getUtilityNoSizeEffect();
		}

		@Override
		public double getUtilityNoSizeEffect() throws ChoiceModelOverflowException {
			return 2.6566;
		}

		@Override
		public Vector getExpectedTargetValues(List<ExpectedValue> ts) {
			return new DenseVector(new double[] { 4.2311, 8.0795, 6.6894 });
		}

		@Override
		public Vector getUtilityDerivativesWRTParameters(List<Coefficient> cs) {
			return new DenseVector(new double[] { 0, 0.73106, 0.26894, 1.2689, 0,
					-0.14554 });
		}

		@Override
		public Matrix getExpectedTargetDerivativesWRTParameters(
				List<ExpectedValue> ts, List<Coefficient> cs) {
			return new DenseMatrix(new double[][] {
					{ 0, 0.3932, -0.3932, 4.6068, 0, 0.098306 },
					{ 0, 3.5390, -3.5390, -2.5389, 0, 0.88474 },
					{ 0, -3.9322, 3.9322, 5.9322, 0, -0.98306 } });
		}

		@Override
		public void startCaching() {
		}

		@Override
		public void endCaching() {
		}

	}
}
