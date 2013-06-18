package com.hbaspecto.pecas.sd;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.MatrixEntry;
import no.uib.cipr.matrix.Vector;
import no.uib.cipr.matrix.VectorEntry;

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
public class TestEasyAlternatives {
	private static LandInventory land;
	private static List<Alternative> nochangealts;
	private static List<List<Alternative>> demoderalts;
	private static List<List<Alternative>> renoalts;
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
		nochangealts = new ArrayList<Alternative>();
		demoderalts = new ArrayList<List<Alternative>>();
		renoalts = new ArrayList<List<Alternative>>();

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
			nochangealts.add(model.getAlternatives().get(1));
			model = (LogitModel) model.getAlternatives().get(0);
			final LogitModel demodermodel = (LogitModel) model.getAlternatives().get(
					0);
			demoderalts.add(demodermodel.getAlternatives());
			final LogitModel renomodel = (LogitModel) model.getAlternatives().get(1);
			renoalts.add(renomodel.getAlternatives());
			disp = model.getDispersionParameter();
			i++;
		}
	}

	@Test
	public void testGetUtility() throws NoAlternativeAvailable,
			ChoiceModelOverflowException {
		land.setToBeforeFirst();
		// Parcel 1.
		land.advanceToNext();
		double utility = ((NoChangeAlternative) nochangealts.get(0))
				.getUtility(disp);
		assertEquals(1.837621, utility, 0.00001);
		utility = ((DemolishAlternative) demoderalts.get(0).get(0))
				.getUtility(disp);
		assertEquals(-455.331, utility, 0.01);
		utility = ((DerelictAlternative) demoderalts.get(0).get(1))
				.getUtility(disp);
		assertEquals(-100.000, utility, 0.01);
		utility = ((RenovateAlternative) renoalts.get(0).get(1)).getUtility(disp);
		assertEquals(-260.714, utility, 0.01);
		// Parcel 2.
		land.advanceToNext();
		utility = ((NoChangeAlternative) nochangealts.get(1)).getUtility(disp);
		assertEquals(7.406170, utility, 0.00001);
		utility = ((DemolishAlternative) demoderalts.get(1).get(0))
				.getUtility(disp);
		assertEquals(-456.333, utility, 0.01);
		utility = ((DerelictAlternative) demoderalts.get(1).get(1))
				.getUtility(disp);
		assertEquals(-100.000, utility, 0.01);
		utility = ((RenovateAlternative) renoalts.get(1).get(1)).getUtility(disp);
		assertEquals(-256.816, utility, 0.01);
		// Parcel 3.
		land.advanceToNext();
		utility = ((NoChangeAlternative) nochangealts.get(2)).getUtility(disp);
		assertEquals(2.297026, utility, 0.00001);
		utility = ((DemolishAlternative) demoderalts.get(2).get(0))
				.getUtility(disp);
		assertEquals(-35.2756, utility, 0.001);
		utility = ((DerelictAlternative) demoderalts.get(2).get(1))
				.getUtility(disp);
		assertEquals(-100.000, utility, 0.01);
		utility = ((RenovateAlternative) renoalts.get(2).get(1)).getUtility(disp);
		assertEquals(-17.6055, utility, 0.001);
		// Parcel 4.
		land.advanceToNext();
		utility = ((NoChangeAlternative) nochangealts.get(3)).getUtility(disp);
		assertEquals(9.257712, utility, 0.00001);
		utility = ((DemolishAlternative) demoderalts.get(3).get(0))
				.getUtility(disp);
		assertEquals(-36.1109, utility, 0.001);
		utility = ((DerelictAlternative) demoderalts.get(3).get(1))
				.getUtility(disp);
		assertEquals(-100.000, utility, 0.01);
		utility = ((RenovateAlternative) renoalts.get(3).get(1)).getUtility(disp);
		assertEquals(-13.9860, utility, 0.001);
		// Parcel 5.
		land.advanceToNext();
		utility = ((NoChangeAlternative) nochangealts.get(4)).getUtility(disp);
		assertEquals(4.134643, utility, 0.00001);
		utility = ((DemolishAlternative) demoderalts.get(4).get(0))
				.getUtility(disp);
		assertEquals(-455.331, utility, 0.01);
		utility = ((DerelictAlternative) demoderalts.get(4).get(1))
				.getUtility(disp);
		assertEquals(-100.000, utility, 0.01);
		utility = ((RenovateAlternative) renoalts.get(4).get(1)).getUtility(disp);
		assertEquals(-258.417, utility, 0.01);
		// Parcel 6.
		land.advanceToNext();
		utility = ((NoChangeAlternative) nochangealts.get(5)).getUtility(disp);
		assertEquals(18.51540, utility, 0.00001);
		assertEquals(1, demoderalts.get(5).size());
		utility = ((DerelictAlternative) demoderalts.get(5).get(0))
				.getUtility(disp);
		assertEquals(-100.000, utility, 0.01);
		utility = ((RenovateAlternative) renoalts.get(5).get(1)).getUtility(disp);
		assertEquals(-4.72829, utility, 0.0001);
		// Parcel 7.
		land.advanceToNext();
		utility = ((NoChangeAlternative) nochangealts.get(6)).getUtility(disp);
		assertEquals(0.0, utility, 0.00001);
		utility = ((DemolishAlternative) demoderalts.get(6).get(0))
				.getUtility(disp);
		assertEquals(-455.331, utility, 0.01);
		utility = ((DerelictAlternative) demoderalts.get(6).get(1))
				.getUtility(disp);
		assertEquals(Double.NEGATIVE_INFINITY, utility);
		utility = ((RenovateAlternative) renoalts.get(6).get(1)).getUtility(disp);
		assertEquals(-258.692, utility, 0.01);
		// Parcel 8.
		land.advanceToNext();
		utility = ((NoChangeAlternative) nochangealts.get(7)).getUtility(disp);
		assertEquals(0.0, utility, 0.00001);
		utility = ((DemolishAlternative) demoderalts.get(7).get(0))
				.getUtility(disp);
		assertEquals(Double.NEGATIVE_INFINITY, utility);
		utility = ((DerelictAlternative) demoderalts.get(7).get(1))
				.getUtility(disp);
		assertEquals(Double.NEGATIVE_INFINITY, utility);
		utility = ((RenovateAlternative) renoalts.get(7).get(1)).getUtility(disp);
		assertEquals(Double.NEGATIVE_INFINITY, utility);
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
		// None of these options allow stuff to be built, so every single result
		// should be identically 0.
		int i = 0;
		while (land.advanceToNext()) {
			Vector expvalues = ((NoChangeAlternative) nochangealts.get(i))
					.getExpectedTargetValues(targets);
			for (final VectorEntry value : expvalues) {
				assertEquals(0.0, value.get());
			}
			if (i == 5) {
				expvalues = ((DerelictAlternative) demoderalts.get(i).get(0))
						.getExpectedTargetValues(targets);
			}
			else {
				expvalues = ((DemolishAlternative) demoderalts.get(i).get(0))
						.getExpectedTargetValues(targets);
				for (final VectorEntry value : expvalues) {
					assertEquals(0.0, value.get());
				}
				expvalues = ((DerelictAlternative) demoderalts.get(i).get(1))
						.getExpectedTargetValues(targets);
			}
			for (final VectorEntry value : expvalues) {
				assertEquals(0.0, value.get());
			}
			expvalues = ((RenovateAlternative) renoalts.get(i).get(1))
					.getExpectedTargetValues(targets);
			for (final VectorEntry value : expvalues) {
				assertEquals(0.0, value.get());
			}
			i++;
		}
	}

	@Test
	public void testGetUtilityDerivativesWRTParameters()
			throws NoAlternativeAvailable, ChoiceModelOverflowException {
		final List<Coefficient> coeffs = new ArrayList<Coefficient>();
		coeffs.add(SpaceTypeCoefficient.getRenovateTransitionConst(3));
		coeffs.add(SpaceTypeCoefficient.getRenovateDerelictConst(3));
		coeffs.add(SpaceTypeCoefficient.getDemolishTransitionConst(3));
		coeffs.add(SpaceTypeCoefficient.getDerelictTransitionConst(3));
		coeffs.add(SpaceTypeCoefficient.getRenovateTransitionConst(5));
		coeffs.add(SpaceTypeCoefficient.getRenovateDerelictConst(5));
		coeffs.add(SpaceTypeCoefficient.getDemolishTransitionConst(5));
		coeffs.add(SpaceTypeCoefficient.getDerelictTransitionConst(5));

		land.setToBeforeFirst();
		// Parcel 1.
		land.advanceToNext();
		Vector derivs = ((NoChangeAlternative) nochangealts.get(0))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((DemolishAlternative) demoderalts.get(0).get(0))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(1.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((DerelictAlternative) demoderalts.get(0).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(1.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((RenovateAlternative) renoalts.get(0).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(1.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		// Parcel 2.
		land.advanceToNext();
		derivs = ((NoChangeAlternative) nochangealts.get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((DemolishAlternative) demoderalts.get(1).get(0))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(1.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((DerelictAlternative) demoderalts.get(1).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(1.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((RenovateAlternative) renoalts.get(1).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(1.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		// Parcel 3.
		land.advanceToNext();
		derivs = ((NoChangeAlternative) nochangealts.get(2))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((DemolishAlternative) demoderalts.get(2).get(0))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(1.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((DerelictAlternative) demoderalts.get(2).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(1.0, derivs.get(7), 0.00001);
		derivs = ((RenovateAlternative) renoalts.get(2).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(1.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		// Parcel 4.
		land.advanceToNext();
		derivs = ((NoChangeAlternative) nochangealts.get(3))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((DemolishAlternative) demoderalts.get(3).get(0))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(1.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((DerelictAlternative) demoderalts.get(3).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(1.0, derivs.get(7), 0.00001);
		derivs = ((RenovateAlternative) renoalts.get(3).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(1.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		// Parcel 5.
		land.advanceToNext();
		derivs = ((NoChangeAlternative) nochangealts.get(4))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((DemolishAlternative) demoderalts.get(4).get(0))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(1.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((DerelictAlternative) demoderalts.get(4).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(1.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((RenovateAlternative) renoalts.get(4).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(1.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		// Parcel 6.
		land.advanceToNext();
		derivs = ((NoChangeAlternative) nochangealts.get(5))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((DerelictAlternative) demoderalts.get(5).get(0))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(1.0, derivs.get(7), 0.00001);
		derivs = ((RenovateAlternative) renoalts.get(5).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(1.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		// Parcel 7.
		land.advanceToNext();
		derivs = ((NoChangeAlternative) nochangealts.get(6))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((DemolishAlternative) demoderalts.get(6).get(0))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(1.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((DerelictAlternative) demoderalts.get(6).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((RenovateAlternative) renoalts.get(6).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(1.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		// Parcel 8.
		land.advanceToNext();
		derivs = ((NoChangeAlternative) nochangealts.get(7))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((DemolishAlternative) demoderalts.get(7).get(0))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((DerelictAlternative) demoderalts.get(7).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
		derivs = ((RenovateAlternative) renoalts.get(7).get(1))
				.getUtilityDerivativesWRTParameters(coeffs);
		assertEquals(0.0, derivs.get(0), 0.00001);
		assertEquals(0.0, derivs.get(1), 0.00001);
		assertEquals(0.0, derivs.get(2), 0.00001);
		assertEquals(0.0, derivs.get(3), 0.00001);
		assertEquals(0.0, derivs.get(4), 0.00001);
		assertEquals(0.0, derivs.get(5), 0.00001);
		assertEquals(0.0, derivs.get(6), 0.00001);
		assertEquals(0.0, derivs.get(7), 0.00001);
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
		coeffs.add(SpaceTypeCoefficient.getRenovateTransitionConst(3));
		coeffs.add(SpaceTypeCoefficient.getRenovateDerelictConst(3));
		coeffs.add(SpaceTypeCoefficient.getDemolishTransitionConst(3));
		coeffs.add(SpaceTypeCoefficient.getDerelictTransitionConst(3));
		coeffs.add(SpaceTypeCoefficient.getRenovateTransitionConst(5));
		coeffs.add(SpaceTypeCoefficient.getRenovateDerelictConst(5));
		coeffs.add(SpaceTypeCoefficient.getDemolishTransitionConst(5));
		coeffs.add(SpaceTypeCoefficient.getDerelictTransitionConst(5));

		land.setToBeforeFirst();
		// None of these options allow stuff to be built, so every single result
		// should be identically 0.
		int i = 0;
		while (land.advanceToNext()) {
			Matrix derivs = ((NoChangeAlternative) nochangealts.get(i))
					.getExpectedTargetDerivativesWRTParameters(targets, coeffs);
			for (final MatrixEntry deriv : derivs) {
				assertEquals(0.0, deriv.get());
			}
			if (i == 5) {
				derivs = ((DerelictAlternative) demoderalts.get(i).get(0))
						.getExpectedTargetDerivativesWRTParameters(targets, coeffs);
			}
			else {
				derivs = ((DemolishAlternative) demoderalts.get(i).get(0))
						.getExpectedTargetDerivativesWRTParameters(targets, coeffs);
				for (final MatrixEntry deriv : derivs) {
					assertEquals(0.0, deriv.get());
				}
				derivs = ((DerelictAlternative) demoderalts.get(i).get(1))
						.getExpectedTargetDerivativesWRTParameters(targets, coeffs);
			}
			for (final MatrixEntry deriv : derivs) {
				assertEquals(0.0, deriv.get());
			}
			derivs = ((RenovateAlternative) renoalts.get(i).get(1))
					.getExpectedTargetDerivativesWRTParameters(targets, coeffs);
			for (final MatrixEntry deriv : derivs) {
				assertEquals(0.0, deriv.get());
			}
			i++;
		}
	}
}
