package com.hbaspecto.pecas.sd.estimation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import simpleorm.sessionjdbc.SSessionJdbc;

import com.hbaspecto.pecas.land.Tazs;
import com.hbaspecto.pecas.sd.ZoningRulesI;

public class SpaceTypeTAZGroupTarget extends EstimationTarget implements ExpectedValue {
	
	private int groupNumber;
	private Set group;
	private int spaceType;
    private double modelledValue;
    private double[] derivs;
	public static final String NAME = "grouptarg";
	
	public SpaceTypeTAZGroupTarget(int groupNumber, Set group, int spacetype) {
		this.groupNumber =groupNumber;
	    this.group = group;
	    spaceType = spacetype;
	}
	
	public Set getGroup() {
	    return group;
	}
	
	public int getSpacetype() {
	    return spaceType;
	}
	
	@Override
	public boolean appliesToCurrentParcel() {
	    if (group.contains(
	    		Tazs.getTazRecord(ZoningRulesI.land.getTaz()).get_TazNumber()
	    )) { 
	    	return true;
	    }
	    return false;
	}
	
    @Override
    public double getModelledTotalNewValueForParcel(int spacetype, double expectedAddedSpace,
            double expectedNewSpace) {
        // Must be in the correct TAZ to be counted.
        SSessionJdbc session = SSessionJdbc.getThreadLocalSession();
        if(!appliesToCurrentParcel())
            return 0;
        // Must be of the correct spacetype to be counted.
        if(spaceType != spacetype)
            return 0;
        return expectedAddedSpace + expectedNewSpace;
    }
    
    @Override
    public double getModelledTotalNewDerivativeWRTAddedSpace(int spacetype, double expectedAddedSpace,
            double expectedNewSpace) {
        if(!appliesToCurrentParcel())
            return 0;
        if(spaceType != spacetype)
            return 0;
        return 1;
    }
    
    @Override
    public double getModelledTotalNewDerivativeWRTNewSpace(int spacetype, double expectedAddedSpace,
            double expectedNewSpace) {
        if(!appliesToCurrentParcel())
            return 0;
        if(spaceType != spacetype)
            return 0;
        return 1;
    }

    @Override
    public String getName() {
        return NAME + "-" + spaceType + "-" + groupNumber;
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
