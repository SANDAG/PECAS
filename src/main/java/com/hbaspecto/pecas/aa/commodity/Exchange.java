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
package com.hbaspecto.pecas.aa.commodity;

import org.apache.log4j.Logger;
import com.hbaspecto.functions.SingleParameterFunction;
import com.hbaspecto.pecas.InvalidFlowError;
import com.hbaspecto.pecas.OverflowException;
import com.hbaspecto.pecas.zones.PECASZone;

/**
 * These are the exchanges, i.e. the market for a commodity in a zone. Here are
 * the commodity prices that need to be adjusted to acheive no shortage or no
 * surplus in the zone at equilibrium
 * 
 * @author John Abraham
 */
public class Exchange
{

    private static Logger               logger                       = Logger.getLogger("com.pb.models.pecas.exchange");

    /**
     * Whether to adjust the price in this zone (and include this exchange in
     * convergence monitoring). If you like the input price and want to model to
     * solve for the supply and demand, set this to false
     */
    private boolean                     doSearch                     = true;
    /**
     * Whether to monitor the exchange in this zone, writing additional info to
     * the log file.
     */
    public boolean                      monitor                      = false;
    /**
     * An attribute that represents the average price of the commodity when
     * exchanged in the zone
     */
    private double                      price                        = 0;
    /**
     * The commodity being exchanged
     */
    public Commodity                    myCommodity;
    /**
     * The integer index of the exchange
     */
    final int                           exchangeLocationIndex;
    /**
     * The user zone number for the exchange
     */
    public final int                    exchangeLocationUserID;
    private double                      buyingFromExchangeDerivative = 0;
    private double                      sellingToExchangeDerivative  = 0;

    /**
     * This is the flow of all of this commodity into and out of this exchange
     * zone Do NOT manually add flows to this vector. The constructor for
     * CommodityFlow automatically adds itself into this vector.
     */
    protected final CommodityZUtility[] sellingToExchangeFlows;
    protected final CommodityZUtility[] buyingFromExchangeFlows;
    protected final double[]            buyingQuantities;
    protected final double[]            sellingQuantities;
    private double                      buyingSizeTerm;
    private double                      sellingSizeTerm;
    private SingleParameterFunction     importFunction;
    private SingleParameterFunction     exportFunction;
    private double                      lastCalculatedSurplus        = 0;
    private double                      lastCalculatedDerivative     = 0;
    private double                      lastCalculatedBuyingTotal    = 0;
    private double                      lastCalculatedSellingTotal   = 0;
    private boolean                     boughtAndSoldTotalsValid     = false;
    private boolean                     surplusValid                 = false;
    private boolean                     derivativeValid              = false;

    public Exchange(Commodity com, PECASZone zone, int arraySize)
    {
        sellingToExchangeFlows = new CommodityZUtility[arraySize];
        buyingFromExchangeFlows = new CommodityZUtility[arraySize];
        buyingQuantities = new double[arraySize];
        sellingQuantities = new double[arraySize];
        myCommodity = com;
        exchangeLocationIndex = zone.zoneIndex;
        exchangeLocationUserID = zone.zoneUserNumber;
        com.addExchange(this);
    }

    void setFlowQuantity(int tazIndex, char selling, double quantity) throws OverflowException
    {
        if (Double.isNaN(quantity) || Double.isInfinite(quantity))
        {
            throw new OverflowException("setting infinite or NaN flow quantity, exchange " + this
                    + " buy/sell " + tazIndex + " quantity:" + quantity);
        }
        if (selling == 's')
        {
            if (sellingToExchangeFlows[tazIndex] == null)
            {
                throw new Error("trying to set quantity for nonexistent flow " + this + " "
                        + selling + " " + tazIndex);
            }
            sellingQuantities[tazIndex] = quantity;
        } else
        {
            if (buyingFromExchangeFlows[tazIndex] == null)
            {
                throw new Error("trying to set quantity for nonexistent flow " + this + " "
                        + selling + " " + tazIndex);
            }
            buyingQuantities[tazIndex] = quantity;
        }
    }

    void setFlowQuantityAndDerivative(int tazIndex, char selling, double quantity, double derivative)
            throws OverflowException
    {
        // To verbose monitoring, removed July 2009
        // if (monitor) {
        // logger.info("Setting flow quantity from " + this +
        // " to taz with index " + tazIndex + " for " + selling + " to " +
        // quantity + " with derivative " + derivative);
        // }
        setFlowQuantity(tazIndex, selling, quantity);
        if (selling == 's')
        {
            setSellingToExchangeDerivative(getSellingToExchangeDerivative() + derivative);
        } else
        {
            setBuyingFromExchangeDerivative(getBuyingFromExchangeDerivative() + derivative);
        }
    }

    public double getFlowQuantity(int tazIndex, char selling)
    {
        if (selling == 's')
        {
            if (sellingToExchangeFlows[tazIndex] == null)
            {
                throw new InvalidFlowError("trying to get quantity for nonexistent flow " + this
                        + " " + selling + " " + tazIndex);
            }
            return sellingQuantities[tazIndex];
        } else
        {
            if (buyingFromExchangeFlows[tazIndex] == null)
            {
                throw new InvalidFlowError("trying to get quantity for nonexistent flow " + this
                        + " " + selling + " " + tazIndex);
            }
            return buyingQuantities[tazIndex];
        }
    }

    double getFlowQuantityZeroForNonExistantFlow(int tazIndex, char selling)
    {
        if (selling == 's')
        {
            if (sellingToExchangeFlows[tazIndex] == null)
            {
                return 0;
            }
            return sellingQuantities[tazIndex];
        } else
        {
            if (buyingFromExchangeFlows[tazIndex] == null)
            {
                return 0;
            }
            return buyingQuantities[tazIndex];
        }
    }

    /**
     * determine the net surplus in the zone, by summing all the selling
     * quantities and subtracting the sum of the buying quantities
     * 
     * @return returns the net surplus or shortage in the zone at the given
     *         price
     */
    public double exchangeSurplus()
    {
        if (surplusValid == true)
        {
            return lastCalculatedSurplus;
        } else
        {
            return exchangeSurplusAndDerivative()[0];
        }
    }

    /**
     * determine the derivative of the net surplus in the zone, by summing all
     * the selling quantities and subtracting the sum of the buying quantities
     * 
     * @return returns the net surplus or shortage in the zone at the given
     *         price
     */
    public double exchangeDerivative()
    {
        if (derivativeValid == true)
        {
            return lastCalculatedDerivative;
        } else
        {
            return exchangeSurplusAndDerivative()[1];
        }
    }

    /**
     * determine the net surplus in the zone, by summing all the selling
     * quantities and subtracting the sum of the buying quantities. Also returns
     * an approximation of the derivative of the surplus w.r.t. price
     * 
     * @return returns the net surplus or shortage in the zone at the given
     *         price
     */
    public double[] exchangeSurplusAndDerivative()
    {
        if (surplusValid == true && derivativeValid == true)
        {
            final double[] sAndD = new double[2];
            sAndD[0] = lastCalculatedSurplus;
            sAndD[1] = lastCalculatedDerivative;
            return sAndD;
        }
        // some debug code //

        /*
         * if (myCommodity.name.equals("HEALTH SERVICES") &&
         * exchangeLocation.getZoneIndex() == 0) { debug = true;
         * System.out.println
         * ("debug info for exchange "+this+" with price "+price); }
         */

        double surplus = 0;
        lastCalculatedBuyingTotal = 0;
        lastCalculatedSellingTotal = 0;
        double derivative = 0;
        CommodityZUtility productionOrConsumptionPoint = null;
        StringBuffer buyingString = null;
        StringBuffer sellingString = null;
        if (!myCommodity.isFlowsValid())
        {
            logger.error("Calculating surplus for " + this + " when the flows are invalid");
            throw new RuntimeException("Calculating surplus for " + this
                    + " when the flows are invalid");
        }
        if (monitor)
        {
            buyingString = new StringBuffer();
            sellingString = new StringBuffer();
        }
        for (int i = 0; i < buyingFromExchangeFlows.length; i++)
        {
            // all origin
            // and
            // destination
            // zones
            for (int b = 0; b < 2; b++)
            {
                double quantity;
                if (b == 0)
                {
                    productionOrConsumptionPoint = buyingFromExchangeFlows[i];
                    quantity = buyingQuantities[i]; // these are negative values
                    lastCalculatedBuyingTotal -= quantity;
                    if (monitor && productionOrConsumptionPoint != null)
                    {
                        buyingString.append(productionOrConsumptionPoint.getTaz()
                                .getZoneUserNumber() + ":" + quantity + " ");
                    }
                } else
                {
                    productionOrConsumptionPoint = sellingToExchangeFlows[i];
                    quantity = sellingQuantities[i]; // these are positive values
                    lastCalculatedSellingTotal += quantity;
                    if (monitor && productionOrConsumptionPoint != null)
                    {
                        sellingString.append(productionOrConsumptionPoint.getTaz()
                                .getZoneUserNumber() + ":" + quantity + " ");
                    }
                }
                if (productionOrConsumptionPoint != null)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("\t Commodity flow " + productionOrConsumptionPoint
                                + " to exchange " + this + " quantity " + quantity);
                    }
                }
                surplus += quantity;
            }
        }
        if (monitor)
        {
            logger.info("\t " + this + " buying quantities " + buyingString);
            logger.info("\t " + this + " selling quantities " + sellingString);
            logger.info("\t " + this + " price " + price);
        }
        derivative = getBuyingFromExchangeDerivative() + getSellingToExchangeDerivative();
        double[] importAndExport = importsAndExports(price);
        surplus += importAndExport[0] - importAndExport[1];
        derivative += importAndExport[2] - importAndExport[3];
        if (logger.isDebugEnabled() || monitor)
        {
            logger.info("\t " + "import:" + importAndExport[0] + " export:" + importAndExport[1]);
            logger.info("\t Total surplus = " + surplus);
        }

        // debug June 2 2002
        if (Double.isNaN(surplus) || Double.isInfinite(surplus) || Double.isNaN(derivative)
                || Double.isInfinite(derivative))
        {
            logger.warn("\t Problem with Exchange surplus and/or derivative in " + this);
            logger.warn("\t surplus:" + surplus + " derivative:" + derivative + " buildup follows:");
            logger.warn(" price " + price);
            surplus = 0;
            derivative = 0;
            productionOrConsumptionPoint = null;
            for (int i = 0; i < buyingFromExchangeFlows.length; i++)
            {
                for (int b = 0; b < 2; b++)
                {
                    double quantity;
                    if (b == 0)
                    {
                        productionOrConsumptionPoint = buyingFromExchangeFlows[i];
                        quantity = buyingQuantities[i];
                    } else
                    {
                        productionOrConsumptionPoint = sellingToExchangeFlows[i];
                        quantity = sellingQuantities[i];
                    }
                    if (productionOrConsumptionPoint != null)
                    {
                        logger.warn("\t Commodity flow " + productionOrConsumptionPoint
                                + " to exchange " + this + " quantity " + quantity);
                        surplus += quantity;
                        derivative += Math.abs(productionOrConsumptionPoint
                                .getDispersionParameter() * 0.5 * quantity);
                    }
                }
            }
            importAndExport = importsAndExports(price);
            logger.warn("\t imports:" + importAndExport[0] + " export:" + importAndExport[1]);
            surplus += importAndExport[0] - importAndExport[1];
            logger.warn("\t importDerivative:" + importAndExport[2] + " exportDerivative:"
                    + importAndExport[3]);
            derivative += importAndExport[2] - importAndExport[3];
        }
        // end of June 2 2002 Debug

        final double[] sAndD = new double[2];
        lastCalculatedSurplus = surplus;
        lastCalculatedDerivative = derivative;
        setSurplusAndDerivativeValid(true);
        boughtAndSoldTotalsValid = true;

        sAndD[0] = lastCalculatedSurplus;
        sAndD[1] = lastCalculatedDerivative;
        return sAndD;
    }

    public void clearFlows()
    {
        for (int i = 0; i < buyingFromExchangeFlows.length; i++)
        {
            if (buyingFromExchangeFlows[i] != null)
            {
                buyingQuantities[i] = 0;
            }
        }
        for (int i = 0; i < sellingToExchangeFlows.length; i++)
        {
            if (sellingToExchangeFlows[i] != null)
            {
                sellingQuantities[i] = 0;
            }
        }
        setBuyingFromExchangeDerivative(0);
        setSellingToExchangeDerivative(0);
        setLastCalculatedSurplus(0);
        setLastCalculatedDerivative(0);
        setSurplusAndDerivativeValid(false);
        boughtAndSoldTotalsValid = false;
    }

    public void setBuyingFromExchangeDerivative(double buyingFromExchangeDerivative)
    {
        this.buyingFromExchangeDerivative = buyingFromExchangeDerivative;
    }

    public double getBuyingFromExchangeDerivative()
    {
        return buyingFromExchangeDerivative;
    }

    public void setSellingToExchangeDerivative(double sellingToExchangeDerivative)
    {
        this.sellingToExchangeDerivative = sellingToExchangeDerivative;
    }

    public double getSellingToExchangeDerivative()
    {
        return sellingToExchangeDerivative;
    }

    public void setSurplusAndDerivativeValid(boolean b)
    {
        surplusValid = b;
        derivativeValid = b;
    }

    public void setSurplusValid(boolean b)
    {
        surplusValid = b;
    }

    public void setDerivativeValid(boolean b)
    {
        derivativeValid = b;
    }

    public void setLastCalculatedSurplus(double surplus)
    {
        lastCalculatedSurplus = surplus;
    }

    public void setLastCalculatedDerivative(double derivative)
    {
        lastCalculatedDerivative = derivative;
    }

    public void setPrice(double newPrice)
    {
        price = newPrice;
    }

    public double getPrice()
    {
        return price;
    }

    public void addFlowIfNotAlreadyThere(CommodityZUtility f, boolean buying)
    {
        final int tazIndex = f.myTaz.getZoneIndex();
        if (buying)
        {
            buyingFromExchangeFlows[tazIndex] = f;
            buyingQuantities[tazIndex] = 0;
        } else
        {
            sellingToExchangeFlows[tazIndex] = f;
            sellingQuantities[tazIndex] = 0;
        }
    }

    @Override
    public String toString()
    {
        return "Exchange:" + exchangeLocationUserID + ":" + myCommodity;
    }

    @Override
    public int hashCode()
    {
        return myCommodity.hashCode() ^ exchangeLocationIndex;
    }

    public double getBuyingSizeTerm()
    {
        return buyingSizeTerm;
    }

    public void setBuyingSizeTerm(double buyingSizeTerm)
    {
        if (buyingSizeTerm < 0)
        {
            final String msg = "Buying size term for " + this + " is negative: " + buyingSizeTerm;
            logger.fatal(msg);
            throw new RuntimeException(msg);
        }
        this.buyingSizeTerm = buyingSizeTerm;
    }

    public double getSellingSizeTerm()
    {
        return sellingSizeTerm;
    }

    public void setSellingSizeTerm(double sellingSizeTerm)
    {
        if (sellingSizeTerm < 0)
        {
            final String msg = "Selling size term for " + this + " is negative: " + sellingSizeTerm;
            logger.fatal(msg);
            throw new RuntimeException(msg);
        }
        this.sellingSizeTerm = sellingSizeTerm;
    }

    public SingleParameterFunction getImportFunction()
    {
        return importFunction;
    }

    public void setImportFunction(SingleParameterFunction importFunction)
    {
        this.importFunction = importFunction;
    }

    public SingleParameterFunction getExportFunction()
    {
        return exportFunction;
    }

    public void setExportFunction(SingleParameterFunction exportFunction)
    {
        this.exportFunction = exportFunction;
    }

    public int getExchangeLocationIndex()
    {
        return exchangeLocationIndex;
    }

    /**
     * Returns the amount of imports in an exchange zone given the price in the
     * exchange zone, and the amount of exports in an exchange zone given the
     * price in the exchange zone. This is a convenience routine for the
     * exchange zone -- each exchange zone can call this routine to determine
     * the imports and exports in that zone.
     * <p>
     * The default implementation uses a simple exponential routine
     * 
     * @param price
     *            price for which the imports and exports are to be determined
     * @return first element is amount of imports, second is amount of export
     * @see Exchange#setImportFunction
     */
    public double[] importsAndExports(double price)
    {
        final double[] impExp = new double[4];
        impExp[0] = importFunction.evaluate(price);
        impExp[1] = exportFunction.evaluate(price);
        impExp[2] = importFunction.derivative(price);
        impExp[3] = exportFunction.derivative(price);
        return impExp;
    }

    public double boughtTotal()
    {
        if (!boughtAndSoldTotalsValid)
        {
            calculateBoughtAndSoldTotals();
        }
        return lastCalculatedBuyingTotal;
    }

    private void calculateBoughtAndSoldTotals()
    {
        if (!myCommodity.isFlowsValid())
        {
            logger.fatal("Calculating Bought and Sold Totals when the flows aren't valid");
            throw new RuntimeException(
                    "Calculate Bought and Sold Totals when the flows aren't valid");
        }
        double total = 0;
        for (int i = 0; i < buyingFromExchangeFlows.length; i++)
        {
            if (buyingFromExchangeFlows[i] != null)
            {
                total += buyingQuantities[i];
            }
        }
        lastCalculatedBuyingTotal = -total;
        total = 0;
        for (int i = 0; i < sellingToExchangeFlows.length; i++)
        {
            if (sellingToExchangeFlows[i] != null)
            {
                total += sellingQuantities[i];
            }
        }
        lastCalculatedSellingTotal = total;
        boughtAndSoldTotalsValid = true;
    }

    public double soldTotal()
    {
        if (!boughtAndSoldTotalsValid)
        {
            calculateBoughtAndSoldTotals();
        }
        return lastCalculatedSellingTotal;
    }

    /**
     * @param maxChangePortion
     *            compare change in size terms with this (as a proportion)
     * @return true if the maximum change in size term was less than
     *         maxChangePortion
     */
    public boolean setSizeTermsBasedOnCurrentQuantities(double maxChangePortion)
    {
        final double[] iE = importsAndExports(price);
        final double newBuyingSizeTerm = soldTotal() + iE[0];
        final double newSellingSizeTerm = boughtTotal() + iE[1];
        boolean belowTolerance = true;
        if (getBuyingSizeTerm() == 0 && newBuyingSizeTerm != 0)
        {
            belowTolerance = false;
        }
        if (getBuyingSizeTerm() != 0)
        {
            if (Math.abs(newBuyingSizeTerm - getBuyingSizeTerm()) / getBuyingSizeTerm() > maxChangePortion)
            {
                belowTolerance = false;
            }
        }
        if (getSellingSizeTerm() == 0 && newSellingSizeTerm != 0)
        {
            belowTolerance = false;
        }
        if (getSellingSizeTerm() != 0)
        {
            if (Math.abs(newSellingSizeTerm - getSellingSizeTerm()) / getSellingSizeTerm() > maxChangePortion)
            {
                belowTolerance = false;
            }
        }
        setBuyingSizeTerm(newBuyingSizeTerm);
        setSellingSizeTerm(newSellingSizeTerm);
        return belowTolerance;

    }

    public void setLastCalculatedBuyingTotal(double d)
    {
        lastCalculatedBuyingTotal = d;
    }

    public void setLastCalculatedSellingTotal(double d)
    {
        lastCalculatedSellingTotal = d;
    }

    public void setBoughtAndSoldTotalsValid(boolean boughtAndSoldTotalsValid)
    {
        this.boughtAndSoldTotalsValid = boughtAndSoldTotalsValid;
    }

    public boolean isBoughtAndSoldTotalsValid()
    {
        return boughtAndSoldTotalsValid;
    }

    public boolean isDoSearch()
    {
        return doSearch;
    }

    public void setDoSearch(boolean doSearch)
    {
        this.doSearch = doSearch;
        myCommodity.updateHasFixedPrices();
    }

}
