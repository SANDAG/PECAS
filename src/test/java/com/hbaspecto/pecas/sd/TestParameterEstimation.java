package com.hbaspecto.pecas.sd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;
import org.junit.Ignore;
import org.junit.Test;
import simpleorm.sessionjdbc.SSessionJdbc;
import com.hbaspecto.discreteChoiceModelling.Coefficient;
import com.hbaspecto.pecas.land.LandInventory;
import com.hbaspecto.pecas.land.PostgreSQLLandInventory;
import com.hbaspecto.pecas.land.SimpleORMLandInventory;
import com.hbaspecto.pecas.sd.estimation.CSVEstimationReader;
import com.hbaspecto.pecas.sd.estimation.DifferentiableModel;
import com.hbaspecto.pecas.sd.estimation.EstimationReader;
import com.hbaspecto.pecas.sd.estimation.EstimationTarget;
import com.hbaspecto.pecas.sd.estimation.ExpectedTargetModel;
import com.hbaspecto.pecas.sd.estimation.RedevelopmentIntoSpaceTypeTarget;
import com.hbaspecto.pecas.sd.estimation.SpaceTypeIntensityTarget;
import com.hbaspecto.pecas.sd.estimation.SpaceTypeTAZTarget;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.GeneralDecimalFormat;
import com.pb.common.datafile.JDBCTableReader;
import com.pb.common.datafile.JDBCTableWriter;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataFileReader;
import com.pb.common.sql.JDBCConnection;
import com.pb.common.util.ResourceUtil;

@Ignore
// TODO: Fix test
public class TestParameterEstimation
{
    private static LandInventory land;

    private static String str(boolean b)
    {
        if (b)
        {
            return "TRUE";
        } else
        {
            return "FALSE";
        }
    }

    private static void setUpTestInventory(String url, String user, String password)
            throws Exception
    {
        final Connection conn = DriverManager.getConnection(url, user, password);
        final Statement statement = conn.createStatement();

        // Overwrite exchange results table with garbage data. The model should
        // replace that data.
        statement.execute("UPDATE exchange_results " + "SET price=42000, internal_bought=42000");

        final int[] parcelnum = {1, 2, 3, 4, 5, 6, 7, 8};
        final String[] parcelid = {"1", "2", "3", "4", "5", "6", "7", "8"};
        final int[] yearbuilt = {2000, 2000, 2000, 2000, 2000, 2000, 2000, 2000};
        final int[] taz = {11, 11, 12, 12, 21, 21, 22, 22};
        final int[] spacetype = {3, 3, 5, 5, 3, 5, 3, 95};
        final double[] quantity = {99000, 399000, 49500, 199500, 79200, 319200, 33000, 0};
        final double[] landarea = {215496, 215496, 107748, 107748, 172397, 172397, 71832, 71832};
        final boolean[] derelict = {false, false, false, false, false, false, true, true};
        final boolean[] brownfield = {false, false, false, false, false, false, false, false};

        for (int i = 0; i < parcelnum.length; i++)
        {
            statement.execute("UPDATE parcels " + "SET parcel_id='" + parcelid[i]
                    + "', year_built=" + yearbuilt[i] + ", taz=" + taz[i] + ", space_type_id="
                    + spacetype[i] + ", space_quantity=" + quantity[i] + ", land_area="
                    + landarea[i] + ", is_derelict=" + str(derelict[i]) + ", is_brownfield="
                    + str(brownfield[i]) + " WHERE pecas_parcel_num=" + parcelnum[i]);
        }

        // Spacetype constants.
        final int[] spacetypenum = {3, 5, 95};
        final double[] newconst = {-491, -109, -539};
        final double[] addconst = {-589, -11.3, -1E+99};
        final double[] renoconst = {-262, -18.8, -1E+99};
        final double[] democonst = {-455, -35, -1E+99};
        final double[] derelictconst = {-100, -100, -1E+99};
        final double[] nochangeconst = {0, 0, 0};
        final double[] newtypedisp = {0.02, 0.4, 0.1};
        final double[] gydisp = {0.02, 0.4, 0.1};
        final double[] gzdisp = {0.02, 0.4, 0.1};
        final double[] gwdisp = {0.02, 0.4, 0.1};
        final double[] gkdisp = {0.02, 0.4, 0.1};
        final double[] nochangedisp = {0.02, 0.4, 0.1};
        final double[] intensitydisp = {0.04, 0.5, 0.2};
        final double[] steppoint = {4.8, 3.6, 0.0};
        final double[] belowstep = {55.3, 70, 0.0};
        final double[] abovestep = {-13, -22, 0.0};
        final double[] stepamount = {20, 15, 0.0};
        final double[] minfar = {0.5, 0.0, 0.0};
        final double[] maxfar = {20, 15, 0.0};

        for (int i = 0; i < spacetypenum.length; i++)
        {
            statement.execute("UPDATE space_types_i " + "SET new_transition_const=" + newconst[i]
                    + ", add_transition_const=" + addconst[i] + ", renovate_transition_const="
                    + renoconst[i] + ", renovate_derelict_const=" + renoconst[i]
                    + ", demolish_transition_const=" + democonst[i]
                    + ", derelict_transition_const=" + derelictconst[i]
                    + ", no_change_transition_const=" + nochangeconst[i]
                    + ", new_type_dispersion_parameter=" + newtypedisp[i]
                    + ", gy_dispersion_parameter=" + gydisp[i] + ", gz_dispersion_parameter="
                    + gzdisp[i] + ", gw_dispersion_parameter=" + gwdisp[i]
                    + ", gk_dispersion_parameter=" + gkdisp[i] + ", nochange_dispersion_parameter="
                    + nochangedisp[i] + ", intensity_dispersion_parameter=" + intensitydisp[i]
                    + ", step_point=" + steppoint[i] + ", below_step_point_adjustment="
                    + belowstep[i] + ", above_step_point_adjustment=" + abovestep[i]
                    + ", step_point_adjustment=" + stepamount[i] + ", min_intensity=" + minfar[i]
                    + ", max_intensity=" + maxfar[i] + " WHERE space_type_id=" + spacetypenum[i]);
        }

        final int[] spacetypenv = {3, 5};
        final double[][] transition = { {-86.3, 122}, {-15.9, 95.8}, {376, 490}};

        for (int i = 0; i < spacetypenum.length; i++)
        {
            for (int j = 0; j < spacetypenv.length; j++)
            {
                statement.execute("UPDATE transition_constants_i " + "SET transition_constant="
                        + transition[i][j] + " WHERE from_space_type_id=" + spacetypenum[i]
                        + " AND to_space_type_id=" + spacetypenv[j]);
            }
        }
        conn.close();
    }

    public static void setUpResources() throws Exception
    {
        final ResourceBundle rb = ResourceUtil.getResourceBundle("sd");

        final String url = ResourceUtil.checkAndGetProperty(rb, "InputDatabase");
        final String user = ResourceUtil.checkAndGetProperty(rb, "InputDatabaseUser");
        final String password = ResourceUtil.checkAndGetProperty(rb, "InputDatabasePassword");

        setUpTestInventory(url, user, password);

        final String driver = ResourceUtil.checkAndGetProperty(rb, "InputJDBCDriver");
        final String schema = null;

        SimpleORMLandInventory.prepareSimpleORMSession(rb);
        final SimpleORMLandInventory sormland = new PostgreSQLLandInventory();
        sormland.setDatabaseConnectionParameter(rb, driver, url, user, password, schema);
        land = sormland;
        ZoningRulesI.land = land;
        land.init(2005);

        final double interestRate = 0.0722;
        final double compounded = Math.pow(1 + interestRate, 30);
        final double amortizationFactor = interestRate * compounded / (compounded - 1);
        ZoningRulesI.amortizationFactor = ResourceUtil.getDoubleProperty(rb, "AmortizationFactor",
                amortizationFactor);
        ZoningRulesI.servicingCostPerUnit = ResourceUtil.getDoubleProperty(rb,
                "ServicingCostPerUnit", 13.76);
        final JDBCConnection inputPBConnection = new JDBCConnection(url, driver, user, password);
        final JDBCTableReader jdbcInputTableReader = new JDBCTableReader(inputPBConnection);
        final JDBCTableWriter jdbcInputTableWriter = new JDBCTableWriter(inputPBConnection);
        final boolean excelInputDatabase = ResourceUtil.getBooleanProperty(rb,
                "ExcelInputDatabase", false);
        jdbcInputTableReader.setMangleTableNamesForExcel(excelInputDatabase);
        jdbcInputTableWriter.setMangleTableNamesForExcel(excelInputDatabase);
        final String landDatabaseDriver = ResourceUtil.checkAndGetProperty(rb, "LandJDBCDriver");
        try
        {
            Class.forName(landDatabaseDriver).newInstance();
        } catch (final Exception e)
        {
        }

        // TableDataSet developmentTypesI =
        // inputDatabase.getTableDataSet("spacetypesi");
        // TableDataSet transitionConstantsI = inputDatabase
        // .getTableDataSet("TransitionConstantsI");

        // ZoningRulesI.setUpZoningSchemes(inputDatabase
        // .getTableDataSet("ZoningSchemesI"));

        // We'll iterate through PECASZones, instead of FloorspaceZones.
        // zoneNumbers = inputDatabase.getTableDataSet("PECASZonesI");

        final OLD_CSVFileReader outputTableReader = new OLD_CSVFileReader();
        final CSVFileWriter outputTableWriter = new CSVFileWriter();
        outputTableWriter.setMyDecimalFormat(new GeneralDecimalFormat("0.#########E0", 10000000,
                .001));
        if (ResourceUtil.getBooleanProperty(rb, "UseYearSubdirectories", true))
        {
            outputTableReader.setMyDirectory(ResourceUtil.getProperty(rb, "AAResultsDirectory")
                    + File.separatorChar);
            outputTableWriter.setMyDirectory(new File(ResourceUtil.getProperty(rb,
                    "AAResultsDirectory") + File.separatorChar));
        } else
        {
            outputTableReader.setMyDirectory(ResourceUtil.getProperty(rb, "AAResultsDirectory"));
            outputTableWriter.setMyDirectory(new File(ResourceUtil.getProperty(rb,
                    "AAResultsDirectory")));
        }

        final TableDataFileReader reader = outputTableReader;

        // if (ResourceUtil.getBooleanProperty(rbSD,
        // "sd.aaUsesDifferentZones",false))
        // readFloorspaceZones(inputDatabase
        // .getTableDataSet("FloorspaceZonesI"));

        // need to get prices from file if it exists
        land.readSpacePrices(reader);
    }

    @Test
    public void testTargetValuesWithFullMatrix() throws Exception
    {
        final CSVEstimationReader reader = new CSVEstimationReader("testconfig\\targets.csv",
                false, "testconfig\\parameters.csv", false);
        testTargetValues(reader);
    }

    @Test
    public void testTargetValuesDiagonal() throws Exception
    {
        final CSVEstimationReader reader = new CSVEstimationReader("testconfig\\targetssimple.csv",
                true, "testconfig\\parameterssimple.csv", true);
        testTargetValues(reader);
    }

    public void testTargetValues(EstimationReader reader) throws Exception
    {
        setUpResources();

        SSessionJdbc.getThreadLocalSession().begin();

        final List<Coefficient> coeffs = reader.readCoeffs();
        final List<EstimationTarget> targets = reader.readTargets();
        final double[] means = reader.readPriorMeans(coeffs);

        final Vector vmeans = new DenseVector(means);
        final DifferentiableModel model = new ExpectedTargetModel(coeffs, land);
        final Vector result = model.getTargetValues(targets, vmeans);

        assertEquals(34.0579, result.get(0), 0.0001);
        assertEquals(0.0, result.get(1));
        assertEquals(28.5721, result.get(2), 0.0001);
        assertEquals(15.1427, result.get(3), 0.0001);
        assertEquals(0.0, result.get(4));
        assertEquals(58362.4, result.get(5), 0.1);
        assertEquals(447.868, result.get(6), 0.001);
        assertEquals(10260.1, result.get(7), 0.1);
        assertEquals(77.7727, result.get(8), 0.0001);
        assertEquals(58810.2, result.get(9), 0.1);
        assertEquals(3.62036E-4, result.get(10), 1E-9);
        assertEquals(0.145748, result.get(11), 0.000001);

        final Matrix jacobian = model.getJacobian(targets, vmeans);
        assertEquals(0.131598, jacobian.get(0, 0), 0.000001);
        assertEquals(0.0, jacobian.get(1, 0));
        assertEquals(0.0382057, jacobian.get(2, 0), 1E-7);
        assertEquals(0.103348, jacobian.get(3, 0), 0.000001);
        assertEquals(0.0, jacobian.get(4, 0));
        assertEquals(0.0, jacobian.get(5, 0));
        assertEquals(-1.09207E-5, jacobian.get(6, 0), 1E-10);
        assertEquals(0.0, jacobian.get(7, 0));
        assertEquals(0.273151, jacobian.get(8, 0), 0.000001);
        assertEquals(-1.09207E-5, jacobian.get(9, 0), 1E-10);
        assertEquals(1.78974E-6, jacobian.get(10, 0), 1E-11);
        assertEquals(-6.33464E-11, jacobian.get(11, 0), 1E-16);

        assertEquals(0.0, jacobian.get(0, 1));
        assertEquals(0.0, jacobian.get(1, 1));
        assertEquals(-9.19661E-6, jacobian.get(2, 1), 1E-11);
        assertEquals(0.0, jacobian.get(3, 1));
        assertEquals(0.0, jacobian.get(4, 1));
        assertEquals(60.8834, jacobian.get(5, 1), 0.0001);
        assertEquals(1.03069, jacobian.get(6, 1), 0.00001);
        assertEquals(138.752, jacobian.get(7, 1), 0.001);
        assertEquals(-9.19661E-6, jacobian.get(8, 1), 1E-11);
        assertEquals(61.9141, jacobian.get(9, 1), 0.0001);
        assertEquals(-2.72830E-11, jacobian.get(10, 1), 1E-16);
        assertEquals(0.00193769, jacobian.get(11, 1), 1E-8);

        assertEquals(0.0, jacobian.get(0, 2));
        assertEquals(0.0, jacobian.get(1, 2));
        assertEquals(0.0, jacobian.get(2, 2));
        assertEquals(0.0, jacobian.get(3, 2));
        assertEquals(0.0, jacobian.get(4, 2));
        assertEquals(0.0, jacobian.get(5, 2));
        assertEquals(0.0, jacobian.get(6, 2));
        assertEquals(0.0, jacobian.get(7, 2));
        assertEquals(0.0, jacobian.get(8, 2));
        assertEquals(0.0, jacobian.get(9, 2));
        assertEquals(0.0, jacobian.get(10, 2));
        assertEquals(0.0, jacobian.get(11, 2));

        assertEquals(-23.2560, jacobian.get(0, 3), 0.0001);
        assertEquals(0.0, jacobian.get(1, 3), 1E-9);
        assertEquals(-50.4360, jacobian.get(2, 3), 0.0001);
        assertEquals(0.0, jacobian.get(3, 3), 1E-9);
        assertEquals(0.0, jacobian.get(4, 3), 1E-9);
        assertEquals(0.0, jacobian.get(5, 3), 1E-9);
        assertEquals(-12.5724, jacobian.get(6, 3), 0.0001);
        assertEquals(0.0, jacobian.get(7, 3), 1E-9);
        assertEquals(-73.6920, jacobian.get(8, 3), 0.0001);
        assertEquals(-12.5724, jacobian.get(9, 3), 0.0001);
        assertEquals(-5.36894E-5, jacobian.get(10, 3), 1E-10);
        assertEquals(-7.29268E-5, jacobian.get(11, 3), 1E-10);

        assertEquals(0.0, jacobian.get(0, 4), 1E-9);
        assertEquals(0.0, jacobian.get(1, 4), 1E-9);
        assertEquals(0.0, jacobian.get(2, 4), 1E-9);
        assertEquals(0.0, jacobian.get(3, 4), 1E-9);
        assertEquals(0.0, jacobian.get(4, 4), 1E-9);
        assertEquals(-140.896, jacobian.get(5, 4), 0.001);
        assertEquals(0.0, jacobian.get(6, 4), 1E-9);
        assertEquals(0.0, jacobian.get(7, 4), 1E-9);
        assertEquals(0.0, jacobian.get(8, 4), 1E-9);
        assertEquals(-140.896, jacobian.get(9, 4), 0.001);
        assertEquals(0.0, jacobian.get(10, 4), 1E-9);
        assertEquals(-0.00111184, jacobian.get(11, 4), 1E-8);

        assertEquals(0.0, jacobian.get(0, 5), 1E-9);
        assertEquals(0.0, jacobian.get(1, 5), 1E-9);
        assertEquals(0.0, jacobian.get(2, 5), 1E-9);
        assertEquals(0.0, jacobian.get(3, 5), 1E-9);
        assertEquals(0.0, jacobian.get(4, 5), 1E-9);
        assertEquals(0.0, jacobian.get(5, 5), 1E-9);
        assertEquals(0.0, jacobian.get(6, 5), 1E-9);
        assertEquals(0.0, jacobian.get(7, 5), 1E-9);
        assertEquals(0.0, jacobian.get(8, 5), 1E-9);
        assertEquals(0.0, jacobian.get(9, 5), 1E-9);
        assertEquals(0.0, jacobian.get(10, 5), 1E-9);
        assertEquals(0.0, jacobian.get(11, 5), 1E-9);

        assertEquals(0.394688, jacobian.get(0, 6), 0.000001);
        assertEquals(0.0, jacobian.get(1, 6));
        assertEquals(0.279172, jacobian.get(2, 6), 0.000001);
        assertEquals(0.0, jacobian.get(3, 6));
        assertEquals(0.0, jacobian.get(4, 6));
        assertEquals(0.0, jacobian.get(5, 6));
        assertEquals(-0.000178432, jacobian.get(6, 6), 1E-9);
        assertEquals(0.0, jacobian.get(7, 6));
        assertEquals(0.673861, jacobian.get(8, 6), 0.000001);
        assertEquals(-0.000178432, jacobian.get(9, 6), 1E-9);
        assertEquals(-5.48455E-11, jacobian.get(10, 6), 1E-16);
        assertEquals(-1.03501E-9, jacobian.get(11, 6), 1E-14);

        assertEquals(0.0, jacobian.get(0, 7));
        assertEquals(0.0, jacobian.get(1, 7));
        assertEquals(0.0, jacobian.get(2, 7));
        assertEquals(0.0, jacobian.get(3, 7));
        assertEquals(0.0, jacobian.get(4, 7));
        assertEquals(18612.0, jacobian.get(5, 7), 0.1);
        assertEquals(0.0, jacobian.get(6, 7));
        assertEquals(0.0, jacobian.get(7, 7));
        assertEquals(0.0, jacobian.get(8, 7));
        assertEquals(18612.0, jacobian.get(9, 7), 0.1);
        assertEquals(0.0, jacobian.get(10, 7));
        assertEquals(-9.01500E-6, jacobian.get(11, 7), 1E-11);

        assertEquals(0.0, jacobian.get(0, 8));
        assertEquals(0.0, jacobian.get(1, 8));
        assertEquals(0.0, jacobian.get(2, 8));
        assertEquals(0.0, jacobian.get(3, 8));
        assertEquals(0.0, jacobian.get(4, 8));
        assertEquals(0.0, jacobian.get(5, 8));
        assertEquals(0.0, jacobian.get(6, 8));
        assertEquals(0.0, jacobian.get(7, 8));
        assertEquals(0.0, jacobian.get(8, 8));
        assertEquals(0.0, jacobian.get(9, 8));
        assertEquals(0.0, jacobian.get(10, 8));
        assertEquals(0.0, jacobian.get(11, 8));

        assertEquals(0.188342, jacobian.get(0, 9), 0.000001);
        assertEquals(0.0, jacobian.get(1, 9));
        assertEquals(0.165090, jacobian.get(2, 9), 0.000001);
        assertEquals(0.0910884, jacobian.get(3, 9), 1E-7);
        assertEquals(0.0, jacobian.get(4, 9));
        assertEquals(0.0, jacobian.get(5, 9));
        assertEquals(-7.81518E-5, jacobian.get(6, 9), 1E-10);
        assertEquals(0.0, jacobian.get(7, 9));
        assertEquals(0.444521, jacobian.get(8, 9), 0.000001);
        assertEquals(-7.81518E-5, jacobian.get(9, 9), 1E-10);
        assertEquals(2.12469E-6, jacobian.get(10, 9), 1E-11);
        assertEquals(-4.53325E-10, jacobian.get(11, 9), 1E-15);

        assertEquals(0.0, jacobian.get(0, 10));
        assertEquals(0.0, jacobian.get(1, 10));
        assertEquals(-7.98759E-5, jacobian.get(2, 10), 1E-10);
        assertEquals(0.0, jacobian.get(3, 10));
        assertEquals(0.0, jacobian.get(4, 10));
        assertEquals(3167.37, jacobian.get(5, 10), 0.01);
        assertEquals(2.53589, jacobian.get(6, 10), 0.00001);
        assertEquals(221.973, jacobian.get(7, 10), 0.001);
        assertEquals(-7.98759E-5, jacobian.get(8, 10), 1E-10);
        assertEquals(3169.91, jacobian.get(9, 10), 0.01);
        assertEquals(-2.36963E-10, jacobian.get(10, 10), 1E-15);
        assertEquals(0.00311128, jacobian.get(11, 10), 1E-8);

        assertEquals(0.0, jacobian.get(0, 11));
        assertEquals(0.0, jacobian.get(1, 11));
        assertEquals(0.0, jacobian.get(2, 11));
        assertEquals(0.0, jacobian.get(3, 11));
        assertEquals(0.0, jacobian.get(4, 11));
        assertEquals(0.0, jacobian.get(5, 11));
        assertEquals(0.0, jacobian.get(6, 11));
        assertEquals(0.0, jacobian.get(7, 11));
        assertEquals(0.0, jacobian.get(8, 11));
        assertEquals(0.0, jacobian.get(9, 11));
        assertEquals(0.0, jacobian.get(10, 11));
        assertEquals(0.0, jacobian.get(11, 11));

        assertEquals(-107.556, jacobian.get(0, 12), 0.001);
        assertEquals(0.0, jacobian.get(1, 12), 1E-9);
        assertEquals(-87.1738, jacobian.get(2, 12), 0.0001);
        assertEquals(-0.283867, jacobian.get(3, 12), 0.000001);
        assertEquals(0.0, jacobian.get(4, 12), 1E-9);
        assertEquals(0.0, jacobian.get(5, 12), 1E-9);
        assertEquals(-1366.45, jacobian.get(6, 12), 0.01);
        assertEquals(0.0, jacobian.get(7, 12), 1E-9);
        assertEquals(-195.014, jacobian.get(8, 12), 0.001);
        assertEquals(-1366.45, jacobian.get(9, 12), 0.01);
        assertEquals(-0.000472528, jacobian.get(10, 12), 1E-9);
        assertEquals(-0.00792618, jacobian.get(11, 12), 1E-8);

        assertEquals(0.0, jacobian.get(0, 13), 1E-9);
        assertEquals(0.0, jacobian.get(1, 13), 1E-9);
        assertEquals(0.0, jacobian.get(2, 13), 1E-9);
        assertEquals(0.0, jacobian.get(3, 13), 1E-9);
        assertEquals(0.0, jacobian.get(4, 13), 1E-9);
        assertEquals(-0.00310843, jacobian.get(5, 13), 1E-8);
        assertEquals(0.0, jacobian.get(6, 13), 1E-9);
        assertEquals(0.0, jacobian.get(7, 13), 1E-9);
        assertEquals(0.0, jacobian.get(8, 13), 1E-9);
        assertEquals(-0.00310843, jacobian.get(9, 13), 1E-8);
        assertEquals(0.0, jacobian.get(10, 13), 1E-9);
        assertEquals(-1.97470E-8, jacobian.get(11, 13), 1E-13);

        assertEquals(0.0, jacobian.get(0, 14), 1E-9);
        assertEquals(0.0, jacobian.get(1, 14), 1E-9);
        assertEquals(0.0, jacobian.get(2, 14), 1E-9);
        assertEquals(0.0, jacobian.get(3, 14), 1E-9);
        assertEquals(0.0, jacobian.get(4, 14), 1E-9);
        assertEquals(0.0, jacobian.get(5, 14), 1E-9);
        assertEquals(0.0, jacobian.get(6, 14), 1E-9);
        assertEquals(0.0, jacobian.get(7, 14), 1E-9);
        assertEquals(0.0, jacobian.get(8, 14), 1E-9);
        assertEquals(0.0, jacobian.get(9, 14), 1E-9);
        assertEquals(0.0, jacobian.get(10, 14), 1E-9);
        assertEquals(0.0, jacobian.get(11, 14), 1E-9);

        assertEquals(0.0232697, jacobian.get(0, 15), 1E-7);
        assertEquals(0.0, jacobian.get(1, 15), 1E-9);
        assertEquals(0.0209205, jacobian.get(2, 15), 1E-7);
        assertEquals(0.0, jacobian.get(3, 15), 1E-9);
        assertEquals(0.0, jacobian.get(4, 15), 1E-9);
        assertEquals(0.0, jacobian.get(5, 15), 1E-9);
        assertEquals(0.327928, jacobian.get(6, 15), 0.000001);
        assertEquals(0.0, jacobian.get(7, 15), 1E-9);
        assertEquals(0.0441902, jacobian.get(8, 15), 1E-7);
        assertEquals(0.327929, jacobian.get(9, 15), 0.000001);
        assertEquals(1.07544E-7, jacobian.get(10, 15), 1E-12);
        assertEquals(1.90217E-6, jacobian.get(11, 15), 1E-11);

        assertEquals(0.0, jacobian.get(0, 16), 1E-9);
        assertEquals(0.0, jacobian.get(1, 16), 1E-9);
        assertEquals(0.0, jacobian.get(2, 16), 1E-9);
        assertEquals(0.0, jacobian.get(3, 16), 1E-9);
        assertEquals(0.0, jacobian.get(4, 16), 1E-9);
        assertEquals(1.34346E-13, jacobian.get(5, 16), 1E-18);
        assertEquals(0.0, jacobian.get(6, 16), 1E-9);
        assertEquals(0.0, jacobian.get(7, 16), 1E-9);
        assertEquals(0.0, jacobian.get(8, 16), 1E-9);
        assertEquals(1.34346E-13, jacobian.get(9, 16), 1E-18);
        assertEquals(0.0, jacobian.get(10, 16), 1E-9);
        assertEquals(9.64421E-21, jacobian.get(11, 16), 1E-25);

        assertEquals(0.0, jacobian.get(0, 17), 1E-9);
        assertEquals(0.0, jacobian.get(1, 17), 1E-9);
        assertEquals(0.0, jacobian.get(2, 17), 1E-9);
        assertEquals(0.0, jacobian.get(3, 17), 1E-9);
        assertEquals(0.0, jacobian.get(4, 17), 1E-9);
        assertEquals(0.0, jacobian.get(5, 17), 1E-9);
        assertEquals(0.0, jacobian.get(6, 17), 1E-9);
        assertEquals(0.0, jacobian.get(7, 17), 1E-9);
        assertEquals(0.0, jacobian.get(8, 17), 1E-9);
        assertEquals(0.0, jacobian.get(9, 17), 1E-9);
        assertEquals(0.0, jacobian.get(10, 17), 1E-9);
        assertEquals(0.0, jacobian.get(11, 17), 1E-9);

        assertEquals(-5.72724E-5, jacobian.get(0, 18), 1E-10);
        assertEquals(0.0, jacobian.get(1, 18));
        assertEquals(-5.16107E-5, jacobian.get(2, 18), 1E-10);
        assertEquals(-3.34016E-5, jacobian.get(3, 18), 1E-10);
        assertEquals(0.0, jacobian.get(4, 18));
        assertEquals(0.0, jacobian.get(5, 18));
        assertEquals(-0.000808999, jacobian.get(6, 18), 1E-9);
        assertEquals(0.0, jacobian.get(7, 18));
        assertEquals(-0.000142285, jacobian.get(8, 18), 1E-9);
        assertEquals(-0.000808999, jacobian.get(9, 18), 1E-9);
        assertEquals(-7.30048E-10, jacobian.get(10, 18), 1E-15);
        assertEquals(-4.69265E-9, jacobian.get(11, 18), 1E-14);

        assertEquals(0.0, jacobian.get(0, 19));
        assertEquals(0.0, jacobian.get(1, 19));
        assertEquals(0.0, jacobian.get(2, 19));
        assertEquals(0.0, jacobian.get(3, 19));
        assertEquals(0.0, jacobian.get(4, 19));
        assertEquals(-0.000257716, jacobian.get(5, 19), 1E-9);
        assertEquals(0.0, jacobian.get(6, 19));
        assertEquals(0.0, jacobian.get(7, 19));
        assertEquals(0.0, jacobian.get(8, 19));
        assertEquals(-0.000257716, jacobian.get(9, 19), 1E-9);
        assertEquals(0.0, jacobian.get(10, 19));
        assertEquals(-2.49747E-11, jacobian.get(11, 19), 1E-16);

        assertEquals(0.0, jacobian.get(0, 20));
        assertEquals(0.0, jacobian.get(1, 20));
        assertEquals(0.0, jacobian.get(2, 20));
        assertEquals(0.0, jacobian.get(3, 20));
        assertEquals(0.0, jacobian.get(4, 20));
        assertEquals(0.0, jacobian.get(5, 20));
        assertEquals(0.0, jacobian.get(6, 20));
        assertEquals(0.0, jacobian.get(7, 20));
        assertEquals(0.0, jacobian.get(8, 20));
        assertEquals(0.0, jacobian.get(9, 20));
        assertEquals(0.0, jacobian.get(10, 20));
        assertEquals(0.0, jacobian.get(11, 20));

        assertEquals(-0.0712114, jacobian.get(0, 21), 1E-7);
        assertEquals(0.0, jacobian.get(1, 21));
        assertEquals(-0.0629656, jacobian.get(2, 21), 1E-7);
        assertEquals(0.0, jacobian.get(3, 21));
        assertEquals(0.0, jacobian.get(4, 21));
        assertEquals(0.0, jacobian.get(5, 21));
        assertEquals(-0.986987, jacobian.get(6, 21), 0.000001);
        assertEquals(0.0, jacobian.get(7, 21));
        assertEquals(-0.134177, jacobian.get(8, 21), 0.000001);
        assertEquals(-0.986987, jacobian.get(9, 21), 0.000001);
        assertEquals(-3.25945E-7, jacobian.get(10, 21), 1E-12);
        assertEquals(-5.72509E-6, jacobian.get(11, 21), 1E-11);

        assertEquals(0.0, jacobian.get(0, 22));
        assertEquals(0.0, jacobian.get(1, 22));
        assertEquals(0.0, jacobian.get(2, 22));
        assertEquals(0.0, jacobian.get(3, 22));
        assertEquals(0.0, jacobian.get(4, 22));
        assertEquals(-2.02266E-15, jacobian.get(5, 22), 1E-20);
        assertEquals(0.0, jacobian.get(6, 22));
        assertEquals(0.0, jacobian.get(7, 22));
        assertEquals(0.0, jacobian.get(8, 22));
        assertEquals(-2.02266E-15, jacobian.get(9, 22), 1E-20);
        assertEquals(0.0, jacobian.get(10, 22));
        assertEquals(-1.43510E-22, jacobian.get(11, 22), 1E-27);

        assertEquals(0.0, jacobian.get(0, 23));
        assertEquals(0.0, jacobian.get(1, 23));
        assertEquals(0.0, jacobian.get(2, 23));
        assertEquals(0.0, jacobian.get(3, 23));
        assertEquals(0.0, jacobian.get(4, 23));
        assertEquals(0.0, jacobian.get(5, 23));
        assertEquals(0.0, jacobian.get(6, 23));
        assertEquals(0.0, jacobian.get(7, 23));
        assertEquals(0.0, jacobian.get(8, 23));
        assertEquals(0.0, jacobian.get(9, 23));
        assertEquals(0.0, jacobian.get(10, 23));
        assertEquals(0.0, jacobian.get(11, 23));

        assertEquals(-48.6097, jacobian.get(0, 24), 0.0001);
        assertEquals(0.0, jacobian.get(1, 24), 1E-9);
        assertEquals(-27.6440, jacobian.get(2, 24), 0.0001);
        assertEquals(-22.1692, jacobian.get(3, 24), 0.001);
        assertEquals(0.0, jacobian.get(4, 24), 1E-9);
        assertEquals(0.0, jacobian.get(5, 24), 1E-9);
        assertEquals(0.0179661, jacobian.get(6, 24), 0.000001);
        assertEquals(0.0, jacobian.get(7, 24), 1E-9);
        assertEquals(-98.4230, jacobian.get(8, 24), 0.01);
        assertEquals(0.0179661, jacobian.get(9, 24), 0.000001);
        assertEquals(-0.000488312, jacobian.get(10, 24), 1E-9);
        assertEquals(1.04213E-7, jacobian.get(11, 24), 1E-12);

        assertEquals(0.0, jacobian.get(0, 25), 1E-9);
        assertEquals(0.0, jacobian.get(1, 25), 1E-9);
        assertEquals(0.000699146, jacobian.get(2, 25), 1E-9);
        assertEquals(0.0, jacobian.get(3, 25), 1E-9);
        assertEquals(0.0, jacobian.get(4, 25), 1E-9);
        assertEquals(-35228.6, jacobian.get(5, 25), 0.1);
        assertEquals(15.1643, jacobian.get(6, 25), 0.0001);
        assertEquals(-4212.96, jacobian.get(7, 25), 0.01);
        assertEquals(0.000699146, jacobian.get(8, 25), 1E-9);
        assertEquals(-35213.4, jacobian.get(9, 25), 0.1);
        assertEquals(2.07411E-9, jacobian.get(10, 25), 1E-14);
        assertEquals(-0.0585830, jacobian.get(11, 25), 1E-7);

        assertEquals(0.0, jacobian.get(0, 26), 1E-9);
        assertEquals(0.0, jacobian.get(1, 26), 1E-9);
        assertEquals(0.0, jacobian.get(2, 26), 1E-9);
        assertEquals(0.0, jacobian.get(3, 26), 1E-9);
        assertEquals(0.0, jacobian.get(4, 26), 1E-9);
        assertEquals(0.0, jacobian.get(5, 26), 1E-9);
        assertEquals(0.0, jacobian.get(6, 26), 1E-9);
        assertEquals(0.0, jacobian.get(7, 26), 1E-9);
        assertEquals(0.0, jacobian.get(8, 26), 1E-9);
        assertEquals(0.0, jacobian.get(9, 26), 1E-9);
        assertEquals(0.0, jacobian.get(10, 26), 1E-9);
        assertEquals(0.0, jacobian.get(11, 26), 1E-9);

        assertEquals(0.286449, jacobian.get(0, 27), 0.000001);
        assertEquals(0.0, jacobian.get(1, 27));
        assertEquals(0.291860, jacobian.get(2, 27), 0.000001);
        assertEquals(0.302845, jacobian.get(3, 27), 0.000001);
        assertEquals(0.0, jacobian.get(4, 27));
        assertEquals(0.0, jacobian.get(5, 27));
        assertEquals(8.95111, jacobian.get(6, 27), 0.00001);
        assertEquals(0.0, jacobian.get(7, 27));
        assertEquals(0.881154, jacobian.get(8, 27), 0.000001);
        assertEquals(8.95111, jacobian.get(9, 27), 0.00001);
        assertEquals(7.23938E-6, jacobian.get(10, 27), 1E-11);
        assertEquals(5.19215E-5, jacobian.get(11, 27), 1E-10);

        assertEquals(0.0, jacobian.get(0, 28));
        assertEquals(0.0, jacobian.get(1, 28));
        assertEquals(0.0, jacobian.get(2, 28));
        assertEquals(0.0, jacobian.get(3, 28));
        assertEquals(0.0, jacobian.get(4, 28));
        assertEquals(12.0947, jacobian.get(5, 28), 0.001);
        assertEquals(0.0, jacobian.get(6, 28));
        assertEquals(0.0, jacobian.get(7, 28));
        assertEquals(0.0, jacobian.get(8, 28));
        assertEquals(12.0947, jacobian.get(9, 28), 0.001);
        assertEquals(0.0, jacobian.get(10, 28));
        assertEquals(0.000126459, jacobian.get(11, 28), 1E-9);

        assertEquals(0.0, jacobian.get(0, 29));
        assertEquals(0.0, jacobian.get(1, 29));
        assertEquals(0.0, jacobian.get(2, 29));
        assertEquals(0.0, jacobian.get(3, 29));
        assertEquals(0.0, jacobian.get(4, 29));
        assertEquals(0.0, jacobian.get(5, 29));
        assertEquals(0.0, jacobian.get(6, 29));
        assertEquals(992.349, jacobian.get(7, 29), 0.001);
        assertEquals(0.0, jacobian.get(8, 29));
        assertEquals(0.0, jacobian.get(9, 29));
        assertEquals(0.0, jacobian.get(10, 29));
        assertEquals(0.0138149, jacobian.get(11, 29), 1E-7);

        assertEquals(0.0, jacobian.get(0, 30), 1E-9);
        assertEquals(0.0, jacobian.get(1, 30), 1E-9);
        assertEquals(-50.9275, jacobian.get(2, 30), 0.0001);
        assertEquals(0.0, jacobian.get(3, 30), 1E-9);
        assertEquals(0.0, jacobian.get(4, 30), 1E-9);
        assertEquals(0.0, jacobian.get(5, 30), 1E-9);
        assertEquals(-13.8967, jacobian.get(6, 30), 0.0001);
        assertEquals(0.0, jacobian.get(7, 30), 1E-9);
        assertEquals(-50.9275, jacobian.get(8, 30), 0.0001);
        assertEquals(-13.8967, jacobian.get(9, 30), 0.0001);
        assertEquals(-0.000295416, jacobian.get(10, 30), 1E-9);
        assertEquals(-8.06090E-5, jacobian.get(11, 30), 1E-10);

        assertEquals(0.0, jacobian.get(0, 31), 1E-9);
        assertEquals(0.0, jacobian.get(1, 31), 1E-9);
        assertEquals(0.0, jacobian.get(2, 31), 1E-9);
        assertEquals(0.0, jacobian.get(3, 31), 1E-9);
        assertEquals(0.0, jacobian.get(4, 31), 1E-9);
        assertEquals(0.0, jacobian.get(5, 31), 1E-9);
        assertEquals(0.0, jacobian.get(6, 31), 1E-9);
        assertEquals(0.0, jacobian.get(7, 31), 1E-9);
        assertEquals(0.0, jacobian.get(8, 31), 1E-9);
        assertEquals(0.0, jacobian.get(9, 31), 1E-9);
        assertEquals(0.0, jacobian.get(10, 31), 1E-9);
        assertEquals(0.0, jacobian.get(11, 31), 1E-9);

        assertEquals(0.0, jacobian.get(0, 32), 1E-9);
        assertEquals(0.0, jacobian.get(1, 32), 1E-9);
        assertEquals(0.0, jacobian.get(2, 32), 1E-9);
        assertEquals(0.0, jacobian.get(3, 32), 1E-9);
        assertEquals(0.0, jacobian.get(4, 32), 1E-9);
        assertEquals(0.0, jacobian.get(5, 32), 1E-9);
        assertEquals(0.0, jacobian.get(6, 32), 1E-9);
        assertEquals(0.0, jacobian.get(7, 32), 1E-9);
        assertEquals(0.0, jacobian.get(8, 32), 1E-9);
        assertEquals(0.0, jacobian.get(9, 32), 1E-9);
        assertEquals(0.0, jacobian.get(10, 32), 1E-9);
        assertEquals(0.0, jacobian.get(11, 32), 1E-9);

        assertEquals(-0.606787, jacobian.get(0, 33), 0.000001);
        assertEquals(0.0, jacobian.get(1, 33));
        assertEquals(-0.505366, jacobian.get(2, 33), 0.000001);
        assertEquals(-0.301107, jacobian.get(3, 33), 0.000001);
        assertEquals(0.0, jacobian.get(4, 33));
        assertEquals(0.0, jacobian.get(5, 33));
        assertEquals(-7.92161, jacobian.get(6, 33), 0.00001);
        assertEquals(0.0, jacobian.get(7, 33));
        assertEquals(-1.41326, jacobian.get(8, 33), 0.00001);
        assertEquals(-7.92161, jacobian.get(9, 33), 0.00001);
        assertEquals(-6.87504E-6, jacobian.get(10, 33), 1E-11);
        assertEquals(-4.59498E-5, jacobian.get(11, 33), 1E-10);

        assertEquals(0.0, jacobian.get(0, 34));
        assertEquals(0.0, jacobian.get(1, 34));
        assertEquals(0.0, jacobian.get(2, 34));
        assertEquals(0.0, jacobian.get(3, 34));
        assertEquals(0.0, jacobian.get(4, 34));
        assertEquals(-18622.4, jacobian.get(5, 34), 0.1);
        assertEquals(0.0, jacobian.get(6, 34));
        assertEquals(0.0, jacobian.get(7, 34));
        assertEquals(0.0, jacobian.get(8, 34));
        assertEquals(-18622.4, jacobian.get(9, 34), 0.1);
        assertEquals(0.0, jacobian.get(10, 34));
        assertEquals(-0.000117412, jacobian.get(11, 34), 1E-9);

        assertEquals(0.0, jacobian.get(0, 35));
        assertEquals(0.0, jacobian.get(1, 35));
        assertEquals(0.0, jacobian.get(2, 35));
        assertEquals(0.0, jacobian.get(3, 35));
        assertEquals(0.0, jacobian.get(4, 35));
        assertEquals(0.0, jacobian.get(5, 35));
        assertEquals(0.0, jacobian.get(6, 35));
        assertEquals(-992.349, jacobian.get(7, 35), 0.001);
        assertEquals(0.0, jacobian.get(8, 35));
        assertEquals(0.0, jacobian.get(9, 35));
        assertEquals(0.0, jacobian.get(10, 35));
        assertEquals(-0.0138149, jacobian.get(11, 35), 1E-7);

        assertEquals(-63.6944, jacobian.get(0, 36), 0.0001);
        assertEquals(0.0, jacobian.get(1, 36), 1E-9);
        assertEquals(-51.4076, jacobian.get(2, 36), 0.0001);
        assertEquals(-77.5186, jacobian.get(3, 36), 0.0001);
        assertEquals(0.0, jacobian.get(4, 36), 1E-9);
        assertEquals(0.0, jacobian.get(5, 36), 1E-9);
        assertEquals(-805.815, jacobian.get(6, 36), 0.001);
        assertEquals(0.0, jacobian.get(7, 36), 1E-9);
        assertEquals(-192.620, jacobian.get(8, 36), 0.001);
        assertEquals(-805.815, jacobian.get(9, 36), 0.001);
        assertEquals(-0.00135588, jacobian.get(10, 36), 1E-8);
        assertEquals(-0.00467418, jacobian.get(11, 36), 1E-8);

        assertEquals(0.0, jacobian.get(0, 37), 1E-9);
        assertEquals(0.0, jacobian.get(1, 37), 1E-9);
        assertEquals(0.0, jacobian.get(2, 37), 1E-9);
        assertEquals(0.0, jacobian.get(3, 37), 1E-9);
        assertEquals(0.0, jacobian.get(4, 37), 1E-9);
        assertEquals(-64269.4, jacobian.get(5, 37), 0.1);
        assertEquals(0.0, jacobian.get(6, 37), 1E-9);
        assertEquals(0.0, jacobian.get(7, 37), 1E-9);
        assertEquals(0.0, jacobian.get(8, 37), 1E-9);
        assertEquals(-64269.4, jacobian.get(9, 37), 0.1);
        assertEquals(0.0, jacobian.get(10, 37), 1E-9);
        assertEquals(-0.00131670, jacobian.get(11, 37), 1E-8);

        assertEquals(0.0, jacobian.get(0, 38), 1E-9);
        assertEquals(0.0, jacobian.get(1, 38), 1E-9);
        assertEquals(0.0, jacobian.get(2, 38), 1E-9);
        assertEquals(0.0, jacobian.get(3, 38), 1E-9);
        assertEquals(0.0, jacobian.get(4, 38), 1E-9);
        assertEquals(0.0, jacobian.get(5, 38), 1E-9);
        assertEquals(0.0, jacobian.get(6, 38), 1E-9);
        assertEquals(-33579.7, jacobian.get(7, 38), 0.1);
        assertEquals(0.0, jacobian.get(8, 38), 1E-9);
        assertEquals(0.0, jacobian.get(9, 38), 1E-9);
        assertEquals(0.0, jacobian.get(10, 38), 1E-9);
        assertEquals(-0.467475, jacobian.get(11, 38), 0.000001);

        assertEquals(-172.205, jacobian.get(0, 39), 0.001);
        assertEquals(0.0, jacobian.get(1, 39), 1E-9);
        assertEquals(-57.3677, jacobian.get(2, 39), 0.0001);
        assertEquals(-78.4406, jacobian.get(3, 39), 0.0001);
        assertEquals(0.0, jacobian.get(4, 39), 1E-9);
        assertEquals(0.0, jacobian.get(5, 39), 1E-9);
        assertEquals(-899.240, jacobian.get(6, 39), 0.001);
        assertEquals(0.0, jacobian.get(7, 39), 1E-9);
        assertEquals(-308.013, jacobian.get(8, 39), 0.001);
        assertEquals(-899.240, jacobian.get(9, 39), 0.001);
        assertEquals(-0.00159916, jacobian.get(10, 39), 1E-8);
        assertEquals(-0.00521610, jacobian.get(11, 39), 1E-8);

        assertEquals(0.0, jacobian.get(0, 40), 1E-9);
        assertEquals(0.0, jacobian.get(1, 40), 1E-9);
        assertEquals(0.0, jacobian.get(2, 40), 1E-9);
        assertEquals(0.0, jacobian.get(3, 40), 1E-9);
        assertEquals(0.0, jacobian.get(4, 40), 1E-9);
        assertEquals(3.02966, jacobian.get(5, 40), 0.00001);
        assertEquals(0.0, jacobian.get(6, 40), 1E-9);
        assertEquals(0.0, jacobian.get(7, 40), 1E-9);
        assertEquals(0.0, jacobian.get(8, 40), 1E-9);
        assertEquals(3.02966, jacobian.get(9, 40), 0.00001);
        assertEquals(0.0, jacobian.get(10, 40), 1E-9);
        assertEquals(-2.51807E-5, jacobian.get(11, 40), 1E-10);

        assertEquals(0.0, jacobian.get(0, 41), 1E-9);
        assertEquals(0.0, jacobian.get(1, 41), 1E-9);
        assertEquals(0.0, jacobian.get(2, 41), 1E-9);
        assertEquals(0.0, jacobian.get(3, 41), 1E-9);
        assertEquals(0.0, jacobian.get(4, 41), 1E-9);
        assertEquals(0.0, jacobian.get(5, 41), 1E-9);
        assertEquals(0.0, jacobian.get(6, 41), 1E-9);
        assertEquals(0.0, jacobian.get(7, 41), 1E-9);
        assertEquals(0.0, jacobian.get(8, 41), 1E-9);
        assertEquals(0.0, jacobian.get(9, 41), 1E-9);
        assertEquals(0.0, jacobian.get(10, 41), 1E-9);
        assertEquals(0.0, jacobian.get(11, 41), 1E-9);

        assertEquals(0.0, jacobian.get(0, 42));
        assertEquals(0.0, jacobian.get(1, 42));
        assertEquals(0.0, jacobian.get(2, 42));
        assertEquals(-0.00170509, jacobian.get(3, 42), 1E-8);
        assertEquals(0.0, jacobian.get(4, 42));
        assertEquals(0.0, jacobian.get(5, 42));
        assertEquals(0.0, jacobian.get(6, 42));
        assertEquals(0.0, jacobian.get(7, 42));
        assertEquals(-0.00170509, jacobian.get(8, 42), 1E-8);
        assertEquals(0.0, jacobian.get(9, 42));
        assertEquals(-2.37372E-8, jacobian.get(10, 42), 1E-13);
        assertEquals(0.0, jacobian.get(11, 42));

        assertEquals(0.0, jacobian.get(0, 43));
        assertEquals(0.0, jacobian.get(1, 43));
        assertEquals(0.0, jacobian.get(2, 43));
        assertEquals(0.0, jacobian.get(3, 43));
        assertEquals(0.0, jacobian.get(4, 43));
        assertEquals(0.0, jacobian.get(5, 43));
        assertEquals(0.0, jacobian.get(6, 43));
        assertEquals(0.0, jacobian.get(7, 43));
        assertEquals(0.0, jacobian.get(8, 43));
        assertEquals(0.0, jacobian.get(9, 43));
        assertEquals(0.0, jacobian.get(10, 43));
        assertEquals(0.0, jacobian.get(11, 43));

        assertEquals(0.0, jacobian.get(0, 44));
        assertEquals(0.0, jacobian.get(1, 44));
        assertEquals(0.0, jacobian.get(2, 44));
        assertEquals(0.0, jacobian.get(3, 44));
        assertEquals(0.0, jacobian.get(4, 44));
        assertEquals(0.0, jacobian.get(5, 44));
        assertEquals(0.0, jacobian.get(6, 44));
        assertEquals(0.0, jacobian.get(7, 44));
        assertEquals(0.0, jacobian.get(8, 44));
        assertEquals(0.0, jacobian.get(9, 44));
        assertEquals(0.0, jacobian.get(10, 44));
        assertEquals(0.0, jacobian.get(11, 44));

        assertEquals(-0.00308132, jacobian.get(0, 45), 1E-8);
        assertEquals(0.0, jacobian.get(1, 45));
        assertEquals(-0.00264920, jacobian.get(2, 45), 1E-8);
        assertEquals(0.0, jacobian.get(3, 45));
        assertEquals(0.0, jacobian.get(4, 45));
        assertEquals(0.0, jacobian.get(5, 45));
        assertEquals(-0.0415262, jacobian.get(6, 45), 1E-7);
        assertEquals(0.0, jacobian.get(7, 45));
        assertEquals(-0.00573052, jacobian.get(8, 45), 1E-8);
        assertEquals(-0.0415262, jacobian.get(9, 45), 1E-7);
        assertEquals(-1.38741E-8, jacobian.get(10, 45), 1E-13);
        assertEquals(-2.40875E-7, jacobian.get(11, 45), 1E-12);

        assertEquals(0.0, jacobian.get(0, 46));
        assertEquals(0.0, jacobian.get(1, 46));
        assertEquals(0.0, jacobian.get(2, 46));
        assertEquals(0.0, jacobian.get(3, 46));
        assertEquals(0.0, jacobian.get(4, 46));
        assertEquals(-1.71859, jacobian.get(5, 46), 0.00001);
        assertEquals(0.0, jacobian.get(6, 46));
        assertEquals(0.0, jacobian.get(7, 46));
        assertEquals(0.0, jacobian.get(8, 46));
        assertEquals(-1.71859, jacobian.get(9, 46), 0.00001);
        assertEquals(0.0, jacobian.get(10, 46));
        assertEquals(-3.19771E-8, jacobian.get(11, 46), 1E-13);

        assertEquals(0.0, jacobian.get(0, 47));
        assertEquals(0.0, jacobian.get(1, 47));
        assertEquals(0.0, jacobian.get(2, 47));
        assertEquals(0.0, jacobian.get(3, 47));
        assertEquals(0.0, jacobian.get(4, 47));
        assertEquals(0.0, jacobian.get(5, 47));
        assertEquals(0.0, jacobian.get(6, 47));
        assertEquals(0.0, jacobian.get(7, 47));
        assertEquals(0.0, jacobian.get(8, 47));
        assertEquals(0.0, jacobian.get(9, 47));
        assertEquals(0.0, jacobian.get(10, 47));
        assertEquals(0.0, jacobian.get(11, 47));

        assertEquals(2.23401, jacobian.get(0, 48), 0.00001);
        assertEquals(0.0, jacobian.get(1, 48));
        assertEquals(1.67275, jacobian.get(2, 48), 0.00001);
        assertEquals(1.27486, jacobian.get(3, 48), 0.00001);
        assertEquals(0.0, jacobian.get(4, 48));
        assertEquals(0.0, jacobian.get(5, 48));
        assertEquals(-0.000511008, jacobian.get(6, 48), 1E-9);
        assertEquals(0.0, jacobian.get(7, 48));
        assertEquals(5.18162, jacobian.get(8, 48), 0.00001);
        assertEquals(-0.000511008, jacobian.get(9, 48), 1E-9);
        assertEquals(2.68710E-5, jacobian.get(10, 48), 1E-10);
        assertEquals(-2.96413E-9, jacobian.get(11, 48), 1E-14);

        assertEquals(0.0, jacobian.get(0, 49));
        assertEquals(0.0, jacobian.get(1, 49));
        assertEquals(-0.00126247, jacobian.get(2, 49), 1E-8);
        assertEquals(0.0, jacobian.get(3, 49));
        assertEquals(0.0, jacobian.get(4, 49));
        assertEquals(10505.9, jacobian.get(5, 49), 0.1);
        assertEquals(110.643, jacobian.get(6, 49), 0.001);
        assertEquals(6062.91, jacobian.get(7, 49), 0.01);
        assertEquals(-0.00126247, jacobian.get(8, 49), 1E-8);
        assertEquals(10616.5, jacobian.get(9, 49), 0.1);
        assertEquals(-3.74528E-9, jacobian.get(10, 49), 1E-14);
        assertEquals(0.0850613, jacobian.get(11, 49), 1E-7);

        assertEquals(0.0, jacobian.get(0, 50));
        assertEquals(0.0, jacobian.get(1, 50));
        assertEquals(0.0, jacobian.get(2, 50));
        assertEquals(0.0, jacobian.get(3, 50));
        assertEquals(0.0, jacobian.get(4, 50));
        assertEquals(0.0, jacobian.get(5, 50));
        assertEquals(0.0, jacobian.get(6, 50));
        assertEquals(0.0, jacobian.get(7, 50));
        assertEquals(0.0, jacobian.get(8, 50));
        assertEquals(0.0, jacobian.get(9, 50));
        assertEquals(0.0, jacobian.get(10, 50));
        assertEquals(0.0, jacobian.get(11, 50));

        assertEquals(0.0341698, jacobian.get(0, 51), 1E-7);
        assertEquals(0.0, jacobian.get(1, 51));
        assertEquals(0.0267551, jacobian.get(2, 51), 1E-7);
        assertEquals(0.0189239, jacobian.get(3, 51), 1E-7);
        assertEquals(0.0, jacobian.get(4, 51));
        assertEquals(0.0, jacobian.get(5, 51));
        assertEquals(-8.53478E-6, jacobian.get(6, 51), 1E-11);
        assertEquals(0.0, jacobian.get(7, 51));
        assertEquals(0.0798488, jacobian.get(8, 51), 1E-7);
        assertEquals(-8.53478E-6, jacobian.get(9, 51), 1E-11);
        assertEquals(4.06909E-7, jacobian.get(10, 51), 1E-12);
        assertEquals(-4.95065E-11, jacobian.get(11, 51), 1E-16);

        assertEquals(0.0, jacobian.get(0, 52));
        assertEquals(0.0, jacobian.get(1, 52));
        assertEquals(-1.63758E-5, jacobian.get(2, 52), 1E-10);
        assertEquals(0.0, jacobian.get(3, 52));
        assertEquals(0.0, jacobian.get(4, 52));
        assertEquals(197.691, jacobian.get(5, 52), 0.001);
        assertEquals(1.16694, jacobian.get(6, 52), 0.00001);
        assertEquals(66.4522, jacobian.get(7, 52), 0.0001);
        assertEquals(-1.63758E-5, jacobian.get(8, 52), 1E-10);
        assertEquals(198.858, jacobian.get(9, 52), 0.001);
        assertEquals(-4.85811E-11, jacobian.get(10, 52), 1E-16);
        assertEquals(0.000932151, jacobian.get(11, 52), 1E-9);

        assertEquals(0.0, jacobian.get(0, 53));
        assertEquals(0.0, jacobian.get(1, 53));
        assertEquals(0.0, jacobian.get(2, 53));
        assertEquals(0.0, jacobian.get(3, 53));
        assertEquals(0.0, jacobian.get(4, 53));
        assertEquals(0.0, jacobian.get(5, 53));
        assertEquals(0.0, jacobian.get(6, 53));
        assertEquals(0.0, jacobian.get(7, 53));
        assertEquals(0.0, jacobian.get(8, 53));
        assertEquals(0.0, jacobian.get(9, 53));
        assertEquals(0.0, jacobian.get(10, 53));
        assertEquals(0.0, jacobian.get(11, 53));

        assertEquals(0.286449, jacobian.get(0, 54), 0.000001);
        assertEquals(0.0, jacobian.get(1, 54));
        assertEquals(0.292246, jacobian.get(2, 54), 0.000001);
        assertEquals(0.302845, jacobian.get(3, 54), 0.000001);
        assertEquals(0.0, jacobian.get(4, 54));
        assertEquals(0.0, jacobian.get(5, 54));
        assertEquals(-0.000191438, jacobian.get(6, 54), 1E-9);
        assertEquals(0.0, jacobian.get(7, 54));
        assertEquals(0.881540, jacobian.get(8, 54), 0.000001);
        assertEquals(-0.000191438, jacobian.get(9, 54), 1E-9);
        assertEquals(7.24052E-6, jacobian.get(10, 54), 1E-11);
        assertEquals(-1.11045E-9, jacobian.get(11, 54), 1E-14);

        assertEquals(0.0, jacobian.get(0, 55));
        assertEquals(0.0, jacobian.get(1, 55));
        assertEquals(-0.000386318, jacobian.get(2, 55), 1E-9);
        assertEquals(0.0, jacobian.get(3, 55));
        assertEquals(0.0, jacobian.get(4, 55));
        assertEquals(0.0, jacobian.get(5, 55));
        assertEquals(8.95131, jacobian.get(6, 55), 0.00001);
        assertEquals(0.0, jacobian.get(7, 55));
        assertEquals(-0.000386318, jacobian.get(8, 55), 1E-9);
        assertEquals(8.95131, jacobian.get(9, 55), 0.00001);
        assertEquals(-1.14607E-9, jacobian.get(10, 55), 1E-14);
        assertEquals(5.19226E-5, jacobian.get(11, 55), 1E-10);

        assertEquals(0.0, jacobian.get(0, 56));
        assertEquals(0.0, jacobian.get(1, 56));
        assertEquals(0.0, jacobian.get(2, 56));
        assertEquals(0.0, jacobian.get(3, 56));
        assertEquals(0.0, jacobian.get(4, 56));
        assertEquals(0.0, jacobian.get(5, 56));
        assertEquals(0.0, jacobian.get(6, 56));
        assertEquals(0.0, jacobian.get(7, 56));
        assertEquals(0.0, jacobian.get(8, 56));
        assertEquals(0.0, jacobian.get(9, 56));
        assertEquals(0.0, jacobian.get(10, 56));
        assertEquals(0.0, jacobian.get(11, 56));

        assertEquals(0.0, jacobian.get(0, 57));
        assertEquals(0.0, jacobian.get(1, 57));
        assertEquals(0.0, jacobian.get(2, 57));
        assertEquals(0.0, jacobian.get(3, 57));
        assertEquals(0.0, jacobian.get(4, 57));
        assertEquals(12.0947, jacobian.get(5, 57), 0.0001);
        assertEquals(0.0, jacobian.get(6, 57));
        assertEquals(0.0, jacobian.get(7, 57));
        assertEquals(0.0, jacobian.get(8, 57));
        assertEquals(12.0947, jacobian.get(9, 57), 0.0001);
        assertEquals(0.0, jacobian.get(10, 57));
        assertEquals(0.000126459, jacobian.get(11, 57), 1E-9);

        assertEquals(0.0, jacobian.get(0, 58));
        assertEquals(0.0, jacobian.get(1, 58));
        assertEquals(0.0, jacobian.get(2, 58));
        assertEquals(0.0, jacobian.get(3, 58));
        assertEquals(0.0, jacobian.get(4, 58));
        assertEquals(0.0, jacobian.get(5, 58));
        assertEquals(0.0, jacobian.get(6, 58));
        assertEquals(0.0, jacobian.get(7, 58));
        assertEquals(0.0, jacobian.get(8, 58));
        assertEquals(0.0, jacobian.get(9, 58));
        assertEquals(0.0, jacobian.get(10, 58));
        assertEquals(0.0, jacobian.get(11, 58));

        assertEquals(0.0, jacobian.get(0, 59));
        assertEquals(0.0, jacobian.get(1, 59));
        assertEquals(0.0, jacobian.get(2, 59));
        assertEquals(0.0, jacobian.get(3, 59));
        assertEquals(0.0, jacobian.get(4, 59));
        assertEquals(0.0, jacobian.get(5, 59));
        assertEquals(0.0, jacobian.get(6, 59));
        assertEquals(992.349, jacobian.get(7, 59), 0.001);
        assertEquals(0.0, jacobian.get(8, 59));
        assertEquals(0.0, jacobian.get(9, 59));
        assertEquals(0.0, jacobian.get(10, 59));
        assertEquals(0.0138149, jacobian.get(11, 59), 1E-7);

        // Check that the derivatives are still correct if we only keep a few of
        // the coefficients.
        final List<Coefficient> minicoeffs = new ArrayList<Coefficient>();
        minicoeffs.add(coeffs.get(0));
        minicoeffs.add(coeffs.get(coeffs.size() - 1));
        final Vector minimeans = new DenseVector(2);
        minimeans.set(0, vmeans.get(0));
        minimeans.set(1, vmeans.get(vmeans.size() - 1));
        final DifferentiableModel minimodel = new ExpectedTargetModel(minicoeffs, land);
        final Vector miniresult = minimodel.getTargetValues(targets, minimeans);

        assertEquals(34.0579, miniresult.get(0), 0.0001);
        assertEquals(0.0, miniresult.get(1));
        assertEquals(28.5721, miniresult.get(2), 0.0001);
        assertEquals(15.1427, miniresult.get(3), 0.0001);
        assertEquals(0.0, miniresult.get(4));
        assertEquals(58362.4, miniresult.get(5), 0.1);
        assertEquals(447.868, miniresult.get(6), 0.001);
        assertEquals(10260.1, miniresult.get(7), 0.1);
        assertEquals(77.7727, miniresult.get(8), 0.0001);
        assertEquals(58810.2, miniresult.get(9), 0.1);
        assertEquals(3.62036E-4, miniresult.get(10), 1E-9);
        assertEquals(0.145748, miniresult.get(11), 0.000001);

        final Matrix minijacobian = minimodel.getJacobian(targets, minimeans);

        assertEquals(0.131598, minijacobian.get(0, 0), 0.000001);
        assertEquals(0.0, minijacobian.get(1, 0));
        assertEquals(0.0382057, minijacobian.get(2, 0), 1E-7);
        assertEquals(0.103348, minijacobian.get(3, 0), 0.000001);
        assertEquals(0.0, minijacobian.get(4, 0));
        assertEquals(0.0, minijacobian.get(5, 0));
        assertEquals(-1.09207E-5, minijacobian.get(6, 0), 1E-10);
        assertEquals(0.0, minijacobian.get(7, 0));
        assertEquals(0.273151, minijacobian.get(8, 0), 0.000001);
        assertEquals(-1.09207E-5, minijacobian.get(9, 0), 1E-10);
        assertEquals(1.78974E-6, minijacobian.get(10, 0), 1E-11);
        assertEquals(-6.33464E-11, minijacobian.get(11, 0), 1E-16);

        assertEquals(0.0, minijacobian.get(0, 1));
        assertEquals(0.0, minijacobian.get(1, 1));
        assertEquals(0.0, minijacobian.get(2, 1));
        assertEquals(0.0, minijacobian.get(3, 1));
        assertEquals(0.0, minijacobian.get(4, 1));
        assertEquals(0.0, minijacobian.get(5, 1));
        assertEquals(0.0, minijacobian.get(6, 1));
        assertEquals(992.349, minijacobian.get(7, 1), 0.001);
        assertEquals(0.0, minijacobian.get(8, 1));
        assertEquals(0.0, minijacobian.get(9, 1));
        assertEquals(0.0, minijacobian.get(10, 1));
        assertEquals(0.0138149, minijacobian.get(11, 1), 1E-7);
    }

    @Test
    public void testEstimation() throws Exception
    {
        final ResourceBundle rb = ResourceUtil.getResourceBundle("sd");

        final String url = ResourceUtil.checkAndGetProperty(rb, "InputDatabase");
        final String user = ResourceUtil.checkAndGetProperty(rb, "InputDatabaseUser");
        final String password = ResourceUtil.checkAndGetProperty(rb, "InputDatabasePassword");

        setUpTestInventory(url, user, password);

        long time = System.currentTimeMillis();
        final StandardSDModel sd = new StandardSDModel();
        final CSVEstimationReader reader = new CSVEstimationReader("testconfig\\targets.csv",
                false, "testconfig\\parameters.csv", false);
        checkTargets(reader.readTargets());
        sd.calibrateModel(reader, 2010, 2011, 1E-5, 50);
        SSessionJdbc.getThreadLocalSession().commit();
        time = System.currentTimeMillis() - time;
        System.out.println(time);
    }

    public void checkTargets(List<EstimationTarget> targets)
    {
        // Insert test targets here.
        // Eventually do something like this to get the target values:
        // SSessionJdbc.getThreadLocalSession.query( new
        // SQuery<ObservedDevelopment>(ObservedDevelopment.meta));
        // to get total development by zone and space type, and total
        // redevelopment (infill,
        // brownfield) by zone and space type
        // SSessionJdbc.getThreadLocalSession.query(new
        // SQuery<ObservedFAR>(ObservedFAR.meta));
        // to get average FAR of new development by space type, or better yet
        // FAR distribution of
        // new development by space type (histogram).
        final int[] spacetypes = {3, 5};
        final int[] zones = {11, 12, 21, 22};
        /*
         * double[][] spaceTypeTazTargets = new double[][] {{30, 0, 32, 12}, {0, 65932, 422, 12209}}; double[] redevelopmentIntoSpaceTypeTargets = new
         * double[] {90, 60099}; double[] spaceTypeIntensityTargets = new double[] {0.0003, 0.102};
         */
        final double[][] spaceTypeTazTargets = new double[][] { {2, 0, 39, 22},
                {0, 36057, 684, 11855}};
        final double[] redevelopmentIntoSpaceTypeTargets = new double[] {167, 42797};
        final double[] spaceTypeIntensityTargets = new double[] {0.0003, 0.127};
        int t = 0;
        for (int i = 0; i < spacetypes.length; i++)
        {
            for (int j = 0; j < zones.length; j++)
            {
                final EstimationTarget target = targets.get(t);
                assertTrue(target instanceof SpaceTypeTAZTarget);
                final SpaceTypeTAZTarget taztarget = (SpaceTypeTAZTarget) target;
                assertEquals(spacetypes[i], taztarget.getSpacetype());
                assertEquals(zones[j], taztarget.getZone());
                assertEquals(spaceTypeTazTargets[i][j], taztarget.getTargetValue());
                t++;
            }
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            final EstimationTarget target = targets.get(t);
            assertTrue(target instanceof RedevelopmentIntoSpaceTypeTarget);
            final RedevelopmentIntoSpaceTypeTarget redevel = (RedevelopmentIntoSpaceTypeTarget) target;
            assertEquals(spacetypes[i], redevel.getSpacetype());
            assertEquals(redevelopmentIntoSpaceTypeTargets[i], target.getTargetValue());
            t++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            final EstimationTarget target = targets.get(t);
            assertTrue(target instanceof SpaceTypeIntensityTarget);
            final SpaceTypeIntensityTarget fartarget = (SpaceTypeIntensityTarget) target;
            assertEquals(spacetypes[i], fartarget.getSpacetype());
            assertEquals(spaceTypeIntensityTargets[i], target.getTargetValue());
            t++;
        }
    }

    public void checkTargetVariance(double[][] variance)
    {
        /*
         * result[0][0] = 9; result[1][1] = 1E-12; result[2][2] = 10.24; result[3][3] = 1.44; result[4][4] = 1E-12; result[5][5] = 43470286;
         * result[6][6] = 1780.84; result[7][7] = 1490597; result[8][8] = 81; result[9][9] = 36118898; result[10][10] = 9E-10; result[11][11] =
         * 0.000104;
         */
        assertEquals(0.04, variance[0][0]);
        assertEquals(1E-12, variance[1][1]);
        assertEquals(15.21, variance[2][2]);
        assertEquals(4.84, variance[3][3]);
        assertEquals(1E-12, variance[4][4]);
        assertEquals(13001072, variance[5][5]);
        assertEquals(4678.56, variance[6][6]);
        assertEquals(1405410, variance[7][7]);
        assertEquals(278.89, variance[8][8]);
        assertEquals(18315832, variance[9][9]);
        assertEquals(9E-10, variance[10][10]);
        assertEquals(0.000161, variance[11][11]);
    }

    public void checkCoeffs(List<Coefficient> coeffs)
    {
        // We need a way for the user to specify which coefficients are to be
        // modified,
        // and possibly allow the user to lock two or more coefficients to the
        // same value
        final int[] spacetypes = {3, 5, 95};
        final int[] spacetypesnv = {3, 5};
        int c = 0;
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("above-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("andisp-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("addconst-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("below-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("chdisp-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("dddisp-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("democonst-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("drltconst-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("intdisp-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("newconst-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("typdisp-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("ncconst-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("typdisp-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("randisp-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("rendconst-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("renoconst-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("step-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            assertEquals("stepamt-" + spacetypes[i], coeffs.get(c).getName());
            c++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            for (int j = 0; j < spacetypesnv.length; j++)
            {
                assertEquals("trans-" + spacetypes[i] + "-" + spacetypesnv[j], coeffs.get(c)
                        .getName());
                c++;
            }
        }
    }

    public void checkPriorMeans(double[] means)
    {

        // read in bayesian prior distribution
        // Here the user specifies their prior knowledge or expectations about
        // the coefficients, for
        // instance which ones
        // should be close in value to other ones, which ones should be close to
        // certain values,
        // etc. etc.
        // users could specify that some coefficients should be zero (wtih 100%
        // confidence) which
        // would remove them from the
        // estimation, and perhaps make specifying the coefficient list
        // unnecessary.

        // For now we set the means equal to the current values from the
        // database. Use
        // SpaceTypesI_gen methods to access the
        // current values - later, this will be wrapped into the coefficient
        // objects.

        // The dispersion parameters are returned as their logarithms, so that
        // they have a
        // log-normal distribution.
        final int[] spacetypes = {3, 5, 95};
        final int[] spacetypesnv = {3, 5};
        int n = 0;
        double value;
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_AboveStepPointAdjustment();
            assertEquals(value, means[n]);
            n++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = Math.log(SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_GyDispersionParameter());
            assertEquals(value, means[n]);
            n++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_AddTransitionConst();
            assertEquals(value, means[n]);
            n++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_BelowStepPointAdjustment();
            assertEquals(value, means[n]);
            n++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = Math.log(SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_GkDispersionParameter());
            assertEquals(value, means[n]);
            n++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = Math.log(SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_GwDispersionParameter());
            assertEquals(value, means[n]);
            n++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_DemolishTransitionConst();
            assertEquals(value, means[n]);
            n++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_DerelictTransitionConst();
            assertEquals(value, means[n]);
            n++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = Math.log(SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_IntensityDispersionParameter());
            assertEquals(value, means[n]);
            n++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_NewFromTransitionConst();
            assertEquals(value, means[n]);
            n++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = Math.log(SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_NewTypeDispersionParameter());
            assertEquals(value, means[n]);
            n++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_NoChangeTransitionConst();
            assertEquals(value, means[n]);
            n++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = Math.log(SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_NochangeDispersionParameter());
            assertEquals(value, means[n]);
            n++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = Math.log(SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_GzDispersionParameter());
            assertEquals(value, means[n]);
            n++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_RenovateDerelictTransitionConst();
            assertEquals(value, means[n]);
            n++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_RenovateTransitionConst();
            assertEquals(value, means[n]);
            n++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_StepPoint();
            assertEquals(value, means[n]);
            n++;
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            value = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                    .get_StepPointAdjustment();
            assertEquals(value, means[n]);
            n++;
        }
        final SSessionJdbc sess = SSessionJdbc.getThreadLocalSession();
        for (int i = 0; i < spacetypes.length; i++)
        {
            for (int j = 0; j < spacetypesnv.length; j++)
            {
                value = SpaceTypesI.getAlreadyCreatedSpaceTypeBySpaceTypeID(spacetypes[i])
                        .getTransitionConstantTo(sess, spacetypesnv[j]);
                assertEquals(value, means[n]);
                n++;
            }
        }
    }

    public void checkPriorVariance(double[][] variance)
    {

        // This will be user-supplied. Probably force the input format to have
        // the rows and columns
        // of this matrix
        // in the same order as the coefficients provided.

        /*
         * result[0][0] = 1.69; result[1][1] = 4.84; result[2][2] = 1E-12; result[3][3] = 0.01; result[4][4] = 0.01; result[5][5] = 0.01; result[6][6]
         * = 3469.21; result[7][7] = 1.2769; result[8][8] = 1E+99; result[9][9] = 30.5809; result[10][10] = 49; result[11][11] = 1E-12; result[12][12]
         * = 0.01; result[13][13] = 0.01; result[14][14] = 0.01; result[15][15] = 0.01; result[16][16] = 0.01; result[17][17] = 0.01; result[18][18] =
         * 2070.25; result[19][19] = 12.25; result[20][20] = 1E+99; result[21][21] = 100; result[22][22] = 100; result[23][23] = 1E+99; result[24][24]
         * = 0.01; result[25][25] = 0.01; result[26][26] = 0.01; result[27][27] = 2410.81; result[28][28] = 118.81; result[29][29] = 2905.21;
         * result[30][30] = 0.01; result[31][31] = 0.01; result[32][32] = 0.01; result[33][33] = 1E-12; result[34][34] = 1E-12; result[35][35] =
         * 1E-12; result[36][36] = 0.01; result[37][37] = 0.01; result[38][38] = 0.01; result[39][39] = 0.01; result[40][40] = 0.01; result[41][41] =
         * 0.01; result[42][42] = 686.44; result[43][43] = 3.5344; result[44][44] = 1E+99; result[45][45] = 686.44; result[46][46] = 3.5344;
         * result[47][47] = 1E+99; result[48][48] = 0.2304; result[49][49] = 0.1296; result[50][50] = 1E-12; result[51][51] = 4; result[52][52] =
         * 2.25; result[53][53] = 1E-12; result[54][54] = 74.4769; result[55][55] = 148.84; result[56][56] = 2.5281; result[57][57] = 91.7764;
         * result[58][58] = 1413.76; result[59][59] = 2401;
         */

        assertEquals(16.9, variance[0][0]);
        assertEquals(48.4, variance[1][1]);
        assertEquals(1E-12, variance[2][2]);
        assertEquals(0.1, variance[3][3]);
        assertEquals(0.1, variance[4][4]);
        assertEquals(0.1, variance[5][5]);
        assertEquals(34692.1, variance[6][6]);
        assertEquals(12.769, variance[7][7]);
        assertEquals(1E+99, variance[8][8]);
        assertEquals(305.809, variance[9][9]);
        assertEquals(490, variance[10][10]);
        assertEquals(1E-12, variance[11][11]);
        assertEquals(0.1, variance[12][12]);
        assertEquals(0.1, variance[13][13]);
        assertEquals(0.1, variance[14][14]);
        assertEquals(0.1, variance[15][15]);
        assertEquals(0.1, variance[16][16]);
        assertEquals(0.1, variance[17][17]);
        assertEquals(20702.5, variance[18][18]);
        assertEquals(122.5, variance[19][19]);
        assertEquals(1E+99, variance[20][20]);
        assertEquals(1000, variance[21][21]);
        assertEquals(1000, variance[22][22]);
        assertEquals(1E+99, variance[23][23]);
        assertEquals(0.1, variance[24][24]);
        assertEquals(0.1, variance[25][25]);
        assertEquals(0.1, variance[26][26]);
        assertEquals(24108.1, variance[27][27]);
        assertEquals(1188.1, variance[28][28]);
        assertEquals(29052.1, variance[29][29]);
        assertEquals(0.1, variance[30][30]);
        assertEquals(0.1, variance[31][31]);
        assertEquals(0.1, variance[32][32]);
        assertEquals(1E-12, variance[33][33]);
        assertEquals(1E-12, variance[34][34]);
        assertEquals(1E-12, variance[35][35]);
        assertEquals(0.1, variance[36][36]);
        assertEquals(0.1, variance[37][37]);
        assertEquals(0.1, variance[38][38]);
        assertEquals(0.1, variance[39][39]);
        assertEquals(0.1, variance[40][40]);
        assertEquals(0.1, variance[41][41]);
        assertEquals(6864.4, variance[42][42]);
        assertEquals(35.344, variance[43][43]);
        assertEquals(1E+99, variance[44][44]);
        assertEquals(6864.4, variance[45][45]);
        assertEquals(35.344, variance[46][46]);
        assertEquals(1E+99, variance[47][47]);
        assertEquals(2.304, variance[48][48]);
        assertEquals(1.296, variance[49][49]);
        assertEquals(1E-12, variance[50][50]);
        assertEquals(40, variance[51][51]);
        assertEquals(22.5, variance[52][52]);
        assertEquals(1E-12, variance[53][53]);
        assertEquals(744.769, variance[54][54]);
        assertEquals(1488.4, variance[55][55]);
        assertEquals(25.281, variance[56][56]);
        assertEquals(917.764, variance[57][57]);
        assertEquals(14137.6, variance[58][58]);
        assertEquals(24010, variance[59][59]);
    }
}
