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

import simpleorm.sessionjdbc.SSessionJdbc;
import com.hbaspecto.pecas.sd.DevelopmentLog;
import com.pb.common.datafile.TableDataFileReader;
import com.pb.common.datafile.TableDataFileWriter;
import com.pb.common.datafile.TableDataSet;

/**
 * A class that represents an collection of discretized land. Each piece of land
 * is uniquely identified by pecas_parcel_num
 * 
 * @author John Abraham
 */
/**
 * @author John
 * 
 */
/**
 * @author Abdel
 * 
 */

/**
 * @author Abdel
 * 
 */
public interface LandInventory
{

    int VACANT_ID = 95;

    /**
     * For sequential access to portions of the land inventory, this method will
     * set the internal counter to the first object.
     */
    void setToBeforeFirst();

    /**
     * For sequential access to portions of the land inventory, this method will
     * set the internal counter to the next object, and return false if there is
     * no next object. If any of the methods with currentZone,id2 parameters
     * have been called, the internal counter will have been changed to point to
     * currentZone, id2, so the "next" will be the next after that, not
     * necessarily the next after
     * 
     * @return true if successful, false if not.
     */
    boolean advanceToNext();

    DevelopmentLog getDevelopmentLogger();

    ParcelErrorLog getParcelErrorLog();

    /**
     * Set the coverage of the current grid/parcel
     * 
     * @param coverageCode
     *            character representing the PECAS description of the current
     *            use (building type)
     */
    void putCoverage(int coverageCode);

    /**
     * Set the quantity of building/space on the current grid/parcel
     * 
     * @param quantity
     *            quantity of space (or other improvements) currently built on
     *            the land
     */
    void putQuantity(double quantity);

    /**
     * Set the construction year for the current grid/parcel -- used to
     * calculate effective age
     * 
     * @param yearBuilt
     */
    void putYearBuilt(int yearBuilt);

    /**
     * Set the quantity of service available for the current grid parcel. For
     * example this can represent gallons/hr of water service, gallons/day of
     * sewer capacity, or some other composite measure of the quantity of
     * service.
     * 
     * @param service
     */
    void putAvailableServiceCode(int service);

    /**
     * Get the construction year for the current grid/parcel -- used to
     * calculate effective age.
     * 
     * @param currentZone
     * @param id2
     *            second part of unique-ID.
     * @return effective year-built
     */
    int getYearBuilt();

    /**
     * Get the quantity of building/space on the current grid/parcel
     * 
     * @return quantity of buildings or space
     */
    double getQuantity();

    /**
     * Get the type of coverage for the current grid/parcel -- a code that
     * represents the type of building or type of improvement
     * 
     * @return the type of coverage
     */
    int getCoverage();

    /**
     * Get the physical land area size of the current grid/parcel in land units.
     * If land units and space units are consistent, then Floor Area Ratio can
     * be calculated as getQuantity(currentZone,id2)/getSize(currentZone,id2).
     * Note that there is no setSize() method -- PECAS does not need to be able
     * to change the physical layout of the parcels or grid cells, except
     * through the splitParcel() method.
     * 
     * @return a measure of how large, in land area, the parcel/grid is.
     */
    double getLandArea();

    /**
     * Get the quantity of service available for the current grid parcel. For
     * example 0 typically represents no service (well/septic) 1 represents low
     * level of service for 1-2 story buildings, 2 represents higher level of
     * service.
     * 
     * @return level of service
     */
    int getAvailableServiceCode();

    /**
     * This returns the integer code that represents the zoning scheme for the
     * <i>current</i> parcel. Note that we now use an int variable, instead of a
     * short variable, so we can have more than 65536 zoning schemes. A zoning
     * scheme represents the legal regulations, restrictions and subsidies/fees
     * associated with a parcel. These need to be stored separately, in a
     * separate table in a database or in memory. The grid file only refers to
     * the numerical uniqueID of the zoning regulations.
     * 
     * @return index used in the ZoningSchemesI table in the PECAS input
     *         database to determine the regulations
     */
    int getZoningRulesCode();

    /**
     * This method is used to get the price (a.k.a. rent) for the <i>current</i>
     * parcel for the floorspace type coverageChar. The zonal average price
     * information is calculated in the AA module for each floorspace type for
     * each land use zone. This method must look up the land use zone for the
     * parcel, and then look up the price in the land use price table, and then
     * apply the local effect rent modifiers for the parcel.
     * 
     * @param coverageCode
     *            the floorspace type for which the price is required.
     * @param currentYear
     *            TODO
     * @param baseYear
     *            TODO
     * @return price (rent) associated with coverageChar at currentZone, id2
     */
    double getPrice(int coverageCode, int currentYear, int baseYear);

    String parcelToString();

    // typical implementation
    // public String elementToString(long currentZone, long id2) {
    // return "Land,"+currentZone+","+id2;
    // }

    /**
     * Convenience method to check whether anything at all can be built on the
     * current parcel/grid. Many grids are water or protected areas that can
     * never be developed. For parcel data, it is often advisable to remove
     * protected parcels before running PECAS, but for grid data it is often
     * advisable to have a complete grid coverage, but to mask out certain
     * parcels by setting this function to return true.
     * 
     * @return false means the grid will never change due to developer actions
     */
    boolean isDevelopable();

    boolean isDerelict();

    /**
     * Returns the land inventory by TAZ in a TableDataSet format. SD uses this
     * function to write out the FloorspaceI.csv file for AA to use.
     * 
     * @return TableDataSet with three columns: TAZ, Commodity and Quantity.
     */
    TableDataSet summarizeInventory();

    /**
     * This returns a string that represents a concatination of pecas_parcel_num
     * and parcel_id attributes.
     * 
     * @return a string that represents a concatination of pecas_parcel_num and
     *         parcel_id attributes.
     */
    String getParcelId();

    /**
     * The maximum size of a parcel. If a parcel is larger than this size, it
     * will be treated in the monte-carlo simulation as multiple parcels of size
     * getSize()/((int)getSize()/getMaxParcelSize()), and if development occurs
     * on some parts but not others, SD will split the parcel into pseudoparcels
     * by calling the splitParcel method.
     * 
     * In all implementations so far this is a fixed value (static attribute)
     * for every parcel, but in the future it could be different for different
     * parcels (e.g. rural areas could have larger pseudoparcel sizes than urban
     * areas)
     * 
     * @return maximum size of parcel for simulation
     */
    double getMaxParcelSize();

    void setMaxParcelSize(double maxParcelSize);

    public class NotSplittableException
            extends Exception
    {

        private static final long serialVersionUID = -2452143144089470229L;

        public NotSplittableException()
        {
            super();
        }

        public NotSplittableException(String arg0, Throwable arg1)
        {
            super(arg0, arg1);
        }

        public NotSplittableException(String arg0)
        {
            super(arg0);
        }

        public NotSplittableException(Throwable arg0)
        {
            super(arg0);
        }

    }

    /**
     * Split the current parcel into an old one and a new one. The new one will
     * be of size newLandSize, the old one will be modified to be of size
     * getSize()-newLandSize.
     * 
     * @param parcelSizes
     *            size of the new parcel to create
     * @return the new object
     */
    ParcelInterface splitParcel(double parcelSizes) throws NotSplittableException;

    /**
     * Housekeeping routine -- if splitParcel is ever called you eventually need
     * to call addNewBits() to add the split parcels to the inventory for future
     * consideration
     */
    void addNewBits();

    /**
     * This method is in case the land inventory database queues up changes to
     * be applied all at once; calling this method tells it that you need the
     * changes to be applied
     */
    void applyDevelopmentChanges();

    /**
     * Get the code used to determine the costs associated with developing the
     * <i>current</i> parcel/grid
     * 
     * @return code indicating costs
     */
    int get_CostScheduleId();

    /**
     * @return the fee schedule ID used to determine the fees associated with
     *         development
     */
    int get_FeeScheduleId();

    long getPECASParcelNumber();

    boolean isBrownfield();

    void init(int year);

    int getTaz();

    void readSpacePrices(TableDataFileReader reader);

    void applyPriceSmoothing(TableDataFileReader reader, TableDataFileWriter writer);

    void putDerelict(boolean isDerelict);

    void putBrownfield(boolean b);

    // This method is added for Capacity Constraint feature, but the plan is to
    // have it
    // as a cost adjustment in the construction, not as a utility adjustment..
    // TODO: Remove this method when the CC testing is finished.
    // public abstract double getConstructionUtilityAdjustment(SpaceTypesI
    // spaceType);

    SSessionJdbc getSession();

    void disconnect();

    void commitAndStayConnected();
}
