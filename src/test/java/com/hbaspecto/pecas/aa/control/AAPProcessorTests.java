package com.hbaspecto.pecas.aa.control;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hbaspecto.pecas.IResource;
import com.hbaspecto.pecas.aa.activities.ProductionActivity;
import com.hbaspecto.pecas.aa.commodity.AbstractCommodity;
import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.hbaspecto.pecas.zones.AbstractZone;
import com.hbaspecto.pecas.zones.PECASZone;
import com.pb.common.matrix.StringIndexedNDimensionalMatrix;

public class AAPProcessorTests {

	private static final float delta = 0.000001f;
	static PECASZone[] _allZones = null;
	static ArrayList<ProductionActivity> _productionActivities = null;
	static ArrayList<AbstractCommodity> _commodities = null;

	@BeforeClass
	public static void setup() {
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

		_commodities = new ArrayList<AbstractCommodity>();
		_commodities.add(Commodity.createOrRetrieveCommodity("One", 'c'));
		_commodities.add(Commodity.createOrRetrieveCommodity("Two", 'p'));
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

}
