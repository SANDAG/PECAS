package com.hbaspecto.pecas.sd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import com.hbaspecto.discreteChoiceModelling.Coefficient;
import com.hbaspecto.pecas.land.PostgreSQLLandInventory;
import com.hbaspecto.pecas.land.SimpleORMLandInventory;
import com.hbaspecto.pecas.sd.estimation.EstimationReader;
import com.hbaspecto.pecas.sd.estimation.EstimationTarget;
import com.hbaspecto.pecas.sd.estimation.RedevelopmentIntoSpaceTypeTarget;
import com.hbaspecto.pecas.sd.estimation.SpaceTypeCoefficient;
import com.hbaspecto.pecas.sd.estimation.SpaceTypeLUZTarget;
import com.hbaspecto.pecas.sd.estimation.TransitionConstant;
import com.pb.common.util.ResourceUtil;

public class BaltimoreTester
        implements EstimationReader
{

    private static String url;
    private static String user;
    private static String password;
    private static String schema;

    public static void main(String[] args) throws Exception
    {
        setUpResources();

        ZoningRulesI.baseYear = 2004;
        ZoningRulesI.currentYear = 2005;

        // StandardSDModel sd = new StandardSDModel();
        /*
         * try { TestParameterEstimation.writer = new BufferedWriter(new
         * FileWriter("coeffs.csv")); } catch(IOException e) { }
         */
        // sd.calibrateModel(new BaltimoreTester(), ZoningRulesI.baseYear,
        // ZoningRulesI.currentYear,
        // 1E-5, 50);
        // SSessionJdbc.getThreadLocalSession().commit();
        /*
         * try { TestParameterEstimation.writer.close(); } catch(IOException e)
         * { }
         */

        System.exit(0);
    }

    private static void setUpResources() throws Exception
    {
        final ResourceBundle rb = ResourceUtil.getResourceBundle("sd");

        url = ResourceUtil.checkAndGetProperty(rb, "InputDatabase");
        user = ResourceUtil.checkAndGetProperty(rb, "InputDatabaseUser");
        password = ResourceUtil.checkAndGetProperty(rb, "InputDatabasePassword");

        final String driver = ResourceUtil.checkAndGetProperty(rb, "InputJDBCDriver");
        schema = ResourceUtil.checkAndGetProperty(rb, "schema");

        setUpTestInventory();

        ZoningRulesI.ignoreErrors = ResourceUtil.getBooleanProperty(rb, "IgnoreErrors", false);

        final SimpleORMLandInventory land = new PostgreSQLLandInventory();
        land.init(2000);
        land.setDatabaseConnectionParameter(rb, driver, url, user, password, schema);
        ZoningRulesI.land = land;

        final double interestRate = 0.0722;
        final double compounded = Math.pow(1 + interestRate, 30);
        final double amortizationFactor = interestRate * compounded / (compounded - 1);
        ZoningRulesI.amortizationFactor = ResourceUtil.getDoubleProperty(rb, "AmortizationFactor",
                amortizationFactor);
        ZoningRulesI.servicingCostPerUnit = ResourceUtil.getDoubleProperty(rb,
                "ServicingCostPerUnit", 13.76);
    }

    private static void setUpTestInventory() throws Exception
    {
        final Connection conn = DriverManager.getConnection(url, user, password);
        final Statement statement = conn.createStatement();
        statement.execute("SET search_path TO " + schema);

        // Spacetype constants.
        final int[] spacetypenum = {1, 2, 3, 4, 11, 12, 13, 33, 34, 95};
        final double[] newconst = {-114.2798233, -17.5, -490.5, -502.3564194, -109, -87,
                -627.8340224, -1E+99, -1E+99, -539.4651359};
        final double[] addconst = {-1e+099, -8.945991767, -589, -571, -11.34618639, -20.14963035,
                -643.1789866, -1e+099, -1e+099, -1e+099};
        final double[] renoconst = {-1e+099, -37, -261.6943042, -286.0912792, -18.81298946,
                -15.8680044, -416.8957565, -1e+099, -1e+099, -1e+099};
        final double[] democonst = {-1e+099, -1e+099, -455, -1e+099, -35, -1e+099, -1e+099,
                -1e+099, -1e+099, -1e+099};
        final double[] derelictconst = {-1e+099, -1e+099, -1e+099, -1e+099, -1e+099, -1e+099,
                -1e+099, -1e+099, -1e+099, -1e+099};
        final double[] nochangeconst = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        final double[] newtypedisp = {0.25, 0.5, 0.02, 0.02, 0.4, 0.4, 0.02, 0.2, 0.2, 0.1};
        final double[] gydisp = {0.25, 0.5, 0.02, 0.02, 0.4, 0.4, 0.02, 0.2, 0.2, 0.1};
        final double[] gzdisp = {0.25, 0.5, 0.02, 0.02, 0.4, 0.4, 0.02, 0.2, 0.2, 0.1};
        final double[] gwdisp = {0.25, 0.5, 0.02, 0.02, 0.4, 0.4, 0.02, 0.2, 0.2, 0.1};
        final double[] gkdisp = {0.25, 0.5, 0.02, 0.02, 0.4, 0.4, 0.02, 0.2, 0.2, 0.1};
        final double[] nochangedisp = {0.25, 0.5, 0.02, 0.02, 0.4, 0.4, 0.02, 0.2, 0.2, 0.1};
        final double[] intensitydisp = {0.4, 0.6, 0.04, 0.04, 0.5, 0.5, 0.04, 0.4, 0.4, 0.2};
        final double[] steppoint = {0.55, 0.125, 5.0, 5.0, 0.15, 0.35, 5.2, 0, 0.5, 0};
        final double[] belowstep = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        final double[] abovestep = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        final double[] stepamount = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        final double[] minfar = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        final double[] maxfar = {32.714, 29.52, 31.44, 39.278, 30.992, 41.322, 27.302, 0, 0, 0};

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

        final int[] spacetypenv = {1, 2, 3, 4, 11, 12, 13, 33, 34};
        final double[][] transition = {
                {-33.31214733, 78.50849284, -5.4, -1e+099, 92.91396929, -1e+099, -1e+099, -1e+099,
                        -1e+099},
                {-1e+099, 4.813021989, -93.5962195, -136.9264961, 1.754979691, -3.196895401,
                        -1e+099, -1e+099, -1e+099},
                {-1e+099, 180.8232265, -86.28453658, -2.391616981, 121.7653459, 32.76080784, -347,
                        -1e+099, -1e+099},
                {-1e+099, 140.6265807, -90.05188198, -89.85165999, 148.9524576, -1e+099, -1e+099,
                        -1e+099, -1e+099},
                {-1e+099, 85.22874179, -15.94716408, -30.66699554, 95.75160411, 90.24569272,
                        -320.0802449, -1e+099, -1e+099},
                {-1e+099, -1e+099, -34, -57, 55, 62, -1e+099, -1e+099, -1e+099},
                {-1e+099, -1e+099, 10, 20, 176.3829396, 263.1754716, -24.19310373, -1e+099, -1e+099},
                {-1e+099, -1e+099, -1e+099, -1e+099, -1e+099, -1e+099, -1e+099, -1e+099, -1e+099},
                {-1e+099, -1e+099, -1e+099, -1e+099, -1e+099, -1e+099, -1e+099, -1e+099, -1e+099},
                {-240.350387, 486.0163422, 375.7172489, -1e+099, 489.8250199, 480.6904559,
                        97.34707047, -1e+099, -1e+099}};

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

    @Override
    public List<EstimationTarget> readTargets()
    {
        final List<EstimationTarget> targets = new ArrayList<EstimationTarget>();
        EstimationTarget t = new RedevelopmentIntoSpaceTypeTarget(2);
        t.setTargetValue(979969.125);
        targets.add(t);

        final int[] luzs = {10100, 10201, 10202, 10300, 10400, 10501, 10502, 10601, 10602, 10603,
                10701, 10702, 10703, 10801, 10802, 10901, 10902, 11000, 11101, 11102, 11103, 11104,
                11105, 11106, 11201, 11202, 11301, 11302, 11303, 11304, 11401, 11402, 11501, 11502,
                11503, 11601, 11602, 11603, 11604, 11701, 11702, 11703, 11704, 11705, 11706, 11707,
                11708, 11709, 11801, 11802, 11803, 11804, 11805, 11806, 11807, 11808, 11809, 11810,
                11811, 11812, 11813, 11901, 11902, 11903, 11904, 11905, 11906, 11907, 11908, 11909,
                11910, 12001, 12002, 12003, 12004, 12005, 12006, 12007, 12101, 12102, 12103, 12104,
                12105, 12106, 12107, 12108, 12201, 12202, 12203, 12301, 12302, 12303, 12401, 12402,
                12403, 12404, 12501, 12502, 12503, 12601, 12602, 12603, 20100, 20200, 20300, 20400,
                20501, 20502, 20503, 20600, 20700, 20800, 20900, 21001, 21002, 21100, 21201, 21202,
                21300, 21400, 21500, 21600, 21700, 30100, 30200, 30300, 30400, 30500, 30600, 30700,
                30801, 30802, 30900, 31000, 31100, 31200, 31300, 31400, 31500, 31600, 31700, 31800,
                31900, 32000, 32100, 32201, 32202, 32300, 32400, 32500, 32600, 32700, 32800, 32900,
                33000, 33100, 40100, 40200, 40301, 40302, 40400, 40500, 40600, 50100, 50200, 50301,
                50302, 50401, 50402, 50403, 50501, 50502, 50503, 50600, 50700, 60100, 60200, 60300,
                60401, 60402, 60501, 60502, 60601, 60602, 60700};
        final double[] targetvalues = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 5625, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15753, 0, 0, 0, 0, 0, 0, 0,
                0, 80821, 91369.875, 47612.125, 33626.5, 0, 15622.5, 0, 0, 0, 15759.75, 0,
                7310.125, 0, 0, 0, 452.875, 0, 0, 0, 0, 0, 0, 0, 0, 75960.75, 0, 127376.75, 0, 0,
                4461.5, 220618.375, 0, 0, 0, 16547.625, 0, 66058.875, 0, 9092, 0, 91174.875,
                236.25, 49959.75, 59222.25, 31551.625, 5518.625, 0, 75423.375, 163451.125,
                202162.75, 0, 3100, 13847, 57881.25, 0, 0, 4137.5, 0, 2650, 0, 0, 0, 0, 35401.125,
                0, 0, 0, 84000, 778.75, 18347.875, 877, 0, 0, 0, 0, 0, 0, 0, 0, 46868, 0,
                318430.375, 16478.75};

        final int numluzs = luzs.length;
        for (int i = 0; i < numluzs; i++)
        {
            t = new SpaceTypeLUZTarget(luzs[i], 2);
            t.setTargetValue(targetvalues[i]);
            targets.add(t);
        }

        return targets;
    }

    @Override
    public double[][] readTargetVariance(List<EstimationTarget> targets)
    {
        final double[][] variance = new double[targets.size()][targets.size()];
        for (int i = 0; i < targets.size(); i++)
        {
            variance[i][i] = targets.get(i).getTargetValue() / 10;
            variance[i][i] = Math.max(variance[i][i] * variance[i][i], 1);
        }

        return variance;
    }

    @Override
    public List<Coefficient> readCoeffs()
    {
        final List<Coefficient> coeffs = new ArrayList<Coefficient>();
        coeffs.add(SpaceTypeCoefficient.getAddTransitionConst(2));

        final int[] spacetypes = {1, 2, 3, 4, 11, 12, 13, 33, 34, 95};
        for (int i = 0; i < spacetypes.length; i++)
        {
            coeffs.add(SpaceTypeCoefficient.getNewFromTransitionConst(spacetypes[i]));
        }
        for (int i = 0; i < spacetypes.length; i++)
        {
            coeffs.add(TransitionConstant.getCoeff(spacetypes[i], 2));
        }
        return coeffs;
    }

    @Override
    public double[] readPriorMeans(List<Coefficient> coeffs)
    {
        final double[] means = new double[coeffs.size()];
        int i = 0;
        for (final Coefficient coeff : coeffs)
        {
            means[i] = coeff.getTransformedValue();
            i++;
        }
        return means;
    }

    @Override
    public double[][] readPriorVariance(List<Coefficient> coeffs)
    {
        final double[][] variance = new double[coeffs.size()][coeffs.size()];
        for (int i = 0; i < coeffs.size(); i++)
        {
            variance[i][i] = coeffs.get(i).getTransformedValue() / 10;
            variance[i][i] = variance[i][i] * variance[i][i];
        }

        return variance;
    }

    @Override
    public double[] readStartingValues(List<Coefficient> coeffs)
    {

        return readPriorMeans(coeffs);
    }
}
