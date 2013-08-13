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
import com.hbaspecto.pecas.land.LandInventory;
import com.hbaspecto.pecas.land.ParcelInterface;
import com.hbaspecto.pecas.land.LandInventory.NotSplittableException;
import com.hbaspecto.pecas.sd.estimation.DemolitionTarget;
import com.hbaspecto.pecas.sd.estimation.ExpectedValue;
import com.hbaspecto.pecas.sd.estimation.SpaceTypeCoefficient;
import com.hbaspecto.pecas.sd.estimation.TransitionConstant;
import com.hbaspecto.pecas.sd.orm.*;


public class DevelopNewAlternative extends DevelopmentAlternative {

    private int numRanges = 2;
    
    static Logger logger = Logger.getLogger(DevelopNewAlternative.class);
    
    private final ZoningRulesI scheme;
    final SpaceTypesI theNewSpaceTypeToBeBuilt;
    final ZoningPermissions zoningReg;
    double sizeTerm; // size term for change alternatives.
    
    // Parameters stored as coefficients.
    private Coefficient dispersionCoeff;
    private Coefficient stepPointCoeff;
    private Coefficient aboveStepPointCoeff;
    private Coefficient belowStepPointCoeff;
    private Coefficient stepPointAmountCoeff;
    private Coefficient newToTransitionCoeff;
    
    private Coefficient transitionCoeff;
    
    // Variables that store parameters for the DevelopmentAlternatives utility methods.
    private double dispersion;
    private double landArea;
    private double existingQuantity;
    private int existingSpaceType;
    private double utilityPerSpace;
    private double utilityPerLand;
    private double[] intensityPoints = new double[numRanges + 1];
    private double[] perSpaceAdjustments = new double[numRanges];
    private double[] perLandAdjustments = new double[numRanges];
    
    private double transitionConstant;
    private double newToTransitionConstant;
    
    private boolean caching = false;
    
    public DevelopNewAlternative(ZoningRulesI scheme, SpaceTypesI dt) {
        this.scheme = scheme;
        
        this.theNewSpaceTypeToBeBuilt = dt;
        zoningReg = this.scheme.getZoningForSpaceType(dt);
        
        int newDT = theNewSpaceTypeToBeBuilt.get_SpaceTypeId();
        dispersionCoeff = SpaceTypeCoefficient.getIntensityDisp(newDT);
        stepPointCoeff = SpaceTypeCoefficient.getStepPoint(newDT);
        aboveStepPointCoeff = SpaceTypeCoefficient.getAboveStepPointAdj(newDT);
        belowStepPointCoeff = SpaceTypeCoefficient.getBelowStepPointAdj(newDT);
        stepPointAmountCoeff = SpaceTypeCoefficient.getStepPointAmount(newDT);
        newToTransitionCoeff = SpaceTypeCoefficient.getNewToTransitionConst(newDT);
    }
    
    private boolean setUpParameters() {
        
        landArea = ZoningRulesI.land.getLandArea();
        existingQuantity = ZoningRulesI.land.getQuantity();
        existingSpaceType = ZoningRulesI.land.getCoverage();
        dispersion = dispersionCoeff.getValue();
        double minFAR = Math.max(theNewSpaceTypeToBeBuilt.get_MinIntensity(), zoningReg.get_MinIntensityPermitted());
        double maxFAR = Math.min(theNewSpaceTypeToBeBuilt.get_MaxIntensity(), zoningReg.get_MaxIntensityPermitted());
        
        // Can't build if there is no allowed range.
        if(minFAR >= maxFAR) return false;
        
        SSessionJdbc tempSession = scheme.land.getSession();
        long costScheduleID = ZoningRulesI.land.get_CostScheduleId();
        TransitionCostCodes costCodes = tempSession.mustFind(TransitionCostCodes.meta, costScheduleID);
        TransitionCosts transitionCost = tempSession.mustFind(TransitionCosts.meta, costScheduleID, theNewSpaceTypeToBeBuilt.get_SpaceTypeId());
        DevelopmentFees df = tempSession.mustFind(DevelopmentFees.meta, ZoningRulesI.land.get_FeeScheduleId(), theNewSpaceTypeToBeBuilt.get_SpaceTypeId());
        
        utilityPerSpace = getUtilityPerUnitSpace(transitionCost, df);
        utilityPerLand = getUtilityPerUnitLand(costCodes, transitionCost, df);

        intensityPoints[0] = minFAR;
        intensityPoints[2] = maxFAR;
        intensityPoints[1] = stepPointCoeff.getValue();
        double a = ZoningRulesI.amortizationFactor;
        perSpaceAdjustments[0] = belowStepPointCoeff.getValue() * a;
        perSpaceAdjustments[1] = aboveStepPointCoeff.getValue() * a;
        perLandAdjustments[0] = 0;
        perLandAdjustments[1] = stepPointAmountCoeff.getValue() * a;
        
        transitionCoeff = TransitionConstant.getCoeff(ZoningRulesI.land.getCoverage(), theNewSpaceTypeToBeBuilt.get_SpaceTypeId());
        transitionConstant = transitionCoeff.getValue();
        
        newToTransitionConstant = newToTransitionCoeff.getValue();
        
        return true;
    }
    
    private double lastUtility;
    private boolean utilityCached = false;
    
    /* (non-Javadoc)
     * @see com.hbaspecto.pecas.Alternative#getUtility(double)
     */
    public double getUtility(double higherLevelDispersionParameter) throws ChoiceModelOverflowException {
        // with continuous intensity options, this is V(tilde)(h)/l in equation 90
    	// note NOT v(tilde)(h) but divided by land size PLUS
    	// the transition constant
        if(utilityCached)
            return lastUtility;
    	boolean canBuild = setUpParameters();
    	
    	if(!canBuild)
    	    return Double.NEGATIVE_INFINITY;
    	
        double result = getCompositeUtility(dispersion, landArea, utilityPerSpace, utilityPerLand,
                intensityPoints, perSpaceAdjustments, perLandAdjustments);

        if (Double.isNaN(result)) {
            // oh oh
            double maximumFAR= zoningReg.get_MaxIntensityPermitted();
            double minimumFAR= zoningReg.get_MinIntensityPermitted();
            logger.error("NAN utility for DevelopmentAlternative"+this);
            logger.error("Trhjp (utility per unit land)= "+utilityPerLand+"; Thjp="+utilityPerSpace+"; "+minimumFAR+"<FAR<"+maximumFAR+" landsize="+landArea);
            throw new ChoiceModelOverflowException("NAN utility for DevelopmentAlternative Trhjp (utility per unit land)= "+utilityPerLand+"; Thjp="+utilityPerSpace+"; "+minimumFAR+"<FAR<"+maximumFAR+" landsize="+landArea);
        }
        result += transitionConstant; // the from-to transition constant in the transitionconstantsi table
        //This is temporary. We want to adjust the costs, not the utility.
        result += theNewSpaceTypeToBeBuilt.getUtilityConstructionAdjustment();
        result += newToTransitionConstant; //the transition constant for all new construction of this type; 
        if(caching)
            utilityCached = true;
        lastUtility = result;
        return lastUtility;
        
    }

	double getUtilityPerUnitSpace(TransitionCosts transitionCost, DevelopmentFees df) {

    	    	
        double constCostPerUnitSpace = transitionCost.get_ConstructionCost();
        if (zoningReg.get_AcknowledgedUse()) constCostPerUnitSpace += zoningReg.get_PenaltyAcknowledgedSpace();
        
        //FIXME: CosnstructionCosts should be adjusted here.
        //FIXME: if CapacityConstraint is ON: AdjTrCost = TrCost * theNewSpaceTypeToBeBuilt.get_CostAdjustmentFactor();
        
     
        constCostPerUnitSpace += df.get_DevelopmentFeePerUnitSpaceInitial();
        double annualCost = constCostPerUnitSpace * ZoningRulesI.amortizationFactor;
        
        // add in ongoing costs
        annualCost += df.get_DevelopmentFeePerUnitSpaceOngoing();
        
        int age = 0; //scheme.currentYear - scheme.land.getYearBuilt();
        
        annualCost += theNewSpaceTypeToBeBuilt.getAdjustedMaintenanceCost(age);
    	//
        theNewSpaceTypeToBeBuilt.cumulativeCostForDevelopNew += annualCost;
    	theNewSpaceTypeToBeBuilt.numberOfParcelsConsideredForDevelopNew++;
  	 
        // TODO store baseyear somewhere, don't assume 1990
        double rent = ZoningRulesI.land.getPrice(theNewSpaceTypeToBeBuilt.getSpaceTypeID(), ZoningRulesI.currentYear, ZoningRulesI.baseYear);
         
         rent *= theNewSpaceTypeToBeBuilt.getRentDiscountFactor(age);
         
        
        return rent - annualCost;
    	
    }

    double getUtilityPerUnitLand(TransitionCostCodes costCodes, TransitionCosts transitionCost, DevelopmentFees df) {
    	SSessionJdbc tempSession = scheme.land.getSession();
	   	
    	long costScheduleID = ZoningRulesI.land.get_CostScheduleId();
    	
    	String parcelid = ZoningRulesI.land.getParcelId();

    	double cost = 0;
    	// first demolish any existing space whether derelict or not
        int oldSpaceType = ZoningRulesI.land.getCoverage();
        
        if (oldSpaceType != LandInventory.VACANT_ID) {
	        TransitionCosts oldSpaceTypeCosts = tempSession.mustFind(TransitionCosts.meta, costScheduleID, oldSpaceType);
	        cost += oldSpaceTypeCosts.get_DemolitionCost()*ZoningRulesI.land.getQuantity()/ZoningRulesI.land.getLandArea();
        }
    	
        if (ZoningRulesI.land.isBrownfield()) {
        	cost += costCodes.get_BrownFieldCleanupCost();
        } else {
        	cost += costCodes.get_GreenFieldPreparationCost();
        }
    	
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

    	// pay the development fees
    	cost += df.get_DevelopmentFeePerUnitLandInitial();
    	
    	double annualCost = cost*ZoningRulesI.amortizationFactor;
    	
    	// ongoing development fees
    	annualCost += df.get_DevelopmentFeePerUnitLandOngoing();
    	
    	return -annualCost;
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
                //System.out.println("Land Area after carving newBit out: "+ ZoningRulesI.land.getLandArea());
            } catch (NotSplittableException e) {
                logger.fatal("Can't split parcel "+e);
                throw new RuntimeException("Can't split parcel",e);
            }
           
            int servicingNeeded = zoningReg.get_ServicesRequirement();  
            newBit.set_AvailableServicesCode(Math.max(newBit.get_AvailableServicesCode(), servicingNeeded));
            
            double oldDevQuantity = newBit.get_SpaceQuantity();
            
            newBit.set_SpaceQuantity( (sampleIntensity()*newBit.get_LandArea()));
            newBit.set_SpaceTypeId(theNewSpaceTypeToBeBuilt.getSpaceTypeID());         
            newBit.set_YearBuilt(ZoningRulesI.currentYear);
            newBit.set_IsDerelict(false);
            newBit.set_IsBrownfield(false);
          
            //keeps track of the total amount of development for a spacetype
            theNewSpaceTypeToBeBuilt.cumulativeAmountOfDevelopment += newBit.get_SpaceQuantity();
            
            ZoningRulesI.land.getDevelopmentLogger().logDevelopmentWithSplit(ZoningRulesI.land, newBit, oldDevQuantity);            		
        } else {
            double newDevQuantity = sampleIntensity() * size;
            double oldDevQuantity = ZoningRulesI.land.getQuantity();
            int oldDT = ZoningRulesI.land.getCoverage();
            boolean oldIsDerelict = ZoningRulesI.land.isDerelict();
            boolean oldIsBrownfield = ZoningRulesI.land.isBrownfield();
            
            ZoningRulesI.land.putCoverage(theNewSpaceTypeToBeBuilt.getSpaceTypeID());
            ZoningRulesI.land.putQuantity(newDevQuantity);
            ZoningRulesI.land.putDerelict(false);
            ZoningRulesI.land.putBrownfield(false);
            
            int servicing = ZoningRulesI.land.getAvailableServiceCode();
            
            /*
             * float servicingNeeded = (float) (newDevQuantity*dt.getServicingRequirement());
             *    if (servicingNeeded >servicing) ZoningRulesI.land.putServiceLevel(servicingNeeded); 
             */
            int servicingNeeded = zoningReg.get_ServicesRequirement();  
            ZoningRulesI.land.putAvailableServiceCode(Math.max(servicing, servicingNeeded));
   
            int oldYear = ZoningRulesI.land.getYearBuilt();
            ZoningRulesI.land.putYearBuilt(ZoningRulesI.currentYear);
            
            //keeps track of the total amount of development for a spacetype
            theNewSpaceTypeToBeBuilt.cumulativeAmountOfDevelopment += ZoningRulesI.land.getQuantity();
            
            ZoningRulesI.land.getDevelopmentLogger().logDevelopment(ZoningRulesI.land, oldDT, oldDevQuantity,  oldYear, oldIsDerelict, oldIsBrownfield);
        }
    }

    double sampleIntensity() {
        boolean canBuild = setUpParameters();
        
        if(!canBuild)
            return 0;
        
        return sampleIntensityWithinRanges(dispersion, landArea, utilityPerSpace, utilityPerLand,
                intensityPoints, perSpaceAdjustments, perLandAdjustments);
    }

	@Override
    public String toString() {
        return "Development alternative to build "+theNewSpaceTypeToBeBuilt+" in zoning "+this.scheme.toString();
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
        int spacetype = theNewSpaceTypeToBeBuilt.get_SpaceTypeId();
        
        double expectedAddedSpace = 0; // Never any added space in a develop new alternative.
        double expectedNewSpace;
        
        // If no construction is possible, the expected new space is 0.
        if(!canBuild)
            expectedNewSpace = 0;
        else {
            double expectedFAR = getExpectedFAR(dispersion, landArea, utilityPerSpace, utilityPerLand,
                intensityPoints, perSpaceAdjustments, perLandAdjustments);
            expectedNewSpace = expectedFAR * ZoningRulesI.land.getLandArea();
        }

        Vector result = new DenseVector(ts.size());
        
        int i = 0;
        for(ExpectedValue value : ts) {
            result.set(i, value.getModelledTotalNewValueForParcel(spacetype, expectedAddedSpace, expectedNewSpace));
            if(value instanceof DemolitionTarget) {
            	DemolitionTarget demoT = (DemolitionTarget) value;
            	result.add(i, demoT.getModelledDemolishQuantityForParcel(existingSpaceType, existingQuantity));
            }
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
        int newToIndex = cs.indexOf(newToTransitionCoeff);
        if(stepPointIndex >= 0) vector.set(stepPointIndex, results.get(1));
        // These ones have to be multiplied by the amortization factor because of the chain rule.
        if(belowStepPointIndex >= 0) vector.set(belowStepPointIndex, results.get(3) * a);
        if(aboveStepPointIndex >= 0) vector.set(aboveStepPointIndex, results.get(4) * a);
        if(stepPointAmountIndex >= 0) vector.set(stepPointAmountIndex, results.get(6) * a);
        if(dispersionIndex >= 0) vector.set(dispersionIndex, results.get(7));
        if(transitionIndex >= 0) vector.set(transitionIndex, 1);
        if (newToIndex >=0) vector.set(newToIndex,1);
        
        if(caching)
            utilDerivCached = true;
        lastUtilDeriv = vector;
        return lastUtilDeriv.copy();
    }

    @Override
    public Matrix getExpectedTargetDerivativesWRTParameters(List<ExpectedValue> ts,
            List<Coefficient> cs) throws NoAlternativeAvailable, ChoiceModelOverflowException {
        boolean canBuild = setUpParameters();
        int spacetype = theNewSpaceTypeToBeBuilt.get_SpaceTypeId();
        
        // If no construction is possible, the expected new space is always zero, so the overall
        // derivatives are all zero.
        if(!canBuild)
            return new DenseMatrix(ts.size(), cs.size());
        
        double expectedFAR = getExpectedFAR(dispersion, landArea, utilityPerSpace, utilityPerLand,
            intensityPoints, perSpaceAdjustments, perLandAdjustments);
        double expectedAddedSpace = 0; // Never any added space in a develop new alternative.
        double expectedNewSpace = expectedFAR * ZoningRulesI.land.getLandArea();
        
        // Build vector of derivatives of the targets with respect to the expected new space.
        Matrix dTdE = new DenseMatrix(ts.size(), 1);
        int i = 0;
        for(ExpectedValue value : ts) {
            dTdE.set(i, 0, value.getModelledTotalNewDerivativeWRTNewSpace(spacetype, expectedAddedSpace, expectedNewSpace));
            i++;
        }
        
        // Scale by land area because of the chain rule.
        dTdE.scale(ZoningRulesI.land.getLandArea());
        
        Vector results = getExpectedFARDerivativesWRTParameters(dispersion, landArea, utilityPerSpace,
                utilityPerLand, intensityPoints, perSpaceAdjustments, perLandAdjustments);
        
        // Build vector of derivatives of the expected new space with respect to the parameters.
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
        caching = true;
    }

    @Override
    public void endCaching() {
        caching = false;
        utilityCached = false;
        targetCached = false;
        utilDerivCached = false;
    }
}