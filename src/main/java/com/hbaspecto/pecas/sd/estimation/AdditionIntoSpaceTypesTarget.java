package com.hbaspecto.pecas.sd.estimation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.hbaspecto.pecas.sd.SpaceTypesI;
import com.hbaspecto.pecas.sd.ZoningRulesI;

public class AdditionIntoSpaceTypesTarget
        extends EstimationTarget
        implements ExpectedValue
{

    private final int[]        spaceTypes;
    private double             modelledValue;
    private double[]           derivs;
    public static final String NAME = "additiontypes";

    public AdditionIntoSpaceTypesTarget(int[] spacetypes)
    {
        spaceTypes = spacetypes;
    }

    public AdditionIntoSpaceTypesTarget(String[] specification)
    {
        spaceTypes = new int[specification.length - 1];
        for (int i = 1; i < specification.length; i++)
        {
            spaceTypes[i - 1] = Integer.valueOf(specification[i]);
        }
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
        // Must not be vacant to be counted.
        if (SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(ZoningRulesI.land.getCoverage())
                .isVacant())
        {
            return 0;
        }
        for (final int spaceTypeIHave : spaceTypes)
        {
            if (spaceTypeIHave == spacetype)
            {
                return expectedAddedSpace;
            }
        }
        return 0;
    }

    @Override
    public double getModelledTotalNewDerivativeWRTAddedSpace(int spacetype,
            double expectedAddedSpace, double expectedNewSpace)
    {
        if (SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(ZoningRulesI.land.getCoverage())
                .isVacant())
        {
            return 0;
        }
        for (final int spaceTypeIHave : spaceTypes)
        {
            if (spaceTypeIHave == spacetype)
            {
                return 1;
            }
        }
        return 0;
    }

    @Override
    public double getModelledTotalNewDerivativeWRTNewSpace(int spacetype,
            double expectedAddedSpace, double expectedNewSpace)
    {
        return 0;
    }

    @Override
    public String getName()
    {
        final StringBuffer buf = new StringBuffer(NAME);
        for (final int type : spaceTypes)
        {
            buf.append("-");
            buf.append(type);
        }
        return buf.toString();
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
