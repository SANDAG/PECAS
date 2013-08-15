package com.hbaspecto.pecas.sd.estimation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import simpleorm.sessionjdbc.SSessionJdbc;
import com.hbaspecto.pecas.land.LandInventory;
import com.hbaspecto.pecas.land.Tazs;
import com.hbaspecto.pecas.sd.SpaceTypesI;
import com.hbaspecto.pecas.sd.ZoningPermissions;
import com.hbaspecto.pecas.sd.ZoningRulesI;
import com.hbaspecto.pecas.sd.orm.DevelopmentFees;
import com.hbaspecto.pecas.sd.orm.ObservedDevelopmentEvents;
import com.hbaspecto.pecas.sd.orm.ObservedDevelopmentEvents_gen;
import com.hbaspecto.pecas.sd.orm.TransitionCostCodes;
import com.hbaspecto.pecas.sd.orm.TransitionCosts;
import com.pb.common.datafile.GeneralDecimalFormat;

public class EstimationDataSet {
	//private static  ArrayList estimationRows = new ArrayList();
	private ArrayList estimationRow = null;
	private double[] sampledQntys;  
	private final int NUM_SAMPLES = 5;
	private final double SAMPLE_RATIO;
	private static BufferedWriter estimationBuffer;
	private String fileNameAndPath;
	private NumberFormat nf = new GeneralDecimalFormat("#.####E0",1E7,1E-2);

	public EstimationDataSet(String fileNameAndPath, double sampleRatio){
		this.fileNameAndPath = fileNameAndPath;
		estimationBuffer = getBufferWriter();	
		SAMPLE_RATIO = sampleRatio;
		sampledQntys = new double[NUM_SAMPLES];
	}
	
	private void updateTheChoiceCode( int newChoiceCode, int seqChoiceCode ){
		estimationRow.set(3, new Integer(newChoiceCode));
		estimationRow.set(14, new Integer(seqChoiceCode));
	}
	
	private BufferedWriter getBufferWriter(){
		if (estimationBuffer != null) return estimationBuffer;
		try {
			estimationBuffer = new BufferedWriter(new FileWriter(fileNameAndPath));
			return estimationBuffer;
		} catch (Exception ex){
			throw new RuntimeException("Can't open estimation file", ex);
		}
	}

	public void writeEstimationRow(){
		if (estimationRow ==null) return;
		StringBuffer row= new StringBuffer("");
		for (int i = 0; i<estimationRow.size()-1; i++) {
			Object o = estimationRow.get(i);
			if (o instanceof Double || o instanceof Float) {
				row.append(nf.format(o));
			} else {
				row.append(o.toString());
			}
			if (i != estimationRow.size()-1) row.append(",");
		}
		row.append("\n");

		BufferedWriter estimationBuffer = getBufferWriter();
		try {
			estimationBuffer.write(row.toString());	
		} catch (Exception ex){
			throw new RuntimeException("Can't write to estimation file.");
		}


	}
	public void compileEstimationRow(LandInventory l){
		
		SSessionJdbc tempSession = SSessionJdbc.getThreadLocalSession();
		if (!tempSession.hasBegun()) tempSession.begin();
		int choice = 1; //assume no_change by default
		double observationWeight = 1;
		
		ObservedDevelopmentEvents devEvn = tempSession.find(ObservedDevelopmentEvents_gen.meta, l.getPECASParcelNumber());
		if (devEvn == null){
			observationWeight = 1/SAMPLE_RATIO;
			if (Math.random() >= SAMPLE_RATIO) {estimationRow = null; return ;}
		} else {
			if (devEvn.get_Eventtype().trim().equals("D")) choice=2;
			if (devEvn.get_Eventtype().trim().equals("L")) choice=3;
			if (devEvn.get_Eventtype().trim().equals("R")) choice=4;
		}
		
		

		estimationRow = new ArrayList();
		ZoningRulesI.land = l;
		addBaseInfo(l, choice, observationWeight);
		addDoNothingAlternativeInfo(l);
		addDemolishAlternativeInfo(l);
		addDerelictAlternativeInfo(l);
		addRenovateAlternativeInfo(l);
		for (int i=0; i<NUM_SAMPLES; i++){
			addAddMoreAlternativeInfo(l, i,NUM_SAMPLES);	
		}
		if (devEvn!=null) {
			if (devEvn.get_Eventtype().trim().equals("A")) {
				choice = findClosestSample(devEvn.get_NewSpaceQuantity());
				//// FIXME: there should be a better system for finding the choice code
				updateTheChoiceCode(choice+4, choice+4);
			}
		}
		List<Integer> sortedSpaceTypesIds = SpaceTypesI.getAllSpaceTypesIDs();
		Collections.sort(sortedSpaceTypesIds);
		
		
		for(Integer newSpaceTypeID : sortedSpaceTypesIds){
			SpaceTypesI newST = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(newSpaceTypeID.intValue());
			boolean storeQntySamples = false;
			if (!newST.isVacant()){
				if(devEvn!=null){
					if (devEvn.get_NewSpaceIypeId() == newST.get_SpaceTypeId()) storeQntySamples=true;					
				}
				for (int i=0; i<NUM_SAMPLES; i++){					
					addDevelopNewAlternativeInfo(l, newSpaceTypeID, i, NUM_SAMPLES, storeQntySamples);	
				}
			}
		}
		if (devEvn!=null) {
			if (devEvn.get_Eventtype().trim().equals("C")) {
				// sampled quantities should be saved only for the new space type chosed.
				choice = findClosestSample(devEvn.get_NewSpaceQuantity());
				// FIXME: there should be a better system for finding the choice code  
				int seqChoiceCode = (devEvn.get_NewSpaceIypeId() * 5) + 4 + choice;
				int choiceCode = (devEvn.get_NewSpaceIypeId() * 100) + choice;
				updateTheChoiceCode(choiceCode, seqChoiceCode);
			}
		}
		
	}
	
	private int findClosestSample(double newSpaceQuantity) {
		int theSample=0;
		double min = Math.abs(newSpaceQuantity-sampledQntys[0]);
		for (int i=1; i < sampledQntys.length; i++){
			if (min> Math.abs(newSpaceQuantity-sampledQntys[i])){
				min = Math.abs(newSpaceQuantity-sampledQntys[i]);
				theSample=i;
			}
		}
		return theSample+1;
	}

	public void close(){
		try{
			estimationBuffer.close();
		} catch(Exception ex){
			throw new RuntimeException("Couldn't close estimation file", ex);
		}
	}

	private void addBaseInfo(LandInventory l, int choice, double observationWeight){

		int luz = Tazs.getTazRecord(l.getTaz()).get_LuzNumber();
		int year = ZoningRulesI.currentYear;

		estimationRow.add(new Long( l.getPECASParcelNumber()));
		estimationRow.add(new String( l.getParcelId()));
		estimationRow.add(new Integer(year)); //year
		estimationRow.add(new Integer(choice)); // Choice. From permit data; default 1. Index=3
		estimationRow.add(new Integer(luz)); //LUZ
		estimationRow.add(l.getTaz());
		estimationRow.add(new Integer(-1)); // Jurisdiction
		estimationRow.add(new Integer(l.getCoverage()));
		estimationRow.add(new Double(l.getQuantity()));
		int isDer = 0;
		if (l.isDerelict()) isDer=1;
		estimationRow.add(new Integer(isDer));
		int isBrown=0;
		if (l.isBrownfield()) isBrown=1;
		estimationRow.add(new Integer(isBrown));
		estimationRow.add(new Double(l.getLandArea()));
		int buildingAge = year - l.getYearBuilt();
		estimationRow.add(new Integer(buildingAge)); // Age of exiting space
		estimationRow.add(new Double(observationWeight)); // Observation weight; default 1
		estimationRow.add(new Integer(choice)); // Sequential choice code.
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
	}

	private void addDoNothingAlternativeInfo(LandInventory l){
		estimationRow.add(new Integer(l.getCoverage()));
		estimationRow.add(new Integer(1)); // Alternative permissible (Zoning)
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Double(l.getQuantity()));
		estimationRow.add(new Double( l.getQuantity()/l.getLandArea())); // existing FAR
		double rent=0;
		double cost=0;
		SpaceTypesI dt = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(l.getCoverage());
		if (!dt.isVacant() && !l.isDerelict()){

			int age = ZoningRulesI.currentYear - l.getYearBuilt();
			// these next two lines are for reference when building the keep-the-same alternative, where age is non-zero.
			// No change alternative implies that the space is one year older. Therefore, adjust the the rent and the maintenance cost. 
			rent = l.getPrice(dt.getSpaceTypeID(), ZoningRulesI.currentYear, ZoningRulesI.baseYear)*dt.getRentDiscountFactor(age);        
			rent = rent * l.getQuantity()/l.getLandArea();
			cost = dt.getAdjustedMaintenanceCost(age);				
			cost = cost * l.getQuantity()/l.getLandArea();				
		}
		estimationRow.add(new Double(rent));
		estimationRow.add(new Double(cost));
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
	}

	private void addDemolishAlternativeInfo(LandInventory l){

		estimationRow.add(new Integer(LandInventory.VACANT_ID));
		ZoningRulesI zoning = ZoningRulesI.getZoningRuleByZoningRulesCode(l.getSession(), l.getZoningRulesCode());
		SpaceTypesI dt = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(l.getCoverage());

		int isPermissible=1;
		if (!zoning.get_DemolitionPossibilities() || dt.isVacant()) isPermissible=-1; 
		estimationRow.add(new Integer(isPermissible)); // Alternative permissible (Zoning)
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0));  //amount of space
		estimationRow.add(new Integer(0));  // existing FAR
		estimationRow.add(new Integer(0));  // rent
		estimationRow.add(new Integer(0));  // maintenance cost


		double demolitionCost = 0;
		if (!dt.isVacant()) demolitionCost = dt.getDemolitionCost(l.get_CostScheduleId()) * l.getQuantity() / l.getLandArea();

		estimationRow.add(new Double(demolitionCost)); // Demolish cost
		estimationRow.add(new Integer(0)); // ren/add cost
		estimationRow.add(new Integer(0)); // fees
		estimationRow.add(new Integer(0)); // site prep

		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
	}

	private void addDerelictAlternativeInfo(LandInventory l){

		estimationRow.add(new Integer(l.getCoverage()));

		ZoningRulesI zoning = ZoningRulesI.getZoningRuleByZoningRulesCode(l.getSession(), l.getZoningRulesCode());
		SpaceTypesI dt = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(l.getCoverage());
		int isPermissible=1;
		if (!zoning.get_DerelictionPossibilities() || l.isDerelict() || dt.isVacant()) isPermissible=-1; 
		estimationRow.add(new Integer(isPermissible)); // Alternative permissible (Zoning)
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Double(l.getQuantity()));  //amount of space
		estimationRow.add(new Double(l.getQuantity()/l.getLandArea()));  // existing FAR
		estimationRow.add(new Integer(0));  // rent
		estimationRow.add(new Integer(0));  // maintenance cost
		estimationRow.add(new Integer(0));  // Demolish cost
		estimationRow.add(new Integer(0));  // ren/add cost
		estimationRow.add(new Integer(0));  // fees
		estimationRow.add(new Integer(0));  // site prep

		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
	}

	private void addRenovateAlternativeInfo(LandInventory l){

		estimationRow.add(new Integer(l.getCoverage()));

		ZoningRulesI zoning = ZoningRulesI.getZoningRuleByZoningRulesCode(l.getSession(), l.getZoningRulesCode());
		SpaceTypesI dt = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(l.getCoverage());
		int isPermissible=1;
		if (!zoning.get_RenovationPossibilities() || dt.isVacant()) isPermissible=-1; 
		estimationRow.add(new Integer(isPermissible)); // Alternative permissible (Zoning)
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Double(l.getQuantity()));  //amount of space
		estimationRow.add(new Double(l.getQuantity()/l.getLandArea()));  // existing FAR

		double rent = 0;
		double cost = 0;
		double maintenanceCost = 0;
		if (!dt.isVacant()){

			int age = 0;
			rent = l.getPrice(dt.getSpaceTypeID(), ZoningRulesI.currentYear, ZoningRulesI.baseYear) * dt.getRentDiscountFactor(age);        				      
			rent = rent * l.getQuantity()/l.getLandArea();

			maintenanceCost = dt.getAdjustedMaintenanceCost(age);
			maintenanceCost = maintenanceCost * l.getQuantity()/l.getLandArea();

			if (l.isDerelict()){
				cost = dt.getRenovationDerelictCost(l.get_CostScheduleId()); 
			} else {
				cost = dt.getRenovationCost(l.get_CostScheduleId());   
			}
			cost = cost * l.getQuantity()/l.getLandArea();
		}
		estimationRow.add(new Double(rent));  // rent
		estimationRow.add(new Double(maintenanceCost));  // maintenance cost
		estimationRow.add(new Integer(0));  // Demolish cost
		estimationRow.add(new Double(cost));  // ren/add cost
		estimationRow.add(new Integer(0));  // No fees for renovation. 
		
		double prepCost=0; 
/*		if (l.isBrownfield()){ 
			//Ask John: What if land is not brownfield?? prepCost is 0
			SSessionJdbc tempSession = SSessionJdbc.getThreadLocalSession();
			TransitionCostCodes costCodes = tempSession.mustFind(TransitionCostCodes.meta, l.get_CostScheduleId());
			prepCost = costCodes.get_BrownFieldCleanupCost();
		} */ /*Brownfield cleanup costs are only on new development */

		estimationRow.add(new Double(prepCost));  // site prep

		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
	}

	private void addAddMoreAlternativeInfo(LandInventory l, int sampleNumber, int numberOfSamples){

		estimationRow.add(new Integer(l.getCoverage()));

		ZoningRulesI zoningRules = ZoningRulesI.getZoningRuleByZoningRulesCode(l.getSession(), l.getZoningRulesCode());
		SpaceTypesI dt = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(l.getCoverage());
		int isPermissible=1;

		double currentFAR= l.getQuantity()/l.getLandArea();
		double minimumFAR= currentFAR;
		double maximumFAR= dt.get_MaxIntensity();
		boolean isParcelFull=false;
		ZoningPermissions zoningPermissions = zoningRules.checkZoningForSpaceType(dt);
		if (zoningPermissions != null){
			maximumFAR = Math.min(dt.get_MaxIntensity(), zoningPermissions.get_MaxIntensityPermitted());
			// can't build less than is already there as part of the "Add More" alternative
			minimumFAR = Math.max(dt.get_MinIntensity(),Math.max(currentFAR,zoningPermissions.get_MinIntensityPermitted()));
		}
		if (currentFAR >= maximumFAR) isParcelFull=true;
		if (minimumFAR > maximumFAR) maximumFAR = minimumFAR;
		if (!zoningRules.get_AdditionPossibilities() || dt.isVacant() || l.isDerelict() || isParcelFull) isPermissible=-1; 
		estimationRow.add(new Integer(isPermissible)); // Alternative permissible (Zoning)
		estimationRow.add(new Double(minimumFAR)); // min FAR
		estimationRow.add(new Double(maximumFAR)); // max FAR
		
		// sample added quantity
		double addedQuantity = (((maximumFAR-minimumFAR)/numberOfSamples)*(sampleNumber*2+1)/2.0+minimumFAR)*l.getLandArea()-l.getQuantity();
		
		sampledQntys[sampleNumber] = addedQuantity;
		
		estimationRow.add(new Double(addedQuantity));  //amount of space added
		double newTotalQnty = l.getQuantity()+addedQuantity;
		estimationRow.add(new Double(newTotalQnty/l.getLandArea()));  // FAR after addition

		double  spaceValue =0;
		if (!dt.isVacant() && !l.isDerelict())
			spaceValue = l.getPrice(dt.getSpaceTypeID(), ZoningRulesI.currentYear, ZoningRulesI.baseYear);

		int age = ZoningRulesI.currentYear - l.getYearBuilt();
		double rentOfExistingSpace = spaceValue * dt.getRentDiscountFactor(age) * l.getQuantity()/l.getLandArea();        
		int ageOfNew=0;

		// Note: spaceValue = 0 if the parcel is derelict and/or isVacant 
		double rentOfNew = spaceValue * dt.getRentDiscountFactor(ageOfNew) * addedQuantity/l.getLandArea();
		double rent = rentOfNew + rentOfExistingSpace;


		double maintenanceCost = dt.getAdjustedMaintenanceCost(age) * l.getQuantity()/l.getLandArea() + 
		dt.getAdjustedMaintenanceCost(ageOfNew) * addedQuantity/l.getLandArea();

		SSessionJdbc tempSession = SSessionJdbc.getThreadLocalSession(); 
		if (!tempSession.hasBegun()) tempSession.begin();
		long costScheduleID = l.get_CostScheduleId();
		
		double cost=0; // in case it's vacant, initialize to zero
		double fees=0;
		double sitePrepCost =0;
		if (!dt.isVacant()) {
			TransitionCostCodes costCodes = tempSession.mustFind(TransitionCostCodes.meta, costScheduleID);
			TransitionCosts transitionCost = tempSession.mustFind(TransitionCosts.meta, costScheduleID, dt.get_SpaceTypeId());
	
			double costPerUnitOfSpace = 0;
			costPerUnitOfSpace += transitionCost.get_AdditionCost();    
			if (zoningPermissions!=null) {
				if (zoningPermissions.get_AcknowledgedUse()) costPerUnitOfSpace += zoningPermissions.get_PenaltyAcknowledgedSpace();
			}
	
			cost = costPerUnitOfSpace * addedQuantity/l.getLandArea();
	
			sitePrepCost = 0;
			// check to see if servicing is required
			int servicingRequired = 0;
			if (zoningPermissions !=null) {
				servicingRequired = zoningPermissions.get_ServicesRequirement();
			}
			if (servicingRequired > l.getAvailableServiceCode()) {
				// ENHANCEMENT don't hard code the two servicing code integer interpretations
				// ENHANCEMENT put future servicing xref into xref table instead of inparcel table.
				if (servicingRequired == 1) {
					sitePrepCost += costCodes.get_LowCapacityServicesInstallationCost();
				} else {
					// assume servicingRequired == 2
					sitePrepCost += costCodes.get_HighCapacityServicesInstallationCost();
				}
			}
	
			DevelopmentFees df = tempSession.mustFind(DevelopmentFees.meta, l.get_FeeScheduleId(), dt.get_SpaceTypeId());
			fees = df.get_DevelopmentFeePerUnitSpaceInitial();
			// TODO account for ongoing fees
			//fees += df.get_DevelopmentFeePerUnitSpaceOngoing();
			fees *= addedQuantity/l.getLandArea();
		}
		estimationRow.add(new Double(rent));  // rent
		estimationRow.add(new Double(maintenanceCost));  // maintenance cost
		estimationRow.add(new Integer(0));  // Demolish cost
		estimationRow.add(new Double(cost));  // ren/add cost
		estimationRow.add(new Double(fees));  // fees
		estimationRow.add(new Double(sitePrepCost));  // site prep

		estimationRow.add(new Double(1)); // Sample weight

		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
	}

	private void addDevelopNewAlternativeInfo(LandInventory l, int newSpaceTypeId, int sampleNumber, int numberOfSamples, boolean storeQntySamples){
		estimationRow.add(new Integer(newSpaceTypeId));

		ZoningRulesI zoningRules = ZoningRulesI.getZoningRuleByZoningRulesCode(l.getSession(), l.getZoningRulesCode());
		SpaceTypesI theNewSpaceType = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(newSpaceTypeId);
		int isPermissible=1;


		double minimumFAR= theNewSpaceType.get_MinIntensity();
		double maximumFAR= theNewSpaceType.get_MaxIntensity();

		ZoningPermissions zoningPermissions = zoningRules.checkZoningForSpaceType(theNewSpaceType);
		if (zoningPermissions != null){
			maximumFAR = Math.min(theNewSpaceType.get_MaxIntensity(), zoningPermissions.get_MaxIntensityPermitted());
			minimumFAR = Math.max(theNewSpaceType.get_MinIntensity(), zoningPermissions.get_MinIntensityPermitted());
		} else {
			isPermissible = -1;
		}

		if (!zoningRules.get_AdditionPossibilities()) isPermissible=-1; 
		estimationRow.add(new Integer(isPermissible)); // Alternative permissible (Zoning)
		estimationRow.add(new Double(minimumFAR)); // min FAR
		estimationRow.add(new Double(maximumFAR)); // max FAR
		
		// sample amount of space
		double quantity = (((maximumFAR-minimumFAR)/numberOfSamples)*(sampleNumber*2+1)/2.0+minimumFAR)*l.getLandArea();
		if (storeQntySamples) sampledQntys[sampleNumber] = quantity;
		
		estimationRow.add(new Double(quantity));  //amount of space 
		estimationRow.add(new Double(quantity/l.getLandArea()));  // FAR 

		double rent = ZoningRulesI.land.getPrice(theNewSpaceType.getSpaceTypeID(), ZoningRulesI.currentYear, ZoningRulesI.baseYear);
		int age=0; 
		rent *= theNewSpaceType.getRentDiscountFactor(age) * quantity/l.getLandArea();
		double maintenanceCost = theNewSpaceType.getAdjustedMaintenanceCost(age) * quantity/l.getLandArea();

		SSessionJdbc tempSession = SSessionJdbc.getThreadLocalSession(); 	
		if (!tempSession.hasBegun()) tempSession.begin();
		long costScheduleID = l.get_CostScheduleId();
		TransitionCostCodes costCodes = tempSession.mustFind(TransitionCostCodes.meta, costScheduleID);
		TransitionCosts transitionCost = tempSession.mustFind(TransitionCosts.meta, costScheduleID, theNewSpaceType.get_SpaceTypeId());

		double cost = transitionCost.get_ConstructionCost() * quantity/l.getLandArea();
		if (zoningPermissions!=null) {
			if (zoningPermissions.get_AcknowledgedUse()) cost += zoningPermissions.get_PenaltyAcknowledgedSpace();
		}

		DevelopmentFees df = tempSession.mustFind(DevelopmentFees.meta, l.get_FeeScheduleId(), theNewSpaceType.get_SpaceTypeId());

		//TODO: account for ongoing fees too, if they exist 
		double fees=0;
		fees += df.get_DevelopmentFeePerUnitSpaceInitial();// + df.get_DevelopmentFeePerUnitSpaceOngoing();
		fees *= quantity/l.getLandArea();
		fees += df.get_DevelopmentFeePerUnitLandInitial();// + df.get_DevelopmentFeePerUnitLandOngoing();

		double sitePrep=0;
		if (l.isBrownfield()) {
			sitePrep += costCodes.get_BrownFieldCleanupCost();
		} else {
			sitePrep += costCodes.get_GreenFieldPreparationCost();
		}
		int servicingRequired = 0;
		if (zoningPermissions!=null) {
			servicingRequired = zoningPermissions.get_ServicesRequirement();
		}
		if (servicingRequired > l.getAvailableServiceCode()) {
			// ENHANCEMENT don't hard code the two servicing code integer interpretations
			// ENHANCEMENT put future servicing xref into xref table instead of inparcel table.
			if (servicingRequired == 1) {
				sitePrep += costCodes.get_LowCapacityServicesInstallationCost();
			} else {
				// assume servicingRequired == 2
				sitePrep += costCodes.get_HighCapacityServicesInstallationCost();
			}
		}

		double demolishCost=0;
		SpaceTypesI oldSpaceType = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(l.getCoverage());
		if (!oldSpaceType.isVacant()) {
			TransitionCosts oldSpaceTypeCosts = tempSession.mustFind(TransitionCosts.meta, costScheduleID, oldSpaceType.get_SpaceTypeId());
			demolishCost = oldSpaceTypeCosts.get_DemolitionCost()* l.getQuantity()/l.getLandArea();
		}

		estimationRow.add(new Double(rent)); // Rent
		estimationRow.add(new Double(maintenanceCost)); // maintenance cost
		estimationRow.add(new Double(demolishCost));  // demolition cost 
		estimationRow.add(new Double(cost));  // construction. cost 
		estimationRow.add(new Double(fees));  // fees
		estimationRow.add(new Double(sitePrep));  // site Prep

		estimationRow.add(new Double(1));  // sample weight

		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
		estimationRow.add(new Integer(0)); // Blank field
	}
}



