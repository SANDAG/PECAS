package com.hbaspecto.pecas.sd;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.hbaspecto.pecas.land.LandInventory;
import com.hbaspecto.pecas.land.ParcelInterface;

public class DevelopmentLog {

	static Logger logger = Logger.getLogger(DevelopmentLog.class);

	static BufferedWriter developmentLogBuffer;

	public void open(String fileNameAndPath) {
		try {
			developmentLogBuffer = new BufferedWriter(new FileWriter(fileNameAndPath));
			developmentLogBuffer.write("event_type," + "parcel_id,"
					+ "original_pecas_parcel_num," + "new_pecas_parcel_num,"
					+ "available_services," + "old_space_type_id," + "new_space_type_id,"
					+ "old_space_quantity," + "new_space_quantity," + "old_year_built,"
					+ "new_year_built," + "land_area," + "old_is_derelict,"
					+ "new_is_derelict," + "old_is_brownfield," + "new_is_brownfield,"
					+ "zoning_rules_code," + "taz\n");
		}
		catch (final IOException e) {
			logger.fatal("Can't open development log", e);
			throw new RuntimeException("Can't open development log", e);
		}
	}

	public void close() {
		try {
			developmentLogBuffer.close();
		}
		catch (final IOException e) {
			logger.error("Can't close stream");
			e.printStackTrace();
		}
	}

	public void flush() {
		try {
			developmentLogBuffer.flush();
		}
		catch (final IOException e) {
			logger.error("Can't close stream");
			e.printStackTrace();
		}
	}

	public void logDevelopment(LandInventory land, int old_space_type_id,
			double old_space_quantity, int old_year_built, boolean oldIsDerelict,
			boolean oldIsBrownfield) {

		rawLog("C", land.getParcelId(), land.getPECASParcelNumber(),
				land.getPECASParcelNumber(), land.getAvailableServiceCode(),
				old_space_type_id, land.getCoverage(), old_space_quantity,
				land.getQuantity(), old_year_built, ZoningRulesI.currentYear,
				land.getLandArea(), oldIsDerelict, land.isDerelict(), oldIsBrownfield,
				land.isBrownfield(), land.getZoningRulesCode(), land.getTaz());
	};

	public void logDevelopmentWithSplit(LandInventory land,
			ParcelInterface newBit, double oldDevQuantity) {
		rawLog("CS", land.getParcelId(), land.getPECASParcelNumber(),
				newBit.get_PecasParcelNum(), newBit.get_AvailableServicesCode(),
				land.getCoverage(), newBit.get_SpaceTypeId(), oldDevQuantity,
				newBit.get_SpaceQuantity(), land.getYearBuilt(),
				newBit.get_YearBuilt(), newBit.get_LandArea(), land.isDerelict(),
				newBit.get_IsDerelict(), land.isBrownfield(),
				newBit.get_IsBrownfield(), land.getZoningRulesCode(), land.getTaz());
	};

	public void logDemolition(LandInventory land, int oldST,
			double oldSquareFeet, int oldYear, boolean oldIsDerelict) {
		rawLog("D", land.getParcelId(), land.getPECASParcelNumber(),
				land.getPECASParcelNumber(), land.getAvailableServiceCode(), oldST,
				land.getCoverage(), oldSquareFeet, land.getQuantity(), oldYear,
				land.getYearBuilt(), land.getLandArea(), oldIsDerelict,
				land.isDerelict(), land.isBrownfield(), land.isBrownfield(),
				land.getZoningRulesCode(), land.getTaz());
	}

	public void logDemolitionWithSplit(LandInventory land,
			ParcelInterface newBit, double quantityDemolished) {
		rawLog("DS", land.getParcelId(), land.getPECASParcelNumber(),
				newBit.get_PecasParcelNum(), land.getAvailableServiceCode(),
				land.getCoverage(), newBit.get_SpaceTypeId(), quantityDemolished,
				newBit.get_SpaceQuantity(), land.getYearBuilt(),
				ZoningRulesI.currentYear, newBit.get_LandArea(), land.isDerelict(),
				newBit.get_IsDerelict(), land.isBrownfield(),
				newBit.get_IsBrownfield(), land.getZoningRulesCode(), land.getTaz());
	}

	public void logDereliction(LandInventory land, boolean oldIsDerelict) {
		rawLog("L", land.getParcelId(), land.getPECASParcelNumber(),
				land.getPECASParcelNumber(), land.getAvailableServiceCode(),
				land.getCoverage(), land.getCoverage(), land.getQuantity(),
				land.getQuantity(), land.getYearBuilt(), land.getYearBuilt(),
				land.getLandArea(), oldIsDerelict, land.isDerelict(),
				land.isBrownfield(), land.isBrownfield(), land.getZoningRulesCode(),
				land.getTaz());
	}

	public void logDerelicationWithSplit(LandInventory land,
			ParcelInterface newBit) {
		rawLog("LS", land.getParcelId(), land.getPECASParcelNumber(),
				newBit.get_PecasParcelNum(), newBit.get_AvailableServicesCode(),
				land.getCoverage(), newBit.get_SpaceTypeId(),
				newBit.get_SpaceQuantity(), newBit.get_SpaceQuantity(),
				land.getYearBuilt(), newBit.get_YearBuilt(), newBit.get_LandArea(),
				land.isDerelict(), newBit.get_IsDerelict(), land.isBrownfield(),
				newBit.get_IsBrownfield(), land.getZoningRulesCode(), land.getTaz());
	}

	public void logRenovation(LandInventory land, int old_year_built,
			boolean oldIsDerelict) {
		rawLog("R", land.getParcelId(), land.getPECASParcelNumber(),
				land.getPECASParcelNumber(), land.getAvailableServiceCode(),
				land.getCoverage(), land.getCoverage(), land.getQuantity(),
				land.getQuantity(), old_year_built, land.getYearBuilt(),
				land.getLandArea(), oldIsDerelict, land.isDerelict(),
				land.isBrownfield(), land.isBrownfield(), land.getZoningRulesCode(),
				land.getTaz());
	}

	public void logRenovationWithSplit(LandInventory land, ParcelInterface newBit) {
		rawLog("RS", land.getParcelId(), land.getPECASParcelNumber(),
				newBit.get_PecasParcelNum(), newBit.get_AvailableServicesCode(),
				land.getCoverage(), newBit.get_SpaceTypeId(),
				newBit.get_SpaceQuantity(), newBit.get_SpaceQuantity(),
				land.getYearBuilt(), newBit.get_YearBuilt(), newBit.get_LandArea(),
				land.isDerelict(), newBit.get_IsDerelict(), land.isBrownfield(),
				newBit.get_IsBrownfield(), land.getZoningRulesCode(), land.getTaz());
	};

	public void logAddition(LandInventory land, double old_space_quantity,
			int old_year_built) {
		rawLog("A", land.getParcelId(), land.getPECASParcelNumber(),
				land.getPECASParcelNumber(), land.getAvailableServiceCode(),
				land.getCoverage(), land.getCoverage(), old_space_quantity,
				land.getQuantity(), old_year_built, land.getYearBuilt(),
				land.getLandArea(), land.isDerelict(), land.isDerelict(),
				land.isBrownfield(), land.isBrownfield(), land.getZoningRulesCode(),
				land.getTaz());
	};

	public void logAdditionWithSplit(LandInventory land, ParcelInterface newBit,
			double oldQuantity) {
		rawLog("AS", land.getParcelId(), land.getPECASParcelNumber(),
				newBit.get_PecasParcelNum(), newBit.get_AvailableServicesCode(),
				land.getCoverage(), newBit.get_SpaceTypeId(), oldQuantity,
				newBit.get_SpaceQuantity(), land.getYearBuilt(),
				newBit.get_YearBuilt(), newBit.get_LandArea(), land.isDerelict(),
				newBit.get_IsDerelict(), land.isBrownfield(),
				newBit.get_IsBrownfield(), land.getZoningRulesCode(), land.getTaz());
	};

	public void logBadZoning(LandInventory land) {
		rawLog("X", land.getParcelId(), land.getPECASParcelNumber(),
				land.getPECASParcelNumber(), land.getAvailableServiceCode(),
				land.getCoverage(), land.getCoverage(), land.getQuantity(),
				land.getQuantity(), land.getYearBuilt(), land.getYearBuilt(), -1.0,
				land.isDerelict(), land.isDerelict(), land.isBrownfield(),
				land.isBrownfield(), land.getZoningRulesCode(), land.getTaz());
	}

	protected void rawLog(String event_type, String parcel_id,
			long original_pecas_parcel_num, long new_pecas_parcel_num,
			int available_services, int old_space_type_id, int new_space_type_id,
			double old_space_quantity, double new_space_quantity, int old_year_built,
			int new_year_built, double land_area, boolean oldIsDerelict,
			boolean newIsDerelict, boolean oldIsBrownfield, boolean newIsBrownfield,
			int zoning_rules_code, int taz) {
		try {
			developmentLogBuffer.write(event_type + "," + parcel_id + ","
					+ original_pecas_parcel_num + "," + new_pecas_parcel_num + ","
					+ available_services + "," + old_space_type_id + ","
					+ new_space_type_id + "," + old_space_quantity + ","
					+ new_space_quantity + "," + old_year_built + "," + new_year_built
					+ "," + land_area + "," + oldIsDerelict + "," + newIsDerelict + ","
					+ oldIsBrownfield + "," + newIsBrownfield + "," + zoning_rules_code
					+ "," + taz + "\n");
		}
		catch (final IOException e) {
			logger.fatal("Can't write out to development log", e);
			throw new RuntimeException("Can't write out to development log", e);
		}
	}

	public void logRemainingOfSplitParcel(LandInventory land) {
		// write out [U]n[S]plit parcel.
		rawLog("US", land.getParcelId(), land.getPECASParcelNumber(),
				land.getPECASParcelNumber(), land.getAvailableServiceCode(),
				land.getCoverage(), land.getCoverage(), land.getQuantity(),
				land.getQuantity(), land.getYearBuilt(), land.getYearBuilt(),
				land.getLandArea(), land.isDerelict(), land.isDerelict(),
				land.isBrownfield(), land.isBrownfield(), land.getZoningRulesCode(),
				land.getTaz());
	}

}
