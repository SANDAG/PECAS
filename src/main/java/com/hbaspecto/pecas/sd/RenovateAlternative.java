/**
 * 
 */
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
import com.hbaspecto.pecas.land.LandInventory.NotSplittableException;
import com.hbaspecto.pecas.land.ParcelInterface;
import com.hbaspecto.pecas.sd.estimation.ExpectedValue;
import com.hbaspecto.pecas.sd.estimation.RenovationTarget;
import com.hbaspecto.pecas.sd.estimation.SpaceTypeCoefficient;

/**
 * @author Abdel
 * @date August 5, 2009 This class represents the renovate alernative
 */
public class RenovateAlternative
        extends DevelopmentAlternative
{

    /*
     * (non-Javadoc)
     * 
     * @see com.hbaspecto.discreteChoiceModelling.Alternative#getUtility(double)
     */

    private static Logger logger = Logger.getLogger(DerelictAlternative.class);

    public RenovateAlternative()
    {
    }

    @Override
    public double getUtility(double dispersionParameterForSizeTermCalculation)
            throws ChoiceModelOverflowException
    {
        return getUtilityNoSizeEffect();
    }

    @Override
    public double getUtilityNoSizeEffect() throws ChoiceModelOverflowException
    {

        if (getExistingSpaceType().isVacant())
        {
            return Double.NEGATIVE_INFINITY;
        }

        final double Thjp = getUtilityPerUnitSpace();

        final double Trhjp = getUtilityPerUnitLand();

        // TODO T(hjp) and Tr(hjp) could be different for different ranges of j,
        // for now assume constant values

        double result = Thjp * ZoningRulesI.land.getQuantity() / ZoningRulesI.land.getLandArea()
                + Trhjp;

        final Coefficient renovateConstant = getTransitionConstant();
        result += renovateConstant.getValue();

        return result;
    }

    private double getUtilityPerUnitLand()
    {
        // no landPrep and no demolishing costs
        return 0;
    }

    private double getUtilityPerUnitSpace()
    {
        final int age = 0;

        final double rent = ZoningRulesI.land.getPrice(getExistingSpaceType().getSpaceTypeID(),
                ZoningRulesI.currentYear, ZoningRulesI.baseYear)
                * getExistingSpaceType().getRentDiscountFactor(age);

        double cost;
        if (ZoningRulesI.land.isDerelict())
        {
            cost = getExistingSpaceType().getRenovationDerelictCost(
                    ZoningRulesI.land.get_CostScheduleId())
                    * ZoningRulesI.amortizationFactor
                    + getExistingSpaceType().getAdjustedMaintenanceCost(age);
        } else
        {
            cost = getExistingSpaceType().getRenovationCost(ZoningRulesI.land.get_CostScheduleId())
                    * ZoningRulesI.amortizationFactor
                    + getExistingSpaceType().getAdjustedMaintenanceCost(age);
        }
        return rent - cost;
    }

    @Override
    public void doDevelopment()
    {
        final double size = ZoningRulesI.land.getLandArea();
        if (size > ZoningRulesI.land.getMaxParcelSize())
        {
            // If development occurs on a parcel that is greater than n acres,
            // split off n acres into a new "pseudo parcel" and add the new
            // pseudo parcel into the database
            final int splits = (int) (size / ZoningRulesI.land.getMaxParcelSize()) + 1;
            final double parcelSizes = size / splits;
            ParcelInterface newBit;
            try
            {
                newBit = ZoningRulesI.land.splitParcel(parcelSizes);
            } catch (final NotSplittableException e)
            {
                logger.fatal("Can't split parcel " + e);
                throw new RuntimeException("Can't split parcel", e);
            }

            newBit.set_IsDerelict(false);
            newBit.set_YearBuilt(ZoningRulesI.currentYear);

            ZoningRulesI.land.getDevelopmentLogger().logRenovationWithSplit(ZoningRulesI.land,
                    newBit);
        } else
        {

            final int old_year_built = ZoningRulesI.land.getYearBuilt();
            final boolean oldIsDerelict = ZoningRulesI.land.isDerelict();

            ZoningRulesI.land.putYearBuilt(ZoningRulesI.currentYear);
            ZoningRulesI.land.putDerelict(false);

            ZoningRulesI.land.getDevelopmentLogger().logRenovation(ZoningRulesI.land,
                    old_year_built, oldIsDerelict);
        }
    }

    private SpaceTypesI getExistingSpaceType()
    {
        final int oldCoverage = ZoningRulesI.land.getCoverage();
        return SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(oldCoverage);
    }

    private Coefficient getTransitionConstant()
    {
        final int spacetype = ZoningRulesI.land.getCoverage();
        if (ZoningRulesI.land.isDerelict())
        {
            // In case of renovating a derelict space!
            return SpaceTypeCoefficient.getRenovateDerelictConst(spacetype);
        } else
        {
            return SpaceTypeCoefficient.getRenovateTransitionConst(spacetype);
        }
    }

    @Override
    public Vector getExpectedTargetValues(List<ExpectedValue> ts) throws NoAlternativeAvailable,
            ChoiceModelOverflowException
    {
        // TODO cache target
        // if(targetCached)
        // return lastTarget.copy();
        final int currentSpaceType = ZoningRulesI.land.getCoverage();
        final double quantity = ZoningRulesI.land.getQuantity();
        final Vector result = new DenseVector(ts.size());
        int i = 0;
        for (final ExpectedValue value : ts)
        {
            if (value instanceof RenovationTarget)
            {
                final RenovationTarget redevT = (RenovationTarget) value;
                result.set(i,
                        redevT.getModelledRenovateQuantityForParcel(currentSpaceType, quantity));
            }
            i++;
        }
        // if(caching)
        // targetCached = true;
        // lastTarget = result;
        // return lastTarget.copy();
        return result;

    }

    @Override
    public Vector getUtilityDerivativesWRTParameters(List<Coefficient> cs)
            throws NoAlternativeAvailable, ChoiceModelOverflowException
    {
        // If parcel is derelict, derivative wrt renovate derelict constant is
        // 1, all others are 0.
        // Otherwise, derivative wrt renovate constant is 1, all others are 0.
        final Vector derivatives = new DenseVector(cs.size());
        if (getExistingSpaceType().isVacant())
        {
            return derivatives;
        }

        final Coefficient renovateConst = getTransitionConstant();
        // depending on whether parcel is derelict or not it will be sensitive
        // to a different parameter
        final int index = cs.indexOf(renovateConst);
        if (index >= 0)
        {
            derivatives.set(index, 1);
        }
        return derivatives;
    }

    @Override
    public Matrix getExpectedTargetDerivativesWRTParameters(List<ExpectedValue> ts,
            List<Coefficient> cs) throws NoAlternativeAvailable, ChoiceModelOverflowException
    {
        final Matrix m = new DenseMatrix(ts.size(), cs.size());

        return m; // Quantity of renovation actually does not change with
        // parameters.

        // SpaceTypesI myDt =
        // SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(ZoningRulesI.land.getCoverage());
        // if (myDt.isVacant()) return m; // can't renovate vacant parcel, no
        // derivative
        //
        // int spacetype = myDt.get_SpaceTypeId();
        //
        // double quantity = ZoningRulesI.land.getQuantity();
        //
        // // Build vector of derivatives of the targets with respect to the
        // expected renovated space.
        // Matrix dTdE = new DenseMatrix(ts.size(), 1);
        // int i = 0;
        // for(ExpectedValue value : ts) {
        // if (value instanceof RenovationTarget) {
        // RenovationTarget redevTarget = (RenovationTarget) value;
        // dTdE.set(i,0,redevTarget.getModelledRenovateDerivativeForParcel(spacetype,
        // quantity));
        // }
        // i++;
        // }
        //
        // // Scale by land area because of the chain rule ?
        // //dTdE.scale(ZoningRulesI.land.getLandArea());
        //
        // // Build vector of derivatives of the expected added space with
        // respect to the parameters.
        // Matrix dEdt = new DenseMatrix(1, cs.size());
        // int renovateIndex = cs.indexOf(getTransitionConstant());
        // if(renovateIndex >= 0) dEdt.set(0, renovateIndex, quantity);
        // Matrix answer = new DenseMatrix(ts.size(), cs.size());
        // answer = dTdE.mult(dEdt, answer);
        //
        // return answer;
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
