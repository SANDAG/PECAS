package com.hbaspecto.pecas.sd.estimation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.hbaspecto.pecas.sd.SpaceTypesI;
import com.hbaspecto.pecas.sd.ZoningRulesI;

public class AdditionIntoSpaceTypeTarget extends EstimationTarget implements ExpectedValue {
	
	private int spaceType;
	private double modelledValue;
	private double[] derivs;
	public static final String NAME = "addition";

	public AdditionIntoSpaceTypeTarget(int spacetype) {
	    spaceType = spacetype;
	}
	
	public int getSpacetype() {
	    return spaceType;
	}
	
	@Override
	public boolean appliesToCurrentParcel() {
	    return true;
	}
	
    @Override
    public double getModelledTotalNewValueForParcel(int spacetype, double expectedAddedSpace,
            double expectedNewSpace) {
        // Must not be vacant to be counted.
        if(SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(ZoningRulesI.land.getCoverage()).isVacant())
            return 0;
        // Must be in the correct spacetype to be counted.
        if(spaceType != spacetype)
            return 0;
        return expectedAddedSpace;
    }

    @Override
    public double getModelledTotalNewDerivativeWRTAddedSpace(int spacetype, double expectedAddedSpace,
            double expectedNewSpace) {
        if(SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(ZoningRulesI.land.getCoverage()).isVacant())
            return 0;
        if(spaceType != spacetype)
            return 0;
        return 1;
    }

    @Override
    public double getModelledTotalNewDerivativeWRTNewSpace(int spacetype, double expectedAddedSpace,
            double expectedNewSpace) {
       
        return 0;
    }

    @Override
    public String getName() {
        return NAME + "-" + spaceType;
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
        return Collections.<ExpectedValue>singletonList(this);
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
