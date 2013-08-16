/**
 * 
 */
package com.hbaspecto.pecas.aa.jppf;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.jppf.server.protocol.JPPFTask;

import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.NoAlternativeAvailable;
import com.hbaspecto.pecas.aa.activities.AggregateActivity;
import com.hbaspecto.pecas.aa.activities.AggregateDistribution;
import com.hbaspecto.pecas.aa.activities.ProductionActivity;
import com.hbaspecto.pecas.aa.commodity.AbstractCommodity;
import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.hbaspecto.pecas.aa.commodity.CommodityZUtility;
import com.hbaspecto.pecas.zones.AbstractZone;

import drasys.or.linear.algebra.Algebra;
import drasys.or.linear.algebra.AlgebraException;
import drasys.or.matrix.DenseMatrix;
import drasys.or.matrix.DenseVector;
import drasys.or.matrix.VectorI;

class ActivityMatrixJPPFInitializer extends JPPFTask {

	private static final long serialVersionUID = -2957932235606882035L;

	static Logger logger = Logger.getLogger(ActivityMatrixJPPFInitializer.class);

	private transient AggregateActivity activity;
	final String activityName;

	final int numCommodities;

	// return value, set at null for sending to node, will be full of values on
	// return
	double[][] dStorage = null;
	int nRows;
	int nCols;

	/**
	 * CommodityZUtilities of buying (price-weighted accessibility measures) by
	 * commodity and zone for buying stuff
	 */
	double[][] commodityBuyingUtilities;
	/**
	 * CommodityZUtilities of selling (price-weighted accessibility measures) by
	 * commodity and zone for selling stuff
	 */
	double[][] commoditySellingUtilities;

	double[][] sizesAndConstants;

	ActivityMatrixJPPFInitializer(AggregateActivity actParam, int rows, int cols,
			int numCommodities, double[][] commodityBuyingZUtilities,
			double[][] commoditySellingZUtilities) {
		nRows = rows;
		nCols = cols;
		activity = actParam;
		activityName = actParam.name;
		this.numCommodities = numCommodities;
		commodityBuyingUtilities = commodityBuyingZUtilities;
		commoditySellingUtilities = commoditySellingZUtilities;
		sizesAndConstants = JppfAAModel.getSizesAndConstants(getActivity());
	}

	@Override
	public void run() {
		try {
			JppfNodeSetup.setup(getDataProvider());
			System.out
					.println("Calculating production/consumption rates and derivatives for "
							+ activityName);
			dStorage = new double[nRows][nCols];
			setZUtilities();
			JppfAAModel.setSizesAndConstants(getActivity(), sizesAndConstants);

			// erase zutility arrays so we don't send them back over the network
			// to the JPPF client
			commodityBuyingUtilities = null;
			commoditySellingUtilities = null;
			sizesAndConstants = null;

			// build up relationship between average commodity price and total
			// surplus
			DenseVector pl; // P(z|a) in new documentation
			DenseMatrix fpl;
			try {
				pl = new DenseVector(
						getActivity().logitModelOfZonePossibilities
								.getChoiceProbabilities());
				fpl = new DenseMatrix(
						getActivity().logitModelOfZonePossibilities
								.choiceProbabilityDerivatives());

			}
			catch (final ChoiceModelOverflowException e) {
				e.printStackTrace();
				setResult(e);
				throw new RuntimeException("Can't solve for amounts in zone", e);
			}
			catch (final NoAlternativeAvailable e) {
				e.printStackTrace();
				setResult(e);
				throw new RuntimeException("Can't solve for amounts in zone", e);
			}
			// dulbydprice is derivative of location utility wrt changes in
			// average prices of commodites
			// is d(LU(a,z)/d(Price(bar)(c)) in new notation
			final DenseMatrix dulbydprice = new DenseMatrix(fpl.sizeOfColumns(),
					numCommodities);
			final int[] rows = new int[1];
			final int[] columns = new int[numCommodities];
			final double[][] valuesToAdd = new double[1][];
			for (int col = 0; col < numCommodities; col++) {
				columns[col] = col;
			}
			for (int location = 0; location < pl.size(); location++) {
				rows[0] = location;
				final AggregateDistribution l = (AggregateDistribution) getActivity().logitModelOfZonePossibilities
						.alternativeAt(location);
				valuesToAdd[0] = l.calculateLocationUtilityWRTAveragePrices();
				// dulbydprice.set(rows,columns,valuesToAdd);
				dulbydprice.setRow(location, new DenseVector(valuesToAdd[0]));
			}
			DenseMatrix dLocationByDPrice = new DenseMatrix(
					AbstractZone.getAllZones().length, numCommodities);
			final Algebra a = new Algebra();
			try {
				// fpl.mult(dulbydprice,dLocationByDPrice);
				dLocationByDPrice = a.multiply(fpl, dulbydprice);
				// ENHANCEMENT remove this debug code to speed things up
				for (int r1 = 0; r1 < dLocationByDPrice.sizeOfRows(); r1++) {
					for (int c1 = 0; c1 < dLocationByDPrice.sizeOfColumns(); c1++) {
						if (Double.isNaN(dLocationByDPrice.elementAt(r1, c1))) {
							logger.fatal("NaN in dLocationByDPrice");
							final RuntimeException e = new RuntimeException(
									"NaN in dLocationByDPrice, printing matrices to console");
							setResult(e);
							throw e;

						}
					}
				}
			}
			catch (final AlgebraException e1) {
				e1.printStackTrace();
				setResult(e1);
				throw new RuntimeException(
						"Can't multiply matrices to figure out average price surplus", e1);
			}
			for (int location = 0; location < pl.size(); location++) {
				final VectorI dThisLocationByPrices = new DenseVector(numCommodities);
				for (int i = 0; i < dThisLocationByPrices.size(); i++) {
					dThisLocationByPrices.setElementAt(i,
							dLocationByDPrice.elementAt(location, i));
				}
				final AggregateDistribution l = (AggregateDistribution) getActivity().logitModelOfZonePossibilities
						.alternativeAt(location);
				l.addTwoComponentsOfDerivativesToAveragePriceMatrix(getActivity()
						.getTotalAmount(), dStorage, dThisLocationByPrices);
			}
			setResult(new Boolean(true));
		}
		catch (final RuntimeException e1) {
			System.out.println("Error in JPPF task " + e1);
			logger.fatal("JPPF Task didn't work", e1);
			System.out.println("JPPF Task didn't work " + e1);
			throw e1;
		}
	}

	private AggregateActivity getActivity() {
		if (activity == null) {
			activity = (AggregateActivity) ProductionActivity
					.retrieveProductionActivity(activityName);
		}
		return activity;
	}

	private void setZUtilities() {
		final AbstractZone[] zones = AbstractZone.getAllZones();
		final Iterator commodityIt = AbstractCommodity.getAllCommodities()
				.iterator();
		// store up commodity z utilities;
		while (commodityIt.hasNext()) {
			final Commodity c = (Commodity) commodityIt.next();
			final int comNum = c.commodityNumber;
			for (int z = 0; z < zones.length; z++) {
				// old debug statement if (zones[z].getZoneUserNumber()==526)
				// logger.info("Zones "+526+" index "+zones[z].zoneIndex+
				// " utility b:"+commodityBuyingUtilities[comNum][zones[z].zoneIndex]+",s:"+commoditySellingUtilities[comNum][zones[z].zoneIndex]+c);
				final CommodityZUtility bzu = c.retrieveCommodityZUtility(zones[z],
						false);
				bzu.setLastCalculatedUtility(commodityBuyingUtilities[comNum][zones[z].zoneIndex]);
				bzu.setLastUtilityValid(true);
				bzu.setPricesFixed(true);
				final CommodityZUtility szu = c.retrieveCommodityZUtility(zones[z],
						true);
				szu.setLastCalculatedUtility(commoditySellingUtilities[comNum][zones[z].zoneIndex]);
				szu.setLastUtilityValid(true);
				szu.setPricesFixed(true);
			}
		}
	}

}