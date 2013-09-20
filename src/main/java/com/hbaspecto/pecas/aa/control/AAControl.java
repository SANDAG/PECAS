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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import com.hbaspecto.pecas.ModelDidntWorkException;
import com.hbaspecto.pecas.OverflowException;
import com.hbaspecto.pecas.aa.AAStatusLogger;
import com.hbaspecto.pecas.aa.activities.AggregateActivity;
import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.pb.common.util.ResourceUtil;

/**
 * This class runs aa. It loads the data, instantiates the AAModel and writes
 * out the data at the end of aa.
 * 
 * @author Christi Willison
 * @version Mar 16, 2004
 * 
 */
public class AAControl
{
    protected static Logger      logger             = Logger.getLogger(AAControl.class);
    private final int            timePeriod;
    private final ResourceBundle aaRb;
    protected AAPProcessor       aaReaderWriter;
    protected int                exitValue          = 1;                                // 1=aa
                                                                                         // exited
                                                                                         // with
                                                                                         // errors
                                                                                         // i.o.w.
                                                                                         // didn't
    // converge, 0=aa exited without errors iow.
    // converged
    private int                  constraintIteration;
    private double               newMeritMeasure;
    private AAModel              aa;
    private boolean              secondIterationRun = false;

    public AAControl(Class pProcessorClass, ResourceBundle aaRb)
    {
        timePeriod = 1;
        this.aaRb = aaRb;
        try
        {
            aaReaderWriter = (AAPProcessor) pProcessorClass.newInstance();
        } catch (final InstantiationException e)
        {
            logger.fatal("Can't create new instance of AAPProcessor of type "
                    + pProcessorClass.getName());
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (final IllegalAccessException e)
        {
            logger.fatal("Can't create new instance of AAPProcessor of type "
                    + pProcessorClass.getName());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        aaReaderWriter.setResourceBundles(this.aaRb);
        aaReaderWriter.setTimePeriod(timePeriod);
    }

    public AAControl(Class pProcessorClass, ResourceBundle aaRb, int baseYear, int timePeriod)
    {
        this.timePeriod = timePeriod;
        this.aaRb = aaRb;
        try
        {
            aaReaderWriter = (AAPProcessor) pProcessorClass.newInstance();
        } catch (final InstantiationException e)
        {
            logger.fatal("Can't create new instance of AAPProcessor of type "
                    + pProcessorClass.getName());
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (final IllegalAccessException e)
        {
            logger.fatal("Can't create new instance of AAPProcessor of type "
                    + pProcessorClass.getName());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        aaReaderWriter.setResourceBundles(this.aaRb);
        aaReaderWriter.setTimePeriod(this.timePeriod);
        aaReaderWriter.setBaseYear(baseYear);
    }

    public void readData()
    {
        logger.info("Reading data and setting up for PECAS run");
        AAStatusLogger.logText("Reading data and setting up for PECAS run");
        final long startTime = System.currentTimeMillis();
        aaReaderWriter.doProjectSpecificInputProcessing();
        aaReaderWriter.setUpAA();
        AAPProcessor.isSetup = true;
        logger.info("Setup is complete. Time in seconds: "
                + (System.currentTimeMillis() - startTime) / 1000.0);
        AAStatusLogger.logText("Setup is complete.");
        return;
    }

    /**
     * This method performs the price search to try to clear all markets for all
     * commodities
     * 
     * @return 0 if it all worked, >0 otherwise
     */
    public int runAAToFindPrices()
    {
        logger.info("*******************************************************************************************");
        logger.info("* Beginning PECAS AA");
        AAStatusLogger.logText("Beginning PECAS AA");

        final InputStreamReader keyboardInput = new InputStreamReader(System.in);
        final long startTime = System.currentTimeMillis();

        boolean stoppingNextIteration = false;
        boolean isCurrentlyConverged = false;
        int maxIterations = 300; // default value.
        boolean nanPresent = false;
        double oldMeritMeasure = Double.POSITIVE_INFINITY; // sum of squares of
        // the surplus over
        // all commodities,
        // all zones
        int nIterations = 0; // a counter to keep track of how many iterations
        // it takes before the meritMeasure is within
        // tolerance.
        // when calculating the merit measure we can log or not log. If you want
        // to log commodity
        // specific info each time then logFrequency should be set to 1,
        // otherwise set to every 10, 50
        // or 100 iterations to reduce the amount of logging produced.
        final int logFrequency = ResourceUtil.getIntegerProperty(aaRb, "aa.logFrequency", 1);
        logger.info("* NOTE: Commodity Specific Merit Measures will be logged every "
                + logFrequency + " iterations.");

        final boolean isParallel = ResourceUtil.getBooleanProperty(aaRb, "aa.jppfParallel", false);

        if (isParallel)
        {
            aa = new com.hbaspecto.pecas.aa.jppf.JppfAAModel(aaRb);
            logger.info("AA is running in multi-machine mode. To modify this configuration, "
                    + "refer to the aa.jppfParallel property in aa.properties file.");
        } else
        {
            aa = new AAModel(aaRb);
            logger.info("\nAA is NOT running in multi-machine mode (but is using all the cores in the current "
                    + "machine).  To modify this configuration, refer to the aa.jppfParallel property in "
                    + "aa.properties file.");
        }

        // aa = new AAModel(aaRb, globalRb);
        if (Commodity.isCalculateSizeTerms())
        {
            logger.info("Calculating exchange sizes for non-floorspace commodities");
            final boolean forceConstraints = AggregateActivity.isForceConstraints();
            AggregateActivity.setForceConstraints(false); // no constraints in
            // size term
            // calculations.
            final int iteration = 0;
            Commodity.storeExistingSizeTerms();
            aa.calculateExchangeSizeTermsForSpecifiedNonFloorspace();
            Commodity.logSizeTermChanges();
            Commodity.forgetOldSizeTerms();
            AggregateActivity.setForceConstraints(forceConstraints);
        }

        maxIterations = ResourceUtil.getIntegerProperty(aaRb, "aa.maxIterations", maxIterations);
        if (maxIterations == 0)
        {
            stoppingNextIteration = true;
        }

        // flag as to whether to calculate average price
        final boolean calcAvgPrice = ResourceUtil.getBooleanProperty(aaRb,
                "aa.calculateAveragePrices", true);
        final boolean calcDeltaUsingDerivatives = ResourceUtil.getBooleanProperty(aaRb,
                "aa.useFullExchangeDerivatives", false);

        logger.info("*******************************************************************************************");

        // calculates CUBuy and CUSell for all commodities in all zones
        if (!nanPresent)
        {
            nanPresent = aa.calculateCompositeBuyAndSellUtilities(); // this is
            // multicore
        }
        // calculates TP,TC, dTP,dTC for all activities in all zones
        if (!nanPresent)
        {
            nanPresent = aa.calculateLocationConsumptionAndProduction();
        }
        // calculates the B qty and S qty for each commodity in all zones
        if (!nanPresent)
        {
            nanPresent = aa.allocateQuantitiesToFlowsAndExchanges(false); // this
            // is
            // multicore
        }
        newMeritMeasure = Double.POSITIVE_INFINITY;
        if (!nanPresent)
        {
            try
            {
                if (nIterations % logFrequency == 0)
                {
                    newMeritMeasure = aa.calculateMeritMeasureWithLogging();
                } else
                {
                    newMeritMeasure = aa.calculateMeritMeasureWithoutLogging();
                }
            } catch (final OverflowException e)
            {
                logger.warn("Overflow " + e);
                nanPresent = true;
            }
        }

        if (nanPresent)
        {
            logger.fatal("Initial prices cause overflow -- try again changing initial prices");
            throw new RuntimeException(
                    "Initial prices cause overflow -- try again changing initial prices");
        }

        isCurrentlyConverged = isConverged(nIterations);

        /******************************************************************************************************
         * Now we begin to iterate to find a solution.
         *******************************************************************************************************/
        while (!stoppingNextIteration)
        {
            final long iterationTime = System.currentTimeMillis();
            final String logStmt = String.format(
                    "*   Starting iteration %d-%d.  Merit measure is %10e   log10 %7.3f",
                    constraintIteration, nIterations + 1, newMeritMeasure,
                    Math.log10(newMeritMeasure));
            logger.info("*******************************************************************************************");
            logger.info(logStmt);
            // logger.info("*   Starting iteration "+
            // (nIterations+1)+".  Merit measure is "+newMeritMeasure);
            logger.info("*******************************************************************************************");

            nanPresent = false;
            if (isCurrentlyConverged)
            {
                stoppingNextIteration = true;
            } else if (newMeritMeasure / oldMeritMeasure < 1.0000000001
                    || aa.getStepSize() <= aa.getMinimumStepSize()
                    && newMeritMeasure != Double.POSITIVE_INFINITY)
            {
                if (newMeritMeasure / oldMeritMeasure < 1.0000000001)
                {
                    // that worked -- we're getting somewhere
                    aa.increaseStepSize();
                    final String msg = "!!  Improving -- increasing step size to "
                            + aa.getStepSize();
                    logger.info(msg);
                } else
                {
                    final String msg = "!!  Not improving, but step size already at minimum "
                            + aa.getStepSize();
                    logger.info(msg);
                }
                aa.snapShotCurrentPrices();

                if (calcAvgPrice)
                {
                    aa.calculateNewPricesUsingBlockDerivatives(calcDeltaUsingDerivatives);
                } else
                {
                    logger.error("Don't use diagonal approximation anymore, please set aa.calculateAveragePrices to "
                            + "true or remove entry from properties file");
                    aa.calculateNewPricesUsingDiagonalApproximation();
                }

                // this third option -- to use full derivatives -- doesn't fit
                // into 2GB of memory, so hasn't been tested. It would probaly
                // be very slow anyways.
                // aa.calculateNewPricesUsingFullDerivatives();

                if (!nanPresent)
                {
                    nanPresent = aa.calculateCompositeBuyAndSellUtilities(); // distributed
                }
                if (!nanPresent)
                {
                    nanPresent = aa.recalculateLocationConsumptionAndProduction();
                }
                if (!nanPresent)
                {
                    nanPresent = aa.allocateQuantitiesToFlowsAndExchanges(false); // distributed
                }
                nIterations++;
                final double tempMeritMeasure = newMeritMeasure;
                if (!nanPresent)
                {
                    try
                    {
                        if (nIterations % logFrequency == 0)
                        {
                            newMeritMeasure = aa.calculateMeritMeasureWithLogging(); // calculate
                            // surplus
                            // for
                            // each
                            // commodity
                            // as
                            // well
                            // as
                            // total
                            // surplus
                        } else
                        {
                            newMeritMeasure = aa.calculateMeritMeasureWithoutLogging();
                        }
                    } catch (final OverflowException e)
                    {
                        logger.warn("Overflow " + e);
                        nanPresent = true;
                    }
                }
                if (!nanPresent)
                {
                    oldMeritMeasure = tempMeritMeasure;
                } else
                {
                    newMeritMeasure = Double.POSITIVE_INFINITY;
                }
            } else
            {
                if (nIterations == maxIterations - 1)
                {
                    aa.backUpToLastValidPrices();
                    logger.warn("!!  Not Improving and at second last iteration -- backing up to last valid prices");
                } else if (newMeritMeasure == Double.POSITIVE_INFINITY
                        && aa.getStepSize() <= aa.getMinimumStepSize())
                {
                    aa.decreaseStepSizeEvenIfBelowMinimumAndAdjustPrices();
                    final String msg = "!!  Current prices lead to overflow -- decreasing step size to "
                            + aa.getStepSize();
                    logger.info(msg);
                } else
                {
                    aa.decreaseStepSizeAndAdjustPrices();
                    logger.info("!!  Not Improving -- decreasing step size to " + aa.getStepSize());
                }

                nanPresent = nanPresent || aa.calculateCompositeBuyAndSellUtilities(); // distributed
                nanPresent = nanPresent || aa.recalculateLocationConsumptionAndProduction();
                nanPresent = nanPresent || aa.allocateQuantitiesToFlowsAndExchanges(false); // distributed
                nIterations++;
                try
                {
                    if (nIterations % logFrequency == 0)
                    {
                        newMeritMeasure = aa.calculateMeritMeasureWithLogging();
                    } else
                    {
                        newMeritMeasure = aa.calculateMeritMeasureWithoutLogging();
                    }
                } catch (final OverflowException e)
                {
                    logger.warn("Overflow " + e);
                    nanPresent = true;
                }
                if (nanPresent)
                {
                    logger.warn("Overflow error, setting new merit measure to positive infinity");
                    newMeritMeasure = Double.POSITIVE_INFINITY;
                }
            }
            isCurrentlyConverged = isConverged(nIterations);
            if (nIterations >= maxIterations && !isCurrentlyConverged)
            {
                stoppingNextIteration = true;
                logger.fatal("Terminating because maximum iterations reached -- did not converge to tolerance");
            }
            stoppingNextIteration = stoppingNextIteration
                    || checkToSeeIfUserWantsToStopModel(keyboardInput);
            nanPresent = false;
            logger.info("*********************************************************************************************");
            logger.info("*   End of iteration " + nIterations + ".  Time in seconds: "
                    + (System.currentTimeMillis() - iterationTime) / 1000.0);
            logger.info("*********************************************************************************************");

            if (isCurrentlyConverged)
            {
                stoppingNextIteration = true;
            }
        }
        String logStmt = null;
        if (nIterations >= maxIterations && !isCurrentlyConverged)
        {
            logStmt = "*   MaxIterations have been reached. Time in seconds: ";
            exitValue = 2;
        } else if (isCurrentlyConverged)
        {
            logStmt = "*   Equilibrium has been reached in " + nIterations + ". Time in seconds: ";
            exitValue = 0;
        } else if (newMeritMeasure == Double.POSITIVE_INFINITY)
        {
            logStmt = " *   Merit measure is infinity";
            exitValue = 1;
        }

        logger.info("*********************************************************************************************");
        logger.info(logStmt + (System.currentTimeMillis() - startTime) / 1000.0);
        logger.info("*   Final merit measure is " + newMeritMeasure);
        logger.info("*********************************************************************************************");

        return exitValue;
    }

    public static boolean checkToSeeIfUserWantsToStopModel(InputStreamReader keyboardInput)
    {
        boolean stoppingNextIteration = false;
        try
        {
            if (keyboardInput.ready())
            {
                while (keyboardInput.ready())
                {
                    keyboardInput.read();
                }
                System.out.println("Do you want to stop the model? (y/n)");
                final char character = (char) keyboardInput.read();
                if (character == 'y')
                {
                    stoppingNextIteration = true;
                }
                while (keyboardInput.ready())
                {
                    keyboardInput.read();
                }
            }
        } catch (final IOException e)
        {
            e.printStackTrace();
        }
        return stoppingNextIteration;
    }

    private void writeCopyrightStatement()
    {
        logger.info("NOTICE:\n" + "  Copyright 2002-2011 HBA Specto Incorporated\n"
                + "  Licensed under the Apache License, Version 2.0 (the \"License\"\n"
                + "  you may not use this file except in compliance with the License.\n"
                + "  You may obtain a copy of the License at\n" + "\n"
                + "      http://www.apache.org/licenses/LICENSE-2.0\n" + "\n"
                + "  Unless required by applicable law or agreed to in writing, software\n"
                + "  distributed under the License is distributed on an \"AS IS\" BASIS,\n"
                + "  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
                + "  See the License for the specific language governing permissions and\n"
                + "  limitations under the License. \n" + "\n");
    }

    private boolean isConverged(int iteration)
    {

        if (ResourceUtil.getProperty(aaRb, "aa.maxTotalClearance") == null)
        {
            // old convergence criteria
            if (newMeritMeasure <= aa.convergenceTolerance)
            {
                return true;
            }
            return false;
        }

        boolean converged = true;

        final double averageExchangeTotal = Commodity.calcTotalAverageExchangeTotal();
        final double tClear = Math.sqrt(newMeritMeasure) / averageExchangeTotal;
        if (tClear > ResourceUtil.getDoubleProperty(aaRb, "aa.maxTotalClearance"))
        {
            converged = false;
        }

        final double largestSClear = Commodity.getLargestSClearForAllCommodities(ResourceUtil
                .getDoubleProperty(aaRb, "aa.ConFac"));
        if (largestSClear > ResourceUtil.getDoubleProperty(aaRb, "aa.maxSpecificClearance"))
        {
            converged = false;
        }

        if (converged)
        {
            final String msg = "tClear = " + tClear + ", largestSClear = " + largestSClear
                    + " CONVERGED";
            logger.info(msg);
            final String title = "At iteration " + constraintIteration + "-" + iteration
                    + ": AA has converged";
            AAStatusLogger.logGraph(title, iteration, tClear, "Iteration", "tClear");
            return true;
        }

        final String msg = "tClear = " + tClear + ", largestSClear = " + largestSClear
                + " Not Converged";
        logger.info(msg);
        final String title = "At iteration " + constraintIteration + "-" + iteration
                + ": AA has NOT yet converged";
        if (Double.isInfinite(tClear) || Double.isNaN(tClear))
        {
            AAStatusLogger
                    .logText("Warning: tClear is "
                            + tClear
                            + " at iteration "
                            + iteration
                            + ". "
                            + "This is usually not an issue, but if this message persists for more than a few "
                            + "iterations, there may be a problem with the inputs.");
        } else
        {
            AAStatusLogger.logGraph(title, iteration, tClear, "Iteration", "tClear");
        }
        return false;
    }

    public void writeData()
    {
        logger.info("Writing out results - this takes up to 15 minutes");
        AAStatusLogger.logText("Writing out results");
        final long startTime = System.currentTimeMillis();
        aaReaderWriter.writeOutputs();
        logger.info("Output has been written. Time in seconds: "
                + (System.currentTimeMillis() - startTime) / 1000.0);
        AAStatusLogger.logText("Output has been written");
        return;
    }

    /**
     * Run PECAS AA module. Note this is duplicated in AAModel.startModel(), if
     * you change here please change it there too.
     * 
     * @param args
     */
    public static void main(String[] args)
    {
        final ResourceBundle aaRb = ResourceUtil.getResourceBundle("aa");
        final Boolean enableStatusLogging = ResourceUtil.getBooleanProperty(aaRb,
                "aa.statusLoggingEnabled", false);
        AAStatusLogger.setModule("AA");
        if (enableStatusLogging)
        {
            AAStatusLogger.enable();
        } else
        {
            AAStatusLogger.disable();
        }
        final String pProcessorClass = ResourceUtil.getProperty(aaRb, "pprocessor.class",
                AAPProcessor.class.toString());
        logger.info("PECAS will be using the " + pProcessorClass
                + " for pre and post AA processing");
        AAControl aa = null;
        if (args.length == 0)
        {
            try
            {
                aa = new AAControl(Class.forName(pProcessorClass), aaRb);
            } catch (final ClassNotFoundException e)
            {
                logger.fatal("Can't find pprocessor class " + pProcessorClass);
                throw new RuntimeException("Can't find pprocessor class " + pProcessorClass, e);
            }
        } else if (args.length == 2)
        {
            final int baseYear = Integer.parseInt(args[0]);
            final int timeInterval = Integer.parseInt(args[1]);
            logger.info("");
            logger.info("***************** Starting AA for year " + (baseYear + timeInterval));
            AAStatusLogger.logText("Starting AA for year " + (baseYear + timeInterval));
            try
            {
                aa = new AAControl(Class.forName(pProcessorClass), aaRb, baseYear, timeInterval);
            } catch (final ClassNotFoundException e)
            {
                e.printStackTrace();
            }
        } else
        {
            logger.fatal("usage: com.pb.models.pecas.AAControl [baseYear timeInterval]");
            throw new RuntimeException(
                    "usage: com.pb.models.pecas.AAControl [baseYear timeInterval]");
        }
        try
        {
            aa.runModelPerhapsWithConstraints();
        } catch (final ModelDidntWorkException e)
        {
            System.exit(1);
        } catch (Throwable e)
        {
            logger.fatal("Unexpected error " + e);
            e.printStackTrace();
            do
            {
                logger.fatal(e.getMessage());
                final StackTraceElement[] elements = e.getStackTrace();
                for (int i = 0; i < elements.length; i++)
                {
                    logger.fatal(elements[i].toString());
                }
                logger.fatal("Caused by...");
            } while ((e = e.getCause()) != null);
        }

        System.exit(aa.exitValue);
    }

    void runModelPerhapsWithConstraints() throws ModelDidntWorkException
    {
        writeCopyrightStatement();
        readData();
        aaReaderWriter.readInHistogramSpecifications();
        final boolean constrainedRun = ResourceUtil.getBooleanProperty(aaRb, "constrained", false);
        if (constrainedRun)
        {
            logger.info("Running in constrained mode");
            // int constraintIterations =
            // Integer.valueOf(ResourceUtil.getProperty(aaRb,
            // "constraint.iterations"));
            final double constraintSmoothing = ResourceUtil.getDoubleProperty(aaRb,
                    "constraint.smoothing", 1);
            // aaReaderWriter.maxConstantChange=
            // ResourceUtil.getDoubleProperty(aaRb,"constraint.maxConstantChange",2.5);
            aaReaderWriter.setupActivityConstraints();

            AggregateActivity.setForceConstraints(true);
            setConstraintIteration(1);
            int didThisWork = runAAToFindPrices();
            writeData();
            if (didThisWork != 0)
            {
                logger.fatal("Stopping constraint process; AA didn't converge");
                throw new RuntimeException("Stopping constraint process; AA didn't converge");
            }
            aa.updateActivityZonalConstantsBasedOnConstraints(constraintSmoothing);
            aaReaderWriter.writeLocationTable("LatestActivityConstants");
            logger.info("Now checking to make sure constraints are matched");
            setConstraintIteration(2);
            AggregateActivity.setForceConstraints(false);
            setSecondIterationRun(true);
            didThisWork = runAAToFindPrices();
            writeData();
            if (!AAModel.checkConstraints(ResourceUtil.getDoubleProperty(aaRb,
                    "constraint.tolerance", 0.001)))
            {
                throw new ModelDidntWorkException("Constraints not met");
            }
        } else
        {
            runAAToFindPrices();
            writeData();
        }
    }

    public void setConstraintIteration(int constraintIteration)
    {
        this.constraintIteration = constraintIteration;
    }

    public int getConstraintIteration()
    {
        return constraintIteration;
    }

    public void setSecondIterationRun(boolean secondIterationRun)
    {
        this.secondIterationRun = secondIterationRun;
    }

    public boolean isSecondIterationRun()
    {
        return secondIterationRun;
    }
}
