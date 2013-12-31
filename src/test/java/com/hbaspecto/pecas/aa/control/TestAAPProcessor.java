package com.hbaspecto.pecas.aa.control;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.util.HashMap;

import com.hbaspecto.functions.SingleParameterFunction;
import com.hbaspecto.pecas.IResource;
import com.hbaspecto.pecas.OverflowException;
import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.hbaspecto.pecas.aa.commodity.Exchange;
import com.hbaspecto.pecas.aa.commodity.NonTransportableExchange;
import com.hbaspecto.pecas.aa.technologyChoice.ConsumptionFunction;
import com.hbaspecto.pecas.aa.technologyChoice.ProductionFunction;
import com.hbaspecto.pecas.zones.PECASZone;
import com.pb.common.datafile.TableDataSet;

public class TestAAPProcessor extends AAPProcessor {

	StringWriter _commodityWriter = new StringWriter();
	StringWriter _activityWriter = new StringWriter();
	StringWriter _zonalMakeUseWriter = new StringWriter();
	StringWriter _aggregateMakeUseWriter = new StringWriter();
	ByteArrayOutputStream _zonalMakeUseObjectWriter = new ByteArrayOutputStream();

	@Override
	protected double[][] readFloorspace(IResource resourceUtil) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void setUpProductionActivities(IResource resourceUtil) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void setUpMakeAndUse(IResource resourceUtil) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void writeTechnologyChoice(IResource resourceUtil) {
		// TODO Auto-generated method stub

	}

	@Override
	protected BufferedWriter GetActivitiesBufferedWriter(String outputPath)
			throws IOException {
		return new BufferedWriter(_activityWriter);
	}

	@Override
	protected BufferedWriter GetCommodityBufferedWriter(String outputPath)
			throws IOException {
		return new BufferedWriter(_commodityWriter);
	}

	@Override
	protected BufferedWriter GetZonalMakeUseBufferedWriter(String outputPath)
			throws IOException {
		return new BufferedWriter(_zonalMakeUseWriter);
	}

	@Override
	protected ObjectOutputStream GetZonalMakeUseObjectOutputStream(
			String filename) throws IOException {
		return new java.io.ObjectOutputStream(_zonalMakeUseObjectWriter);
	}

	@Override
	protected BufferedWriter GetAggregateMakeUseBufferedWriter(String outputPath)
			throws IOException {
		return new BufferedWriter(_aggregateMakeUseWriter);
	}

	@Override
	protected double[] CalculateBuyingZUtilities(ConsumptionFunction cf,
			PECASZone zone) throws OverflowException {
		double[] util = new double[cf.size()];
		for (int i = 0; i < cf.size(); i++) {
			util[i] = i;
		}
		return util;
	}

	@Override
	protected double[] CalculateSellingZUtilities(ProductionFunction pf,
			PECASZone zone) throws OverflowException {
		double[] util = new double[pf.size()];
		for (int i = 0; i < pf.size(); i++) {
			util[i] = i;
		}
		return util;
	}

	public void callSetUpExchangesAndZUtilities(IResource resourceUtil) {
		setUpExchangesAndZUtilities(resourceUtil);

	}

	@Override
	protected TableDataSet loadTableDataSet(String tableName, String source,
			boolean check, IResource resourceUtil) {
		TableDataSet table = null;
		if (useExchange == 0 && source.compareTo("aa.current.data") == 0) {
			String[][] f = new String[4][17];
			String[] monitorExchanges = { "true", "true", "false", "false" };
			String[] commodities = { "One", "One", "Two", "Two" };
			String[] specifiedExchanges = { "true", "false", "true", "false" };
			for (int i = 0; i < 4; i++) {
				String[] row = { "", monitorExchanges[i], commodities[i], "",
						"", "", specifiedExchanges[i], "", "", "", "", "", "",
						"", "", "", "" };
				f[i] = row;
			}

			// HashMap<String, float> row = new HashMap<String, String>();
			// row.put("Price", "1.0");
			String[] columns = { "Price", "MonitorExchange", "Commodity",
					"ZoneNumber", "BuyingSize", "SellingSize",
					"SpecifiedExchange", "ImportFunctionMidpoint",
					"ImportFunctionMidpointPrice", "ImportFunctionEta",
					"ImportFunctionDelta", "ImportFunctionSlope",
					"ExportFunctionMidpoint", "ExportFunctionMidpointPrice",
					"ExportFunctionEta", "ExportFunctionDelta",
					"ExportFunctionSlope" };

			float[] zoneNumbers = { 0f, 1f, 0f, 1f };
			table = TableDataSet.create(f, columns);
			table.setColumnAsFloat(1, new float[4]);
			table.setColumnAsFloat(4, zoneNumbers);
			table.setColumnAsFloat(5, new float[4]);
			table.setColumnAsFloat(6, new float[4]);
			table.setColumnAsFloat(8, new float[4]);
			table.setColumnAsFloat(9, new float[4]);
			table.setColumnAsFloat(10, new float[4]);
			table.setColumnAsFloat(11, new float[4]);
			table.setColumnAsFloat(12, new float[4]);
			table.setColumnAsFloat(13, new float[4]);
			table.setColumnAsFloat(14, new float[4]);
			table.setColumnAsFloat(15, new float[4]);
			table.setColumnAsFloat(16, new float[4]);
			table.setColumnAsFloat(17, new float[4]);

			for (float r = 1; r <= 4; r++) {
				table.setValueAt((int) r, "Price", r * 1.1f);
				table.setValueAt((int) r, "BuyingSize", r * 1.2f);
				table.setValueAt((int) r, "SellingSize", r * 1.3f);
				for (float y = 8; y <= 17; y++)
					table.setValueAt((int) r, (int) y, (r * (1.0f + y / 10)));
			}
		} else if (useExchange == 1 && source.compareTo("aa.base.data") == 0) {
			String[][] f = new String[4][17];
			String[] monitorExchanges = { "true", "true", "false", "false" };
			String[] commodities = { "One", "One", "Two", "Two" };
			String[] specifiedExchanges = { "true", "false", "true", "false" };
			for (int i = 0; i < 4; i++) {
				String[] row = { "", monitorExchanges[i], commodities[i], "",
						"", "", specifiedExchanges[i], "", "", "", "", "", "",
						"", "", "", "" };
				f[i] = row;
			}

			// HashMap<String, float> row = new HashMap<String, String>();
			// row.put("Price", "1.0");
			String[] columns = { "Price", "MonitorExchange", "Commodity",
					"ZoneNumber", "BuyingSize", "SellingSize",
					"SpecifiedExchange", "ImportFunctionMidpoint",
					"ImportFunctionMidpointPrice", "ImportFunctionEta",
					"ImportFunctionDelta", "ImportFunctionSlope",
					"ExportFunctionMidpoint", "ExportFunctionMidpointPrice",
					"ExportFunctionEta", "ExportFunctionDelta",
					"ExportFunctionSlope" };

			float[] zoneNumbers = { 0f, 1f, 0f, 1f };
			table = TableDataSet.create(f, columns);
			table.setColumnAsFloat(1, new float[4]);
			table.setColumnAsFloat(4, zoneNumbers);
			table.setColumnAsFloat(5, new float[4]);
			table.setColumnAsFloat(6, new float[4]);
			table.setColumnAsFloat(8, new float[4]);
			table.setColumnAsFloat(9, new float[4]);
			table.setColumnAsFloat(10, new float[4]);
			table.setColumnAsFloat(11, new float[4]);
			table.setColumnAsFloat(12, new float[4]);
			table.setColumnAsFloat(13, new float[4]);
			table.setColumnAsFloat(14, new float[4]);
			table.setColumnAsFloat(15, new float[4]);
			table.setColumnAsFloat(16, new float[4]);
			table.setColumnAsFloat(17, new float[4]);

			for (float r = 1; r <= 4; r++) {
				table.setValueAt((int) r, "Price", r * 5f);
			}
		} else if (useExchange == 2) {
			// Invalid Commodity
			String[][] f = new String[4][17];
			String[] monitorExchanges = { "true", "true", "false", "false" };
			String[] commodities = { "One", "Wrong", "Two", "Two" };
			String[] specifiedExchanges = { "true", "false", "true", "false" };
			for (int i = 0; i < 4; i++) {
				String[] row = { "", monitorExchanges[i], commodities[i], "",
						"", "", specifiedExchanges[i], "", "", "", "", "", "",
						"", "", "", "" };
				f[i] = row;
			}

			// HashMap<String, float> row = new HashMap<String, String>();
			// row.put("Price", "1.0");
			String[] columns = { "Price", "MonitorExchange", "Commodity",
					"ZoneNumber", "BuyingSize", "SellingSize",
					"SpecifiedExchange", "ImportFunctionMidpoint",
					"ImportFunctionMidpointPrice", "ImportFunctionEta",
					"ImportFunctionDelta", "ImportFunctionSlope",
					"ExportFunctionMidpoint", "ExportFunctionMidpointPrice",
					"ExportFunctionEta", "ExportFunctionDelta",
					"ExportFunctionSlope" };

			float[] zoneNumbers = { 0f, 1f, 0f, 1f };
			table = TableDataSet.create(f, columns);
			table.setColumnAsFloat(1, new float[4]);
			table.setColumnAsFloat(4, zoneNumbers);
			table.setColumnAsFloat(5, new float[4]);
			table.setColumnAsFloat(6, new float[4]);
			table.setColumnAsFloat(8, new float[4]);
			table.setColumnAsFloat(9, new float[4]);
			table.setColumnAsFloat(10, new float[4]);
			table.setColumnAsFloat(11, new float[4]);
			table.setColumnAsFloat(12, new float[4]);
			table.setColumnAsFloat(13, new float[4]);
			table.setColumnAsFloat(14, new float[4]);
			table.setColumnAsFloat(15, new float[4]);
			table.setColumnAsFloat(16, new float[4]);
			table.setColumnAsFloat(17, new float[4]);

			for (float r = 1; r <= 4; r++) {
				table.setValueAt((int) r, "Price", r * 1.1f);
				table.setValueAt((int) r, "BuyingSize", r * 1.2f);
				table.setValueAt((int) r, "SellingSize", r * 1.3f);
				for (float y = 8; y <= 17; y++)
					table.setValueAt((int) r, (int) y, (r * (1.0f + y / 10)));
			}
		} else if (useExchange == 3) {
			// Invalid Zone
			String[][] f = new String[4][17];
			String[] monitorExchanges = { "true", "true", "false", "false" };
			String[] commodities = { "One", "One", "Two", "Two" };
			String[] specifiedExchanges = { "true", "false", "true", "false" };
			for (int i = 0; i < 4; i++) {
				String[] row = { "", monitorExchanges[i], commodities[i], "",
						"", "", specifiedExchanges[i], "", "", "", "", "", "",
						"", "", "", "" };
				f[i] = row;
			}

			// HashMap<String, float> row = new HashMap<String, String>();
			// row.put("Price", "1.0");
			String[] columns = { "Price", "MonitorExchange", "Commodity",
					"ZoneNumber", "BuyingSize", "SellingSize",
					"SpecifiedExchange", "ImportFunctionMidpoint",
					"ImportFunctionMidpointPrice", "ImportFunctionEta",
					"ImportFunctionDelta", "ImportFunctionSlope",
					"ExportFunctionMidpoint", "ExportFunctionMidpointPrice",
					"ExportFunctionEta", "ExportFunctionDelta",
					"ExportFunctionSlope" };
			float[] zoneNumbers = { 0f, 1f, 0f, 75f };
			table = TableDataSet.create(f, columns);
			table.setColumnAsFloat(1, new float[4]);
			table.setColumnAsFloat(4, zoneNumbers);
			table.setColumnAsFloat(5, new float[4]);
			table.setColumnAsFloat(6, new float[4]);
			table.setColumnAsFloat(8, new float[4]);
			table.setColumnAsFloat(9, new float[4]);
			table.setColumnAsFloat(10, new float[4]);
			table.setColumnAsFloat(11, new float[4]);
			table.setColumnAsFloat(12, new float[4]);
			table.setColumnAsFloat(13, new float[4]);
			table.setColumnAsFloat(14, new float[4]);
			table.setColumnAsFloat(15, new float[4]);
			table.setColumnAsFloat(16, new float[4]);
			table.setColumnAsFloat(17, new float[4]);

			for (float r = 1; r <= 4; r++) {
				table.setValueAt((int) r, "Price", r * 1.1f);
				table.setValueAt((int) r, "BuyingSize", r * 1.2f);
				table.setValueAt((int) r, "SellingSize", r * 1.3f);
				for (float y = 8; y <= 17; y++)
					table.setValueAt((int) r, (int) y, (r * (1.0f + y / 10)));
			}
		} else if (useExchange == 4) {
			// No Price
			String[][] f = new String[4][17];
			String[] monitorExchanges = { "true", "true", "false", "false" };
			String[] commodities = { "One", "One", "Two", "Two" };
			String[] specifiedExchanges = { "true", "false", "true", "false" };
			for (int i = 0; i < 4; i++) {
				String[] row = { "", monitorExchanges[i], commodities[i], "",
						"", "", specifiedExchanges[i], "", "", "", "", "", "",
						"", "", "", "" };
				f[i] = row;
			}

			// HashMap<String, float> row = new HashMap<String, String>();
			// row.put("Price", "1.0");
			String[] columns = { "P", "MonitorExchange", "Commodity",
					"ZoneNumber", "BuyingSize", "SellingSize",
					"SpecifiedExchange", "ImportFunctionMidpoint",
					"ImportFunctionMidpointPrice", "ImportFunctionEta",
					"ImportFunctionDelta", "ImportFunctionSlope",
					"ExportFunctionMidpoint", "ExportFunctionMidpointPrice",
					"ExportFunctionEta", "ExportFunctionDelta",
					"ExportFunctionSlope" };

			float[] zoneNumbers = { 0f, 1f, 0f, 1f };
			table = TableDataSet.create(f, columns);
			table.setColumnAsFloat(1, new float[4]);
			table.setColumnAsFloat(4, zoneNumbers);
			table.setColumnAsFloat(5, new float[4]);
			table.setColumnAsFloat(6, new float[4]);
			table.setColumnAsFloat(8, new float[4]);
			table.setColumnAsFloat(9, new float[4]);
			table.setColumnAsFloat(10, new float[4]);
			table.setColumnAsFloat(11, new float[4]);
			table.setColumnAsFloat(12, new float[4]);
			table.setColumnAsFloat(13, new float[4]);
			table.setColumnAsFloat(14, new float[4]);
			table.setColumnAsFloat(15, new float[4]);
			table.setColumnAsFloat(16, new float[4]);
			table.setColumnAsFloat(17, new float[4]);

			for (float r = 1; r <= 4; r++) {
				// table.setValueAt((int) r, "Price", r * 1.1f);
				table.setValueAt((int) r, "BuyingSize", r * 1.2f);
				table.setValueAt((int) r, "SellingSize", r * 1.3f);
				for (float y = 8; y <= 17; y++)
					table.setValueAt((int) r, (int) y, (r * (1.0f + y / 10)));
			}
		} else if (useExchange == 5) {
			// No Montor
			String[][] f = new String[4][17];
			String[] monitorExchanges = { "true", "true", "false", "false" };
			String[] commodities = { "One", "One", "Two", "Two" };
			String[] specifiedExchanges = { "true", "false", "true", "false" };
			for (int i = 0; i < 4; i++) {
				String[] row = { "", monitorExchanges[i], commodities[i], "",
						"", "", specifiedExchanges[i], "", "", "", "", "", "",
						"", "", "", "" };
				f[i] = row;
			}

			// HashMap<String, float> row = new HashMap<String, String>();
			// row.put("Price", "1.0");
			String[] columns = { "Price", "M", "Commodity", "ZoneNumber",
					"BuyingSize", "SellingSize", "SpecifiedExchange",
					"ImportFunctionMidpoint", "ImportFunctionMidpointPrice",
					"ImportFunctionEta", "ImportFunctionDelta",
					"ImportFunctionSlope", "ExportFunctionMidpoint",
					"ExportFunctionMidpointPrice", "ExportFunctionEta",
					"ExportFunctionDelta", "ExportFunctionSlope" };

			float[] zoneNumbers = { 0f, 1f, 0f, 1f };
			table = TableDataSet.create(f, columns);
			table.setColumnAsFloat(1, new float[4]);
			table.setColumnAsFloat(4, zoneNumbers);
			table.setColumnAsFloat(5, new float[4]);
			table.setColumnAsFloat(6, new float[4]);
			table.setColumnAsFloat(8, new float[4]);
			table.setColumnAsFloat(9, new float[4]);
			table.setColumnAsFloat(10, new float[4]);
			table.setColumnAsFloat(11, new float[4]);
			table.setColumnAsFloat(12, new float[4]);
			table.setColumnAsFloat(13, new float[4]);
			table.setColumnAsFloat(14, new float[4]);
			table.setColumnAsFloat(15, new float[4]);
			table.setColumnAsFloat(16, new float[4]);
			table.setColumnAsFloat(17, new float[4]);

			for (float r = 1; r <= 4; r++) {
				table.setValueAt((int) r, "Price", r * 1.1f);
				table.setValueAt((int) r, "BuyingSize", r * 1.2f);
				table.setValueAt((int) r, "SellingSize", r * 1.3f);
				for (float y = 8; y <= 17; y++)
					table.setValueAt((int) r, (int) y, (r * (1.0f + y / 10)));
			}
		} else if (useExchange == 6) {
			// Missing Zone
			String[][] f = new String[4][17];
			String[] monitorExchanges = { "true", "true", "false", "false" };
			String[] commodities = { "One", "One", "Two", "Two" };
			String[] specifiedExchanges = { "true", "false", "true", "false" };
			for (int i = 0; i < 4; i++) {
				String[] row = { "", monitorExchanges[i], commodities[i], "",
						"", "", specifiedExchanges[i], "", "", "", "", "", "",
						"", "", "", "" };
				f[i] = row;
			}

			// HashMap<String, float> row = new HashMap<String, String>();
			// row.put("Price", "1.0");
			String[] columns = { "Price", "MonitorExchange", "Commodity",
					"ZoneNumber", "BuyingSize", "SellingSize",
					"SpecifiedExchange", "ImportFunctionMidpoint",
					"ImportFunctionMidpointPrice", "ImportFunctionEta",
					"ImportFunctionDelta", "ImportFunctionSlope",
					"ExportFunctionMidpoint", "ExportFunctionMidpointPrice",
					"ExportFunctionEta", "ExportFunctionDelta",
					"ExportFunctionSlope" };

			float[] zoneNumbers = { -1f, 1f, 0f, 1f };
			table = TableDataSet.create(f, columns);
			table.setColumnAsFloat(1, new float[4]);
			table.setColumnAsFloat(4, zoneNumbers);
			table.setColumnAsFloat(5, new float[4]);
			table.setColumnAsFloat(6, new float[4]);
			table.setColumnAsFloat(8, new float[4]);
			table.setColumnAsFloat(9, new float[4]);
			table.setColumnAsFloat(10, new float[4]);
			table.setColumnAsFloat(11, new float[4]);
			table.setColumnAsFloat(12, new float[4]);
			table.setColumnAsFloat(13, new float[4]);
			table.setColumnAsFloat(14, new float[4]);
			table.setColumnAsFloat(15, new float[4]);
			table.setColumnAsFloat(16, new float[4]);
			table.setColumnAsFloat(17, new float[4]);

			for (float r = 1; r <= 4; r++) {
				table.setValueAt((int) r, "Price", r * 1.1f);
				table.setValueAt((int) r, "BuyingSize", r * 1.2f);
				table.setValueAt((int) r, "SellingSize", r * 1.3f);
				for (float y = 8; y <= 17; y++)
					table.setValueAt((int) r, (int) y, (r * (1.0f + y / 10)));
			}
		}

		else if (useExchange == 7) {
			// Missing Zone
			String[][] f = new String[1][17];
			String[] monitorExchanges = { "false" };
			String[] commodities = { "One" };
			String[] specifiedExchanges = { "false" };
			for (int i = 0; i < 1; i++) {
				String[] row = { "", monitorExchanges[i], commodities[i], "",
						"", "", specifiedExchanges[i], "", "", "", "", "", "",
						"", "", "", "" };
				f[i] = row;
			}

			// HashMap<String, float> row = new HashMap<String, String>();
			// row.put("Price", "1.0");
			String[] columns = { "Price", "MonitorExchange", "Commodity",
					"ZoneNumber", "BuyingSize", "SellingSize",
					"SpecifiedExchange", "ImportFunctionMidpoint",
					"ImportFunctionMidpointPrice", "ImportFunctionEta",
					"ImportFunctionDelta", "ImportFunctionSlope",
					"ExportFunctionMidpoint", "ExportFunctionMidpointPrice",
					"ExportFunctionEta", "ExportFunctionDelta",
					"ExportFunctionSlope" };

			float[] zoneNumbers = { -1f };
			table = TableDataSet.create(f, columns);
			table.setColumnAsFloat(1, new float[1]);
			table.setColumnAsFloat(4, zoneNumbers);
			table.setColumnAsFloat(5, new float[1]);
			table.setColumnAsFloat(6, new float[1]);
			table.setColumnAsFloat(8, new float[1]);
			table.setColumnAsFloat(9, new float[1]);
			table.setColumnAsFloat(10, new float[1]);
			table.setColumnAsFloat(11, new float[1]);
			table.setColumnAsFloat(12, new float[1]);
			table.setColumnAsFloat(13, new float[1]);
			table.setColumnAsFloat(14, new float[1]);
			table.setColumnAsFloat(15, new float[1]);
			table.setColumnAsFloat(16, new float[1]);
			table.setColumnAsFloat(17, new float[1]);

			for (float r = 1; r <= 1; r++) {
				table.setValueAt((int) r, "Price", r * 1.1f);
				table.setValueAt((int) r, "BuyingSize", r * 1.2f);
				table.setValueAt((int) r, "SellingSize", r * 1.3f);
				for (float y = 8; y <= 17; y++)
					table.setValueAt((int) r, (int) y, (r * (1.0f + y / 10)));
			}
		}

		return table;
	}

	@Override
	protected SingleParameterFunction GetLinearFunction(float midpoint,
			float midpointPrice, float lambda, float delta, float slope) {
		return new FakeLogisticPlusLinearFunction(midpoint, midpointPrice,
				lambda, delta, slope);
	}

	protected int useExchange = 0;

	public void setUseExchange(int i) {
		useExchange = i;

	}

	@Override
	protected Exchange CreateNonTransportableExchange(Commodity c,
			PECASZone pecasZone) {
		return new FakeNonTransportableExchange(c, pecasZone);
	}

	@Override
	protected Exchange CreateExchange(Commodity c, PECASZone pecasZone,
			int length) {

		return new FakeExchange(c, pecasZone, zones.length);
	}
}
