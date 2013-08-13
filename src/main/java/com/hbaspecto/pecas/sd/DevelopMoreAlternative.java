/*
 * Created on 28-Oct-2005
 *
 * Copyright  2005 HBA Specto Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.hbaspecto.pecas.sd;

import java.util.List;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

import org.apache.log4j.Logger;

import simpleorm.sessionjdbc.SSessionJdbc;

import com.hbaspecto.discreteChoiceModelling.Coefficient;
import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.NoAlternativeAvailable;
import com.hbaspecto.pecas.land.ParcelInterface;
import com.hbaspecto.pecas.land.LandInventory.NotSplittableException;
import com.hbaspecto.pecas.sd.estimation.ExpectedValue;
import com.hbaspecto.pecas.sd.estimation.SpaceTypeCoefficient;
import com.hbaspecto.pecas.sd.orm.DevelopmentFees;
import com.hbaspecto.pecas.sd.orm.TransitionCostCodes;
import com.hbaspecto.pecas.sd.orm.TransitionCosts;

class DevelopMoreAlternative extends DevelopmentAlternative {
    
    private int numRanges = 2;
    
	static Logger logger = Logger.getLogger(DevelopMoreAlternative.class);
	private final ZoningRulesI scheme;
	private SpaceTypesI myDt;
	ZoningPermissions zoningReg;
	
	// Parameters stored as coefficients.
    private Coefficient dispersionCoeff;
    private Coefficient stepPointCoeff;
    private Coefficient aboveStepPointCoeff;
    private Coefficient belowStepPointCoeff;
    private Coefficient stepPointAmountCoeff;
    
    private Coefficient transitionCoeff;
	
	// Variables that store parameters for the DevelopmentAlternatives utility methods.
	private double dispersion;
	private double landArea;
	private double utilityPerSpace;
	private double utilityPerLand;
	private double[] intensityPoints = new double[numRanges + 1];
	private double[] perSpaceAdjustments = new double[numRanges];
	private double[] perLandAdjustments = new double[numRanges];
	
	private boolean caching = false;

	DevelopMoreAlternative(ZoningRulesI scheme) {
		this.scheme = scheme;
	}

	ZoningPermissions getMyZoningReg() {
		return (ZoningPermissions) this.scheme.getZoningForSpaceType(myDt);
	}

	// Returns true if there is actually a possibility of development.
	private boolean setUpParameters() {
	    myDt = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(ZoningRulesI.land.getCoverage());
	    
        int spacetype = myDt.get_SpaceTypeId();
        dispersionCoeff = SpaceTypeCoefficient.getIntensityDisp(spacetype);
        stepPointCoeff = SpaceTypeCoefficient.getStepPoint(spacetype);
        aboveStepPointCoeff = SpaceTypeCoefficient.getAboveStepPointAdj(spacetype);
        belowStepPointCoeff = SpaceTypeCoefficient.getBelowStepPointAdj(spacetype);
        stepPointAmountCoeff = SpaceTypeCoefficient.getStepPointAmount(spacetype);
        transitionCoeff = SpaceTypeCoefficient.getAddTransitionConst(spacetype);
            
	    // Can't adjust the quantity of vacant types of land.
	    if(myDt.isVacant() || ZoningRulesI.land.isDerelict()) return false;
	    
	    zoningReg = this.scheme.checkZoningForSpaceType(myDt);
	    
	    // If the existing spacetype is not allowed anymore, prevent the development.
	    if(zoningReg == null) return false;
	    
	    landArea = ZoningRulesI.land.getLandArea();
        double currentFAR = ZoningRulesI.land.getQuantity() / landArea;
        dispersion = dispersionCoeff.getValue();
        double minFAR = Math.max(myDt.get_MinIntensity(),zoningReg.get_MinIntensityPermitted());
        if (minFAR < currentFAR) minFAR = currentFAR;
        double maxFAR = Math.min(myDt.get_MaxIntensity(),zoningReg.get_MaxIntensityPermitted());
        
        // Can't build if already at or above the maximum intensity.
        if(currentFAR >= maxFAR) return false;
        
        // Can't build if there is no allowed range.
        if(minFAR >= maxFAR) return false;

        SSessionJdbc tempSession = scheme.land.getSession();        
        long costScheduleID = ZoningRulesI.land.get_CostScheduleId();
        TransitionCostCodes costCodes = tempSession.mustFind(TransitionCostCodes.meta, costScheduleID);
        TransitionCosts transitionCost = tempSession.mustFind(TransitionCosts.meta, costScheduleID, myDt.get_SpaceTypeId() );
        DevelopmentFees df = tempSession.mustFind(DevelopmentFees.meta, ZoningRulesI.land.get_FeeScheduleId(), myDt.get_SpaceTypeId() );
        double rent = ZoningRulesI.land.getPrice(myDt.getSpaceTypeID(), ZoningRulesI.currentYear, ZoningRulesI.baseYear);

        utilityPerSpace = getUtilityPerUnitNewSpace(transitionCost, df);
        utilityPerLand = getUtilityPerUnitLand(costCodes, transitionCost, df);
        double perSpaceExisting = getUtilityPerUnitExistingSpace(rent);
        
        // Adjust utility per land to account for existing development.
        // FIXME problem here, relying on substantial numerical precision if utility of new space is extremely low (e.g. if penaltyAcknlowledged = 1e37).
        utilityPerLand += (perSpaceExisting - utilityPerSpace) * currentFAR;

        intensityPoints[0] = minFAR;
        intensityPoints[2] = maxFAR;
        intensityPoints[1] = stepPointCoeff.getValue();
        double a = ZoningRulesI.amortizationFactor;
        perSpaceAdjustments[0] = belowStepPointCoeff.getValue()*a;
        perSpaceAdjustments[1] = aboveStepPointCoeff.getValue()*a;
        perLandAdjustments[0] = 0;
        perLandAdjustments[1] = stepPointAmountCoeff.getValue()*a;
        
        return true;
	}
	
	private double lastUtility;
	private boolean utilityCached = false;
	
	public double getUtility(double higherLevelDispersionParameter) throws ChoiceModelOverflowException {
	    if(utilityCached)
	        return lastUtility;
	    boolean canBuild = setUpParameters();
	    if(!canBuild)
	        return Double.NEGATIVE_INFINITY;
	    
		double result = getCompositeUtility(dispersion, landArea, utilityPerSpace, utilityPerLand,
		        intensityPoints, perSpaceAdjustments, perLandAdjustments);
        
        if (Double.isNaN(result)) {
            // oh oh
            String msg = "NAN utility for DevelopMoreAlternative Trhjp (utility per unit land)= "
                +utilityPerLand+"; Thjp="+utilityPerSpace+"; "+intensityPoints[0]+"<FAR<"+intensityPoints[1]+" landsize="+landArea;
            logger.error(msg);
            throw new ChoiceModelOverflowException(msg);
        }
        
		// add in alternative specific constant for addition
		result += transitionCoeff.getValue();
		
		//This is temporary. We want to adjust the costs, not the utility.		
		result += myDt.getUtilityConstructionAdjustment();

		if(caching)
		    utilityCached = true;
		lastUtility = result;
		return result; 

	}

	public void doDevelopment() {

		double size = ZoningRulesI.land.getLandArea();
		if (size>ZoningRulesI.land.getMaxParcelSize()) {
			// If development occurs on a parcel that is greater than n acres,
			// split off n acres into a new "pseudo parcel" and add the new pseudo parcel into the database
			int splits = ((int) (size/ZoningRulesI.land.getMaxParcelSize()))+1;
			double parcelSizes = size/splits;
			ParcelInterface newBit;
			try {
				newBit = ZoningRulesI.land.splitParcel(parcelSizes);

			} catch (NotSplittableException e) {
				logger.fatal("Can't split parcel "+e);
				throw new RuntimeException("Can't split parcel",e);
			}
			double oldDevQuantity = newBit.get_SpaceQuantity();
			if (zoningReg != null) {
				if (Math.min(myDt.get_MaxIntensity(),zoningReg.get_MaxIntensityPermitted()) * ZoningRulesI.land.getLandArea() > oldDevQuantity) {
					newBit.set_SpaceQuantity(sampleIntensity()*newBit.get_LandArea());
				}
			}
			newBit.set_SpaceTypeId(myDt.getSpaceTypeID());

			int servicingNeeded = zoningReg.get_ServicesRequirement();  
			newBit.set_AvailableServicesCode(Math.max(newBit.get_AvailableServicesCode(), servicingNeeded));

			int oldYear = newBit.get_YearBuilt(); 
			newBit.set_YearBuilt((oldYear + ZoningRulesI.currentYear)/2);

			//keeps track of the total amount of development added for a spacetype.
			myDt.cumulativeAmountOfDevelopment += newBit.get_SpaceQuantity()- oldDevQuantity;
			ZoningRulesI.land.getDevelopmentLogger().logAdditionWithSplit(ZoningRulesI.land, newBit, oldDevQuantity);
		} else {

			double oldDevQuantity = ZoningRulesI.land.getQuantity();
			double newDevQuantity = oldDevQuantity;
			if (zoningReg != null) {
				if (Math.min(zoningReg.get_MaxIntensityPermitted(),myDt.get_MaxIntensity()) * ZoningRulesI.land.getLandArea() > oldDevQuantity) {
					newDevQuantity = (float) sampleIntensity()*ZoningRulesI.land.getLandArea();
				}
			}

			ZoningRulesI.land.putQuantity(newDevQuantity);
			int servicing = ZoningRulesI.land.getAvailableServiceCode();
			int servicingNeeded = zoningReg.get_ServicesRequirement();
			if (servicingNeeded > servicing) ZoningRulesI.land.putAvailableServiceCode(servicingNeeded); 

			int oldYear = ZoningRulesI.land.getYearBuilt();

			int newYear = (int) ((oldYear + ZoningRulesI.currentYear)/2);
			ZoningRulesI.land.putYearBuilt(newYear);

			//keeps track of the total amount of development added for a spacetype. 
			myDt.cumulativeAmountOfDevelopment += ZoningRulesI.land.getQuantity() - oldDevQuantity;
			ZoningRulesI.land.getDevelopmentLogger().logAddition(ZoningRulesI.land, oldDevQuantity, oldYear);
		}
	}


	private double getUtilityPerUnitExistingSpace(double rent) {

		if (myDt.isVacant() || ZoningRulesI.land.isDerelict()) return 0;

		int age = ZoningRulesI.currentYear - ZoningRulesI.land.getYearBuilt();
		// these next two lines are for reference when building the keep-the-same alternative, where age is non-zero.
		// No change alternative implies that the space is one year older. Therefore, adjust the the rent and the maintenance cost. 
		rent *= myDt.getRentDiscountFactor(age);        

		double cost = myDt.getAdjustedMaintenanceCost(age);
		
		return rent - cost;        
	}

	private double getUtilityPerUnitNewSpace(TransitionCosts transitionCost, DevelopmentFees df) {            
		double rent = ZoningRulesI.land.getPrice(myDt.getSpaceTypeID(), ZoningRulesI.currentYear, ZoningRulesI.baseYear);        
		return getUtilityPerUnitNewSpace(transitionCost, df, rent);
	}

	private double getUtilityPerUnitNewSpace(TransitionCosts transitionCost, DevelopmentFees df, double rent) {

		//SpaceTypesI myDt = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(ZoningRulesI.land.getCoverage());

		//TODO adjust constructoin cost for density shaping functions
		//FIXME: CosnstructionCosts should be adjusted here for capacityConstraint (refer to JEA).
		double constCostPerUnitSpace = transitionCost.get_AdditionCost();    
		if (zoningReg.get_AcknowledgedUse()) constCostPerUnitSpace += zoningReg.get_PenaltyAcknowledgedSpace();

		constCostPerUnitSpace += df.get_DevelopmentFeePerUnitSpaceInitial();
		double annualCost = constCostPerUnitSpace * ZoningRulesI.amortizationFactor;

		// add in ongoing costs
		annualCost += df.get_DevelopmentFeePerUnitSpaceOngoing();

		int age = 0;

		annualCost += myDt.getAdjustedMaintenanceCost(age);
		
		//Update the variables of CapacityConstrained feature 
		myDt.cumulativeCostForAdd += annualCost;  
		myDt.numberOfParcelsConsideredForAdd++;
		
		rent *= myDt.getRentDiscountFactor(age);

		return rent - annualCost;
	}

	private double getUtilityPerUnitLand(TransitionCostCodes costCodes, TransitionCosts transitionCost, DevelopmentFees df) {

		double cost = 0;

		// we decided that for "add" the the development fees were already paid (when it was "new") so we don't add them in again.
		/*
    	if (ZoningRulesI.land.isBrownfield()) {
        	cost += costCodes.get_BrownFieldCleanupCost();
        } else {
        	cost += costCodes.get_GreenFieldPreparationCost();
        }
		 */

		// check to see if servicing is required
		int servicingRequired = zoningReg.get_ServicesRequirement();
		if (servicingRequired > ZoningRulesI.land.getAvailableServiceCode()) {
			// ENHANCEMENT don't hard code the two servicing code integer interpretations
			// ENHANCEMENT put future servicing xref into xref table instead of inparcel table.
			if (servicingRequired == 1) {
				cost += costCodes.get_LowCapacityServicesInstallationCost();
			} else {
				// assume servicingRequired == 2
				cost += costCodes.get_HighCapacityServicesInstallationCost();
			}
		}

		// we decided that for "add" the the development fees were already paid (when it was "new") so we don't add them in again.
		/*
    	 DevelopmentFees df = tempSession.mustFind(DevelopmentFees.meta, ZoningRulesI.land.get_FeeScheduleId(), spaceType.get_SpaceTypeId());
    	 cost += df.get_DevelopmentFeePerUnitLandInitial();
		 */
		double annualCost = cost*ZoningRulesI.amortizationFactor;

		// ongoing development fees, again these were assessed when the development was new, so don't assess them again.
		//annualCost += df.get_DevelopmentFeePerUnitLandOngoing();

		return -annualCost;    
	}


	private double sampleIntensity() {
		boolean canBuild = setUpParameters();
		
		// If no construction is possible, the new intensity must equal the old intensity.
		if(!canBuild)
		    return ZoningRulesI.land.getQuantity() / ZoningRulesI.land.getLandArea();
		
		return sampleIntensityWithinRanges(dispersion, landArea, utilityPerSpace, utilityPerLand,
		        intensityPoints, perSpaceAdjustments, perLandAdjustments);
	}


	public ZoningRulesI getScheme() {
		return scheme;
	}

	private Vector lastTarget;
	private boolean targetCached = false;
	
    @Override
    public Vector getExpectedTargetValues(List<ExpectedValue> ts) throws NoAlternativeAvailable,
            ChoiceModelOverflowException {
        if(targetCached)
            return lastTarget.copy();
        boolean canBuild = setUpParameters();
        int spacetype = myDt.get_SpaceTypeId();
        
        double expectedAddedSpace;
        double expectedNewSpace = 0; // Never any new space in a develop more alternative.
        
        // If no construction is possible, the expected added space is 0.
        if(!canBuild)
            expectedAddedSpace = 0;
        else {
            double expectedFAR = getExpectedFAR(dispersion, landArea, utilityPerSpace, utilityPerLand,
                intensityPoints, perSpaceAdjustments, perLandAdjustments);
            expectedAddedSpace = expectedFAR * ZoningRulesI.land.getLandArea();
        }

        Vector result = new DenseVector(ts.size());
        
        int i = 0;
        for(ExpectedValue value : ts) {
            result.set(i, value.getModelledTotalNewValueForParcel(spacetype, expectedAddedSpace, expectedNewSpace));
            i++;
        }
        if(caching)
            targetCached = true;
        lastTarget = result;
        return lastTarget.copy();
    }

    private Vector lastUtilDeriv;
    private boolean utilDerivCached = false;
    
    @Override
    public Vector getUtilityDerivativesWRTParameters(List<Coefficient> cs)
            throws NoAlternativeAvailable, ChoiceModelOverflowException {
        if(utilDerivCached)
            return lastUtilDeriv.copy();
        boolean canBuild = setUpParameters();
        
        // If no construction is possible, the utility is always negative infinity. Just return all zeroes.
        if(!canBuild)
            return new DenseVector(cs.size());
        
        Vector results = getUtilityDerivativesWRTParameters(dispersion, landArea, utilityPerSpace,
                utilityPerLand, intensityPoints, perSpaceAdjustments, perLandAdjustments);
        
        // Pack the results into the output vector.
        Vector vector = new DenseVector(cs.size());
        double a = ZoningRulesI.amortizationFactor;
        int stepPointIndex = cs.indexOf(stepPointCoeff);
        int belowStepPointIndex = cs.indexOf(belowStepPointCoeff);
        int aboveStepPointIndex = cs.indexOf(aboveStepPointCoeff);
        int stepPointAmountIndex = cs.indexOf(stepPointAmountCoeff);
        int dispersionIndex = cs.indexOf(dispersionCoeff);
        int transitionIndex = cs.indexOf(transitionCoeff);
        if(stepPointIndex >= 0) vector.set(stepPointIndex, results.get(1));
        // These ones have to be multiplied by the amortization factor because of the chain rule.
        if(belowStepPointIndex >= 0) vector.set(belowStepPointIndex, results.get(3) * a);
        if(aboveStepPointIndex >= 0) vector.set(aboveStepPointIndex, results.get(4) * a);
        if(stepPointAmountIndex >= 0) vector.set(stepPointAmountIndex, results.get(6) * a);
        if(dispersionIndex >= 0) vector.set(dispersionIndex, results.get(7));
        if(transitionIndex >= 0) vector.set(transitionIndex, 1);
        
        if(caching)
            utilDerivCached = true;
        lastUtilDeriv = vector;
        return lastUtilDeriv.copy();
    }

    @Override
    public Matrix getExpectedTargetDerivativesWRTParameters(List<ExpectedValue> ts,
            List<Coefficient> cs) throws NoAlternativeAvailable, ChoiceModelOverflowException {
        
        boolean canBuild = setUpParameters();
        int spacetype = myDt.get_SpaceTypeId();
        
        // If no construction is possible, the expected added space is always zero, so the overall
        // derivatives are all zero.
        if(!canBuild)
            return new DenseMatrix(ts.size(), cs.size());
        
        double expectedFAR = getExpectedFAR(dispersion, landArea, utilityPerSpace, utilityPerLand,
            intensityPoints, perSpaceAdjustments, perLandAdjustments);
        double expectedAddedSpace = expectedFAR * ZoningRulesI.land.getLandArea();
        double expectedNewSpace = 0; // Never any new space in a develop more alternative.
        
        // Build vector of derivatives of the targets with respect to the expected added space.
        Matrix dTdE = new DenseMatrix(ts.size(), 1);
        int i = 0;
        for(ExpectedValue value : ts) {
            dTdE.set(i, 0, value.getModelledTotalNewDerivativeWRTAddedSpace(spacetype, expectedAddedSpace, expectedNewSpace));
            i++;
        }
        
        // Scale by land area because of the chain rule.
        dTdE.scale(ZoningRulesI.land.getLandArea());
        
        Vector results = getExpectedFARDerivativesWRTParameters(dispersion, landArea, utilityPerSpace,
                utilityPerLand, intensityPoints, perSpaceAdjustments, perLandAdjustments);
        
        // Build vector of derivatives of the expected added space with respect to the parameters.
        Matrix dEdt = new DenseMatrix(1, cs.size());
        double a = ZoningRulesI.amortizationFactor;
        int stepPointIndex = cs.indexOf(stepPointCoeff);
        int belowStepPointIndex = cs.indexOf(belowStepPointCoeff);
        int aboveStepPointIndex = cs.indexOf(aboveStepPointCoeff);
        int stepPointAmountIndex = cs.indexOf(stepPointAmountCoeff);
        int dispersionIndex = cs.indexOf(dispersionCoeff);
        if(stepPointIndex >= 0) dEdt.set(0, stepPointIndex, results.get(1));
        // These ones have to be multiplied by the amortization factor because of the chain rule.
        if(belowStepPointIndex >= 0) dEdt.set(0, belowStepPointIndex, results.get(3) * a);
        if(aboveStepPointIndex >= 0) dEdt.set(0, aboveStepPointIndex, results.get(4) * a);
        if(stepPointAmountIndex >= 0) dEdt.set(0, stepPointAmountIndex, results.get(6) * a);
        if(dispersionIndex >= 0) dEdt.set(0, dispersionIndex, results.get(7));
        
        Matrix answer = new DenseMatrix(ts.size(), cs.size());
        answer = dTdE.mult(dEdt, answer);
        
        return answer;
    }

    @Override
    public void startCaching() {
        caching = false;
    }

    @Override
    public void endCaching() {
        caching = false;
        utilityCached = false;
        targetCached = false;
        utilDerivCached = false;
    }
}