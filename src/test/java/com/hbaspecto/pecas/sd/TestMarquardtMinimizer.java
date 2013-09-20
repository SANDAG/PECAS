package com.hbaspecto.pecas.sd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import com.hbaspecto.discreteChoiceModelling.Alternative;
import com.hbaspecto.discreteChoiceModelling.Coefficient;
import com.hbaspecto.discreteChoiceModelling.LogitModel;
import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.NoAlternativeAvailable;
import com.hbaspecto.pecas.sd.estimation.Constraint;
import com.hbaspecto.pecas.sd.estimation.DifferentiableModel;
import com.hbaspecto.pecas.sd.estimation.EstimationTarget;
import com.hbaspecto.pecas.sd.estimation.ExpectedValue;
import com.hbaspecto.pecas.sd.estimation.GaussBayesianObjective;
import com.hbaspecto.pecas.sd.estimation.MarquardtMinimizer;
import com.hbaspecto.pecas.sd.estimation.OptimizationException;
import com.hbaspecto.pecas.sd.estimation.SimpleBoundary;

@Ignore
// TODO: Fix test
public class TestMarquardtMinimizer
{
    private MarquardtMinimizer  minnyTheMarquardtMinimizer;
    private TestTarget          target1;
    private TestTarget          target2;

    private static final Vector EPSILON = new DenseVector(new double[] {1E-6, 1E-6, 1E-6});

    @Before
    public void setUp() throws Exception
    {
        final List<Coefficient> theCoeffs = new ArrayList<Coefficient>();
        final List<EstimationTarget> theTargets = new ArrayList<EstimationTarget>();

        final LogitModel logit = new LogitModel();
        final Coefficient disp1 = new TestCoefficient("Spacetype 1 intensity dispersion");
        final Coefficient disp2 = new TestCoefficient("Spacetype 2 intensity dispersion");
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
        final Matrix targetVariance = new DenseMatrix(new double[][] { {0.5, 0}, {0, 0.3}});
        final Vector means = new DenseVector(new double[] {disp1.getValue(), disp2.getValue(),
                disp3.getValue()});
        final Matrix priorVariance = new DenseMatrix(new double[][] { {0.09, -0.07, -0.05},
                {-0.07, 0.09, 0.03}, {-0.05, 0.03, 0.05}});
        final GaussBayesianObjective gauss = new GaussBayesianObjective(model, theCoeffs,
                theTargets, targetVariance, means, priorVariance);

        minnyTheMarquardtMinimizer = new MarquardtMinimizer(gauss, means);
    }

    @Test
    public void testDoOneIteration() throws OptimizationException
    {
        target1.setTheTargetValue(1);
        target2.setTheTargetValue(5);

        minnyTheMarquardtMinimizer.reset(new DenseVector(new double[] {0.2, 0.5, 0.1}));
        minnyTheMarquardtMinimizer.setOptimizationParameters(1E-7, 1, 0.01);
        assertEquals(0, minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertFalse(minnyTheMarquardtMinimizer.lastRunConverged());
        assertFalse(minnyTheMarquardtMinimizer.lastRunMaxIterations());
        assertFalse(minnyTheMarquardtMinimizer.lastRunInvalidStep());
        assertEquals(1.33012, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.00001);

        Vector coeffs = minnyTheMarquardtMinimizer.doOneIteration();
        assertEquals(1, minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertEquals(0.180229, coeffs.get(0), 0.000001);
        assertEquals(0.521685, coeffs.get(1), 0.000001);
        assertEquals(0.130054, coeffs.get(2), 0.000001);
        assertEquals(0.855485, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.000001);

        coeffs = minnyTheMarquardtMinimizer.doOneIteration();
        assertEquals(2, minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertEquals(0.174836, coeffs.get(0), 0.000001);
        assertEquals(0.524698, coeffs.get(1), 0.000001);
        assertEquals(0.136367, coeffs.get(2), 0.000001);
        assertEquals(0.846586, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.000001);

        minnyTheMarquardtMinimizer.reset(new DenseVector(new double[] {0.9, 0.6, 0.2}));
        assertEquals(0, minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertEquals(40.4401, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.0001);

        coeffs = minnyTheMarquardtMinimizer.doOneIteration();
        assertEquals(1, minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertEquals(0.258454, coeffs.get(0), 0.000001);
        assertEquals(0.471076, coeffs.get(1), 0.000001);
        assertEquals(0.0464834, coeffs.get(2), 1E-7);
        assertEquals(5.70526, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.00001);

        coeffs = minnyTheMarquardtMinimizer.doOneIteration();
        assertEquals(2, minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertEquals(0.205139, coeffs.get(0), 0.000001);
        assertEquals(0.504708, coeffs.get(1), 0.000001);
        assertEquals(0.107990, coeffs.get(2), 0.000001);
        assertEquals(1.06955, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.00001);

        target1.setTheTargetValue(5);
        target2.setTheTargetValue(1);

        minnyTheMarquardtMinimizer.reset(new DenseVector(new double[] {0.2, 0.5, 0.1}));
        assertEquals(0, minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertEquals(78.3321, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.0001);

        coeffs = minnyTheMarquardtMinimizer.doOneIteration();
        assertEquals(1, minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertEquals(0.510563, coeffs.get(0), 0.000001);
        assertEquals(0.368707, coeffs.get(1), 0.000001);
        assertEquals(-0.265405, coeffs.get(2), 0.000001);
        assertEquals(11.2046, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.0001);

        // For the second iteration, the Gauss step is invalid, so the only
        // thing I know is that
        // the Marquardt correction needs to work here i.e. this iteration's
        // objective must be
        // less than the previous iteration's objective.
        double lastIterationObjective = minnyTheMarquardtMinimizer.getCurrentObjectiveValue();
        minnyTheMarquardtMinimizer.doOneIteration();
        assertEquals(2, minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertTrue(lastIterationObjective > minnyTheMarquardtMinimizer.getCurrentObjectiveValue());

        minnyTheMarquardtMinimizer.reset(new DenseVector(new double[] {0.9, 0.6, 0.2}));
        assertEquals(0, minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertEquals(151.777, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.001);

        coeffs = minnyTheMarquardtMinimizer.doOneIteration();
        assertEquals(1, minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertEquals(1.16263, coeffs.get(0), 0.00001);
        assertEquals(-0.126672, coeffs.get(1), 0.000001);
        assertEquals(-0.823543, coeffs.get(2), 0.000001);
        assertEquals(77.7507, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.0001);

        lastIterationObjective = minnyTheMarquardtMinimizer.getCurrentObjectiveValue();
        minnyTheMarquardtMinimizer.doOneIteration();
        assertEquals(2, minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertTrue(lastIterationObjective > minnyTheMarquardtMinimizer.getCurrentObjectiveValue());
    }

    @Test
    public void testIterateToConvergence() throws OptimizationException
    {
        target1.setTheTargetValue(1);
        target2.setTheTargetValue(5);

        minnyTheMarquardtMinimizer.reset(new DenseVector(new double[] {0.2, 0.5, 0.1}));
        minnyTheMarquardtMinimizer.setOptimizationParameters(1E-7, 1, 0.01);
        assertEquals(0, minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertFalse(minnyTheMarquardtMinimizer.lastRunConverged());
        assertFalse(minnyTheMarquardtMinimizer.lastRunMaxIterations());
        assertFalse(minnyTheMarquardtMinimizer.lastRunInvalidStep());
        assertEquals(1.33012, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.00001);

        Vector coeffs = minnyTheMarquardtMinimizer.iterateToConvergence(EPSILON, 50);
        assertEquals(22, minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertTrue(minnyTheMarquardtMinimizer.lastRunConverged());
        assertFalse(minnyTheMarquardtMinimizer.lastRunMaxIterations());
        assertFalse(minnyTheMarquardtMinimizer.lastRunInvalidStep());
        assertEquals(0.176167, coeffs.get(0), 0.000001);
        assertEquals(0.523641, coeffs.get(1), 0.000001);
        assertEquals(0.136034, coeffs.get(2), 0.000001);
        assertEquals(0.846541, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.000001);

        minnyTheMarquardtMinimizer.reset(new DenseVector(new double[] {0.9, 0.6, 0.2}));
        assertTrue(minnyTheMarquardtMinimizer.lastRunConverged());
        assertFalse(minnyTheMarquardtMinimizer.lastRunMaxIterations());
        assertFalse(minnyTheMarquardtMinimizer.lastRunInvalidStep());
        coeffs = minnyTheMarquardtMinimizer.iterateToConvergence(EPSILON, 50);
        assertEquals(25, minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertTrue(minnyTheMarquardtMinimizer.lastRunConverged());
        assertFalse(minnyTheMarquardtMinimizer.lastRunMaxIterations());
        assertFalse(minnyTheMarquardtMinimizer.lastRunInvalidStep());
        assertEquals(0.176167, coeffs.get(0), 0.000001);
        assertEquals(0.523641, coeffs.get(1), 0.000001);
        assertEquals(0.136033, coeffs.get(2), 0.000001);
        assertEquals(0.846541, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.000001);

        target1.setTheTargetValue(5);
        target2.setTheTargetValue(1);

        minnyTheMarquardtMinimizer.reset(new DenseVector(new double[] {0.2, 0.5, 0.1}));
        minnyTheMarquardtMinimizer.setOptimizationParameters(1E-7, 1, 0.01);
        assertEquals(0, minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertTrue(minnyTheMarquardtMinimizer.lastRunConverged());
        assertFalse(minnyTheMarquardtMinimizer.lastRunMaxIterations());
        assertFalse(minnyTheMarquardtMinimizer.lastRunInvalidStep());
        assertEquals(78.3321, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.0001);

        coeffs = minnyTheMarquardtMinimizer.iterateToConvergence(EPSILON, 50);
        assertTrue(15 >= minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertTrue(minnyTheMarquardtMinimizer.lastRunConverged());
        assertFalse(minnyTheMarquardtMinimizer.lastRunMaxIterations());
        assertFalse(minnyTheMarquardtMinimizer.lastRunInvalidStep());
        assertEquals(0.560726, coeffs.get(0), 0.000001);
        assertEquals(0.245360, coeffs.get(1), 0.000001);
        assertEquals(-0.148403, coeffs.get(2), 0.000001);
        assertEquals(9.49604, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.00001);

        minnyTheMarquardtMinimizer.reset(new DenseVector(new double[] {0.9, 0.6, 0.2}));
        assertTrue(minnyTheMarquardtMinimizer.lastRunConverged());
        assertFalse(minnyTheMarquardtMinimizer.lastRunMaxIterations());
        assertFalse(minnyTheMarquardtMinimizer.lastRunInvalidStep());
        coeffs = minnyTheMarquardtMinimizer.iterateToConvergence(EPSILON, 50);
        assertTrue(29 >= minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertTrue(minnyTheMarquardtMinimizer.lastRunConverged());
        assertFalse(minnyTheMarquardtMinimizer.lastRunMaxIterations());
        assertFalse(minnyTheMarquardtMinimizer.lastRunInvalidStep());
        assertEquals(0.560726, coeffs.get(0), 0.000001);
        assertEquals(0.245360, coeffs.get(1), 0.000001);
        assertEquals(-0.148403, coeffs.get(2), 0.000001);
        assertEquals(9.49604, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.00001);

        // Check termination by maximum iterations.
        minnyTheMarquardtMinimizer.reset(new DenseVector(new double[] {0.9, 0.6, 0.2}));
        assertTrue(minnyTheMarquardtMinimizer.lastRunConverged());
        assertFalse(minnyTheMarquardtMinimizer.lastRunMaxIterations());
        assertFalse(minnyTheMarquardtMinimizer.lastRunInvalidStep());
        minnyTheMarquardtMinimizer.iterateToConvergence(EPSILON, 15);
        assertEquals(15, minnyTheMarquardtMinimizer.getNumberOfIterations());
        assertFalse(minnyTheMarquardtMinimizer.lastRunConverged());
        assertTrue(minnyTheMarquardtMinimizer.lastRunMaxIterations());
        assertFalse(minnyTheMarquardtMinimizer.lastRunInvalidStep());
        assertEquals(0.5607, coeffs.get(0), 0.0001);
        assertEquals(0.2454, coeffs.get(1), 0.0001);
        assertEquals(-0.1484, coeffs.get(2), 0.0001);
        assertEquals(9.49604, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.00001);
    }

    // @Test
    public void testMinimize() throws OptimizationException
    {
        // Tests with constraints.
        target1.setTheTargetValue(5);
        target2.setTheTargetValue(1);

        final Constraint cons1 = new SimpleBoundary(0, 0, true);
        final Constraint cons2 = new SimpleBoundary(1, 0, true);
        final Constraint cons3 = new SimpleBoundary(2, 0, true);

        minnyTheMarquardtMinimizer.addConstraint(cons1);
        minnyTheMarquardtMinimizer.addConstraint(cons2);
        minnyTheMarquardtMinimizer.addConstraint(cons3);

        minnyTheMarquardtMinimizer.resetPenalty(new DenseVector(new double[] {0.2, 0.5, 0.1}));
        minnyTheMarquardtMinimizer.setOptimizationParameters(1E-7, 1, 0.01);

        final Vector coeffs = minnyTheMarquardtMinimizer.minimize(EPSILON, 50, EPSILON, 50);
        assertTrue(minnyTheMarquardtMinimizer.lastRunConverged());
        assertFalse(minnyTheMarquardtMinimizer.lastRunMaxIterations());
        assertFalse(minnyTheMarquardtMinimizer.lastRunInvalidStep());
        assertTrue(minnyTheMarquardtMinimizer.lastRunPenaltyConverged());
        assertFalse(minnyTheMarquardtMinimizer.lastRunPenaltyMaxIterations());
        System.out.println(minnyTheMarquardtMinimizer.getCurrentObjectiveValue());
        System.out.println(coeffs);
        assertEquals(0.535144, coeffs.get(0), 0.000001);
        assertEquals(0.066437, coeffs.get(1), 0.000001);
        assertEquals(0.0, coeffs.get(2), 0.000001);
        assertEquals(14.3776, minnyTheMarquardtMinimizer.getCurrentObjectiveValue(), 0.0001);
    }

    private static final class TestModel
            implements DifferentiableModel
    {
        List<Coefficient> myCoeffs;
        LogitModel        myModel;

        private TestModel(LogitModel model)
        {
            myModel = model;
            myCoeffs = new ArrayList<Coefficient>();
            for (final Alternative alt : myModel.getAlternatives())
            {
                myCoeffs.add(((TestAlternative) alt).getIntensityDispersion());
            }
            myCoeffs.add(myModel.getDispersionParameterAsCoeff());
        }

        @Override
        public Vector getTargetValues(List<EstimationTarget> targets, Vector params)
                throws OptimizationException
        {
            int i = 0;
            for (final Coefficient coeff : myCoeffs)
            {
                coeff.setValue(params.get(i));
                i++;
            }
            Vector result;
            try
            {
                final List<ExpectedValue> expvalues = new ArrayList<ExpectedValue>();
                for (final EstimationTarget t : targets)
                {
                    expvalues.addAll(t.getAssociatedExpectedValues());
                }
                result = myModel.getExpectedTargetValues(expvalues);
            } catch (final ChoiceModelOverflowException e)
            {
                throw new OptimizationException(e);
            } catch (final NoAlternativeAvailable e)
            {
                throw new OptimizationException(e);
            }
            return result;
        }

        @Override
        public Matrix getJacobian(List<EstimationTarget> targets, Vector params)
                throws OptimizationException
        {
            int i = 0;
            for (final Coefficient coeff : myCoeffs)
            {
                coeff.setValue(params.get(i));
                i++;
            }
            Matrix result;
            try
            {
                final List<ExpectedValue> expvalues = new ArrayList<ExpectedValue>();
                for (final EstimationTarget t : targets)
                {
                    expvalues.addAll(t.getAssociatedExpectedValues());
                }
                result = myModel.getExpectedTargetDerivativesWRTParameters(expvalues, myCoeffs);
            } catch (final ChoiceModelOverflowException e)
            {
                throw new OptimizationException(e);
            } catch (final NoAlternativeAvailable e)
            {
                throw new OptimizationException(e);
            }
            return result;
        }
    }

    private static final class TestAlternative
            extends DevelopmentAlternative
    {
        private final Coefficient   intensityDispersion;

        private final double        utilPerLand;
        private final double        utilPerSpace;
        private final double[]      intensityPoints;
        private final double[]      perSpaceAdjustments;
        private final double[]      perLandAdjustments;
        private final int           mySpacetype;

        private static final double landArea = 43560;

        private TestAlternative(int spacetype, double utilityPerLand, double utilityPerSpace,
                double minFAR, double maxFAR, Coefficient dispersion)
        {
            mySpacetype = spacetype;
            utilPerLand = utilityPerLand;
            utilPerSpace = utilityPerSpace;
            intensityDispersion = dispersion;

            intensityPoints = new double[] {minFAR, maxFAR};
            perSpaceAdjustments = new double[] {0};
            perLandAdjustments = new double[] {0};
        }

        @Override
        public Vector getExpectedTargetValues(List<ExpectedValue> ts)
                throws NoAlternativeAvailable, ChoiceModelOverflowException
        {
            final double expvalue = getExpectedFAR(intensityDispersion.getValue(), landArea,
                    utilPerSpace, utilPerLand, intensityPoints, perSpaceAdjustments,
                    perLandAdjustments);

            final Vector result = new DenseVector(ts.size());

            int i = 0;
            for (final ExpectedValue target : ts)
            {
                result.set(i, target.getModelledTotalNewValueForParcel(mySpacetype, 0, expvalue));
                i++;
            }

            return result;
        }

        @Override
        public Vector getUtilityDerivativesWRTParameters(List<Coefficient> cs)
                throws NoAlternativeAvailable, ChoiceModelOverflowException
        {
            final Vector derivs = getUtilityDerivativesWRTParameters(
                    intensityDispersion.getValue(), landArea, utilPerSpace, utilPerLand,
                    intensityPoints, perSpaceAdjustments, perLandAdjustments);

            final Vector result = new DenseVector(cs.size());
            result.set(cs.indexOf(intensityDispersion), derivs.get(derivs.size() - 1));

            return result;
        }

        @Override
        public Matrix getExpectedTargetDerivativesWRTParameters(List<ExpectedValue> ts,
                List<Coefficient> cs) throws NoAlternativeAvailable, ChoiceModelOverflowException
        {
            final Vector derivs = getExpectedFARDerivativesWRTParameters(
                    intensityDispersion.getValue(), landArea, utilPerSpace, utilPerLand,
                    intensityPoints, perSpaceAdjustments, perLandAdjustments);

            final double expvalue = getExpectedFAR(intensityDispersion.getValue(), landArea,
                    utilPerSpace, utilPerLand, intensityPoints, perSpaceAdjustments,
                    perLandAdjustments);

            final Matrix result = new DenseMatrix(ts.size(), cs.size());

            final int dispIndex = cs.indexOf(intensityDispersion);
            int i = 0;
            for (final ExpectedValue target : ts)
            {
                double deriv = derivs.get(derivs.size() - 1);
                deriv = deriv
                        * target.getModelledTotalNewDerivativeWRTNewSpace(mySpacetype, 0, expvalue);
                result.set(i, dispIndex, deriv);
                i++;
            }
            return result;
        }

        @Override
        public double getUtility(double dispersionParameterForSizeTermCalculation)
                throws ChoiceModelOverflowException
        {
            return getCompositeUtility(intensityDispersion.getValue(), landArea, utilPerSpace,
                    utilPerLand, intensityPoints, perSpaceAdjustments, perLandAdjustments);
        }

        @Override
        public double getUtilityNoSizeEffect() throws ChoiceModelOverflowException
        {
            throw new RuntimeException("Method not implemented");
        }

        @Override
        public void doDevelopment()
        {
        }

        public Coefficient getIntensityDispersion()
        {
            return intensityDispersion;
        }

        @Override
        public void startCaching()
        {
        }

        @Override
        public void endCaching()
        {
        }
    }

    private static final class TestCoefficient
            implements Coefficient
    {
        private final String myName;
        private double       myValue;

        private TestCoefficient(String name)
        {
            myName = name;
        }

        @Override
        public double getValue()
        {
            return myValue;
        }

        @Override
        public void setValue(double v)
        {
            myValue = v;
        }

        @Override
        public String getName()
        {
            return myName;
        }

        @Override
        public double getTransformedValue()
        {
            return getValue();
        }

        @Override
        public void setTransformedValue(double v)
        {
            setValue(v);
        }

        @Override
        public double getTransformationDerivative()
        {
            return 1;
        }

        @Override
        public double getInverseTransformationDerivative()
        {
            return 1;
        }

    }

    private static final class TestTarget
            extends EstimationTarget
            implements ExpectedValue
    {
        private final int mySpacetype;
        private double    modelledValue;
        private double[]  derivs;

        private TestTarget(int spacetype)
        {
            mySpacetype = spacetype;
        }

        private void setTheTargetValue(double target)
        {
            super.setTargetValue(target);
        }

        @Override
        public double getModelledTotalNewValueForParcel(int spacetype, double expectedAddedSpace,
                double expectedNewSpace)
        {
            if (spacetype == mySpacetype)
            {
                return expectedNewSpace + expectedAddedSpace;
            } else
            {
                return 0;
            }
        }

        @Override
        public double getModelledTotalNewDerivativeWRTAddedSpace(int spacetype,
                double expectedAddedSpace, double expectedNewSpace)
        {
            if (spacetype == mySpacetype)
            {
                return 1;
            } else
            {
                return 0;
            }
        }

        @Override
        public double getModelledTotalNewDerivativeWRTNewSpace(int spacetype,
                double expectedAddedSpace, double expectedNewSpace)
        {
            if (spacetype == mySpacetype)
            {
                return 1;
            } else
            {
                return 0;
            }
        }

        @Override
        public boolean appliesToCurrentParcel()
        {
            return true;
        }

        @Override
        public String getName()
        {
            return "Test target " + mySpacetype;
        }

        @Override
        public void setModelledValue(double value)
        {
            modelledValue = value;
        }

        @Override
        public double getModelledValue()
        {
            return modelledValue;
        }

        @Override
        public List<ExpectedValue> getAssociatedExpectedValues()
        {
            return Collections.<ExpectedValue> singletonList(this);
        }

        @Override
        public void setDerivatives(double[] derivatives)
        {
            derivs = Arrays.copyOf(derivatives, derivatives.length);
        }

        @Override
        public double[] getDerivatives()
        {
            return Arrays.copyOf(derivs, derivs.length);
        }
    }

}
