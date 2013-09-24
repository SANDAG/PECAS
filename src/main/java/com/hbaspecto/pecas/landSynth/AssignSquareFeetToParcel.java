/*
 * Created on Dec 2, 2003
 * 
 * Copyright 2003-2008 HBA Specto Incorporated
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
package com.hbaspecto.pecas.landSynth;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import com.hbaspecto.pecas.land.LoadingQueue;
import com.pb.common.util.ResourceUtil;

/**
 * @author jabraham
 * 
 *         To change the template for this generated type comment go to
 *         Window>Preferences>Java>Code Generation>Code and Comments
 */
public abstract class AssignSquareFeetToParcel
{
    // parameters

    // initialization
    protected static ResourceBundle                props;
    private Connection                             connection                    = null;
    static String                                  database;

    static String                                  user                          = null;
    static String                                  password                      = null;

    private PreparedStatement                      updateStatement;

    // Parcel Fields
    String                                         parcelZoneColumnName          = null;
    String                                         parcelIdField                 = null;
    String                                         areaColumnName                = null;
    String                                         initialFARColumnName          = null;
    String                                         parcelGeomName                = null;

    // Floorspace Fields
    static String                                  floorspaceZoneColumnName      = null;
    String                                         floorspaceSpaceTypeColumnName = null;

    // TAZ Fields
    String                                         tazGeomName                   = null;
    String                                         tazNumberName                 = null;

    double                                         bufferSize                    = 0;

    // PrintWriter errorLog = null;
    boolean                                        isIntegerSpaceTypeCode;
    ResultSet                                      sqFeetInventory;
    Hashtable                                      sqFtTypes                     = new Hashtable();
    Hashtable                                      zoningTypes                   = new Hashtable();
    int                                            typeCount                     = 0;
    // ArrayList zoneNumbers = new ArrayList();
    static String                                  sqftInventoryTableName        = null;
    String                                         matchCoeffTableName           = null;
    String                                         parcelTableName               = null;
    String                                         tazTableName                  = null;
    TreeMap                                        inventoryMap                  = new TreeMap();
    private final Hashtable<Integer, ParcelScorer> parcelScorers                 = new Hashtable<Integer, ParcelScorer>();
    String[]                                       typeNames                     = null;
    Hashtable<Integer, List<ParcelInterface>>      sortedParcelLists;
    private ParcelResultSet                        theParcelsSQLResultSet;
    static Logger                                  logger                        = Logger.getLogger(AssignSquareFeetToParcel.class
                                                                                         .getName());

    // This property returns a readOnly connection. getConnection returns
    // readOnly connections. Not involved in updating.
    Connection getConnection(String source)
    {
        try
        {
            if (connection == null)
            {
                connection = getNewConnection(props, true, false, source);
            } else
            {
                if (connection.isClosed())
                {
                    connection = getNewConnection(props, true, false, source);
                }
            }
        } catch (final Exception e)
        {
            logger.fatal("AssignSquareFeetToParcel can't connect to database", e);
            throw new RuntimeException("AssignSquareFeetToParcel can't connect to database", e);
        }
        return connection;
    }

    public static void closeConnection(Connection conn, String message) throws SQLException
    {
        if (!conn.isClosed())
        {
            if (!conn.isReadOnly() && !conn.getAutoCommit())
            {
                logger.info("Commit changes before closing the connection.");
                conn.commit();
            }
            logger.info(message);
            conn.close();
        }

    }

    protected void setupDatabaseAccess(ResourceBundle props)
    {
        connection = getNewConnection(props, true, false, "Initial setup");
    }

    /**
     * @param args
     */
    public static Properties loadProperties(String[] args)
    {

        FileInputStream fin = null;
        final Properties props = new Properties();
        try
        {
            fin = new FileInputStream(args[0]);
        } catch (final FileNotFoundException e)
        {
            System.out
                    .println("error: be sure to put the location of the properties file on the command line");
            e.printStackTrace();
            System.exit(-1);
        }
        try
        {
            props.load(fin);
        } catch (final IOException e)
        {
            System.out.println("Error reading properties file " + args[0]);
            System.exit(-1);
        }
        return props;
    }

    /**
	 * 
	 */
    public static Connection getNewConnection(ResourceBundle props2, boolean readOnly,
            boolean autoCommit, String source)
    {
        if (source == null)
        {
            source = "";
        }

        Connection connection = null;
        String databaseDriver = "";
        try
        {
            databaseDriver = ResourceUtil.checkAndGetProperty(props2, "JDBCDriver");
            Class.forName(databaseDriver).newInstance();
            database = ResourceUtil.checkAndGetProperty(props2, "Database");
            final String user = ResourceUtil.checkAndGetProperty(props2, "DatabaseUser");
            final String password = ResourceUtil.checkAndGetProperty(props2, "DatabasePassword");
            if (user == null)
            {
                logger.warn("Connecting to database without username for " + source);
                connection = DriverManager.getConnection(ResourceUtil.checkAndGetProperty(props2,
                        "Database"));
                connection.setAutoCommit(autoCommit);
                connection.setReadOnly(readOnly);
            } else
            {
                logger.info("Connecting to database with username " + user + " and password "
                        + password + " for " + source);
                connection = DriverManager.getConnection(
                        ResourceUtil.checkAndGetProperty(props2, "Database"), user, password);
                connection.setAutoCommit(autoCommit);
                connection.setReadOnly(readOnly);
            }
        } catch (final Exception e)
        {
            System.out.println("error opening JDBC connection to database " + databaseDriver + " "
                    + database);
            System.out.println(e.toString());
            e.printStackTrace();
            System.exit(-1);
        }
        return connection;
    }

    public void initialize()
    {
        // try {
        // errorLog = new PrintWriter(new FileOutputStream(new
        // File(props.getProperty("OutputFile"))));
        // } catch (Exception e) {
        // System.out.println("error opening error log file "+props.getProperty("OutputFile"));
        // System.out.println("be sure to set OutputFile property in properties file");
        // System.exit(-1);
        // }

        parcelZoneColumnName = ResourceUtil.checkAndGetProperty(props, "ParcelZoneField");
        areaColumnName = ResourceUtil.checkAndGetProperty(props, "LandAreaField");
        parcelIdField = ResourceUtil.checkAndGetProperty(props, "ParcelIdField");
        parcelGeomName = ResourceUtil.getProperty(props, "ParcelGeomField");
        if (parcelGeomName != null)
        {
            parcelGeomName = parcelGeomName.trim();
        }

        // some more initializers

        sqftInventoryTableName = ResourceUtil.checkAndGetProperty(props, "SqftInventoryTable");
        floorspaceZoneColumnName = ResourceUtil.checkAndGetProperty(props, "FloorspaceZoneField");
        floorspaceSpaceTypeColumnName = ResourceUtil.checkAndGetProperty(props,
                "FloorspaceSpaceTypeField");

        matchCoeffTableName = ResourceUtil.checkAndGetProperty(props, "MatchCoeffTableName");
        parcelTableName = ResourceUtil.checkAndGetProperty(props, "ParcelTableName");

        tazTableName = ResourceUtil.checkAndGetProperty(props, "TazTableName");
        tazGeomName = ResourceUtil.checkAndGetProperty(props, "TazGeomField");
        tazNumberName = ResourceUtil.checkAndGetProperty(props, "TazNumberField");

        isIntegerSpaceTypeCode = ResourceUtil.getBooleanProperty(props, "IntegerSpaceTypeCode",
                true);
        // optional entry
        initialFARColumnName = ResourceUtil.checkAndGetProperty(props, "InitialFARField");

        bufferSize = ResourceUtil.getDoubleProperty(props, "Buffer", 0);
        // FIXME don't hard code "space_type_id" column name
        // FIXME: Check location of initialization
        ParcelInMemory.initializeLookupCoverageType(
                getConnection("initializing Lookup for coverageTypes"), sqftInventoryTableName,
                floorspaceSpaceTypeColumnName, matchCoeffTableName, "pecastype");
    }

    static class SizeAndChunk
    {
        double size;
        double chunk;
    }

    public void setUpInventory()
    {

        try
        {
            // Statement getSqFtStatement =
            // conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_READ_ONLY);
            final Statement getSqFtStatement = getConnection("SetupInventory").createStatement(
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY); // SQLite
                                                                              // only
            // supports
            // TYPE_FORWARD_ONLY
            // cursors

            // Added by Eric
            getSqFtStatement.setQueryTimeout(0);

            // get a list of the types and a list of the zones, could use the
            // database to group by gridcode and zoneColumnName
            sqFeetInventory = getSqFtStatement.executeQuery("SELECT * FROM "
                    + sqftInventoryTableName);
            while (sqFeetInventory.next())
            {
                String type = null;
                try
                {

                    // type = sqFeetInventory.getString("gridcode"); //why it
                    // was hardcoded here!!
                    type = sqFeetInventory.getString(floorspaceSpaceTypeColumnName);
                } catch (final SQLException e)
                {
                    type = null;
                }
                if (type == null)
                {
                    final String error = "Can't find column named gridcode in table "
                            + sqftInventoryTableName;
                    logger.fatal(error);
                    throw new RuntimeException(error);
                }
                if (!sqFtTypes.containsKey(type))
                {
                    sqFtTypes.put(type, new Integer(typeCount++));
                }

                Integer zoneNumber = null;
                try
                {
                    zoneNumber = new Integer(sqFeetInventory.getInt(floorspaceZoneColumnName));
                } catch (final SQLException e)
                {
                    zoneNumber = null;
                }
                if (zoneNumber == null)
                {
                    final String error = "Can't find column " + floorspaceZoneColumnName
                            + " in table " + sqftInventoryTableName;
                    logger.fatal(error);
                    throw new RuntimeException(error);
                }
            }

            // create array of inventory of square feet in a map of double
            // arrays mapped by zone number

            sqFeetInventory.close();
            sqFeetInventory = getSqFtStatement.executeQuery("SELECT * FROM "
                    + sqftInventoryTableName);

            sqFeetInventory.next();

            typeNames = new String[typeCount];
            do
            {
                final String type = sqFeetInventory.getString(floorspaceSpaceTypeColumnName);
                final int typeIndex = ((Integer) sqFtTypes.get(type)).intValue();
                typeNames[typeIndex] = type;
                final double amount = sqFeetInventory.getDouble("QUANTITY");
                final double chunkSizeFromTable = sqFeetInventory.getDouble("chunksize");
                final Integer zoneNum = new Integer(
                        sqFeetInventory.getInt(floorspaceZoneColumnName));
                SizeAndChunk[] inventoryForZone = (SizeAndChunk[]) inventoryMap.get(zoneNum);
                if (inventoryForZone == null)
                {
                    inventoryForZone = new SizeAndChunk[typeCount];
                    for (int i = 0; i < typeCount; i++)
                    {
                        inventoryForZone[i] = new SizeAndChunk();
                    }
                    inventoryMap.put(zoneNum, inventoryForZone);
                }
                inventoryForZone[typeIndex].size += amount;
                inventoryForZone[typeIndex].chunk = chunkSizeFromTable;

            } while (sqFeetInventory.next());

        } catch (final Exception e)
        {
            logger.fatal("Error in setting up square feet inventory");
            logger.fatal(e);
            throw new RuntimeException(e);
        }
    }

    public void setUpSorters()
    {
        try
        {
            // create parcel sorters
            final Connection localConn = getConnection("setupSorters");

            final Statement getMatchStatement = localConn.createStatement();
            // Added by Eric
            getMatchStatement.setQueryTimeout(0);
            final ResultSet coverageTypes = getMatchStatement.executeQuery("SELECT PECASTYPE FROM "
                    + matchCoeffTableName + " GROUP BY PECASTYPE");

            int coverageType;

            while (coverageTypes.next())
            {
                // one sorter for each coverage type

                if (isIntegerSpaceTypeCode)
                {
                    coverageType = coverageTypes.getInt("PECASTYPE");
                } else
                {
                    coverageType = '_'; // EMPTY COVERAGE PLACEHOLDER for char
                    // is '_'
                    final String str = coverageTypes.getString("PECASTYPE").trim();
                    if (!str.isEmpty())
                    {
                        coverageType = str.charAt(0); // OK
                    }
                }

                final ParcelScorer ps = new ParcelScorer(coverageType, isIntegerSpaceTypeCode,
                        props);
                final Statement statement1 = localConn.createStatement();

                // Added by Eric
                statement1.setQueryTimeout(0);
                String sqlString;
                if (isIntegerSpaceTypeCode)
                {
                    sqlString = "SELECT FIELDNAME FROM " + matchCoeffTableName
                            + " WHERE PECASTYPE=" + coverageType + " GROUP BY FIELDNAME";
                } else
                {
                    sqlString = "SELECT FIELDNAME FROM " + matchCoeffTableName
                            + " WHERE PECASTYPE='" + (char) coverageType + "' GROUP BY FIELDNAME";
                }

                final ResultSet fieldNames = statement1.executeQuery(sqlString);
                while (fieldNames.next())
                {
                    // one hintlist for each field in the parcel database that
                    // we are using for a hint
                    final String field = fieldNames.getString("FIELDNAME");
                    // Statement statement2 =
                    // conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_READ_ONLY);
                    final Statement statement2 = localConn.createStatement(
                            ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                    // Added by Eric
                    statement2.setQueryTimeout(0);

                    if (isIntegerSpaceTypeCode)
                    {
                        sqlString = "SELECT * FROM " + matchCoeffTableName + " WHERE FIELDNAME='"
                                + field + "' AND PECASTYPE=" + coverageType;
                    } else
                    {
                        sqlString = "SELECT * FROM " + matchCoeffTableName + " WHERE FIELDNAME='"
                                + field + "' AND PECASTYPE='" + (char) coverageType + "'";
                    }

                    final ResultSet coeffs = statement2.executeQuery(sqlString);

                    final ArrayList<String> fieldsList = new ArrayList<String>();
                    final ArrayList<Double> coeffValuesList = new ArrayList<Double>();
                    final ArrayList<Double> farCoeffsList = new ArrayList<Double>();

                    while (coeffs.next())
                    {
                        fieldsList.add(coeffs.getString("FIELDVALUE"));
                        coeffValuesList.add(coeffs.getDouble("MATCH"));
                        farCoeffsList.add(coeffs.getDouble("FARTARGET"));
                    }

                    statement2.close();

                    /*
                     * //coeffs.last(); int number = 2 ;// coeffs.getRow();
                     * //coeffs.first();
                     * 
                     * String[] fields = new String[number]; double[]
                     * coeffValues = new double[number]; double[] farCoeffs =
                     * new double[number]; int fieldNum = 0; do {
                     * fields[fieldNum] = coeffs.getString("FIELDVALUE");
                     * coeffValues[fieldNum] = coeffs.getDouble("MATCH");
                     * farCoeffs[fieldNum] = coeffs.getDouble("FARTARGET");
                     * fieldNum++; } while (coeffs.next());
                     */
                    final String[] fields = new String[fieldsList.size()];
                    final double[] coeffValues = new double[coeffValuesList.size()];
                    final double[] farCoeffs = new double[farCoeffsList.size()];

                    for (int i = 0; i < coeffValuesList.size(); i++)
                    {
                        fields[i] = fieldsList.get(i);
                        coeffValues[i] = coeffValuesList.get(i);
                        farCoeffs[i] = farCoeffsList.get(i);
                    }

                    ps.addHint(new ParcelScorer.HintList(field, fields, coeffValues, farCoeffs));

                } // end of while
                parcelScorers.put(coverageType, ps);
                statement1.close();
            }
            final String msg = "Closing Initial setup Connection.";
            closeConnection(localConn, msg);
        } catch (final Exception e)
        {
            System.out.println("Error in sq feet to grid");
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private final LoadingQueue<Integer> zoneNumberQueue;

    AssignSquareFeetToParcel(LoadingQueue<Integer> queue)
    {
        zoneNumberQueue = queue;
    }

    public void assignSquareFeet()
    {
        try
        {
            // get next zone number from zone number queue
            int zoneConuter = 0;
            Integer zoneNumberInteger = null;
            do
            {
                zoneNumberInteger = zoneNumberQueue.getNext();
                if (zoneNumberInteger != null)
                {
                    zoneConuter++;

                    final int taz = zoneNumberInteger.intValue();
                    assignSquareFeetForTAZ(zoneConuter, taz);

                }
            } while (zoneNumberInteger != null);
            logger.info("TAZ Queue is empty. Thread should be ending.");
        } catch (final Exception e)
        {
            System.out.println("Error in sq feet to grid");
            System.out.println(e);
            e.printStackTrace();
        } catch (final OutOfMemoryError e)
        {
            System.out.println("Error in sq feet to grid");
            System.out.println(e);
            e.printStackTrace();
        }
    }

    /**
     * @param zoneNumber
     *            : A sequential number to indicate the number of zones
     *            processed by the thread.
     * @param stmt
     *            :
     * @param taz
     *            : zone number to be processed.
     * @throws SQLException
     */
    private void assignSquareFeetForTAZ(int zoneNumber, Integer taz) throws SQLException
    {
        // make our lists of sorted parcels
        sortedParcelLists = new Hashtable<Integer, List<ParcelInterface>>();

        final Enumeration<Integer> coverageTypeEnumeration = parcelScorers.keys();
        int coverageType;

        // Set current TAZ.
        final Iterator<ParcelScorer> myIterator = parcelScorers.values().iterator();
        while (myIterator.hasNext())
        {
            final ParcelScorer ps = myIterator.next();
            ps.setCurrentTaz(taz);
        }

        while (coverageTypeEnumeration.hasMoreElements())
        {
            coverageType = coverageTypeEnumeration.nextElement();
            sortedParcelLists.put(coverageType, new ArrayList<ParcelInterface>());
        }

        logger.info("Progress: now starting zone " + taz + "(sequence " + zoneNumber
                + " for thread)");

        assignSquareFeetForTAZ(taz);

        // this is necessary to delete cache of scores
        for (final ParcelScorer ps : parcelScorers.values())
        {
            ps.clearScoreRecord();
        }

        sortedParcelLists.clear();

        // Added by Eric
        sortedParcelLists = null;

    }

    private void assignSquareFeetForTAZ(Integer taz) throws SQLException
    {
        final SizeAndChunk[] inventoryForZone = (SizeAndChunk[]) inventoryMap.get(taz);
        if (inventoryForZone != null)
        {

            if (!putParcelsInArray(taz))
            {
                finishedProcessingTAZ();
                return;
            }
            // now sort each of the lists;
            final Enumeration coverageTypeIterator = sortedParcelLists.keys();
            while (coverageTypeIterator.hasMoreElements())
            {
                final int coverageType = (Integer) coverageTypeIterator.nextElement();
                // System.out.println((char) coverageType );
                logger.info("sorting parcels for usage " + coverageType + " in "
                        + parcelZoneColumnName + " " + taz);
                final List parcelList = sortedParcelLists.get(coverageType);
                final ParcelScorer parcelScorer = parcelScorers.get(coverageType);
                Collections.sort(parcelList, parcelScorer);
            }

            // set up the ratios so that we can go through the types maintaining
            // the same ratios,
            // so that a use with only a small amount of square feet doesn't get
            // first dibs on
            // the best parcels in the zone
            final double[] ratiosForZone = new double[typeCount];
            double totalInventoryForZone = 0;
            for (int type = 0; type < typeCount; type++)
            {
                totalInventoryForZone += inventoryForZone[type].size;
            }
            for (int type = 0; type < typeCount; type++)
            {
                ratiosForZone[type] = inventoryForZone[type].size / totalInventoryForZone;
            }

            // now go through the types assigning chunks

            // TODO only assign x% the first round; also allow spillover into
            // other TAZ's
            double lastSquareFeetReported = -1;
            boolean done = true;
            do
            { // while !done
                double remainingSquareFeet = 0;
                for (int type = 0; type < typeCount; type++)
                {
                    remainingSquareFeet += inventoryForZone[type].size;
                }
                done = true;
                for (int type = 0; type < typeCount; type++)
                {
                    while (inventoryForZone[type].size > 0
                            && inventoryForZone[type].size / remainingSquareFeet >= ratiosForZone[type])
                    {

                        done = false; // keep going until we've assigned
                        // everything
                        String typeName = typeNames[type];
                        float amount = (float) inventoryForZone[type].chunk;
                        if (inventoryForZone[type].size < amount)
                        {
                            amount = (float) inventoryForZone[type].size;
                            inventoryForZone[type].size = 0;
                            remainingSquareFeet -= amount;
                        } else
                        {
                            inventoryForZone[type].size -= amount;
                            remainingSquareFeet -= amount;
                        }

                        // find the right sorted list
                        int intTypeName = 0;
                        if (isIntegerSpaceTypeCode)
                        {
                            intTypeName = Integer.valueOf(typeName);
                        } else
                        {
                            intTypeName = '_'; // EMPTY COVERAGE PLACEHOLDER for
                            // char is '_'
                            typeName = typeName.trim();
                            if (!typeName.isEmpty())
                            {
                                intTypeName = typeName.charAt(0); // OK
                            }
                        }

                        final List parcelList = sortedParcelLists.get(intTypeName);
                        if (parcelList == null)
                        {
                            logger.error("No parcel list for " + typeName);
                        } else
                        {
                            if (parcelList.size() == 0)
                            {
                                logger.error("NoSuitableParcel," + typeName + "," + taz + ","
                                        + amount);
                            } else
                            {
                                final ParcelInterface theParcel = (ParcelInterface) parcelList
                                        .get(parcelList.size() - 1);

                                removeForRescoring(theParcel);
                                // some debugging information, SACRAMENTO COUNTY
                                // SPECIFIC
                                // ParcelLinkToDatabase firstParcel
                                // =(ParcelLinkToDatabase) parcelList.get(0);
                                // ParcelScorer ps = (ParcelScorer)
                                // parcelSorters.get(typeName);
                                // System.out.println("In taz "+taz+" for type "+typeNames[type]);
                                // System.out.println("first score is "+ps.score(firstParcel)+" for parcel of type "+firstParcel.getValue("Vacancy"));
                                // System.out.println("last score is "+ps.score(theParcel)+" for parcel of type "+theParcel.getValue("Vacancy"));

                                int currentCoverage = theParcel.getCoverage();

                                if (theParcel.isVacantCoverege())
                                {
                                    theParcel.setCoverage(typeNames[type]);

                                    if (isIntegerSpaceTypeCode)
                                    {
                                        currentCoverage = Integer.parseInt(typeNames[type]);
                                    } else
                                    {
                                        currentCoverage = typeNames[type].trim().charAt(0); // OK!
                                    }
                                }
                                if (theParcel.isSameSpaceType(typeNames[type]))
                                {
                                    theParcel.addSqFtAssigned(amount);
                                } else
                                {
                                    // now we have to deal with the fact that
                                    // the
                                    // best scored parcel
                                    // already has another coverage assigned.
                                    // The
                                    // strategy is to
                                    // find another parcel with blank or current
                                    // coverage and see if it's better
                                    // to put the new chunk of floorspace onto
                                    // the
                                    // other parcel,
                                    // or whether to put the new chunk of
                                    // floorspace
                                    // onto the original
                                    // best scored parcel, and swap the existing
                                    // assigned floorspace
                                    // so that each parcel still only has one
                                    // type
                                    // of floorspace
                                    ParcelInterface anotherParcel = null;
                                    for (int i = parcelList.size() - 1; i >= 0; i--)
                                    {
                                        anotherParcel = (ParcelInterface) parcelList.get(i);
                                        if (anotherParcel.isVacantCoverege()
                                                || anotherParcel.isSameSpaceType(typeNames[type]))
                                        {
                                            break;
                                        }
                                        anotherParcel = null;
                                    }
                                    if (anotherParcel == null)
                                    {

                                        logger.error("NotEnoughParcelsForCoverageTypesInTaz,"
                                                + typeName + "," + taz + "," + amount);
                                    } else
                                    {
                                        possibleSwapSpaceTypes(theParcel, anotherParcel,
                                                currentCoverage, type, amount);

                                    }

                                } // end of else
                                  // now the parcel has changed, it needs to
                                  // be
                                  // reinserted into each of the sorted lists
                                rescoreParcel(theParcel);

                            }
                        }
                    }
                }
                if (lastSquareFeetReported == -1)
                {
                    lastSquareFeetReported = remainingSquareFeet;
                    logger.info("******************************************");
                    logger.info("Assigning " + remainingSquareFeet + " in " + parcelZoneColumnName
                            + " " + taz);
                } else if (lastSquareFeetReported - remainingSquareFeet > 50000)
                {
                    logger.info(remainingSquareFeet
                            + " sqft of buildings still to be assigned in taz " + taz);
                    lastSquareFeetReported = remainingSquareFeet;
                }
            } while (!done);
            finishedProcessingTAZ();
        }
    }

    /**
     * @param theParcel
     * @param anotherParcel
     * @param currentCoverage
     * @param type
     * @param amount
     */
    private void possibleSwapSpaceTypes(ParcelInterface theParcel, ParcelInterface anotherParcel,
            int currentCoverage, int type, float amount)
    {
        // first try adding space to parcel without
        // swapping coverage
        removeForRescoring(anotherParcel);
        if (anotherParcel.isVacantCoverege())
        {
            anotherParcel.setCoverage(typeNames[type]);
        }

        anotherParcel.addSqFtAssigned(amount);
        final ParcelScorer addingTypeScorer = getScorer(typeNames[type]);
        final ParcelScorer bumpingTypeScorer = getScorer(currentCoverage);
        final double score1 = addingTypeScorer.score(anotherParcel)
                + bumpingTypeScorer.score(theParcel);

        // now try swapping them
        final float inventorySwap = anotherParcel.getQuantity();
        // /
        ;
        // / This variable includes either the string of an integer coverage
        // value such as "11" or a character coverage value, e.g. "a" or "b".
        // This is based on whether
        // / the gridcode\coveragetype is of type integer or character.

        String strCurrentCoverage;
        if (isIntegerSpaceTypeCode)
        {
            // convert int coverage (e.g. 65) to string ("65")
            strCurrentCoverage = Integer.toString(currentCoverage);
        } else
        {
            // convert int (e.g. 65) to char ('A'), then to string ("A")
            strCurrentCoverage = Character.toString((char) currentCoverage);
        }
        anotherParcel.setCoverage(strCurrentCoverage);
        anotherParcel.setQuantity(theParcel.getQuantity());
        theParcel.setCoverage(typeNames[type]);
        theParcel.setQuantity(inventorySwap);

        final double score2 = addingTypeScorer.score(theParcel)
                + bumpingTypeScorer.score(anotherParcel);
        if (score1 > score2)
        {
            // ok, the first option is better, put
            // it back
            theParcel.setQuantity(anotherParcel.getQuantity());
            theParcel.setCoverage(strCurrentCoverage);
            anotherParcel.setQuantity(inventorySwap);
            anotherParcel.setCoverage(typeNames[type]);
        }

        rescoreParcel(anotherParcel);
    }

    protected abstract boolean putParcelsInArray(Integer taz) throws SQLException;

    protected void finishedProcessingTAZ() throws SQLException
    {
        theParcelsSQLResultSet.close();
    }

    private ParcelScorer getScorer(int currentCoverage)
    {
        String coverageName = null;
        for (int i = 0; i < typeNames.length; i++)
        {
            if (isIntegerSpaceTypeCode)
            {
                if (Integer.parseInt(typeNames[i]) == currentCoverage)
                {
                    // just a check for duplicates
                    if (coverageName != null)
                    {
                        throw new RuntimeException("Coverage " + coverageName + " and "
                                + typeNames[i] + " both start with the same character...error");
                    }
                    coverageName = typeNames[i];
                }
            } else
            { // Coverage is a character
                if (typeNames[i].charAt(0) == currentCoverage)
                {
                    // just a check for duplicates
                    if (coverageName != null)
                    {
                        throw new RuntimeException("Coverage " + coverageName + " and "
                                + typeNames[i] + " both start with the same character...error");
                    }
                    coverageName = typeNames[i];
                }
            }
        }
        return getScorer(coverageName);
    }

    private ParcelScorer getScorer(String string)
    {
        ParcelScorer result;
        if (isIntegerSpaceTypeCode)
        {
            result = parcelScorers.get(Integer.valueOf(string));
        } else
        {
            result = parcelScorers.get(Integer.valueOf(string.charAt(0)));
        }
        return result;
    }

    /**
     * If a parcel has changed, it's scores may have changed as well. This
     * method removes the parcel from each of the sorted lists and reinserts it
     * so that it is again in score order.
     * 
     * @param theParcel
     */
    public void rescoreParcel(ParcelInterface theParcel)
    {
        final Enumeration spaceTypeIterator = sortedParcelLists.keys();
        while (spaceTypeIterator.hasMoreElements())
        {
            final int spaceType = (Integer) spaceTypeIterator.nextElement();
            final List parcelList = sortedParcelLists.get(spaceType);

            /* Now reinsert the parcel into the sorted list */
            final ParcelScorer parcelScorer = parcelScorers.get(spaceType);
            final int location = Collections.binarySearch(parcelList, theParcel, parcelScorer);
            if (location >= 0)
            {
                parcelList.add(location, theParcel);
            } else
            {
                parcelList.add(-(location + 1), theParcel);
            }
        }
    }

    private void removeForRescoring(ParcelInterface theParcel)
    {
        final Enumeration spaceTypeIterator = sortedParcelLists.keys();
        while (spaceTypeIterator.hasMoreElements())
        {
            final int spaceType = (Integer) spaceTypeIterator.nextElement();
            final List parcelList = sortedParcelLists.get(spaceType);
            final int size = parcelList.size();
            if (!parcelList.remove(theParcel))
            {
                throw new Error("Can't remove " + theParcel + " from list " + parcelList);
            }
            if (parcelList.remove(theParcel))
            {
                throw new Error("Parcel " + theParcel + " was in list " + parcelList
                        + " more than once!");
            }
        }
    }

}
