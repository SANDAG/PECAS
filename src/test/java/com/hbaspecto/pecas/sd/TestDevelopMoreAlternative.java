package com.hbaspecto.pecas.sd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.hbaspecto.discreteChoiceModelling.Alternative;
import com.hbaspecto.discreteChoiceModelling.Coefficient;
import com.hbaspecto.discreteChoiceModelling.LogitModel;
import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.NoAlternativeAvailable;
import com.hbaspecto.pecas.land.LandInventory;
import com.hbaspecto.pecas.land.PostgreSQLLandInventory;
import com.hbaspecto.pecas.land.SimpleORMLandInventory;
import com.hbaspecto.pecas.sd.estimation.ExpectedValue;
import com.hbaspecto.pecas.sd.estimation.RedevelopmentIntoSpaceTypeTarget;
import com.hbaspecto.pecas.sd.estimation.SpaceTypeCoefficient;
import com.hbaspecto.pecas.sd.estimation.SpaceTypeIntensityTarget;
import com.hbaspecto.pecas.sd.estimation.SpaceTypeTAZTarget;
import com.pb.common.util.ResourceUtil;

@Ignore
// TODO: Fix test
public class TestDevelopMoreAlternative {
	private static LandInventory land;
	private static List<List<Alternative>> alts;
	private static double disp;

	private static String str(boolean b) {
		if (b) {
			return "TRUE";
		}
		else {
			return "FALSE";
		}
	}

	private static void setUpTestInventory(String url, String user,
			String password) throws Exception {
		final Connection conn = DriverManager.getConnection(url, user, password);
		final Statement statement = conn.createStatement();

		final int[] parcelnum = { 1, 2, 3, 4, 5, 6, 7, 8 };
		final String[] parcelid = { "1", "2", "3", "4", "5", "6", "7", "8" };
		final int[] yearbuilt = { 2000, 2000, 2000, 2000, 2000, 2000, 2000, 2000 };
		final int[] taz = { 11, 11, 12, 12, 21, 21, 22, 22 };
		final int[] spacetype = { 3, 3, 5, 5, 3, 5, 3, 95 };
		final double[] quantity = { 99000, 399000, 49500, 199500, 79200, 319200,
				33000, 0 };
		final double[] landarea = { 215496, 215496, 107748, 107748, 172397, 172397,
				71832, 71832 };
		final boolean[] derelict = { false, false, false, false, false, false,
				true, true };
		final boolean[] brownfield = { false, false, false, false, false, false,
				false, false };

		for (int i = 0; i < parcelnum.length; i++) {
			statement.execute("UPDATE parcels " + "SET parcel_id='" + parcelid[i]
					+ "', year_built=" + yearbuilt[i] + ", taz=" + taz[i]
					+ ", space_type_id=" + spacetype[i] + ", space_quantity="
					+ quantity[i] + ", land_area=" + landarea[i] + ", is_derelict="
					+ str(derelict[i]) + ", is_brownfield=" + str(brownfield[i])
					+ " WHERE pecas_parcel_num=" + parcelnum[i]);
		}

		// Spacetype constants.
		final int[] spacetypenum = { 3, 5, 95 };
		final double[] newconst = { -491, -109, -539 };
		final double[] addconst = { -589, -11.3, -1E+99 };
		final double[] renoconst = { -262, -18.8, -1E+99 };
		final double[] democonst = { -455, -35, -1E+99 };
		final double[] derelictconst = { -100, -100, -1E+99 };
		final double[] nochangeconst = { 0, 0, 0 };
		final double[] newtypedisp = { 0.02, 0.4, 0.1 };
		final double[] gydisp = { 0.02, 0.4, 0.1 };
		final double[] gzdisp = { 0.02, 0.4, 0.1 };
		final double[] gwdisp = { 0.02, 0.4, 0.1 };
		final double[] gkdisp = { 0.02, 0.4, 0.1 };
		final double[] nochangedisp = { 0.02, 0.4, 0.1 };
		final double[] intensitydisp = { 0.04, 0.5, 0.2 };
		final double[] steppoint = { 4.8, 3.6, 0.0 };
		final double[] belowstep = { 55.3, 70, 0.0 };
		final double[] abovestep = { -13, -22, 0.0 };
		final double[] stepamount = { 20, 15, 0.0 };
		final double[] minfar = { 0.5, 0.0, 0.0 };
		final double[] maxfar = { 20, 15, 0.0 };

		for (int i = 0; i < spacetypenum.length; i++) {
			statement.execute("UPDATE space_types_i " + "SET new_transition_const="
					+ newconst[i] + ", add_transition_const=" + addconst[i]
					+ ", renovate_transition_const=" + renoconst[i]
					+ ", renovate_derelict_const=" + renoconst[i]
					+ ", demolish_transition_const=" + democonst[i]
					+ ", derelict_transition_const=" + derelictconst[i]
					+ ", no_change_transition_const=" + nochangeconst[i]
					+ ", new_type_dispersion_parameter=" + newtypedisp[i]
					+ ", gy_dispersion_parameter=" + gydisp[i]
					+ ", gz_dispersion_parameter=" + gzdisp[i]
					+ ", gw_dispersion_parameter=" + gwdisp[i]
					+ ", gk_dispersion_parameter=" + gkdisp[i]
					+ ", nochange_dispersion_parameter=" + nochangedisp[i]
					+ ", intensity_dispersion_parameter=" + intensitydisp[i]
					+ ", step_point=" + steppoint[i] + ", below_step_point_adjustment="
					+ belowstep[i] + ", above_step_point_adjustment=" + abovestep[i]
					+ ", step_point_adjustment=" + stepamount[i] + ", min_intensity="
					+ minfar[i] + ", max_intensity=" + maxfar[i]
					+ " WHERE space_type_id=" + spacetypenum[i]);
		}

		final int[] spacetypenv = { 3, 5 };
		final double[][] transition = { { -86.3, 122 }, { -15.9, 95.8 },
				{ 376, 490 } };

		for (int i = 0; i < spacetypenum.length; i++) {
			for (int j = 0; j < spacetypenv.length; j++) {
				statement.execute("UPDATE transition_constants_i "
						+ "SET transition_constant=" + transition[i][j]
						+ " WHERE from_space_type_id=" + spacetypenum[i]
						+ " AND to_space_type_id=" + spacetypenv[j]);
			}
		}

		conn.close();
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		final ResourceBundle rb = ResourceUtil.getResourceBundle("sd");

		final String url = ResourceUtil.checkAndGetProperty(rb, "InputDatabase");
		final String user = ResourceUtil.checkAndGetProperty(rb,
				"InputDatabaseUser");
		final String password = ResourceUtil.checkAndGetProperty(rb,
				"InputDatabasePassword");

		setUpTestInventory(url, user, password);

		final String driver = ResourceUtil.checkAndGetProperty(rb,
				"InputJDBCDriver");
		final String schema = null;

		SimpleORMLandInventory.prepareSimpleORMSession(rb);
		final SimpleORMLandInventory sormland = new PostgreSQLLandInventory();
		sormland.setDatabaseConnectionParameter(rb, driver, url, user, password,
				schema);
		land = sormland;
		ZoningRulesI.land = land;
		land.init(2005);
		land.setToBeforeFirst();

		// Set up alternatives.
		int i = 0;
		alts = new ArrayList<List<Alternative>>();
		while (land.advanceToNext()) {
			final ZoningRulesI zoning = ZoningRulesI.getZoningRuleByZoningRulesCode(
					land.getSession(), land.getZoningRulesCode());

			final double interestRate = 0.0722;
			final double compounded = Math.pow(1 + interestRate, 30);
			final double amortizationFactor = interestRate * compounded
					/ (compounded - 1);
			ZoningRulesI.amortizationFactor = ResourceUtil.getDoubleProperty(rb,
					"AmortizationFactor", amortizationFactor);
			ZoningRulesI.servicingCostPerUnit = ResourceUtil.getDoubleProperty(rb,
					"ServicingCostPerUnit", 13.76);

			final Method getmodel = ZoningRulesI.class
					.getDeclaredMethod("getMyLogitModel");
			getmodel.setAccessible(true);
			LogitModel model = (LogitModel) getmodel.invoke(zoning);
			model = (LogitModel) model.getAlternatives().get(0);
			model = (LogitModel) model.getAlternatives().get(1);
			model = (LogitModel) model.getAlternatives().get(0);
			alts.add(model.getAlternatives());
			disp = model.getDispersionParameter();
			i++;
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testGetUtility() {
		try {
			land.setToBeforeFirst();
			// Parcel 1 - low-density commercial.
			land.advanceToNext();
			double utility;
			utility = ((DevelopMoreAlternative) alts.get(0).get(1)).getUtility(disp);
			assertEquals(-603.9736, utility, 0.001);
			// Parcel 2 - high-density commercial.
			land.advanceToNext();
			utility = ((DevelopMoreAlternative) alts.get(1).get(1)).getUtility(disp);
			assertEquals(-537.2510, utility, 0.001);
			// Parcel 3 - low-density residential.
			land.advanceToNext();
			utility = ((DevelopMoreAlternative) alts.get(2).get(1)).getUtility(disp);
			assertEquals(-13.5141, utility, 0.001);
			// Parcel 4 - high-density residential.
			land.advanceToNext();
			utility = ((DevelopMoreAlternative) alts.get(3).get(1)).getUtility(disp);
			assertEquals(5.83204, utility, 0.0001);
			// Parcel 5 - mixed-use.
			land.advanceToNext();
			utility = ((DevelopMoreAlternative) alts.get(4).get(1)).getUtility(disp);
			assertEquals(-530.9103, utility, 0.001);
			// Parcel 6 - historical - can't build.
			land.advanceToNext();
			assertEquals(1, alts.get(5).size());
			// Parcel 7 - derelict - can't build.
			land.advanceToNext();
			utility = ((DevelopMoreAlternative) alts.get(6).get(1)).getUtility(disp);
			assertEquals(Double.NEGATIVE_INFINITY, utility);
			// Parcel 8 - vacant - can't build.
			land.advanceToNext();
			utility = ((DevelopMoreAlternative) alts.get(7).get(1)).getUtility(disp);
			assertEquals(Double.NEGATIVE_INFINITY, utility);
		}
		catch (final ChoiceModelOverflowException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Overflow utility in getUtility of DevelopMore alternative");
		}
	}

	@Test
	public void testGetExpectedTargetValues() throws NoAlternativeAvailable,
			ChoiceModelOverflowException {
		final List<ExpectedValue> targets = new ArrayList<ExpectedValue>();
		targets.add(new SpaceTypeTAZTarget(11, 3));
		targets.add(new SpaceTypeTAZTarget(11, 5));
		targets.add(new SpaceTypeTAZTarget(12, 3));
		targets.add(new SpaceTypeTAZTarget(12, 5));
		targets.add(new SpaceTypeTAZTarget(21, 3));
		targets.add(new SpaceTypeTAZTarget(21, 5));
		targets.add(new SpaceTypeTAZTarget(22, 3));
		targets.add(new SpaceTypeTAZTarget(22, 5));
		targets.add(new RedevelopmentIntoSpaceTypeTarget(3));
		targets.add(new RedevelopmentIntoSpaceTypeTarget(5));
		targets.addAll(new SpaceTypeIntensityTarget(3)
				.getAssociatedExpectedValues());
		targets.addAll(new SpaceTypeIntensityTarget(5)
				.getAssociatedExpectedValues());

		land.setToBeforeFirst();
		// Parcel 1 - low-density commercial.
		land.advanceToNext();
		Vector expvalues = ((DevelopMoreAlternative) alts.get(0).get(1))
				.getExpectedTargetValues(targets);
		assertEquals(160997, expvalues.get(0), 1);
		assertEquals(0.0, expvalues.get(1), 0.00001);
		assertEquals(0.0, expvalues.get(2), 0.00001);
		assertEquals(0.0, expvalues.get(3), 0.00001);
		assertEquals(0.0, expvalues.get(4), 0.00001);
		assertEquals(0.0, expvalues.get(5), 0.00001);
		assertEquals(0.0, expvalues.get(6), 0.00001);
		assertEquals(0.0, expvalues.get(7), 0.00001);
		assertEquals(160997, expvalues.get(8), 1);
		assertEquals(0.0, expvalues.get(9), 0.00001);
		assertEquals(0.0, expvalues.get(10), 0.00001);
		assertEquals(0.0, expvalues.get(11), 0.00001);
		assertEquals(0.0, expvalues.get(12), 0.00001);
		assertEquals(0.0, expvalues.get(13), 0.00001);
		// Parcel 2 - high-density commercial.
		land.advanceToNext();
		expvalues = ((DevelopMoreAlternative) alts.get(1).get(1))
				.getExpectedTargetValues(targets);
		assertEquals(1144500, expvalues.get(0), 1);
		assertEquals(0.0, expvalues.get(1), 0.00001);
		assertEquals(0.0, expvalues.get(2), 0.00001);
		assertEquals(0.0, expvalues.get(3), 0.00001);
		assertEquals(0.0, expvalues.get(4), 0.00001);
		assertEquals(0.0, expvalues.get(5), 0.00001);
		assertEquals(0.0, expvalues.get(6), 0.00001);
		assertEquals(0.0, expvalues.get(7), 0.00001);
		assertEquals(1144500, expvalues.get(8), 1);
		assertEquals(0.0, expvalues.get(9), 0.00001);
		assertEquals(0.0, expvalues.get(10), 0.00001);
		assertEquals(0.0, expvalues.get(11), 0.00001);
		assertEquals(0.0, expvalues.get(12), 0.00001);
		assertEquals(0.0, expvalues.get(13), 0.00001);
		// Parcel 3 - low-density residential.
		land.advanceToNext();
		expvalues = ((DevelopMoreAlternative) alts.get(2).get(1))
				.getExpectedTargetValues(targets);
		assertEquals(0.0, expvalues.get(0), 0.00001);
		assertEquals(0.0, expvalues.get(1), 0.00001);
		assertEquals(0.0, expvalues.get(2), 0.00001);
		assertEquals(51675, expvalues.get(3), 1);
		assertEquals(0.0, expvalues.get(4), 0.00001);
		assertEquals(0.0, expvalues.get(5), 0.00001);
		assertEquals(0.0, expvalues.get(6), 0.00001);
		assertEquals(0.0, expvalues.get(7), 0.00001);
		assertEquals(0.0, expvalues.get(8), 0.00001);
		assertEquals(51675, expvalues.get(9), 1);
		assertEquals(0.0, expvalues.get(10), 0.00001);
		assertEquals(0.0, expvalues.get(11), 0.00001);
		assertEquals(0.0, expvalues.get(12), 0.00001);
		assertEquals(0.0, expvalues.get(13), 0.00001);
		// Parcel 4 - high-density residential.
		land.advanceToNext();
		expvalues = ((DevelopMoreAlternative) alts.get(3).get(1))
				.getExpectedTargetValues(targets);
		assertEquals(0.0, expvalues.get(0), 0.00001);
		assertEquals(0.0, expvalues.get(1), 0.00001);
		assertEquals(0.0, expvalues.get(2), 0.00001);
		assertEquals(287516, expvalues.get(3), 1);
		assertEquals(0.0, expvalues.get(4), 0.00001);
		assertEquals(0.0, expvalues.get(5), 0.00001);
		assertEquals(0.0, expvalues.get(6), 0.00001);
		assertEquals(0.0, expvalues.get(7), 0.00001);
		assertEquals(0.0, expvalues.get(8), 0.00001);
		assertEquals(287516, expvalues.get(9), 1);
		assertEquals(0.0, expvalues.get(10), 0.00001);
		assertEquals(0.0, expvalues.get(11), 0.00001);
		assertEquals(0.0, expvalues.get(12), 0.00001);
		assertEquals(0.0, expvalues.get(13), 0.00001);
		// Parcel 5 - develop commercial on mixed-use.
		land.advanceToNext();
		expvalues = ((DevelopMoreAlternative) alts.get(4).get(1))
				.getExpectedTargetValues(targets);
		assertEquals(0.0, expvalues.get(0), 0.00001);
		assertEquals(0.0, expvalues.get(1), 0.00001);
		assertEquals(0.0, expvalues.get(2), 0.00001);
		assertEquals(0.0, expvalues.get(3), 0.00001);
		assertEquals(700757, expvalues.get(4), 1);
		assertEquals(0.0, expvalues.get(5), 0.00001);
		assertEquals(0.0, expvalues.get(6), 0.00001);
		assertEquals(0.0, expvalues.get(7), 0.00001);
		assertEquals(700757, expvalues.get(8), 1);
		assertEquals(0.0, expvalues.get(9), 0.00001);
		assertEquals(0.0, expvalues.get(10), 0.00001);
		assertEquals(0.0, expvalues.get(11), 0.00001);
		assertEquals(0.0, expvalues.get(12), 0.00001);
		assertEquals(0.0, expvalues.get(13), 0.00001);
		// Parcel 6 - historical - can't build.
		land.advanceToNext();
		// Parcel 7 - derelict.
		land.advanceToNext();
		expvalues = ((DevelopMoreAlternative) alts.get(6).get(1))
				.getExpectedTargetValues(targets);
		assertEquals(0.0, expvalues.get(0), 0.00001);
		assertEquals(0.0, expvalues.get(1), 0.00001);
		assertEquals(0.0, expvalues.get(2), 0.00001);
		assertEquals(0.0, expvalues.get(3), 0.00001);
		assertEquals(0.0, expvalues.get(4), 0.00001);
		assertEquals(0.0, expvalues.get(5), 0.00001);
		assertEquals(0.0, expvalues.get(6), 0.00001);
		assertEquals(0.0, expvalues.get(7), 0.00001);
		assertEquals(0.0, expvalues.get(8), 0.00001);
		assertEquals(0.0, expvalues.get(9), 0.00001);
		assertEquals(0.0, expvalues.get(10), 0.00001);
		assertEquals(0.0, expvalues.get(11), 0.00001);
		assertEquals(0.0, expvalues.get(12), 0.00001);
		assertEquals(0.0, expvalues.get(13), 0.00001);
		// Parcel 8 - vacant.
		land.advanceToNext();
		expvalues = ((DevelopMoreAlternative) alts.get(7).get(1))
				.getExpectedTargetValues(targets);
		assertEquals(0.0, expvalues.get(0), 0.00001);
		assertEquals(0.0, expvalues.get(1), 0.00001);
		assertEquals(0.0, expvalues.get(2), 0.00001);
		assertEquals(0.0, expvalues.get(3), 0.00001);
		assertEquals(0.0, expvalues.get(4), 0.00001);
		assertEquals(0.0, expvalues.get(5), 0.00001);
		assertEquals(0.0, expvalues.get(6), 0.00001);
		assertEquals(0.0, expvalues.get(7), 0.00001);
		assertEquals(0.0, expvalues.get(8), 0.00001);
		assertEquals(0.0, expvalues.get(9), 0.00001);
		assertEquals(0.0, expvalues.get(10), 0.00001);
		assertEquals(0.0, expvalues.get(11), 0.00001);
		assertEquals(0.0, expvalues.get(12), 0.00001);
		assertEquals(0.0, expvalues.get(13), 0.00001);
	}

	@Test
	public void testGetUtilityDerivativeWRTParameters()
			throws NoAlternativeAvailable, ChoiceModelOverflowException {
		final List<Coefficient> coeffs = new ArrayList<Coefficient>();
		coeffs.add(SpaceTypeCoefficient.getStepPoint(3));
		coeffs.add(SpaceTypeCoefficient.getBelowStepPointAdj(3));
		coeffs.add(SpaceTypeCoefficient.getAboveStepPointAdj(3));
		coeffs.add(SpaceTypeCoefficient.getStepPointAmount(3));
		coeffs.add(SpaceTypeCoefficient.getIntensityDisp(3));
		coeffs.add(SpaceTypeCoefficient.getStepPoint(5));
		coeffs.add(SpaceTypeCoefficient.getBelowStepPointAdj(5));
		coeffs.add(SpaceTypeCoefficient.getAboveStepPointAdj(5));
		coeffs.add(SpaceTypeCoefficient.getStepPointAmount(5));
		coeffs.add(SpaceTypeCoefficient.getIntensityDisp(5));
		coeffs.add(SpaceTypeCoefficient.getAddTransitionConst(3));
		coeffs.add(SpaceTypeCoefficient.getAddTransitionConst(5));

		land.setToBeforeFirst();
		// Parcel 1 - low-density commercial.
		land.advanceToNext();
		Vector derivs = ((DevelopMoreAlternative) alts.get(0).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.044826, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(433.343, derivs.get(4), 0.01);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		assertEquals(0.0, derivs.get(8), 0.00001);
		assertEquals(0.0, derivs.get(9), 0.00001);
		assertEquals(1.0, derivs.get(10), 0.00001);
		assertEquals(0.0, derivs.get(11), 0.00001);
		// Parcel 2 - high-density commercial.
		land.advanceToNext();
		derivs = ((DevelopMoreAlternative) alts.get(1).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(1.771062, derivs.get(0), 0.00001);
		assertEquals(0.238425, derivs.get(1), 0.00001);
		assertEquals(0.080235, derivs.get(2), 0.00001);
		assertEquals(0.028521, derivs.get(3), 0.00001);
		assertEquals(-1378.87, derivs.get(4), 0.1);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		assertEquals(0.0, derivs.get(8), 0.00001);
		assertEquals(0.0, derivs.get(9), 0.00001);
		assertEquals(1.0, derivs.get(10), 0.00001);
		assertEquals(0.0, derivs.get(11), 0.00001);
		// Parcel 3 - low-density residential.
		land.advanceToNext();
		derivs = ((DevelopMoreAlternative) alts.get(2).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.028776, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		assertEquals(0.0, derivs.get(8), 0.00001);
		assertEquals(12.8166, derivs.get(9), 0.001);
		assertEquals(0.0, derivs.get(10), 0.00001);
		assertEquals(1.0, derivs.get(11), 0.00001);
		// Parcel 4 - high-density residential.
		land.advanceToNext();
		derivs = ((DevelopMoreAlternative) alts.get(3).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.304504, derivs.get(5), 0.00001);
		assertEquals(0.158359, derivs.get(6), 0.00001);
		assertEquals(0.001746, derivs.get(7), 0.00001);
		assertEquals(0.006215, derivs.get(8), 0.00001);
		assertEquals(-2.94444, derivs.get(9), 0.00001);
		assertEquals(0.0, derivs.get(10), 0.00001);
		assertEquals(1.0, derivs.get(11), 0.00001);
		// Parcel 5 - develop commercial on mixed-use.
		land.advanceToNext();
		derivs = ((DevelopMoreAlternative) alts.get(4).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(1.422831, derivs.get(0), 0.00001);
		assertEquals(0.213365, derivs.get(1), 0.00001);
		assertEquals(0.030522, derivs.get(2), 0.00001);
		assertEquals(0.023709, derivs.get(3), 0.00001);
		assertEquals(-1214.21, derivs.get(4), 0.1);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		assertEquals(0.0, derivs.get(8), 0.00001);
		assertEquals(0.0, derivs.get(9), 0.00001);
		assertEquals(1.0, derivs.get(10), 0.00001);
		assertEquals(0.0, derivs.get(11), 0.00001);
		// Parcel 6 - historical - can't build.
		land.advanceToNext();
		// Parcel 7 - derelict.
		land.advanceToNext();
		derivs = ((DevelopMoreAlternative) alts.get(6).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		assertEquals(0.0, derivs.get(8), 0.00001);
		assertEquals(0.0, derivs.get(9), 0.00001);
		assertEquals(0.0, derivs.get(10), 0.00001);
		assertEquals(0.0, derivs.get(11), 0.00001);
		// Parcel 8 - vacant.
		land.advanceToNext();
		derivs = ((DevelopMoreAlternative) alts.get(7).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		assertEquals(0.0, derivs.get(8), 0.00001);
		assertEquals(0.0, derivs.get(9), 0.00001);
		assertEquals(0.0, derivs.get(10), 0.00001);
		assertEquals(0.0, derivs.get(11), 0.00001);
	}

	@Test
	public void testGetExpectedTargetDerivativesWRTParameters()
			throws NoAlternativeAvailable, ChoiceModelOverflowException {
		final List<ExpectedValue> targets = new ArrayList<ExpectedValue>();
		targets.add(new SpaceTypeTAZTarget(11, 3));
		targets.add(new SpaceTypeTAZTarget(11, 5));
		targets.add(new SpaceTypeTAZTarget(12, 3));
		targets.add(new SpaceTypeTAZTarget(12, 5));
		targets.add(new SpaceTypeTAZTarget(21, 3));
		targets.add(new SpaceTypeTAZTarget(21, 5));
		targets.add(new SpaceTypeTAZTarget(22, 3));
		targets.add(new SpaceTypeTAZTarget(22, 5));
		targets.add(new RedevelopmentIntoSpaceTypeTarget(3));
		targets.add(new RedevelopmentIntoSpaceTypeTarget(5));
		targets.addAll(new SpaceTypeIntensityTarget(3)
				.getAssociatedExpectedValues());
		targets.addAll(new SpaceTypeIntensityTarget(5)
				.getAssociatedExpectedValues());

		final List<Coefficient> coeffs = new ArrayList<Coefficient>();
		coeffs.add(SpaceTypeCoefficient.getStepPoint(3));
		coeffs.add(SpaceTypeCoefficient.getBelowStepPointAdj(3));
		coeffs.add(SpaceTypeCoefficient.getAboveStepPointAdj(3));
		coeffs.add(SpaceTypeCoefficient.getStepPointAmount(3));
		coeffs.add(SpaceTypeCoefficient.getIntensityDisp(3));
		coeffs.add(SpaceTypeCoefficient.getStepPoint(5));
		coeffs.add(SpaceTypeCoefficient.getBelowStepPointAdj(5));
		coeffs.add(SpaceTypeCoefficient.getAboveStepPointAdj(5));
		coeffs.add(SpaceTypeCoefficient.getStepPointAmount(5));
		coeffs.add(SpaceTypeCoefficient.getIntensityDisp(5));

		land.setToBeforeFirst();
		// Parcel 1 - low-density commercial.
		land.advanceToNext();
		Matrix derivs = ((DevelopMoreAlternative) alts.get(0).get(1))
				.getExpectedTargetDerivativesWRTParameters(targets, coeffs);
		assertEquals(0.0, derivs.get(0, 0), 0.00001);
		assertEquals(10.7722, derivs.get(0, 1), 0.001);
		assertEquals(0.0, derivs.get(0, 2), 0.00001);
		assertEquals(0.0, derivs.get(0, 3), 0.00001);
		assertEquals(-15629, derivs.get(0, 4), 1);
		assertEquals(0.0, derivs.get(0, 5), 0.00001);
		assertEquals(0.0, derivs.get(0, 6), 0.00001);
		assertEquals(0.0, derivs.get(0, 7), 0.00001);
		assertEquals(0.0, derivs.get(0, 8), 0.00001);
		assertEquals(0.0, derivs.get(0, 9), 0.00001);
		assertEquals(0.0, derivs.get(1, 0), 0.00001);
		assertEquals(0.0, derivs.get(1, 1), 0.00001);
		assertEquals(0.0, derivs.get(1, 2), 0.00001);
		assertEquals(0.0, derivs.get(1, 3), 0.00001);
		assertEquals(0.0, derivs.get(1, 4), 0.00001);
		assertEquals(0.0, derivs.get(1, 5), 0.00001);
		assertEquals(0.0, derivs.get(1, 6), 0.00001);
		assertEquals(0.0, derivs.get(1, 7), 0.00001);
		assertEquals(0.0, derivs.get(1, 8), 0.00001);
		assertEquals(0.0, derivs.get(1, 9), 0.00001);
		assertEquals(0.0, derivs.get(2, 0), 0.00001);
		assertEquals(0.0, derivs.get(2, 1), 0.00001);
		assertEquals(0.0, derivs.get(2, 2), 0.00001);
		assertEquals(0.0, derivs.get(2, 3), 0.00001);
		assertEquals(0.0, derivs.get(2, 4), 0.00001);
		assertEquals(0.0, derivs.get(2, 5), 0.00001);
		assertEquals(0.0, derivs.get(2, 6), 0.00001);
		assertEquals(0.0, derivs.get(2, 7), 0.00001);
		assertEquals(0.0, derivs.get(2, 8), 0.00001);
		assertEquals(0.0, derivs.get(2, 9), 0.00001);
		assertEquals(0.0, derivs.get(3, 0), 0.00001);
		assertEquals(0.0, derivs.get(3, 1), 0.00001);
		assertEquals(0.0, derivs.get(3, 2), 0.00001);
		assertEquals(0.0, derivs.get(3, 3), 0.00001);
		assertEquals(0.0, derivs.get(3, 4), 0.00001);
		assertEquals(0.0, derivs.get(3, 5), 0.00001);
		assertEquals(0.0, derivs.get(3, 6), 0.00001);
		assertEquals(0.0, derivs.get(3, 7), 0.00001);
		assertEquals(0.0, derivs.get(3, 8), 0.00001);
		assertEquals(0.0, derivs.get(3, 9), 0.00001);
		assertEquals(0.0, derivs.get(4, 0), 0.00001);
		assertEquals(0.0, derivs.get(4, 1), 0.00001);
		assertEquals(0.0, derivs.get(4, 2), 0.00001);
		assertEquals(0.0, derivs.get(4, 3), 0.00001);
		assertEquals(0.0, derivs.get(4, 4), 0.00001);
		assertEquals(0.0, derivs.get(4, 5), 0.00001);
		assertEquals(0.0, derivs.get(4, 6), 0.00001);
		assertEquals(0.0, derivs.get(4, 7), 0.00001);
		assertEquals(0.0, derivs.get(4, 8), 0.00001);
		assertEquals(0.0, derivs.get(4, 9), 0.00001);
		assertEquals(0.0, derivs.get(5, 0), 0.00001);
		assertEquals(0.0, derivs.get(5, 1), 0.00001);
		assertEquals(0.0, derivs.get(5, 2), 0.00001);
		assertEquals(0.0, derivs.get(5, 3), 0.00001);
		assertEquals(0.0, derivs.get(5, 4), 0.00001);
		assertEquals(0.0, derivs.get(5, 5), 0.00001);
		assertEquals(0.0, derivs.get(5, 6), 0.00001);
		assertEquals(0.0, derivs.get(5, 7), 0.00001);
		assertEquals(0.0, derivs.get(5, 8), 0.00001);
		assertEquals(0.0, derivs.get(5, 9), 0.00001);
		assertEquals(0.0, derivs.get(6, 0), 0.00001);
		assertEquals(0.0, derivs.get(6, 1), 0.00001);
		assertEquals(0.0, derivs.get(6, 2), 0.00001);
		assertEquals(0.0, derivs.get(6, 3), 0.00001);
		assertEquals(0.0, derivs.get(6, 4), 0.00001);
		assertEquals(0.0, derivs.get(6, 5), 0.00001);
		assertEquals(0.0, derivs.get(6, 6), 0.00001);
		assertEquals(0.0, derivs.get(6, 7), 0.00001);
		assertEquals(0.0, derivs.get(6, 8), 0.00001);
		assertEquals(0.0, derivs.get(6, 9), 0.00001);
		assertEquals(0.0, derivs.get(7, 0), 0.00001);
		assertEquals(0.0, derivs.get(7, 1), 0.00001);
		assertEquals(0.0, derivs.get(7, 2), 0.00001);
		assertEquals(0.0, derivs.get(7, 3), 0.00001);
		assertEquals(0.0, derivs.get(7, 4), 0.00001);
		assertEquals(0.0, derivs.get(7, 5), 0.00001);
		assertEquals(0.0, derivs.get(7, 6), 0.00001);
		assertEquals(0.0, derivs.get(7, 7), 0.00001);
		assertEquals(0.0, derivs.get(7, 8), 0.00001);
		assertEquals(0.0, derivs.get(7, 9), 0.00001);
		assertEquals(0.0, derivs.get(8, 0), 0.00001);
		assertEquals(10.7722, derivs.get(8, 1), 0.001);
		assertEquals(0.0, derivs.get(8, 2), 0.00001);
		assertEquals(0.0, derivs.get(8, 3), 0.00001);
		assertEquals(-15629, derivs.get(8, 4), 1);
		assertEquals(0.0, derivs.get(8, 5), 0.00001);
		assertEquals(0.0, derivs.get(8, 6), 0.00001);
		assertEquals(0.0, derivs.get(8, 7), 0.00001);
		assertEquals(0.0, derivs.get(8, 8), 0.00001);
		assertEquals(0.0, derivs.get(8, 9), 0.00001);
		assertEquals(0.0, derivs.get(9, 0), 0.00001);
		assertEquals(0.0, derivs.get(9, 1), 0.00001);
		assertEquals(0.0, derivs.get(9, 2), 0.00001);
		assertEquals(0.0, derivs.get(9, 3), 0.00001);
		assertEquals(0.0, derivs.get(9, 4), 0.00001);
		assertEquals(0.0, derivs.get(9, 5), 0.00001);
		assertEquals(0.0, derivs.get(9, 6), 0.00001);
		assertEquals(0.0, derivs.get(9, 7), 0.00001);
		assertEquals(0.0, derivs.get(9, 8), 0.00001);
		assertEquals(0.0, derivs.get(9, 9), 0.00001);
		assertEquals(0.0, derivs.get(10, 0), 0.00001);
		assertEquals(0.0, derivs.get(10, 1), 0.00001);
		assertEquals(0.0, derivs.get(10, 2), 0.00001);
		assertEquals(0.0, derivs.get(10, 3), 0.00001);
		assertEquals(0.0, derivs.get(10, 4), 0.00001);
		assertEquals(0.0, derivs.get(10, 5), 0.00001);
		assertEquals(0.0, derivs.get(10, 6), 0.00001);
		assertEquals(0.0, derivs.get(10, 7), 0.00001);
		assertEquals(0.0, derivs.get(10, 8), 0.00001);
		assertEquals(0.0, derivs.get(10, 9), 0.00001);
		assertEquals(0.0, derivs.get(11, 0), 0.00001);
		assertEquals(0.0, derivs.get(11, 1), 0.00001);
		assertEquals(0.0, derivs.get(11, 2), 0.00001);
		assertEquals(0.0, derivs.get(11, 3), 0.00001);
		assertEquals(0.0, derivs.get(11, 4), 0.00001);
		assertEquals(0.0, derivs.get(11, 5), 0.00001);
		assertEquals(0.0, derivs.get(11, 6), 0.00001);
		assertEquals(0.0, derivs.get(11, 7), 0.00001);
		assertEquals(0.0, derivs.get(11, 8), 0.00001);
		assertEquals(0.0, derivs.get(11, 9), 0.00001);
		// Parcel 2 - high-density commercial.
		land.advanceToNext();
		derivs = ((DevelopMoreAlternative) alts.get(1).get(1))
				.getExpectedTargetDerivativesWRTParameters(targets, coeffs);
		assertEquals(39436, derivs.get(0, 0), 1);
		assertEquals(1086.3, derivs.get(0, 1), 0.1);
		assertEquals(2992.7, derivs.get(0, 2), 0.1);
		assertEquals(565.99, derivs.get(0, 3), 0.01);
		assertEquals(-1.07452E+7, derivs.get(0, 4), 100);
		assertEquals(0.0, derivs.get(0, 5), 0.00001);
		assertEquals(0.0, derivs.get(0, 6), 0.00001);
		assertEquals(0.0, derivs.get(0, 7), 0.00001);
		assertEquals(0.0, derivs.get(0, 8), 0.00001);
		assertEquals(0.0, derivs.get(0, 9), 0.00001);
		assertEquals(0.0, derivs.get(1, 0), 0.00001);
		assertEquals(0.0, derivs.get(1, 1), 0.00001);
		assertEquals(0.0, derivs.get(1, 2), 0.00001);
		assertEquals(0.0, derivs.get(1, 3), 0.00001);
		assertEquals(0.0, derivs.get(1, 4), 0.00001);
		assertEquals(0.0, derivs.get(1, 5), 0.00001);
		assertEquals(0.0, derivs.get(1, 6), 0.00001);
		assertEquals(0.0, derivs.get(1, 7), 0.00001);
		assertEquals(0.0, derivs.get(1, 8), 0.00001);
		assertEquals(0.0, derivs.get(1, 9), 0.00001);
		assertEquals(0.0, derivs.get(2, 0), 0.00001);
		assertEquals(0.0, derivs.get(2, 1), 0.00001);
		assertEquals(0.0, derivs.get(2, 2), 0.00001);
		assertEquals(0.0, derivs.get(2, 3), 0.00001);
		assertEquals(0.0, derivs.get(2, 4), 0.00001);
		assertEquals(0.0, derivs.get(2, 5), 0.00001);
		assertEquals(0.0, derivs.get(2, 6), 0.00001);
		assertEquals(0.0, derivs.get(2, 7), 0.00001);
		assertEquals(0.0, derivs.get(2, 8), 0.00001);
		assertEquals(0.0, derivs.get(2, 9), 0.00001);
		assertEquals(0.0, derivs.get(3, 0), 0.00001);
		assertEquals(0.0, derivs.get(3, 1), 0.00001);
		assertEquals(0.0, derivs.get(3, 2), 0.00001);
		assertEquals(0.0, derivs.get(3, 3), 0.00001);
		assertEquals(0.0, derivs.get(3, 4), 0.00001);
		assertEquals(0.0, derivs.get(3, 5), 0.00001);
		assertEquals(0.0, derivs.get(3, 6), 0.00001);
		assertEquals(0.0, derivs.get(3, 7), 0.00001);
		assertEquals(0.0, derivs.get(3, 8), 0.00001);
		assertEquals(0.0, derivs.get(3, 9), 0.00001);
		assertEquals(0.0, derivs.get(4, 0), 0.00001);
		assertEquals(0.0, derivs.get(4, 1), 0.00001);
		assertEquals(0.0, derivs.get(4, 2), 0.00001);
		assertEquals(0.0, derivs.get(4, 3), 0.00001);
		assertEquals(0.0, derivs.get(4, 4), 0.00001);
		assertEquals(0.0, derivs.get(4, 5), 0.00001);
		assertEquals(0.0, derivs.get(4, 6), 0.00001);
		assertEquals(0.0, derivs.get(4, 7), 0.00001);
		assertEquals(0.0, derivs.get(4, 8), 0.00001);
		assertEquals(0.0, derivs.get(4, 9), 0.00001);
		assertEquals(0.0, derivs.get(5, 0), 0.00001);
		assertEquals(0.0, derivs.get(5, 1), 0.00001);
		assertEquals(0.0, derivs.get(5, 2), 0.00001);
		assertEquals(0.0, derivs.get(5, 3), 0.00001);
		assertEquals(0.0, derivs.get(5, 4), 0.00001);
		assertEquals(0.0, derivs.get(5, 5), 0.00001);
		assertEquals(0.0, derivs.get(5, 6), 0.00001);
		assertEquals(0.0, derivs.get(5, 7), 0.00001);
		assertEquals(0.0, derivs.get(5, 8), 0.00001);
		assertEquals(0.0, derivs.get(5, 9), 0.00001);
		assertEquals(0.0, derivs.get(6, 0), 0.00001);
		assertEquals(0.0, derivs.get(6, 1), 0.00001);
		assertEquals(0.0, derivs.get(6, 2), 0.00001);
		assertEquals(0.0, derivs.get(6, 3), 0.00001);
		assertEquals(0.0, derivs.get(6, 4), 0.00001);
		assertEquals(0.0, derivs.get(6, 5), 0.00001);
		assertEquals(0.0, derivs.get(6, 6), 0.00001);
		assertEquals(0.0, derivs.get(6, 7), 0.00001);
		assertEquals(0.0, derivs.get(6, 8), 0.00001);
		assertEquals(0.0, derivs.get(6, 9), 0.00001);
		assertEquals(0.0, derivs.get(7, 0), 0.00001);
		assertEquals(0.0, derivs.get(7, 1), 0.00001);
		assertEquals(0.0, derivs.get(7, 2), 0.00001);
		assertEquals(0.0, derivs.get(7, 3), 0.00001);
		assertEquals(0.0, derivs.get(7, 4), 0.00001);
		assertEquals(0.0, derivs.get(7, 5), 0.00001);
		assertEquals(0.0, derivs.get(7, 6), 0.00001);
		assertEquals(0.0, derivs.get(7, 7), 0.00001);
		assertEquals(0.0, derivs.get(7, 8), 0.00001);
		assertEquals(0.0, derivs.get(7, 9), 0.00001);
		assertEquals(39436, derivs.get(8, 0), 1);
		assertEquals(1086.3, derivs.get(8, 1), 0.1);
		assertEquals(2992.8, derivs.get(8, 2), 0.1);
		assertEquals(565.99, derivs.get(8, 3), 0.01);
		assertEquals(-1.07452E+7, derivs.get(8, 4), 100);
		assertEquals(0.0, derivs.get(8, 5), 0.00001);
		assertEquals(0.0, derivs.get(8, 6), 0.00001);
		assertEquals(0.0, derivs.get(8, 7), 0.00001);
		assertEquals(0.0, derivs.get(8, 8), 0.00001);
		assertEquals(0.0, derivs.get(8, 9), 0.00001);
		assertEquals(0.0, derivs.get(9, 0), 0.00001);
		assertEquals(0.0, derivs.get(9, 1), 0.00001);
		assertEquals(0.0, derivs.get(9, 2), 0.00001);
		assertEquals(0.0, derivs.get(9, 3), 0.00001);
		assertEquals(0.0, derivs.get(9, 4), 0.00001);
		assertEquals(0.0, derivs.get(9, 5), 0.00001);
		assertEquals(0.0, derivs.get(9, 6), 0.00001);
		assertEquals(0.0, derivs.get(9, 7), 0.00001);
		assertEquals(0.0, derivs.get(9, 8), 0.00001);
		assertEquals(0.0, derivs.get(9, 9), 0.00001);
		assertEquals(0.0, derivs.get(10, 0), 0.00001);
		assertEquals(0.0, derivs.get(10, 1), 0.00001);
		assertEquals(0.0, derivs.get(10, 2), 0.00001);
		assertEquals(0.0, derivs.get(10, 3), 0.00001);
		assertEquals(0.0, derivs.get(10, 4), 0.00001);
		assertEquals(0.0, derivs.get(10, 5), 0.00001);
		assertEquals(0.0, derivs.get(10, 6), 0.00001);
		assertEquals(0.0, derivs.get(10, 7), 0.00001);
		assertEquals(0.0, derivs.get(10, 8), 0.00001);
		assertEquals(0.0, derivs.get(10, 9), 0.00001);
		assertEquals(0.0, derivs.get(11, 0), 0.00001);
		assertEquals(0.0, derivs.get(11, 1), 0.00001);
		assertEquals(0.0, derivs.get(11, 2), 0.00001);
		assertEquals(0.0, derivs.get(11, 3), 0.00001);
		assertEquals(0.0, derivs.get(11, 4), 0.00001);
		assertEquals(0.0, derivs.get(11, 5), 0.00001);
		assertEquals(0.0, derivs.get(11, 6), 0.00001);
		assertEquals(0.0, derivs.get(11, 7), 0.00001);
		assertEquals(0.0, derivs.get(11, 8), 0.00001);
		assertEquals(0.0, derivs.get(11, 9), 0.00001);
		// Parcel 3 - low-density residential.
		land.advanceToNext();
		derivs = ((DevelopMoreAlternative) alts.get(2).get(1))
				.getExpectedTargetDerivativesWRTParameters(targets, coeffs);
		assertEquals(0.0, derivs.get(0, 0), 0.00001);
		assertEquals(0.0, derivs.get(0, 1), 0.00001);
		assertEquals(0.0, derivs.get(0, 2), 0.00001);
		assertEquals(0.0, derivs.get(0, 3), 0.00001);
		assertEquals(0.0, derivs.get(0, 4), 0.00001);
		assertEquals(0.0, derivs.get(0, 5), 0.00001);
		assertEquals(0.0, derivs.get(0, 6), 0.00001);
		assertEquals(0.0, derivs.get(0, 7), 0.00001);
		assertEquals(0.0, derivs.get(0, 8), 0.00001);
		assertEquals(0.0, derivs.get(0, 9), 0.00001);
		assertEquals(0.0, derivs.get(1, 0), 0.00001);
		assertEquals(0.0, derivs.get(1, 1), 0.00001);
		assertEquals(0.0, derivs.get(1, 2), 0.00001);
		assertEquals(0.0, derivs.get(1, 3), 0.00001);
		assertEquals(0.0, derivs.get(1, 4), 0.00001);
		assertEquals(0.0, derivs.get(1, 5), 0.00001);
		assertEquals(0.0, derivs.get(1, 6), 0.00001);
		assertEquals(0.0, derivs.get(1, 7), 0.00001);
		assertEquals(0.0, derivs.get(1, 8), 0.00001);
		assertEquals(0.0, derivs.get(1, 9), 0.00001);
		assertEquals(0.0, derivs.get(2, 0), 0.00001);
		assertEquals(0.0, derivs.get(2, 1), 0.00001);
		assertEquals(0.0, derivs.get(2, 2), 0.00001);
		assertEquals(0.0, derivs.get(2, 3), 0.00001);
		assertEquals(0.0, derivs.get(2, 4), 0.00001);
		assertEquals(0.0, derivs.get(2, 5), 0.00001);
		assertEquals(0.0, derivs.get(2, 6), 0.00001);
		assertEquals(0.0, derivs.get(2, 7), 0.00001);
		assertEquals(0.0, derivs.get(2, 8), 0.00001);
		assertEquals(0.0, derivs.get(2, 9), 0.00001);
		assertEquals(0.0, derivs.get(3, 0), 0.00001);
		assertEquals(0.0, derivs.get(3, 1), 0.00001);
		assertEquals(0.0, derivs.get(3, 2), 0.00001);
		assertEquals(0.0, derivs.get(3, 3), 0.00001);
		assertEquals(0.0, derivs.get(3, 4), 0.00001);
		assertEquals(0.0, derivs.get(3, 5), 0.00001);
		assertEquals(0.44388, derivs.get(3, 6), 0.0001);
		assertEquals(0.0, derivs.get(3, 7), 0.00001);
		assertEquals(0.0, derivs.get(3, 8), 0.00001);
		assertEquals(-23.6736, derivs.get(3, 9), 0.001);
		assertEquals(0.0, derivs.get(4, 0), 0.00001);
		assertEquals(0.0, derivs.get(4, 1), 0.00001);
		assertEquals(0.0, derivs.get(4, 2), 0.00001);
		assertEquals(0.0, derivs.get(4, 3), 0.00001);
		assertEquals(0.0, derivs.get(4, 4), 0.00001);
		assertEquals(0.0, derivs.get(4, 5), 0.00001);
		assertEquals(0.0, derivs.get(4, 6), 0.00001);
		assertEquals(0.0, derivs.get(4, 7), 0.00001);
		assertEquals(0.0, derivs.get(4, 8), 0.00001);
		assertEquals(0.0, derivs.get(4, 9), 0.00001);
		assertEquals(0.0, derivs.get(5, 0), 0.00001);
		assertEquals(0.0, derivs.get(5, 1), 0.00001);
		assertEquals(0.0, derivs.get(5, 2), 0.00001);
		assertEquals(0.0, derivs.get(5, 3), 0.00001);
		assertEquals(0.0, derivs.get(5, 4), 0.00001);
		assertEquals(0.0, derivs.get(5, 5), 0.00001);
		assertEquals(0.0, derivs.get(5, 6), 0.00001);
		assertEquals(0.0, derivs.get(5, 7), 0.00001);
		assertEquals(0.0, derivs.get(5, 8), 0.00001);
		assertEquals(0.0, derivs.get(5, 9), 0.00001);
		assertEquals(0.0, derivs.get(6, 0), 0.00001);
		assertEquals(0.0, derivs.get(6, 1), 0.00001);
		assertEquals(0.0, derivs.get(6, 2), 0.00001);
		assertEquals(0.0, derivs.get(6, 3), 0.00001);
		assertEquals(0.0, derivs.get(6, 4), 0.00001);
		assertEquals(0.0, derivs.get(6, 5), 0.00001);
		assertEquals(0.0, derivs.get(6, 6), 0.00001);
		assertEquals(0.0, derivs.get(6, 7), 0.00001);
		assertEquals(0.0, derivs.get(6, 8), 0.00001);
		assertEquals(0.0, derivs.get(6, 9), 0.00001);
		assertEquals(0.0, derivs.get(7, 0), 0.00001);
		assertEquals(0.0, derivs.get(7, 1), 0.00001);
		assertEquals(0.0, derivs.get(7, 2), 0.00001);
		assertEquals(0.0, derivs.get(7, 3), 0.00001);
		assertEquals(0.0, derivs.get(7, 4), 0.00001);
		assertEquals(0.0, derivs.get(7, 5), 0.00001);
		assertEquals(0.0, derivs.get(7, 6), 0.00001);
		assertEquals(0.0, derivs.get(7, 7), 0.00001);
		assertEquals(0.0, derivs.get(7, 8), 0.00001);
		assertEquals(0.0, derivs.get(7, 9), 0.00001);
		assertEquals(0.0, derivs.get(8, 0), 0.00001);
		assertEquals(0.0, derivs.get(8, 1), 0.00001);
		assertEquals(0.0, derivs.get(8, 2), 0.00001);
		assertEquals(0.0, derivs.get(8, 3), 0.00001);
		assertEquals(0.0, derivs.get(8, 4), 0.00001);
		assertEquals(0.0, derivs.get(8, 5), 0.00001);
		assertEquals(0.0, derivs.get(8, 6), 0.00001);
		assertEquals(0.0, derivs.get(8, 7), 0.00001);
		assertEquals(0.0, derivs.get(8, 8), 0.00001);
		assertEquals(0.0, derivs.get(8, 9), 0.00001);
		assertEquals(0.0, derivs.get(9, 0), 0.00001);
		assertEquals(0.0, derivs.get(9, 1), 0.00001);
		assertEquals(0.0, derivs.get(9, 2), 0.00001);
		assertEquals(0.0, derivs.get(9, 3), 0.00001);
		assertEquals(0.0, derivs.get(9, 4), 0.00001);
		assertEquals(0.0, derivs.get(9, 5), 0.00001);
		assertEquals(0.44388, derivs.get(9, 6), 0.0001);
		assertEquals(0.0, derivs.get(9, 7), 0.00001);
		assertEquals(0.0, derivs.get(9, 8), 0.00001);
		assertEquals(-23.6736, derivs.get(9, 9), 0.001);
		assertEquals(0.0, derivs.get(10, 0), 0.00001);
		assertEquals(0.0, derivs.get(10, 1), 0.00001);
		assertEquals(0.0, derivs.get(10, 2), 0.00001);
		assertEquals(0.0, derivs.get(10, 3), 0.00001);
		assertEquals(0.0, derivs.get(10, 4), 0.00001);
		assertEquals(0.0, derivs.get(10, 5), 0.00001);
		assertEquals(0.0, derivs.get(10, 6), 0.00001);
		assertEquals(0.0, derivs.get(10, 7), 0.00001);
		assertEquals(0.0, derivs.get(10, 8), 0.00001);
		assertEquals(0.0, derivs.get(10, 9), 0.00001);
		assertEquals(0.0, derivs.get(11, 0), 0.00001);
		assertEquals(0.0, derivs.get(11, 1), 0.00001);
		assertEquals(0.0, derivs.get(11, 2), 0.00001);
		assertEquals(0.0, derivs.get(11, 3), 0.00001);
		assertEquals(0.0, derivs.get(11, 4), 0.00001);
		assertEquals(0.0, derivs.get(11, 5), 0.00001);
		assertEquals(0.0, derivs.get(11, 6), 0.00001);
		assertEquals(0.0, derivs.get(11, 7), 0.00001);
		assertEquals(0.0, derivs.get(11, 8), 0.00001);
		assertEquals(0.0, derivs.get(11, 9), 0.00001);
		// Parcel 4 - high-density residential.
		land.advanceToNext();
		derivs = ((DevelopMoreAlternative) alts.get(3).get(1))
				.getExpectedTargetDerivativesWRTParameters(targets, coeffs);
		assertEquals(0.0, derivs.get(0, 0), 0.00001);
		assertEquals(0.0, derivs.get(0, 1), 0.00001);
		assertEquals(0.0, derivs.get(0, 2), 0.00001);
		assertEquals(0.0, derivs.get(0, 3), 0.00001);
		assertEquals(0.0, derivs.get(0, 4), 0.00001);
		assertEquals(0.0, derivs.get(0, 5), 0.00001);
		assertEquals(0.0, derivs.get(0, 6), 0.00001);
		assertEquals(0.0, derivs.get(0, 7), 0.00001);
		assertEquals(0.0, derivs.get(0, 8), 0.00001);
		assertEquals(0.0, derivs.get(0, 9), 0.00001);
		assertEquals(0.0, derivs.get(1, 0), 0.00001);
		assertEquals(0.0, derivs.get(1, 1), 0.00001);
		assertEquals(0.0, derivs.get(1, 2), 0.00001);
		assertEquals(0.0, derivs.get(1, 3), 0.00001);
		assertEquals(0.0, derivs.get(1, 4), 0.00001);
		assertEquals(0.0, derivs.get(1, 5), 0.00001);
		assertEquals(0.0, derivs.get(1, 6), 0.00001);
		assertEquals(0.0, derivs.get(1, 7), 0.00001);
		assertEquals(0.0, derivs.get(1, 8), 0.00001);
		assertEquals(0.0, derivs.get(1, 9), 0.00001);
		assertEquals(0.0, derivs.get(2, 0), 0.00001);
		assertEquals(0.0, derivs.get(2, 1), 0.00001);
		assertEquals(0.0, derivs.get(2, 2), 0.00001);
		assertEquals(0.0, derivs.get(2, 3), 0.00001);
		assertEquals(0.0, derivs.get(2, 4), 0.00001);
		assertEquals(0.0, derivs.get(2, 5), 0.00001);
		assertEquals(0.0, derivs.get(2, 6), 0.00001);
		assertEquals(0.0, derivs.get(2, 7), 0.00001);
		assertEquals(0.0, derivs.get(2, 8), 0.00001);
		assertEquals(0.0, derivs.get(2, 9), 0.00001);
		assertEquals(0.0, derivs.get(3, 0), 0.00001);
		assertEquals(0.0, derivs.get(3, 1), 0.00001);
		assertEquals(0.0, derivs.get(3, 2), 0.00001);
		assertEquals(0.0, derivs.get(3, 3), 0.00001);
		assertEquals(0.0, derivs.get(3, 4), 0.00001);
		assertEquals(23935, derivs.get(3, 5), 1);
		assertEquals(1106.3, derivs.get(3, 6), 0.1);
		assertEquals(140.45, derivs.get(3, 7), 0.01);
		assertEquals(405.94, derivs.get(3, 8), 0.01);
		assertEquals(-80156, derivs.get(3, 9), 1);
		assertEquals(0.0, derivs.get(4, 0), 0.00001);
		assertEquals(0.0, derivs.get(4, 1), 0.00001);
		assertEquals(0.0, derivs.get(4, 2), 0.00001);
		assertEquals(0.0, derivs.get(4, 3), 0.00001);
		assertEquals(0.0, derivs.get(4, 4), 0.00001);
		assertEquals(0.0, derivs.get(4, 5), 0.00001);
		assertEquals(0.0, derivs.get(4, 6), 0.00001);
		assertEquals(0.0, derivs.get(4, 7), 0.00001);
		assertEquals(0.0, derivs.get(4, 8), 0.00001);
		assertEquals(0.0, derivs.get(4, 9), 0.00001);
		assertEquals(0.0, derivs.get(5, 0), 0.00001);
		assertEquals(0.0, derivs.get(5, 1), 0.00001);
		assertEquals(0.0, derivs.get(5, 2), 0.00001);
		assertEquals(0.0, derivs.get(5, 3), 0.00001);
		assertEquals(0.0, derivs.get(5, 4), 0.00001);
		assertEquals(0.0, derivs.get(5, 5), 0.00001);
		assertEquals(0.0, derivs.get(5, 6), 0.00001);
		assertEquals(0.0, derivs.get(5, 7), 0.00001);
		assertEquals(0.0, derivs.get(5, 8), 0.00001);
		assertEquals(0.0, derivs.get(5, 9), 0.00001);
		assertEquals(0.0, derivs.get(6, 0), 0.00001);
		assertEquals(0.0, derivs.get(6, 1), 0.00001);
		assertEquals(0.0, derivs.get(6, 2), 0.00001);
		assertEquals(0.0, derivs.get(6, 3), 0.00001);
		assertEquals(0.0, derivs.get(6, 4), 0.00001);
		assertEquals(0.0, derivs.get(6, 5), 0.00001);
		assertEquals(0.0, derivs.get(6, 6), 0.00001);
		assertEquals(0.0, derivs.get(6, 7), 0.00001);
		assertEquals(0.0, derivs.get(6, 8), 0.00001);
		assertEquals(0.0, derivs.get(6, 9), 0.00001);
		assertEquals(0.0, derivs.get(7, 0), 0.00001);
		assertEquals(0.0, derivs.get(7, 1), 0.00001);
		assertEquals(0.0, derivs.get(7, 2), 0.00001);
		assertEquals(0.0, derivs.get(7, 3), 0.00001);
		assertEquals(0.0, derivs.get(7, 4), 0.00001);
		assertEquals(0.0, derivs.get(7, 5), 0.00001);
		assertEquals(0.0, derivs.get(7, 6), 0.00001);
		assertEquals(0.0, derivs.get(7, 7), 0.00001);
		assertEquals(0.0, derivs.get(7, 8), 0.00001);
		assertEquals(0.0, derivs.get(7, 9), 0.00001);
		assertEquals(0.0, derivs.get(8, 0), 0.00001);
		assertEquals(0.0, derivs.get(8, 1), 0.00001);
		assertEquals(0.0, derivs.get(8, 2), 0.00001);
		assertEquals(0.0, derivs.get(8, 3), 0.00001);
		assertEquals(0.0, derivs.get(8, 4), 0.00001);
		assertEquals(0.0, derivs.get(8, 5), 0.00001);
		assertEquals(0.0, derivs.get(8, 6), 0.00001);
		assertEquals(0.0, derivs.get(8, 7), 0.00001);
		assertEquals(0.0, derivs.get(8, 8), 0.00001);
		assertEquals(0.0, derivs.get(8, 9), 0.00001);
		assertEquals(0.0, derivs.get(9, 0), 0.00001);
		assertEquals(0.0, derivs.get(9, 1), 0.00001);
		assertEquals(0.0, derivs.get(9, 2), 0.00001);
		assertEquals(0.0, derivs.get(9, 3), 0.00001);
		assertEquals(0.0, derivs.get(9, 4), 0.00001);
		assertEquals(23935, derivs.get(9, 5), 1);
		assertEquals(1106.3, derivs.get(9, 6), 0.1);
		assertEquals(140.45, derivs.get(9, 7), 0.01);
		assertEquals(405.94, derivs.get(9, 8), 0.01);
		assertEquals(-80156, derivs.get(9, 9), 1);
		assertEquals(0.0, derivs.get(10, 0), 0.00001);
		assertEquals(0.0, derivs.get(10, 1), 0.00001);
		assertEquals(0.0, derivs.get(10, 2), 0.00001);
		assertEquals(0.0, derivs.get(10, 3), 0.00001);
		assertEquals(0.0, derivs.get(10, 4), 0.00001);
		assertEquals(0.0, derivs.get(10, 5), 0.00001);
		assertEquals(0.0, derivs.get(10, 6), 0.00001);
		assertEquals(0.0, derivs.get(10, 7), 0.00001);
		assertEquals(0.0, derivs.get(10, 8), 0.00001);
		assertEquals(0.0, derivs.get(10, 9), 0.00001);
		assertEquals(0.0, derivs.get(11, 0), 0.00001);
		assertEquals(0.0, derivs.get(11, 1), 0.00001);
		assertEquals(0.0, derivs.get(11, 2), 0.00001);
		assertEquals(0.0, derivs.get(11, 3), 0.00001);
		assertEquals(0.0, derivs.get(11, 4), 0.00001);
		assertEquals(0.0, derivs.get(11, 5), 0.00001);
		assertEquals(0.0, derivs.get(11, 6), 0.00001);
		assertEquals(0.0, derivs.get(11, 7), 0.00001);
		assertEquals(0.0, derivs.get(11, 8), 0.00001);
		assertEquals(0.0, derivs.get(11, 9), 0.00001);
		// Parcel 5 - develop commercial on mixed-use.
		land.advanceToNext();
		derivs = ((DevelopMoreAlternative) alts.get(4).get(1))
				.getExpectedTargetDerivativesWRTParameters(targets, coeffs);
		assertEquals(0.0, derivs.get(0, 0), 0.00001);
		assertEquals(0.0, derivs.get(0, 1), 0.00001);
		assertEquals(0.0, derivs.get(0, 2), 0.00001);
		assertEquals(0.0, derivs.get(0, 3), 0.00001);
		assertEquals(0.0, derivs.get(0, 4), 0.00001);
		assertEquals(0.0, derivs.get(0, 5), 0.00001);
		assertEquals(0.0, derivs.get(0, 6), 0.00001);
		assertEquals(0.0, derivs.get(0, 7), 0.00001);
		assertEquals(0.0, derivs.get(0, 8), 0.00001);
		assertEquals(0.0, derivs.get(0, 9), 0.00001);
		assertEquals(0.0, derivs.get(1, 0), 0.00001);
		assertEquals(0.0, derivs.get(1, 1), 0.00001);
		assertEquals(0.0, derivs.get(1, 2), 0.00001);
		assertEquals(0.0, derivs.get(1, 3), 0.00001);
		assertEquals(0.0, derivs.get(1, 4), 0.00001);
		assertEquals(0.0, derivs.get(1, 5), 0.00001);
		assertEquals(0.0, derivs.get(1, 6), 0.00001);
		assertEquals(0.0, derivs.get(1, 7), 0.00001);
		assertEquals(0.0, derivs.get(1, 8), 0.00001);
		assertEquals(0.0, derivs.get(1, 9), 0.00001);
		assertEquals(0.0, derivs.get(2, 0), 0.00001);
		assertEquals(0.0, derivs.get(2, 1), 0.00001);
		assertEquals(0.0, derivs.get(2, 2), 0.00001);
		assertEquals(0.0, derivs.get(2, 3), 0.00001);
		assertEquals(0.0, derivs.get(2, 4), 0.00001);
		assertEquals(0.0, derivs.get(2, 5), 0.00001);
		assertEquals(0.0, derivs.get(2, 6), 0.00001);
		assertEquals(0.0, derivs.get(2, 7), 0.00001);
		assertEquals(0.0, derivs.get(2, 8), 0.00001);
		assertEquals(0.0, derivs.get(2, 9), 0.00001);
		assertEquals(0.0, derivs.get(3, 0), 0.00001);
		assertEquals(0.0, derivs.get(3, 1), 0.00001);
		assertEquals(0.0, derivs.get(3, 2), 0.00001);
		assertEquals(0.0, derivs.get(3, 3), 0.00001);
		assertEquals(0.0, derivs.get(3, 4), 0.00001);
		assertEquals(0.0, derivs.get(3, 5), 0.00001);
		assertEquals(0.0, derivs.get(3, 6), 0.00001);
		assertEquals(0.0, derivs.get(3, 7), 0.00001);
		assertEquals(0.0, derivs.get(3, 8), 0.00001);
		assertEquals(0.0, derivs.get(3, 9), 0.00001);
		assertEquals(21589, derivs.get(4, 0), 1);
		assertEquals(1064.4, derivs.get(4, 1), 0.1);
		assertEquals(524.65, derivs.get(4, 2), 0.01);
		assertEquals(330.68, derivs.get(4, 3), 0.01);
		assertEquals(274547, derivs.get(4, 4), 1);
		assertEquals(0.0, derivs.get(4, 5), 0.00001);
		assertEquals(0.0, derivs.get(4, 6), 0.00001);
		assertEquals(0.0, derivs.get(4, 7), 0.00001);
		assertEquals(0.0, derivs.get(4, 8), 0.00001);
		assertEquals(0.0, derivs.get(4, 9), 0.00001);
		assertEquals(0.0, derivs.get(5, 0), 0.00001);
		assertEquals(0.0, derivs.get(5, 1), 0.00001);
		assertEquals(0.0, derivs.get(5, 2), 0.00001);
		assertEquals(0.0, derivs.get(5, 3), 0.00001);
		assertEquals(0.0, derivs.get(5, 4), 0.00001);
		assertEquals(0.0, derivs.get(5, 5), 0.00001);
		assertEquals(0.0, derivs.get(5, 6), 0.00001);
		assertEquals(0.0, derivs.get(5, 7), 0.00001);
		assertEquals(0.0, derivs.get(5, 8), 0.00001);
		assertEquals(0.0, derivs.get(5, 9), 0.00001);
		assertEquals(0.0, derivs.get(6, 0), 0.00001);
		assertEquals(0.0, derivs.get(6, 1), 0.00001);
		assertEquals(0.0, derivs.get(6, 2), 0.00001);
		assertEquals(0.0, derivs.get(6, 3), 0.00001);
		assertEquals(0.0, derivs.get(6, 4), 0.00001);
		assertEquals(0.0, derivs.get(6, 5), 0.00001);
		assertEquals(0.0, derivs.get(6, 6), 0.00001);
		assertEquals(0.0, derivs.get(6, 7), 0.00001);
		assertEquals(0.0, derivs.get(6, 8), 0.00001);
		assertEquals(0.0, derivs.get(6, 9), 0.00001);
		assertEquals(0.0, derivs.get(7, 0), 0.00001);
		assertEquals(0.0, derivs.get(7, 1), 0.00001);
		assertEquals(0.0, derivs.get(7, 2), 0.00001);
		assertEquals(0.0, derivs.get(7, 3), 0.00001);
		assertEquals(0.0, derivs.get(7, 4), 0.00001);
		assertEquals(0.0, derivs.get(7, 5), 0.00001);
		assertEquals(0.0, derivs.get(7, 6), 0.00001);
		assertEquals(0.0, derivs.get(7, 7), 0.00001);
		assertEquals(0.0, derivs.get(7, 8), 0.00001);
		assertEquals(0.0, derivs.get(7, 9), 0.00001);
		assertEquals(21589, derivs.get(8, 0), 1);
		assertEquals(1064.4, derivs.get(8, 1), 0.1);
		assertEquals(524.65, derivs.get(8, 2), 0.01);
		assertEquals(330.68, derivs.get(8, 3), 0.01);
		assertEquals(274547, derivs.get(8, 4), 1);
		assertEquals(0.0, derivs.get(8, 5), 0.00001);
		assertEquals(0.0, derivs.get(8, 6), 0.00001);
		assertEquals(0.0, derivs.get(8, 7), 0.00001);
		assertEquals(0.0, derivs.get(8, 8), 0.00001);
		assertEquals(0.0, derivs.get(8, 9), 0.00001);
		assertEquals(0.0, derivs.get(9, 0), 0.00001);
		assertEquals(0.0, derivs.get(9, 1), 0.00001);
		assertEquals(0.0, derivs.get(9, 2), 0.00001);
		assertEquals(0.0, derivs.get(9, 3), 0.00001);
		assertEquals(0.0, derivs.get(9, 4), 0.00001);
		assertEquals(0.0, derivs.get(9, 5), 0.00001);
		assertEquals(0.0, derivs.get(9, 6), 0.00001);
		assertEquals(0.0, derivs.get(9, 7), 0.00001);
		assertEquals(0.0, derivs.get(9, 8), 0.00001);
		assertEquals(0.0, derivs.get(9, 9), 0.00001);
		assertEquals(0.0, derivs.get(10, 0), 0.00001);
		assertEquals(0.0, derivs.get(10, 1), 0.00001);
		assertEquals(0.0, derivs.get(10, 2), 0.00001);
		assertEquals(0.0, derivs.get(10, 3), 0.00001);
		assertEquals(0.0, derivs.get(10, 4), 0.00001);
		assertEquals(0.0, derivs.get(10, 5), 0.00001);
		assertEquals(0.0, derivs.get(10, 6), 0.00001);
		assertEquals(0.0, derivs.get(10, 7), 0.00001);
		assertEquals(0.0, derivs.get(10, 8), 0.00001);
		assertEquals(0.0, derivs.get(10, 9), 0.00001);
		assertEquals(0.0, derivs.get(11, 0), 0.00001);
		assertEquals(0.0, derivs.get(11, 1), 0.00001);
		assertEquals(0.0, derivs.get(11, 2), 0.00001);
		assertEquals(0.0, derivs.get(11, 3), 0.00001);
		assertEquals(0.0, derivs.get(11, 4), 0.00001);
		assertEquals(0.0, derivs.get(11, 5), 0.00001);
		assertEquals(0.0, derivs.get(11, 6), 0.00001);
		assertEquals(0.0, derivs.get(11, 7), 0.00001);
		assertEquals(0.0, derivs.get(11, 8), 0.00001);
		assertEquals(0.0, derivs.get(11, 9), 0.00001);
		// Parcel 6 - historical - can't build.
		land.advanceToNext();
		// Parcel 7 - derelict.
		land.advanceToNext();
		derivs = ((DevelopMoreAlternative) alts.get(6).get(1))
				.getExpectedTargetDerivativesWRTParameters(targets, coeffs);
		assertEquals(0.0, derivs.get(0, 0), 0.00001);
		assertEquals(0.0, derivs.get(0, 1), 0.00001);
		assertEquals(0.0, derivs.get(0, 2), 0.00001);
		assertEquals(0.0, derivs.get(0, 3), 0.00001);
		assertEquals(0.0, derivs.get(0, 4), 0.00001);
		assertEquals(0.0, derivs.get(0, 5), 0.00001);
		assertEquals(0.0, derivs.get(0, 6), 0.00001);
		assertEquals(0.0, derivs.get(0, 7), 0.00001);
		assertEquals(0.0, derivs.get(0, 8), 0.00001);
		assertEquals(0.0, derivs.get(0, 9), 0.00001);
		assertEquals(0.0, derivs.get(1, 0), 0.00001);
		assertEquals(0.0, derivs.get(1, 1), 0.00001);
		assertEquals(0.0, derivs.get(1, 2), 0.00001);
		assertEquals(0.0, derivs.get(1, 3), 0.00001);
		assertEquals(0.0, derivs.get(1, 4), 0.00001);
		assertEquals(0.0, derivs.get(1, 5), 0.00001);
		assertEquals(0.0, derivs.get(1, 6), 0.00001);
		assertEquals(0.0, derivs.get(1, 7), 0.00001);
		assertEquals(0.0, derivs.get(1, 8), 0.00001);
		assertEquals(0.0, derivs.get(1, 9), 0.00001);
		assertEquals(0.0, derivs.get(2, 0), 0.00001);
		assertEquals(0.0, derivs.get(2, 1), 0.00001);
		assertEquals(0.0, derivs.get(2, 2), 0.00001);
		assertEquals(0.0, derivs.get(2, 3), 0.00001);
		assertEquals(0.0, derivs.get(2, 4), 0.00001);
		assertEquals(0.0, derivs.get(2, 5), 0.00001);
		assertEquals(0.0, derivs.get(2, 6), 0.00001);
		assertEquals(0.0, derivs.get(2, 7), 0.00001);
		assertEquals(0.0, derivs.get(2, 8), 0.00001);
		assertEquals(0.0, derivs.get(2, 9), 0.00001);
		assertEquals(0.0, derivs.get(3, 0), 0.00001);
		assertEquals(0.0, derivs.get(3, 1), 0.00001);
		assertEquals(0.0, derivs.get(3, 2), 0.00001);
		assertEquals(0.0, derivs.get(3, 3), 0.00001);
		assertEquals(0.0, derivs.get(3, 4), 0.00001);
		assertEquals(0.0, derivs.get(3, 5), 0.00001);
		assertEquals(0.0, derivs.get(3, 6), 0.00001);
		assertEquals(0.0, derivs.get(3, 7), 0.00001);
		assertEquals(0.0, derivs.get(3, 8), 0.00001);
		assertEquals(0.0, derivs.get(3, 9), 0.00001);
		assertEquals(0.0, derivs.get(4, 0), 0.00001);
		assertEquals(0.0, derivs.get(4, 1), 0.00001);
		assertEquals(0.0, derivs.get(4, 2), 0.00001);
		assertEquals(0.0, derivs.get(4, 3), 0.00001);
		assertEquals(0.0, derivs.get(4, 4), 0.00001);
		assertEquals(0.0, derivs.get(4, 5), 0.00001);
		assertEquals(0.0, derivs.get(4, 6), 0.00001);
		assertEquals(0.0, derivs.get(4, 7), 0.00001);
		assertEquals(0.0, derivs.get(4, 8), 0.00001);
		assertEquals(0.0, derivs.get(4, 9), 0.00001);
		assertEquals(0.0, derivs.get(5, 0), 0.00001);
		assertEquals(0.0, derivs.get(5, 1), 0.00001);
		assertEquals(0.0, derivs.get(5, 2), 0.00001);
		assertEquals(0.0, derivs.get(5, 3), 0.00001);
		assertEquals(0.0, derivs.get(5, 4), 0.00001);
		assertEquals(0.0, derivs.get(5, 5), 0.00001);
		assertEquals(0.0, derivs.get(5, 6), 0.00001);
		assertEquals(0.0, derivs.get(5, 7), 0.00001);
		assertEquals(0.0, derivs.get(5, 8), 0.00001);
		assertEquals(0.0, derivs.get(5, 9), 0.00001);
		assertEquals(0.0, derivs.get(6, 0), 0.00001);
		assertEquals(0.0, derivs.get(6, 1), 0.00001);
		assertEquals(0.0, derivs.get(6, 2), 0.00001);
		assertEquals(0.0, derivs.get(6, 3), 0.00001);
		assertEquals(0.0, derivs.get(6, 4), 0.00001);
		assertEquals(0.0, derivs.get(6, 5), 0.00001);
		assertEquals(0.0, derivs.get(6, 6), 0.00001);
		assertEquals(0.0, derivs.get(6, 7), 0.00001);
		assertEquals(0.0, derivs.get(6, 8), 0.00001);
		assertEquals(0.0, derivs.get(6, 9), 0.00001);
		assertEquals(0.0, derivs.get(7, 0), 0.00001);
		assertEquals(0.0, derivs.get(7, 1), 0.00001);
		assertEquals(0.0, derivs.get(7, 2), 0.00001);
		assertEquals(0.0, derivs.get(7, 3), 0.00001);
		assertEquals(0.0, derivs.get(7, 4), 0.00001);
		assertEquals(0.0, derivs.get(7, 5), 0.00001);
		assertEquals(0.0, derivs.get(7, 6), 0.00001);
		assertEquals(0.0, derivs.get(7, 7), 0.00001);
		assertEquals(0.0, derivs.get(7, 8), 0.00001);
		assertEquals(0.0, derivs.get(7, 9), 0.00001);
		assertEquals(0.0, derivs.get(8, 0), 0.00001);
		assertEquals(0.0, derivs.get(8, 1), 0.00001);
		assertEquals(0.0, derivs.get(8, 2), 0.00001);
		assertEquals(0.0, derivs.get(8, 3), 0.00001);
		assertEquals(0.0, derivs.get(8, 4), 0.00001);
		assertEquals(0.0, derivs.get(8, 5), 0.00001);
		assertEquals(0.0, derivs.get(8, 6), 0.00001);
		assertEquals(0.0, derivs.get(8, 7), 0.00001);
		assertEquals(0.0, derivs.get(8, 8), 0.00001);
		assertEquals(0.0, derivs.get(8, 9), 0.00001);
		assertEquals(0.0, derivs.get(9, 0), 0.00001);
		assertEquals(0.0, derivs.get(9, 1), 0.00001);
		assertEquals(0.0, derivs.get(9, 2), 0.00001);
		assertEquals(0.0, derivs.get(9, 3), 0.00001);
		assertEquals(0.0, derivs.get(9, 4), 0.00001);
		assertEquals(0.0, derivs.get(9, 5), 0.00001);
		assertEquals(0.0, derivs.get(9, 6), 0.00001);
		assertEquals(0.0, derivs.get(9, 7), 0.00001);
		assertEquals(0.0, derivs.get(9, 8), 0.00001);
		assertEquals(0.0, derivs.get(9, 9), 0.00001);
		assertEquals(0.0, derivs.get(10, 0), 0.00001);
		assertEquals(0.0, derivs.get(10, 1), 0.00001);
		assertEquals(0.0, derivs.get(10, 2), 0.00001);
		assertEquals(0.0, derivs.get(10, 3), 0.00001);
		assertEquals(0.0, derivs.get(10, 4), 0.00001);
		assertEquals(0.0, derivs.get(10, 5), 0.00001);
		assertEquals(0.0, derivs.get(10, 6), 0.00001);
		assertEquals(0.0, derivs.get(10, 7), 0.00001);
		assertEquals(0.0, derivs.get(10, 8), 0.00001);
		assertEquals(0.0, derivs.get(10, 9), 0.00001);
		assertEquals(0.0, derivs.get(11, 0), 0.00001);
		assertEquals(0.0, derivs.get(11, 1), 0.00001);
		assertEquals(0.0, derivs.get(11, 2), 0.00001);
		assertEquals(0.0, derivs.get(11, 3), 0.00001);
		assertEquals(0.0, derivs.get(11, 4), 0.00001);
		assertEquals(0.0, derivs.get(11, 5), 0.00001);
		assertEquals(0.0, derivs.get(11, 6), 0.00001);
		assertEquals(0.0, derivs.get(11, 7), 0.00001);
		assertEquals(0.0, derivs.get(11, 8), 0.00001);
		assertEquals(0.0, derivs.get(11, 9), 0.00001);
		// Parcel 8 - vacant.
		land.advanceToNext();
		derivs = ((DevelopMoreAlternative) alts.get(7).get(1))
				.getExpectedTargetDerivativesWRTParameters(targets, coeffs);
		assertEquals(0.0, derivs.get(0, 0), 0.00001);
		assertEquals(0.0, derivs.get(0, 1), 0.00001);
		assertEquals(0.0, derivs.get(0, 2), 0.00001);
		assertEquals(0.0, derivs.get(0, 3), 0.00001);
		assertEquals(0.0, derivs.get(0, 4), 0.00001);
		assertEquals(0.0, derivs.get(0, 5), 0.00001);
		assertEquals(0.0, derivs.get(0, 6), 0.00001);
		assertEquals(0.0, derivs.get(0, 7), 0.00001);
		assertEquals(0.0, derivs.get(0, 8), 0.00001);
		assertEquals(0.0, derivs.get(0, 9), 0.00001);
		assertEquals(0.0, derivs.get(1, 0), 0.00001);
		assertEquals(0.0, derivs.get(1, 1), 0.00001);
		assertEquals(0.0, derivs.get(1, 2), 0.00001);
		assertEquals(0.0, derivs.get(1, 3), 0.00001);
		assertEquals(0.0, derivs.get(1, 4), 0.00001);
		assertEquals(0.0, derivs.get(1, 5), 0.00001);
		assertEquals(0.0, derivs.get(1, 6), 0.00001);
		assertEquals(0.0, derivs.get(1, 7), 0.00001);
		assertEquals(0.0, derivs.get(1, 8), 0.00001);
		assertEquals(0.0, derivs.get(1, 9), 0.00001);
		assertEquals(0.0, derivs.get(2, 0), 0.00001);
		assertEquals(0.0, derivs.get(2, 1), 0.00001);
		assertEquals(0.0, derivs.get(2, 2), 0.00001);
		assertEquals(0.0, derivs.get(2, 3), 0.00001);
		assertEquals(0.0, derivs.get(2, 4), 0.00001);
		assertEquals(0.0, derivs.get(2, 5), 0.00001);
		assertEquals(0.0, derivs.get(2, 6), 0.00001);
		assertEquals(0.0, derivs.get(2, 7), 0.00001);
		assertEquals(0.0, derivs.get(2, 8), 0.00001);
		assertEquals(0.0, derivs.get(2, 9), 0.00001);
		assertEquals(0.0, derivs.get(3, 0), 0.00001);
		assertEquals(0.0, derivs.get(3, 1), 0.00001);
		assertEquals(0.0, derivs.get(3, 2), 0.00001);
		assertEquals(0.0, derivs.get(3, 3), 0.00001);
		assertEquals(0.0, derivs.get(3, 4), 0.00001);
		assertEquals(0.0, derivs.get(3, 5), 0.00001);
		assertEquals(0.0, derivs.get(3, 6), 0.00001);
		assertEquals(0.0, derivs.get(3, 7), 0.00001);
		assertEquals(0.0, derivs.get(3, 8), 0.00001);
		assertEquals(0.0, derivs.get(3, 9), 0.00001);
		assertEquals(0.0, derivs.get(4, 0), 0.00001);
		assertEquals(0.0, derivs.get(4, 1), 0.00001);
		assertEquals(0.0, derivs.get(4, 2), 0.00001);
		assertEquals(0.0, derivs.get(4, 3), 0.00001);
		assertEquals(0.0, derivs.get(4, 4), 0.00001);
		assertEquals(0.0, derivs.get(4, 5), 0.00001);
		assertEquals(0.0, derivs.get(4, 6), 0.00001);
		assertEquals(0.0, derivs.get(4, 7), 0.00001);
		assertEquals(0.0, derivs.get(4, 8), 0.00001);
		assertEquals(0.0, derivs.get(4, 9), 0.00001);
		assertEquals(0.0, derivs.get(5, 0), 0.00001);
		assertEquals(0.0, derivs.get(5, 1), 0.00001);
		assertEquals(0.0, derivs.get(5, 2), 0.00001);
		assertEquals(0.0, derivs.get(5, 3), 0.00001);
		assertEquals(0.0, derivs.get(5, 4), 0.00001);
		assertEquals(0.0, derivs.get(5, 5), 0.00001);
		assertEquals(0.0, derivs.get(5, 6), 0.00001);
		assertEquals(0.0, derivs.get(5, 7), 0.00001);
		assertEquals(0.0, derivs.get(5, 8), 0.00001);
		assertEquals(0.0, derivs.get(5, 9), 0.00001);
		assertEquals(0.0, derivs.get(6, 0), 0.00001);
		assertEquals(0.0, derivs.get(6, 1), 0.00001);
		assertEquals(0.0, derivs.get(6, 2), 0.00001);
		assertEquals(0.0, derivs.get(6, 3), 0.00001);
		assertEquals(0.0, derivs.get(6, 4), 0.00001);
		assertEquals(0.0, derivs.get(6, 5), 0.00001);
		assertEquals(0.0, derivs.get(6, 6), 0.00001);
		assertEquals(0.0, derivs.get(6, 7), 0.00001);
		assertEquals(0.0, derivs.get(6, 8), 0.00001);
		assertEquals(0.0, derivs.get(6, 9), 0.00001);
		assertEquals(0.0, derivs.get(7, 0), 0.00001);
		assertEquals(0.0, derivs.get(7, 1), 0.00001);
		assertEquals(0.0, derivs.get(7, 2), 0.00001);
		assertEquals(0.0, derivs.get(7, 3), 0.00001);
		assertEquals(0.0, derivs.get(7, 4), 0.00001);
		assertEquals(0.0, derivs.get(7, 5), 0.00001);
		assertEquals(0.0, derivs.get(7, 6), 0.00001);
		assertEquals(0.0, derivs.get(7, 7), 0.00001);
		assertEquals(0.0, derivs.get(7, 8), 0.00001);
		assertEquals(0.0, derivs.get(7, 9), 0.00001);
		assertEquals(0.0, derivs.get(8, 0), 0.00001);
		assertEquals(0.0, derivs.get(8, 1), 0.00001);
		assertEquals(0.0, derivs.get(8, 2), 0.00001);
		assertEquals(0.0, derivs.get(8, 3), 0.00001);
		assertEquals(0.0, derivs.get(8, 4), 0.00001);
		assertEquals(0.0, derivs.get(8, 5), 0.00001);
		assertEquals(0.0, derivs.get(8, 6), 0.00001);
		assertEquals(0.0, derivs.get(8, 7), 0.00001);
		assertEquals(0.0, derivs.get(8, 8), 0.00001);
		assertEquals(0.0, derivs.get(8, 9), 0.00001);
		assertEquals(0.0, derivs.get(9, 0), 0.00001);
		assertEquals(0.0, derivs.get(9, 1), 0.00001);
		assertEquals(0.0, derivs.get(9, 2), 0.00001);
		assertEquals(0.0, derivs.get(9, 3), 0.00001);
		assertEquals(0.0, derivs.get(9, 4), 0.00001);
		assertEquals(0.0, derivs.get(9, 5), 0.00001);
		assertEquals(0.0, derivs.get(9, 6), 0.00001);
		assertEquals(0.0, derivs.get(9, 7), 0.00001);
		assertEquals(0.0, derivs.get(9, 8), 0.00001);
		assertEquals(0.0, derivs.get(9, 9), 0.00001);
		assertEquals(0.0, derivs.get(10, 0), 0.00001);
		assertEquals(0.0, derivs.get(10, 1), 0.00001);
		assertEquals(0.0, derivs.get(10, 2), 0.00001);
		assertEquals(0.0, derivs.get(10, 3), 0.00001);
		assertEquals(0.0, derivs.get(10, 4), 0.00001);
		assertEquals(0.0, derivs.get(10, 5), 0.00001);
		assertEquals(0.0, derivs.get(10, 6), 0.00001);
		assertEquals(0.0, derivs.get(10, 7), 0.00001);
		assertEquals(0.0, derivs.get(10, 8), 0.00001);
		assertEquals(0.0, derivs.get(10, 9), 0.00001);
		assertEquals(0.0, derivs.get(11, 0), 0.00001);
		assertEquals(0.0, derivs.get(11, 1), 0.00001);
		assertEquals(0.0, derivs.get(11, 2), 0.00001);
		assertEquals(0.0, derivs.get(11, 3), 0.00001);
		assertEquals(0.0, derivs.get(11, 4), 0.00001);
		assertEquals(0.0, derivs.get(11, 5), 0.00001);
		assertEquals(0.0, derivs.get(11, 6), 0.00001);
		assertEquals(0.0, derivs.get(11, 7), 0.00001);
		assertEquals(0.0, derivs.get(11, 8), 0.00001);
		assertEquals(0.0, derivs.get(11, 9), 0.00001);
	}

}
