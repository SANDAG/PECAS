/*
 * Copyright 2005 HBA Specto Incorporated
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
package com.hbaspecto.pecas.zones;

import java.util.ArrayList;
import com.pb.common.datafile.TableDataSet;

/**
 * A class that represents a transport analysis zone - a higher level amount of
 * land.
 * 
 * @author J. Abraham
 */
public final class PECASZone
        extends AbstractZone
        implements UnitOfLand
{

    public int      zoneUserNumber;
    String          zoneName;
    private boolean external;

    /**
     * private constructor to ensure that only one zone of each zone number is
     * created
     */
    private PECASZone(int index, int zUserNumber, String zname, boolean external)
    {
        super(index, zUserNumber);
        zoneUserNumber = zUserNumber;
        zoneName = zname;
        setExternal(external);
    }

    /**
     * Creates a PECASZone and puts it in the global PECASZone array
     */
    public static PECASZone createTaz(int zoneIndex)
    {
        final AbstractZone[] zones = getAllZones();
        if (zoneIndex >= zones.length || zoneIndex < 0)
        {
            throw new Error("Need to index zones consecutively within the allocated array size");
        }
        if (zones[zoneIndex] != null)
        {
            throw new Error("Attempt to create zone with index" + zoneIndex + " more than once");
        }
        return new PECASZone(zoneIndex, zoneIndex, "", false);
    }

    public static PECASZone createTaz(int zoneIndex, int zoneUserNumber, String zoneName,
            boolean external)
    {
        final AbstractZone[] zones = getAllZones();
        if (zoneIndex >= zones.length || zoneIndex < 0)
        {
            throw new Error("Need to index zones consecutively within the allocated array size");
        }
        if (zones[zoneIndex] != null)
        {
            throw new Error("Attempt to create zone with index" + zoneIndex + " more than once");
        }
        return new PECASZone(zoneIndex, zoneUserNumber, zoneName, external);
    }

    @Override
    public int getZoneUserNumber()
    {
        return zoneUserNumber;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof PECASZone))
        {
            return false;
        }
        final PECASZone other = (PECASZone) o;
        if (other.zoneIndex == zoneIndex)
        {
            return true;
        }
        return false;
    }

    public static void setUpZones(TableDataSet ztab)
    {
        // inner class to set up the name/number pair of each exchange zone
        class NumberName
        {
            String  zoneName;
            int     zoneNumber;
            boolean external;

            public NumberName(int znum, String zname, boolean external)
            {
                zoneName = zname;
                zoneNumber = znum;
                this.external = external;
            }
        }

        // Reads the betazone table, creates a NumberName object that
        // is temporarily stored in an array list. Seems like
        // this step could be skipped, the tempZoneStorage.size() =
        // ztab.getRowCount()
        // so you could create the AbstractZone array, then read in each row and
        // create
        // the PECASZone on the fly and store it in the array. That would
        // eliminate
        // the call to the array list for the NumberName object.
        final ArrayList tempZoneStorage = new ArrayList();
        final int externalColumn = ztab.getColumnPosition("External");
        if (externalColumn == -1)
        {
            logger.warn("No External column in PECASZonesI -- flows will be allowed between all zone pairs and histograms will include all zones");
        } else
        {
            logger.info("External column found in PECASZonesI -- flows will be disallowed between pairs of external zones");
        }
        for (int row = 1; row <= ztab.getRowCount(); row++)
        {
            final String zoneName = ztab.getStringValueAt(row, "ZoneName");
            final int zoneNumber = (int) ztab.getValueAt(row, "ZoneNumber");
            boolean external = false;
            if (externalColumn != -1)
            {
                external = ztab.getBooleanValueAt(row, externalColumn);
            }
            final NumberName nn = new NumberName(zoneNumber, zoneName, external);
            tempZoneStorage.add(nn);
        }
        // this creates an empty AbstractZone[] which is in fact an array of
        // land use zone (luz)s which
        // are the exchange zones refered to throughout the AA Model
        AbstractZone.createTazArray(tempZoneStorage.size());
        for (int z = 0; z < tempZoneStorage.size(); z++)
        {
            final NumberName nn = (NumberName) tempZoneStorage.get(z);
            // this creates a PECASZone object and stores it in the AbstractZone
            // array
            PECASZone.createTaz(z, nn.zoneNumber, nn.zoneName, nn.external);
        }
    }

    /**
     * Gets a list of the external zones. Returns null if there are no external
     * zones
     * 
     * @return list of external zones, or null if no external zones
     */
    public static int[] getOverrideExternalZones()
    {
        final AbstractZone[] zones = AbstractZone.getAllZones();
        final ArrayList<Integer> externalZones = new ArrayList<Integer>();
        for (int z = 0; z < zones.length; z++)
        {
            if (zones[z] instanceof PECASZone)
            {
                if (((PECASZone) zones[z]).isExternal())
                {
                    externalZones.add(new Integer(zones[z].getZoneUserNumber()));
                }
            }
        }
        if (externalZones.size() == 0)
        {
            return null;
        }
        final int[] theOnes = new int[externalZones.size()];
        for (int i = 0; i < theOnes.length; i++)
        {
            theOnes[i] = externalZones.get(i).intValue();
        }
        return theOnes;
    }

    public void setExternal(boolean external)
    {
        this.external = external;
    }

    public boolean isExternal()
    {
        return external;
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////////////////////////

    // private Hashtable sellingCommodityZUtilities;

    /**
     * The buyingCommodityZUtilities is a table of the buying ZUtilities for
     * this zone. The items are stored in a hastable, so they can be looked up
     * by the associated commodity
     */
    // private Hashtable buyingCommodityZUtilities;

    /**
     * This gets the ZUtility for a commodity in a zone, either the selling
     * ZUtility or the buying ZUtility.
     * 
     * @param c
     *            the commodity to get the buying or selling utility of
     * @param selling
     *            if true, get the selling utility. Otherwise get the buying
     *            utility
     */
    /*
     * public double calcZUtility(Commodity c, boolean selling) throws
     * ChoiceModelOverflowException { // message #1.2.3.1.3.1 to
     * theZutilityOfACommodityInAZone:com.pb.models.pecas.CommodityZutility //
     * double unnamed = theZutilityOfACommodityInAZone.getUtility(); if
     * (selling) { return ((Alternative)
     * sellingCommodityZUtilities.get(c)).getUtility(1); } else { return
     * ((Alternative) buyingCommodityZUtilities.get(c)).getUtility(1); } }
     */

    /**
     * This gets the ZUtility for a commodity in a zone, either the selling
     * ZUtility or the buying ZUtility.
     * 
     * @param c
     *            the commodity to get the buying or selling utility of
     * @param selling
     *            if true, get the selling utility. Otherwise get the buying
     *            utility
     */
    /*
     * public double calcZUtilityForPreferences(Commodity c, boolean selling,
     * TravelUtilityCalculatorInterface tp, boolean withRouteChoice) { //
     * message #2.5.1 to
     * zUtilityOfLabourOrConsumption:com.pb.models.pecas.CommodityZutility //
     * double unnamed =
     * zUtilityOfLabourOrConsumption.calcUtilityForPreferences(TravelPreferences
     * ); if (selling) { return
     * ((CommodityZUtility)sellingCommodityZUtilities.get
     * (c)).calcUtilityForPreferences(tp, withRouteChoice); } else { return
     * ((CommodityZUtility
     * )buyingCommodityZUtilities.get(c)).calcUtilityForPreferences(tp,
     * withRouteChoice); } }
     */

    /*
     * public static void createTestTazAndExchange(ZoningSchemeInterface zs) {
     * final int numTestZones = 10; final int numTestComs = 4; final int
     * numGridCellsPerTypePerZone = 24; // set up zones
     * AbstractZone.createTazArray(numTestZones); for (int i = 0; i <
     * numTestZones; i++) { PECASZone t = new PECASZone(i,i,""); } // set up
     * commodities Commodity[] coms = new Commodity[numTestComs]; for (int i =
     * 0; i < numTestComs; i++) { coms[i] =
     * Commodity.createOrRetrieveCommodity(String.valueOf(i),'a');
     * coms[i].setSellingUtilityCoefficients(1.0, 1.0, 1.0);
     * coms[i].setBuyingUtilityCoefficients(1.0,1.0,1.0);
     * coms[i].setDefaultBuyingDispersionParameter(new Parameter(1.0));
     * coms[i].setDefaultSellingDispersionParameter(new Parameter(1.0)); } //
     * set up BuyingZUtilities, SellingZUtilities and Exchanges
     * 
     * Commodity.setUpExchangesAndZUtilities(""); AbstractZone[] allZonesCopy =
     * AbstractZone.getAllZones(); /* change this section of code to match RunPA
     * for (int c = 0; c < numTestComs; c++) { for (int z = 0; z < numTestZones;
     * z++) { SellingZUtility szu = new SellingZUtility(coms[c],
     * (PECASZone)allZonesCopy[z]); // constructor has to set up links in both
     * directions BuyingZUtility bzu = new BuyingZUtility(coms[c],
     * (PECASZone)allZonesCopy[z]); szu.setDispersionParameter(0.2);
     * bzu.setDispersionParameter(0.2); Exchange xc = new Exchange(coms[c],
     * (PECASZone)allZonesCopy[z]); if (c < numTestComs / 2) // half of
     * commodities exchanged in consumption zone { bzu.addExchange(xc); } else {
     * szu.addExchange(xc); } } for (int z = 0; z < numTestZones; z++) {
     * PECASZone t = (PECASZone)allZonesCopy[z]; if (c < numTestComs / 2) {
     * CommodityZUtility czu =
     * (CommodityZUtility)t.sellingCommodityZUtilities.get(coms[c]);
     * czu.addAllExchanges(); } else { CommodityZUtility czu =
     * (CommodityZUtility)t.buyingCommodityZUtilities.get(coms[c]);
     * czu.addAllExchanges(); } } } /* end of code section to change to match
     * RunPA
     * 
     * 
     * 
     * //set up grid cells; Iterator it = zs.allowedDevelopmentTypes();
     * DevelopmentTypeInterface dt1 = (DevelopmentTypeInterface) it.next();
     * DevelopmentTypeInterface dt2 = (DevelopmentTypeInterface) it.next(); for
     * (int z = 0; z < numTestZones; z++) { for (int g = 0; g <
     * numGridCellsPerTypePerZone; g++) { GridCell gc = new
     * GridCell(allZonesCopy[z], (float)10.0, dt1, (float) 10000.0,
     * Math.random() * 50, zs); // 10 acres, 10000 // sq ft of space, up to 50
     * years old gc = new GridCell(allZonesCopy[z], (float) 10.0, dt2, (float)
     * 10000.0, Math.random() * 50, zs); // 10 acres, 10000 sq ft of space, up
     * to 50 years old } } }
     */

    /*
     * public void addSellingZUtility(SellingZUtility aSellingCommodityZUtility,
     * Commodity com) { sellingCommodityZUtilities.put(com,
     * aSellingCommodityZUtility); }
     */

    /*
     * public void addBuyingZUtility(BuyingZUtility aBuyingCommodityZUtility,
     * Commodity com) { buyingCommodityZUtilities.put(com,
     * aBuyingCommodityZUtility); }
     */

    /*
     * public static void main(String[] args) { createTestTazAndExchange(); }
     */

    /**
     * Should rely return an immutable hashtable
     */
    /*
     * public Hashtable getSellingCommodityZUtilities() { return
     * sellingCommodityZUtilities; }
     */

    /**
     * Should rely return an immutable hashtable
     */
    /*
     * public Hashtable getBuyingCommodityZUtilities() { return
     * buyingCommodityZUtilities; }
     */

    /* setUpZones method has been moved to AADataReader */
    // public static void setUpZones(TableDataSet ztab)

    /* setUpExchangesAndZUtilities() has been moved to AADataReader */
    // public static void setUpExchangesAndZUtilities()
}
