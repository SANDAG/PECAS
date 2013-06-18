package com.hbaspecto.pecas.sd;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

import org.apache.log4j.Logger;

import com.hbaspecto.discreteChoiceModelling.ParameterSearchAlternative;

public abstract class DevelopmentAlternative implements
		ParameterSearchAlternative {
	static Logger logger = Logger.getLogger(DevelopmentAlternative.class);

	// If the per space utility is less than this value in absolute, it will be
	// considered equal to zero.
	// This strange value of epsilon balances between the various sensitive
	// methods, providing acceptable error in all of them.
	// (if epsilon is too high, the approximation of utility = 0 is too rough
	// and causes errors in some outputs;
	// if epsilon is too low, some methods suffer large roundoff error).
	private static final double epsilon = 8e-6;
	private static final int NUM_INT_PARAMS = 5;
	private static final int IND_US = 0;
	private static final int IND_UA = 1;
	private static final int IND_SMIN = 2;
	private static final int IND_SMAX = 3;
	private static final int IND_DISP = 4;

	private static double square(double arg) {
		return arg * arg;
	}

	private static double cube(double arg) {
		return arg * arg * arg;
	}

	// Takes an array of row vectors and an array of matrices (which must be the
	// same length k), multiplies
	// each vector by the corresponding matrix, and aggregates the resulting row
	// vectors into a new matrix.
	// The vectors must be 1 by m, the matrices must be m by n (where n is any
	// integer).
	// Returns a k by n matrix.
	private static Matrix multiplyAndAggregate(int k, int m, int n,
			Matrix[] vectors, Matrix[] matrices) {
		final Matrix[] products = new Matrix[k];
		for (int i = 0; i < k; i++) {
			products[i] = new DenseMatrix(1, n);
			products[i] = vectors[i].mult(matrices[i], products[i]);
		}

		// Copy the matrix entries into the new matrix.
		final Matrix result = new DenseMatrix(k, n);
		for (int i = 0; i < k; i++) {
			for (int j = 0; j < n; j++) {
				result.set(i, j, products[i].get(0, j));
			}
		}

		return result;
	}

	// Removes degenerate ranges, storing the new parameters in the provided
	// arrays.
	private static void removeInvalidRanges(int numRanges, int numProperRanges,
			int lowestValidRange, int highestValidRange, double[] intensityPoints,
			double[] perSpaceAdjustments, double[] perLandAdjustments,
			double[] properIntensityPoints, double[] properPerSpaceAdjustments,
			double[] properPerLandAdjustments) {
		properPerLandAdjustments[0] = perLandAdjustments[0];
		for (int i = 0; i < lowestValidRange; i++) {
			properPerLandAdjustments[0] += perLandAdjustments[i + 1]
					+ (perSpaceAdjustments[i] - perSpaceAdjustments[i + 1])
					* intensityPoints[i + 1];
		}
		for (int i = 1; i < numProperRanges; i++) {
			properPerLandAdjustments[i] = perLandAdjustments[i + lowestValidRange];
		}

		for (int i = 0; i < numProperRanges; i++) {
			properPerSpaceAdjustments[i] = perSpaceAdjustments[i + lowestValidRange];
		}

		properIntensityPoints[0] = intensityPoints[0];
		properIntensityPoints[numProperRanges] = intensityPoints[numRanges];
		for (int i = 1; i < numProperRanges; i++) {
			properIntensityPoints[i] = intensityPoints[i + lowestValidRange];
		}
	}

	// Method that restores the actual relationships to the original parameters
	// when degenerate ranges are removed.
	private static Vector transformDerivativesForValidRanges(Vector derivs,
			int numRanges, int lowestValidRange, int highestValidRange,
			double[] perSpaceAdjustments, double[] perLandAdjustments,
			double[] stepPoints, double dispersion) {
		// Push minimum range from lowestValidRange back to zero.
		Vector result = derivs;
		for (int currentMin = lowestValidRange; currentMin > 0; currentMin--) {
			final int currentNumRanges = highestValidRange - currentMin + 1;
			final int newNumRanges = currentNumRanges + 1;

			// Old positions of the parameter types.
			final int firstStepPointPos = 0;
			final int firstPerSpacePos = currentNumRanges + 1;
			final int firstPerLandPos = 2 * currentNumRanges + 1;
			final int dispersionPos = 3 * currentNumRanges + 1;

			// New positions of the parameter types.
			final int newFirstStepPointPos = 0;
			final int newFirstPerSpacePos = newNumRanges + 1;
			final int newFirstPerLandPos = 2 * newNumRanges + 1;
			final int newDispersionPos = 3 * newNumRanges + 1;

			final Vector oldresult = result;
			result = new DenseVector(3 * newNumRanges + 2);

			// The step points.
			result.add(newFirstStepPointPos, oldresult.get(firstStepPointPos));
			for (int i = 1; i <= currentNumRanges; i++) {
				result.add(newFirstStepPointPos + i + 1,
						oldresult.get(firstStepPointPos + i));
			}
			// The per-space adjustments.
			for (int i = 0; i < currentNumRanges; i++) {
				result.add(newFirstPerSpacePos + i + 1,
						oldresult.get(firstPerSpacePos + i));
			}
			// The per-land adjustments.
			for (int i = 0; i < currentNumRanges; i++) {
				result.add(newFirstPerLandPos + i + 1,
						oldresult.get(firstPerLandPos + i));
			}
			// The dispersion parameter.
			result.add(newDispersionPos, oldresult.get(dispersionPos));
			// The transformation dependencies of the new first per-land
			// adjustment.
			result.add(newFirstPerLandPos, oldresult.get(firstPerLandPos));
			result.add(newFirstPerSpacePos,
					stepPoints[currentMin] * oldresult.get(firstPerLandPos));
			result.add(newFirstPerSpacePos + 1,
					-stepPoints[currentMin] * oldresult.get(firstPerLandPos));
			result
					.add(
							newFirstStepPointPos + 1,
							(perSpaceAdjustments[currentMin - 1] - perSpaceAdjustments[currentMin])
									* oldresult.get(firstPerLandPos));
		}

		// Push maximum range from highestValidRange back up to numRanges - 1.
		for (int currentMax = highestValidRange; currentMax < numRanges - 1; currentMax++) {
			final int currentNumRanges = currentMax + 1;
			final int newNumRanges = currentNumRanges + 1;

			// Old positions of the parameter types.
			final int firstStepPointPos = 0;
			final int firstPerSpacePos = currentNumRanges + 1;
			final int firstPerLandPos = 2 * currentNumRanges + 1;
			final int dispersionPos = 3 * currentNumRanges + 1;

			// New positions of the parameter types.
			final int newFirstStepPointPos = 0;
			final int newFirstPerSpacePos = newNumRanges + 1;
			final int newFirstPerLandPos = 2 * newNumRanges + 1;
			final int newDispersionPos = 3 * newNumRanges + 1;

			final Vector oldresult = result;
			result = new DenseVector(3 * newNumRanges + 2);

			// The step points.
			for (int i = 0; i < currentNumRanges; i++) {
				result.add(newFirstStepPointPos + i,
						oldresult.get(firstStepPointPos + i));
			}
			result.add(newFirstStepPointPos + newNumRanges,
					oldresult.get(firstStepPointPos + currentNumRanges));
			// The per-space adjustments.
			for (int i = 0; i < currentNumRanges; i++) {
				result
						.add(newFirstPerSpacePos + i, oldresult.get(firstPerSpacePos + i));
			}
			// The per-land adjustments.
			for (int i = 0; i < currentNumRanges; i++) {
				result.add(newFirstPerLandPos + i, oldresult.get(firstPerLandPos + i));
			}
			// The dispersion parameter.
			result.add(newDispersionPos, oldresult.get(dispersionPos));
		}

		return result;
	}

	/**
	 * This method does the integration over the range of the intensities,
	 * returning the total weight over the range of intensities (space-per-land)
	 * that are allowed.
	 * 
	 * @param perSpace
	 *          utility per unit space (typically rent-cost per square foot)
	 * @param perLand
	 *          utility per unit land (typically any fees or costs associated with
	 *          preparing land, independent of building size)
	 * @param landSize
	 *          size of land (not currently used, as it is a "per unit land"
	 *          value)
	 * @return total weight over the range of allowed intensities.
	 */
	protected static double integrateOverIntensityRange(double perSpace,
			double perLand, double landSize, double minIntensity,
			double maxIntensity, double dispersion) {
		// Equation 91
		// double atQmax = zoningReg.get_MaxIntensityPermitted();
		// double atQmin = zoningReg.get_MinIntensityPermitted();
		// double idp =
		// theNewSpaceTypeToBeBuilt.get_IntensityDispersionParameter();
		double atQmax = maxIntensity;
		double atQmin = minIntensity;
		final double idp = dispersion;
		if (Math.abs(perSpace) > epsilon) {
			atQmax = Math.exp(idp * (perSpace * maxIntensity));
			atQmax = atQmax / idp / perSpace;
			atQmin = Math.exp(idp * (perSpace * minIntensity));
			atQmin = atQmin / idp / perSpace;
		} // else {
			// simiplifies to maxIntensity and minIntensity
		double result = atQmax - atQmin;
		result = result * Math.exp(idp * perLand);
		return result;
	}

	protected static double sampleIntensityWithinRanges(
			double dispersionParameter, double landArea, double utilityPerUnitSpace,
			double utilityPerUnitLand, double[] intensityPoints,
			double[] perSpaceAdjustments, double[] perLandAdjustments) {
		if (intensityPoints.length < 2) {
			final String msg = "Need to have at least 2 allowed intensities";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		if (perSpaceAdjustments.length != intensityPoints.length - 1) {
			final String msg = "Intensity space adjustments need to be the same as the specified intensity ranges (i.e. needs to be "
					+ (intensityPoints.length - 1) + ")";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		if (perLandAdjustments.length != intensityPoints.length - 1) {
			final String msg = "Per Land Adjustments need to be one for each specified intensity point below the maximum (i.e. needs to be "
					+ (intensityPoints.length - 1) + ")";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		final int numRanges = intensityPoints.length - 1;
		if (intensityPoints[0] > intensityPoints[numRanges]) {
			final String msg = "Minimum intensity must not be greater than maximum intensity: "
					+ intensityPoints[0] + " > " + intensityPoints[1];
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}

		// Check for any degenerate ranges.
		int i = 1;
		while (intensityPoints[0] > intensityPoints[i]) {
			i++;
		}
		final int lowestValidRange = i - 1;

		i = numRanges - 1;
		while (intensityPoints[numRanges] < intensityPoints[i]) {
			i--;
		}
		final int highestValidRange = i;

		// If there are no degenerate ranges, we can proceed to the
		// calculations.
		if (lowestValidRange == 0 && highestValidRange == numRanges - 1) {
			return sampleIntensityProperRanges(dispersionParameter, landArea,
					utilityPerUnitSpace, utilityPerUnitLand, intensityPoints,
					perSpaceAdjustments, perLandAdjustments);
		}

		// Reduce number of ranges if necessary to remove any degenerates.
		final int numProperRanges = highestValidRange - lowestValidRange + 1;
		final double[] properIntensityPoints = new double[numProperRanges + 1];
		final double[] properPerSpaceAdjustments = new double[numProperRanges];
		final double[] properPerLandAdjustments = new double[numProperRanges];

		removeInvalidRanges(numRanges, numProperRanges, lowestValidRange,
				highestValidRange, intensityPoints, perSpaceAdjustments,
				perLandAdjustments, properIntensityPoints, properPerSpaceAdjustments,
				properPerLandAdjustments);

		return sampleIntensityProperRanges(dispersionParameter, landArea,
				utilityPerUnitSpace, utilityPerUnitLand, properIntensityPoints,
				properPerSpaceAdjustments, properPerLandAdjustments);
	}

	private static double sampleIntensityProperRanges(double dispersionParameter,
			double landArea, double utilityPerUnitSpace, double utilityPerUnitLand,
			double[] intensityPoints, double[] perSpaceAdjustments,
			double[] perLandAdjustments) {
		final double uniformRandomNumber = Math.random();
		final double[] Dplus = new double[intensityPoints.length]; // indefinite
		// integral
		// just
		// below
		// boundary
		final double[] Dminus = new double[intensityPoints.length]; // indefinite
		// integral
		// just
		// above
		// boundary
		final double[] D = new double[intensityPoints.length];
		double netUtility = utilityPerUnitLand;
		for (int point = 0; point < intensityPoints.length; point++) {
			double perSpace = utilityPerUnitSpace;
			{
				perSpace += perSpaceAdjustments[Math.max(0, point - 1)];
				if (perSpace == 0) {
					// have to consider the possibility of costs cancelling out
					// and being exactly zero
					Dminus[point] = intensityPoints[point] * landArea
							* Math.exp(dispersionParameter * netUtility);
				}
				else {
					double lowPoint = 0;
					if (point != 0) {
						lowPoint = intensityPoints[point - 1];
					}
					netUtility += (intensityPoints[point] - lowPoint)
							* (utilityPerUnitSpace + perSpaceAdjustments[Math.max(0,
									point - 1)]);
					Dminus[point] = landArea * Math.exp(dispersionParameter * netUtility)
							/ dispersionParameter / perSpace;
				}
				if (point == 0) {
					D[point] = 0;
				}
				else {
					// definite integral for full region up to
					// intensityPoints[point]
					D[point] = Dminus[point] - Dplus[point - 1] + D[point - 1];
				}
			}
			if (point < perSpaceAdjustments.length) {
				// no Dplus at the top boundary.
				perSpace = utilityPerUnitSpace + perSpaceAdjustments[point];

				netUtility += perLandAdjustments[point];
				if (perSpace == 0) {
					Dplus[point] = intensityPoints[point] * landArea
							* Math.exp(dispersionParameter * netUtility);
				}
				else {
					Dplus[point] = landArea * Math.exp(dispersionParameter * netUtility)
							/ dispersionParameter / perSpace;
				}
			}
		}
		// now work down through boundary points
		double quantity = 0;
		for (int highPoint = intensityPoints.length - 1; highPoint > 0; highPoint--) {
			if (highPoint < intensityPoints.length - 1) {
				// subtract this out in general, but not in the first loop
				// because we never added it in for Dplus at the top boundary
				netUtility -= perLandAdjustments[highPoint];
			}
			if (D[highPoint - 1] < uniformRandomNumber
					* D[intensityPoints.length - 1]) {
				// it's in this range
				// find the slope of our cost curve
				final double perSpace = utilityPerUnitSpace
						+ perSpaceAdjustments[highPoint - 1];
				// back out the intercept of our cost curve
				final double perLand = netUtility - intensityPoints[highPoint]
						* perSpace;

				if (perSpace == 0) {
					final double samplePoint = uniformRandomNumber
							* D[intensityPoints.length - 1] - D[highPoint - 1];
					final double intensity = samplePoint
							/ (D[highPoint] - D[highPoint - 1])
							* (intensityPoints[highPoint] - intensityPoints[highPoint - 1])
							+ intensityPoints[highPoint - 1];
					quantity = intensity * landArea;
				}
				else {
					final double samplePoint = uniformRandomNumber
							* D[intensityPoints.length - 1] - D[highPoint - 1]
							+ Dplus[highPoint - 1];
					final double numerator = Math.log(dispersionParameter * perSpace
							* samplePoint / landArea);
					quantity = (numerator / dispersionParameter - perLand) * landArea
							/ perSpace;
				}
				break;
			}
			// back down to next interval
			double lowPoint = 0;
			if (highPoint > 0) {
				lowPoint = intensityPoints[highPoint - 1];
			}
			netUtility -= (intensityPoints[highPoint] - lowPoint)
					* (utilityPerUnitSpace + perSpaceAdjustments[Math.max(0,
							highPoint - 1)]);
		}
		if (Double.isInfinite(quantity) || Double.isNaN(quantity)) {
			// if (logger.isDebugEnabled())
			logger.warn("truncating sampled intensity at maximum");
			return intensityPoints[intensityPoints.length - 1];
		}
		if (quantity > intensityPoints[intensityPoints.length - 1] * landArea) {
			// if (logger.isDebugEnabled())
			logger.warn("truncating sampled intensity at maximum");
			return intensityPoints[intensityPoints.length - 1];
		}
		if (quantity < intensityPoints[0] * landArea) {
			// if (logger.isDebugEnabled())
			logger.warn("truncating sampled intensity at minimum");
			return intensityPoints[0];
		}
		return quantity / landArea;
	}

	/**
	 * Returns the expected maximum utility over the allowed intensity range, with
	 * one step point in the (otherwise linear) utility function.
	 * 
	 * @param perSpace
	 *          The utility per unit floor area ratio.
	 * @param perLand
	 *          The utility per unit land area.
	 * @param landSize
	 *          The total size of the land (currently not used - the method deals
	 *          with utility per land area).
	 * @param stepPoint
	 *          The FAR at which the step discontinuity occurs.
	 * @param stepPointAdjustment
	 *          The size of the step discontinuity.
	 * @param belowStepPointAdjustment
	 *          The extra per-space utility for FARs below the step point.
	 * @param aboveStepPointAdjustment
	 *          The extra per-space utility for FARs above the step point.
	 * @param minIntensity
	 *          The minimum allowed FAR.
	 * @param maxIntensity
	 *          The maximum allowed FAR.
	 * @param intensityDispersion
	 *          The dispersion parameter over the intensity nest.
	 * @return The composite utility per unit land area.
	 * @deprecated
	 */
	@Deprecated
	protected static double getCompositeUtilityTwoRangesWithAdjustments(
			double perSpace, double perLand, double landSize, double stepPoint,
			double stepPointAdjustment, double belowStepPointAdjustment,
			double aboveStepPointAdjustment, double minIntensity,
			double maxIntensity, double intensityDispersion) {
		if (maxIntensity <= minIntensity) {
			return Double.NEGATIVE_INFINITY;
		}
		double result = 0;
		if (minIntensity < stepPoint) {
			result += integrateOverIntensityRange(
					perSpace + belowStepPointAdjustment, perLand, landSize, minIntensity,
					Math.min(stepPoint, maxIntensity), intensityDispersion);
		}
		if (maxIntensity > stepPoint) {
			result += integrateOverIntensityRange(
					perSpace + aboveStepPointAdjustment, perLand + stepPointAdjustment
							+ (belowStepPointAdjustment - aboveStepPointAdjustment)
							* stepPoint, landSize, Math.max(minIntensity, stepPoint),
					maxIntensity, intensityDispersion);
		}

		result = 1 / intensityDispersion * Math.log(result);

		return result;
	}

	// For this method, intensityPoints MUST be in strictly non-descending
	// order.
	private static double getCompositeUtilityProperRanges(
			double dispersionParameter, double landArea, double utilityPerUnitSpace,
			double utilityPerUnitLand, double[] intensityPoints,
			double[] perSpaceAdjustments, double[] perLandAdjustments) {

		final int numRanges = intensityPoints.length - 1;
		double currentLandUtility = utilityPerUnitLand;
		double result = 0;

		for (int i = 0; i < numRanges; i++) {
			if (intensityPoints[i + 1] < intensityPoints[i]) {
				final String msg = "Step points must be non-decreasing; point "
						+ (i + 1) + ": " + intensityPoints[i + 1] + " is less than point "
						+ i + ": " + intensityPoints[i];
				logger.fatal(msg);
				throw new RuntimeException(msg);
			}

			final double perSpace = utilityPerUnitSpace + perSpaceAdjustments[i];
			currentLandUtility += perLandAdjustments[i];
			if (i > 0) {
				currentLandUtility += (perSpaceAdjustments[i - 1] - perSpaceAdjustments[i])
						* intensityPoints[i];
			}

			final double minFAR = intensityPoints[i];
			final double maxFAR = intensityPoints[i + 1];

			result += integrateOverIntensityRange(perSpace, currentLandUtility,
					landArea, minFAR, maxFAR, dispersionParameter);
		}

		result = 1 / dispersionParameter * Math.log(result);

		return result;
	}

	/**
	 * Returns the expected maximum utility per unit land area over the allowed
	 * intensity range. The utility function is piecewise linear, and is defined
	 * by the parameters <code>utilityPerUnitSpace</code>,
	 * <code>utilityPerUnitLand</code>, <code>intensityPoints</code>,
	 * <code>perSpaceAdjustments</code>, and <code>perLandAdjustments</code>. If
	 * there are <i>w</i> linear subranges, <code>intensityPoints</code> must have
	 * length <i>w</i>+1, while <code>perSpaceAdjustments</code> and
	 * <code>perLandAdjustments</code> must have length <i>w</i>.
	 * 
	 * @param dispersionParameter
	 *          The dispersion parameter for the choice over the possible
	 *          intensities.
	 * @param landArea
	 *          The area of the parcel.
	 * @param utilityPerUnitSpace
	 *          The base utility of each unit area of floorspace.
	 * @param utilityPerUnitLand
	 *          The base utility of each unit area of land - i.e. the component of
	 *          utility that does not depend on the amount of floorspace.
	 * @param intensityPoints
	 *          The boundary points for the subranges as FARs. The first and last
	 *          elements are the minimum and maximum allowed FAR while the other
	 *          points are the boundaries between the different subranges. The
	 *          boundary points must be properly ordered - i.e. each boundary
	 *          point must be no less than the previous boundary point. The
	 *          minimum and maximum, however, may be out of order with respect to
	 *          the boundary points, meaning that some of the ranges are
	 *          disallowed entirely.
	 * @param perSpaceAdjustments
	 *          The adjustment to the utility per unit area of floorspace for each
	 *          subrange.
	 * @param perLandAdjustments
	 *          The step sizes at each boundary point - i.e. the difference
	 *          between the utility just above the boundary point and the utility
	 *          just below it. The first element applies at the minimum FAR, so it
	 *          is effectively a global adjustment to the utility.
	 * @return The composite utility.
	 */
	protected static double getCompositeUtility(double dispersionParameter,
			double landArea, double utilityPerUnitSpace, double utilityPerUnitLand,
			double[] intensityPoints, double[] perSpaceAdjustments,
			double[] perLandAdjustments) {
		if (intensityPoints.length < 2) {
			final String msg = "Need to have at least 2 allowed intensities";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		if (perSpaceAdjustments.length != intensityPoints.length - 1) {
			final String msg = "Intensity space adjustments need to be the same as the specified intensity ranges (i.e. needs to be "
					+ (intensityPoints.length - 1) + ")";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		if (perLandAdjustments.length != intensityPoints.length - 1) {
			final String msg = "Per Land Adjustments need to be one for each specified intensity point below the maximum (i.e. needs to be "
					+ (intensityPoints.length - 1) + ")";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		final int numRanges = intensityPoints.length - 1;
		if (intensityPoints[0] > intensityPoints[numRanges]) {
			final String msg = "Minimum intensity must not be greater than maximum intensity: "
					+ intensityPoints[0] + " > " + intensityPoints[1];
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		// Check for any degenerate ranges.
		int i = 1;
		while (intensityPoints[0] > intensityPoints[i]) {
			i++;
		}
		final int lowestValidRange = i - 1;

		i = numRanges - 1;
		while (intensityPoints[numRanges] < intensityPoints[i]) {
			i--;
		}
		final int highestValidRange = i;

		// If there are no degenerate ranges, we can proceed to the
		// calculations.
		if (lowestValidRange == 0 && highestValidRange == numRanges - 1) {
			return getCompositeUtilityProperRanges(dispersionParameter, landArea,
					utilityPerUnitSpace, utilityPerUnitLand, intensityPoints,
					perSpaceAdjustments, perLandAdjustments);
		}

		// Reduce number of ranges if necessary to remove any degenerates.
		final int numProperRanges = highestValidRange - lowestValidRange + 1;
		final double[] properIntensityPoints = new double[numProperRanges + 1];
		final double[] properPerSpaceAdjustments = new double[numProperRanges];
		final double[] properPerLandAdjustments = new double[numProperRanges];

		removeInvalidRanges(numRanges, numProperRanges, lowestValidRange,
				highestValidRange, intensityPoints, perSpaceAdjustments,
				perLandAdjustments, properIntensityPoints, properPerSpaceAdjustments,
				properPerLandAdjustments);

		return getCompositeUtilityProperRanges(dispersionParameter, landArea,
				utilityPerUnitSpace, utilityPerUnitLand, properIntensityPoints,
				properPerSpaceAdjustments, properPerLandAdjustments);
	}

	private static double getUtilityDerivativeWRTMaxIntensity(double perSpace,
			double perLand, double minIntensity, double maxIntensity,
			double dispersion) {
		final double landweight = Math.exp(dispersion * perLand);
		final double maxweight = Math.exp(dispersion * perSpace * maxIntensity);
		return landweight * maxweight;
	}

	private static double getUtilityDerivativeWRTMinIntensity(double perSpace,
			double perLand, double minIntensity, double maxIntensity,
			double dispersion) {
		final double landweight = Math.exp(dispersion * perLand);
		final double minweight = Math.exp(dispersion * perSpace * minIntensity);
		return -landweight * minweight;
	}

	private static double getUtilityDerivativeWRTPerSpace(double perSpace,
			double perLand, double minIntensity, double maxIntensity,
			double dispersion) {
		if (Math.abs(perSpace) <= epsilon) {
			final double landweight = Math.exp(dispersion * perLand);
			return dispersion * landweight
					* (square(maxIntensity) - square(minIntensity)) / 2;
		}
		else {
			final double landweight = Math.exp(dispersion * perLand);
			final double maxutil = dispersion * maxIntensity * perSpace;
			final double minutil = dispersion * minIntensity * perSpace;
			// This method is particularly erratic in the face of nearly zero
			// space utility.
			// To keep precision, the formula is rearranged to make use of the
			// expm1 method.
			final double maxweight = Math.expm1(maxutil);
			final double minweight = Math.expm1(minutil);
			final double weightdiff = minweight - maxweight;
			final double utildiff = maxutil * (maxweight + 1) - minutil
					* (minweight + 1);
			return landweight / (dispersion * square(perSpace))
					* (weightdiff + utildiff);
		}
	}

	private static double getUtilityDerivativeWRTPerLand(double perSpace,
			double perLand, double minIntensity, double maxIntensity,
			double dispersion) {
		if (Math.abs(perSpace) <= epsilon) {
			final double landweight = Math.exp(dispersion * perLand);
			return dispersion * landweight * (maxIntensity - minIntensity);
		}
		else {
			final double landweight = Math.exp(dispersion * perLand);
			final double maxweight = Math.exp(dispersion * maxIntensity * perSpace);
			final double minweight = Math.exp(dispersion * minIntensity * perSpace);
			return landweight / perSpace * (maxweight - minweight);
		}
	}

	private static double getUtilityDerivativeWRTDispersion(double perSpace,
			double perLand, double minIntensity, double maxIntensity,
			double dispersion) {
		if (Math.abs(perSpace) <= epsilon) {
			final double landweight = Math.exp(dispersion * perLand);
			return perLand * landweight * (maxIntensity - minIntensity);
		}
		else {
			final double landutil = dispersion * perLand;
			final double maxutil = dispersion * maxIntensity * perSpace;
			final double minutil = dispersion * minIntensity * perSpace;
			final double landweight = Math.exp(landutil);
			final double maxweight = Math.exp(maxutil);
			final double minweight = Math.exp(minutil);
			return landweight
					/ (square(dispersion) * perSpace)
					* ((maxutil + landutil - 1) * maxweight - (minutil + landutil - 1)
							* minweight);
		}
	}

	private static Matrix getUtilityDerivativesWRTUtilityIntegrals(int numRanges,
			double[] integrals, double dispersion) {
		double integralsum = 0;
		for (int i = 0; i < numRanges; i++) {
			integralsum += integrals[i];
		}

		final Matrix result = new DenseMatrix(1, numRanges);
		for (int i = 0; i < numRanges; i++) {
			result.set(0, i, 1 / (dispersion * integralsum));
		}

		return result;
	}

	// Returns a numRanges by 5 matrix as an array of 1 by 5 matrices
	// (there are 5 integral parameters: perSpace, perLand, minIntensity,
	// maxIntensity, dispersion).
	private static Matrix[] getUtilityDerivativesWRTIntegralParameters(
			int numRanges, double[] perSpaces, double[] perLands,
			double[] minIntensities, double[] maxIntensities, double dispersion) {
		final Matrix[] result = new Matrix[numRanges];
		for (int i = 0; i < numRanges; i++) {
			final double[] row = new double[NUM_INT_PARAMS];
			row[IND_US] = getUtilityDerivativeWRTPerSpace(perSpaces[i], perLands[i],
					minIntensities[i], maxIntensities[i], dispersion);
			row[IND_UA] = getUtilityDerivativeWRTPerLand(perSpaces[i], perLands[i],
					minIntensities[i], maxIntensities[i], dispersion);
			row[IND_SMIN] = getUtilityDerivativeWRTMinIntensity(perSpaces[i],
					perLands[i], minIntensities[i], maxIntensities[i], dispersion);
			row[IND_SMAX] = getUtilityDerivativeWRTMaxIntensity(perSpaces[i],
					perLands[i], minIntensities[i], maxIntensities[i], dispersion);
			row[IND_DISP] = getUtilityDerivativeWRTDispersion(perSpaces[i],
					perLands[i], minIntensities[i], maxIntensities[i], dispersion);

			final Matrix m = new DenseMatrix(1, 5);
			result[i] = new DenseMatrix(new DenseVector(row)).transpose(m);
		}
		return result;
	}

	// Returns an array of 2 5 by 5 matrices.
	/**
	 * @deprecated
	 */
	@Deprecated
	private static Matrix[] getIntegralParameterDerivativesWRTParameters(
			double minIntensity, double maxIntensity, double stepPoint,
			double stepPointAdjustment, double belowStepPointAdjustment,
			double aboveStepPointAdjustment, double dispersion) {
		final double b = belowStepPointAdjustment;
		final double a = aboveStepPointAdjustment;
		final double p = stepPoint;

		Matrix[] result;
		if (maxIntensity <= stepPoint) {
			final double[][] matrix = new double[][] { { 0, 1, 0, 0, 0 },
					{ 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 },
					{ 0, 0, 0, 0, 1 } };
			result = new Matrix[1];
			result[0] = new DenseMatrix(matrix);
		}
		else if (minIntensity >= stepPoint) {
			final double[][] matrix = new double[][] { { 0, 0, 1, 0, 0 },
					{ b - a, p, -p, 1, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 },
					{ 0, 0, 0, 0, 1 } };
			result = new Matrix[1];
			result[0] = new DenseMatrix(matrix);
		}
		else {
			final double[][] matrix1 = new double[][] { { 0, 1, 0, 0, 0 },
					{ 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 1, 0, 0, 0, 0 },
					{ 0, 0, 0, 0, 1 } };
			final double[][] matrix2 = new double[][] { { 0, 0, 1, 0, 0 },
					{ b - a, p, -p, 1, 0 }, { 1, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 },
					{ 0, 0, 0, 0, 1 } };
			result = new Matrix[2];
			result[0] = new DenseMatrix(matrix1);
			result[1] = new DenseMatrix(matrix2);
		}

		return result;
	}

	// Returns an array of numRanges matrices. Each matrix is 5 by (numRanges *
	// 3 + 2).
	private static Matrix[] getIntegralParameterDerivativesWRTParameters(
			int numRanges, double[] perSpaceAdjustments, double[] perLandAdjustments,
			double[] stepPoints, double dispersion) {
		// Positions of the different parameter types.
		final int firstStepPointPos = 0;
		final int firstPerSpacePos = numRanges + 1;
		final int firstPerLandPos = 2 * numRanges + 1;
		final int dispersionPos = 3 * numRanges + 1;

		final Matrix[] result = new Matrix[numRanges];
		for (int i = 0; i < numRanges; i++) {
			result[i] = new DenseMatrix(NUM_INT_PARAMS, 3 * numRanges + 2);
			// Utility per space depends on corresponding per-space adjustment.
			result[i].add(IND_US, firstPerSpacePos + i, 1);
			// Minimum intensity depends on corresponding step point.
			result[i].add(IND_SMIN, firstStepPointPos + i, 1);
			// Maximum intensity depends on next step point.
			result[i].add(IND_SMAX, firstStepPointPos + i + 1, 1);
			// Dispersion parameter depends only on dispersion parameter.
			result[i].add(IND_DISP, dispersionPos, 1);
			// Utility per land depends on per-land adjustments...
			for (int j = 0; j <= i; j++) {
				result[i].add(IND_UA, firstPerLandPos + j, 1);
			}
			// ... and on all the step points up to the current one...
			for (int j = 0; j < i; j++) {
				result[i].add(IND_UA, firstStepPointPos + j + 1, perSpaceAdjustments[j]
						- perSpaceAdjustments[j + 1]);
			}
			// ... and on all the per-space adjustments up to the next one.
			for (int j = 0; j < i; j++) {
				result[i].add(IND_UA, firstPerSpacePos + j, stepPoints[j + 1]);
			}
			for (int j = 0; j < i; j++) {
				result[i].add(IND_UA, firstPerSpacePos + j + 1, -stepPoints[j + 1]);
			}
		}

		return result;
	}

	// Returns the derivatives in the order: wrt step point, wrt below step
	// point adjustment, wrt above step point adjustment,
	// wrt step point adjustment, wrt dispersion parameter.
	/**
	 * @deprecated
	 */
	@Deprecated
	protected static Vector getTwoRangeUtilityDerivativesWRTParameters(
			double perSpace, double perLand, double landSize, double stepPoint,
			double stepPointAdjustment, double belowStepPointAdjustment,
			double aboveStepPointAdjustment, double minIntensity,
			double maxIntensity, double intensityDispersion) {

		final int numCoeffs = 5;

		int numRanges;
		double[] perSpaces;
		double[] perLands;
		double[] minIntensities;
		double[] maxIntensities;
		double[] integrals;
		if (maxIntensity <= stepPoint) {
			numRanges = 1;
			perSpaces = new double[] { perSpace + belowStepPointAdjustment };
			perLands = new double[] { perLand };
			minIntensities = new double[] { minIntensity };
			maxIntensities = new double[] { maxIntensity };
			integrals = new double[1];
			integrals[0] = integrateOverIntensityRange(perSpaces[0], perLands[0],
					landSize, minIntensities[0], maxIntensities[0], intensityDispersion);
		}
		else if (minIntensity >= stepPoint) {
			numRanges = 1;
			perSpaces = new double[] { perSpace + aboveStepPointAdjustment };
			perLands = new double[] { perLand + stepPointAdjustment
					+ (belowStepPointAdjustment - aboveStepPointAdjustment) * stepPoint };
			minIntensities = new double[] { minIntensity };
			maxIntensities = new double[] { maxIntensity };
			integrals = new double[1];
			integrals[0] = integrateOverIntensityRange(perSpaces[0], perLands[0],
					landSize, minIntensities[0], maxIntensities[0], intensityDispersion);
		}
		else {
			numRanges = 2;
			perSpaces = new double[] { perSpace + belowStepPointAdjustment,
					perSpace + aboveStepPointAdjustment };
			perLands = new double[] {
					perLand,
					perLand + stepPointAdjustment
							+ (belowStepPointAdjustment - aboveStepPointAdjustment)
							* stepPoint };
			minIntensities = new double[] { minIntensity, stepPoint };
			maxIntensities = new double[] { stepPoint, maxIntensity };
			integrals = new double[2];
			integrals[0] = integrateOverIntensityRange(perSpaces[0], perLands[0],
					landSize, minIntensities[0], maxIntensities[0], intensityDispersion);
			integrals[1] = integrateOverIntensityRange(perSpaces[1], perLands[1],
					landSize, minIntensities[1], maxIntensities[1], intensityDispersion);
		}

		final Matrix[] dadt = getIntegralParameterDerivativesWRTParameters(
				minIntensity, maxIntensity, stepPoint, stepPointAdjustment,
				belowStepPointAdjustment, aboveStepPointAdjustment, intensityDispersion);
		final Matrix[] dIda = getUtilityDerivativesWRTIntegralParameters(numRanges,
				perSpaces, perLands, minIntensities, maxIntensities,
				intensityDispersion);
		final Matrix dIdt = multiplyAndAggregate(numRanges, NUM_INT_PARAMS,
				numCoeffs, dIda, dadt);
		final Matrix dUdI = getUtilityDerivativesWRTUtilityIntegrals(numRanges,
				integrals, intensityDispersion);

		Matrix result = new DenseMatrix(1, numCoeffs);
		result = dUdI.mult(dIdt, result);

		// Convert into a vector.
		final Vector vector = new DenseVector(numCoeffs);
		for (int i = 0; i < numCoeffs; i++) {
			vector.set(i, result.get(0, i));
		}

		return vector;
	}

	private static Vector getUtilityDerivativesWRTParametersProperRanges(
			double dispersionParameter, double landArea, double utilityPerUnitSpace,
			double utilityPerUnitLand, double[] intensityPoints,
			double[] perSpaceAdjustments, double[] perLandAdjustments) {

		final int numRanges = intensityPoints.length - 1;
		final int numCoeffs = 3 * numRanges + 2;

		double currentLandUtility = utilityPerUnitLand;
		final double[] perSpaces = new double[numRanges];
		final double[] perLands = new double[numRanges];
		final double[] minFARs = new double[numRanges];
		final double[] maxFARs = new double[numRanges];
		final double[] integrals = new double[numRanges];

		// Find the modified parameters for each range.
		for (int i = 0; i < numRanges; i++) {
			if (intensityPoints[i + 1] < intensityPoints[i]) {
				final String msg = "Step points must be non-decreasing; point "
						+ (i + 1) + ": " + intensityPoints[i + 1] + " is less than point "
						+ i + ": " + intensityPoints[i];
				logger.fatal(msg);
				throw new RuntimeException(msg);
			}

			perSpaces[i] = utilityPerUnitSpace + perSpaceAdjustments[i];
			currentLandUtility += perLandAdjustments[i];
			if (i > 0) {
				currentLandUtility += (perSpaceAdjustments[i - 1] - perSpaceAdjustments[i])
						* intensityPoints[i];
			}
			perLands[i] = currentLandUtility;

			minFARs[i] = intensityPoints[i];
			maxFARs[i] = intensityPoints[i + 1];

			integrals[i] = integrateOverIntensityRange(perSpaces[i], perLands[i],
					landArea, minFARs[i], maxFARs[i], dispersionParameter);
		}

		final Matrix[] dadt = getIntegralParameterDerivativesWRTParameters(
				numRanges, perSpaceAdjustments, perLandAdjustments, intensityPoints,
				dispersionParameter);
		final Matrix[] dIda = getUtilityDerivativesWRTIntegralParameters(numRanges,
				perSpaces, perLands, minFARs, maxFARs, dispersionParameter);
		final Matrix dIdt = multiplyAndAggregate(numRanges, NUM_INT_PARAMS,
				numCoeffs, dIda, dadt);
		final Matrix dUdI = getUtilityDerivativesWRTUtilityIntegrals(numRanges,
				integrals, dispersionParameter);

		Matrix result = new DenseMatrix(1, numCoeffs);
		result = dUdI.mult(dIdt, result);

		// Convert into a vector.
		final Vector vector = new DenseVector(numCoeffs);
		for (int i = 0; i < numCoeffs; i++) {
			vector.set(i, result.get(0, i));
		}

		// Add direct dependency of the composite utility on the dispersion
		// parameter.
		double integralsum = 0;
		for (int i = 0; i < numRanges; i++) {
			integralsum += integrals[i];
		}
		final double dUdlambda = -1 / square(dispersionParameter)
				* Math.log(integralsum);
		vector.add(numCoeffs - 1, dUdlambda);

		return vector;
	}

	/**
	 * Returns the partial derivatives of the composite utility with respect to
	 * each parameter. The utility function is defined as in
	 * <code>getCompositeUtility()</code>.
	 * 
	 * @param dispersionParameter
	 *          The dispersion parameter for the choice over the possible
	 *          intensities.
	 * @param landArea
	 *          The area of the parcel.
	 * @param utilityPerUnitSpace
	 *          The base utility of each unit area of floorspace.
	 * @param utilityPerUnitLand
	 *          The base utility of each unit area of land - i.e. the component of
	 *          utility that does not depend on the amount of floorspace.
	 * @param intensityPoints
	 *          The boundary points for the subranges as FARs. The first and last
	 *          elements are the minimum and maximum allowed FAR while the other
	 *          points are the boundaries between the different subranges. The
	 *          boundary points must be properly ordered - i.e. each boundary
	 *          point must be no less than the previous boundary point. The
	 *          minimum and maximum, however, may be out of order with respect to
	 *          the boundary points, meaning that some of the ranges are
	 *          disallowed entirely.
	 * @param perSpaceAdjustments
	 *          The adjustment to the utility per unit area of floorspace for each
	 *          subrange.
	 * @param perLandAdjustments
	 *          The step sizes at each boundary point - i.e. the difference
	 *          between the utility just above the boundary point and the utility
	 *          just below it. The first element applies at the minimum FAR, so it
	 *          is effectively a global adjustment to the utility.
	 * @return The derivatives with respect to each parameter, with the parameters
	 *         in the following order: the boundary points, the per-space
	 *         adjustments, the per-land adjustments, and the dispersion
	 *         parameter.
	 * @see getCompositeUtility.
	 */
	protected static Vector getUtilityDerivativesWRTParameters(
			double dispersionParameter, double landArea, double utilityPerUnitSpace,
			double utilityPerUnitLand, double[] intensityPoints,
			double[] perSpaceAdjustments, double[] perLandAdjustments) {
		if (intensityPoints.length < 2) {
			final String msg = "Need to have at least 2 allowed intensities";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		if (perSpaceAdjustments.length != intensityPoints.length - 1) {
			final String msg = "Intensity space adjustments need to be the same as the specified intensity ranges (i.e. needs to be "
					+ (intensityPoints.length - 1) + ")";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		if (perLandAdjustments.length != intensityPoints.length - 1) {
			final String msg = "Per Land Adjustments need to be one for each specified intensity point below the maximum (i.e. needs to be "
					+ (intensityPoints.length - 1) + ")";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		final int numRanges = intensityPoints.length - 1;
		if (intensityPoints[0] > intensityPoints[numRanges]) {
			final String msg = "Minimum intensity must not be greater than maximum intensity: "
					+ intensityPoints[0] + " > " + intensityPoints[1];
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		// Check for any degenerate ranges.
		int i = 1;
		while (intensityPoints[0] > intensityPoints[i]) {
			i++;
		}
		final int lowestValidRange = i - 1;

		i = numRanges - 1;
		while (intensityPoints[numRanges] < intensityPoints[i]) {
			i--;
		}
		final int highestValidRange = i;

		// If there are no degenerate ranges, we can proceed to the
		// calculations.
		if (lowestValidRange == 0 && highestValidRange == numRanges - 1) {
			return getUtilityDerivativesWRTParametersProperRanges(
					dispersionParameter, landArea, utilityPerUnitSpace,
					utilityPerUnitLand, intensityPoints, perSpaceAdjustments,
					perLandAdjustments);
		}

		// Reduce number of ranges if necessary to remove any degenerates.
		final int numProperRanges = highestValidRange - lowestValidRange + 1;
		final double[] properIntensityPoints = new double[numProperRanges + 1];
		final double[] properPerSpaceAdjustments = new double[numProperRanges];
		final double[] properPerLandAdjustments = new double[numProperRanges];

		removeInvalidRanges(numRanges, numProperRanges, lowestValidRange,
				highestValidRange, intensityPoints, perSpaceAdjustments,
				perLandAdjustments, properIntensityPoints, properPerSpaceAdjustments,
				properPerLandAdjustments);

		final Vector result = getUtilityDerivativesWRTParametersProperRanges(
				dispersionParameter, landArea, utilityPerUnitSpace, utilityPerUnitLand,
				properIntensityPoints, properPerSpaceAdjustments,
				properPerLandAdjustments);

		// The above vector will be too small. Add zeroes to positions
		// corresponding to invalid ranges (since these parameters
		// will not affect the utility).
		final Vector restored = transformDerivativesForValidRanges(result,
				numRanges, lowestValidRange, highestValidRange, perSpaceAdjustments,
				perLandAdjustments, intensityPoints, dispersionParameter);
		return restored;
	}

	// Return the expected FAR in one intensity range, not yet normalized by the
	// total area under the allowed range.
	protected static double getExpectedFARSum(double perSpace, double perLand,
			double landSize, double minIntensity, double maxIntensity,
			double dispersion) {
		if (Math.abs(perSpace) <= epsilon) {
			final double landweight = Math.exp(dispersion * perLand);
			return landweight / 2 * (square(maxIntensity) - square(minIntensity));
		}
		else {
			final double maxutil = dispersion * maxIntensity * perSpace;
			final double minutil = dispersion * minIntensity * perSpace;
			final double landweight = Math.exp(dispersion * perLand);
			// Do the expm1 trick to preserve precision.
			final double maxweight = Math.expm1(maxutil);
			final double minweight = Math.expm1(minutil);
			final double denominator = square(dispersion) * square(perSpace);
			final double weightdiff = minweight - maxweight;
			final double utildiff = maxutil * (maxweight + 1) - minutil
					* (minweight + 1);

			return landweight / denominator * (weightdiff + utildiff);
		}
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	protected static double getExpectedFARTwoRangesWithAdjustments(
			double perSpace, double perLand, double landSize, double stepPoint,
			double stepPointAdjustment, double belowStepPointAdjustment,
			double aboveStepPointAdjustment, double minIntensity,
			double maxIntensity, double intensityDispersion) {
		// Add up expected value from each range.
		final double belowPerSpace = perSpace + belowStepPointAdjustment;
		final double belowMaxIntensity = Math.min(stepPoint, maxIntensity);
		final double abovePerSpace = perSpace + aboveStepPointAdjustment;
		final double abovePerLand = perLand + stepPointAdjustment
				+ (belowStepPointAdjustment - aboveStepPointAdjustment) * stepPoint;
		final double aboveMinIntensity = Math.max(minIntensity, stepPoint);
		double result = 0;
		if (minIntensity < stepPoint) {
			result += getExpectedFARSum(belowPerSpace, perLand, landSize,
					minIntensity, belowMaxIntensity, intensityDispersion);
		}
		if (maxIntensity > stepPoint) {
			result += getExpectedFARSum(abovePerSpace, abovePerLand, landSize,
					aboveMinIntensity, maxIntensity, intensityDispersion);
		}

		// Normalize by the total area.
		double integral = 0;
		if (minIntensity < stepPoint) {
			integral += integrateOverIntensityRange(belowPerSpace, perLand, landSize,
					minIntensity, belowMaxIntensity, intensityDispersion);
		}
		if (maxIntensity > stepPoint) {
			integral += integrateOverIntensityRange(abovePerSpace, abovePerLand,
					landSize, aboveMinIntensity, maxIntensity, intensityDispersion);
		}
		return result / integral;
	}

	private static double getExpectedFARProperRanges(double dispersionParameter,
			double landArea, double utilityPerUnitSpace, double utilityPerUnitLand,
			double[] intensityPoints, double[] perSpaceAdjustments,
			double[] perLandAdjustments) {
		final int numRanges = intensityPoints.length - 1;
		double currentLandUtility = utilityPerUnitLand;
		double result = 0;
		double integral = 0;

		for (int i = 0; i < numRanges; i++) {
			if (intensityPoints[i + 1] < intensityPoints[i]) {
				final String msg = "Step points must be non-decreasing; point "
						+ (i + 1) + ": " + intensityPoints[i + 1] + " is less than point "
						+ i + ": " + intensityPoints[i];
				logger.fatal(msg);
				throw new RuntimeException(msg);
			}

			final double perSpace = utilityPerUnitSpace + perSpaceAdjustments[i];
			currentLandUtility += perLandAdjustments[i];
			if (i > 0) {
				currentLandUtility += (perSpaceAdjustments[i - 1] - perSpaceAdjustments[i])
						* intensityPoints[i];
			}

			final double minFAR = intensityPoints[i];
			final double maxFAR = intensityPoints[i + 1];

			result += getExpectedFARSum(perSpace, currentLandUtility, landArea,
					minFAR, maxFAR, dispersionParameter);
			// These integrals are needed to normalize by the total area.
			integral += integrateOverIntensityRange(perSpace, currentLandUtility,
					landArea, minFAR, maxFAR, dispersionParameter);
		}

		return result / integral;
	}

	/**
	 * Returns the expected development intensity over the allowed intensity
	 * range. The utility function is defined as in
	 * <code>getCompositeUtility()</code>.
	 * 
	 * @param dispersionParameter
	 *          The dispersion parameter for the choice over the possible
	 *          intensities.
	 * @param landArea
	 *          The area of the parcel.
	 * @param utilityPerUnitSpace
	 *          The base utility of each unit area of floorspace.
	 * @param utilityPerUnitLand
	 *          The base utility of each unit area of land - i.e. the component of
	 *          utility that does not depend on the amount of floorspace.
	 * @param intensityPoints
	 *          The boundary points for the subranges as FARs. The first and last
	 *          elements are the minimum and maximum allowed FAR while the other
	 *          points are the boundaries between the different subranges. The
	 *          boundary points must be properly ordered - i.e. each boundary
	 *          point must be no less than the previous boundary point. The
	 *          minimum and maximum, however, may be out of order with respect to
	 *          the boundary points, meaning that some of the ranges are
	 *          disallowed entirely.
	 * @param perSpaceAdjustments
	 *          The adjustment to the utility per unit area of floorspace for each
	 *          subrange.
	 * @param perLandAdjustments
	 *          The step sizes at each boundary point - i.e. the difference
	 *          between the utility just above the boundary point and the utility
	 *          just below it. The first element applies at the minimum FAR, so it
	 *          is effectively a global adjustment to the utility.
	 * @return The expected FAR.
	 * @see getCompositeUtility.
	 */
	protected static double getExpectedFAR(double dispersionParameter,
			double landArea, double utilityPerUnitSpace, double utilityPerUnitLand,
			double[] intensityPoints, double[] perSpaceAdjustments,
			double[] perLandAdjustments) {
		if (intensityPoints.length < 2) {
			final String msg = "Need to have at least 2 allowed intensities";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		if (perSpaceAdjustments.length != intensityPoints.length - 1) {
			final String msg = "Intensity space adjustments need to be the same as the specified intensity ranges (i.e. needs to be "
					+ (intensityPoints.length - 1) + ")";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		if (perLandAdjustments.length != intensityPoints.length - 1) {
			final String msg = "Per Land Adjustments need to be one for each specified intensity point below the maximum (i.e. needs to be "
					+ (intensityPoints.length - 1) + ")";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		final int numRanges = intensityPoints.length - 1;
		if (intensityPoints[0] > intensityPoints[numRanges]) {
			final String msg = "Minimum intensity must not be greater than maximum intensity: "
					+ intensityPoints[0] + " > " + intensityPoints[1];
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		// Check for any degenerate ranges.
		int i = 1;
		while (intensityPoints[0] > intensityPoints[i]) {
			i++;
		}
		final int lowestValidRange = i - 1;

		i = numRanges - 1;
		while (intensityPoints[numRanges] < intensityPoints[i]) {
			i--;
		}
		final int highestValidRange = i;

		// If there are no degenerate ranges, we can proceed to the
		// calculations.
		if (lowestValidRange == 0 && highestValidRange == numRanges - 1) {
			return getExpectedFARProperRanges(dispersionParameter, landArea,
					utilityPerUnitSpace, utilityPerUnitLand, intensityPoints,
					perSpaceAdjustments, perLandAdjustments);
		}

		// Reduce number of ranges if necessary to remove any degenerates.
		final int numProperRanges = highestValidRange - lowestValidRange + 1;
		final double[] properIntensityPoints = new double[numProperRanges + 1];
		final double[] properPerSpaceAdjustments = new double[numProperRanges];
		final double[] properPerLandAdjustments = new double[numProperRanges];

		removeInvalidRanges(numRanges, numProperRanges, lowestValidRange,
				highestValidRange, intensityPoints, perSpaceAdjustments,
				perLandAdjustments, properIntensityPoints, properPerSpaceAdjustments,
				properPerLandAdjustments);

		return getExpectedFARProperRanges(dispersionParameter, landArea,
				utilityPerUnitSpace, utilityPerUnitLand, properIntensityPoints,
				properPerSpaceAdjustments, properPerLandAdjustments);
	}

	// Returns a 1 by numRanges matrix.
	private static Matrix getExpectedFARDerivativesWRTExpectedFARSums(
			int numRanges) {
		// The total expected FAR is simply the sum of the FAR sum in each
		// range, so these are all 1.
		final double[] result = new double[numRanges];
		for (int i = 0; i < numRanges; i++) {
			result[i] = 1;
		}
		final Matrix m = new DenseMatrix(1, numRanges);
		return new DenseMatrix(new DenseVector(result)).transpose(m);
	}

	// All of these arrays have to have length numRanges.
	// Returns a numRanges by numRanges matrix.
	private static Matrix getExpectedFARSumDerivativesWRTUtilityIntegrals(
			int numRanges, double[] integrals, double[] perSpaces, double[] perLands,
			double[] minIntensities, double[] maxIntensities, double dispersion) {
		final double[][] result = new double[numRanges][numRanges];
		for (int i = 0; i < numRanges; i++) {
			double derivative;
			final double landweight = Math.exp(dispersion * perLands[i]);
			double integralsum = 0;
			for (int j = 0; j < numRanges; j++) {
				integralsum += integrals[j];
			}
			if (Math.abs(perSpaces[i]) <= epsilon) {
				final double intensityterm = square(maxIntensities[i])
						- square(minIntensities[i]);
				derivative = -landweight / (2 * square(integralsum)) * intensityterm;
			}
			else {
				final double maxutil = dispersion * maxIntensities[i] * perSpaces[i];
				final double minutil = dispersion * minIntensities[i] * perSpaces[i];
				final double maxweight = Math.expm1(maxutil);
				final double minweight = Math.expm1(minutil);

				final double denominator = square(dispersion) * square(perSpaces[i])
						* square(integralsum);
				final double weightdiff = minweight - maxweight;
				final double utildiff = maxutil * (maxweight + 1) - minutil
						* (minweight + 1);
				derivative = -landweight / denominator * (weightdiff + utildiff);
			}
			for (int j = 0; j < numRanges; j++) {
				// Derivative is the same with respect to all utility integrals.
				result[i][j] = derivative;
			}
		}

		return new DenseMatrix(result);
	}

	// integralsum is the total weight from the composite utility calculation.
	private static double getExpectedFARSumDerivativeWRTMaxIntensity(
			double integralsum, double perSpace, double perLand, double minIntensity,
			double maxIntensity, double dispersion) {
		final double landweight = Math.exp(dispersion * perLand);
		final double maxweight = Math.exp(dispersion * maxIntensity * perSpace);
		return landweight / integralsum * maxIntensity * maxweight;
	}

	private static double getExpectedFARSumDerivativeWRTMinIntensity(
			double integralsum, double perSpace, double perLand, double minIntensity,
			double maxIntensity, double dispersion) {
		final double landweight = Math.exp(dispersion * perLand);
		final double minweight = Math.exp(dispersion * perSpace * minIntensity);
		return -landweight / integralsum * minIntensity * minweight;
	}

	private static double getExpectedFARSumDerivativeWRTPerSpace(
			double integralsum, double perSpace, double perLand, double minIntensity,
			double maxIntensity, double dispersion) {
		if (Math.abs(perSpace) <= epsilon) {
			final double landweight = Math.exp(dispersion * perLand);
			return dispersion * landweight / (3 * integralsum)
					* (cube(maxIntensity) - cube(minIntensity));
		}
		else {
			final double landweight = Math.exp(dispersion * perLand);
			final double denominator = square(dispersion) * cube(perSpace)
					* integralsum;
			final double maxutil = dispersion * maxIntensity * perSpace;
			final double minutil = dispersion * minIntensity * perSpace;
			final double maxweight = Math.expm1(maxutil);
			final double minweight = Math.expm1(minutil);

			final double squarediff = square(maxutil) * (maxweight + 1)
					- square(minutil) * (minweight + 1);
			final double utildiff = 2 * minutil * (minweight + 1) - 2 * maxutil
					* (maxweight + 1);
			final double weightdiff = 2 * maxweight - 2 * minweight;

			return landweight / denominator * (squarediff + utildiff + weightdiff);
		}
	}

	private static double getExpectedFARSumDerivativeWRTPerLand(
			double integralsum, double perSpace, double perLand, double minIntensity,
			double maxIntensity, double dispersion) {
		if (Math.abs(perSpace) <= epsilon) {
			final double landweight = Math.exp(dispersion * perLand);
			return dispersion * landweight / (2 * integralsum)
					* (square(maxIntensity) - square(minIntensity));
		}
		else {
			final double landweight = Math.exp(dispersion * perLand);
			final double denominator = dispersion * square(perSpace) * integralsum;
			final double maxutil = dispersion * maxIntensity * perSpace;
			final double minutil = dispersion * minIntensity * perSpace;
			final double maxweight = Math.expm1(maxutil);
			final double minweight = Math.expm1(minutil);

			final double utildiff = maxutil * (maxweight + 1) - minutil
					* (minweight + 1);
			final double weightdiff = minweight - maxweight;

			return landweight / denominator * (utildiff + weightdiff);
		}
	}

	private static double getExpectedFARSumDerivativeWRTDispersion(
			double integralsum, double perSpace, double perLand, double minIntensity,
			double maxIntensity, double dispersion) {
		if (Math.abs(perSpace) <= epsilon) {
			final double landweight = Math.exp(dispersion * perLand);
			return perLand * landweight / (2 * integralsum)
					* (square(maxIntensity) - square(minIntensity));
		}
		else {
			final double landutil = dispersion * perLand;
			final double denominator = square(perSpace) * cube(dispersion)
					* integralsum;
			final double maxutil = dispersion * maxIntensity * perSpace;
			final double minutil = dispersion * minIntensity * perSpace;
			final double landweight = Math.exp(landutil);
			final double maxweight = Math.expm1(maxutil);
			final double minweight = Math.expm1(minutil);

			final double produtildiff = landutil * maxutil * (maxweight + 1)
					- landutil * minutil * (minweight + 1);
			final double squarediff = square(maxutil) * (maxweight + 1)
					- square(minutil) * (minweight + 1);
			final double landutildiff = landutil * (minweight - maxweight);
			final double spaceutildiff = 2 * minutil * (minweight + 1) - 2 * maxutil
					* (maxweight + 1);
			final double weightdiff = 2 * maxweight - 2 * minweight;

			return landweight
					/ denominator
					* (produtildiff + squarediff + landutildiff + spaceutildiff + weightdiff);
		}
	}

	// Finds the direct partial derivatives of the expected FAR sum with respect
	// to the integral parameters.
	// Returns a numRanges by 5 matrix as an array of 1 by 5 matrices.
	private static Matrix[] getExpectedFARSumDerivativesWRTIntegralParameters(
			int numRanges, double[] integrals, double[] perSpaces, double[] perLands,
			double[] minIntensities, double[] maxIntensities, double dispersion) {
		final Matrix[] result = new Matrix[numRanges];

		double integralsum = 0;
		for (int i = 0; i < numRanges; i++) {
			integralsum += integrals[i];
		}

		for (int i = 0; i < numRanges; i++) {
			final double[] row = new double[NUM_INT_PARAMS];
			row[IND_US] = getExpectedFARSumDerivativeWRTPerSpace(integralsum,
					perSpaces[i], perLands[i], minIntensities[i], maxIntensities[i],
					dispersion);
			row[IND_UA] = getExpectedFARSumDerivativeWRTPerLand(integralsum,
					perSpaces[i], perLands[i], minIntensities[i], maxIntensities[i],
					dispersion);
			row[IND_SMIN] = getExpectedFARSumDerivativeWRTMinIntensity(integralsum,
					perSpaces[i], perLands[i], minIntensities[i], maxIntensities[i],
					dispersion);
			row[IND_SMAX] = getExpectedFARSumDerivativeWRTMaxIntensity(integralsum,
					perSpaces[i], perLands[i], minIntensities[i], maxIntensities[i],
					dispersion);
			row[IND_DISP] = getExpectedFARSumDerivativeWRTDispersion(integralsum,
					perSpaces[i], perLands[i], minIntensities[i], maxIntensities[i],
					dispersion);

			final Matrix m = new DenseMatrix(1, 5);
			result[i] = new DenseMatrix(new DenseVector(row)).transpose(m);
		}
		return result;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	protected static Vector getTwoRangeExpectedFARDerivativesWRTParameters(
			double perSpace, double perLand, double landSize, double stepPoint,
			double stepPointAdjustment, double belowStepPointAdjustment,
			double aboveStepPointAdjustment, double minIntensity,
			double maxIntensity, double intensityDispersion) {

		final int numCoeffs = 5;

		int numRanges;
		double[] perSpaces;
		double[] perLands;
		double[] minIntensities;
		double[] maxIntensities;
		double[] integrals;
		if (maxIntensity <= stepPoint) {
			numRanges = 1;
			perSpaces = new double[] { perSpace + belowStepPointAdjustment };
			perLands = new double[] { perLand };
			minIntensities = new double[] { minIntensity };
			maxIntensities = new double[] { maxIntensity };
			integrals = new double[1];
			integrals[0] = integrateOverIntensityRange(perSpaces[0], perLands[0],
					landSize, minIntensities[0], maxIntensities[0], intensityDispersion);
		}
		else if (minIntensity >= stepPoint) {
			numRanges = 1;
			perSpaces = new double[] { perSpace + aboveStepPointAdjustment };
			perLands = new double[] { perLand + stepPointAdjustment
					+ (belowStepPointAdjustment - aboveStepPointAdjustment) * stepPoint };
			minIntensities = new double[] { minIntensity };
			maxIntensities = new double[] { maxIntensity };
			integrals = new double[1];
			integrals[0] = integrateOverIntensityRange(perSpaces[0], perLands[0],
					landSize, minIntensities[0], maxIntensities[0], intensityDispersion);
		}
		else {
			numRanges = 2;
			perSpaces = new double[] { perSpace + belowStepPointAdjustment,
					perSpace + aboveStepPointAdjustment };
			perLands = new double[] {
					perLand,
					perLand + stepPointAdjustment
							+ (belowStepPointAdjustment - aboveStepPointAdjustment)
							* stepPoint };
			minIntensities = new double[] { minIntensity, stepPoint };
			maxIntensities = new double[] { stepPoint, maxIntensity };
			integrals = new double[2];
			integrals[0] = integrateOverIntensityRange(perSpaces[0], perLands[0],
					landSize, minIntensities[0], maxIntensities[0], intensityDispersion);
			integrals[1] = integrateOverIntensityRange(perSpaces[1], perLands[1],
					landSize, minIntensities[1], maxIntensities[1], intensityDispersion);
		}
		final Matrix[] dadt = getIntegralParameterDerivativesWRTParameters(
				minIntensity, maxIntensity, stepPoint, stepPointAdjustment,
				belowStepPointAdjustment, aboveStepPointAdjustment, intensityDispersion);
		final Matrix[] dIda = getUtilityDerivativesWRTIntegralParameters(numRanges,
				perSpaces, perLands, minIntensities, maxIntensities,
				intensityDispersion);
		final Matrix dIdt = multiplyAndAggregate(numRanges, NUM_INT_PARAMS,
				numCoeffs, dIda, dadt);
		final Matrix dIedI = getExpectedFARSumDerivativesWRTUtilityIntegrals(
				numRanges, integrals, perSpaces, perLands, minIntensities,
				maxIntensities, intensityDispersion);
		Matrix dIedt1 = new DenseMatrix(numRanges, numCoeffs);
		dIedt1 = dIedI.mult(dIdt, dIedt1);

		final Matrix[] dIeda = getExpectedFARSumDerivativesWRTIntegralParameters(
				numRanges, integrals, perSpaces, perLands, minIntensities,
				maxIntensities, intensityDispersion);
		final Matrix dIedt2 = multiplyAndAggregate(numRanges, NUM_INT_PARAMS,
				numCoeffs, dIeda, dadt);
		final Matrix dIedt = dIedt1.add(dIedt2);

		final Matrix dEdIe = getExpectedFARDerivativesWRTExpectedFARSums(numRanges);
		Matrix result = new DenseMatrix(1, numCoeffs);
		result = dEdIe.mult(dIedt, result);

		// Convert into a vector.
		final Vector vector = new DenseVector(numCoeffs);
		for (int i = 0; i < numCoeffs; i++) {
			vector.set(i, result.get(0, i));
		}

		return vector;
	}

	private static Vector getExpectedFARDerivativesWRTParametersProperRanges(
			double dispersionParameter, double landArea, double utilityPerUnitSpace,
			double utilityPerUnitLand, double[] intensityPoints,
			double[] perSpaceAdjustments, double[] perLandAdjustments) {
		final int numRanges = intensityPoints.length - 1;
		final int numCoeffs = 3 * numRanges + 2;

		double currentLandUtility = utilityPerUnitLand;
		final double[] perSpaces = new double[numRanges];
		final double[] perLands = new double[numRanges];
		final double[] minFARs = new double[numRanges];
		final double[] maxFARs = new double[numRanges];
		final double[] integrals = new double[numRanges];

		// Find the modified parameters for each range.
		for (int i = 0; i < numRanges; i++) {
			if (intensityPoints[i + 1] < intensityPoints[i]) {
				final String msg = "Step points must be non-decreasing; point "
						+ (i + 1) + ": " + intensityPoints[i + 1] + " is less than point "
						+ i + ": " + intensityPoints[i];
				logger.fatal(msg);
				throw new RuntimeException(msg);
			}

			perSpaces[i] = utilityPerUnitSpace + perSpaceAdjustments[i];
			currentLandUtility += perLandAdjustments[i];
			if (i > 0) {
				currentLandUtility += (perSpaceAdjustments[i - 1] - perSpaceAdjustments[i])
						* intensityPoints[i];
			}
			perLands[i] = currentLandUtility;

			minFARs[i] = intensityPoints[i];
			maxFARs[i] = intensityPoints[i + 1];

			integrals[i] = integrateOverIntensityRange(perSpaces[i], perLands[i],
					landArea, minFARs[i], maxFARs[i], dispersionParameter);
		}

		// Component of partial derivatives through dependency on utility
		// integrals.
		final Matrix[] dadt = getIntegralParameterDerivativesWRTParameters(
				numRanges, perSpaceAdjustments, perLandAdjustments, intensityPoints,
				dispersionParameter);
		final Matrix[] dIda = getUtilityDerivativesWRTIntegralParameters(numRanges,
				perSpaces, perLands, minFARs, maxFARs, dispersionParameter);
		final Matrix dIdt = multiplyAndAggregate(numRanges, NUM_INT_PARAMS,
				numCoeffs, dIda, dadt);
		final Matrix dIedI = getExpectedFARSumDerivativesWRTUtilityIntegrals(
				numRanges, integrals, perSpaces, perLands, minFARs, maxFARs,
				dispersionParameter);
		Matrix dIedt = new DenseMatrix(numRanges, numCoeffs);
		dIedt = dIedI.mult(dIdt, dIedt);

		// Component of partial derivatives through direct dependency of
		// expected value on parameters.
		final Matrix[] dIeda = getExpectedFARSumDerivativesWRTIntegralParameters(
				numRanges, integrals, perSpaces, perLands, minFARs, maxFARs,
				dispersionParameter);
		dIedt.add(multiplyAndAggregate(numRanges, NUM_INT_PARAMS, numCoeffs, dIeda,
				dadt));

		// Multiply by the dependency of expected value on the expected value
		// for each range.
		final Matrix dEdIe = getExpectedFARDerivativesWRTExpectedFARSums(numRanges);
		Matrix result = new DenseMatrix(1, numCoeffs);
		result = dEdIe.mult(dIedt, result);

		// Convert into a vector.
		final Vector vector = new DenseVector(numCoeffs);
		for (int i = 0; i < numCoeffs; i++) {
			vector.set(i, result.get(0, i));
		}

		return vector;
	}

	/**
	 * Returns the partial derivatives of the expected intensity with respect to
	 * each parameter. The utility function is defined as in
	 * <code>getCompositeUtility()</code>.
	 * 
	 * @param dispersionParameter
	 *          The dispersion parameter for the choice over the possible
	 *          intensities.
	 * @param landArea
	 *          The area of the parcel.
	 * @param utilityPerUnitSpace
	 *          The base utility of each unit area of floorspace.
	 * @param utilityPerUnitLand
	 *          The base utility of each unit area of land - i.e. the component of
	 *          utility that does not depend on the amount of floorspace.
	 * @param intensityPoints
	 *          The boundary points for the subranges as FARs. The first and last
	 *          elements are the minimum and maximum allowed FAR while the other
	 *          points are the boundaries between the different subranges. The
	 *          boundary points must be properly ordered - i.e. each boundary
	 *          point must be no less than the previous boundary point. The
	 *          minimum and maximum, however, may be out of order with respect to
	 *          the boundary points, meaning that some of the ranges are
	 *          disallowed entirely.
	 * @param perSpaceAdjustments
	 *          The adjustment to the utility per unit area of floorspace for each
	 *          subrange.
	 * @param perLandAdjustments
	 *          The step sizes at each boundary point - i.e. the difference
	 *          between the utility just above the boundary point and the utility
	 *          just below it. The first element applies at the minimum FAR, so it
	 *          is effectively a global adjustment to the utility.
	 * @return The derivatives with respect to each parameter, with the parameters
	 *         in the following order: the boundary points, the per-space
	 *         adjustments, the per-land adjustments, and the dispersion
	 *         parameter.
	 * @see getCompositeUtility.
	 */
	protected static Vector getExpectedFARDerivativesWRTParameters(
			double dispersionParameter, double landArea, double utilityPerUnitSpace,
			double utilityPerUnitLand, double[] intensityPoints,
			double[] perSpaceAdjustments, double[] perLandAdjustments) {
		if (intensityPoints.length < 2) {
			final String msg = "Need to have at least 2 allowed intensities";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		if (perSpaceAdjustments.length != intensityPoints.length - 1) {
			final String msg = "Intensity space adjustments need to be the same as the specified intensity ranges (i.e. needs to be "
					+ (intensityPoints.length - 1) + ")";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		if (perLandAdjustments.length != intensityPoints.length - 1) {
			final String msg = "Per Land Adjustments need to be one for each specified intensity point below the maximum (i.e. needs to be "
					+ (intensityPoints.length - 1) + ")";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		final int numRanges = intensityPoints.length - 1;
		if (intensityPoints[0] > intensityPoints[numRanges]) {
			final String msg = "Minimum intensity must not be greater than maximum intensity: "
					+ intensityPoints[0] + " > " + intensityPoints[1];
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		// Check for any degenerate ranges.
		int i = 1;
		while (intensityPoints[0] > intensityPoints[i]) {
			i++;
		}
		final int lowestValidRange = i - 1;

		i = numRanges - 1;
		while (intensityPoints[numRanges] < intensityPoints[i]) {
			i--;
		}
		final int highestValidRange = i;

		// If there are no degenerate ranges, we can proceed to the
		// calculations.
		if (lowestValidRange == 0 && highestValidRange == numRanges - 1) {
			return getExpectedFARDerivativesWRTParametersProperRanges(
					dispersionParameter, landArea, utilityPerUnitSpace,
					utilityPerUnitLand, intensityPoints, perSpaceAdjustments,
					perLandAdjustments);
		}

		// Reduce number of ranges if necessary to remove any degenerates.
		final int numProperRanges = highestValidRange - lowestValidRange + 1;
		final double[] properIntensityPoints = new double[numProperRanges + 1];
		final double[] properPerSpaceAdjustments = new double[numProperRanges];
		final double[] properPerLandAdjustments = new double[numProperRanges];

		removeInvalidRanges(numRanges, numProperRanges, lowestValidRange,
				highestValidRange, intensityPoints, perSpaceAdjustments,
				perLandAdjustments, properIntensityPoints, properPerSpaceAdjustments,
				properPerLandAdjustments);

		final Vector result = getExpectedFARDerivativesWRTParametersProperRanges(
				dispersionParameter, landArea, utilityPerUnitSpace, utilityPerUnitLand,
				properIntensityPoints, properPerSpaceAdjustments,
				properPerLandAdjustments);

		// The above vector will be too small. Add zeroes to positions
		// corresponding to invalid ranges (since these parameters
		// will not affect the utility).
		final Vector restored = transformDerivativesForValidRanges(result,
				numRanges, lowestValidRange, highestValidRange, perSpaceAdjustments,
				perLandAdjustments, intensityPoints, dispersionParameter);
		return restored;
	}

	public abstract void doDevelopment();
}
