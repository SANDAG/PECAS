/*
 * Copyright 2005 HBA Specto Incorporated and PB Consult Inc.
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

package com.hbaspecto.pecas.landSynth;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.GeneralDecimalFormat;
import com.pb.common.datafile.JDBCTableReader;
import com.pb.common.datafile.JDBCTableWriter;
import com.pb.common.datafile.MissingValueException;
import com.pb.common.datafile.TableDataReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.TableDataSetCollection;
import com.pb.common.datafile.TableDataSetIndexedValue;
import com.pb.common.datafile.TableDataWriter;
import com.pb.common.matrix.NDimensionalMatrixBalancerDouble;
import com.pb.common.matrix.NDimensionalMatrixDouble;
import com.pb.common.model.Alternative;
import com.pb.common.model.ObservedChoiceProbabilities;
import com.pb.common.sql.JDBCConnection;
import com.pb.common.util.ResourceUtil;


/**
 * This class creates grid cells in a zone based on the aggregate characteristics of the built form in the zone
 * @author John Abraham 
 */
public class GridCellSynthesizer {

    private static Logger logger = Logger.getLogger(GridCellSynthesizer.class);
    protected static long gridCellCounter = 1;

    protected static LandCategory[] landCategories;  //needs to be static since each type needs access to the forestry/protected and ag lands (for borrowing)


     public static void main(String[] args) {
         ResourceBundle ldRb = ResourceUtil.getResourceBundle("synth");
         GridCellSynthesizer me = new GridCellSynthesizer();
         me.initialize(ldRb);
         me.synthesizeGridCells();
     }

    private TableDataSetCollection landData;
    private TableDataSetCollection referenceData;

    ResourceBundle ldRb;

    java.util.Random myRandom;
    private TableDataSet landQuantityTable;
    private TableDataSet yrBuiltProbTable;
    private TableDataSet zoningAcres;
    int[] devTypeZoningCorrespondence;
    private String inventoryTableName;
    private String landQuantityTableName;
    private double gridCellSize;
    private int zoneRow;
    private int taz;
    String[] names = new String[255];
    TableDataWriter myWriter = null;
    /** Description:
     *               ID1 -  taz number
     *               ID2 - grid cell number incremented over all cells (String)
     *               PH4BetaZone - place holder for land use zone (luz) number.
     *               AmountOfLand - grid cell size
     *               DevelopmentType - code for dev type, see lookup table
     *               AmountOfDevelopment - empl. spaces, acres or sqft depending on devtype.
     *               YearBuilt - year of development
     *               ZoningScheme - 1 of 9 categories, see lookup table.
    */
   // String[] gridCellDataTableHeader = {"ID1","ID2", "PH4BetaZone", "AmountOfLand","DevelopmentTypeCode","AmountOfDevelopment","YearBuilt","ZoningSchemeCode"};
    private TableDataSet gridCounts;
    private TableDataSet intensities;
    private TableDataSet zoningTDSxTAZ;
    private HashMap<String,ObservedChoiceProbabilities> zoningChoices;
    private TableDataSet zoningProbabilityMap;
    private HashMap<String,Integer> developmentTypeCodes;
    private TreeMap zoningSchemeCodes;
    private TreeMap zoningSchemeSequentialLookupFromCode;
    private int[] zoningSchemeCodeLookupFromSequential;
    TableDataSetIndexedValue minMaxLookup = new TableDataSetIndexedValue("IntensityRangeI",new String[] {"DevelopmentType"},new String[0],new String[1][1], new int[1][0], "MinIntensity"); 

     public void initialize(ResourceBundle ldRb) {

         myRandom = new java.util.Random();
         this.ldRb = ldRb;
         String myDirectory = ldRb.getString("input.file.path");

         //check to see if the Employment, Residential Acres and Land table should be read using
         //using a JDBC reader or CSVFileReader.  (By default, use CSVFileReader)
         String readerType = ldRb.getString("reader.type");

         TableDataReader myReader = null;

        if(readerType.equalsIgnoreCase("jdbc")) {
            JDBCConnection myConnection = new JDBCConnection(ldRb.getString("land.database"), ldRb.getString("land.jdbcDriver"), 
            		ResourceUtil.getProperty(ldRb, "land.databaseUser",""),
            		ResourceUtil.getProperty(ldRb, "land.databasePassword",""));
            myReader = new JDBCTableReader(myConnection);
            myWriter = new JDBCTableWriter(myConnection);
        }else{
            myReader = new CSVFileReader();
            ((CSVFileReader)myReader).setMyDirectory(myDirectory);
            myWriter = new CSVFileWriter();
            ((CSVFileWriter)myWriter).setMyDirectory(new File(myDirectory));
            ((CSVFileWriter)myWriter).setQuoteStrings(true);
            ((CSVFileWriter)myWriter).setMyDecimalFormat(new GeneralDecimalFormat("0.#########E0",10000000,.001));
        }

         landData = new TableDataSetCollection(myReader,myWriter);

         if (readerType.equalsIgnoreCase("jdbc")) {
             referenceData = landData;
         } else {
             CSVFileReader referenceReader = new CSVFileReader();
             referenceReader.setMyDirectory(ldRb.getString("reference.data"));
             referenceData = new TableDataSetCollection(referenceReader,null);
         }

         //add the land quantity table to the tableDataSetCollection
         landQuantityTableName= ldRb.getString("land.quantities.table");
         landQuantityTable = landData.getTableDataSet(landQuantityTableName);
         //add the floorspace inventory table to the tableDataSetCollection.
         inventoryTableName = ldRb.getString("inventory.table");
         landData.getTableDataSet(inventoryTableName);

         names[11]="ResRLow";
         names[12]="ResRHi";
         names[15]="ResULow";
         names[16]="ResUMed";
         names[17]="ResUHi";
         names[21]="CommRetAccom";
         names[22]="Office";
         names[23]="K12";
         names[24]="GovtInst";
         names[31]="LtInd";
         names[32]="HvyInd";
         names[40]="Mil";
         names[50]="Ag";
         names[60]="ForestPro";
         names[82]="VacNoServ";
         names[83]="VacServ";

        developmentTypeCodes = new HashMap<String,Integer>();
        for (int code=0;code<names.length;code++) {
             if (names[code]!=null) {
                 developmentTypeCodes.put(names[code],new Integer(code));
             }
         }



         gridCellSize = Double.parseDouble(ldRb.getString("land.gridSize"));

         TableDataSet zoningSchemesTable = referenceData.getTableDataSet("ZoningSchemesI");
         int codeColumn = zoningSchemesTable.checkColumnPosition("ZoningSchemeCode");
         int nameColumn = zoningSchemesTable.checkColumnPosition("ZoningScheme");
         zoningSchemeCodes = new TreeMap();

        for (int i =1;i<=zoningSchemesTable.getRowCount();i++) {
             zoningSchemeCodes.put(new Integer((int) zoningSchemesTable.getValueAt(i,codeColumn)),
                     zoningSchemesTable.getStringValueAt(i,nameColumn));
         }

        zoningSchemeSequentialLookupFromCode = new TreeMap();
        zoningSchemeCodeLookupFromSequential = new int[zoningSchemeCodes.keySet().size()];
        Iterator it = zoningSchemeCodes.keySet().iterator();
        int number = 0;
        while (it.hasNext()) {
            Integer i = (Integer) it.next();
            zoningSchemeSequentialLookupFromCode.put(i,new Integer(number));
            zoningSchemeCodeLookupFromSequential[number] = i.intValue();
            number++;
        }

         //devTypes are numbered 1-12, the associated zoning codes are associated with indices 1-12.
     //    devTypeZoningCorrespondence = new int[]{0,1,2,5,4,5,7,7,8,9,12,12,12};

         yrBuiltProbTable = landData.getTableDataSet(ldRb.getString("year.built.probabilities"));
         yrBuiltProbTable.buildIndex(yrBuiltProbTable.checkColumnPosition("TAZ"));

         zoningTDSxTAZ = landData.getTableDataSet("ZoningProbabilityTablesI");
         zoningProbabilityMap = landData.getTableDataSet("ZoningProbabilityMapI");
         buildZoningChoices();
         zoningAcres = landData.getTableDataSet("LandAcresxZoningI");
         

     }

     class ZoningAlternative  implements Alternative {
         double choiceWeight = 0;
         int code;
         boolean available = true;

        ZoningAlternative(int code,double weight) {
            this.code = code;
            choiceWeight = weight;
        }
        @Override
        public double getUtility() {
            return 0;
        }

        @Override
        public void setUtility(double utility) {
        }

        @Override
        public void setConstant(double constant) {
        }

        @Override
        public double getConstant() {
            return 0;
        }

        @Override
        public void setExpConstant(double expConstant) {
            choiceWeight = expConstant;
        }

        @Override
        public double getExpConstant() {
            return choiceWeight;
        }

        @Override
        public String getName() {
            return String.valueOf(code);
        }

        @Override
        public void setName(String name) {
            code = Integer.valueOf(name).intValue();
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public void setAvailability(boolean available) {
            this.available = available;
        }
        public int getCode() {
            return code;
        }
     }

     void buildZoningChoices() {
         int zoneColumn = zoningTDSxTAZ.checkColumnPosition("TAZ");
         int dTypeColumn = zoningTDSxTAZ.checkColumnPosition("DevelopmentType");
         zoningChoices = new HashMap<String,ObservedChoiceProbabilities>();
         String[][] zoningStringArray = new String[1][1];
         TableDataSetIndexedValue zoningCodeLookup = new TableDataSetIndexedValue("ZoningSchemesI", new String[] {"ZoningScheme"},new String[0],zoningStringArray, new int[1][0],"ZoningSchemeCode");
         zoningCodeLookup.setValueMode(TableDataSetIndexedValue.AVERAGE_MODE);
         for (int r=1;r<=zoningTDSxTAZ.getRowCount();r++) {
             String devTypeName= zoningTDSxTAZ.getStringValueAt(r,dTypeColumn);
             Integer dtInt = developmentTypeCodes.get(devTypeName);
             if (dtInt==null) {
                 logger.fatal("Development type name "+devTypeName+" is invalid in ZoningProbabilityTables");
                 throw new RuntimeException("Development type name "+devTypeName+" is invalid in ZoningProbabilityTables");
             }
             int devType = dtInt.intValue();
             String devTypeString = String.valueOf(devType);
             int taz = (int) zoningTDSxTAZ.getValueAt(r,zoneColumn);
             ObservedChoiceProbabilities probs = zoningChoices.get(taz+"&"+devTypeString);
             if (probs == null) {
                 probs = new ObservedChoiceProbabilities(taz+"&"+devTypeString);
                 zoningChoices.put(taz+"&"+devTypeString,probs);
             }
             for (int c = 1;c<=zoningTDSxTAZ.getColumnCount();c++) {
                 if (c!= zoneColumn && c!= dTypeColumn && !zoningTDSxTAZ.getColumnLabel(c).equalsIgnoreCase("DefaultTAZ")) {
                     String zoningName = zoningTDSxTAZ.getColumnLabel(c);
                     zoningStringArray[0][0] = zoningName;
                     zoningCodeLookup.setStringValues(zoningStringArray);
                     int zoningCode = (int) zoningCodeLookup.retrieveValue(referenceData);
                     ZoningAlternative alt = new ZoningAlternative(zoningCode,zoningTDSxTAZ.getValueAt(r,c));
                     probs.addAlternative(alt);
                 }
             }
         }

         for (ObservedChoiceProbabilities p : zoningChoices.values()) {
             p.calculateProbabilities();
         }
         zoningProbabilityMap.buildIndex(1);

     }


     class LandCategory {
         final char code;    //development type code
         float observedLand;    //number of acres from the Land.csv table
         float space;             //inventory value from the FloorspaceInventory file generated in LD?
         final String title;
         double intensity=0;
         double synthLandQuantity;   //the number of acres after the intensity has been adjusted (if necessary) - if intensity falls within min,max
                                                    //range, then this number should be the same as the observedLand.
         int cells;
         int vacantCells = 0;

         double maxIntensity = 1000000.0;
         double minIntensity = 0;
         double servicesPerUnit;

         /**
         * Use this constructor when you want to create a LandCategory but not initialize it yet.
         * You'll need to call LandCategory.initialize(...) later
         * @param code the code representing the development type
         */
        LandCategory(char code, String myTitle){
             this.code = code;
             title = myTitle;
         }

         /**
          * Use this constructor to create a LandCategory and assign a quantity of observed land based
          * on a TableDataSet, but not yet initialize it.  Call LandCategory.initialize(...) later.
         * @param code the code representing the development type
         * @param landTdis the TableDataSetIndexedValue where the retrieveValue() will get the quantity of land
         */
        LandCategory(char code, TableDataSetIndexedValue landTdis, String myTitle){
             observedLand = landTdis.retrieveValue(landData);
             this.code = code;
             title = myTitle;
         }

        
         /**
          * 
          * Creates and initializes a LandCategory
         * @param code the code representing the development type
         * @param inventoryTdis used to retrieve the inventory of space to be applied to the land (column name already set)
         * @param landTdis used to retrieve the quantity of land on which the inventory is to be applied, using column names specified in landCategories
         * @param landCategories a list of the land categories to be used.
         * @param minIntensity the minimum allowed intensity
         * @param maxIntensity the maximum allowed intensity
         * @param serviceLevel the service level to be applied per unit of inventory.
         */
        LandCategory(char code, TableDataSetIndexedValue inventoryTdis, TableDataSetIndexedValue landTdis, String[] landCategories, String myTitle) {
            title = myTitle;

             observedLand = 0;
             for (int i=0;i<landCategories.length;i++) {
                 landTdis.setMyFieldName(landCategories[i]);
                 observedLand += landTdis.retrieveValue(landData);
             }
             this.code = code;
             float space =inventoryTdis.retrieveValue(landData);
             String[][] stringValues = new String[1][1];
             stringValues[0][0] = names[code];
             minMaxLookup.setStringValues(stringValues);
             minMaxLookup.setMyFieldName("ServiceLevel");
             float serviceLevel = minMaxLookup.retrieveValue(landData);
             minMaxLookup.setMyFieldName("MinIntensity");
             float minIntens = minMaxLookup.retrieveValue(landData);
             minMaxLookup.setMyFieldName("MaxIntensity");
             float maxIntens = minMaxLookup.retrieveValue(landData);
             initialize( names[code],  space,  minIntens,  maxIntens, serviceLevel);
         }

        /**
         * Creates and initializes a LandCategory
        * @param code the code representing the development type
        * @param inventoryTdis used to retrieve the inventory of space to be applied to the land (column name already set)
        * @param landTdis used to retrieve the quantity of land on which the inventory is to be applied (column name already set)
        */
       LandCategory(char code, TableDataSetIndexedValue inventoryTdis, TableDataSetIndexedValue landTdis, String myTitle) {
           title = myTitle;
            observedLand = landTdis.retrieveValue(landData);
            this.code = code;
            float space = inventoryTdis.retrieveValue(landData);
            String[][] stringValues = new String[1][1];
            stringValues[0][0] = names[code];
            minMaxLookup.setStringValues(stringValues);
            minMaxLookup.setMyFieldName("ServiceLevel");
            float serviceLevel = minMaxLookup.retrieveValue(landData);
            minMaxLookup.setMyFieldName("MinIntensity");
            float minIntens = minMaxLookup.retrieveValue(landData);
            minMaxLookup.setMyFieldName("MaxIntensity");
            float maxIntens = minMaxLookup.retrieveValue(landData);
            initialize( names[code],  space,  minIntens,  maxIntens, serviceLevel);
        }

         /**
          * Creates an unitialized LandCategory, observed land is set but nothing else is.
          * Call initialize(...) later.
         * @param c the code representing the development type
         * @param landTdis landTdis used to retrieve the quantity of land on which the inventory is to be applied, using column names specified in landCategories
         * @param landCategories a list of the land categories to be used.
         */
        public LandCategory(char c, TableDataSetIndexedValue landTdis, String[] landCategories, String myTitle) {
            title = myTitle;
             observedLand = 0;
             for (int i=0;i<landCategories.length;i++) {
                 landTdis.setMyFieldName(landCategories[i]);
                 observedLand += landTdis.retrieveValue(landData);
             }
             this.code = c;
        }

        void initialize(String name, float spaceParam, double minIntensity, double maxIntensity, double serviceLevel) {
             servicesPerUnit = serviceLevel;
             this.minIntensity = minIntensity;
             this.maxIntensity = maxIntensity;
             double ableToBorrow = 0;
             boolean addedVacant = false;
             space = spaceParam;
             if (space <=0) space = 0;// just to be sure.
             if (observedLand <=0) { 
                 cells = 0;
                 if (space>0){
                     intensity =maxIntensity*1.01;// this will make us borrow later.
                 } else {
                     intensity = 0;
                 }
             } else {
                 cells = (int) Math.round(observedLand/gridCellSize);
                 if (cells==0) cells=1;
                 intensity = space/(cells*gridCellSize);
             }


             if (intensity >maxIntensity){
                 intensity = maxIntensity;  //
                 ableToBorrow = borrowSomeAgForestCells();
             }
             if (intensity <minIntensity) {
                 intensity = minIntensity;   //the intensity is now set at what we want.
                 addedVacant = createSomeVacantCells();
             }

             if (ableToBorrow!=0 || addedVacant) {
                 synthLandQuantity=0;
                 if (intensity != 0) {
                     synthLandQuantity = space/intensity;
                 }
                 synthLandQuantity = calculateCellsIntensityAndLand((float)synthLandQuantity);
             } else{
                observedLand = calculateCellsIntensityAndLand(observedLand);
             }
            gridCounts.setValueAt(zoneRow,gridCounts.checkColumnPosition(name),cells);
             intensities.setValueAt(zoneRow,gridCounts.checkColumnPosition(name),(float) intensity);
         }

        public int getCode() {
            return code;
        }

         public float calculateCellsIntensityAndLand(float qtyOfLand){
            cells = (int) Math.round (qtyOfLand/gridCellSize);
            if (cells ==0 && space >0) cells = 1;
            qtyOfLand = (float)(cells * gridCellSize);
            if (qtyOfLand > 0) intensity = space/qtyOfLand;
            else intensity = 0;
            if (intensity > maxIntensity) {
                cells ++;
                qtyOfLand = (float)(cells * gridCellSize);
                intensity = space/qtyOfLand;
            }
            return qtyOfLand;
        }

         /**This method will borrow a Ag and Forest/Protected cells based on a percentage of total available cells to borrow.
          *  If there are no cells to borrow, the method will return and the intensities will stay as they are.
          */
         private double borrowSomeAgForestCells(){

             float agLand = landCategories[0].observedLand;
             float forLand = landCategories[1].observedLand;
             double landToBorrow = space/intensity - observedLand;  //remember that the intensity has now
                                                                    //been set to the desired intensity,
                                                                    //not the original intensity.
             if(code != 50 ) {
                 if(agLand + forLand == 0) {
                     logger.warn("No agricultural or Forest land to borrow for "+this);
                     return 0;
                 }
                 if(landToBorrow > agLand + forLand) {
                     logger.warn("Not enough Agriculture land and Forest land to borrow for "+this.toString());
                     landToBorrow = agLand + forLand;
                 }
                 landCategories[0].observedLand -= (agLand / (agLand+forLand)) * landToBorrow;
                 landCategories[1].observedLand -=(forLand /(agLand + forLand)) * landToBorrow;
                 return landToBorrow;
             }else { //just try to borrow Forest
                 if(forLand == 0) {
                     logger.warn("No Forest land to borrow for agricultural use in "+this.toString());
                     return 0;
                 }
                 if(landToBorrow > forLand) {
                     logger.warn("Not enough Forest land to borrow for agricultural use in "+this.toString());
                     landToBorrow = forLand;
                 }
                 landCategories[1].observedLand -=landToBorrow;
                 return landToBorrow;
             }

         }

         private boolean createSomeVacantCells(){
             if (cells ==1 && space >0) return false;
             if (cells == 0) return false;
             double amtToMakeVacant = cells*gridCellSize - space/intensity;

             vacantCells = (int) Math.round(amtToMakeVacant/gridCellSize);

             if(vacantCells > cells || vacantCells <= 0) return false; //TODO - log this as a potential data problem or rounding issue.

             // TODO the new vacant cells should have a zoning consistent with what we thought their use was
             landCategories[14].cells += vacantCells;
             landCategories[14].intensity = 0;
             return true;
         }

        @Override
        public String toString() {
            return title;
        }

     }




     public void synthesizeGridCells() {
         // do this row by row in the land table, each row is a TAZ
         // can't use the indexing function built into TableDataSet because the TAZ numbers are way too huge, would eat up to much memory
         // instead use TableDataSetIndexedValue
         int[][] keyValues = new int[1][1];
         String[][] stringKeyValues = new String[1][0];
         String[] stringKeyColumns = new String[0];
         String[] tazKeyColumn = new String[] {"TAZ"};
         String[] inventoryKeyColumn = new String[] {"FloorspaceZone"};

         //ATTENTION: It matters in the code below that vacant is the last column!!
         String[] resultColumnNames = new String[] {"TAZ","Ag","ForestPro","Office","HvyInd","LtInd","CommRetAccom","K12","GovtInst","Mil","ResUHi","ResUMed","ResULow","ResRHi","ResRLow","Vac",};
         gridCounts = TableDataSet.create(new float[landQuantityTable.getRowCount()][resultColumnNames.length],resultColumnNames);
         gridCounts.setName("gridcounts");
         intensities = TableDataSet.create(new float[landQuantityTable.getRowCount()][resultColumnNames.length],resultColumnNames);
         intensities.setName("intensities");

         for (zoneRow =1; zoneRow <= landQuantityTable.getRowCount();zoneRow++) {
             taz = (int) landQuantityTable.getValueAt(zoneRow,landQuantityTable.checkColumnPosition("TAZ"));
             keyValues[0][0] = taz;
             int cellsInTAZ = 0;

             //put TAZ number in row=zoneRow, col=1
             gridCounts.setValueAt(zoneRow,1,taz);
             intensities.setValueAt(zoneRow,1,taz);

             //I made landCategories static so that the LandCategory class can see (and change) the amount of Forest/Protected Cells.
             landCategories = new LandCategory[resultColumnNames.length-1];

             //Get the Land database table.
             TableDataSetIndexedValue landTdiv = new TableDataSetIndexedValue(landQuantityTableName,stringKeyColumns,tazKeyColumn,stringKeyValues,keyValues,"Agricultural Land");
             landTdiv.setErrorOnMissingValues(true);

             //Initially for Ag and For/Protected we are just going to get the observed land.  Once we finish with the
             //other devTypes (that may borrow some or all of the AG  and FOREST/PROTECTED LAND, then we will figure
             //out the intensity of the Ag land and borrow any remaining FOREST/PROTECTED if the AG intensities are way too high or create vacant AG
             // if the AG intensities are too low.   We will also calculated the number of cells.
             // ATTENTION:  The order of these categories matters!!

             //ag - devType 50
             landCategories[0] = new LandCategory((char) 50,landTdiv,"Ag in "+taz);     //this constructors will set the devType code and the observed land total

             //forest/protected and Airport/Port - devType60
             String[] landCategoryStrings = new String[] {"Forestry and protected land","Airport/Port Land"};
             landCategories[1] = new LandCategory((char) 60,landTdiv,landCategoryStrings,"Forestry/Protected/Airport in "+taz);
             // old way without airports landCategories[1] = new LandCategory((char) 60,landTdiv);    //this constructor will set the devType code and the observed land total.

             landTdiv.setMyFieldName("Vacant land");
             landCategories[14] = new LandCategory((char) 82,"Vacant in "+taz);

             //Now we need the Inventory table because we are going to start populating the grid.
             //Get floorspace inventory table for non-residential lands
             TableDataSetIndexedValue inventoryTdiv = new TableDataSetIndexedValue(inventoryTableName,stringKeyColumns,inventoryKeyColumn,stringKeyValues,keyValues,"Office_SFB");
             inventoryTdiv.setErrorOnMissingValues(true);

             //office - devType22
             inventoryTdiv.setMyFieldName("Office_SFB");
             landTdiv.setMyFieldName("Office Land");
             landCategories[2] = new LandCategory((char) 22,inventoryTdiv,landTdiv,"Office in "+taz);

             //heavy industry land - devType32
             inventoryTdiv.setMyFieldName("HvyInd_ES");
             landTdiv.setMyFieldName("Heavy industrial Land");
             landCategories[3] = new LandCategory((char) 32,inventoryTdiv,landTdiv,"HvyIndustry in "+taz);

             //light industry land - devType31
             inventoryTdiv.setMyFieldName("LtInd_ES");
             landTdiv.setMyFieldName("Light industrial Land");
             landCategories[4] = new LandCategory((char) 31,inventoryTdiv,landTdiv,"Light Industry in "+taz);

               //commercial retail- devType21
             inventoryTdiv.setMyFieldName("CommRetAccom_SFB");
             landCategoryStrings = new String[] {"Commercial Land","Retail Land","Accommodation"};
             landCategories[5] = new LandCategory((char) 21,inventoryTdiv,landTdiv,landCategoryStrings,"CommRetAccom in "+taz);

             // K12 land - devType23
             inventoryTdiv.setMyFieldName("K12_ES");
             landTdiv.setMyFieldName("K-12 school land");
             landCategories[6] = new LandCategory((char) 23,inventoryTdiv,landTdiv,"K12 in "+taz);
//
//             //govtInst - devType24
             inventoryTdiv.setMyFieldName("GovtInst_SFB");
             landCategoryStrings = new String[] {"Government Land","Higher Educ Land","Healthcare Institution Land"};
             landCategories[7] = new LandCategory((char) 24,inventoryTdiv,landTdiv,landCategoryStrings,"GovInst in "+taz);

             //military - devType40
             inventoryTdiv.setMyFieldName("Military_Space");
             landTdiv.setMyFieldName("Military base land");
             landCategories[8] = new LandCategory((char) 40,inventoryTdiv,landTdiv,"Military in "+taz);

             //Residential Rural Low - devType11
             inventoryTdiv.setMyFieldName("ResRLow_SQFT");
             landTdiv.setMyFieldName("Residential Rural Subdivision");
             landCategories[9] = new LandCategory((char) 11,inventoryTdiv,landTdiv,"ResRLow in "+taz);

             //Residential Rural High - devType12
             inventoryTdiv.setMyFieldName("ResRHi_SQFT");
             landTdiv.setMyFieldName("Residential Rural Acreages");
             landCategories[10] = new LandCategory((char) 12,inventoryTdiv,landTdiv,"ResRHi in "+taz);

             //Residential Urban Low - devType15
             inventoryTdiv.setMyFieldName("ResULow_SQFT");
             landTdiv.setMyFieldName("Low");
             landCategories[11] = new LandCategory((char) 15,inventoryTdiv,landTdiv, "ResULow in "+taz);

             //Residential Urban Medium - devType16
             inventoryTdiv.setMyFieldName("ResUMed_SQFT");
             landTdiv.setMyFieldName("Med");
             landCategories[12] = new LandCategory((char) 16,inventoryTdiv,landTdiv,"ResUMed in "+taz);

             //Residential Urban high - devType17
             inventoryTdiv.setMyFieldName("ResUHi_SQFT");
             landTdiv.setMyFieldName("High");
             landCategories[13] = new LandCategory((char) 17,inventoryTdiv,landTdiv, "ResUHi in "+taz);

             //Get the Ag land and Ag floorspace and calculate cells and intensity
             inventoryTdiv.setMyFieldName("Ag_ES");
             float agSpace = inventoryTdiv.retrieveValue();
             landCategories[0].initialize( names[50],  agSpace,  0,  1000000, 0);
             
             landCategories[1].initialize(names[60], 0, 0, 1000000, 0);

             cellsInTAZ =0;
             for (int i=0;i<landCategories.length;i++) {
                 cellsInTAZ += landCategories[i].cells;
             }

             String[] id1Array = new String[cellsInTAZ];
             String[] id2Array = new String[cellsInTAZ];
             float[] landSizeArray = new float[cellsInTAZ];
             int[] devTypeArray = new int[cellsInTAZ];
             float[] quantityArray = new float[cellsInTAZ];
             int[] yearBuiltArray = new int[cellsInTAZ];
             int[] zoningArray = new int[cellsInTAZ];
             float[] servicing = new float[cellsInTAZ];
             int[] serviceCostCode = new int[cellsInTAZ];
             float[] constructionCostMultiplier = new float[cellsInTAZ];

             int row = 0;
             
             // First see if we need to adjust the probabilities to match the total quantity of zoned land

             double[][] balances = new double[landCategories.length][zoningSchemeCodes.size()];
             double[][] balancingTargets = new double[2][];
             balancingTargets[0] = new double[landCategories.length];
             balancingTargets[1] = new double[zoningSchemeCodes.size()];
             
             for (int d=0;d<landCategories.length-1;d++) {
                 int probabilityTableToUse = (int) zoningProbabilityMap.getIndexedValueAt(taz,2);
                 ObservedChoiceProbabilities p = zoningChoices.get(probabilityTableToUse+"&"+landCategories[d].getCode());
                 if (p==null) throw new RuntimeException("No probability table for zone "+probabilityTableToUse+" and development type "+landCategories[d].getCode());
                 ArrayList alts = p.getAlternatives();
                 double[] probs = p.getProbabilities();
                 for (int alt = 0;alt<alts.size();alt++) {
                     ZoningAlternative zAlt = (ZoningAlternative) alts.get(alt);
                     int zoningCode = zAlt.getCode();
                     Integer sequentialScheme = (Integer) (zoningSchemeSequentialLookupFromCode.get(new Integer(zoningCode)));
                     if (sequentialScheme==null) {
                         logger.error("No zoning code "+zoningCode+" in ZoningSchemes");
                         throw new RuntimeException("No zoning code "+zoningCode+" in ZoningSchemes");
                     }
                     int col = sequentialScheme.intValue();
                     balances[d][col]+=landCategories[d].cells*gridCellSize*probs[alt];
                     balancingTargets[0][d]=landCategories[d].cells*gridCellSize;
                 }
                 
                 
             }
             
             
             int[][] zoningAcresKeys = new int[1][1];
             zoningAcresKeys[0][0] = taz;
             NDimensionalMatrixDouble balancesMatrix = new NDimensionalMatrixDouble("balancingAct", balances);
             try {
                 TableDataSetIndexedValue zoningAcres = new TableDataSetIndexedValue("LandAcresxZoningI",new String[0], new String[] {"TAZ"}, new String[1][0], zoningAcresKeys, "ACRES");
                 zoningAcres.setErrorOnMissingValues(true);
                 
                 Iterator it = zoningSchemeCodes.keySet().iterator();
                 while (it.hasNext()) {
                     Integer schemeCode = (Integer) it.next();
                     String schemeName = (String) zoningSchemeCodes.get(schemeCode);
                     zoningAcres.setMyFieldName(schemeName);
                     double value = zoningAcres.retrieveValue(landData);
                     int col = ((Integer) zoningSchemeSequentialLookupFromCode.get(schemeCode)).intValue();
                     balancingTargets[1][col] = value;
                 }
                 NDimensionalMatrixBalancerDouble balancer = new NDimensionalMatrixBalancerDouble(balancesMatrix,balancingTargets);
                 balancer.balance();
                 balancesMatrix = balancer.getBalancedMatrix();
             } catch ( MissingValueException e) {
                 logger.warn("No landXzoning totals for zone "+taz);
             }
             double[] rowTotals = balancesMatrix.collapseToVectorAsDouble(0);

             for(int d=0; d<landCategories.length-1; d++){ //We will treat "VACANT" that is the
                                                            //last landCategories entry differently.
                    for(int n=0; n<landCategories[d].cells; n++){
                        id1Array[row] = Integer.toString(taz);         //TAZ - ID1
                        id2Array[row] = Long.toString(gridCellCounter);  //ID2
                        landSizeArray[row] = (float) gridCellSize;  //AmountOfLand
                        devTypeArray[row] = landCategories[d].getCode(); //DevelopmentType
                        // TODO sample intensity
                        quantityArray[row] =  (float) (landCategories[d].intensity*gridCellSize);
                        if(landCategories[d].getCode() == 82){
                            yearBuiltArray[row] = 1990;   //YearBuilt
                        } else {
                            yearBuiltArray[row] = selectYearBuilt(taz);   //YearBuilt
                        }
                        
                        double selector = Math.random()*rowTotals[d];
                        double sum = 0;
                        int[] location = new int[2];
                        location[0] = d;
                        int zoning;
                        for (zoning = 0;zoning < balancesMatrix.getShape(1);zoning++) {
                            location[1] = zoning;
                            sum += balancesMatrix.getValue(location);
                            if (sum > selector) break;
                        }
                        if (zoning >= zoningSchemeCodeLookupFromSequential.length) {
                            logger.error("Unrealistic zoned land totals for TAZ "+taz+ " Applying unbalanced zoning for devtype "+names[landCategories[d].getCode()]);
                            int probabilityTableToUse = (int) zoningProbabilityMap.getIndexedValueAt(taz,2);
                            ObservedChoiceProbabilities p = zoningChoices.get(probabilityTableToUse+"&"+landCategories[d].getCode());
                            zoning = ((ZoningAlternative) p.chooseAlternative()).getCode();
                            zoningArray[row] = zoning;
                        } else {
                            zoningArray[row] = zoningSchemeCodeLookupFromSequential[zoning];   //ZoningScheme
                        }
                        // TODO if intensity is sampled than servicing needs to be sampled too
                        servicing[row] = (float) (landCategories[d].servicesPerUnit*landCategories[d].intensity*gridCellSize);
                        serviceCostCode[row] = 0;
                        constructionCostMultiplier[row] = (float) 1.0;
                        row++;
                        gridCellCounter++;
                    }

                 for(int v =0; v <landCategories[d].vacantCells; v++){
                        id1Array[row] = Integer.toString(taz);         //TAZ - ID1
                        id2Array[row] = Long.toString(gridCellCounter);  //ID2
                        landSizeArray[row] = (float) gridCellSize;  //AmountOfLand
                        devTypeArray[row] = 82; //Vacant
                        // TODO sample intensity
                        quantityArray[row] =  0.0f;
                        yearBuiltArray[row] = 1990;   //default for vacant land is 1990
                        int probabilityTableToUse = (int) zoningProbabilityMap.getIndexedValueAt(taz,2);
                        ObservedChoiceProbabilities p = zoningChoices.get(probabilityTableToUse+"&"+String.valueOf(devTypeArray[row]));
                        ZoningAlternative z = (ZoningAlternative) p.chooseAlternative();
                        zoningArray[row] = z.getCode();   //ZoningScheme
                        // TODO if intensity is ampled than servicing needs to be sampled too
                        servicing[row] = (float) (landCategories[d].servicesPerUnit*1*gridCellSize);
                        serviceCostCode[row] = 0;
                        constructionCostMultiplier[row] = (float) 1.0;
                        row++;
                        gridCellCounter++;
                    }
                 }
             TableDataSet gridCellDataTable = new TableDataSet();
             gridCellDataTable.appendColumn(id1Array,"ID1");
             gridCellDataTable.appendColumn(id2Array,"ID2");
             gridCellDataTable.appendColumn(landSizeArray,"AmountOfLand");
             gridCellDataTable.appendColumn(devTypeArray,"DevelopmentTypeCode");
             gridCellDataTable.appendColumn(quantityArray,"AmountOfDevelopment");
             gridCellDataTable.appendColumn(yearBuiltArray,"YearBuilt");
             gridCellDataTable.appendColumn(zoningArray,"ZoningSchemeCode");
             gridCellDataTable.appendColumn(servicing,"AmountOfService");
             gridCellDataTable.appendColumn(serviceCostCode,"ServiceCostCode");
             gridCellDataTable.appendColumn(constructionCostMultiplier,"ConstructFactor");
             gridCellDataTable.setName("GridCellData_"+taz);
             landData.addTableDataSet(gridCellDataTable);
             landData.flushAndForget(gridCellDataTable);

         }
         landData.addTableDataSet(gridCounts);
         landData.addTableDataSet(intensities);
         landData.flush();
    }

    private int selectYearBuilt(int zoneNumber) throws RuntimeException {
        int yrBuilt = -1;
        //get the row from the YearBuiltProbability.csv file.  We need
        //to create an array of probabilities meaning we need to
        //take all elements of the row except the first and second element which is the
        //TAZ number and the TAZName.  Check that the probs add up to 1 while you are at it.
        float[] row = yrBuiltProbTable.getIndexedRowValuesAt(zoneNumber); //returns row values starting in position 0
        float[] probabilities = new float[row.length-2];
        float sum = 0.0f;
        for(int i=0; i< row.length-2; i++){
            probabilities[i] = row[i+2];  //skip the TAZ  and TAZName column.
            sum+=probabilities[i];   // I am anticipating some rounding issues so will
                                    //throw runtime if rounding error is greater than .05 (meaning sum
                                    //is 99.95 or less instead of 1.
        }
        if((1.0 - sum) >= 0.5 ) throw new RuntimeException("Probability Sum is  " + sum + " - check data - should add to 1.0");

        int index = DistributionSelector.getMonteCarloSelection(probabilities);
        //if(logger.isDebugEnabled()) logger.debug("index chosen was " + index);
        String intervalHeader = yrBuiltProbTable.getColumnLabel(index + 3); //the index starts at 1 and doesn't take into account the TAZ or TAZName column in the table.

        //if(logger.isDebugEnabled()) logger.debug("Interval Header is " + intervalHeader);
        double lowerBound = Double.parseDouble(intervalHeader.substring(0,intervalHeader.indexOf('-')));
        double upperBound = Double.parseDouble(intervalHeader.substring(intervalHeader.indexOf('-')+1));

        yrBuilt = (int) DistributionSelector.selectFromUniformDistribution(lowerBound,upperBound);

        return yrBuilt;
    }

}