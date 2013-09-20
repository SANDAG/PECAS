package com.hbaspecto.pecas.sd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import no.uib.cipr.matrix.Vector;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
// TODO: Fix test
public class TestDevelopmentAlternativeEstimationMethods
{

    private final double epsilon  = 8e-6;
    private final double increase = epsilon * 1.01;

    @Test
    public void testGetCompositeUtilityTwoRangesWithAdjustments()
    {
        // The variables to test.
        double perSpace = 3.59177038715575;
        final double perLand = -1.49337546097972;
        final double landSize = 43560;
        final double[] stepPoints = new double[] {1, 1.8, 4};
        final double[] perLandAdjustments = new double[] {0, -5.97350184391888};
        final double[] perSpaceAdjustments = {1.9515430524083, -6.50514350802766};
        final double intensityDispersion = 0.2;

        double computil = DevelopmentAlternative.getCompositeUtility(intensityDispersion, landSize,
                perSpace, perLand, stepPoints, perSpaceAdjustments, perLandAdjustments);

        assertEquals(7.990412, computil, 0.00001);

        // Test if one of the space utilities is zero.
        perSpace = -perSpaceAdjustments[1];
        assertEquals(perSpace + perSpaceAdjustments[1], 0.0);

        computil = DevelopmentAlternative.getCompositeUtility(intensityDispersion, landSize,
                perSpace, perLand, stepPoints, perSpaceAdjustments, perLandAdjustments);

        assertEquals(14.22604, computil, 0.0001);

        // Test if one of the space utilities close enough to zero to be caught
        // by the epsilon check.
        perSpace = Math.nextUp(perSpace);
        assertEquals(perSpace + perSpaceAdjustments[1], 0, epsilon);
        assertFalse(perSpace + perSpaceAdjustments[1] == 0);

        computil = DevelopmentAlternative.getCompositeUtility(intensityDispersion, landSize,
                perSpace, perLand, stepPoints, perSpaceAdjustments, perLandAdjustments);

        assertEquals(14.22604, computil, 0.0001);

        // Test if one of the space utilities is close to zero, but not close
        // enough to be caught by epsilon.
        perSpace = -perSpaceAdjustments[1] + increase;

        computil = DevelopmentAlternative.getCompositeUtility(intensityDispersion, landSize,
                perSpace, perLand, stepPoints, perSpaceAdjustments, perLandAdjustments);

        assertEquals(14.22604, computil, 0.0001);

        // Test if the minimum FAR is above the step point.
        perSpace = 3.59177038715575;
        stepPoints[1] = 0;

        computil = DevelopmentAlternative.getCompositeUtility(intensityDispersion, landSize,
                perSpace, perLand, stepPoints, perSpaceAdjustments, perLandAdjustments);

        assertEquals(-8.636134, computil, 0.00001);

        // Test if maximum FAR is below the step point.
        stepPoints[1] = 5;

        computil = DevelopmentAlternative.getCompositeUtility(intensityDispersion, landSize,
                perSpace, perLand, stepPoints, perSpaceAdjustments, perLandAdjustments);

        assertEquals(19.98111, computil, 0.0001);
    }

    @Test
    public void testGetCompositeUtility()
    {
        // Testing with multiple step points.
        final double dispersionParameter = 0.2;
        final double landArea = 43560;

        final double minFar = 1;
        final double maxFar = 4;

        final double[] intensityPoints = new double[5];
        intensityPoints[0] = minFar;
        intensityPoints[4] = maxFar;
        intensityPoints[1] = 1.8;
        intensityPoints[2] = 2.4;
        intensityPoints[3] = 3.2;

        final double[] perSpaceAdjustments = new double[] {-2.5, 1.5, -4.5, -3.8};

        final double[] perLandAdjustments = new double[] {0.5, 1, -0.6, -2.2};

        final double perSpaceInitial = 3.59177038715575;
        final double perLandInitial = -1.49337546097972;

        double computil = DevelopmentAlternative.getCompositeUtility(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);

        assertEquals(8.032602, computil, 0.00001);

        // With various ranges missing.
        intensityPoints[1] = 0.0;
        computil = DevelopmentAlternative.getCompositeUtility(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(15.15193, computil, 0.0001);
        intensityPoints[2] = 0.2;
        computil = DevelopmentAlternative.getCompositeUtility(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(3.533560, computil, 0.00001);
        intensityPoints[3] = 0.4;
        computil = DevelopmentAlternative.getCompositeUtility(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(3.102363, computil, 0.00001);
        intensityPoints[1] = 1.8;
        intensityPoints[2] = 2.4;
        intensityPoints[3] = 5.0;
        computil = DevelopmentAlternative.getCompositeUtility(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(8.509154, computil, 0.00001);
        intensityPoints[2] = 4.8;
        computil = DevelopmentAlternative.getCompositeUtility(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(12.87193, computil, 0.0001);
        intensityPoints[1] = 4.6;
        computil = DevelopmentAlternative.getCompositeUtility(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(7.318192, computil, 0.00001);
        intensityPoints[1] = 0;
        intensityPoints[2] = 2.4;
        computil = DevelopmentAlternative.getCompositeUtility(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(15.63587, computil, 0.0001);
        intensityPoints[2] = 0.2;
        computil = DevelopmentAlternative.getCompositeUtility(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(3.890826, computil, 0.00001);
        intensityPoints[2] = 4.8;
        computil = DevelopmentAlternative.getCompositeUtility(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(20.04144, computil, 0.0001);
    }

    @Test
    public void testGetTwoRangeUtilityDerivativesWRTParameters()
    {
        // The variables to test.
        double perSpace = 3.59177038715575;
        final double perLand = -1.49337546097972;
        final double landSize = 43560;
        final double[] stepPoints = new double[] {1, 1.8, 4};
        final double[] perLandAdjustments = new double[] {0, -5.97350184391888};
        final double[] perSpaceAdjustments = {1.9515430524083, -6.50514350802766};
        final double intensityDispersion = 0.2;

        Vector answers = DevelopmentAlternative.getUtilityDerivativesWRTParameters(
                intensityDispersion, landSize, perSpace, perLand, stepPoints, perSpaceAdjustments,
                perLandAdjustments);

        // wrt step point.
        assertEquals(7.353069, answers.get(1), 0.00001);
        // wrt below step point.
        assertEquals(1.599958, answers.get(3), 0.00001);
        // wrt above step point.
        assertEquals(0.361064, answers.get(4), 0.00001);
        // wrt step point adjust.
        assertEquals(0.414453, answers.get(6), 0.00001);
        // wrt dispersion.
        assertEquals(-20.71184, answers.get(7), 0.0001);

        // Test if one of the space utilities is zero.
        perSpace = -perSpaceAdjustments[1];
        assertEquals(perSpace + perSpaceAdjustments[1], 0.0);

        answers = DevelopmentAlternative.getUtilityDerivativesWRTParameters(intensityDispersion,
                landSize, perSpace, perLand, stepPoints, perSpaceAdjustments, perLandAdjustments);

        assertEquals(8.255938, answers.get(1), 0.00001);
        assertEquals(1.675987, answers.get(3), 0.00001);
        assertEquals(0.66338, answers.get(4), 0.00001);
        assertEquals(0.603072, answers.get(6), 0.00001);
        assertEquals(-25.74285, answers.get(7), 0.0001);

        // Test if one of the space utilities close enough to zero to be caught
        // by the epsilon check.
        perSpace = Math.nextUp(perSpace);
        assertEquals(perSpace + perSpaceAdjustments[1], 0, epsilon);
        assertFalse(perSpace + perSpaceAdjustments[1] == 0);

        answers = DevelopmentAlternative.getUtilityDerivativesWRTParameters(intensityDispersion,
                landSize, perSpace, perLand, stepPoints, perSpaceAdjustments, perLandAdjustments);

        assertEquals(8.255938, answers.get(1), 0.00001);
        assertEquals(1.675987, answers.get(3), 0.00001);
        assertEquals(0.66338, answers.get(4), 0.00001);
        assertEquals(0.603072, answers.get(6), 0.00001);
        assertEquals(-25.74285, answers.get(7), 0.0001);

        // Test if one of the space utilities is close to zero, but not close
        // enough to be caught by epsilon.
        perSpace = -perSpaceAdjustments[1] + increase;

        answers = DevelopmentAlternative.getUtilityDerivativesWRTParameters(intensityDispersion,
                landSize, perSpace, perLand, stepPoints, perSpaceAdjustments, perLandAdjustments);

        assertEquals(8.255938, answers.get(1), 0.00001);
        assertEquals(1.675987, answers.get(3), 0.00001);
        assertEquals(0.66338, answers.get(4), 0.00001);
        assertEquals(0.603072, answers.get(6), 0.00001);
        assertEquals(-25.74285, answers.get(7), 0.0001);

        // Test if the range is entirely above the step point.
        perSpace = 3.59177038715575;
        stepPoints[1] = 0;

        answers = DevelopmentAlternative.getUtilityDerivativesWRTParameters(intensityDispersion,
                landSize, perSpace, perLand, stepPoints, perSpaceAdjustments, perLandAdjustments);

        assertEquals(8.456687, answers.get(1), 0.00001);
        assertEquals(0.0, answers.get(3), 0.00001);
        assertEquals(2.083745, answers.get(4), 0.00001);
        assertEquals(1.0, answers.get(6), 0.00001);
        assertEquals(-24.50735, answers.get(7), 0.0001);

        // Test if the range is entirely below the step point.
        stepPoints[1] = 5;

        answers = DevelopmentAlternative.getUtilityDerivativesWRTParameters(intensityDispersion,
                landSize, perSpace, perLand, stepPoints, perSpaceAdjustments, perLandAdjustments);

        assertEquals(0.0, answers.get(1), 0.00001);
        assertEquals(3.209842, answers.get(3), 0.00001);
        assertEquals(0.0, answers.get(4), 0.00001);
        assertEquals(0.0, answers.get(6), 0.00001);
        assertEquals(-18.40663, answers.get(7), 0.0001);
    }

    @Test
    public void testGetUtilityDerivativesWRTParameters()
    {
        // Testing with multiple step points.
        final double dispersionParameter = 0.2;
        final double landArea = 43560;

        final double minFar = 1;
        final double maxFar = 4;

        final double[] intensityPoints = new double[5];
        intensityPoints[0] = minFar;
        intensityPoints[4] = maxFar;
        intensityPoints[1] = 1.8;
        intensityPoints[2] = 2.4;
        intensityPoints[3] = 3.2;

        final double[] perSpaceAdjustments = new double[] {-2.5, 1.5, -4.5, -3.8};

        final double[] perLandAdjustments = new double[] {0.5, 1, -0.6, -2.2};

        final double perSpaceInitial = 3.59177038715575;
        final double perLandInitial = -1.49337546097972;

        Vector answers = DevelopmentAlternative.getUtilityDerivativesWRTParameters(
                dispersionParameter, landArea, perSpaceInitial, perLandInitial, intensityPoints,
                perSpaceAdjustments, perLandAdjustments);

        // Step points.
        assertEquals(-1.022854, answers.get(0), 0.00001);
        assertEquals(-3.554405, answers.get(1), 0.00001);
        assertEquals(3.760348, answers.get(2), 0.00001);
        assertEquals(0.599163, answers.get(3), 0.00001);
        assertEquals(1.309517, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(1.730553, answers.get(5), 0.00001);
        assertEquals(0.426347, answers.get(6), 0.00001);
        assertEquals(0.311742, answers.get(7), 0.00001);
        assertEquals(0.084748, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(1.0, answers.get(9), 0.00001);
        assertEquals(0.821179, answers.get(10), 0.00001);
        assertEquals(0.575068, answers.get(11), 0.00001);
        assertEquals(0.213052, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(-26.29554, answers.get(13), 0.0001);

        // With various ranges missing.
        intensityPoints[1] = 0.0;
        answers = DevelopmentAlternative.getUtilityDerivativesWRTParameters(dispersionParameter,
                landArea, perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        // Step points.
        assertEquals(-0.669470, answers.get(0), 0.00001);
        assertEquals(-4.0, answers.get(1), 0.00001);
        assertEquals(3.821514, answers.get(2), 0.00001);
        assertEquals(0.608909, answers.get(3), 0.00001);
        assertEquals(1.330818, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(0.0, answers.get(5), 0.00001);
        assertEquals(2.175985, answers.get(6), 0.00001);
        assertEquals(0.316813, answers.get(7), 0.00001);
        assertEquals(0.086126, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(1.0, answers.get(9), 0.00001);
        assertEquals(1.0, answers.get(10), 0.00001);
        assertEquals(0.584422, answers.get(11), 0.00001);
        assertEquals(0.216518, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(-25.99176, answers.get(13), 0.0001);

        intensityPoints[2] = 0.2;
        answers = DevelopmentAlternative.getUtilityDerivativesWRTParameters(dispersionParameter,
                landArea, perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        // Step points.
        assertEquals(-2.321944, answers.get(0), 0.00001);
        assertEquals(-4.0, answers.get(1), 0.00001);
        assertEquals(6.0, answers.get(2), 0.00001);
        assertEquals(0.443786, answers.get(3), 0.00001);
        assertEquals(0.969929, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(0.0, answers.get(5), 0.00001);
        assertEquals(0.2, answers.get(6), 0.00001);
        assertEquals(2.012044, answers.get(7), 0.00001);
        assertEquals(0.062771, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(1.0, answers.get(9), 0.00001);
        assertEquals(1.0, answers.get(10), 0.00001);
        assertEquals(1.0, answers.get(11), 0.00001);
        assertEquals(0.157803, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(-26.48108, answers.get(13), 0.0001);

        intensityPoints[3] = 0.4;
        answers = DevelopmentAlternative.getUtilityDerivativesWRTParameters(dispersionParameter,
                landArea, perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        // Step points.
        assertEquals(-1.772949, answers.get(0), 0.00001);
        assertEquals(-4.0, answers.get(1), 0.00001);
        assertEquals(6.0, answers.get(2), 0.00001);
        assertEquals(-0.7, answers.get(3), 0.00001);
        assertEquals(1.564719, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(0.0, answers.get(5), 0.00001);
        assertEquals(0.2, answers.get(6), 0.00001);
        assertEquals(0.2, answers.get(7), 0.00001);
        assertEquals(2.068774, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(1.0, answers.get(9), 0.00001);
        assertEquals(1.0, answers.get(10), 0.00001);
        assertEquals(1.0, answers.get(11), 0.00001);
        assertEquals(1.0, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(-27.44905, answers.get(13), 0.0001);

        intensityPoints[1] = 1.8;
        intensityPoints[2] = 2.4;
        intensityPoints[3] = 5.0;
        answers = DevelopmentAlternative.getUtilityDerivativesWRTParameters(dispersionParameter,
                landArea, perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        // Step points.
        assertEquals(-0.929867, answers.get(0), 0.00001);
        assertEquals(-3.594914, answers.get(1), 0.00001);
        assertEquals(3.963953, answers.get(2), 0.00001);
        assertEquals(0.0, answers.get(3), 0.00001);
        assertEquals(1.652598, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(1.736867, answers.get(5), 0.00001);
        assertEquals(0.442133, answers.get(6), 0.00001);
        assertEquals(0.467210, answers.get(7), 0.00001);
        assertEquals(0.0, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(1.0, answers.get(9), 0.00001);
        assertEquals(0.837436, answers.get(10), 0.00001);
        assertEquals(0.613698, answers.get(11), 0.00001);
        assertEquals(0.0, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(-26.55073, answers.get(13), 0.0001);

        intensityPoints[2] = 4.8;
        answers = DevelopmentAlternative.getUtilityDerivativesWRTParameters(dispersionParameter,
                landArea, perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        // Step points.
        assertEquals(-0.388575, answers.get(0), 0.00001);
        assertEquals(-3.830722, answers.get(1), 0.00001);
        assertEquals(0.0, answers.get(2), 0.00001);
        assertEquals(0.0, answers.get(3), 0.00001);
        assertEquals(5.311067, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(1.773618, answers.get(5), 0.00001);
        assertEquals(1.379483, answers.get(6), 0.00001);
        assertEquals(0.0, answers.get(7), 0.00001);
        assertEquals(0.0, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(1.0, answers.get(9), 0.00001);
        assertEquals(0.932067, answers.get(10), 0.00001);
        assertEquals(0.0, answers.get(11), 0.00001);
        assertEquals(0.0, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(-19.86423, answers.get(13), 0.0001);

        intensityPoints[1] = 4.6;
        answers = DevelopmentAlternative.getUtilityDerivativesWRTParameters(dispersionParameter,
                landArea, perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        // Step points.
        assertEquals(-1.179958, answers.get(0), 0.00001);
        assertEquals(0.0, answers.get(1), 0.00001);
        assertEquals(0.0, answers.get(2), 0.00001);
        assertEquals(0.0, answers.get(3), 0.00001);
        assertEquals(2.271728, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(2.662606, answers.get(5), 0.00001);
        assertEquals(0.0, answers.get(6), 0.00001);
        assertEquals(0.0, answers.get(7), 0.00001);
        assertEquals(0.0, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(1.0, answers.get(9), 0.00001);
        assertEquals(0.0, answers.get(10), 0.00001);
        assertEquals(0.0, answers.get(11), 0.00001);
        assertEquals(0.0, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(-27.02306, answers.get(13), 0.00001);

        intensityPoints[1] = 0;
        intensityPoints[2] = 2.4;
        answers = DevelopmentAlternative.getUtilityDerivativesWRTParameters(dispersionParameter,
                landArea, perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        // Step points.
        assertEquals(-0.607711, answers.get(0), 0.00001);
        assertEquals(-4.0, answers.get(1), 0.00001);
        assertEquals(4.022482, answers.get(2), 0.00001);
        assertEquals(0.0, answers.get(3), 0.00001);
        assertEquals(1.676999, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(0.0, answers.get(5), 0.00001);
        assertEquals(2.196651, answers.get(6), 0.00001);
        assertEquals(0.474109, answers.get(7), 0.00001);
        assertEquals(0.0, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(1.0, answers.get(9), 0.00001);
        assertEquals(1.0, answers.get(10), 0.00001);
        assertEquals(0.622759, answers.get(11), 0.00001);
        assertEquals(0.0, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(-26.24329, answers.get(13), 0.0001);

        intensityPoints[2] = 0.2;
        answers = DevelopmentAlternative.getUtilityDerivativesWRTParameters(dispersionParameter,
                landArea, perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        // Step points.
        assertEquals(-2.161823, answers.get(0), 0.00001);
        assertEquals(-4.0, answers.get(1), 0.00001);
        assertEquals(6.0, answers.get(2), 0.00001);
        assertEquals(0.0, answers.get(3), 0.00001);
        assertEquals(1.253593, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(0.0, answers.get(5), 0.00001);
        assertEquals(0.2, answers.get(6), 0.00001);
        assertEquals(2.164435, answers.get(7), 0.00001);
        assertEquals(0.0, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(1.0, answers.get(9), 0.00001);
        assertEquals(1.0, answers.get(10), 0.00001);
        assertEquals(1.0, answers.get(11), 0.00001);
        assertEquals(0.0, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(-27.15826, answers.get(13), 0.0001);

        intensityPoints[2] = 4.8;
        answers = DevelopmentAlternative.getUtilityDerivativesWRTParameters(dispersionParameter,
                landArea, perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        // Step points.
        assertEquals(-0.251787, answers.get(0), 0.00001);
        assertEquals(-4.0, answers.get(1), 0.00001);
        assertEquals(0.0, answers.get(2), 0.00001);
        assertEquals(0.0, answers.get(3), 0.00001);
        assertEquals(5.343558, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(0.0, answers.get(5), 0.00001);
        assertEquals(3.166373, answers.get(6), 0.00001);
        assertEquals(0.0, answers.get(7), 0.00001);
        assertEquals(0.0, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(1.0, answers.get(9), 0.00001);
        assertEquals(1.0, answers.get(10), 0.00001);
        assertEquals(0.0, answers.get(11), 0.00001);
        assertEquals(0.0, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(-19.56185, answers.get(13), 0.0001);
    }

    @Test
    public void testGetExpectedFARTwoRangesWithAdjustments()
    {
        // The variables to test.
        double perSpace = 3.59177038715575;
        final double perLand = -1.49337546097972;
        final double landSize = 43560;
        final double[] stepPoints = new double[] {1, 1.8, 4};
        final double[] perLandAdjustments = new double[] {0, -5.97350184391888};
        final double[] perSpaceAdjustments = {1.9515430524083, -6.50514350802766};
        final double intensityDispersion = 0.2;

        double expvalue = DevelopmentAlternative.getExpectedFAR(intensityDispersion, landSize,
                perSpace, perLand, stepPoints, perSpaceAdjustments, perLandAdjustments);

        assertEquals(1.961022, expvalue, 0.00001);

        // Test if one of the space utilities is zero.
        perSpace = -perSpaceAdjustments[1];
        assertEquals(perSpace + perSpaceAdjustments[1], 0.0);

        expvalue = DevelopmentAlternative.getExpectedFAR(intensityDispersion, landSize, perSpace,
                perLand, stepPoints, perSpaceAdjustments, perLandAdjustments);

        assertEquals(2.339366, expvalue, 0.00001);

        // Test if one of the space utilities close enough to zero to be caught
        // by the epsilon check.
        perSpace = Math.nextUp(perSpace);
        assertEquals(perSpace + perSpaceAdjustments[1], 0, epsilon);
        assertFalse(perSpace + perSpaceAdjustments[1] == 0);

        expvalue = DevelopmentAlternative.getExpectedFAR(intensityDispersion, landSize, perSpace,
                perLand, stepPoints, perSpaceAdjustments, perLandAdjustments);

        assertEquals(2.339366, expvalue, 0.00001);

        // Test if one of the space utilities is close to zero, but not close
        // enough to be caught by epsilon.
        perSpace = -perSpaceAdjustments[1] + increase;

        expvalue = DevelopmentAlternative.getExpectedFAR(intensityDispersion, landSize, perSpace,
                perLand, stepPoints, perSpaceAdjustments, perLandAdjustments);

        assertEquals(2.339366, expvalue, 0.00001);

        // Test if the range is entirely above the step point.
        perSpace = 3.59177038715575;
        stepPoints[1] = 0;

        expvalue = DevelopmentAlternative.getExpectedFAR(intensityDispersion, landSize, perSpace,
                perLand, stepPoints, perSpaceAdjustments, perLandAdjustments);

        assertEquals(2.083745, expvalue, 0.00001);

        // Test if the range is entirely below the step point.
        stepPoints[1] = 5;

        expvalue = DevelopmentAlternative.getExpectedFAR(intensityDispersion, landSize, perSpace,
                perLand, stepPoints, perSpaceAdjustments, perLandAdjustments);

        assertEquals(3.209842, expvalue, 0.00001);
    }

    @Test
    public void testGetExpectedFAR()
    {
        // Testing with multiple step points.
        final double dispersionParameter = 0.2;
        final double landArea = 43560;

        final double minFar = 1;
        final double maxFar = 4;

        final double[] intensityPoints = new double[5];
        intensityPoints[0] = minFar;
        intensityPoints[4] = maxFar;
        intensityPoints[1] = 1.8;
        intensityPoints[2] = 2.4;
        intensityPoints[3] = 3.2;

        final double[] perSpaceAdjustments = new double[] {-2.5, 1.5, -4.5, -3.8};

        final double[] perLandAdjustments = new double[] {0.5, 1, -0.6, -2.2};

        final double perSpaceInitial = 3.59177038715575;
        final double perLandInitial = -1.49337546097972;

        double expvalue = DevelopmentAlternative.getExpectedFAR(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);

        assertEquals(2.553390, expvalue, 0.00001);

        // With various ranges missing.
        intensityPoints[1] = 0.0;
        expvalue = DevelopmentAlternative.getExpectedFAR(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(2.578924, expvalue, 0.00001);
        intensityPoints[2] = 0.2;
        expvalue = DevelopmentAlternative.getExpectedFAR(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(2.274815, expvalue, 0.00001);
        intensityPoints[3] = 0.4;
        expvalue = DevelopmentAlternative.getExpectedFAR(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(2.468774, expvalue, 0.00001);
        intensityPoints[1] = 1.8;
        intensityPoints[2] = 2.4;
        intensityPoints[3] = 5.0;
        expvalue = DevelopmentAlternative.getExpectedFAR(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(2.646210, expvalue, 0.00001);
        intensityPoints[2] = 4.8;
        expvalue = DevelopmentAlternative.getExpectedFAR(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(3.153101, expvalue, 0.00001);
        intensityPoints[1] = 4.6;
        expvalue = DevelopmentAlternative.getExpectedFAR(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(2.662606, expvalue, 0.00001);
        intensityPoints[1] = 0;
        intensityPoints[2] = 2.4;
        expvalue = DevelopmentAlternative.getExpectedFAR(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(2.670760, expvalue, 0.00001);
        intensityPoints[2] = 0.2;
        expvalue = DevelopmentAlternative.getExpectedFAR(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(2.364435, expvalue, 0.00001);
        intensityPoints[2] = 4.8;
        expvalue = DevelopmentAlternative.getExpectedFAR(dispersionParameter, landArea,
                perSpaceInitial, perLandInitial, intensityPoints, perSpaceAdjustments,
                perLandAdjustments);
        assertEquals(3.166373, expvalue, 0.00001);
    }

    @Test
    public void testGetTwoRangeExpectedFARDerivativesWRTParameters()
    {
        // The variables to test.
        double perSpace = 3.59177038715575;
        final double perLand = -1.49337546097972;
        final double landSize = 43560;
        final double[] stepPoints = new double[] {1, 1.8, 4};
        final double[] perLandAdjustments = new double[] {0, -5.97350184391888};
        final double[] perSpaceAdjustments = {1.9515430524083, -6.50514350802766};
        final double intensityDispersion = 0.2;

        Vector answers = DevelopmentAlternative.getExpectedFARDerivativesWRTParameters(
                intensityDispersion, landSize, perSpace, perLand, stepPoints, perSpaceAdjustments,
                perLandAdjustments);

        // wrt step point.
        assertEquals(0.373880, answers.get(1), 0.00001);
        // wrt below step point.
        assertEquals(0.026118, answers.get(3), 0.00001);
        // wrt above step point.
        assertEquals(0.082138, answers.get(4), 0.00001);
        // wrt step point adjust.
        assertEquals(0.058866, answers.get(6), 0.00001);
        // wrt dispersion.
        assertEquals(-2.230755, answers.get(7), 0.00001);

        // Test if one of the space utilities is zero.
        perSpace = -perSpaceAdjustments[1];
        assertEquals(perSpace + perSpaceAdjustments[1], 0.0);

        answers = DevelopmentAlternative.getExpectedFARDerivativesWRTParameters(
                intensityDispersion, landSize, perSpace, perLand, stepPoints, perSpaceAdjustments,
                perLandAdjustments);

        assertEquals(0.231404, answers.get(1), 0.00001);
        assertEquals(0.025000, answers.get(3), 0.00001);
        assertEquals(0.123030, answers.get(4), 0.00001);
        assertEquals(0.067621, answers.get(6), 0.00001);
        assertEquals(-0.962585, answers.get(7), 0.00001);

        // Test if one of the space utilities close enough to zero to be caught
        // by the epsilon check.
        perSpace = Math.nextUp(perSpace);
        assertEquals(perSpace + perSpaceAdjustments[1], 0, epsilon);
        assertFalse(perSpace + perSpaceAdjustments[1] == 0);

        answers = DevelopmentAlternative.getExpectedFARDerivativesWRTParameters(
                intensityDispersion, landSize, perSpace, perLand, stepPoints, perSpaceAdjustments,
                perLandAdjustments);

        assertEquals(0.231404, answers.get(1), 0.00001);
        assertEquals(0.025000, answers.get(3), 0.00001);
        assertEquals(0.123030, answers.get(4), 0.00001);
        assertEquals(0.067621, answers.get(6), 0.00001);
        assertEquals(-0.962585, answers.get(7), 0.00001);

        // Test if one of the space utilities is close to zero, but not close
        // enough to be caught by epsilon.
        perSpace = -perSpaceAdjustments[1] + increase;

        answers = DevelopmentAlternative.getExpectedFARDerivativesWRTParameters(
                intensityDispersion, landSize, perSpace, perLand, stepPoints, perSpaceAdjustments,
                perLandAdjustments);

        assertEquals(0.231404, answers.get(1), 0.00001);
        assertEquals(0.025000, answers.get(3), 0.00001);
        assertEquals(0.123030, answers.get(4), 0.00001);
        assertEquals(0.067621, answers.get(6), 0.00001);
        assertEquals(-0.962585, answers.get(7), 0.00001);

        // Test if the range is entirely above the step point.
        perSpace = 3.59177038715575;
        stepPoints[1] = 0;

        answers = DevelopmentAlternative.getExpectedFARDerivativesWRTParameters(
                intensityDispersion, landSize, perSpace, perLand, stepPoints, perSpaceAdjustments,
                perLandAdjustments);

        assertEquals(0.0, answers.get(1), 0.00001);
        assertEquals(0.0, answers.get(3), 0.00001);
        assertEquals(0.129592, answers.get(4), 0.00001);
        assertEquals(0.0, answers.get(6), 0.00001);
        assertEquals(-1.887743, answers.get(7), 0.00001);

        // Test if the range is entirely below the step point.
        stepPoints[1] = 5;

        answers = DevelopmentAlternative.getExpectedFARDerivativesWRTParameters(
                intensityDispersion, landSize, perSpace, perLand, stepPoints, perSpaceAdjustments,
                perLandAdjustments);

        assertEquals(0.0, answers.get(1), 0.00001);
        assertEquals(0.093117, answers.get(3), 0.00001);
        assertEquals(0.0, answers.get(4), 0.00001);
        assertEquals(0.0, answers.get(6), 0.00001);
        assertEquals(2.580891, answers.get(7), 0.00001);
    }

    @Test
    public void testGetExpectedFARDerivativesWRTParameters()
    {
        // Testing with multiple step points.
        final double dispersionParameter = 0.2;
        final double landArea = 43560;

        final double minFar = 1;
        final double maxFar = 4;

        final double[] intensityPoints = new double[5];
        intensityPoints[0] = minFar;
        intensityPoints[4] = maxFar;
        intensityPoints[1] = 1.8;
        intensityPoints[2] = 2.4;
        intensityPoints[3] = 3.2;

        final double[] perSpaceAdjustments = new double[] {-2.5, 1.5, -4.5, -3.8};

        final double[] perLandAdjustments = new double[] {0.5, 1, -0.6, -2.2};

        final double perSpaceInitial = 3.59177038715575;
        final double perLandInitial = -1.49337546097972;

        Vector answers = DevelopmentAlternative.getExpectedFARDerivativesWRTParameters(
                dispersionParameter, landArea, perSpaceInitial, perLandInitial, intensityPoints,
                perSpaceAdjustments, perLandAdjustments);

        // Step points.
        assertEquals(0.317778, answers.get(0), 0.00001);
        assertEquals(-0.122699, answers.get(1), 0.00001);
        assertEquals(0.360428, answers.get(2), 0.00001);
        assertEquals(0.065620, answers.get(3), 0.00001);
        assertEquals(0.378872, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(0.017763, answers.get(5), 0.00001);
        assertEquals(0.031564, answers.get(6), 0.00001);
        assertEquals(0.046154, answers.get(7), 0.00001);
        assertEquals(0.019974, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(0.0, answers.get(9), 0.00001);
        assertEquals(0.040834, answers.get(10), 0.00001);
        assertEquals(0.061656, answers.get(11), 0.00001);
        assertEquals(0.044502, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(0.199842, answers.get(13), 0.00001);

        // With various ranges missing.
        intensityPoints[1] = 0.0;
        answers = DevelopmentAlternative.getExpectedFARDerivativesWRTParameters(
                dispersionParameter, landArea, perSpaceInitial, perLandInitial, intensityPoints,
                perSpaceAdjustments, perLandAdjustments);
        // Step points.
        assertEquals(0.211409, answers.get(0), 0.00001);
        assertEquals(0.0, answers.get(1), 0.00001);
        assertEquals(0.346775, answers.get(2), 0.00001);
        assertEquals(0.063578, answers.get(3), 0.00001);
        assertEquals(0.378239, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(0.0, answers.get(5), 0.00001);
        assertEquals(0.044467, answers.get(6), 0.00001);
        assertEquals(0.045287, answers.get(7), 0.00001);
        assertEquals(0.019859, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(0.0, answers.get(9), 0.00001);
        assertEquals(0.0, answers.get(10), 0.00001);
        assertEquals(0.059674, answers.get(11), 0.00001);
        assertEquals(0.044120, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(0.241399, answers.get(13), 0.00001);

        intensityPoints[2] = 0.2;
        answers = DevelopmentAlternative.getExpectedFARDerivativesWRTParameters(
                dispersionParameter, landArea, perSpaceInitial, perLandInitial, intensityPoints,
                perSpaceAdjustments, perLandAdjustments);
        // Step points.
        assertEquals(0.592010, answers.get(0), 0.00001);
        assertEquals(0.0, answers.get(1), 0.00001);
        assertEquals(0.0, answers.get(2), 0.00001);
        assertEquals(0.073329, answers.get(3), 0.00001);
        assertEquals(0.334661, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(0.0, answers.get(5), 0.00001);
        assertEquals(0.0, answers.get(6), 0.00001);
        assertEquals(0.116378, answers.get(7), 0.00001);
        assertEquals(0.018292, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(0.0, answers.get(9), 0.00001);
        assertEquals(0.0, answers.get(10), 0.00001);
        assertEquals(0.0, answers.get(11), 0.00001);
        assertEquals(0.041753, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(-1.006822, answers.get(13), 0.00001);

        intensityPoints[3] = 0.4;
        answers = DevelopmentAlternative.getExpectedFARDerivativesWRTParameters(
                dispersionParameter, landArea, perSpaceInitial, perLandInitial, intensityPoints,
                perSpaceAdjustments, perLandAdjustments);
        // Step points.
        assertEquals(0.520812, answers.get(0), 0.00001);
        assertEquals(0.0, answers.get(1), 0.00001);
        assertEquals(0.0, answers.get(2), 0.00001);
        assertEquals(0.0, answers.get(3), 0.00001);
        assertEquals(0.479188, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(0.0, answers.get(5), 0.00001);
        assertEquals(0.0, answers.get(6), 0.00001);
        assertEquals(0.0, answers.get(7), 0.00001);
        assertEquals(0.149883, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(0.0, answers.get(9), 0.00001);
        assertEquals(0.0, answers.get(10), 0.00001);
        assertEquals(0.0, answers.get(11), 0.00001);
        assertEquals(0.0, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(-0.156050, answers.get(13), 0.00001);

        intensityPoints[1] = 1.8;
        intensityPoints[2] = 2.4;
        intensityPoints[3] = 5.0;
        answers = DevelopmentAlternative.getExpectedFARDerivativesWRTParameters(
                dispersionParameter, landArea, perSpaceInitial, perLandInitial, intensityPoints,
                perSpaceAdjustments, perLandAdjustments);
        // Step points.
        assertEquals(0.306151, answers.get(0), 0.00001);
        assertEquals(-0.119064, answers.get(1), 0.00001);
        assertEquals(0.365459, answers.get(2), 0.00001);
        assertEquals(0.0, answers.get(3), 0.00001);
        assertEquals(0.447454, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(0.017320, answers.get(5), 0.00001);
        assertEquals(0.031625, answers.get(6), 0.00001);
        assertEquals(0.074206, answers.get(7), 0.00001);
        assertEquals(0.0, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(0.0, answers.get(9), 0.00001);
        assertEquals(0.040139, answers.get(10), 0.00001);
        assertEquals(0.063222, answers.get(11), 0.00001);
        assertEquals(0.0, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(0.573745, answers.get(13), 0.00001);

        intensityPoints[2] = 4.8;
        answers = DevelopmentAlternative.getExpectedFARDerivativesWRTParameters(
                dispersionParameter, landArea, perSpaceInitial, perLandInitial, intensityPoints,
                perSpaceAdjustments, perLandAdjustments);
        // Step points.
        assertEquals(0.167328, answers.get(0), 0.00001);
        assertEquals(-0.066916, answers.get(1), 0.00001);
        assertEquals(0.0, answers.get(2), 0.00001);
        assertEquals(0.0, answers.get(3), 0.00001);
        assertEquals(0.899588, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(0.009912, answers.get(5), 0.00001);
        assertEquals(0.094527, answers.get(6), 0.00001);
        assertEquals(0.0, answers.get(7), 0.00001);
        assertEquals(0.0, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(0.0, answers.get(9), 0.00001);
        assertEquals(0.023660, answers.get(10), 0.00001);
        assertEquals(0.0, answers.get(11), 0.00001);
        assertEquals(0.0, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(2.578953, answers.get(13), 0.00001);

        intensityPoints[1] = 4.6;
        answers = DevelopmentAlternative.getExpectedFARDerivativesWRTParameters(
                dispersionParameter, landArea, perSpaceInitial, perLandInitial, intensityPoints,
                perSpaceAdjustments, perLandAdjustments);
        // Step points.
        assertEquals(0.392361, answers.get(0), 0.00001);
        assertEquals(0.0, answers.get(1), 0.00001);
        assertEquals(0.0, answers.get(2), 0.00001);
        assertEquals(0.0, answers.get(3), 0.00001);
        assertEquals(0.607639, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(0.146836, answers.get(5), 0.00001);
        assertEquals(0.0, answers.get(6), 0.00001);
        assertEquals(0.0, answers.get(7), 0.00001);
        assertEquals(0.0, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(0.0, answers.get(9), 0.00001);
        assertEquals(0.0, answers.get(10), 0.00001);
        assertEquals(0.0, answers.get(11), 0.00001);
        assertEquals(0.0, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(0.801554, answers.get(13), 0.00001);

        intensityPoints[1] = 0;
        intensityPoints[2] = 2.4;
        answers = DevelopmentAlternative.getExpectedFARDerivativesWRTParameters(
                dispersionParameter, landArea, perSpaceInitial, perLandInitial, intensityPoints,
                perSpaceAdjustments, perLandAdjustments);
        // Step points.
        assertEquals(0.203068, answers.get(0), 0.00001);
        assertEquals(0.0, answers.get(1), 0.00001);
        assertEquals(0.351105, answers.get(2), 0.00001);
        assertEquals(0.0, answers.get(3), 0.00001);
        assertEquals(0.445827, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(0.0, answers.get(5), 0.00001);
        assertEquals(0.044100, answers.get(6), 0.00001);
        assertEquals(0.072973, answers.get(7), 0.00001);
        assertEquals(0.0, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(0.0, answers.get(9), 0.00001);
        assertEquals(0.0, answers.get(10), 0.00001);
        assertEquals(0.061098, answers.get(11), 0.00001);
        assertEquals(0.0, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(0.608049, answers.get(13), 0.0001);

        intensityPoints[2] = 0.2;
        answers = DevelopmentAlternative.getExpectedFARDerivativesWRTParameters(
                dispersionParameter, landArea, perSpaceInitial, perLandInitial, intensityPoints,
                perSpaceAdjustments, perLandAdjustments);
        // Step points.
        assertEquals(0.589933, answers.get(0), 0.00001);
        assertEquals(0.0, answers.get(1), 0.00001);
        assertEquals(0.0, answers.get(2), 0.00001);
        assertEquals(0.0, answers.get(3), 0.00001);
        assertEquals(0.410067, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(0.0, answers.get(5), 0.00001);
        assertEquals(0.0, answers.get(6), 0.00001);
        assertEquals(0.147799, answers.get(7), 0.00001);
        assertEquals(0.0, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(0.0, answers.get(9), 0.00001);
        assertEquals(0.0, answers.get(10), 0.00001);
        assertEquals(0.0, answers.get(11), 0.00001);
        assertEquals(0.0, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(-0.671176, answers.get(13), 0.00001);

        intensityPoints[2] = 4.8;
        answers = DevelopmentAlternative.getExpectedFARDerivativesWRTParameters(
                dispersionParameter, landArea, perSpaceInitial, perLandInitial, intensityPoints,
                perSpaceAdjustments, perLandAdjustments);
        // Step points.
        assertEquals(0.109093, answers.get(0), 0.00001);
        assertEquals(0.0, answers.get(1), 0.00001);
        assertEquals(0.0, answers.get(2), 0.00001);
        assertEquals(0.0, answers.get(3), 0.00001);
        assertEquals(0.890907, answers.get(4), 0.00001);
        // Per-space adjustments.
        assertEquals(0.0, answers.get(5), 0.00001);
        assertEquals(0.099444, answers.get(6), 0.00001);
        assertEquals(0.0, answers.get(7), 0.00001);
        assertEquals(0.0, answers.get(8), 0.00001);
        // Per-land adjustments.
        assertEquals(0.0, answers.get(9), 0.00001);
        assertEquals(0.0, answers.get(10), 0.00001);
        assertEquals(0.0, answers.get(11), 0.00001);
        assertEquals(0.0, answers.get(12), 0.00001);
        // Dispersion parameter.
        assertEquals(2.531740, answers.get(13), 0.00001);
    }
}
