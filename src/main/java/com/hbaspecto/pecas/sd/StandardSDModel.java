/*
 * Created on 11-Oct-2006
 * 
 * Copyright 2006 HBA Specto Incorporated
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
package com.hbaspecto.pecas.sd;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;
import org.apache.log4j.Logger;
import simpleorm.dataset.SQuery;
import simpleorm.dataset.SRecordMeta;
import simpleorm.sessionjdbc.SSessionJdbc;
import com.hbaspecto.discreteChoiceModelling.Coefficient;
import com.hbaspecto.pecas.land.ExchangeResults;
import com.hbaspecto.pecas.land.Parcels;
import com.hbaspecto.pecas.land.PostgreSQLLandInventory;
import com.hbaspecto.pecas.land.SimpleORMLandInventory;
import com.hbaspecto.pecas.sd.estimation.DifferentiableModel;
import com.hbaspecto.pecas.sd.estimation.EstimationDataSet;
import com.hbaspecto.pecas.sd.estimation.EstimationMatrix;
import com.hbaspecto.pecas.sd.estimation.EstimationReader;
import com.hbaspecto.pecas.sd.estimation.EstimationTarget;
import com.hbaspecto.pecas.sd.estimation.ExpectedTargetModel;
import com.hbaspecto.pecas.sd.estimation.GaussBayesianObjective;
import com.hbaspecto.pecas.sd.estimation.MarquardtMinimizer;
import com.hbaspecto.pecas.sd.estimation.ObjectiveFunction;
import com.hbaspecto.pecas.sd.estimation.OptimizationException;
import com.hbaspecto.pecas.sd.estimation.RedevelopmentIntoSpaceTypeTarget;
import com.hbaspecto.pecas.sd.estimation.SpaceTypeCoefficient;
import com.hbaspecto.pecas.sd.estimation.SpaceTypeIntensityTarget;
import com.hbaspecto.pecas.sd.estimation.SpaceTypeTAZTarget;
import com.hbaspecto.pecas.sd.estimation.TransitionConstant;
import com.hbaspecto.pecas.sd.orm.ObservedDevelopmentEvents;
import com.hbaspecto.pecas.sd.orm.SiteSpecTotals;
import com.hbaspecto.pecas.sd.orm.SpaceTypesGroup;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.GeneralDecimalFormat;
import com.pb.common.datafile.JDBCTableReader;
import com.pb.common.datafile.JDBCTableWriter;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataFileReader;
import com.pb.common.datafile.TableDataFileWriter;
import com.pb.common.datafile.TableDataReader;
import com.pb.common.datafile.TableDataSetCollection;
import com.pb.common.datafile.TableDataWriter;
import com.pb.common.sql.JDBCConnection;
import com.pb.common.util.ResourceUtil;

public class StandardSDModel
        extends SDModel
{

    static boolean                    excelLandDatabase;

    static boolean                    useSQLParcels = false;

    protected static transient Logger logger        = Logger.getLogger(StandardSDModel.class);

    protected String                  landDatabaseUser;

    protected String                  landDatabasePassword, databaseSchema;

    private TableDataFileWriter       writer;

    public static void main(String[] args)
    {
        boolean worked = true; // assume this is going to work
        rbSD = ResourceUtil.getResourceBundle("sd");
        initOrm();
        SDModel mySD = new StandardSDModel();
        try
        {
            currentYear = Integer.valueOf(args[0]) + Integer.valueOf(args[1]);
            baseYear = Integer.valueOf(args[0]);
        } catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException("Put base year and time interval on command line"
                    + "\n For example, 1990 1");
        }
        try
        {
            mySD.setUp();

            // for testing, remove this line
            ((SimpleORMLandInventory) mySD.land).readInventoryTable("floorspacei_view");

            mySD.runSD(currentYear, baseYear, rbSD);
        } catch (Throwable e)
        {
            logger.fatal("Unexpected error " + e);
            e.printStackTrace();
            do
            {
                logger.fatal(e.getMessage());
                StackTraceElement elements[] = e.getStackTrace();
                for (int i = 0; i < elements.length; i++)
                {
                    logger.fatal(elements[i].toString());
                }
                logger.fatal("Caused by...");
            } while ((e = e.getCause()) != null);
            worked = false; // oops it didn't work
        } finally
        {
            if (mySD.land != null) mySD.land.disconnect();
            if (!worked) System.exit(1); // don't need to manually call exit if everything worked ok.
        }
    }

    public StandardSDModel()
    {
        super();
    }

    static void initOrm(ResourceBundle rb)
    {
        rbSD = rb;
        initOrm();
    }

    static void initOrm()
    {
        Parcels.init(rbSD);
        ZoningPermissions.init(rbSD);
        ExchangeResults.init(rbSD);
        SiteSpecTotals.init(rbSD);
        // FIXME this shouldn't be here

    }

    @Override
    public void setUpLandInventory(String className, int year)
    {

        try
        {
            Class landInventoryClass = Class.forName(className);
            SimpleORMLandInventory sormland = (SimpleORMLandInventory) landInventoryClass
                    .newInstance();
            land = sormland;
            sormland.setDatabaseConnectionParameter(rbSD, landDatabaseDriver,
                    landDatabaseSpecifier, landDatabaseUser, landDatabasePassword, databaseSchema);

            sormland.setLogFile(logFilePath + "developmentEvents.csv");
            logger.info("Log file is at " + logFilePath + "developmentEvents.csv");

            land.init(year);
            land.setMaxParcelSize(ResourceUtil.getDoubleProperty(rbSD, "MaxParcelSize",
                    Double.POSITIVE_INFINITY));

        } catch (InstantiationException ie)
        {
            logger.fatal("Instantiating : " + className + '\n' + ie.getMessage());
            throw new RuntimeException("Instantiating " + className, ie);
        } catch (Exception e)
        {
            logger.fatal("Can't open land database table using " + landDatabaseDriver);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void setUp()
    {
        // FIXME this shouldn't be here
        SpaceTypesGroup.setCurrentYear(currentYear);
        useSQLInputs = ResourceUtil.getBooleanProperty(rbSD, "UseSQLInputs");
        useSQLParcels = ResourceUtil.getBooleanProperty(rbSD, "UseSQLParcels");
        TableDataReader inputTableReader;
        TableDataWriter inputTableWriter;
        inputDatabaseDriver = ResourceUtil.checkAndGetProperty(rbSD, "InputJDBCDriver");
        inputDatabaseSpecifier = ResourceUtil.checkAndGetProperty(rbSD, "InputDatabase");
        JDBCConnection inputPBConnection = new JDBCConnection(inputDatabaseSpecifier,
                inputDatabaseDriver, ResourceUtil.getProperty(rbSD, "InputDatabaseUser", ""),
                ResourceUtil.getProperty(rbSD, "InputDatabasePassword", ""));
        JDBCTableReader jdbcInputTableReader = new JDBCTableReader(inputPBConnection);
        JDBCTableWriter jdbcInputTableWriter = new JDBCTableWriter(inputPBConnection);
        excelInputDatabase = ResourceUtil.getBooleanProperty(rbSD, "ExcelInputDatabase", false);
        jdbcInputTableReader.setMangleTableNamesForExcel(excelInputDatabase);
        jdbcInputTableWriter.setMangleTableNamesForExcel(excelInputDatabase);
        inputTableReader = jdbcInputTableReader;
        inputTableWriter = jdbcInputTableWriter;
        landDatabaseDriver = ResourceUtil.checkAndGetProperty(rbSD, "LandJDBCDriver");
        try
        {
            Class.forName(landDatabaseDriver).newInstance();
        } catch (Exception e)
        {
            logger.fatal("Can't start land database driver" + e);
            throw new RuntimeException("Can't start land database driver", e);
        }
        landDatabaseSpecifier = ResourceUtil.checkAndGetProperty(rbSD, "LandDatabase");

        // landDatabaseTable = ResourceUtil.checkAndGetProperty(rbSD,
        // "LandTable");

        landDatabaseUser = ResourceUtil.getProperty(rbSD, "LandDatabaseUser", "");
        landDatabasePassword = ResourceUtil.getProperty(rbSD, "LandDatabasePassword", "");
        databaseSchema = ResourceUtil.getProperty(rbSD, "schema");

        TableDataSetCollection inputDatabase = new TableDataSetCollection(inputTableReader,
                inputTableWriter);
        // TableDataSet developmentTypesI = inputDatabase.getTableDataSet("spacetypesi");
        // TableDataSet transitionConstantsI = inputDatabase
        // .getTableDataSet("TransitionConstantsI");

        // FIXME read in the LUZ table or make sure it can be read in record-by-record on demand using SimpleORM
        // PECASZone.setUpZones(inputDatabase.getTableDataSet("PECASZonesI"));

        // ZoningRulesI.setUpZoningSchemes(inputDatabase
        // .getTableDataSet("ZoningSchemesI"));

        // We'll iterate through PECASZones, instead of FloorspaceZones.
        // zoneNumbers = inputDatabase.getTableDataSet("PECASZonesI");

        logFilePath = ResourceUtil.checkAndGetProperty(rbSD, "LogFilePath");
        String className = ResourceUtil.getProperty(rbSD, "LandInventoryClass",
                PostgreSQLLandInventory.class.getName());
        setUpLandInventory(className, currentYear);
        ZoningRulesI.land = land;
        setUpDevelopmentTypes();
        TableDataFileReader reader = setUpCsvReaderWriter();

        // if (ResourceUtil.getBooleanProperty(rbSD, "sd.aaUsesDifferentZones",false))
        // readFloorspaceZones(inputDatabase
        // .getTableDataSet("FloorspaceZonesI"));

        // need to get prices from file if it exists
        if (ResourceUtil.getBooleanProperty(rbSD, "ReadExchangeResults", true))
        {
            land.readSpacePrices(reader);
        }
        if (ResourceUtil.getBooleanProperty(rbSD, "SmoothPrices", false))
        {
            land.applyPriceSmoothing(reader, writer);
        }
    }

    private TableDataFileReader setUpCsvReaderWriter()
    {
        OLD_CSVFileReader outputTableReader = new OLD_CSVFileReader();
        CSVFileWriter outputTableWriter = new CSVFileWriter();
        outputTableWriter.setMyDecimalFormat(new GeneralDecimalFormat("0.#########E0", 10000000,
                .001));
        if (ResourceUtil.getBooleanProperty(rbSD, "UseYearSubdirectories", true))
        {
            outputTableReader.setMyDirectory(ResourceUtil.getProperty(rbSD, "AAResultsDirectory")
                    + currentYear + File.separatorChar);
            outputTableWriter.setMyDirectory(new File(ResourceUtil.getProperty(rbSD,
                    "AAResultsDirectory") + (currentYear + 1) + File.separatorChar));
        } else
        {
            outputTableReader.setMyDirectory(ResourceUtil.getProperty(rbSD, "AAResultsDirectory"));
            outputTableWriter.setMyDirectory(new File(ResourceUtil.getProperty(rbSD,
                    "AAResultsDirectory")));
        }

        outputDatabase = new TableDataSetCollection(outputTableReader, outputTableWriter);
        writer = outputTableWriter;
        return outputTableReader;
    }

    public void simulateDevelopment()
    {
        // ZoningRulesI.openLogFile(logFilePath);
        // land.getDevelopmentLogger().open(logFilePath);

        boolean prepareEstimationData = ResourceUtil.getBooleanProperty(rbSD,
                "PrepareEstimationDataset", false);
        EstimationDataSet eDataSet = null;
        if (prepareEstimationData)
        {
            String estimationFileNamePath = ResourceUtil.checkAndGetProperty(rbSD,
                    "EstimationDatasetFileNameAndPath");
            double sampleRatio = ResourceUtil.getDoubleProperty(rbSD, "SampleRatio");
            eDataSet = new EstimationDataSet(estimationFileNamePath, sampleRatio);
        }

        boolean ignoreErrors = ResourceUtil.getBooleanProperty(rbSD, "IgnoreErrors", false);
        if (ignoreErrors)
        {
            String path = ResourceUtil.checkAndGetProperty(rbSD, "AAResultsDirectory");
            // create the logger here

        }

        ZoningRulesI currentZoningRule = null;
        land.setToBeforeFirst();
        long parcelCounter = 0;
        if (prepareEstimationData)
        {
            // grab all development permit records into the SimpleORM Cache
            land.getSession().query(
                    new SQuery<ObservedDevelopmentEvents>(ObservedDevelopmentEvents.meta));
        }
        while (land.advanceToNext())
        {
            parcelCounter++;
            if (parcelCounter % 1000 == 0)
            {
                logger.info("finished gridcell " + parcelCounter);
            }
            currentZoningRule = ZoningRulesI.getZoningRuleByZoningRulesCode(land.getSession(),
                    land.getZoningRulesCode());
            if (currentZoningRule == null)
            {
                land.getDevelopmentLogger().logBadZoning(land);
            } else
            {
                if (prepareEstimationData)
                {

                    // Keeping the csv file opened for the whole period of the run might not be a good idea.
                    eDataSet.compileEstimationRow(land);
                    eDataSet.writeEstimationRow();
                } else
                {
                    currentZoningRule.simulateDevelopmentOnCurrentParcel(land, ignoreErrors);
                }
            }
        }
        if (prepareEstimationData) eDataSet.close();

        land.getDevelopmentLogger().flush();
        land.addNewBits();

        land.getDevelopmentLogger().close();
    }

    public void calibrateModel(EstimationReader reader, int baseY, int currentY, double epsilon,
            int maxits)
    {
        // ZoningRulesI.openLogFile(logFilePath);
        baseYear = baseY;
        currentYear = currentY;
        rbSD = ResourceUtil.getResourceBundle("sd");
        try
        {
            setUp();
            initZoningScheme(currentY, baseY);

            List<EstimationTarget> targets = reader.readTargets();

            Matrix targetVariance = new DenseMatrix(reader.readTargetVariance(targets));

            List<Coefficient> coeffs = reader.readCoeffs();

            Vector means = new DenseVector(reader.readPriorMeans(coeffs));

            Matrix variance = new DenseMatrix(reader.readPriorVariance(coeffs));

            Vector epsilons = new DenseVector(means);
            for (int i = 0; i < means.size(); i++)
                epsilons.set(i, Math.max(Math.abs(means.get(i)), epsilon) * epsilon);

            ZoningRulesI.ignoreErrors = ResourceUtil
                    .getBooleanProperty(rbSD, "IgnoreErrors", false);
            double initialStepSize = ResourceUtil.getDoubleProperty(rbSD, "InitialLambda", 600);

            DifferentiableModel theModel = new ExpectedTargetModel(coeffs, land);
            ObjectiveFunction theObjective = new GaussBayesianObjective(theModel, coeffs, targets,
                    targetVariance, means, variance);

            // DEBUG
            // This section prints out targets with perturbation so that we can check numerical derivatives.
            /*
             * double delta = 0.01; Vector targetValues; BufferedWriter writer = null; try { writer = new BufferedWriter(new
             * FileWriter("perturbed.csv")); // Write target names across the top. for(EstimationTarget target: targets) writer.write("," +
             * target.getName()); writer.newLine(); // Write unperturbed target values. try { targetValues = theModel.getTargetValues(targets, means);
             * writer.write("Unperturbed"); for(int i = 0; i < targetValues.size(); i++) writer.write("," + targetValues.get(i)); writer.newLine(); }
             * catch(OptimizationException e) {}
             * 
             * // Perturb each coefficient in turn. for(int i = 0; i < means.size(); i++) { Vector perturbed = means.copy(); perturbed.set(i,
             * perturbed.get(i) + delta); try { targetValues = theModel.getTargetValues(targets, perturbed); writer.write(coeffs.get(i).getName());
             * for(int j = 0; j < targetValues.size(); j++) writer.write("," + targetValues.get(j)); writer.newLine(); } catch(OptimizationException
             * e) {} } } catch(IOException e) {} finally { if(writer != null) try { writer.close(); } catch(IOException e) {} }
             */
            try
            {
                land.getSession(); // opens up the session and begins a transaction
                MarquardtMinimizer theMinimizer = new MarquardtMinimizer(theObjective,
                        new DenseVector(reader.readStartingValues(coeffs)));
                theMinimizer.setInitialMarquardtFactor(initialStepSize);
                double initialObj = theMinimizer.getCurrentObjectiveValue();
                Vector optimalParameters = theMinimizer.iterateToConvergence(epsilons, maxits,
                        new MarquardtMinimizer.EachIterationCallback()
                        {

                            @Override
                            public void finishedIteration(int iteration)
                            {
                                land.commitAndStayConnected();
                                // land.commit();
                                // caused all of our objects to be destroyed
                                // so leave it all open for now, in transaction
                            }
                        });
                Vector optimalTargets = theModel.getTargetValues(targets, optimalParameters);
                String paramsAsString = Arrays.toString(Matrices.getArray(optimalParameters));
                if (theMinimizer.lastRunConverged())
                {
                    logger.info("SD parameter estimation converged on a solution: "
                            + paramsAsString);
                    logger.info("Initial objective function = " + initialObj);
                    logger.info("Optimal objective function = "
                            + theMinimizer.getCurrentObjectiveValue());
                    logger.info("Convergence after " + theMinimizer.getNumberOfIterations()
                            + " iterations");
                } else
                {
                    int numits = theMinimizer.getNumberOfIterations();
                    logger.info("SD parameter estimation stopped after " + numits + " iteration"
                            + (numits == 1 ? "" : "s") + " without finding a solution");
                    logger.info("Current parameter values: " + paramsAsString);
                    if (theMinimizer.lastRunMaxIterations()) logger
                            .info("Reason: stopped at maximum allowed iterations");
                    else logger.info("Reason: could not find a valid next iteration");
                    logger.info("Initial objective function = " + initialObj);
                    logger.info("Optimal objective function = "
                            + theMinimizer.getCurrentObjectiveValue());
                }
                logger.info("Target values at optimum: "
                        + Arrays.toString(Matrices.getArray(optimalTargets)));
            } catch (OptimizationException e)
            {
                logger.error("Bad initial guess: " + Arrays.toString(Matrices.getArray(means)));
            }
        } finally
        {
            if (land != null) land.disconnect();
        }
    }

}