package com.hbaspecto.pecas.sd.estimation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SpaceTypeTotalTarget
        extends EstimationTarget
        implements ExpectedValue
{

    private final int          spaceType;
    private double             modelledValue;
    private double[]           derivs;
    public static final String NAME = "tottarg";

    public SpaceTypeTotalTarget(int spacetype)
    {
        spaceType = spacetype;
    }

    public int getSpacetype()
    {
        return spaceType;
    }

    @Override
    public boolean appliesToCurrentParcel()
    {
        return true;
    }

    @Override
    public double getModelledTotalNewValueForParcel(int spacetype, double expectedAddedSpace,
            double expectedNewSpace)
    {
        // Must be of the correct spacetype to be counted.
        if (spaceType != spacetype)
        {
            return 0;
        }
        return expectedAddedSpace + expectedNewSpace;
    }

    @Override
    public double getModelledTotalNewDerivativeWRTAddedSpace(int spacetype,
            double expectedAddedSpace, double expectedNewSpace)
    {
        if (spaceType != spacetype)
        {
            return 0;
        }
        return 1;
    }

    @Override
    public double getModelledTotalNewDerivativeWRTNewSpace(int spacetype,
            double expectedAddedSpace, double expectedNewSpace)
    {
        if (spaceType != spacetype)
        {
            return 0;
        }
        return 1;
    }

    @Override
    public String getName()
    {
        return NAME + "-" + spaceType;
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
