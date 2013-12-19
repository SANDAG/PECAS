package com.hbaspecto.pecas.aa.control;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.StringWriter;

import com.hbaspecto.pecas.IResource;
import com.hbaspecto.pecas.OverflowException;
import com.hbaspecto.pecas.aa.technologyChoice.ConsumptionFunction;
import com.hbaspecto.pecas.aa.technologyChoice.ProductionFunction;
import com.hbaspecto.pecas.zones.PECASZone;

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
}
