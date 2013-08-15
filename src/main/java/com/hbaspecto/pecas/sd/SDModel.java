/*
 * Created on 28-Oct-2007
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

import java.sql.Connection;
import java.util.List;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import simpleorm.dataset.SQuery;
import simpleorm.sessionjdbc.SSessionJdbc;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TableDataSetCollection;
import com.pb.common.util.ResourceUtil;
import com.hbaspecto.pecas.land.LandInventory;
import com.hbaspecto.pecas.sd.orm.SpaceTypesI_gen;




/**
 * @author Abdel
 *	An example of a SD model indepedent of what type of land inventory is used.
 * it is an absrtact class that has some common code that can be used by lot of different implementation. 
 */
public abstract class SDModel {
    protected static transient Logger logger = Logger.getLogger(SDModel.class);
    protected static ResourceBundle rbSD = null;
    
    protected static int currentYear;
    protected static int baseYear;
    protected static String landDatabaseDriver;
    protected static String landDatabaseSpecifier;
    //protected static String landDatabaseTable;
    protected static String inputDatabaseDriver;
    static Connection inputConnection;
    protected static String inputDatabaseSpecifier;
    protected static String logFilePath;
    protected static boolean excelInputDatabase;
    protected static boolean useSQLInputs=false;
    protected static String  inputPath;
    static String referencePath;
    protected static String outputPath;
    protected static String gridPath;
    

    protected TableDataSetCollection outputDatabase = null;
    protected TableDataSet zoneNumbers = null;
    protected LandInventory land;
    TableDataSetCollection referenceDatabase;
//    protected TableDataSet developmentTypesI;
//    protected TableDataSet transitionConstantsI;
    protected static TableDataSet realDevelopmentTypesI;
//    protected HashtableAlphaToBeta floorspaceZoneCrossref;
    
    public void runSD(int currentYear, int baseYear, ResourceBundle rb) {
        logger.info("Doing development model for year "+ currentYear);
        
        rbSD = rb;
        initZoningScheme(currentYear, baseYear);
        simulateDevelopment();
        land.applyDevelopmentChanges();
        writeOutInventoryTable(realDevelopmentTypesI);

//        TableDataSet crossTabbed= TableDataSetCrosstabber.crossTabDataset(outputDatabase, "FloorspaceI", "FloorspaceZone", "Commodity", "Quantity");
//        crossTabbed.setName("ZonalFloorspaceData");
//        outputDatabase.addTableDataSet(crossTabbed);
        outputDatabase.flush();

        outputDatabase.close();
    }
   
    protected static void initZoningScheme(int currentYear, int baseYear) {
        ZoningRulesI.currentYear = currentYear;
        ZoningRulesI.baseYear = baseYear;

        if (ResourceUtil.getProperty(rbSD,"DevelopmentDispersionParameter") != null) {
        	logger.warn("DevelopmentDispersionParameter is set in sd.properties, this is no longer the place to put SD dispersion parameters");
        }

        double interestRate = 0.0722;
        double compounded = Math.pow(1+interestRate,30);
        double amortizationFactor = interestRate *compounded/(compounded -1);
        ZoningRulesI.amortizationFactor=ResourceUtil.getDoubleProperty(rbSD,"AmortizationFactor",amortizationFactor);
        ZoningRulesI.servicingCostPerUnit=ResourceUtil.getDoubleProperty(rbSD,"ServicingCostPerUnit",13.76);
    }
    
    public abstract void setUpLandInventory(String className, int year);
        
    public abstract void setUp();
    
    /**
     * Runs through the inventory simulating development
     * NOTE only works with TableDataSetLandInventory, if your land inventory
     * is of some other type you'll have to write your own code to iterate through
     * all the land.
     */
    public abstract void simulateDevelopment();
    /*{
        TableDataSetLandInventory tdsLand = (TableDataSetLandInventory) land;
        ZoningScheme.openLogFile(logFilePath);
        ZoningScheme currentScheme = null;
        int zoneNumberColumn = zoneNumbers.checkColumnPosition("AlphaZone");
        tdsLand.setToBeforeFirst();
        while (tdsLand.advanceToNext()) {
//            long zoneNumber = (long) zoneNumbers.getValueAt(row,zoneNumberColumn);
//            long[] parcelNumbers = tdsLand.getId2Numbers(zoneNumber);
//            for (long parcelSequence=0;parcelSequence<parcelNumbers.length;parcelSequence++) {
//                long parcel = parcelNumbers[(int) parcelSequence];
               currentScheme = ZoningScheme.getZoningSchemeByIndex(land.getZoningRulesCode());
               if (currentScheme == null) {
                   ZoningScheme.logBadZoning(land);
               } else {
                    currentScheme.doDevelopment(land);
               }
        }
        // might need this next line just to be safe.
        tdsLand.writeAndReclaimMemory();
        ZoningScheme.closeLogFile();
        
    }
    */
    public void writeOutInventoryTable(TableDataSet landTypes) {
        TableDataSet landInventoryTable = land.summarizeInventory();
        landInventoryTable.setName("FloorspaceI");
        outputDatabase.addTableDataSet(landInventoryTable);
//        TableDataSet crossTabbed = TableDataSetCrosstabber.crossTabDataset(outputDatabase, landInventoryTable.getName(), "TAZ", "Commodity", "Quantity");
//        crossTabbed.setName("ZonalFloorspace");
//        outputDatabase.addTableDataSet(crossTabbed);
        outputDatabase.flush();
    }

    public SpaceTypesI[] setUpDevelopmentTypes() {
    	
    	SSessionJdbc session = SSessionJdbc.getThreadLocalSession();

    	SQuery<SpaceTypesI> devQry = new SQuery<SpaceTypesI>(SpaceTypesI_gen.meta);                      
    	List<SpaceTypesI> dtypes = session.query(devQry);
    	   	
        SpaceTypesI[] d = new SpaceTypesI[dtypes.size()];
        SpaceTypesI[] dTypes = dtypes.toArray(d);
    
        return dTypes;
    }
    
//    protected void readFloorspaceZones(TableDataSet alphaZoneTable) {
//        logger.info("Reading Floorspace Zones");
//        floorspaceZoneCrossref = new HashtableAlphaToBeta();
//        for (int zRow = 1; zRow <= alphaZoneTable.getRowCount(); zRow++) {
//            Integer floorspaceZone = new Integer( (int) alphaZoneTable.getValueAt(zRow, "AlphaZone"));
//            int pecasZoneInt = (int) alphaZoneTable.getValueAt(zRow,"PECASZone");
//            AbstractZone pecasZone = AbstractZone.findZoneByUserNumber(pecasZoneInt);
//            if (pecasZone != null) {
//                // don't add in bogus records -- there might be land use zones that aren't covered by the spatial IO model
//                Integer pecasZoneInteger = new Integer(pecasZoneInt);
//                floorspaceZoneCrossref.put(floorspaceZone,pecasZoneInteger);
//            } else {
//                logger.warn("Bad spatial IO zone number "+pecasZoneInt+" in FloorspaceZonesI ... ignoring land use zone "+floorspaceZone.intValue());
//            }
//        }
//    }


    // this is what a main method might look like in a subclass.
//  public static void main(String[] args) {
//  SDModel myLD = new YourImplementationOfLDModel();
//  int currentYear;
//  try {
//      currentYear = Integer.valueOf(args[0]) + Integer.valueOf(args[1]);
//  } catch (Exception e) {
//      e.printStackTrace();
//      throw new RuntimeException("Put base year and time interval on command line" +
//              "\n For example, 1990 1");
//  }
//  myLD.setUp();
//  myLD.runLD(currentYear);
//}

}
