package com.hbaspecto.pecas.sd.estimation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbaspecto.pecas.sd.ZoningRulesI;

public class SpaceTypeIntensityTarget extends EstimationTarget {

	private final int spaceType;
	private double expectedFARSum;
	private double expectedBuildNewEvents;
	private double[] expectedFARSumDerivatives;
	private double[] expectedBuildNewEventsDerivatives;
	private List<ExpectedValue> associates = null;
	public static final String NAME = "fartarg";

	public SpaceTypeIntensityTarget(int spacetype) {
		spaceType = spacetype;
	}

	public int getSpacetype() {
		return spaceType;
	}

	@Override
	public String getName() {
		return NAME + "-" + spaceType;
	}

	@Override
	public double getModelledValue() {
		return expectedFARSum / expectedBuildNewEvents;
	}

	@Override
	public double[] getDerivatives() {
		final int numCoeffs = expectedFARSumDerivatives.length;
		final double[] result = new double[numCoeffs];
		for (int i = 0; i < numCoeffs; i++) {
			// Apply the quotient rule to find the derivative of expectedFARSum
			// / expectedBuildNewEvents.
			final double loDhi = expectedBuildNewEvents
					* expectedFARSumDerivatives[i];
			final double hiDlo = expectedFARSum
					* expectedBuildNewEventsDerivatives[i];
			final double denominatorSquared = expectedBuildNewEvents
					* expectedBuildNewEvents;
			result[i] = (loDhi - hiDlo) / denominatorSquared;
		}

		return result;
	}

	@Override
	public List<ExpectedValue> getAssociatedExpectedValues() {
		if (associates == null) {
			associates = new ArrayList<ExpectedValue>();
			associates.add(new ExpectedFARSum());
			associates.add(new ExpectedBuildNewEvents());
			return associates;
		}
		return associates;
	}

	// Calculates the expected sum of the FARs of all of the parcels on which
	// Build-new is selected.
	private class ExpectedFARSum implements ExpectedValue {
		@Override
		public boolean appliesToCurrentParcel() {
			return true;
		}

		@Override
		public double getModelledTotalNewValueForParcel(int spacetype,
				double expectedAddedSpace, double expectedNewSpace) {
			if (spaceType != spacetype) {
				return 0;
			}
			// Only new space counts for this target.
			return expectedNewSpace / ZoningRulesI.land.getLandArea();
		}

		@Override
		public double getModelledTotalNewDerivativeWRTAddedSpace(int spacetype,
				double expectedAddedSpace, double expectedNewSpace) {
			// This target doesn't include added space.
			return 0;
		}

		@Override
		public double getModelledTotalNewDerivativeWRTNewSpace(int spacetype,
				double expectedAddedSpace, double expectedNewSpace) {
			if (spaceType != spacetype) {
				return 0;
			}
			return 1 / ZoningRulesI.land.getLandArea();
		}

		@Override
		public void setModelledValue(double value) {
			expectedFARSum = value;
		}

		@Override
		public void setDerivatives(double[] derivatives) {
			expectedFARSumDerivatives = Arrays
					.copyOf(derivatives, derivatives.length);
		}
	}

	// Class that counts the expected number of times the Build-new alternative
	// will be selected.
	private class ExpectedBuildNewEvents implements ExpectedValue {

		@Override
		public boolean appliesToCurrentParcel() {
			return true;
		}

		@Override
		public double getModelledTotalNewValueForParcel(int spacetype,
				double expectedAddedSpace, double expectedNewSpace) {
			if (spaceType == spacetype && expectedNewSpace > 0) {
				return 1;
			}
			else {
				return 0;
			}
		}

		@Override
		public double getModelledTotalNewDerivativeWRTAddedSpace(int spacetype,
				double expectedAddedSpace, double expectedNewSpace) {
			return 0;
		}

		@Override
		public double getModelledTotalNewDerivativeWRTNewSpace(int spacetype,
				double expectedAddedSpace, double expectedNewSpace) {
			return 0;
		}

		@Override
		public void setModelledValue(double value) {
			expectedBuildNewEvents = value;
		}

		@Override
		public void setDerivatives(double[] derivatives) {
			expectedBuildNewEventsDerivatives = Arrays.copyOf(derivatives,
					derivatives.length);
		}

	}
}
