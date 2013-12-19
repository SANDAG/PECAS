/*
 * Copyright 2005 PB Consult Inc and HBA Specto Incorporated
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
package com.hbaspecto.pecas.aa.control;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.hbaspecto.models.FutureObject;
import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.IResource;
import com.hbaspecto.pecas.ModelDidntWorkException;
import com.hbaspecto.pecas.NoAlternativeAvailable;
import com.hbaspecto.pecas.OverflowException;
import com.hbaspecto.pecas.aa.activities.AggregateActivity;
import com.hbaspecto.pecas.aa.activities.AmountInZone;
import com.hbaspecto.pecas.aa.activities.ProductionActivity;
import com.hbaspecto.pecas.aa.commodity.AbstractCommodity;
import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.hbaspecto.pecas.aa.commodity.CommodityZUtility;
import com.hbaspecto.pecas.aa.commodity.Exchange;
import com.pb.common.util.ResourceUtil;

import drasys.or.linear.algebra.Algebra;
import drasys.or.linear.algebra.CroutPivot;
import drasys.or.matrix.DenseMatrix;
import drasys.or.matrix.DenseVector;
import drasys.or.matrix.Matrix;
import drasys.or.matrix.SMFWriter;

/**
 * aaModel essentially has to: 1) Call migrationAndAllocation for each
 * ProductionActivity for a given time step. 2) if some Production Activities
 * are modelled as being in equilibrium with each other, aaModel will need to
 * adjust prices/utilities and then call reMigrationAndReAllocation for those
 * ProductionActivities repeatedly, adjusting prices until an equilibrium is
 * achived Note that different ProductionActivities could have different time
 * steps. Thus a DisaggregateActivity could be simulated with a monthly time
 * step with some prices adjusted after each month, while a set of
 * AggregateActivities are simulated as being in equilibrium at the end of each
 * one-year time period.
 * 
 * @author John Abraham
 */
public class AAModel
{

    protected static Logger      logger                             = Logger.getLogger(AAModel.class);

    private double               conFac                             = 0.01;                           // default
                                                                                                       // value

    private double               minimumStepSize                    = 1.0E-4;                         // default
                                                                                                       // value

    private double               stepSize                           = .5;                             // actual
                                                                                                       // stepSize
                                                                                                       // that
                                                                                                       // will
                                                                                                       // be
                                                                                                       // used
                                                                                                       // by
                                                                                                       // aa,
    // will go up or down depending on the

    // merit measure. It is changed via the "increaseStepSize"
    // and "decreaseStepSizeAndAdjustPrices" method.
    private double               maximumStepSize                    = 1.0;                            // default
                                                                                                       // value

    // re-activate this code if we need some specific adjustments for specific
    // stubborn commodities
    private double               commoditySpecificScalingAdjustment = 0;                              // default
                                                                                                       // value

    HashMap                      newPricesC                         = null;                           // a
                                                                                                       // new
                                                                                                       // HashMap
                                                                                                       // will
                                                                                                       // be
                                                                                                       // created
                                                                                                       // every
                                                                                                       // time
    // "calculateNewPrices" is called

    HashMap                      oldPricesC                         = new HashMap();

    public double                convergenceTolerance;

    double                       localPriceStepSizeAdjustment       = 1.0;

    public ResourceBundle        aaRb;

    public static ResourceBundle lastAaRb;

    private static Executor      commodityThreadPool;

    private static Executor      activityThreadPool;

    /*
     * This constructor is used by AAControl and by AADAF - The aa.properties
     * file will not be in the classpath when running on the cluster (or even in
     * the mono-version when running for mulitple years using AO) and therefore
     * we need to pass in the ResourceBundle to aaModel from within the
     * AAServerTask. Because there may be more than 1 aa.properties file over
     * the course of a 30 year simulation it is not practical to include 30
     * different location in the classpath. AO will locate the most recent
     * aa.properties file based on the timeInterval and will write it's absolute
     * path location into a file called 'RunParameters.txt' where it will be
     * read in by AAServerTask in it's 'onStart( ) method and will subsequently
     * be passed to aaModel during the 'new aaModel(rb)' call.
     */
    public AAModel(ResourceBundle aaRb)
    {
        setResourceBundles(aaRb);
        final String initialStepSizeString = ResourceUtil.getProperty(aaRb, "aa.initialStepSize");
        if (initialStepSizeString == null)
        {
            logger.info("*   No aa.initialStepSize set in properties file -- using default");
        } else
        {
            final double iss = Double.valueOf(initialStepSizeString);
            stepSize = iss; // set the stepSize to the initial value
            logger.info("*   Initial step size set to " + iss);
        }

        minimumStepSize = ResourceUtil.getDoubleProperty(aaRb, "aa.minimumStepSize", 1.0E-4);
        logger.info("*   Minimum step size set to " + minimumStepSize);

        conFac = ResourceUtil.getDoubleProperty(aaRb, "aa.ConFac");

        final String maximumStepSizeString = ResourceUtil.getProperty(aaRb, "aa.maximumStepSize");
        if (maximumStepSizeString == null)
        {
            logger.info("*   No aa.maximumStepSize set in properties file -- using default");
        } else
        {
            final double mss = Double.valueOf(maximumStepSizeString);
            maximumStepSize = mss;
            logger.info("*   Maximum step size set to " + mss);
        }

        final String convergedString = ResourceUtil.getProperty(aaRb, "aa.converged");
        if (convergedString == null)
        {
            logger.info("*   No aa.converged set in properties file -- using default");
        } else
        {
            final double converged = Double.valueOf(convergedString);
            convergenceTolerance = converged;
            logger.info("*   Convergence tolerance set to " + converged);
        }

        final String localPriceStepSizeAdjustmentString = ResourceUtil.getProperty(aaRb,
                "aa.localPriceStepSizeAdjustment");
        if (localPriceStepSizeAdjustmentString == null)
        {
            logger.info("*   No aa.localPriceStepSizeAdjustment set in properties file -- using default of 1.0");
        } else
        {
            final double lpssa = Double.valueOf(localPriceStepSizeAdjustmentString);
            localPriceStepSizeAdjustment = lpssa;
            logger.info("*   Local price step size adjustment set to " + lpssa);
        }

        // different step sizes for stubborn commodities
        final String commoditySpecificAdjustmentString = ResourceUtil.getProperty(aaRb,
                "aa.commoditySpecificAdjustment");
        if (commoditySpecificAdjustmentString == null)
        {
            logger.info("*   No aa.commoditySpecificAdjustment set in properties file -- using default");
        } else
        {
            final double csa = Double.valueOf(commoditySpecificAdjustmentString);
            commoditySpecificScalingAdjustment = csa;
            logger.info("*   Commodity specific adjustment set to " + csa);
        }
        

    }

    public boolean calculateExchangeSizeTermsForSpecifiedNonFloorspace()
    {
        snapShotCurrentPrices();
        // set prices to default values;
        // set size terms to 1.0 just in case.
        for (final AbstractCommodity ac : AbstractCommodity.getAllCommodities())
        {
            final Commodity c = (Commodity) ac;
            for (final Exchange x : c.getAllExchanges())
            {
                x.setPrice(c.getExpectedPrice());
                if (!c.isFloorspaceCommodity() && !c.isManualSizeTerms())
                {
                    x.setBuyingSizeTerm(1.0);
                    x.setSellingSizeTerm(1.0);
                }
            }

        }

        // don't use zone constants with size terms
        for (final ProductionActivity p : ProductionActivity.getAllProductionActivities())
        {
            p.setUsingZoneConstants(false);
        }
        boolean nanPresent = false;
        if (!nanPresent)
        {
            nanPresent = calculateCompositeBuyAndSellUtilities(); // this is
            // distributed
        }
        // calculates TP,TC, dTP,dTC for all activities in all zones
        if (!nanPresent)
        {
            nanPresent = calculateLocationConsumptionAndProduction();
        }
        // calculates the B qty and S qty for each commodity in all zones
        if (!nanPresent)
        {
            nanPresent = allocateQuantitiesToFlowsAndExchanges(true); // this is
            // distributed
        }
        if (nanPresent)
        {
            logger.fatal("Default prices caused overflow in size term calculation");
            throw new RuntimeException("Default prices caused overflow in size term calculation");
        }
        final boolean belowTolerance = true;
        for (final AbstractCommodity ac : AbstractCommodity.getAllCommodities())
        {
            final Commodity c = (Commodity) ac;
            if (!c.isFloorspaceCommodity() && !c.isManualSizeTerms())
            {
                for (final Exchange x : c.getAllExchanges())
                {
                    x.setSizeTermsBasedOnCurrentQuantities(1.0);
                }
            }
        }
        // gotta use those zone constants in general!
        for (final ProductionActivity p : ProductionActivity.getAllProductionActivities())
        {
            p.setUsingZoneConstants(true);
        }
        backUpToLastValidPrices();
        return belowTolerance;

    }

    /*
     * This method calculates CUBuy and CUSell for each commodity in each zone.
     * These are the logsum of the destination (or origin) choice models.
     * 
     * The prices must have been set already in each Exchange object for the
     * Commodity. These prices are effectively the input to this routine,
     * although they do not appear in the parameter list because a precondition
     * is that the prices have already been set.
     * 
     * The Commodity object must also know that the prices have been changed,
     * this can be accomplished by calling the unfixPricesAndConditions method.
     * 
     * The output values are set in the appropriate commodityZUtility object in
     * CommodityZUtility.lastCalculatedUtility, thus this method does not return
     * any results; the values have been calculated
     */
    public boolean calculateCompositeBuyAndSellUtilities()
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Entering 'fixPricesAndConditionsForAllCommodities'");
        }
        final long startTime = System.currentTimeMillis();
        Commodity.unfixPricesAndConditionsForAllCommodities();
        boolean nanPresent = false;
        final Iterator allOfUs = AbstractCommodity.getAllCommodities().iterator();

        class ConditionCalculator
                implements Runnable
        {

            double[]        prices;
            double[][]      zutilities;

            final Commodity c;
            FutureObject    worked = new FutureObject();

            ConditionCalculator(Commodity cParam)
            {
                c = cParam;
                // SMP program, prices already set in shared memory
            }

            @Override
            public void run()
            {
                try
                {
                    // this is the method call where we are telling the
                    // commodity object
                    // c that the prices have been set, and hence it should
                    // calculate the CommodityZUtility.lastCalculatedUtility
                    // values.

                    zutilities = c.fixPricesAndConditionsAtNewValues();
                } catch (final OverflowException e)
                {
                    worked.setValue(e);
                }
                if (!worked.isSet())
                {
                    worked.setValue(new Boolean(true));
                }
            }

        }

        final ArrayList<ConditionCalculator> conditionCalculators = new ArrayList<ConditionCalculator>();

        while (allOfUs.hasNext())
        {
            final Commodity c = (Commodity) allOfUs.next();

            final ConditionCalculator calc = new ConditionCalculator(c);
            conditionCalculators.add(calc);
            getCommodityThreadPool().execute(calc);
        }
        for (int c = 0; c < conditionCalculators.size(); c++)
        {
            Object worked;
            try
            {
                worked = conditionCalculators.get(c).worked.getValue();
            } catch (final InterruptedException e)
            {
                throw new RuntimeException("Thread was interrupted");
            }
            if (worked instanceof OverflowException)
            {
                nanPresent = true;
                logger.error("Overflow error in CUBuy, CUSell calcs " + worked);
                ((OverflowException) worked).printStackTrace();
            }
        }
        logger.info("Composite buy and sell utilities have been calculated for all commodities. Time in seconds: "
                + (System.currentTimeMillis() - startTime) / 1000.0);
        return nanPresent;
    }

    /**
     * This method calculates the TC, TP and derivative of TC and derivative of
     * TP for each commodity in each zone. Along the way, it calculates several
     * utilitiy functions. The TC, TP and dTC, dTP are stored in the appropriate
     * CommodityZUtilitiy object (quantity and derivative)
     */
    public boolean calculateLocationConsumptionAndProduction()
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Beginning Activity Iteration: calling 'migrationAndAllocationWithOverflowTracking' for each activity");
        }
        CommodityZUtility.resetCommodityBoughtAndSoldQuantities();
        final long startTime = System.currentTimeMillis();
        boolean nanPresent = false;
        int count = 1;
        final Iterator it = ProductionActivity.getAllProductionActivities().iterator();
        while (it.hasNext())
        {
            final AggregateActivity aa = (AggregateActivity) it.next();
            final long activityStartTime = System.currentTimeMillis();
            try
            {
                aa.migrationAndAllocation(1.0, 0, 0);
            } catch (final OverflowException e)
            {
                nanPresent = true;
                logger.warn("Overflow error in CUBuy, CUSell calcs " + e);
                e.printStackTrace();
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("Finished activity " + count + " in "
                        + (System.currentTimeMillis() - activityStartTime) / 1000.0 + " seconds");
            }
            count++;
        }
        logger.info("Finished all Activity allocation: Time in seconds: "
                + (System.currentTimeMillis() - startTime) / 1000.0);
        return nanPresent;
    }

    /**
     * This method calculates the TC, TP and derivative of TC and derivative of
     * TP for each commodity in each zone. Along the way, it calculates several
     * utility functions. The TC, TP and dTC, dTP are stored in the appropriate
     * CommodityZUtilitiy object (quantity and derivative)
     */
    public boolean recalculateLocationConsumptionAndProduction()
    {

        class LocationConsumptionProductionAllocator
                implements Runnable
        {
            final AggregateActivity mine;
            FutureObject            done = new FutureObject();

            LocationConsumptionProductionAllocator(AggregateActivity aParam)
            {
                mine = aParam;
            }

            @Override
            public void run()
            {
                try
                {
                    mine.reMigrationAndReAllocationWithOverflowTracking();
                } catch (final OverflowException e)
                {
                    logger.warn("Overflow error in CUBuy, CUSell calcs " + e);
                    e.printStackTrace();
                    done.setValue(e);
                }
                if (!done.isSet())
                {
                    done.setValue(new Boolean(true));
                }
            }

        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Beginning Activity Iteration: calling 'reMigrationAndAllocationWithOverflowTracking' for each activity");
        }
        CommodityZUtility.resetCommodityBoughtAndSoldQuantities();
        final long startTime = System.currentTimeMillis();
        boolean nanPresent = false;
        int count = 1;
        final Iterator it = ProductionActivity.getAllProductionActivities().iterator();
        final ArrayList<LocationConsumptionProductionAllocator> allocators = new ArrayList<LocationConsumptionProductionAllocator>();
        while (it.hasNext())
        {
            final AggregateActivity aa = (AggregateActivity) it.next();
            final LocationConsumptionProductionAllocator allocator = new LocationConsumptionProductionAllocator(
                    aa);
            allocators.add(allocator);
            getActivityThreadPool().execute(allocator);
        }
        for (int anum = 0; anum < allocators.size(); anum++)
        {
            // will block until it's done
            Object done;
            try
            {
                done = allocators.get(anum).done.getValue();
            } catch (final InterruptedException e)
            {
                logger.fatal("Unexpected interrupted exception");
                throw new RuntimeException("Unexpected interrupted exception", e);
            }
            if (done instanceof OverflowException)
            {
                nanPresent = true;
                logger.warn("Overflow error in CUBuy, CUSell calcs " + done);
                ((OverflowException) done).printStackTrace();
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("Finished activity " + anum);
            }
            count++;
        }
        logger.info("Finished all Activity allocation: Time in seconds: "
                + (System.currentTimeMillis() - startTime) / 1000.0);
        return nanPresent;
    }

    /**
     * This method calculates the Buying and Selling quantities of each
     * commodity produced or consumed in each zone that is allocated to a
     * particular exchange zone for selling or buying. Bc,z,k and Sc,z,k.
     * 
     * This is the multithreaded version for a shared memory (multicore)
     * machine.
     * 
     * @param settingSizeTerms
     *            indicates whether we are currently setting size terms, ignored
     *            in this Symmetric Multiprocessing version but in
     *            multiple-machine subclasses this is important
     */
    public boolean allocateQuantitiesToFlowsAndExchanges(boolean settingSizeTerms)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Beginning 'allocateQuantitiesToFlowsAndExchanges'");
        }
        final long startTime = System.currentTimeMillis();
        boolean nanPresent = false;
        Commodity.clearAllCommodityExchangeQuantities();
        // iterates through the
        // exchange objects
        // inside the commodity
        // objects and sets the sell, buy qtys and the derivatives to 0
        final Iterator allComms = AbstractCommodity.getAllCommodities().iterator();
        final int count = 1;

        class FlowAllocator
                implements Runnable
        {

            final Commodity c;
            FutureObject    worked = new FutureObject();

            FlowAllocator(Commodity cParam)
            {
                c = cParam;
            }

            @Override
            public void run()
            {
                OverflowException error = null;
                final Hashtable<Integer, CommodityZUtility> ht;
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
                            czu.allocateQuantityToFlowsAndExchanges();
                        } catch (final OverflowException e)
                        {
                            error = e;
                            logger.warn("Overflow error in Bc,z,k and Sc,z,k calculations");
                        }
                    }
                }
                if (error != null)
                {
                    worked.setValue(error);
                } else
                {
                    worked.setValue(new Boolean(true));
                }
            }

        }

        final ArrayList<FlowAllocator> flowAllocators = new ArrayList<FlowAllocator>();
        // creating a job with one task for each commodity.
        while (allComms.hasNext())
        {
            final Commodity c = (Commodity) allComms.next();
            final FlowAllocator flower = new FlowAllocator(c);
            final long activityStartTime = System.currentTimeMillis();
            flowAllocators.add(flower);
            getCommodityThreadPool().execute(flower);
        }
        //
        // getting the results back.
        for (int cnum = 0; cnum < flowAllocators.size(); cnum++)
        {
            final FlowAllocator flower = flowAllocators.get(cnum);
            Object worked;
            try
            {
                worked = flower.worked.getValue();
            } catch (final InterruptedException e)
            {
                logger.fatal("Thread was interrupted unexpectedly");
                throw new RuntimeException("Thread was interrupted unexpectedly", e);
            }
            if (worked instanceof Exception)
            {
                logger.warn("Overflow error in Bc,z,k and Sc,z,k calculations for " + flower.c,
                        (Exception) worked);
                nanPresent = true;
            } else
            {
                flower.c.setFlowsValid(true);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Finished allocating commodity " + flower.c);
                }
            }
        }
        logger.info("All commodities have been allocated.  Time in seconds: "
                + (System.currentTimeMillis() - startTime) / 1000.0);
        return nanPresent;
    }

    public double calculateMeritMeasureWithoutLogging() throws OverflowException
    {
        boolean nanPresent = false;
        double meritMeasure = 0;
        final Iterator commodities = AbstractCommodity.getAllCommodities().iterator();
        while (commodities.hasNext())
        {
            final Commodity c = (Commodity) commodities.next();
            final Iterator exchanges = c.getAllExchanges().iterator();
            while (exchanges.hasNext() && !nanPresent)
            {
                final Exchange ex = (Exchange) exchanges.next();
                final double surplus = ex.exchangeSurplus();
                if (Double.isNaN(surplus))
                {
                    nanPresent = true;
                    throw new OverflowException("NaN present at " + ex);
                }
                if (c.isDoSearch() && ex.isDoSearch())
                {
                    meritMeasure += c.compositeMeritMeasureWeighting
                            * c.compositeMeritMeasureWeighting * surplus * surplus;
                }
            }
        }
        return meritMeasure;
    }

    public double calculateMeritMeasureWithLogging() throws OverflowException
    {
        boolean nanPresent = false;
        double meritMeasure = 0;
        final Iterator commodities = AbstractCommodity.getAllCommodities().iterator();
        while (commodities.hasNext())
        {
            int numExchanges = 0;
            double totalSurplus = 0;
            double totalPrice = 0;
            double maxSurplus = 0;
            double maxSurplusSigned = 0;
            double maxPrice = -Double.MAX_VALUE;
            double minPrice = Double.MAX_VALUE;
            double commodityMeritMeasure = 0;
            Exchange minPriceExchange = null;
            Exchange maxPriceExchange = null;

            Exchange maxExchange = null;
            final Commodity c = (Commodity) commodities.next();
            logger.info(c.toString());
            final Iterator exchanges = c.getAllExchanges().iterator();
            while (exchanges.hasNext() && !nanPresent)
            {
                final Exchange ex = (Exchange) exchanges.next();
                final double surplus = ex.exchangeSurplus();
                if (Math.abs(surplus) > maxSurplus)
                {
                    maxExchange = ex;
                    maxSurplus = Math.abs(surplus);
                    maxSurplusSigned = surplus;
                }
                totalSurplus += ex.exchangeSurplus();
                totalPrice += ex.getPrice();
                numExchanges++;
                if (ex.getPrice() > maxPrice)
                {
                    maxPrice = ex.getPrice();
                    maxPriceExchange = ex;
                }
                if (ex.getPrice() < minPrice)
                {
                    minPrice = ex.getPrice();
                    minPriceExchange = ex;
                }
                if (Double.isNaN(surplus))
                { /* || newPrice.isNaN() */
                    nanPresent = true;
                    logger.warn("\t NaN present at " + ex);
                    throw new OverflowException("\t NaN present at " + ex);
                }
                if (c.isDoSearch() && ex.isDoSearch())
                {
                    meritMeasure += c.compositeMeritMeasureWeighting
                            * c.compositeMeritMeasureWeighting * surplus * surplus;
                }
                commodityMeritMeasure += surplus * surplus;
            }
            if (maxExchange != null)
            {
                logger.info("\t maxSurp: " + maxSurplusSigned + " in " + maxExchange
                        + ". Current price is " + maxExchange.getPrice() /*
                                                                          * changing
                                                                          * " to "
                                                                          * +
                                                                          * newPriceAtMaxExchange
                                                                          */);
            }
            if (maxPriceExchange != null)
            {
                logger.info("\t PMax " + maxPrice + " in " + maxPriceExchange);
            }
            if (minPriceExchange != null)
            {
                logger.info("\t PMin " + minPrice + " in " + minPriceExchange);
            }
            logger.info("\t Total surplus " + totalSurplus);
            logger.info("\t Average price " + totalPrice / numExchanges);

            final double rmsError = Math.sqrt(commodityMeritMeasure / c.getAllExchanges().size());
            final double oldRmsError = Math.sqrt(c.oldMeritMeasure / c.getAllExchanges().size());
            if (commodityMeritMeasure > c.oldMeritMeasure)
            {
                if (c.scalingAdjustmentFactor > 1)
                {
                    c.scalingAdjustmentFactor = 1.0;
                    // c.scalingAdjustmentFactor /=
                    // Math.pow(1+commoditySpecificScalingAdjustment,3);
                }
                logger.info("\t Commodity RMS Error " + rmsError + " NOT IMPROVING was "
                        + oldRmsError + " (adj now " + c.scalingAdjustmentFactor + ")");
            } else
            {
                c.scalingAdjustmentFactor *= 1 + commoditySpecificScalingAdjustment;
                logger.info("\t Commodity RMS Error " + rmsError + " (was " + oldRmsError
                        + ") (adj now " + c.scalingAdjustmentFactor + ")");
            }
            logger.info("\t Weighted Commodity Merit Measure is " + commodityMeritMeasure
                    * c.compositeMeritMeasureWeighting * c.compositeMeritMeasureWeighting);
            final double tClear = Math.sqrt(commodityMeritMeasure
                    * c.compositeMeritMeasureWeighting * c.compositeMeritMeasureWeighting)
                    / c.getAverageExchangeTotal();
            final double maxSClear = c.getLargestSClear(conFac);
            logger.info("\t TClear is " + tClear + ",  maxSClear is " + maxSClear);
            c.oldMeritMeasure = commodityMeritMeasure;

        }
        return meritMeasure;

    }

    public void increaseStepSize()
    {
        final double increaseStepSizeMultiplier = 1.1;
        stepSize = stepSize * increaseStepSizeMultiplier;
        if (stepSize > maximumStepSize)
        {
            stepSize = maximumStepSize;
        }
    }

    public void decreaseStepSizeAndAdjustPrices()
    {
        final double decreaseStepSizeMultiplier = 0.5;
        stepSize = stepSize * decreaseStepSizeMultiplier;
        if (stepSize < minimumStepSize)
        {
            stepSize = minimumStepSize;
        }
        newPricesC = StepPartwayBackBetweenTwoOtherPrices(oldPricesC, newPricesC,
                decreaseStepSizeMultiplier);
        setExchangePrices(newPricesC);
    }

    public void decreaseStepSizeEvenIfBelowMinimumAndAdjustPrices()
    {
        final double decreaseStepSizeMultiplier = 0.5;
        stepSize = stepSize * decreaseStepSizeMultiplier;
        if (stepSize < minimumStepSize)
        {
            logger.warn("Setting step size to " + stepSize + " which is *below* minimum of "
                    + minimumStepSize);
        }
        newPricesC = StepPartwayBackBetweenTwoOtherPrices(oldPricesC, newPricesC,
                decreaseStepSizeMultiplier);
        setExchangePrices(newPricesC);
    }

    public void backUpToLastValidPrices()
    {
        setExchangePrices(oldPricesC);
    }

    public void calculateNewPricesUsingBlockDerivatives(boolean calcDeltaUsingDerivatives)
    {
        // ENHANCEMENT change this to use the MTJ library for matrices instead
        // of using the ORObjects library which is not open source and is no
        // longer available
        final Algebra a = new Algebra();
        newPricesC = new HashMap();
        logger.info("Calculating average commodity price change");
        AveragePriceSurplusDerivativeMatrix.calculateMatrixSize();

        final AveragePriceSurplusDerivativeMatrix avgMatrix = makeAveragePriceSurplusDerivativeMatrix();
        final DenseVector totalSurplusVector = new TotalSurplusVector();
        for (int i = 0; i < totalSurplusVector.size(); i++)
        {
            totalSurplusVector.setElementAt(i, totalSurplusVector.elementAt(i) * -1);
        }

        // try something like Levenberg Marquadt, increasing the diagonals
        if (stepSize < 1)
        {
            for (int i = 0; i < avgMatrix.sizeOfColumns(); i++)
            {
                avgMatrix.setElementAt(i, i, avgMatrix.elementAt(i, i) / stepSize);
            }
        }
        DenseVector averagePriceChange = null;
        try
        {
            final CroutPivot solver = new CroutPivot(avgMatrix);
            averagePriceChange = solver.solveEquations(totalSurplusVector);
            // avgMatrix.solve(totalSurplusVector,averagePriceChange);
        } catch (final Exception e)
        {
            logger.error("Can't solve average price matrix " + e);
            writeOutMatrix(avgMatrix, e);
        }

        if (stepSize >= 1)
        {
            for (int i = 0; i < averagePriceChange.size(); i++)
            {
                averagePriceChange.setElementAt(i, averagePriceChange.elementAt(i) * stepSize);
            }
        }

        final Iterator comIt = AbstractCommodity.getAllCommodities().iterator();
        int commodityNumber = 0;
        while (comIt.hasNext())
        {
            final Commodity c = (Commodity) comIt.next();
            if (logger.isDebugEnabled())
            {
                logger.debug("Calculating local price change for commodity " + c);
            }
            double[] deltaPricesDouble = null;
            if (calcDeltaUsingDerivatives)
            {
                try
                {
                    logger.debug("Calculating local price change for commodity " + c
                            + " using matrix calculations");
                    final CommodityPriceSurplusDerivativeMatrix comMatrixData = new CommodityPriceSurplusDerivativeMatrix(
                            c);
                    final DenseMatrix comMatrix = new DenseMatrix(comMatrixData.data);
                    final double[] surplus = c.getSurplusInAllExchanges();
                    for (int i = 0; i < surplus.length; i++)
                    {
                        surplus[i] *= -1;
                    }
                    final DenseVector deltaSurplusPlus = new DenseVector(surplus);
                    // for (int i=0;i<surplus.length;i++) {
                    // deltaSurplusPlus.setElementAt(i,-surplus[i]-totalSurplusVector.elementAt(commodityNumber)/surplus.length);
                    // }
                    // deltaSurplusPlus.setElementAt(surplus.length,0);
                    // DenseMatrix crossTransposed = new
                    // DenseMatrix(surplus.length,surplus.length);
                    // comMatrix.transAmult(comMatrix,crossTransposed);
                    // DenseVector crossTransposedVector = new
                    // DenseVector(surplus.length);
                    // comMatrix.transMult(deltaSurplusPlus,crossTransposedVector);
                    final DenseVector deltaPrices = new DenseVector(surplus.length);
                    // regular solution
                    // crossTransposed.solve(crossTransposedVector,deltaPrices);
                    // using the libraries least squares type rectangular matrix
                    // solver
                    // try {
                    // comMatrix.solve(deltaSurplusPlus,deltaPrices);
                    // } catch (MatrixSingularException e) {
                    // e.printStackTrace();
                    // throw new RuntimeException("Can't find delta prices for
                    // commodity "+c,e);
                    // }

                    // something like Levenberg Marquadt where we increase the
                    // diagonal
                    if (stepSize * localPriceStepSizeAdjustment * c.scalingAdjustmentFactor < 1)
                    {
                        for (int i = 0; i < comMatrix.sizeOfColumns(); i++)
                        {
                            comMatrix.setElementAt(i, i, comMatrix.elementAt(i, i) / stepSize
                                    / localPriceStepSizeAdjustment / c.scalingAdjustmentFactor);
                        }
                    }
                    final CroutPivot solver2 = new CroutPivot(comMatrix);
                    solver2.solveEquations(deltaSurplusPlus, deltaPrices);
                    if (stepSize * localPriceStepSizeAdjustment * c.scalingAdjustmentFactor > 1)
                    {
                        for (int i = 0; i < deltaPrices.size(); i++)
                        {
                            deltaPrices.setElementAt(i, deltaPrices.elementAt(i) * stepSize
                                    * localPriceStepSizeAdjustment * c.scalingAdjustmentFactor);
                        }
                    }

                    // ENHANCEMENT experiment with not setting average price
                    // change to zero (ie comment out this block)
                    final double totalPrice = deltaPrices.sum();
                    deltaPricesDouble = deltaPrices.getArray();
                    // make sure average price change is zero
                    for (int i = 0; i < deltaPricesDouble.length; i++)
                    {
                        deltaPricesDouble[i] -= totalPrice / deltaPricesDouble.length;
                    }

                } catch (final Exception e)
                {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            } else
            {
                logger.debug("Calculating local price change for commodity " + c
                        + " using diagonal approximation");
                final List exchanges = c.getAllExchanges();
                deltaPricesDouble = new double[exchanges.size()];
                double totalIncrease = 0;
                int numExchanges = 0;
                for (int xNum = 0; xNum < exchanges.size(); xNum++)
                {
                    final Exchange x = (Exchange) exchanges.get(xNum);
                    final double[] sAndD = x.exchangeSurplusAndDerivative();
                    final double increase = -sAndD[0] / sAndD[1];

                    // double increase =
                    // (-sAndD[0]-totalSurplusVector.elementAt(commodityNumber)/deltaPricesDouble.length)/sAndD[1];
                    deltaPricesDouble[xNum] = increase * stepSize * localPriceStepSizeAdjustment
                            * c.scalingAdjustmentFactor;
                    // TODO remove this debug code
                    // Debug code of 2007.05.10
                    if (x.monitor)
                    {
                        logger.info("In " + x + " surplus is " + sAndD[0] + " derivative is "
                                + sAndD[1] + " unscaled local price change " + increase
                                + " scaled " + deltaPricesDouble[xNum]);
                    }
                    totalIncrease += increase;
                    numExchanges++;
                }
                // ENHANCEMENT but average price change for this commodity
                // should be zero because the average price change is calculated
                // separately, try reactivating this code!
                // for (int xNum=0;xNum<exchanges.size();xNum++) {
                // deltaPricesDouble[xNum] -= totalIncrease/numExchanges;
                // }
            }
            final List exchanges = c.getAllExchanges();
            for (int xNum = 0; xNum < exchanges.size(); xNum++)
            {
                final Exchange x = (Exchange) exchanges.get(xNum);
                double price;
                // already did scaling for stepSize and
                // localPriceStepSizeAdjsutment
                price = x.getPrice() + averagePriceChange.elementAt(commodityNumber)
                        + deltaPricesDouble[xNum];
                if (c.isHasFixedPrices())
                {
                    // don't do average price change for this commodity if some
                    // zones are not changing their prices,
                    // unfair to the zones that ARE chaning their prices to try
                    // to solve any global inbalance problem with the commodity.
                    price = x.getPrice() + deltaPricesDouble[xNum];
                }
                if (Double.isNaN(price))
                {
                    logger.error("Planning NaN price in " + x + " oldPrice:" + x.getPrice()
                            + " averagePriceChange:"
                            + averagePriceChange.elementAt(commodityNumber)
                            + " local price change:" + deltaPricesDouble[xNum]);
                    writeOutMatrix(avgMatrix, new RuntimeException("Planning NaN price in " + x
                            + " oldPrice:" + x.getPrice() + " averagePriceChange:"
                            + averagePriceChange.elementAt(commodityNumber)
                            + " local price change:" + deltaPricesDouble[xNum]));
                }
                if (x.monitor)
                {
                    logger.info("Planning " + price + " price in " + x + " oldPrice:"
                            + x.getPrice() + " averagePriceChange:"
                            + averagePriceChange.elementAt(commodityNumber)
                            + " local price change:" + deltaPricesDouble[xNum]);
                }
                if (c.isDoSearch() && x.isDoSearch())
                {
                    newPricesC.put(x, new Double(price));
                } else
                {
                    newPricesC.put(x, x.getPrice());
                }
            }
            commodityNumber++;

        }
        setExchangePrices(newPricesC);

    }

    protected AveragePriceSurplusDerivativeMatrix makeAveragePriceSurplusDerivativeMatrix()
    {
        final AveragePriceSurplusDerivativeMatrix avgMatrix = new AveragePriceSurplusDerivativeMatrix();
        avgMatrix.init();
        return avgMatrix;
    }

    protected boolean useJPPF()
    {
        return false;
    }

    private void writeOutMatrix(AveragePriceSurplusDerivativeMatrix avgMatrix, Exception e)
    {
        logger.fatal("Having problems solving for change in average prices", e);
        logger.fatal("Writing out average price change derivative matrix to file AvgMatrix.txt");
        FileOutputStream badMatrixStream = null;
        try
        {
            badMatrixStream = new FileOutputStream(ResourceUtil.getProperty(aaRb, "output.data")
                    + "AvgMatrix.txt");
        } catch (final FileNotFoundException e1)
        {
            logger.fatal("Can't seem to open file " + ResourceUtil.getProperty(aaRb, "output.data")
                    + "AvgMatrix.txt");
            throw new RuntimeException(e);
        }
        final SMFWriter matrixWriter = new SMFWriter(badMatrixStream);
        matrixWriter.writeMatrix(avgMatrix);
        matrixWriter.flush();
        matrixWriter.close();
        try
        {
            badMatrixStream.flush();
            badMatrixStream.close();
        } catch (final IOException e1)
        {
        }
        throw new RuntimeException(e);
    }

    public static void writeOutMatrix(Matrix aMatrix, String matrixName)
    {
        logger.info("Having problems with matrix " + matrixName + " writing it out to a file");
        FileOutputStream badMatrixStream = null;
        try
        {
            badMatrixStream = new FileOutputStream(lastAaRb.getString("output.data") + matrixName
                    + ".txt");
        } catch (final FileNotFoundException e1)
        {
            logger.error("Can't seem to open file " + lastAaRb.getString("output.data")
                    + matrixName + ".txt");
        }
        final SMFWriter matrixWriter = new SMFWriter(badMatrixStream);
        matrixWriter.writeMatrix(aMatrix);
        matrixWriter.flush();
        matrixWriter.close();
        try
        {
            badMatrixStream.flush();
            badMatrixStream.close();
        } catch (final IOException e1)
        {
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void calculateNewPricesUsingDiagonalApproximation()
    {
        newPricesC = new HashMap();
        final Iterator commodities = AbstractCommodity.getAllCommodities().iterator();
        while (commodities.hasNext())
        {
            final Commodity c = (Commodity) commodities.next();
            final Iterator exchanges = c.getAllExchanges().iterator();
            while (exchanges.hasNext())
            {
                final Exchange ex = (Exchange) exchanges.next();
                final double[] surplusAndDerivative = ex.exchangeSurplusAndDerivative();
                final Double newPrice = new Double(ex.getPrice() - stepSize
                        * c.scalingAdjustmentFactor * surplusAndDerivative[0]
                        / surplusAndDerivative[1]);
                if (ex.monitor || Double.isNaN(surplusAndDerivative[0]) || newPrice.isNaN())
                {
                    logger.info("Exchange:" + ex + " surplus:" + surplusAndDerivative[0]
                            + " planning price change from " + ex.getPrice() + " to " + newPrice);
                }
                if (c.isDoSearch() && ex.isDoSearch())
                {
                    newPricesC.put(ex, newPrice);
                } else
                {
                    newPricesC.put(ex, ex.getPrice());
                }
            }
        }
        setExchangePrices(newPricesC);
    }

    /**
     *  
     */
    public void snapShotCurrentPrices()
    {
        final Iterator commodities = AbstractCommodity.getAllCommodities().iterator();
        while (commodities.hasNext())
        {
            final Commodity c = (Commodity) commodities.next();
            final Iterator exchanges = c.getAllExchanges().iterator();
            while (exchanges.hasNext())
            {
                final Exchange ex = (Exchange) exchanges.next();
                oldPricesC.put(ex, new Double(ex.getPrice()));
            }
        }
    }

    private static void setExchangePrices(HashMap prices)
    {
        Iterator it = AbstractCommodity.getAllCommodities().iterator();
        while (it.hasNext())
        {
            final Commodity c = (Commodity) it.next();
            c.unfixPricesAndConditions();
        }
        it = prices.entrySet().iterator();
        while (it.hasNext())
        {
            final Map.Entry e = (Map.Entry) it.next();
            final Exchange x = (Exchange) e.getKey();
            // if (x.myCommodity.name.equals("FLR Agriculture")) {
            // System.out.println(x.getPrice()+" to "+e.getValue()+" in "+x);
            // }
            final Double price = (Double) e.getValue();
            x.setPrice(price.doubleValue());
        }
    }

    /**
     * For each price calculates the difference between the first one and the
     * second one, and returns a new price that is a scaled back step
     * 
     * @param firstPrices
     *            Hashmap of first set of prices keyed by exchange
     * @param secondPrices
     *            Hashmap of second set of prices keyed by exchange
     * @param howFarBack
     *            how far back to step (0.5 is halfway back, 0.75 is 3/4 way
     *            back)
     * @return Hashmap of new prices in between the other two
     */
    private static HashMap StepPartwayBackBetweenTwoOtherPrices(HashMap firstPrices,
            HashMap secondPrices, double howFarBack)
    {
        final HashMap newPrices = new HashMap();
        final Iterator commodities = AbstractCommodity.getAllCommodities().iterator();
        while (commodities.hasNext())
        {
            final Commodity c = (Commodity) commodities.next();
            final Iterator exchanges = c.getAllExchanges().iterator();
            while (exchanges.hasNext())
            {
                final Exchange ex = (Exchange) exchanges.next();
                final Double aggressivePrice = (Double) secondPrices.get(ex);
                final Double previousPrice = (Double) firstPrices.get(ex);
                final double lessAggressivePrice = (aggressivePrice.doubleValue() - previousPrice
                        .doubleValue()) * howFarBack + previousPrice.doubleValue();
                newPrices.put(ex, new Double(lessAggressivePrice));
                if (ex.monitor)
                {
                    logger.info("Exchange:" + ex
                            + "  reducing amount of price change, changing p from "
                            + aggressivePrice + " to " + lessAggressivePrice);
                }
            }
        }
        return newPrices;
    }

    public double getStepSize()
    {
        return stepSize;
    }

    public double getMinimumStepSize()
    {
        return minimumStepSize;
    }

    public void setResourceBundles(ResourceBundle appRb)
    {
        aaRb = appRb;
        lastAaRb = appRb;
    }

    /*
     * This method is called by AO. Before startModel is called the most current
     * aa.properties file has been found in the tn subdirectories and has been
     * set by calling the setApplicationResourceBundle method (a ModelComponent
     * method) from AO.
     * 
     * This is duplicated code from AAControl.main(). If you change this code
     * please change AAControl.main() as well.
     */
    public void startModel(int baseYear, int timeInterval, IResource resourceUtil)
    {
        final String pProcessorClass = ResourceUtil.getProperty(aaRb, "pprocessor.class");
        logger.info("PECAS will be using the " + pProcessorClass + " for pre and post processing");
        AAControl aa;
        try
        {
            aa = new AAControl(Class.forName(pProcessorClass), aaRb, baseYear, timeInterval);
        } catch (final ClassNotFoundException e)
        {
            throw new RuntimeException(pProcessorClass + " could not be instantiated ", e);
        }
        try
        {
            aa.runModelPerhapsWithConstraints(resourceUtil);
        } catch (final ModelDidntWorkException e)
        {
            logger.fatal("AA model did not work, " + e);
            throw new RuntimeException("AA model didn't work", e);
        }
    }

    public void setStepSize(double stepSizeParameter)
    {
        stepSize = stepSizeParameter;

    }

    public void updateActivityZonalConstantsBasedOnConstraints(double adjustment)
    {
        final Logger ascChangeLogger = Logger
                .getLogger("com.pb.models.pecas.aaPProcessor.updateActivityZonalConstants");
        final Collection activities = ProductionActivity.getAllProductionActivities();
        final Iterator it = activities.iterator();
        while (it.hasNext())
        {
            final AggregateActivity a = (AggregateActivity) it.next();
            final AmountInZone[] amounts = a.myDistribution;
            final double totalAmount = a.getTotalAmount();
            double[] probs;
            int runAroundCount = 0;
            double maxAbsChange = 0;
            do
            {
                // have to do this iteratively because the logsum denominator
                // changes
                maxAbsChange = 0;
                runAroundCount++;
                try
                {
                    probs = a.logitModelOfZonePossibilities.getChoiceProbabilities();
                } catch (final NoAlternativeAvailable e)
                {
                    final String msg = "Activity " + a + " seems to have no location alternatives";
                    logger.fatal(msg);
                    throw new RuntimeException(msg, e);
                } catch (final ChoiceModelOverflowException e)
                {
                    final String msg = "Activity " + a
                            + " overflows in allocation when adjusting constants";
                    logger.fatal(msg);
                    throw new RuntimeException(msg, e);
                }
                for (int beta = 0; beta < amounts.length; beta++)
                {
                    if (amounts[beta].isConstrained())
                    {
                        if (amounts[beta].constraintQuantity != amounts[beta].quantity)
                        {
                            final String msg = "Problem in constraint process -- constraint quantity is unequal to quantity for "
                                    + amounts[beta];
                            logger.fatal(msg);
                            throw new RuntimeException("msg");
                        }
                        final double currentConstant = amounts[beta]
                                .getLocationSpecificUtilityInclTaxes();
                        double newConstant = 0;
                        double constantChange = 0;
                        if (amounts[beta].constraintQuantity == 0)
                        {
                            if (probs[beta] == 0)
                            {
                                // We use to leave these unchanged, but then the
                                // zero wasn't always repeatable.
                                newConstant = Double.NEGATIVE_INFINITY;
                                constantChange = Double.NEGATIVE_INFINITY;
                            } else
                            {
                                // with constraint = 0 might as well set the
                                // constant to negative infinity rather than
                                // goofing around with minor changes
                                newConstant = Double.NEGATIVE_INFINITY;
                                constantChange = Double.NEGATIVE_INFINITY;
                            }
                        } else
                        {
                            // constraint !=0
                            if (probs[beta] == 0)
                            {
                                if (runAroundCount == 1)
                                {
                                    ascChangeLogger
                                            .error("In zone "
                                                    + amounts[beta].getMyTaz().getZoneUserNumber()
                                                    + " constraint for "
                                                    + a.name
                                                    + " is non-zero ("
                                                    + amounts[beta].constraintQuantity
                                                    + ") but current amount is zero.  Increasing Zonal ASC by arbitrary amount.");
                                }
                                constantChange = 1 / a.getLocationDispersionParameter()
                                        * adjustment;
                                newConstant = currentConstant + constantChange;
                            } else
                            {
                                // This is the normal case of adjusting the
                                // constant based on ratio
                                final double probRatio = amounts[beta].constraintQuantity
                                        / totalAmount / probs[beta];
                                constantChange = 1 / a.getLocationDispersionParameter()
                                        * Math.log(probRatio);
                                maxAbsChange = Math.max(Math.abs(constantChange), maxAbsChange);
                                newConstant = currentConstant + constantChange;
                            }
                        }
                        ascChangeLogger.debug("Changing zone "
                                + amounts[beta].getMyTaz().getZoneUserNumber() + " constant on "
                                + a.name + " by " + constantChange + " to attempt to change "
                                + amounts[beta].quantity + " to "
                                + amounts[beta].constraintQuantity + ", current constant is "
                                + currentConstant);
                        amounts[beta].setLocationSpecificUtilityInclTaxes(newConstant);
                    }
                }
            } while (maxAbsChange > .000001 / a.getLocationDispersionParameter()
                    && runAroundCount < 500);
            if (runAroundCount >= 500)
            {
                final String msg = "Can't update ASC for activity " + a + ", iterated "
                        + runAroundCount + " times but this isn't converging, giving up";
            }
        }

    }

    public static boolean checkConstraints(double constraintTolerance)
    {
        final Collection activities = ProductionActivity.getAllProductionActivities();
        final Iterator it = activities.iterator();
        boolean ok = true;
        while (it.hasNext())
        {
            final AggregateActivity a = (AggregateActivity) it.next();
            final AmountInZone[] amounts = a.myDistribution;
            for (int beta = 0; beta < amounts.length; beta++)
            {
                if (amounts[beta].isConstrained())
                {
                    if (!checkRelativeError(constraintTolerance, amounts[beta].constraintQuantity,
                            amounts[beta].quantity))
                    {
                        final String msg = "constraint not matched, cons="
                                + amounts[beta].constraintQuantity + " , modelled="
                                + amounts[beta].quantity + ", in " + amounts[beta];
                        logger.error(msg);
                        ok = false;
                    }
                }
            }
        }
        return ok;
    }

    private static boolean checkRelativeError(double tolerance, double target, double modelled)
    {
        boolean isItOk = false;
        if (modelled == 0 && target == 0)
        {
            isItOk = true;
        }
        if (Math.abs(modelled - target) / target < tolerance)
        {
            isItOk = true;
        }
        return isItOk;
    }

    protected static Executor getCommodityThreadPool()
    {
        if (commodityThreadPool == null)
        {
            commodityThreadPool = Executors.newFixedThreadPool(AbstractCommodity
                    .getAllCommodities().size());
        }
        return commodityThreadPool;
    }

    private static Executor getActivityThreadPool()
    {
        if (activityThreadPool == null)
        {
            activityThreadPool = Executors.newFixedThreadPool(ProductionActivity
                    .getAllProductionActivities().size());
        }
        return activityThreadPool;
    }

}
