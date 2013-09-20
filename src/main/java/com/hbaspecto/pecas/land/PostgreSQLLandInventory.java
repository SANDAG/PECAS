/*
 * Copyright 2007 HBA Specto Incorporated
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
package com.hbaspecto.pecas.land;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ResourceBundle;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import com.hbaspecto.pecas.sd.DevelopmentLog;
import com.hbaspecto.pecas.sd.orm.Parcels_gen;
import com.pb.common.util.ResourceUtil;

public class PostgreSQLLandInventory
        extends SimpleORMLandInventory
{

    // TODO assumes column names, but we allow the user to change column names
    // using the sdorm properties.

    // protected final Connection conn;
    protected DevelopmentLog developmentLogger;

    public PostgreSQLLandInventory()
    {
        // conn = session.getJdbcConnection();
    }

    public PostgreSQLLandInventory(ResourceBundle rb, String landDatabaseDriver,
            String landDatabaseSpecifier, String user, String password, String logFileNameAndPath,
            String schema) throws SQLException
    {
        super(rb, landDatabaseDriver, landDatabaseSpecifier, user, password, schema);
        // conn = session.getJdbcConnection();
        this.logFileNameAndPath = logFileNameAndPath;
    }

    @Override
    protected void createParcelsTemp(int year)
    {
        try
        {
            final Connection conn = session.getJdbcConnection();
            final Statement statement = conn.createStatement();
            statement.execute("drop table if exists parcels_temp");
            statement.execute("update current_year_table set current_year=" + year);

            final boolean fetchParcelsByTaz = ResourceUtil.getBooleanProperty(rbSD,
                    "FetchParcelsByTaz", false);

            final int N = numberOfBatches;
            statement.execute("SELECT fp.parcel_id, "
                    + "fp.pecas_parcel_num, "
                    + "fp.year_built, fp.taz, "
                    + "fp.space_type_id, "
                    + "fp.space_quantity, "
                    + "fp.land_area, "
                    + "fp.available_services_code, "
                    + "zxref.zoning_rules_code, "
                    + "costxref.cost_schedule_id, "
                    + "feexref.fee_schedule_id, "
                    + "fp.is_derelict, "
                    + "fp.is_brownfield, "
                    +
                    // ceil((random())* 250 ) as randnum
                    "ceil((random())* " + N + " ) as randnum " + "INTO parcels_temp " + "FROM "
                    + Parcels_gen.meta.getTableName() + " fp, " + "most_recent_zoning_year z, "
                    + "most_recent_fee_year f, " + "most_recent_cost_year c, "
                    + "parcel_zoning_xref zxref, " + "parcel_fee_xref feexref, "
                    + "parcel_cost_xref costxref "
                    + "WHERE fp.pecas_parcel_num = z.pecas_parcel_num "
                    + "AND fp.pecas_parcel_num = zxref.pecas_parcel_num "
                    + "AND zxref.year_effective = z.current_zoning_year AND "
                    + "fp.pecas_parcel_num = feexref.pecas_parcel_num AND "
                    + "fp.pecas_parcel_num = f.pecas_parcel_num AND "
                    + "feexref.year_effective = f.current_fee_year AND "
                    + "fp.pecas_parcel_num = costxref.pecas_parcel_num AND "
                    + "fp.pecas_parcel_num = c.pecas_parcel_num AND "
                    + "costxref.year_effective = c.current_cost_year;");
            statement.execute("ALTER TABLE parcels_temp "
                    + "ADD CONSTRAINT parcels_temp_pkey PRIMARY KEY(pecas_parcel_num)");

            if (fetchParcelsByTaz)
            {
                statement.execute("CREATE INDEX taz_idx ON parcels_temp (taz)");
            } else
            {
                statement.execute("CREATE INDEX randnum_idx  ON parcels_temp (randnum)");
            }

            conn.commit();
        } catch (final SQLException e)
        {
            logger.fatal("Couldn't create temporary table in database", e);
            throw new RuntimeException("Couldn't create temporary table in database", e);
        }
    }

    @Override
    public void applyDevelopmentChanges()
    {

        try
        {
            session.flush();
            final Connection conn = session.getJdbcConnection();
            final Statement statement = conn.createStatement();

            logger.info("Reading in developmentevents.csv");
            statement.execute("TRUNCATE development_events;");

            final PGConnection pConn = (PGConnection) conn;
            final CopyManager copyManager = pConn.getCopyAPI();

            // CopyManager copyManager = new CopyManager((BaseConnection) conn);

            final FileInputStream devEventStream = new FileInputStream(logFileNameAndPath);
            copyManager.copyIn("copy development_events from stdin CSV HEADER;", devEventStream);
            // statement.execute("copy development_events from '"+logFileNameAndPath+"' CSV HEADER;  ");

            logger.info("Now applying changes to parcel file. NOTE: PSEUDOPARCELLING IS IMPLEMENTED.");

            String strUpdate = "UPDATE "
                    + Parcels_gen.meta.getTableName()
                    + " p "
                    + "SET	space_quantity   = d.new_space_quantity, "
                    + "       space_type_id    = d.new_space_type_id, "
                    + "       year_built       = d.new_year_built, "
                    + "       land_area        = d.land_area, "
                    + // to update the new area of the remaining parcel
                    "       is_derelict      = d.new_is_derelict, "
                    + "       is_brownfield    = d.new_is_brownfield "
                    + "FROM development_events d "
                    + "WHERE p.pecas_parcel_num = d.original_pecas_parcel_num "
                    + "  	   AND (d.event_type = 'C' OR " + "			d.event_type = 'R' OR "
                    + "			d.event_type = 'D' OR " + "           d.event_type = 'A' OR "
                    + "           d.event_type = 'L' OR " + "           d.event_type = 'US' );\n";

            // TODO: Do the insertions in transaction mode.

            strUpdate += "INSERT INTO " + Parcels_gen.meta.getTableName() + " ( "
                    + "    	SELECT  parcel_id, " + "		new_pecas_parcel_num, "
                    + "		new_year_built, " + "		taz, " + "		new_space_type_id, "
                    + "		new_space_quantity, " + "		land_area, " + "		available_services, "
                    + "		new_is_derelict, " + "		new_is_brownfield "
                    + "   	FROM development_events d " + "    	WHERE "
                    + "	     d.event_type = 'CS' " + "	OR   d.event_type = 'AS' "
                    + "	OR   d.event_type = 'RS' " + "	OR   d.event_type = 'DS' "
                    + "	OR   d.event_type = 'LS' " + "   	); \n";

            strUpdate += " INSERT INTO parcel_cost_xref( "
                    + " SELECT de.new_pecas_parcel_num, xref.cost_schedule_id, xref.year_effective "
                    + " FROM parcel_cost_xref xref, development_events de "
                    + " WHERE xref.pecas_parcel_num=de.original_pecas_parcel_num " + " AND ( "
                    + "     	 event_type = 'CS' " + "	OR   event_type = 'AS' "
                    + "	OR   event_type = 'RS' " + "	OR   event_type = 'DS' "
                    + "	OR   event_type = 'LS' ) " + "	); \n";

            strUpdate += " INSERT INTO parcel_fee_xref( "
                    + " SELECT de.new_pecas_parcel_num, xref.fee_schedule_id, xref.year_effective "
                    + " FROM parcel_fee_xref xref, development_events de "
                    + " WHERE xref.pecas_parcel_num=de.original_pecas_parcel_num " + " AND ( "
                    + "     	 event_type = 'CS' " + "	OR   event_type = 'AS' "
                    + "	OR   event_type = 'RS' " + "	OR   event_type = 'DS' "
                    + "	OR   event_type = 'LS' ) " + "	); \n";

            strUpdate += " INSERT INTO parcel_zoning_xref( "
                    + " SELECT de.new_pecas_parcel_num, xref.zoning_rules_code, xref.year_effective "
                    + " FROM parcel_zoning_xref xref, development_events de "
                    + " WHERE xref.pecas_parcel_num=de.original_pecas_parcel_num " + " AND ( "
                    + "     	 event_type = 'CS' " + "	OR   event_type = 'AS' "
                    + "	OR   event_type = 'RS' " + "	OR   event_type = 'DS' "
                    + "	OR   event_type = 'LS' ) " + "	); \n";

            strUpdate += " INSERT INTO local_effect_distances( "
                    + " SELECT de.new_pecas_parcel_num, dist.local_effect_id, dist.local_effect_distance, dist.year_effective"
                    + " FROM local_effect_distances dist, development_events de "
                    + " WHERE dist.pecas_parcel_num=de.original_pecas_parcel_num " + " AND ( "
                    + "     	 event_type = 'CS' " + "	OR   event_type = 'AS' "
                    + "	OR   event_type = 'RS' " + "	OR   event_type = 'DS' "
                    + "	OR   event_type = 'LS' ) " + "	); \n";

            // System.out.println(strUpdate);

            statement.execute(strUpdate);
            session.commit();

        } catch (final SQLException e)
        {
            logger.fatal("Can't apply development events", e);
            throw new RuntimeException("Can't apply development events", e);
        } catch (final FileNotFoundException e)
        {
            logger.fatal("Can't read development events file", e);
            throw new RuntimeException("Can't read development events file", e);
        } catch (final IOException e)
        {
            logger.fatal("Can't read development events file", e);
            throw new RuntimeException("Can't read development events file", e);
        }
    }

    @Override
    public DevelopmentLog getDevelopmentLogger()
    {
        if (developmentLogger != null)
        {
            return developmentLogger;
        }
        developmentLogger = new DevelopmentLog();
        developmentLogger.open(logFileNameAndPath);
        return developmentLogger;
    }
}
