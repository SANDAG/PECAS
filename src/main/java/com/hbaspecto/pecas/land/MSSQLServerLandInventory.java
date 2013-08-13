package com.hbaspecto.pecas.land;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;

import simpleorm.sessionjdbc.SSessionJdbc;

import com.hbaspecto.pecas.sd.DevelopmentLog;
import com.hbaspecto.pecas.sd.MSSQLServerDevelopmentLog;
import com.pb.common.datafile.TableDataFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

public class MSSQLServerLandInventory extends SimpleORMLandInventory {
	protected Connection conn;
	private DevelopmentLog msSQLDevelopmentLogger;


	public MSSQLServerLandInventory(){
	}
	public MSSQLServerLandInventory(ResourceBundle rb, String landDatabaseDriver,
			String landDatabaseSpecifier, 
			String user, String password, 
			String logFileNameAndPath,
			String schema) throws SQLException {

		super(rb, landDatabaseDriver, landDatabaseSpecifier, user, password, schema);
		conn = session.getJdbcConnection();
		this.logFileNameAndPath=logFileNameAndPath;
	}



	
	@Override
	protected void initSessionAndBatches() {
		session=prepareSimpleORMSession(rbSD);
		conn=session.getJdbcConnection();
		maxPecasParcelNum = Parcels.getMaximumPecasParcelNum(session);	
		numberOfBatches = ResourceUtil.getIntegerProperty(rbSD, "NumberOfBatches",250);
		if (numberOfBatches < 1) {
			logger.error("NumberOfBatches cannot be less than 1 in properties file");
			numberOfBatches=1;
		}

	}
	@Override
	public void applyDevelopmentChanges() {

		try {
			
			String exactpath= ResourceUtil.checkAndGetProperty(rbSD, "Exactpath");
				
			
			session.flush();
			Statement statement = conn.createStatement();

			logger.info("Reading in developmentevents.csv");
			statement.execute("TRUNCATE TABLE development_events;");
			statement.execute("BULK INSERT development_events FROM '" + exactpath+"DevelopmentEvents.csv" +             				  
			"' WITH (FIRSTROW = 2,FORMATFILE='"+exactpath+"development_events.xml');");

			logger.info("Now applying changes to parcel file. NOTE PSEUDOPARCELLING IS IMPLEMENTED");

			String strUpdate =  "UPDATE "+Parcels.meta.getTableName()+" " +
			"SET	space_quantity   = d.new_space_quantity, " +
			"       space_type_id    = d.new_space_type_id, " +
			"       year_built       = d.new_year_built, " +
			"       land_area        = d.land_area, " + // to update the new area of the remaining parcel
			"       is_derelict      = d.new_is_derelict, " +
			"       is_brownfield    = d.new_is_brownfield " +
			"FROM development_events d " +
			"WHERE pecas_parcel_num = d.original_pecas_parcel_num " +
			"  	   AND (d.event_type = 'C' OR "+ 
			"			d.event_type = 'D' OR "+ 
			"           d.event_type = 'A' OR " +
			"           d.event_type = 'L' OR " +
			"           d.event_type = 'R' );\n";


    		
    		strUpdate += "INSERT INTO "+Parcels.meta.getTableName()+" " +
    		"    	SELECT  parcel_id, " + 
    		"		new_pecas_parcel_num, " +
    		"		new_year_built, " + 
    		"		taz, " + 
    		"		new_space_type_id, " +
    		"		new_space_quantity, " +
    		"		land_area, " +
    		"		available_services, " +
    		"		new_is_derelict, " +
    		"		new_is_brownfield " +
    		"   	FROM development_events d " +
    		"    	WHERE " +
    		"	     d.event_type = 'CS' " +
    		"	OR   d.event_type = 'AS' " +
    		"	OR   d.event_type = 'RS' " +
    		"	OR   d.event_type = 'DS' " +
    		"	OR   d.event_type = 'LS' " +
    		"   	; \n";
    		
   		    			   
    		strUpdate += " INSERT INTO parcel_cost_xref "+
    		" SELECT de.new_pecas_parcel_num, xref.cost_schedule_id, xref.year_effective "+ 
    		" FROM parcel_cost_xref xref, development_events de "+
    		" WHERE xref.pecas_parcel_num=de.original_pecas_parcel_num "+    		
    		" AND ( "+	     
			"     	 event_type = 'CS' "+ 
    		"	OR   event_type = 'AS' "+
    		"	OR   event_type = 'RS' "+
    		"	OR   event_type = 'DS' "+
    		"	OR   event_type = 'LS' ) "+
    		"	; \n";
    			
    		strUpdate +=" INSERT INTO parcel_fee_xref "+
    		" SELECT de.new_pecas_parcel_num, xref.fee_schedule_id, xref.year_effective "+ 
    		" FROM parcel_fee_xref xref, development_events de "+
    		" WHERE xref.pecas_parcel_num=de.original_pecas_parcel_num "+    		
    		" AND ( "+	     
			"     	 event_type = 'CS' "+ 
    		"	OR   event_type = 'AS' "+
    		"	OR   event_type = 'RS' "+
    		"	OR   event_type = 'DS' "+
    		"	OR   event_type = 'LS' ) "+
    		"	; \n";
    			
    		strUpdate +=" INSERT INTO parcel_zoning_xref "+
    		" SELECT de.new_pecas_parcel_num, xref.zoning_rules_code, xref.year_effective "+ 
    		" FROM parcel_zoning_xref xref, development_events de "+
    		" WHERE xref.pecas_parcel_num=de.original_pecas_parcel_num "+    		
    			" AND ( "+	     
    			"     	 event_type = 'CS' "+ 
        		"	OR   event_type = 'AS' "+
        		"	OR   event_type = 'RS' "+
        		"	OR   event_type = 'DS' "+
        		"	OR   event_type = 'LS' ) "+
        		"	; \n";
    			
    		strUpdate +=" INSERT INTO local_effect_distances "+
    		" SELECT de.new_pecas_parcel_num, dist.local_effect_id, dist.local_effect_distance, dist.year_effective"+ 
    		" FROM local_effect_distances dist, development_events de "+
    		" WHERE dist.pecas_parcel_num=de.original_pecas_parcel_num "+  		
    			" AND ( "+	     
    			"     	 event_type = 'CS' "+ 
        		"	OR   event_type = 'AS' "+
        		"	OR   event_type = 'RS' "+
        		"	OR   event_type = 'DS' "+
        		"	OR   event_type = 'LS' ) "+
        		"	; \n";
		      
    		//System.out.println(strUpdate);

			statement.execute(strUpdate);
			session.commit();



		} catch (SQLException e) {
			logger.fatal("Can't apply development events",e);
			throw new RuntimeException("Can't apply development events", e);
		}
	}


	protected void createParcelsTemp(int year) {
		try {
			Statement statement = conn.createStatement();		
			String strSQL = "IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('parcels_temp') AND type in (N'U')) " +
							" DROP TABLE parcels_temp ";
							
			statement.execute(strSQL);
			
			strSQL = "update current_year_table set current_year ="+year;
			statement.execute(strSQL);			
			
			boolean fetchParcelsByTaz = ResourceUtil.getBooleanProperty(rbSD, "FetchParcelsByTaz",false);
			int N = numberOfBatches;

			statement.execute(
					"SELECT fp.pecas_parcel_num as parcel_id, " + //TODO: In Cali there is no parcel_id. We use pecasParcelNum
					"fp.pecas_parcel_num, " +
					"fp.year_built, fp.taz, " +
					"fp.space_type_id, " +
					"fp.space_quantity, " +
					"fp.land_area, " +
					"fp.available_services_code, " +
					"zxref.zoning_rules_code, " +
					"costxref.cost_schedule_id, " +
					"feexref.fee_schedule_id, " +
					"fp.is_derelict, " +
					"fp.is_brownfield, "+
					"CEILING((rand(fp.pecas_parcel_num))* "+ N + " ) as randnum "+
					"INTO parcels_temp "+
					"FROM "+Parcels.meta.getTableName()+" fp, " +
					"most_recent_zoning_year z, " +
					"most_recent_fee_year f, " +
					"most_recent_cost_year c, " +
					"parcel_zoning_xref zxref, " +
					"parcel_fee_xref feexref, " +
					"parcel_cost_xref costxref " +
					"WHERE " +
					"fp.space_type_id is not null " + 
					"AND fp.pecas_parcel_num = z.pecas_parcel_num " +
					"AND fp.pecas_parcel_num = zxref.pecas_parcel_num " +
					"AND zxref.year_effective = z.current_zoning_year AND " +
					"fp.pecas_parcel_num = feexref.pecas_parcel_num AND " +
					"fp.pecas_parcel_num = f.pecas_parcel_num AND " +
					"feexref.year_effective = f.current_fee_year AND " +
					"fp.pecas_parcel_num = costxref.pecas_parcel_num AND " +
					"fp.pecas_parcel_num = c.pecas_parcel_num AND " +
			"costxref.year_effective = c.current_cost_year;");

			statement.execute("ALTER TABLE parcels_temp "+
			"ADD CONSTRAINT parcels_temp_pkey PRIMARY KEY(pecas_parcel_num)");
			
			if (fetchParcelsByTaz)
				statement.execute("CREATE INDEX taz_idx ON parcels_temp (taz)");
			else
				statement.execute("CREATE INDEX randnum_idx  ON parcels_temp (randnum)");

			statement.execute("UPDATE STATISTICS parcels_temp");	  

			conn.commit();
		} catch (SQLException e) {
			logger.fatal("Couldn't create temporary table in database", e);
			throw new RuntimeException("Couldn't create temporary table in database", e);
		}
	}

	@Override
	public DevelopmentLog getDevelopmentLogger() {
		if (msSQLDevelopmentLogger!= null) return msSQLDevelopmentLogger;
		msSQLDevelopmentLogger = new MSSQLServerDevelopmentLog();
		msSQLDevelopmentLogger.open(logFileNameAndPath);
		return msSQLDevelopmentLogger;
	}

}
