/*
 * Created on 28-Oct-2005
 * 
 * Copyright 2005 HBA Specto Incorporated
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
import com.hbaspecto.pecas.land.LandInventory;
import com.hbaspecto.pecas.land.LandInventory.NotSplittableException;
import com.hbaspecto.pecas.land.ParcelInterface;
import com.hbaspecto.pecas.sd.estimation.DemolitionTarget;
import com.hbaspecto.pecas.sd.estimation.ExpectedValue;
import com.hbaspecto.pecas.sd.estimation.SpaceTypeCoefficient;

class DemolishAlternative
        extends DevelopmentAlternative
{

    // ZoningPermissions zoningReg;
    double        sizeTerm;                                            // size
                                                                        // term
                                                                        // for
                                                                        // change
                                                                        // alternatives.
    static Logger logger = Logger.getLogger(DemolishAlternative.class);

    public DemolishAlternative()
    {
    }

    public double getUtility(double higherLevelDispersionParameter)
    {
        // can't demolish vacant land to make it vacant still.
        if (ZoningRulesI.land.getCoverage() == LandInventory.VACANT_ID)
            return Double.NEGATIVE_INFINITY;

        // Equation is: 1/LotSize * (-EArea(v) * TrCostsS(v,h=vEx)) +
        // TrConst(v,h=vEx)
        double Trhjp = getUtilityPerUnitLand();

        return Trhjp + getTransitionConstant().getValue();
    }

    @Override
    public double getUtilityNoSizeEffect() throws ChoiceModelOverflowException
    {
        return getUtility(1.0);
    }

    public void doDevelopment()
    {
        double size = ZoningRulesI.land.getLandArea();
        if (size > ZoningRulesI.land.getMaxParcelSize())
        {
            // If development occurs on a parcel that is greater than n acres,
            // split off n acres into a new "pseudo parcel" and add the new
            // pseudo parcel into the database
            long orig_parcel_num = ZoningRulesI.land.getPECASParcelNumber();
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
            double oldDevQuantity = newBit.get_SpaceQuantity();

            newBit.set_SpaceQuantity(0);
            newBit.set_SpaceTypeId(LandInventory.VACANT_ID);
            newBit.set_YearBuilt(ZoningRulesI.currentYear);
            newBit.set_IsDerelict(false);

            ZoningRulesI.land.getDevelopmentLogger().logDemolitionWithSplit(ZoningRulesI.land,
                    newBit, oldDevQuantity);
        } else
        {

            double oldSquareFeet = ZoningRulesI.land.getQuantity();
            int oldDT = ZoningRulesI.land.getCoverage();
            int oldYear = ZoningRulesI.land.getYearBuilt();
            boolean oldIsDerelict = ZoningRulesI.land.isDerelict();

            ZoningRulesI.land.putCoverage(LandInventory.VACANT_ID);
            ZoningRulesI.land.putQuantity(0);
            ZoningRulesI.land.putYearBuilt(ZoningRulesI.currentYear);
            ZoningRulesI.land.putDerelict(false);

            ZoningRulesI.land.getDevelopmentLogger().logDemolition(ZoningRulesI.land, oldDT,
                    oldSquareFeet, oldYear, oldIsDerelict);
        }
    }

    private double getUtilityPerUnitLand()
    {
        int oldCoverageCode = ZoningRulesI.land.getCoverage();

        SpaceTypesI oldSpaceType = SpaceTypesI
                .getAlreadyCreatedSpaceTypeBySpaceTypeID(oldCoverageCode);
        double amortizedDemolitionCost = oldSpaceType.getDemolitionCost(ZoningRulesI.land
                .get_CostScheduleId())
                * ZoningRulesI.land.getQuantity()
                * ZoningRulesI.amortizationFactor / ZoningRulesI.land.getLandArea();

        return -amortizedDemolitionCost;
    }

    public Vector getExpectedTargetValues(List<ExpectedValue> ts) throws NoAlternativeAvailable,
            ChoiceModelOverflowException
    {
        int currentSpaceType = ZoningRulesI.land.getCoverage();
        double quantity = ZoningRulesI.land.getQuantity();
        Vector result = new DenseVector(ts.size());
        int i = 0;
        for (ExpectedValue value : ts)
        {
            if (value instanceof DemolitionTarget)
            {
                DemolitionTarget demoT = (DemolitionTarget) value;
                result.set(i,
                        demoT.getModelledDemolishQuantityForParcel(currentSpaceType, quantity));
            }
            i++;
        }
        return result;
    }

    @Override
    public Vector getUtilityDerivativesWRTParameters(List<Coefficient> cs)
            throws NoAlternativeAvailable, ChoiceModelOverflowException
    {
        // Derivative wrt demolish constant is 1, all others are 0.
        Vector derivatives = new DenseVector(cs.size());
        // Can't demolish vacant land - return all 0s.
        if (ZoningRulesI.land.getCoverage() == LandInventory.VACANT_ID) return derivatives;

        Coefficient demolishConst = getTransitionConstant();
        int index = cs.indexOf(demolishConst);
        if (index >= 0) derivatives.set(index, 1);
        return derivatives;
    }

    @Override
    public Matrix getExpectedTargetDerivativesWRTParameters(List<ExpectedValue> ts,
            List<Coefficient> cs) throws NoAlternativeAvailable, ChoiceModelOverflowException
    {
        // Quantity of demolition does not change with parameters.
        return new DenseMatrix(ts.size(), cs.size());
    }

    private Coefficient getTransitionConstant()
    {
        int spacetype = ZoningRulesI.land.getCoverage();
        return SpaceTypeCoefficient.getDemolishTransitionConst(spacetype);
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
