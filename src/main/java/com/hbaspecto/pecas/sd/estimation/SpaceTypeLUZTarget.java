package com.hbaspecto.pecas.sd.estimation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.hbaspecto.pecas.land.Tazs;
import com.hbaspecto.pecas.sd.ZoningRulesI;

public class SpaceTypeLUZTarget extends EstimationTarget implements
		ExpectedValue {

	private final int luz;
	private final int spaceType;
	private double modelledValue;
	private double[] derivs;
	public static final String NAME = "luztarg";

	public SpaceTypeLUZTarget(int zone, int spacetype) {
		luz = zone;
		spaceType = spacetype;
	}

	public int getZone() {
		return luz;
	}

	public int getSpacetype() {
		return spaceType;
	}

	@Override
	public boolean appliesToCurrentParcel() {
		return Tazs.getTazRecord(ZoningRulesI.land.getTaz()).get_LuzNumber() == luz;
	}

	@Override
	public double getModelledTotalNewValueForParcel(int spacetype,
			double expectedAddedSpace, double expectedNewSpace) {
		// Must be in the correct TAZ to be counted.
		if (Tazs.getTazRecord(ZoningRulesI.land.getTaz()).get_LuzNumber() != luz) {
			return 0;
		}
		// Must be of the correct spacetype to be counted.
		if (spaceType != spacetype) {
			return 0;
		}
		return expectedAddedSpace + expectedNewSpace;
	}

	@Override
	public double getModelledTotalNewDerivativeWRTAddedSpace(int spacetype,
			double expectedAddedSpace, double expectedNewSpace) {
		if (Tazs.getTazRecord(ZoningRulesI.land.getTaz()).get_LuzNumber() != luz) {
			return 0;
		}
		if (spaceType != spacetype) {
			return 0;
		}
		return 1;
	}

	@Override
	public double getModelledTotalNewDerivativeWRTNewSpace(int spacetype,
			double expectedAddedSpace, double expectedNewSpace) {
		if (Tazs.getTazRecord(ZoningRulesI.land.getTaz()).get_LuzNumber() != luz) {
			return 0;
		}
		if (spaceType != spacetype) {
			return 0;
		}
		return 1;
	}

	@Override
	public String getName() {
		return NAME + "-" + spaceType + "-" + luz;
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
