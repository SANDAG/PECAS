/*
 * Copyright 2005 HBA Specto Incorporated
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.hbaspecto.pecas.aa.commodity;

import java.util.Iterator;
import org.apache.log4j.Logger;
import com.hbaspecto.discreteChoiceModelling.Alternative;
import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.OverflowException;
import com.hbaspecto.pecas.aa.travelAttributes.TravelUtilityCalculatorInterface;
import com.hbaspecto.pecas.zones.PECASZone;

/**
 * The Zutility of a commodity in a zone, whether it is the selling Zutility or the buying Zutility, is based on the prices in the relevant exchange
 * zones and the transport costs to the relevant exchange zones. Generally (e.g. for ODOT), each commodity will either have one exchange zone for its
 * buying zutility (and multiple exchange zones for its selling zutility) or it will have one exchange zone for its selling zutility (and multiple
 * exchagne zone for its buying zutility). Thus the cardinaility of this relationship will be 1 in about half of the instances of Zutility objects,
 * and N=number-of-zones in the remainder of the instances
 */

abstract public class CommodityZUtility
        implements Alternative
{
    private static Logger                          logger                             = Logger.getLogger("com.pb.models.pecas");

    private double                                 derivative                         = 0;
    private boolean                                pricesFixed                        = false;
    private boolean                                lastUtilityValid                   = false;
    private double                                 myDispersionParameter;
    private final TravelUtilityCalculatorInterface temporaryTravelPreferences         = null;
    private final boolean                          useRouteChoice                     = false;
    /**
     * This attribute represents the amount bought or sold in the zone. Changing the quantity in the zone does <b> not </b> automatically change the
     * quantities in the flows and exchanges. The method allocateQuantityToFlowsAndExchanges must be called to update the amounts in the flows and
     * exchanges
     * 
     * @see CommodityZUtility#allocateQuantityToFlowsAndExchanges()
     * @see CommodityFlowArray
     * @see Exchange
     */
    private double                                 quantity                           = 0;                                      // TC or TP depending
                                                                                                                                 // on whether the
                                                                                                                                 // commZUtil
    // is a buy or sell Utility
    public Commodity                               myCommodity;
    private double                                 lastCalculatedUtility;                                                       // CUBuy or CUSell
                                                                                                                                 // depending on
    // whether the commZUtil is a buy or
    // sell util
    public CommodityFlowArray                      myFlows;
    public PECASZone                               myTaz;
    private double                                 lastHigherLevelDispersionParameter = 1;

    public double getLastHigherLevelDispersionParameter()
    {
        return lastHigherLevelDispersionParameter;
    }

    // for profiling int utilityCalcCount = 0;

    /**
     * sets up one-way associations. Should only be called by a subclass, which can inform the PECASZone and the Commodity to set up the other-way
     * associations.
     */
    protected CommodityZUtility(Commodity c, PECASZone z, TravelUtilityCalculatorInterface tp)
    {
        myFlows = new CommodityFlowArray(this, tp);
        myCommodity = c;
        myTaz = z;
    }

    public abstract void addExchange(Exchange x);

    public abstract void addAllExchanges();

    public abstract void allocateQuantityToFlowsAndExchanges() throws OverflowException;

    /* calculates the CommodityUtility */
    @Override
    public double getUtility(double higherLevelDispersionParameter)
            throws ChoiceModelOverflowException
    {
        if (pricesFixed && lastUtilityValid)
        {
            return lastCalculatedUtility;
        }

        lastHigherLevelDispersionParameter = higherLevelDispersionParameter;
        // last calculatedUtility is either the SUc,z,k or CUSellc,z depending
        // on the exhange type
        lastCalculatedUtility = myFlows.getUtility(higherLevelDispersionParameter);
        if (pricesFixed)
        {
            lastUtilityValid = true;
        }
        return lastCalculatedUtility;
    }

    @Override
    public double getUtilityNoSizeEffect() throws ChoiceModelOverflowException
    {
        return myFlows.getUtilityNoSizeEffect();
    }

    public double[] getUtilityComponents(double higherLevelDispersionParameter)
            throws ChoiceModelOverflowException
    {
        lastHigherLevelDispersionParameter = higherLevelDispersionParameter;
        return myFlows.getUtilityComponents(higherLevelDispersionParameter);
    }

    public synchronized void changeDerivativeBy(double d) throws OverflowException
    {
        // debug 2007.05.11
        if (d < 0)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("A portion of derivative is negative for " + toString());
            }
        }
        derivative += d;
        if (Double.isNaN(derivative) || Double.isInfinite(derivative))
        {
            throw new OverflowException("Derivative is NaN/Infinite in " + this);
        }
    }

    public synchronized void changeQuantityBy(double change) throws OverflowException
    {
        // debug June 5 2002
        /*
         * if (myCommodity.name.equals("FLR Agriculture") && myTaz.getZoneIndex()==726) { System.out.println(this + " quantity changing from "
         * +this.quantity+" to "+(this.quantity+change)); }
         */
        quantity += change;
        if (Double.isNaN(quantity) || Double.isInfinite(quantity))
        {
            throw new OverflowException(" quantity is " + quantity + " in " + this);
        }
    }

    /*
     * ------------------ GETTERS and SETTERS -----------------------------------------------------------
     */
    /**
     * @param d
     *            the amount that the quantity will change if the composite utility of buying or selling changes by 1 unit.
     * @throws OverflowException
     */
    public synchronized void setDerivative(double d) throws OverflowException
    {
        // debug Feb232004
        if (derivative != 0)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("resetting derivative in " + this);
            }
        }
        derivative = d;
        if (Double.isNaN(d))
        {
            logger.warn("Setting derivative to NaN for " + this);
            throw new OverflowException("Setting derivative to NaN for " + this);
        }
    }

    /**
     * @return amount that the quantity will change with respect to changes in the composite utility measure.
     */
    public double getDerivative()
    {
        return derivative;
    }

    /**
     * @param quantity
     *            quantity bought or sold -- always should be positive, when it is an amount purchased the subclass will apply appropriate sign change
     * @throws OverflowException
     */
    public synchronized void setQuantity(double quantity) throws OverflowException
    {
        this.quantity = quantity;
        if (Double.isNaN(quantity) || Double.isInfinite(quantity))
        {
            throw new OverflowException(" quantity is " + quantity + " in " + this);
        }
    }

    /**
     * @return quantity bought or sold. Always should be positive whether it is bought or sold; subclass determines whether it is bought or sold
     */
    public double getQuantity()
    {
        return quantity;
    }

    public void setDispersionParameter(double dispersionParameter)
    {
        myDispersionParameter = dispersionParameter;
        myFlows.dispersionParameter = myDispersionParameter;
    }

    public double getDispersionParameter()
    {
        return myDispersionParameter;
    }

    public TravelUtilityCalculatorInterface getMyTravelPreferences()
    {
        if (temporaryTravelPreferences == null)
        {
            return myCommodity.getCommodityTravelPreferences();
        } else
        {
            return temporaryTravelPreferences;
        }
    }

    public boolean getUseRouteChoice()
    {
        return useRouteChoice;
    }

    public CommodityFlowArray getMyFlows()
    {
        return myFlows;
    }

    public Commodity getCommodity()
    {
        return myCommodity;
    }

    public PECASZone getTaz()
    {
        return myTaz;
    }

    @Override
    public String toString()
    {
        return myTaz + " " + myCommodity;
    }

    /**
     * @param b
     */
    public void setPricesFixed(boolean b)
    {
        if (pricesFixed == false || b == false)
        {
            lastUtilityValid = false;
        }
        pricesFixed = b;
    }

    public void setLastCalculatedUtility(double lastCalculatedUtility)
    {
        this.lastCalculatedUtility = lastCalculatedUtility;
    }

    public void setLastUtilityValid(boolean b)
    {
        lastUtilityValid = b;
    }

    public double[] getExchangeProbabilities()
    {
        return myFlows.getChoiceProbabilities();
    }

    /*
     * This method just resets the quantities and derviative to 0 to prepare for the next iteration
     */
    static public void resetCommodityBoughtAndSoldQuantities()
    {
        final Iterator cit = AbstractCommodity.getAllCommodities().iterator();
        while (cit.hasNext())
        {
            final Commodity c = (Commodity) cit.next();
            for (int b = 0; b < 2; b++)
            {
                Iterator<CommodityZUtility> it;
                if (b == 0)
                {
                    it = c.getBuyingUtilitiesIterator();
                } else
                {
                    it = c.getSellingUtilitiesIterator();
                }
                while (it.hasNext())
                {
                    final CommodityZUtility czu = it.next();
                    try
                    {
                        czu.setQuantity(0);
                        czu.setDerivative(0);
                    } catch (final OverflowException e)
                    {
                    }
                }
            }
        }
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Has to use its logit model of exchange zones and inspect the knowledge of transport conditions to figure out the composite utility of buying or
     * selling
     */
    /*
     * LogitModel getMyLogitModelOfExchangeAlternatives() { if (myLogitModelOfExchangeAlternatives == null) { myLogitModelOfExchangeAlternatives = new
     * LogitModel(); Iterator it = flows.values().iterator(); while (it.hasNext()) { myLogitModelOfExchangeAlternatives
     * .addAlternative((CommodityFlow)it.next()); // message #1.2.3.1.3.1.1 to travelConditions:com.pb.despair.ts.TransportKnowledge with arguments:
     * commodityTravelPreferences // Object unnamed = travelConditions.getRutilities(TravelPreferences); // message #1.2.3.1.3.1.2 to
     * allExchanges:com.pb.models.pecas.Exchange // double unnamed = allExchanges.getPrice(); }
     * myLogitModelOfExchangeAlternatives.setDispersionParameter (getDispersionParameter()); } return myLogitModelOfExchangeAlternatives; }
     */

    /**
     * Has to use its logit model of exchange zones and inspect the knowledge of transport conditions to figure out the composite utility of buying or
     * selling
     */
    /*
     * public double calcUtilityForPreferences(TravelUtilityCalculatorInterface tp, boolean withRouteChoice) { double bob; synchronized(this) {
     * temporaryTravelPreferences = tp; this.useRouteChoice = withRouteChoice; bob = getUtility(); temporaryTravelPreferences = null;
     * this.useRouteChoice = false; } return bob; // message #1.2.3.1.3.1.1 to travelConditions:com.pb.despair.ts.TransportKnowledge with arguments:
     * commodityTravelPreferences // Object unnamed = travelConditions.getRutilities(TravelPreferences); // message #1.2.3.1.3.1.2 to
     * allExchanges:com.pb.models.pecas.Exchange // double unnamed = allExchanges.getPrice(); }
     */

    /*
     * CommodityFlow monteCarloChoiceOfExchange() throws NoAlternativeAvailable { LogitModel exc = getMyLogitModelOfExchangeAlternatives(); return
     * (CommodityFlow)exc.monteCarloChoice(); }
     */

    /*
     * CommodityFlow monteCarloChoiceOfExchangeForPreferences(TravelUtilityCalculatorInterface tp) throws NoAlternativeAvailable {
     * temporaryTravelPreferences = tp; LogitModel exc = getMyLogitModelOfExchangeAlternatives(); CommodityFlow cf =
     * (CommodityFlow)exc.monteCarloChoice(); temporaryTravelPreferences = null; return cf; }
     */

    /*
     * public void addFlowIfNotAlreadyThere(CommodityFlow f) { if (!(flows.containsKey(f.theExchange))) flows.put(f.theExchange, f); }
     */

    /*
     * public CommodityFlow findExistingCommodityFlow(Exchange ex) { return (CommodityFlow) flows.get(ex); }
     * 
     * /* public CommodityFlow findExistingCommodityFlow(PECASZone ex) { Iterator it = flows.iterator(); while (it.hasNext()) { CommodityFlow cf =
     * (CommodityFlow)it.next(); if (cf.theExchange.exchangeLocation.equals(ex)) return cf; } return null; }
     */

    /*
     * for(int i=0;i<myFlows.theExchanges.length;i++) { if (myFlows.theExchanges[i]!=null) { try { table.insertRow(false);
     * table.setString("Commodity", myCommodity.toString()); if (this instanceof BuyingZUtility) {
     * table.setInt("OriginZone",myFlows.theExchanges[i].exchangeLocation .getZoneUserNumber());
     * table.setInt("DestinationZone",myTaz.getZoneUserNumber()); table.setDouble("Quantity",-myFlows.quantities[i]); } else {
     * table.setInt("OriginZone",myTaz.getZoneUserNumber()); table.setInt("DestinationZone"
     * ,myFlows.theExchanges[i].exchangeLocation.getZoneUserNumber()); table.setDouble("Quantity",myFlows.quantities[i]); } table.post(); } catch
     * (com.borland.dx.dataset.DataSetException e) { System.err.println("Error adding commodity flow to commodity flow table" ); e.printStackTrace();
     * } } } }
     */

}
