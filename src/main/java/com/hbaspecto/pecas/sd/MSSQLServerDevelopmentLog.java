package com.hbaspecto.pecas.sd;

import java.io.IOException;

public class MSSQLServerDevelopmentLog extends DevelopmentLog {

	public MSSQLServerDevelopmentLog() {
		
	}
	
	// booleans in SQL Server is  a bit 0/1, it doesn't support True/False values
	protected void rawLog(String event_type, String parcel_id, 
			long original_pecas_parcel_num, long new_pecas_parcel_num,
			int available_services, 
			int old_space_type_id, int new_space_type_id,
			double old_space_quantity, double new_space_quantity,
			int old_year_built, int new_year_built,
			double land_area, 
			boolean oldIsDerelict, boolean newIsDerelict,
			boolean oldIsBrownfield, boolean newIsBrownfield, 
			int zoning_rules_code,
			int taz){
		int intOldIsBrownfield=0, intOldIsDerelict=0, intNewIsBrownfield=0, intNewIsDerelict=0;;
		if (oldIsBrownfield) intOldIsBrownfield =1;
		if (newIsBrownfield) intNewIsBrownfield =1;
		if (oldIsDerelict) intOldIsDerelict =1;
		if (newIsDerelict) intNewIsDerelict =1;
		
		try {
			developmentLogBuffer.write(event_type+","+ parcel_id +"," + 
				original_pecas_parcel_num + "," + new_pecas_parcel_num + ","+
				available_services +"," + 
				old_space_type_id + ","+ new_space_type_id+ ","+
				Double.toString(old_space_quantity)+","+ Double.toString(new_space_quantity) + ","+
				old_year_built + ","+ new_year_built + "," +
				Double.toString(land_area) + "," +
				intOldIsDerelict + "," + intNewIsDerelict + "," +
				intOldIsBrownfield + "," + intNewIsBrownfield + "," +
				zoning_rules_code +","+
				taz + "\n");
		} catch (IOException e) {
			logger.fatal("Can't write out to development log",e);
			throw new RuntimeException("Can't write out to development log", e);
		}
	}	
}
