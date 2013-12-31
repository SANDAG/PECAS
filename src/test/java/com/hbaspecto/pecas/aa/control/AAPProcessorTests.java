package com.hbaspecto.pecas.aa.control;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hbaspecto.functions.LogisticPlusLinearFunction;
import com.hbaspecto.functions.SingleParameterFunction;
import com.hbaspecto.pecas.IResource;
import com.hbaspecto.pecas.aa.activities.ProductionActivity;
import com.hbaspecto.pecas.aa.commodity.AbstractCommodity;
import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.hbaspecto.pecas.aa.commodity.CommodityZUtility;
import com.hbaspecto.pecas.aa.commodity.Exchange;
import com.hbaspecto.pecas.aa.commodity.NonTransportableExchange;
import com.hbaspecto.pecas.zones.AbstractZone;
import com.hbaspecto.pecas.zones.PECASZone;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.StringIndexedNDimensionalMatrix;

public class AAPProcessorTests {

	private static final float delta = 0.000001f;
	static PECASZone[] _allZones = null;
	static ArrayList<ProductionActivity> _productionActivities = null;
	static ArrayList<AbstractCommodity> _commodities = null;

	@BeforeClass
	public static void setup() {
		setupZone();
		setupCommoditites();
	}

	public static void setupZone() {
		_allZones = GetAllTestZones();

		_productionActivities = new ArrayList<ProductionActivity>();
		_productionActivities.add(new FakeProductionActivity("one", _allZones));
		_productionActivities.add(new FakeProductionActivity("two", _allZones));
		for (int z = 0; z < _allZones.length; z++) {
			_productionActivities.get(0).setDistribution(_allZones[z],
					z * 10 + 1);
			_productionActivities.get(1).setDistribution(_allZones[z],
					z * 10 + 2);
		}
	}

	public static void setupCommoditites() {
		_commodities = new ArrayList<AbstractCommodity>();
		_commodities.add(Commodity.createOrRetrieveCommodity("One", 'c'));
		_commodities.add(Commodity.createOrRetrieveCommodity("Two", 'p'));
		((Commodity) _commodities.get(0)).setExpectedPrice(4.5);
		((Commodity) _commodities.get(1)).setExpectedPrice(5.5);
	}

	public static void setupCommodititesS() {
		FakeCommodity.clearCommodities();
		_commodities = new ArrayList<AbstractCommodity>();
		_commodities.add(Commodity.createOrRetrieveCommodity("One", 's'));
		_commodities.add(Commodity.createOrRetrieveCommodity("Two", 's'));
		((Commodity) _commodities.get(0)).setExpectedPrice(4.5);
		((Commodity) _commodities.get(1)).setExpectedPrice(5.5);
	}

	public static void setupCommodititesN() {
		FakeCommodity.clearCommodities();
		_commodities = new ArrayList<AbstractCommodity>();
		_commodities.add(Commodity.createOrRetrieveCommodity("One", 'n'));
		_commodities.add(Commodity.createOrRetrieveCommodity("Two", 'n'));
		((Commodity) _commodities.get(0)).setExpectedPrice(4.5);
		((Commodity) _commodities.get(1)).setExpectedPrice(5.5);
	}

	@Test
	public void TestAAProcessorConstructor() {
		AAPProcessor processor = new TestAAPProcessor();
	}

	@Test
	public void TestAAProcessorWriteZonalMakeUseCoefficientsStringsInZonalMakeUse() {
		FakeResourceUtil fakeResourceUtil = (FakeResourceUtil) createFakeResourceUtil();
		TestAAPProcessor processor = new TestAAPProcessor();

		processor.writeZonalMakeUseCoefficients(fakeResourceUtil, _allZones,
				_productionActivities, _commodities);

		String expectedCommodityString = "CommodityNumber,Commodity\n0,One\n1,Two\n";
		String expectedActivityString = "ActivityNumber,Activity\n0,one\n1,two\n";

		String activityString = processor._activityWriter.toString();
		String commodityString = processor._commodityWriter.toString();

		Assert.assertEquals(expectedActivityString, activityString);
		Assert.assertEquals(expectedCommodityString, commodityString);

		fakeResourceUtil.setBoolean("aa.stringsInZonalMakeUse", true);

		processor = new TestAAPProcessor();
		processor.writeZonalMakeUseCoefficients(fakeResourceUtil, _allZones,
				_productionActivities, _commodities);

		activityString = processor._activityWriter.toString();
		commodityString = processor._commodityWriter.toString();

		Assert.assertEquals("", activityString);
		Assert.assertEquals("", commodityString);
	}

	@Test
	public void TestAAProcessorWriteZonalMakeUseCoefficientsAsciiWithStrings() {
		FakeResourceUtil fakeResourceUtil = (FakeResourceUtil) createFakeResourceUtil();
		TestAAPProcessor processor = new TestAAPProcessor();

		fakeResourceUtil.setBoolean("aa.stringsInZonalMakeUse", false);
		fakeResourceUtil.setBoolean("aa.writeAsciiZonalMakeUse", true);
		processor.writeZonalMakeUseCoefficients(fakeResourceUtil, _allZones,
				_productionActivities, _commodities);

		String zonalString = processor._zonalMakeUseWriter.toString();
		String expectedZonalString = "Activity,ZoneNumber,Commodity,MorU,Coefficient,Utility,Amount\n";
		Iterator<ProductionActivity> pIter = _productionActivities.iterator();
		for (int p = 0; p < _productionActivities.size(); p++) {
			ProductionActivity prod = pIter.next();
			for (int z = 0; z < _allZones.length; z++) {
				PECASZone zone = _allZones[z];
				Iterator<AbstractCommodity> iter = _commodities.iterator();
				for (int c = 0; c < _commodities.size(); c++) {
					Commodity com = (Commodity) iter.next();
					String line = prod.myNumber + "," + z + ","
							+ com.commodityNumber + "," + "U" + ","
							+ (.5 * (double) c) + "," + (double) c + ","
							+ ((double) z * 10 + 1 + p) * (double) c / 2 + "\n";
					expectedZonalString += line;
				}

				iter = _commodities.iterator();
				for (int c = 0; c < _commodities.size(); c++) {
					Commodity com = (Commodity) iter.next();

					String line = prod.myNumber + "," + z + ","
							+ com.commodityNumber + "," + "M" + ","
							+ (.5 * (double) c) + "," + (double) c + ","
							+ ((double) z * 10 + 1 + p) * (double) c / 2 + "\n";
					expectedZonalString += line;
				}
			}
		}

		Assert.assertEquals(expectedZonalString, zonalString);
	}

	@Test
	public void TestAAProcessorWriteZonalMakeUseCoefficientsAsciiWithoutStrings() {
		FakeResourceUtil fakeResourceUtil = (FakeResourceUtil) createFakeResourceUtil();
		TestAAPProcessor processor = new TestAAPProcessor();

		fakeResourceUtil.setBoolean("aa.stringsInZonalMakeUse", true);
		fakeResourceUtil.setBoolean("aa.writeAsciiZonalMakeUse", true);
		processor.writeZonalMakeUseCoefficients(fakeResourceUtil, _allZones,
				_productionActivities, _commodities);

		String zonalString = processor._zonalMakeUseWriter.toString();
		String expectedZonalString = "Activity,ZoneNumber,Commodity,MorU,Coefficient,Utility,Amount\n";
		Iterator<ProductionActivity> pIter = _productionActivities.iterator();
		for (int p = 0; p < _productionActivities.size(); p++) {
			ProductionActivity prod = pIter.next();
			for (int z = 0; z < _allZones.length; z++) {
				PECASZone zone = _allZones[z];
				Iterator<AbstractCommodity> iter = _commodities.iterator();
				for (int c = 0; c < _commodities.size(); c++) {
					Commodity com = (Commodity) iter.next();
					String line = prod.name + "," + z + "," + com.name + ","
							+ "U" + "," + (.5 * (double) c) + "," + (double) c
							+ "," + ((double) z * 10 + 1 + p) * (double) c / 2
							+ "\n";
					expectedZonalString += line;
				}

				iter = _commodities.iterator();
				for (int c = 0; c < _commodities.size(); c++) {
					Commodity com = (Commodity) iter.next();

					String line = prod.name + "," + z + "," + com.name + ","
							+ "M" + "," + (.5 * (double) c) + "," + (double) c
							+ "," + ((double) z * 10 + 1 + p) * (double) c / 2
							+ "\n";
					expectedZonalString += line;
				}
			}
		}

		Assert.assertEquals(expectedZonalString, zonalString);
	}

	@Test
	public void TestAAProcessorWriteZonalMakeUseCoefficientsBinary()
			throws ClassNotFoundException, IOException {
		FakeResourceUtil fakeResourceUtil = (FakeResourceUtil) createFakeResourceUtil();
		TestAAPProcessor processor = new TestAAPProcessor();

		fakeResourceUtil.setBoolean("aa.stringsInZonalMakeUse", true);
		fakeResourceUtil.setBoolean("aa.writeAsciiZonalMakeUse", false);
		fakeResourceUtil.setBoolean("aa.writeBinaryZonalMakeUse", true);
		processor.writeZonalMakeUseCoefficients(fakeResourceUtil, _allZones,
				_productionActivities, _commodities);

		String zonalString = processor._zonalMakeUseWriter.toString();
		Assert.assertEquals("", zonalString);

		ByteArrayOutputStream stream = processor._zonalMakeUseObjectWriter;

		ByteArrayInputStream inputStream = new ByteArrayInputStream(
				stream.toByteArray());
		ObjectInputStream is = new ObjectInputStream(inputStream);

		StringIndexedNDimensionalMatrix zonalMakeUseCoefficients = (StringIndexedNDimensionalMatrix) is
				.readObject();
		StringIndexedNDimensionalMatrix utilities = (StringIndexedNDimensionalMatrix) is
				.readObject();
		StringIndexedNDimensionalMatrix quantities = (StringIndexedNDimensionalMatrix) is
				.readObject();

		int[] shape = zonalMakeUseCoefficients.getShape();
		Assert.assertEquals(_productionActivities.size(), shape[0]);
		Assert.assertEquals(_allZones.length, shape[1]);
		Assert.assertEquals(_commodities.size(), shape[2]);
		Assert.assertEquals(2, shape[3]);

		shape = utilities.getShape();
		Assert.assertEquals(_productionActivities.size(), shape[0]);
		Assert.assertEquals(_allZones.length, shape[1]);
		Assert.assertEquals(_commodities.size(), shape[2]);
		Assert.assertEquals(2, shape[3]);

		shape = quantities.getShape();
		Assert.assertEquals(_productionActivities.size(), shape[0]);
		Assert.assertEquals(_allZones.length, shape[1]);
		Assert.assertEquals(_commodities.size(), shape[2]);
		Assert.assertEquals(2, shape[3]);

		final String[] indices = new String[4];

		Iterator<ProductionActivity> pIter = _productionActivities.iterator();
		for (int p = 0; p < _productionActivities.size(); p++) {
			ProductionActivity prod = pIter.next();
			indices[0] = prod.name;

			for (int z = 0; z < _allZones.length; z++) {
				PECASZone zone = _allZones[z];
				indices[1] = Integer.toString(zone.getZoneUserNumber());
				Iterator<AbstractCommodity> iter = _commodities.iterator();
				for (int c = 0; c < _commodities.size(); c++) {
					Commodity com = (Commodity) iter.next();
					indices[2] = com.getName();
					indices[3] = "U";

					float consumption = (float) (.5 * (float) c);
					Assert.assertEquals(consumption,
							zonalMakeUseCoefficients.getValue(indices), delta);

					float buying = (float) c;
					Assert.assertEquals(buying, utilities.getValue(indices),
							delta);

					float quantity = ((float) z * 10 + 1 + p) * (float) c / 2;
					Assert.assertEquals(quantity, quantities.getValue(indices),
							delta);

					indices[3] = "M";

					consumption = (float) (.5 * (float) c);
					Assert.assertEquals(consumption,
							zonalMakeUseCoefficients.getValue(indices), delta);

					buying = (float) c;
					Assert.assertEquals(buying, utilities.getValue(indices),
							delta);

					quantity = ((float) z * 10 + 1 + p) * (float) c / 2;
					Assert.assertEquals(quantity, quantities.getValue(indices),
							delta);
				}

			}
		}
	}

	@Test
	public void TestAAProcessorWriteZonalMakeUseCoefficientsAsciiAggregate() {
		FakeResourceUtil fakeResourceUtil = (FakeResourceUtil) createFakeResourceUtil();
		TestAAPProcessor processor = new TestAAPProcessor();

		fakeResourceUtil.setBoolean("aa.stringsInZonalMakeUse", true);
		fakeResourceUtil.setBoolean("aa.writeAsciiZonalMakeUse", true);
		processor.writeZonalMakeUseCoefficients(fakeResourceUtil, _allZones,
				_productionActivities, _commodities);

		String aggregateString = processor._aggregateMakeUseWriter.toString();
		String expectedZonalString = "Activity,Commodity,MorU,Coefficient,StdDev,Amount\n"
				+ "one,Two,M,0.5,0.0,52.5\n"
				+ "one,Two,U,0.5,0.0,52.5\n"
				+ "two,Two,M,0.5,0.0,55.0\n" + "two,Two,U,0.5,0.0,55.0\n";

		Assert.assertEquals(expectedZonalString, aggregateString);
	}

	private static PECASZone[] GetAllTestZones() {
		int nZones = 5;
		PECASZone[] zones = new PECASZone[nZones];
		AbstractZone.createTazArray(nZones);
		for (int i = 0; i < nZones; i++) {
			zones[i] = PECASZone.createTaz(i, i, Integer.toString(i), false);
		}

		return zones;
	}

	private IResource createFakeResourceUtil() {
		return new FakeResourceUtil();
	}

	@Test
	public void TestAAProcessorSetUpExchangesAndZUtilities() {
		clearExchanges();
		TestAAPProcessor processor = new TestAAPProcessor();
		FakeResourceUtil fakeResourceUtil = (FakeResourceUtil) createFakeResourceUtil();
		processor.zones = _allZones;
		processor.setUseExchange(0);
		processor.callSetUpExchangesAndZUtilities(fakeResourceUtil);
		String[] monitorExchanges = { "true", "true", "false", "false" };
		String[] specifiedExchanges = { "true", "false", "true", "false" };

		float[] zoneNumbers = { 0f, 1f, 0f, 1f, 0f };
		String[] commodities = { "One", "One", "Two", "Two" };

		Exchange[] exchanges = {
				((Commodity) _commodities.get(0)).getExchange(0),
				((Commodity) _commodities.get(0)).getExchange(1),
				((Commodity) _commodities.get(1)).getExchange(0),
				((Commodity) _commodities.get(1)).getExchange(1) };
		for (float x = 1; x < 5; x++) {
			int i = (int) x - 1;
			Exchange exchange = exchanges[i];
			Assert.assertEquals(Boolean.parseBoolean(monitorExchanges[i]),
					exchange.monitor);
			Assert.assertEquals(x * 1.1, exchange.getPrice(), delta);
			Assert.assertEquals(x * 1.2, exchange.getBuyingSizeTerm(), delta);
			Assert.assertEquals(x * 1.3, exchange.getSellingSizeTerm(), delta);
			FakeLogisticPlusLinearFunction fInput = new FakeLogisticPlusLinearFunction(
					(x * 1.8), (x * 1.9), (x * 2.0), (x * 2.1), (x * 2.2));
			FakeLogisticPlusLinearFunction importFunction = (FakeLogisticPlusLinearFunction) exchange
					.getImportFunction();
			Assert.assertEquals(fInput.getY0(), importFunction.getY0(), delta);
			Assert.assertEquals(fInput.getX0(), importFunction.getX0(), delta);
			Assert.assertEquals(fInput.getDelta(), importFunction.getDelta(),
					delta);
			Assert.assertEquals(fInput.getLambda(), importFunction.getLambda(),
					delta);
			Assert.assertEquals(fInput.getSlope(), importFunction.getSlope(),
					delta);

			FakeLogisticPlusLinearFunction fOutput = new FakeLogisticPlusLinearFunction(
					(x * 2.3), (x * 2.4), (x * 2.5), (x * 2.6), (x * 2.7));
			FakeLogisticPlusLinearFunction exportFunction = (FakeLogisticPlusLinearFunction) exchange
					.getExportFunction();
			Assert.assertEquals(fOutput.getY0(), exportFunction.getY0(), delta);
			Assert.assertEquals(fOutput.getX0(), exportFunction.getX0(), delta);
			Assert.assertEquals(fOutput.getDelta(), exportFunction.getDelta(),
					delta);
			Assert.assertEquals(fOutput.getLambda(),
					exportFunction.getLambda(), delta);
			Assert.assertEquals(fOutput.getSlope(), exportFunction.getSlope(),
					delta);

			Assert.assertEquals(_commodities.get(i / 2), exchange.myCommodity);

			FakeExchange fe = (FakeExchange) exchange;
			CommodityZUtility[] buyFlow = fe.GetBuyingFlow();
			CommodityZUtility[] sellFlow = fe.GetSellingFlow();

			for (int j = 0; j < 4; j++) {
				if (i < 2) {
					// One buying, all selling
					Assert.assertNotNull(sellFlow[j]);
					if (i == j)
						Assert.assertNotNull(buyFlow[j]);
					else
						Assert.assertNull(buyFlow[j]);
				} else {
					// One selling, all buying
					Assert.assertNotNull(buyFlow[j]);
					if (i % 2 == j)
						Assert.assertNotNull(sellFlow[j]);
					else
						Assert.assertNull(sellFlow[j]);
				}
			}

		}
	}

	@Test
	public void TestAAProcessorSetUpExchangesAndZUtilitiesSecondExchange() {
		clearExchanges();
		TestAAPProcessor processor = new TestAAPProcessor();
		FakeResourceUtil fakeResourceUtil = (FakeResourceUtil) createFakeResourceUtil();
		processor.zones = _allZones;
		processor.setUseExchange(1);
		processor.callSetUpExchangesAndZUtilities(fakeResourceUtil);
		String[] monitorExchanges = { "true", "true", "false", "false" };
		String[] specifiedExchanges = { "true", "false", "true", "false" };

		float[] zoneNumbers = { 0f, 1f, 0f, 1f, 0f };
		String[] commodities = { "One", "One", "Two", "Two", "One" };

		Exchange[] exchanges = {
				((Commodity) _commodities.get(0)).getExchange(0),
				((Commodity) _commodities.get(0)).getExchange(1),
				((Commodity) _commodities.get(1)).getExchange(0),
				((Commodity) _commodities.get(1)).getExchange(1) };
		for (float x = 1; x < 5; x++) {
			int i = (int) x - 1;
			Exchange exchange = exchanges[i];
			Assert.assertEquals(x * 5, exchange.getPrice(), delta);
		}

	}

	private void clearExchanges() {
		for (Iterator i = _commodities.iterator(); i.hasNext();) {
			Commodity c = (Commodity) i.next();
			c.getAllExchanges().clear();
		}

	}

	@Test(expected = RuntimeException.class)
	public void TestAAProcessorSetUpExchangesAndZUtilitiesBadCommodity() {
		clearExchanges();
		TestAAPProcessor processor = new TestAAPProcessor();
		FakeResourceUtil fakeResourceUtil = (FakeResourceUtil) createFakeResourceUtil();
		processor.zones = _allZones;
		processor.setUseExchange(2);
		processor.callSetUpExchangesAndZUtilities(fakeResourceUtil);

	}

	@Test(expected = RuntimeException.class)
	public void TestAAProcessorSetUpExchangesAndZUtilitiesBadZone() {
		clearExchanges();
		TestAAPProcessor processor = new TestAAPProcessor();
		FakeResourceUtil fakeResourceUtil = (FakeResourceUtil) createFakeResourceUtil();
		processor.zones = _allZones;
		processor.setUseExchange(3);
		processor.callSetUpExchangesAndZUtilities(fakeResourceUtil);

	}

	@Test
	public void TestAAProcessorSetUpExchangesAndZUtilitiesNoPrice() {
		clearExchanges();
		TestAAPProcessor processor = new TestAAPProcessor();
		FakeResourceUtil fakeResourceUtil = (FakeResourceUtil) createFakeResourceUtil();
		processor.zones = _allZones;
		processor.setUseExchange(4);
		processor.callSetUpExchangesAndZUtilities(fakeResourceUtil);
		String[] monitorExchanges = { "true", "true", "false", "false" };
		String[] specifiedExchanges = { "true", "false", "true", "false" };

		float[] zoneNumbers = { 0f, 1f, 0f, 1f, 0f };
		String[] commodities = { "One", "One", "Two", "Two", "One" };

		Exchange[] exchanges = {
				((Commodity) _commodities.get(0)).getExchange(0),
				((Commodity) _commodities.get(0)).getExchange(1),
				((Commodity) _commodities.get(1)).getExchange(0),
				((Commodity) _commodities.get(1)).getExchange(1) };
		for (float x = 1; x < 5; x++) {
			int i = (int) x - 1;
			Exchange exchange = exchanges[i];
			Assert.assertEquals(Boolean.parseBoolean(monitorExchanges[i]),
					exchange.monitor);
			Assert.assertEquals(
					((Commodity) _commodities.get(i / 2)).getExpectedPrice(),
					exchange.getPrice(), delta);
			Assert.assertEquals(x * 1.2, exchange.getBuyingSizeTerm(), delta);
			Assert.assertEquals(x * 1.3, exchange.getSellingSizeTerm(), delta);
			FakeLogisticPlusLinearFunction fInput = new FakeLogisticPlusLinearFunction(
					(x * 1.8), (x * 1.9), (x * 2.0), (x * 2.1), (x * 2.2));
			FakeLogisticPlusLinearFunction importFunction = (FakeLogisticPlusLinearFunction) exchange
					.getImportFunction();
			Assert.assertEquals(fInput.getY0(), importFunction.getY0(), delta);
			Assert.assertEquals(fInput.getX0(), importFunction.getX0(), delta);
			Assert.assertEquals(fInput.getDelta(), importFunction.getDelta(),
					delta);
			Assert.assertEquals(fInput.getLambda(), importFunction.getLambda(),
					delta);
			Assert.assertEquals(fInput.getSlope(), importFunction.getSlope(),
					delta);

			FakeLogisticPlusLinearFunction fOutput = new FakeLogisticPlusLinearFunction(
					(x * 2.3), (x * 2.4), (x * 2.5), (x * 2.6), (x * 2.7));
			FakeLogisticPlusLinearFunction exportFunction = (FakeLogisticPlusLinearFunction) exchange
					.getExportFunction();
			Assert.assertEquals(fOutput.getY0(), exportFunction.getY0(), delta);
			Assert.assertEquals(fOutput.getX0(), exportFunction.getX0(), delta);
			Assert.assertEquals(fOutput.getDelta(), exportFunction.getDelta(),
					delta);
			Assert.assertEquals(fOutput.getLambda(),
					exportFunction.getLambda(), delta);
			Assert.assertEquals(fOutput.getSlope(), exportFunction.getSlope(),
					delta);

			Assert.assertEquals(_commodities.get(i / 2), exchange.myCommodity);

		}
	}

	@Test
	public void TestAAProcessorSetUpExchangesAndZUtilitiesNoMonitor() {
		clearExchanges();
		TestAAPProcessor processor = new TestAAPProcessor();
		FakeResourceUtil fakeResourceUtil = (FakeResourceUtil) createFakeResourceUtil();
		processor.zones = _allZones;
		processor.setUseExchange(5);
		processor.callSetUpExchangesAndZUtilities(fakeResourceUtil);
		String[] monitorExchanges = { "true", "true", "false", "false" };

		float[] zoneNumbers = { 0f, 1f, 0f, 1f, 0f };
		String[] commodities = { "One", "One", "Two", "Two", "One" };

		Exchange[] exchanges = {
				((Commodity) _commodities.get(0)).getExchange(0),
				((Commodity) _commodities.get(0)).getExchange(1),
				((Commodity) _commodities.get(1)).getExchange(0),
				((Commodity) _commodities.get(1)).getExchange(1) };
		for (float x = 1; x < 5; x++) {
			int i = (int) x - 1;
			Exchange exchange = exchanges[i];
			Assert.assertEquals(false, exchange.monitor);
			Assert.assertEquals(x * 1.1, exchange.getPrice(), delta);
			Assert.assertEquals(x * 1.2, exchange.getBuyingSizeTerm(), delta);
			Assert.assertEquals(x * 1.3, exchange.getSellingSizeTerm(), delta);
			FakeLogisticPlusLinearFunction fInput = new FakeLogisticPlusLinearFunction(
					(x * 1.8), (x * 1.9), (x * 2.0), (x * 2.1), (x * 2.2));
			FakeLogisticPlusLinearFunction importFunction = (FakeLogisticPlusLinearFunction) exchange
					.getImportFunction();
			Assert.assertEquals(fInput.getY0(), importFunction.getY0(), delta);
			Assert.assertEquals(fInput.getX0(), importFunction.getX0(), delta);
			Assert.assertEquals(fInput.getDelta(), importFunction.getDelta(),
					delta);
			Assert.assertEquals(fInput.getLambda(), importFunction.getLambda(),
					delta);
			Assert.assertEquals(fInput.getSlope(), importFunction.getSlope(),
					delta);

			FakeLogisticPlusLinearFunction fOutput = new FakeLogisticPlusLinearFunction(
					(x * 2.3), (x * 2.4), (x * 2.5), (x * 2.6), (x * 2.7));
			FakeLogisticPlusLinearFunction exportFunction = (FakeLogisticPlusLinearFunction) exchange
					.getExportFunction();
			Assert.assertEquals(fOutput.getY0(), exportFunction.getY0(), delta);
			Assert.assertEquals(fOutput.getX0(), exportFunction.getX0(), delta);
			Assert.assertEquals(fOutput.getDelta(), exportFunction.getDelta(),
					delta);
			Assert.assertEquals(fOutput.getLambda(),
					exportFunction.getLambda(), delta);
			Assert.assertEquals(fOutput.getSlope(), exportFunction.getSlope(),
					delta);

			Assert.assertEquals(_commodities.get(i / 2), exchange.myCommodity);

		}
	}

	@Test
	public void TestAAProcessorSetUpExchangesAndZUtilitiesNoZone() {
		// This uses the default one which is set
		clearExchanges();
		TestAAPProcessor processor = new TestAAPProcessor();
		FakeResourceUtil fakeResourceUtil = (FakeResourceUtil) createFakeResourceUtil();
		processor.zones = _allZones;
		processor.setUseExchange(6);
		processor.callSetUpExchangesAndZUtilities(fakeResourceUtil);
		String[] monitorExchanges = { "true", "true", "false", "false" };
		String[] specifiedExchanges = { "true", "false", "true", "false" };

		float[] zoneNumbers = { 0f, 1f, 0f, 1f };
		String[] commodities = { "One", "One", "Two", "Two", "One" };

		Exchange[] exchanges = {
				((Commodity) _commodities.get(0)).getExchange(0),
				((Commodity) _commodities.get(0)).getExchange(1),
				((Commodity) _commodities.get(1)).getExchange(0),
				((Commodity) _commodities.get(1)).getExchange(1) };
		for (float x = 1; x < 5; x++) {
			int i = (int) x - 1;
			Exchange exchange = exchanges[i];
			Assert.assertEquals(x * 1.1, exchange.getPrice(), delta);
			Assert.assertEquals(Boolean.parseBoolean(monitorExchanges[i]),
					exchange.monitor);
			Assert.assertEquals(x * 1.2, exchange.getBuyingSizeTerm(), delta);
			Assert.assertEquals(x * 1.3, exchange.getSellingSizeTerm(), delta);
			FakeLogisticPlusLinearFunction fInput = new FakeLogisticPlusLinearFunction(
					(x * 1.8), (x * 1.9), (x * 2.0), (x * 2.1), (x * 2.2));
			FakeLogisticPlusLinearFunction importFunction = (FakeLogisticPlusLinearFunction) exchange
					.getImportFunction();
			Assert.assertEquals(fInput.getY0(), importFunction.getY0(), delta);
			Assert.assertEquals(fInput.getX0(), importFunction.getX0(), delta);
			Assert.assertEquals(fInput.getDelta(), importFunction.getDelta(),
					delta);
			Assert.assertEquals(fInput.getLambda(), importFunction.getLambda(),
					delta);
			Assert.assertEquals(fInput.getSlope(), importFunction.getSlope(),
					delta);

			FakeLogisticPlusLinearFunction fOutput = new FakeLogisticPlusLinearFunction(
					(x * 2.3), (x * 2.4), (x * 2.5), (x * 2.6), (x * 2.7));
			FakeLogisticPlusLinearFunction exportFunction = (FakeLogisticPlusLinearFunction) exchange
					.getExportFunction();
			Assert.assertEquals(fOutput.getY0(), exportFunction.getY0(), delta);
			Assert.assertEquals(fOutput.getX0(), exportFunction.getX0(), delta);
			Assert.assertEquals(fOutput.getDelta(), exportFunction.getDelta(),
					delta);
			Assert.assertEquals(fOutput.getLambda(),
					exportFunction.getLambda(), delta);
			Assert.assertEquals(fOutput.getSlope(), exportFunction.getSlope(),
					delta);

			Assert.assertEquals(_commodities.get(i / 2), exchange.myCommodity);
		}
	}

	@Test
	public void TestAAProcessorSetUpExchangesAndZUtilitiesLessEntries() {
		clearExchanges();
		TestAAPProcessor processor = new TestAAPProcessor();
		FakeResourceUtil fakeResourceUtil = (FakeResourceUtil) createFakeResourceUtil();
		processor.zones = _allZones;
		processor.setUseExchange(7);
		processor.callSetUpExchangesAndZUtilities(fakeResourceUtil);
		String[] monitorExchanges = { "true", "true", "false", "false" };
		String[] specifiedExchanges = { "true", "false", "true", "false" };

		float[] zoneNumbers = { 0f, 1f, 0f, 1f };
		String[] commodities = { "One", "One", "Two", "Two", "One" };

		Exchange[] exchanges = {
				((Commodity) _commodities.get(0)).getExchange(0),
				((Commodity) _commodities.get(0)).getExchange(1),
				((Commodity) _commodities.get(1)).getExchange(0),
				((Commodity) _commodities.get(1)).getExchange(1) };
		for (float x = 1; x < 5; x++) {
			int i = (int) x - 1;
			Exchange exchange = exchanges[i];

			if (i / 2 == 0) {
				Assert.assertEquals(1.1, exchange.getPrice(), delta);
				Assert.assertEquals(false, exchange.monitor);
				Assert.assertEquals(1.2, exchange.getBuyingSizeTerm(), delta);
				Assert.assertEquals(1.3, exchange.getSellingSizeTerm(), delta);
				FakeLogisticPlusLinearFunction fInput = new FakeLogisticPlusLinearFunction(
						(1.8), (1.9), (2.0), (2.1), (2.2));
				FakeLogisticPlusLinearFunction importFunction = (FakeLogisticPlusLinearFunction) exchange
						.getImportFunction();
				Assert.assertEquals(fInput.getY0(), importFunction.getY0(),
						delta);
				Assert.assertEquals(fInput.getX0(), importFunction.getX0(),
						delta);
				Assert.assertEquals(fInput.getDelta(),
						importFunction.getDelta(), delta);
				Assert.assertEquals(fInput.getLambda(),
						importFunction.getLambda(), delta);
				Assert.assertEquals(fInput.getSlope(),
						importFunction.getSlope(), delta);

				FakeLogisticPlusLinearFunction fOutput = new FakeLogisticPlusLinearFunction(
						(2.3), (2.4), (2.5), (2.6), (2.7));
				FakeLogisticPlusLinearFunction exportFunction = (FakeLogisticPlusLinearFunction) exchange
						.getExportFunction();
				Assert.assertEquals(fOutput.getY0(), exportFunction.getY0(),
						delta);
				Assert.assertEquals(fOutput.getX0(), exportFunction.getX0(),
						delta);
				Assert.assertEquals(fOutput.getDelta(),
						exportFunction.getDelta(), delta);
				Assert.assertEquals(fOutput.getLambda(),
						exportFunction.getLambda(), delta);
				Assert.assertEquals(fOutput.getSlope(),
						exportFunction.getSlope(), delta);
			}

			else {
				Assert.assertEquals(((Commodity) _commodities.get(i / 2))
						.getExpectedPrice(), exchange.getPrice(), delta);
				Assert.assertEquals(false, exchange.monitor);
				Assert.assertEquals(1, exchange.getBuyingSizeTerm(), delta);
				Assert.assertEquals(1, exchange.getSellingSizeTerm(), delta);

				Assert.assertEquals(Commodity.zeroFunction,
						exchange.getImportFunction());
				Assert.assertEquals(Commodity.zeroFunction,
						exchange.getExportFunction());
			}
			Assert.assertEquals(_commodities.get(i / 2), exchange.myCommodity);

			FakeExchange fe = (FakeExchange) exchange;
			CommodityZUtility[] buyFlow = fe.GetBuyingFlow();
			CommodityZUtility[] sellFlow = fe.GetSellingFlow();

			for (int j = 0; j < 4; j++) {
				if (i < 2) {
					// One buying, all selling
					Assert.assertNotNull(sellFlow[j]);
					if (i == j)
						Assert.assertNotNull(buyFlow[j]);
					else
						Assert.assertNull(buyFlow[j]);
				} else {
					// One selling, all buying
					Assert.assertNotNull(buyFlow[j]);
					if (i % 2 == j)
						Assert.assertNotNull(sellFlow[j]);
					else
						Assert.assertNull(sellFlow[j]);
				}
			}

		}
	}

	@Test
	public void TestAAProcessorSetUpExchangesAndZUtilitiesLessEntriesTypeS() {
		clearExchanges();
		setupCommodititesS();
		TestAAPProcessor processor = new TestAAPProcessor();
		FakeResourceUtil fakeResourceUtil = (FakeResourceUtil) createFakeResourceUtil();
		processor.zones = _allZones;
		processor.setUseExchange(6);
		processor.callSetUpExchangesAndZUtilities(fakeResourceUtil);
		String[] monitorExchanges = { "true", "true", "false", "false" };
		String[] specifiedExchanges = { "true", "false", "true", "false" };

		float[] zoneNumbers = { 0f, 1f, 0f, 1f };
		String[] commodities = { "One", "One", "Two", "Two" };

		Exchange[] exchanges = {
				((Commodity) _commodities.get(0)).getExchange(0),
				((Commodity) _commodities.get(0)).getExchange(1),
				((Commodity) _commodities.get(1)).getExchange(0),
				((Commodity) _commodities.get(1)).getExchange(1) };
		for (float x = 1; x < 5; x++) {
			int i = (int) x - 1;
			Exchange exchange = exchanges[i];

			if (i % 2 == 1) {
				Assert.assertNull(exchange);
			}

			else {
				Assert.assertNotNull(exchange);
				Assert.assertEquals(_commodities.get(i / 2),
						exchange.myCommodity);

				FakeExchange fe = (FakeExchange) exchange;
				CommodityZUtility[] buyFlow = fe.GetBuyingFlow();
				CommodityZUtility[] sellFlow = fe.GetSellingFlow();

				for (int j = 0; j < 4; j++) {
					// All all
					Assert.assertNotNull(sellFlow[j]);
					Assert.assertNotNull(buyFlow[j]);

				}
			}

		}

	}

	@Test
	public void TestAAProcessorSetUpExchangesAndZUtilitiesLessEntriesTypeN() {
		clearExchanges();
		setupCommodititesN();
		TestAAPProcessor processor = new TestAAPProcessor();
		FakeResourceUtil fakeResourceUtil = (FakeResourceUtil) createFakeResourceUtil();
		processor.zones = _allZones;
		processor.setUseExchange(0);
		processor.callSetUpExchangesAndZUtilities(fakeResourceUtil);
		String[] monitorExchanges = { "true", "true", "false", "false" };
		String[] specifiedExchanges = { "true", "false", "true", "false" };

		float[] zoneNumbers = { 0f, 1f, 0f, 1f };
		String[] commodities = { "One", "One", "Two", "Two" };

		Exchange[] exchanges = {
				((Commodity) _commodities.get(0)).getExchange(0),
				((Commodity) _commodities.get(0)).getExchange(1),
				((Commodity) _commodities.get(1)).getExchange(0),
				((Commodity) _commodities.get(1)).getExchange(1) };
		for (float x = 1; x < 5; x++) {
			int i = (int) x - 1;
			Exchange exchange = exchanges[i];

			Assert.assertNotNull((NonTransportableExchange) exchange);
			Assert.assertEquals(_commodities.get(i / 2), exchange.myCommodity);

			FakeNonTransportableExchange fe = (FakeNonTransportableExchange) exchange;
			CommodityZUtility[] buyFlow = fe.GetBuyingFlow();
			CommodityZUtility[] sellFlow = fe.GetSellingFlow();

			Assert.assertEquals(1, buyFlow.length);
			Assert.assertEquals(1, sellFlow.length);
			
			Assert.assertEquals((int)zoneNumbers[i], buyFlow[0].getTaz().zoneIndex);			
		}

	}

	@Before
	public void clear() {
		FakeCommodity.clearCommodities();
		setupCommoditites();
	}
}
