/*
 *  Copyright 2005 HBA Specto Incorporated
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
package com.hbaspecto.pecas.aa.commodity;

import com.hbaspecto.functions.SingleParameterFunction;
import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.InvalidFlowError;
import com.hbaspecto.pecas.OverflowException;
import com.hbaspecto.pecas.zones.AbstractZone;
import com.hbaspecto.pecas.zones.PECASZone;
import com.pb.common.matrix.Matrix;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

/**
 * These are the thingies (goods or services) that need to be transported on the network.  E.g. "labour", "manufactured
 * appliances", "personal grooming services", etc.  Not really commodities in the economic sense because they are
 * heterogeneous in the model.
 *
 * @author John Abraham
 */
public class Commodity extends AbstractCommodity {

    
    private boolean isFloorspaceCommodity = false;
    private boolean doSearch = true;
    private boolean hasFixedPrices = false;
    private static Logger logger = Logger.getLogger("com.pb.models.pecas");

    public double oldMeritMeasure = 0.0;
    
    // reactivate scalingAdjustmentFactor if you need bigger step sizes for some stubborn commodities
    public double scalingAdjustmentFactor = 1.0;
    public double compositeMeritMeasureWeighting = 1.0;
    private double expectedPrice;
    /**
     * @associates <{BuyingZUtility}>
     * @supplierCardinality 1..*
     */
    private Hashtable<Integer, CommodityZUtility> buyingTazZUtilities = new Hashtable<Integer,CommodityZUtility>();
    private Hashtable<Integer, CommodityZUtility> sellingTazZUtilities = new Hashtable<Integer,CommodityZUtility>();
    public final char exchangeType;

    /** @associates <{Exchange}> */
    private ArrayList<Exchange> allExchanges = new ArrayList<Exchange>();
    private double buyingUtilitySizeCoefficient;
    private double sellingUtilitySizeCoefficient;
    private double buyingUtilityPriceCoefficient;
    private double sellingUtilityPriceCoefficient;
    private double buyingUtilityTransportCoefficient;
    private double sellingUtilityTransportCoefficient;
    private boolean flowsValid = false;
    private boolean manualSizeTerms = false;

    static int numExchangeNotFoundErrors = 0;
    //private boolean pricesAndConditionsFixed = false;
    private double defaultBuyingDispersionParameter;
    private double defaultSellingDispersionParameter;

    public static SingleParameterFunction zeroFunction = new SingleParameterFunction() {
        public double evaluate(double x) {
            return 0;
        }

        public double derivative(double x) {
            return 0;
        }
    };
	private static Hashtable<Exchange,Double[]> oldSizeTerms;
	private static boolean usesManualSizeTermColumn = false;
	private static boolean calculateSizeTerms;

    private Commodity(String name, char exchangeTypePar) {
        super(name);
        this.exchangeType = exchangeTypePar;
        if (exchangeType != 'c' && exchangeType != 'p' && exchangeType != 'a' && exchangeType != 's' && exchangeType != 'n') {
            throw new Error("Commodity " + name + " has invalid exchange type" + exchangeTypePar +
                    ": only c,p,a,n or s are allowed");
        }
    }
        
	public double[][] fixPricesAndConditionsAtNewValues() throws OverflowException {
        AbstractZone[] zones = AbstractZone.getAllZones();
             
        double[][] compositeUtilities = new double[2][zones.length]; //hold the CUBuys[0][] and CUSells[1][] for each zone

        //first do the CUBuy calculations
        boolean isSelling = false;
        for (int z = 0; z < zones.length; z++) {
            AbstractZone t = zones[z];

            //get the CUBuy zone utility object out of the CommodityZUtility hashtable
            CommodityZUtility czu = retrieveCommodityZUtility(t,isSelling);
            try {
                //This will return CUBuyc,z (or the BUc,z,k) which
                //is the utility of buying a commodity 'c' consumed in zone 'z'
                czu.setPricesFixed(true);
                compositeUtilities[0][zones[z].zoneIndex]= czu.getUtility(1);
            } catch (ChoiceModelOverflowException e) {
                throw new OverflowException(e.toString());
            }
        }

        //then do the CUSell calculations
        isSelling = true;
        for (int z = 0; z < zones.length; z++) {
            AbstractZone t = zones[z];
            //get the CUSell zone utility object out of the CommodityZUtility hashtable
            CommodityZUtility czu = retrieveCommodityZUtility(t,isSelling);
            try {
                //This will return CUSellc,z (or the SUc,z,k ) which
                //is the utility of selling a commodity 'c' produced in zone 'z'
                czu.setPricesFixed(true);
                compositeUtilities[1][zones[z].zoneIndex]=czu.getUtility(1);
            } catch (ChoiceModelOverflowException e) {
                throw new OverflowException(e.toString());
            }
        }
		return compositeUtilities;
	}
	
	
	public void setCommodityZUtilities(double[][] zutilities) {
		AbstractZone[] zones = AbstractZone.getAllZones();

		//first do the CUBuy setting
		boolean isSelling = false;
		for (int z = 0; z < zones.length; z++) {
			AbstractZone t = zones[z];

			//get the CUBuy zone utility object out of the CommodityZUtility hashtable
			CommodityZUtility czu = retrieveCommodityZUtility(t,isSelling);
			czu.setPricesFixed(true);
			czu.setLastCalculatedUtility(zutilities[0][z]);
			czu.setLastUtilityValid(true);
		}

		//then do the CUSell setting
		isSelling = true;
		for (int z = 0; z < zones.length; z++) {
			AbstractZone t = zones[z];
			//get the CUSell zone utility object out of the CommodityZUtility hashtable
			CommodityZUtility czu = retrieveCommodityZUtility(t,isSelling);
			czu.setPricesFixed(true);
			czu.setLastCalculatedUtility(zutilities[1][z]);
			czu.setLastUtilityValid(true);
		}
	}

	public void unfixPricesAndConditions() {
        
        AbstractZone[] zones = AbstractZone.getAllZones();
        for (int s = 0; s < 2; s++) {
            boolean selling = true;
            if (s == 1) selling = false;
            for (int z = 0; z < zones.length; z++) {
                //get the CUSell or CUBuy utility calculator for each zone out of the CommodityZUtility hashtable
                CommodityZUtility czu = retrieveCommodityZUtility((PECASZone) zones[z],selling);
                    czu.setPricesFixed(false);
            }
        }
	}

//Getters and Setters for the Commodity Parameters used in Utility calculations
//We could pass doubles instead of Parameter objects.
    public double getDefaultSellingDispersionParameter() {
        return defaultSellingDispersionParameter;
    }

    public double getDefaultBuyingDispersionParameter() {
        return defaultBuyingDispersionParameter;
    }

    public void setDefaultBuyingDispersionParameter(double defaultBuyingDispersionParameter) {
        this.defaultBuyingDispersionParameter = defaultBuyingDispersionParameter;
    }

    public void setDefaultSellingDispersionParameter(double defaultSellingDispersionParameter) {
        this.defaultSellingDispersionParameter = defaultSellingDispersionParameter;
    }

//    public Hashtable<Integer, CommodityZUtility> getBuyingTazZUtilities() {
//        return buyingTazZUtilities;
//    }

    public Iterator<CommodityZUtility> getBuyingUtilitiesIterator() {
    	return buyingTazZUtilities.values().iterator();
    }
//    public Hashtable<Integer, CommodityZUtility> getSellingTazZUtilities() {
//        return sellingTazZUtilities;
//    }
    
    public Iterator<CommodityZUtility> getSellingUtilitiesIterator() {
    	return sellingTazZUtilities.values().iterator();
    }

    public static Commodity createOrRetrieveCommodity(String name, char exchangeTypePar) {
        Commodity c = retrieveCommodity(name);
        if (c == null) {
            c = new Commodity(name, exchangeTypePar);
        }
        return c;
    }

    public static Commodity retrieveCommodity(String name) {
        Commodity commodity = (Commodity)allCommoditiesHashmap.get(name);
        if (commodity == null) {
            return null;
        }
        else {
            return commodity;
        }
    }
    
	/**
     * This gets the ZUtility for a commodity in a zone, either the selling ZUtility or the buying ZUtility.
     *
     * @param t       the PECASZone to get the buying or selling utility of
     * @param selling if true, get the selling utility.  Otherwise get the buying utility
     */
    public double calcZUtility(AbstractZone t, boolean selling) throws OverflowException {
        try {
            CommodityZUtility czu = retrieveCommodityZUtility(t, selling);
            return czu.getUtility(1);
        } catch (ChoiceModelOverflowException e) {
            throw new OverflowException(e.toString());
        }
    }


    public CommodityZUtility retrieveCommodityZUtility(AbstractZone t, boolean selling) {
        Hashtable<Integer, CommodityZUtility> ht;
        if (selling)
            ht = sellingTazZUtilities;
        else
            ht = buyingTazZUtilities;
        CommodityZUtility czu = ht.get(t.getZoneUserNumber());
        return czu;
    }
    
	public int getNumBuyingUtilities() {
		return buyingTazZUtilities.values().size();
	}
	public int getNumSellingUtilities() {
		return sellingTazZUtilities.values().size();
	}

    
    public CommodityZUtility retrieveCommodityZUtility(int zoneNumber, boolean selling) {
        PECASZone t = (PECASZone) PECASZone.findZoneByUserNumber(zoneNumber);
        return retrieveCommodityZUtility(t,selling);
    }

	/* This method is called by the Exchange constructor to add an Exchange
    *  to this Commodity's list of Exchanges
    */
    public void addExchange(Exchange ex) {
        allExchanges.add(ex);
    }

    public void addSellingZUtility(CommodityZUtility czu) {
        sellingTazZUtilities.put(czu.getTaz().zoneUserNumber, czu);
    }

    public void addBuyingZUtility(CommodityZUtility czu) {
        buyingTazZUtilities.put(czu.getTaz().zoneUserNumber, czu);
    }


    public List<Exchange> getAllExchanges() {
        return allExchanges;
    }

    public Exchange getExchange(int tazIndex) {
        if (this.exchangeType!='s') {
            return (Exchange) allExchanges.get(tazIndex);
        } else {
            for (int i=0;i<allExchanges.size();i++) {
                if (((Exchange) allExchanges.get(i)).exchangeLocationIndex == tazIndex) {
                    return (Exchange) allExchanges.get(i);
                }
            }
        }
        return null;
//        logger.fatal("Can't find exchange with index "+tazIndex+" for Commodity "+name);
//        throw new RuntimeException("Can't find exchange with index "+tazIndex+" for Commodity "+name);
    }

    public void setBuyingUtilityCoefficients(double size, double price, double transport) {
        buyingUtilitySizeCoefficient = size;
        buyingUtilityPriceCoefficient = price;
        buyingUtilityTransportCoefficient = transport;
    }

    public void setSellingUtilityCoefficients(double size, double price, double transport) {
        sellingUtilitySizeCoefficient = size;
        sellingUtilityPriceCoefficient = price;
        sellingUtilityTransportCoefficient = transport;
    }

    public double getBuyingUtilitySizeCoefficient() {
        return buyingUtilitySizeCoefficient;
    }

	public double getSellingUtilitySizeCoefficient() {
        return sellingUtilitySizeCoefficient;
    }

	public double getBuyingUtilityPriceCoefficient() {
        return buyingUtilityPriceCoefficient;
    }

	public double getSellingUtilityPriceCoefficient() {
			return sellingUtilityPriceCoefficient;
	}

	public double getBuyingUtilityTransportCoefficient() {
			return buyingUtilityTransportCoefficient;
	}

	public double getSellingUtilityTransportCoefficient() {
		return sellingUtilityTransportCoefficient;
	}

	public void clearAllExchangeQuantities() {
			Iterator it = getAllExchanges().iterator();
			while (it.hasNext()) {
				Exchange ex = (Exchange) it.next();
				ex.clearFlows();
			}
            setFlowsValid(false);
	}

	public static void clearAllCommodityExchangeQuantities() {
		Iterator it = getAllCommodities().iterator();
		while (it.hasNext()) {
			Commodity com = (Commodity) it.next();
			com.clearAllExchangeQuantities();
		}
	}



	public static void unfixPricesAndConditionsForAllCommodities() {
		Iterator allOfUs = getAllCommodities().iterator();
		while (allOfUs.hasNext()) {
			Commodity c = (Commodity) allOfUs.next();
			c.unfixPricesAndConditions();
		}
	}

    public void setFloorspaceCommodity(boolean isFloorspaceCommodity) {
        this.isFloorspaceCommodity = isFloorspaceCommodity;
    }

    public boolean isFloorspaceCommodity() {
        return isFloorspaceCommodity;
    }

    public void setExpectedPrice(double expectedPrice) {
        this.expectedPrice = expectedPrice;
    }

    public double getExpectedPrice() {
        return expectedPrice;
    }

    public void setCompositeMeritMeasureWeighting(double compositeMeritMeasureWeighting) {
        this.compositeMeritMeasureWeighting = compositeMeritMeasureWeighting;
    }

    public double[] getPriceInAllExchanges(){
        //exchanges are indexed by their location index which corresponds to a BetaZone index.
        //These index values could be greater than the number of exchange zones so you must
        //use the length of the buyingTazZUtility objects since there is one of these for each zone
        double[] prices = new double[buyingTazZUtilities.values().size()];

        Iterator exchanges = getAllExchanges().iterator();
        while(exchanges.hasNext()){
            Exchange ex = (Exchange)exchanges.next();
            prices[ex.exchangeLocationIndex]=ex.getPrice();
        }
        return prices;
    }

    public void setPriceInAllExchanges(double[] prices){
        Iterator exchanges = getAllExchanges().iterator();
        while(exchanges.hasNext()){
            Exchange ex = (Exchange)exchanges.next();
            ex.setPrice(prices[ex.exchangeLocationIndex]);
        }
        setFlowsValid(false);
        unfixPricesAndConditions();
    }

    public void setFlowsValid(boolean flowsValid) {
        this.flowsValid = flowsValid;
    }

    boolean isFlowsValid() {
        return flowsValid;
    }

    /**
     * @return a new Matrix containing the flow quantities
     */
    public Matrix getBuyingFlowMatrix() {
    	if (!flowsValid) {
    		logger.error("Flows have not been calculated on this machine for "+getName()+", not writing flows");
    		return null;
    	}
        int nZones = PECASZone.getAllZones().length;
        float[][] flows = new float[nZones][nZones];
        int[] zoneNumbers = new int[nZones+1];
        for (int exchange=0;exchange<nZones;exchange++) {//exchange zones
            Exchange ex = getExchange(exchange);
            if (ex!=null) {
                zoneNumbers[exchange+1] = PECASZone.getZone(exchange).getZoneUserNumber();
                for (int consumption =0;consumption<nZones;consumption++) { //consumption zones
                    try {
                        flows[exchange][consumption]=(float) -ex.getFlowQuantityZeroForNonExistantFlow(consumption,'b');
                    } catch (InvalidFlowError e) {
                        // do nothing -- if there is no flow, leave the entry in the matrix as zero
                    }
                }
            }
        }
        
        Matrix m = new Matrix(flows);
        m.setExternalNumbers(zoneNumbers);
        return m;
    }

    public Matrix getSellingFlowMatrix() {
    	if (!flowsValid) {
    		logger.error("Flows have not been calculated on this machine for "+getName()+", not writing flows");
    		return null;
    	}
        int nZones = PECASZone.getAllZones().length;
        float[][] flows = new float[nZones][nZones];
        int[] zoneNumbers = new int[nZones+1];
        for (int exchange=0;exchange<nZones;exchange++) {
            Exchange ex = getExchange(exchange);
            if (ex!=null) {
                zoneNumbers[exchange+1] = PECASZone.getZone(exchange).getZoneUserNumber();
                for (int production =0;production<nZones;production++) {
                    try {
                        flows[production][exchange]=(float) ex.getFlowQuantityZeroForNonExistantFlow(production,'s');
                    } catch (InvalidFlowError e) {
                        // do nothing -- if there is no flow, leave the entry in the matrix as zero
                    }
                }
            }
        }
        
        Matrix m = new Matrix(flows);
        m.setExternalNumbers(zoneNumbers);
        return m;
    }

    public double[] getSurplusInAllExchanges() {
        double[] surplus = new double[getAllExchanges().size()];
        Iterator exIt = getAllExchanges().iterator();
        int exNum = 0;
        while (exIt.hasNext()) {
            Exchange x = (Exchange) exIt.next();
            surplus[exNum] = x.exchangeSurplus();
            exNum++;
        }
        return surplus;
    }

    /**
     * In PECAS Software User Guide this is AveExchgTotal
     * [SumC(VergeWtc2 SumK(0.5*(TSupc,k + TDem c,k))2  ] ^(1/2)
     * @return measure of weighted total bought and sold for commodity 
     */
    public double getAverageExchangeTotal() {
        double avgExchangeTotal = 0;
        Iterator exIt = getAllExchanges().iterator();
        while (exIt.hasNext()) {
            Exchange x = (Exchange) exIt.next();
            if (x.isDoSearch()) {
	            double exchSize=(x.boughtTotal()+x.soldTotal())/2;
	            avgExchangeTotal+= Math.sqrt((compositeMeritMeasureWeighting*exchSize*compositeMeritMeasureWeighting*exchSize));
            }
        }
        
//        if (avgExchangeTotal==0) {
//        	logger.warn ("Average Exchange Total is zero for "+this+", perhaps neither bought nor sold internally");
//        }
        return avgExchangeTotal;
    }

    public static double getLargestSClearForAllCommodities(double confac) {
        Iterator<AbstractCommodity> iterator = getAllCommodities().iterator();
        double largest = Double.NEGATIVE_INFINITY;
        while (iterator.hasNext()) {
            Commodity c = (Commodity) iterator.next();
            if (c.isDoSearch()) {
                largest = Math.max(largest, c.getLargestSClear(confac));
            }
        }
        return largest;
    }

    public double getLargestSClear(double confac) {
        double avgExchangeTotal = getAverageExchangeTotal();
        if (avgExchangeTotal == 0) {
        	logger.warn("avgExchangeTotal is zero for "+this+", can't calculate maxSClear; setting to 0");
        	return 0;
        }
        double maxSClear = 0;
        Iterator exIt = getAllExchanges().iterator();
        while (exIt.hasNext()) {
            Exchange x = (Exchange) exIt.next();
            double surplus = x.exchangeSurplus();
            // TODO this is not DAF safe, as boughtTotal() and soldTotal() are not DAF-enabled yet.  
            // They are fixed in the Oregon PI code, but that needs to be brought back into PECAS
            double singleExchangeTotal = (x.boughtTotal()+x.soldTotal())/2+confac*avgExchangeTotal/compositeMeritMeasureWeighting;
            double sClear = Math.abs(surplus)/singleExchangeTotal;
            if (x.isDoSearch()) maxSClear = Math.max(maxSClear, sClear);
        }
        if (Double.isNaN(maxSClear)) {
        	logger.warn("maxSClear is NaN for "+this+", perhaps it is neither made nor used, overriding to 0");
        	return 0;
        }
        return maxSClear;
    }

    public static double calcTotalAverageExchangeTotal() {
        Iterator<AbstractCommodity> iterator = getAllCommodities().iterator();
        double total=0;
        while (iterator.hasNext()) {
            Commodity c = (Commodity) iterator.next();
            if (c.isDoSearch()) {
            	double commodityValue = c.getAverageExchangeTotal();
            	total += commodityValue*commodityValue;
            }
        }
        return Math.sqrt(total);
    }

	public static void storeExistingSizeTerms() {
		oldSizeTerms = new Hashtable<Exchange,Double[]>();
        Iterator<AbstractCommodity> iterator = getAllCommodities().iterator();
        while (iterator.hasNext()) {
        	Commodity c = (Commodity) iterator.next();
        	for (Exchange e : c.allExchanges) {
        		Double[] exchangeSizes = new Double[2];
        		exchangeSizes[0] = e.getBuyingSizeTerm();
        		exchangeSizes[1] = e.getSellingSizeTerm();
        		oldSizeTerms.put(e,exchangeSizes);
        	}
        }

		
	}

	public static void logSizeTermChanges() {
        Iterator<AbstractCommodity> iterator = getAllCommodities().iterator();
        while (iterator.hasNext()) {
        	Commodity c = (Commodity) iterator.next();
        	for (Exchange e : c.allExchanges) {
        		Double[] oldExchangeSizes = oldSizeTerms.get(e);
        		if (e.getBuyingSizeTerm() != oldExchangeSizes[0] && logger.isDebugEnabled()) logger.debug("Buying exchange size change from "+oldExchangeSizes[0]+" to "+e.getBuyingSizeTerm()+" for "+e);
        		if (e.getSellingSizeTerm() != oldExchangeSizes[1] && logger.isDebugEnabled()) logger.debug("Selling exchange size change from "+oldExchangeSizes[1]+" to "+e.getSellingSizeTerm()+" for "+e);
        	}
        }
	}

	public static void forgetOldSizeTerms() {
		oldSizeTerms = null;
	}

	public static void setCalculateSizeTerms(boolean calculateSizeTerms) {
		Commodity.calculateSizeTerms = calculateSizeTerms;
	}

	public static boolean isCalculateSizeTerms() {
		return calculateSizeTerms;
	}

	public void setManualSizeTerms(boolean manualSizeTerms) {
		this.manualSizeTerms = manualSizeTerms;
	}

	public boolean isManualSizeTerms() {
		return manualSizeTerms;
	}

	public void setDoSearch(boolean doSearch) {
		this.doSearch = doSearch;
	}

	public boolean isDoSearch() {
		return doSearch;
	}
	

	public boolean isHasFixedPrices() {
		return hasFixedPrices;
	}

	public void updateHasFixedPrices() {
		this.hasFixedPrices = false;
		for (Exchange x : allExchanges) {
			if (!x.isDoSearch()) hasFixedPrices = true;
		}
	}
	


}
