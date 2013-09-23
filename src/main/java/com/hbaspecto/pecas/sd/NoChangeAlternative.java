/*
 * Created on 28-Oct-2005
 * 
 * Copyright 2005 HBA Specto Incorporated
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
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
import com.hbaspecto.pecas.sd.estimation.ExpectedValue;
import com.hbaspecto.pecas.sd.estimation.SpaceTypeCoefficient;

class NoChangeAlternative
        extends DevelopmentAlternative
{
    /**
     * Comment for <code>scheme</code>
     */
    private final ZoningRulesI scheme;
    static Logger              logger = Logger.getLogger(NoChangeAlternative.class);

    /**
     * @param scheme
     */
    NoChangeAlternative(ZoningRulesI scheme)
    {
        this.scheme = scheme;
    }

    @Override
    public double getUtility(double dispersionParameterForSizeTermCalculation)
            throws ChoiceModelOverflowException
    {
        return getUtilityNoSizeEffect();
    }

    public double getUtilityNoSizeEffect() throws ChoiceModelOverflowException
    {
        // the new way, with continuous intensity options
        double Thjp = getUtilityPerUnitSpace();

        double Trhjp = getUtilityPerUnitLand();

        // TODO T(hjp) and Tr(hjp) could be different for different ranges of j, for now assume constant values

        double result = Thjp * ZoningRulesI.land.getQuantity() / ZoningRulesI.land.getLandArea()
                + Trhjp;
        if (Double.isNaN(result))
        {
            // oh oh!!
            double landSize = ZoningRulesI.land.getLandArea();
            logger.error("NAN utility for NoChangeAlternative");
            logger.error("Trhjp (utility per unit land)= " + Trhjp + "; Thjp=" + Thjp
                    + " landsize=" + landSize);
            throw new ChoiceModelOverflowException(
                    "NAN utility for NoChangeAlternative Trhjp (utility per unit land)= " + Trhjp
                            + " landsize=" + landSize);
        }
        result += getTransitionConstant().getValue();
        return result;
    }

    private double getUtilityPerUnitSpace()
    {

        SpaceTypesI dt = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(ZoningRulesI.land
                .getCoverage());
        if (dt.isVacant() || ZoningRulesI.land.isDerelict()) return 0;

        int age = ZoningRulesI.currentYear - ZoningRulesI.land.getYearBuilt();
        // these next two lines are for reference when building the keep-the-same alternative, where age is non-zero.
        // No change alternative implies that the space is one year older. Therefore, adjust the the rent and the maintenance cost.
        double rent = ZoningRulesI.land.getPrice(dt.getSpaceTypeID(), ZoningRulesI.currentYear,
                ZoningRulesI.baseYear) * dt.getRentDiscountFactor(age);
        double cost = dt.getAdjustedMaintenanceCost(age);
        return rent - cost;
    }

    private double getUtilityPerUnitLand()
    {
        // no landPrep and no demolishing cost.
        return 0;
    }

    @Override
    public void doDevelopment()
    {
        // DO absolutely nothing here;
    }

    @Override
    public Vector getExpectedTargetValues(List<ExpectedValue> ts) throws NoAlternativeAvailable,
            ChoiceModelOverflowException
    {
        // Never any development in the no-change option.
        return new DenseVector(ts.size());
    }

    @Override
    public Vector getUtilityDerivativesWRTParameters(List<Coefficient> cs)
            throws NoAlternativeAvailable, ChoiceModelOverflowException
    {
        // Derivative wrt no-change constant is 1, all others are 0.
        Vector derivatives = new DenseVector(cs.size());
        Coefficient noChangeConst = getTransitionConstant();
        int index = cs.indexOf(noChangeConst);
        if (index >= 0) derivatives.set(index, 1);
        return derivatives;
    }

    @Override
    public Matrix getExpectedTargetDerivativesWRTParameters(List<ExpectedValue> ts,
            List<Coefficient> cs) throws NoAlternativeAvailable, ChoiceModelOverflowException
    {
        // Never any development in the no-change option - d0/dc=0.
        return new DenseMatrix(ts.size(), cs.size());
    }

    private Coefficient getTransitionConstant()
    {
        int spacetype = ZoningRulesI.land.getCoverage();
        return SpaceTypeCoefficient.getNoChangeConst(spacetype);
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