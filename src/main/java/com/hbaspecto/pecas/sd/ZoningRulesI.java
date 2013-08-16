/*
 * Copyright  2007 HBA Specto Incorporated
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

import org.apache.log4j.Logger;

import simpleorm.dataset.SQuery;
import simpleorm.sessionjdbc.SSessionJdbc;

import com.hbaspecto.discreteChoiceModelling.Alternative;
import com.hbaspecto.discreteChoiceModelling.Coefficient;
import com.hbaspecto.discreteChoiceModelling.LogitModel;
import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.NoAlternativeAvailable;
import com.hbaspecto.pecas.land.LandInventory;
import com.hbaspecto.pecas.sd.estimation.EstimationMatrix;
import com.hbaspecto.pecas.sd.estimation.ExpectedValue;
import com.hbaspecto.pecas.sd.estimation.SpaceTypeCoefficient;
import com.hbaspecto.pecas.sd.orm.TransitionCostCodes;
import com.hbaspecto.pecas.sd.orm.ZoningPermissions_gen;
import com.hbaspecto.pecas.sd.orm.ZoningRulesI_gen;

/**
 * A class that represents the regulations that control the DevelopmentTypes
 * that are allowed to occur on a parcel. The regulations are stored in a map
 * (lookup) by development type.
 * 
 * Each ZoningScheme also builds itself a DiscreteChoice model that can be used
 * to monte carlo simulate specific construction actions from within the set of
 * allowable possibilities.
 * 
 * @author John Abraham
 */
public class ZoningRulesI extends ZoningRulesI_gen implements
		ZoningRulesIInterface, java.io.Serializable {

	protected static transient Logger logger = Logger.getLogger("com.pb.osmp.ld");
	public static int currentYear;
	public static int baseYear;
	public static boolean ignoreErrors;
	public static final int maxZoningSchemeIndex = 32767;
	// TODO servicing cost should be a zonal variable?
	static double servicingCostPerUnit = 13.76;

	/**
	 * This is the storage for the zoning regulations for the zoning scheme. Each
	 * ZoningRegulation describes what is allowed (and hence possible) for a
	 * particular DevelopmentType.
	 */
	private List<ZoningPermissions> zoning;

	// tree of options which includes the options in zoning_permissions table
	LogitModel myLogitModel = null;

	// protected static Hashtable allZoningSchemes = new Hashtable();
	protected static ZoningRulesI[] allZoningSchemesIndexArray = new ZoningRulesI[maxZoningSchemeIndex];

	static double amortizationFactor = 1.0 / 30;
	public static LandInventory land = null;

	// these three variables are used in the alternative classes in the logit
	// model,
	// so they need to be set before the logitmodel is used.

	// this is just a temporary place to store the current parcel's development
	// type,
	// TODO should be removed from this class as it's not directly related to
	// zoning.
	SpaceTypesI existingDT;

	// is mapped to zoning_rules_code
	private int gridCode;

	/**
	 * Method getZoningSchemeByIndex.
	 * 
	 * @param i
	 * @return ZoningScheme
	 */
	public static ZoningRulesI getZoningRuleByZoningRulesCode(
			SSessionJdbc session, int zoningRulesCode) {

		final ZoningRulesI zoningScheme = session.find(ZoningRulesI_gen.meta,
				zoningRulesCode);
		return zoningScheme;

	}

	private final Set<SpaceTypeInterface> notAllowedSpaceTypes = new HashSet<SpaceTypeInterface>();
	private LogitModel gkChangeOptions;
	private LogitModel gzRenovateOrGy;
	private LogitModel gyAddOrNewSpace;
	private LogitModel gwDemolishAndDerelict;
	private LogitModel developNewOptions;

	@Override
	public Iterator<ZoningPermissions> getAllowedSpaceTypes() {
		final SSessionJdbc session = land.getSession();

		final SQuery<ZoningPermissions> qryPermissions = new SQuery<ZoningPermissions>(
				ZoningPermissions_gen.meta).eq(ZoningPermissions_gen.ZoningRulesCode,
				get_ZoningRulesCode());

		final List<ZoningPermissions> zoning = session.query(qryPermissions);
		return zoning.iterator();
	}

	public void simulateDevelopmentOnCurrentParcel(LandInventory l,
			boolean ignoreErrors) {

		land = l;
		existingDT = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(land
				.getCoverage());
		if (existingDT == null) {
			logger.fatal("Invalid coverage code " + land.getCoverage() + " at "
					+ l.parcelToString());
			throw new RuntimeException("Invalid coverage code " + land.getCoverage()
					+ " at " + l.parcelToString());
		}
		final boolean doIt = land.isDevelopable();
		if (!doIt) {
			return; // don't do development if it's impossible to develop!
		}

		setDispersionParameters();

		// gridFee = 0.0;
		final LogitModel developChoice = getMyLogitModel();

		developNewOptions.setConstantUtility(existingDT
				.get_NewFromTransitionConst());
		// If we are considering land n acres at a time, then when a parcel is
		// greater than n acres we need to
		// call monteCarloElementalChoice repeatedly. There may also need to be
		// some special treatment for parcels that are
		// much less than n acres.
		//
		final double originalLandArea = land.getLandArea();
		for (int sampleTimes = 0; sampleTimes <= originalLandArea
				/ land.getMaxParcelSize(); sampleTimes++) {
			DevelopmentAlternative a;
			try {
				a = (DevelopmentAlternative) developChoice.monteCarloElementalChoice();
			}
			catch (final NoAlternativeAvailable e) {
				final String msg = "No reasonable development choices available for "
						+ this + " in parcel " + land.getParcelId();
				logger.fatal(msg);
				if (!ignoreErrors) {
					throw new RuntimeException(msg, e);
				}
				else {
					land.getParcelErrorLog().logParcelError(land, e);
					continue;
				}

			}
			catch (final ChoiceModelOverflowException e) {
				final String msg = "Choice model overflow exception for " + this
						+ " in parcel " + land.getParcelId();
				logger.fatal(msg);
				if (!ignoreErrors) {
					throw new RuntimeException(msg, e);
				}
				else {
					land.getParcelErrorLog().logParcelError(land, e);
					continue;
				}
			}
			a.doDevelopment();
			// System.out.println(sampleTimes +", "+ land.getLandArea());
		}
		if (originalLandArea > land.getLandArea()) {
			// if the original parcel area is bigger than the current land area,
			// it means
			// that the parcel was split. Therefore, we need to write out the
			// remaining parcel area.
			ZoningRulesI.land.getDevelopmentLogger().logRemainingOfSplitParcel(land);

		}
	}

	/**
	 * 
	 */
	private void setDispersionParameters() {
		// set dispersion parameters before the tree is built.
		getMyLogitModel().setDispersionParameter(
				existingDT.get_NochangeDispersionParameter());
		gkChangeOptions.setDispersionParameter(existingDT
				.get_GkDispersionParameter());
		gwDemolishAndDerelict.setDispersionParameter(existingDT
				.get_GwDispersionParameter());
		gzRenovateOrGy.setDispersionParameter(existingDT
				.get_GzDispersionParameter());
		gyAddOrNewSpace.setDispersionParameter(existingDT
				.get_GyDispersionParameter());
		developNewOptions.setDispersionParameter(existingDT
				.get_NewTypeDispersionParameter());
	}

	private void insertDispersionParameterObjects() {
		final int spacetype = land.getCoverage();
		final Coefficient rootParam = SpaceTypeCoefficient
				.getNoChangeDisp(spacetype);
		getMyLogitModel().setDispersionParameterAsCoeff(rootParam);
		final Coefficient changeParam = SpaceTypeCoefficient
				.getChangeOptionsDisp(spacetype);
		gkChangeOptions.setDispersionParameterAsCoeff(changeParam);
		final Coefficient demoderParam = SpaceTypeCoefficient
				.getDemolishDerelictDisp(spacetype);
		gwDemolishAndDerelict.setDispersionParameterAsCoeff(demoderParam);
		final Coefficient renoaddnewParam = SpaceTypeCoefficient
				.getRenovateAddNewDisp(spacetype);
		gzRenovateOrGy.setDispersionParameterAsCoeff(renoaddnewParam);
		final Coefficient addnewParam = SpaceTypeCoefficient
				.getAddNewDisp(spacetype);
		gyAddOrNewSpace.setDispersionParameterAsCoeff(addnewParam);
		final Coefficient newParam = SpaceTypeCoefficient.getNewTypeDisp(spacetype);
		developNewOptions.setDispersionParameterAsCoeff(newParam);
	}

	@Override
	public double getAllowedFAR(SpaceTypeInterface dt) {
		final ZoningPermissions reg = getZoningForSpaceType(dt);
		if (reg == null) {
			return 0;
		}
		return reg.get_MaxIntensityPermitted();
	}

	public int getGridCode() {
		return gridCode;
	}

	private LogitModel getMyLogitModel() {
		if (myLogitModel != null) {
			return myLogitModel;
		}

		// Tree structure currently hard coded. TODO change this?

		myLogitModel = new LogitModel();
		gkChangeOptions = new LogitModel();
		myLogitModel.addAlternative(gkChangeOptions);
		gwDemolishAndDerelict = new LogitModel();
		gkChangeOptions.addAlternative(gwDemolishAndDerelict);
		gzRenovateOrGy = new LogitModel();
		gkChangeOptions.addAlternative(gzRenovateOrGy);
		gyAddOrNewSpace = new LogitModel();
		gzRenovateOrGy.addAlternative(gyAddOrNewSpace);
		developNewOptions = new LogitModel();
		gyAddOrNewSpace.addAlternative(developNewOptions);

		if (get_NoChangePossibilities()) {
			final Alternative noChange = new NoChangeAlternative(this);
			myLogitModel.addAlternative(noChange);
		}

		if (get_DemolitionPossibilities()) {
			final Alternative demolishAlternative = new DemolishAlternative();
			gwDemolishAndDerelict.addAlternative(demolishAlternative);
		}

		if (get_DerelictionPossibilities()) {
			final Alternative derelictAlternative = new DerelictAlternative();
			gwDemolishAndDerelict.addAlternative(derelictAlternative);
		}

		if (get_RenovationPossibilities()) {
			final Alternative renovateAlternative = new RenovateAlternative();
			gzRenovateOrGy.addAlternative(renovateAlternative);
		}

		if (get_AdditionPossibilities()) {
			final Alternative addSpaceAlternative = new DevelopMoreAlternative(this);
			gyAddOrNewSpace.addAlternative(addSpaceAlternative);
		}

		if (get_NewDevelopmentPossibilities()) {
			final Iterator<ZoningPermissions> it = getAllowedSpaceTypes();
			while (it.hasNext()) {
				final ZoningPermissions zp = it.next();
				// if this didn't work, use this:
				final SpaceTypesI whatWeCouldBuild = SpaceTypesI
						.getAlreadyCreatedSpaceTypeBySpaceTypeID(zp.get_SpaceTypeId());
				// SpaceTypesI whatWeCouldBuild =
				// zp.get_SPACE_TYPES_I(SSessionJdbc.getThreadLocalSession());
				if (!whatWeCouldBuild.isVacant()) {
					Alternative aNewSpaceAlternative;
					aNewSpaceAlternative = new DevelopNewAlternative(this,
							whatWeCouldBuild);
					developNewOptions.addAlternative(aNewSpaceAlternative);
				}

			}
		}
		return myLogitModel;
	}

	@Override
	public String getName() {
		return get_ZoningRulesCodeName();
	}

	// FIXME getServicingCost() method
	public double getServicingCost(SpaceTypesI dt) {
		final SSessionJdbc session = land.getSession();

		// 1. Get service required in zoning
		final int serviceRequired = session.mustFind(ZoningPermissions_gen.meta,
				get_ZoningRulesCode(), dt.get_SpaceTypeId()).get_ServicesRequirement();

		// 2. Get the available service level on the parcel
		final int availableService = land.getAvailableServiceCode();

		// 3. Get the costs associated with installing new services:
		if (availableService < serviceRequired) {
			// new costs should be applied!
			// TODO: do required calculations to get the value of services costs
			final TransitionCostCodes costsRecord = session.mustFind(
					TransitionCostCodes.meta, land.get_CostScheduleId());
			costsRecord.get_LowCapacityServicesInstallationCost();
			costsRecord.get_HighCapacityServicesInstallationCost();
			// costsRecord.get_BrownFieldCleanupCost();
			// costsRecord.get_GreenFieldPreparationCost();
		}
		return 0;
	}

	public List<ZoningPermissions> getZoning() {
		if (zoning != null) {
			return zoning;
		}
		final SSessionJdbc session = land.getSession();
		final SQuery<ZoningPermissions> qryPermissions = new SQuery<ZoningPermissions>(
				ZoningPermissions_gen.meta).eq(ZoningPermissions_gen.ZoningRulesCode,
				get_ZoningRulesCode());

		zoning = session.query(qryPermissions);
		return zoning;
	}

	public ZoningPermissions getZoningForSpaceType(SpaceTypeInterface dt) {
		final SSessionJdbc session = land.getSession();

		final ZoningPermissions zp = session.mustFind(ZoningPermissions_gen.meta,
				get_ZoningRulesCode(), dt.getSpaceTypeID());

		return zp;
	}

	public ZoningPermissions checkZoningForSpaceType(SpaceTypeInterface dt) {

		// cache the items not found, otherwise SimpleORM will continually check
		// the database to look for them
		if (notAllowedSpaceTypes.contains(dt)) {
			return null;
		}

		final SSessionJdbc session = land.getSession();

		final ZoningPermissions zp = session.find(ZoningPermissions_gen.meta,
				get_ZoningRulesCode(), dt.getSpaceTypeID());

		if (zp == null) {
			notAllowedSpaceTypes.add(dt);
		}

		return zp;

	}

	public boolean isAllowed(SpaceTypeInterface dt) {

		if (getZoningForSpaceType(dt) == null) {
			return false;
		}
		return true;
	}

	@Override
	public void noLongerAllowDevelopmentType(SpaceTypeInterface dt) {
		if (getZoning() != null) {
			getZoning().remove(dt);
		}
		myLogitModel = null;
	}

	@Override
	public int size() {
		return getZoning().size();
	}

	@Override
	public String toString() {
		return "ZoningScheme " + getName();
	}

	public void addExpectedValuesToMatrix(EstimationMatrix values, LandInventory l) {
		land = l;
		existingDT = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(land
				.getCoverage());
		if (existingDT == null) {
			logger.fatal("Invalid coverage code " + land.getCoverage() + " at "
					+ l.parcelToString());
			throw new RuntimeException("Invalid coverage code " + land.getCoverage()
					+ " at " + l.parcelToString());
		}
		final boolean doIt = land.isDevelopable();
		if (!doIt) {
			return; // no contribution to identified targets if it's impossible
			// to develop!
		}

		insertDispersionParameterObjects();

		// gridFee = 0.0;
		final LogitModel developChoice = getMyLogitModel();

		developNewOptions.setConstantUtilityAsCoeff(SpaceTypeCoefficient
				.getNewFromTransitionConst(land.getCoverage()));
		// If we are considering land n acres at a time, then when a parcel is
		// greater than n acres we need to
		// call monteCarloElementalChoice repeatedly. There may also need to be
		// some special treatment for parcels that are
		// much less than n acres.
		//
		final double originalLandArea = land.getLandArea();
		// for (int sampleTimes=0;sampleTimes <=
		// originalLandArea/land.getMaxParcelSize();sampleTimes++) {
		try {
			final List<ExpectedValue> expValues = values
					.getTargetsApplicableToCurrentParcel();
			final Vector component = developChoice.getExpectedTargetValues(expValues);
			values.addExpectedValueComponentApplicableToCurrentParcel(component);

		}
		catch (final ChoiceModelOverflowException e) {
			final String msg = "Choice model overflow exception for " + this
					+ " in parcel " + land.getParcelId() + ": " + e.getMessage();
			logger.fatal(msg);

			if (!ignoreErrors) {
				throw new RuntimeException(msg, e);
			}
			else {
				land.getParcelErrorLog().logParcelError(land, e);
			}
		}
		catch (final NoAlternativeAvailable e) {
			final String msg = "No alternative available for parcel "
					+ land.getParcelId();
			logger.fatal(msg);

			if (!ignoreErrors) {
				throw new RuntimeException(msg, e);
			}
			else {
				land.getParcelErrorLog().logParcelError(land, e);
			}
		}
		// }
	}

	public void addDerivativesToMatrix(EstimationMatrix partialDerivatives,
			LandInventory l) {
		land = l;
		existingDT = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(land
				.getCoverage());
		if (existingDT == null) {
			logger.fatal("Invalid coverage code " + land.getCoverage() + " at "
					+ l.parcelToString());
			throw new RuntimeException("Invalid coverage code " + land.getCoverage()
					+ " at " + l.parcelToString());
		}
		final boolean doIt = land.isDevelopable();
		if (!doIt) {
			return; // no contribution to identified targets if it's impossible
			// to develop!
		}

		insertDispersionParameterObjects();

		// gridFee = 0.0;
		final LogitModel developChoice = getMyLogitModel();

		developNewOptions.setConstantUtilityAsCoeff(SpaceTypeCoefficient
				.getNewFromTransitionConst(land.getCoverage()));
		// If we are considering land n acres at a time, then when a parcel is
		// greater than n acres we need to
		// call monteCarloElementalChoice repeatedly. There may also need to be
		// some special treatment for parcels that are
		// much less than n acres.
		//
		final double originalLandArea = land.getLandArea();
		// for (int sampleTimes=0;sampleTimes <=
		// originalLandArea/land.getMaxParcelSize();sampleTimes++) {
		try {
			final List<ExpectedValue> expValues = partialDerivatives
					.getTargetsApplicableToCurrentParcel();
			final List<Coefficient> coeffs = partialDerivatives.getCoefficients();
			final Matrix component = developChoice
					.getExpectedTargetDerivativesWRTParameters(expValues, coeffs);
			partialDerivatives
					.addDerivativeComponentApplicableToCurrentParcel(component);

		}
		catch (final ChoiceModelOverflowException e) {
			final String msg = "Choice model overflow exception for " + this
					+ " in parcel " + land.getParcelId() + ": " + e.getMessage();
			logger.fatal(msg);

			if (!ignoreErrors) {
				throw new RuntimeException(msg, e);
			}
			else {
				land.getParcelErrorLog().logParcelError(land, e);
			}
		}
		catch (final NoAlternativeAvailable e) {
			final String msg = "No alternative available for parcel "
					+ land.getParcelId();
			logger.fatal(msg);

			if (!ignoreErrors) {
				throw new RuntimeException(msg, e);
			}
			else {
				land.getParcelErrorLog().logParcelError(land, e);
			}
		}
		// }
	}

	public void startCaching() {
		getMyLogitModel().startCaching();
	}

	public void endCaching() {
		getMyLogitModel().endCaching();
	}
}
