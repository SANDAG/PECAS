package com.hbaspecto.pecas.sd.estimation;

import java.util.HashMap;
import simpleorm.sessionjdbc.SSessionJdbc;
import com.hbaspecto.discreteChoiceModelling.Coefficient;
import com.hbaspecto.pecas.sd.SpaceTypesI;

public class TransitionConstant
        implements Coefficient
{
    private int                                                                 spacetype1;
    private int                                                                 spacetype2;

    public static final String                                                  TRANSITION_CONST = "trans";

    private static final HashMap<Integer, HashMap<Integer, TransitionConstant>> theCoeffs        = new HashMap<Integer, HashMap<Integer, TransitionConstant>>();

    private TransitionConstant(int oldtype, int newtype)
    {
        spacetype1 = oldtype;
        spacetype2 = newtype;
    }

    @Override
    public double getValue()
    {
        SSessionJdbc sess = SSessionJdbc.getThreadLocalSession();
        return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetype1)
                .getTransitionConstantTo(sess, spacetype2);
    }

    @Override
    public void setValue(double v)
    {
        SSessionJdbc sess = SSessionJdbc.getThreadLocalSession();
        SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetype1).setTransitionConstantTo(
                sess, spacetype2, v);
    }

    @Override
    public String getName()
    {
        return TRANSITION_CONST + "-" + spacetype1 + "-" + spacetype2;
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

    public int getOldSpaceType()
    {
        return spacetype1;
    }

    public int getNewSpaceType()
    {
        return spacetype2;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    public static TransitionConstant getCoeff(int oldtype, int newtype)
    {
        if (theCoeffs.containsKey(oldtype))
        {
            HashMap<Integer, TransitionConstant> spaceTypeCoeffs = theCoeffs.get(oldtype);
            if (spaceTypeCoeffs.containsKey(newtype)) return spaceTypeCoeffs.get(newtype);
            else
            {
                TransitionConstant newCoeff = new TransitionConstant(oldtype, newtype);
                spaceTypeCoeffs.put(newtype, newCoeff);
                return newCoeff;
            }
        } else
        {
            HashMap<Integer, TransitionConstant> spaceTypeCoeffs = new HashMap<Integer, TransitionConstant>();
            theCoeffs.put(oldtype, spaceTypeCoeffs);
            TransitionConstant newCoeff = new TransitionConstant(oldtype, newtype);
            spaceTypeCoeffs.put(newtype, newCoeff);
            return newCoeff;
        }
    }
}
