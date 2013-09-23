package com.hbaspecto.pecas.sd;

import java.util.List;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;
import org.apache.log4j.Logger;
import com.hbaspecto.discreteChoiceModelling.Coefficient;
import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.NoAlternativeAvailable;
import com.hbaspecto.pecas.land.ParcelInterface;
import com.hbaspecto.pecas.land.LandInventory.NotSplittableException;
import com.hbaspecto.pecas.sd.estimation.ExpectedValue;
import com.hbaspecto.pecas.sd.estimation.SpaceTypeCoefficient;

/**
 * @author Abdel
 * @date August 5, 2009 This class represents the derelict alernative
 */

public class DerelictAlternative
        extends DevelopmentAlternative
{

    private static Logger logger = Logger.getLogger(DerelictAlternative.class);

    public DerelictAlternative()
    {
    }

    @Override
    public double getUtilityNoSizeEffect() throws ChoiceModelOverflowException
    {

        int oldCoverage = ZoningRulesI.land.getCoverage();
        SpaceTypesI spaceType = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(oldCoverage);
        // You can't choose derelict for an already derelict land or a vacant
        // space type of land.
        if (spaceType.isVacant() || ZoningRulesI.land.isDerelict())
            return Double.NEGATIVE_INFINITY;

        return getTransitionConstant().getValue();
    }

    @Override
    public double getUtility(double dispersionParameterForSizeTermCalculation)
            throws ChoiceModelOverflowException
    {
        return getUtilityNoSizeEffect();
    }

    public void doDevelopment()
    {
        double size = ZoningRulesI.land.getLandArea();
        if (size > ZoningRulesI.land.getMaxParcelSize())
        {
            // If development occurs on a parcel that is greater than n acres,
            // split off n acres into a new "pseudo parcel" and add the new
            // pseudo parcel into the database
            int splits = ((int) (size / ZoningRulesI.land.getMaxParcelSize())) + 1;
            double parcelSizes = size / splits;
            ParcelInterface newBit;
            try
            {
                newBit = ZoningRulesI.land.splitParcel(parcelSizes);
            } catch (NotSplittableException e)
            {
                logger.fatal("Can't split parcel " + e);
                throw new RuntimeException("Can't split parcel", e);
            }

            newBit.set_IsDerelict(true);
            ZoningRulesI.land.getDevelopmentLogger().logDerelicationWithSplit(ZoningRulesI.land,
                    newBit);
        } else
        {
            boolean oldIsDerelict = ZoningRulesI.land.isDerelict();

            ZoningRulesI.land.putDerelict(true);
            ZoningRulesI.land.getDevelopmentLogger().logDereliction(ZoningRulesI.land,
                    oldIsDerelict);
        }
    }

    @Override
    public Vector getExpectedTargetValues(List<ExpectedValue> ts) throws NoAlternativeAvailable,
            ChoiceModelOverflowException
    {
        // Never any development in the derelict option.
        return new DenseVector(ts.size());
    }

    @Override
    public Vector getUtilityDerivativesWRTParameters(List<Coefficient> cs)
            throws NoAlternativeAvailable, ChoiceModelOverflowException
    {
        // Derivative wrt derelict constant is 1, all others are 0.
        Vector derivatives = new DenseVector(cs.size());
        int spacetype = ZoningRulesI.land.getCoverage();
        SpaceTypesI spaceType = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetype);
        // If derelict alternative is not available, return all 0s.
        if (spaceType.isVacant() || ZoningRulesI.land.isDerelict()) return derivatives;

        Coefficient derelictConst = getTransitionConstant();
        int index = cs.indexOf(derelictConst);
        if (index >= 0) derivatives.set(index, 1);
        return derivatives;
    }

    @Override
    public Matrix getExpectedTargetDerivativesWRTParameters(List<ExpectedValue> ts,
            List<Coefficient> cs) throws NoAlternativeAvailable, ChoiceModelOverflowException
    {
        // Never any development in the derelict option.
        return new DenseMatrix(ts.size(), cs.size());
    }

    private Coefficient getTransitionConstant()
    {
        int spacetype = ZoningRulesI.land.getCoverage();
        return SpaceTypeCoefficient.getDerelictTransitionConst(spacetype);
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
