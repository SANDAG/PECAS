package com.hbaspecto.pecas.aa.jppf;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.jppf.server.protocol.JPPFTask;

import com.hbaspecto.pecas.OverflowException;
import com.hbaspecto.pecas.aa.commodity.BuyingZUtility;
import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.hbaspecto.pecas.aa.commodity.CommodityZUtility;
import com.hbaspecto.pecas.aa.commodity.Exchange;
import com.hbaspecto.pecas.aa.commodity.SellingZUtility;

class FlowAllocator extends JPPFTask {

	static final Logger logger = Logger.getLogger(FlowAllocator.class);

	static class SandDResults implements Serializable {
		private static final long serialVersionUID = -1772331813374446669L;
		public double[][] surplusAndDerivative;
		public double[][] buyingAndSelling;
	}

	final String commodityName;
	double[] totalProduction;
	double[] totalConsumption;
	/**
	 * Derivative of total production
	 */
	double[] dTotalProduction;
	/**
	 * Derivative of total consumption
	 */
	double[] dTotalConsumption;
	double[] prices;
	boolean calculatingSizeTerms;
	double[][] sizeTerms;

	transient Commodity commodity = null;

	FlowAllocator(String cName, double[] tp, double[] tc, double[] dTP,
			double[] dTC, double[] prices, boolean calculatingSizeTerms) {
		commodityName = cName;
		commodity = Commodity.retrieveCommodity(commodityName);
		totalProduction = tp;
		totalConsumption = tc;
		dTotalProduction = dTP;
		dTotalConsumption = dTC;
		this.prices = prices;
		this.calculatingSizeTerms = calculatingSizeTerms;
		// shouldn't be necessary to send the size terms if we're calculating
		// size terms
		// if (!calculatingSizeTerms) {
		// get the size terms
		sizeTerms = JppfAAModel.getSizeTerms(commodity);
		// }
	}

	@Override
	public void run() {
		try {
			JppfNodeSetup.setup(getDataProvider());
			OverflowException error = null;

			commodity = Commodity.retrieveCommodity(commodityName);
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Setting up input values for " + commodity
							+ " commoditID:" + commodity.commodityNumber);
				}
				setInputValuesInCommodityAndExchangeObjects();

				// set size terms just in case it's a Floorspace commodity type,
				// or if we're not calculating them here.
				JppfAAModel.setSizeTerms(commodity, sizeTerms);
				if (calculatingSizeTerms) {
					setInitialSizeTermsTo1();
				}
				else {
					// null it out so we don't send it back
					sizeTerms = null;
				}

				final Hashtable<Integer, CommodityZUtility> ht;
				for (int b = 0; b < 2; b++) {
					Iterator<CommodityZUtility> it;
					if (b == 0) {
						it = commodity.getBuyingUtilitiesIterator();
						if (logger.isDebugEnabled()) {
							logger.debug("Allocating buying quantitites");
						}
					}
					else {
						it = commodity.getSellingUtilitiesIterator();
						if (logger.isDebugEnabled()) {
							logger.debug("Allocating selling quantities");
						}
					}
					while (it.hasNext()) {
						final CommodityZUtility czu = it.next();
						czu.allocateQuantityToFlowsAndExchanges();
					}
				}
			}
			catch (final OverflowException e) {
				error = e;
				logger.warn("Overflow error in Bc,z,k and Sc,z,k calculations");
			}
			logger.info("Finished allocating " + commodity);
			if (error != null) {
				setResult(error);
			}
			else {
				commodity.setFlowsValid(true);
				if (logger.isDebugEnabled()) {
					logger
							.debug("Calculating surpus and derivative for returning to client process");
				}
				final SandDResults sAndD = calculateSurplusAndDerivatives();
				if (calculatingSizeTerms) {
					calculateSizeTerms();
				}
				setResult(sAndD);
				if (logger.isDebugEnabled()) {
					logger.debug("Task finished for " + commodity);
				}
			}
		}
		catch (final RuntimeException e1) {
			System.out.println("Error in JPPF task " + e1);
			logger.fatal("JPPF Task didn't work", e1);
			System.out.println("JPPF Task didn't work " + e1);
			throw e1;
		}
	}

	private void calculateSizeTerms() {
		sizeTerms = new double[2][commodity.getAllExchanges().size()];
		for (final Exchange x : commodity.getAllExchanges()) {
			if (!commodity.isFloorspaceCommodity()) {
				x.setSizeTermsBasedOnCurrentQuantities(1.0);
			}
			sizeTerms[0][x.getExchangeLocationIndex()] = x.getBuyingSizeTerm();
			sizeTerms[1][x.getExchangeLocationIndex()] = x.getSellingSizeTerm();
		}
	}

	private void setInitialSizeTermsTo1() {
		for (final Exchange x : commodity.getAllExchanges()) {
			if (!commodity.isFloorspaceCommodity()) {
				x.setBuyingSizeTerm(1.0);
				x.setSellingSizeTerm(1.0);
			}
		}
	}

	/*
	 * Set the in memory objects based on the class double[] attributes. Does the
	 * opposite of what was done when the FlowAllocator was created with
	 * createFlowAllocator.
	 * 
	 * Runs on the node.
	 */
	public void setInputValuesInCommodityAndExchangeObjects()
			throws OverflowException {
		commodity.clearAllExchangeQuantities();// iterates through the exchange
		// objects
		// inside the commodity
		// object and sets the sell, buy qtys, surplus and the derivatives to 0

		commodity.setPriceInAllExchanges(prices);
		// Next go through and set the quantities and derivatives in the
		// appropriate CommodityZUtility object
		// and then allocate those quantities to flows and exchanges.

		// First set TC, dTC
		final Iterator<CommodityZUtility> bUtils = commodity
				.getBuyingUtilitiesIterator();
		while (bUtils.hasNext()) {
			final CommodityZUtility czu = bUtils.next();
			czu.setQuantity(totalConsumption[czu.getTaz().getZoneIndex()]);
			czu.setDerivative(dTotalConsumption[czu.getTaz().getZoneIndex()]);
		}

		// Next set TP, dTP
		final Iterator<CommodityZUtility> sUtils = commodity
				.getSellingUtilitiesIterator();
		while (sUtils.hasNext()) {
			final CommodityZUtility czu = sUtils.next();
			czu.setQuantity(totalProduction[czu.getTaz().getZoneIndex()]);
			czu.setDerivative(dTotalProduction[czu.getTaz().getZoneIndex()]);
		}
	}

	/*
	 * get output values for JPPF to return
	 */
	public SandDResults calculateSurplusAndDerivatives() {
		final SandDResults result = new SandDResults();
		result.surplusAndDerivative = new double[2][commodity
				.getNumBuyingUtilities()]; // sAndD[0][]=
		// surplus
		// in
		// each
		// exchange,
		// sAndD[1]
		// =
		// derivative
		// in
		// each
		// zone

		result.buyingAndSelling = new double[2][commodity.getNumBuyingUtilities()];

		boolean nanPresent = false;

		// calulate the surplus and derivatives in each exchange and buying and
		// selling totals.
		final Iterator exchanges = commodity.getAllExchanges().iterator();
		while (exchanges.hasNext() && !nanPresent) {
			final Exchange ex = (Exchange) exchanges.next();
			final int eIndex = ex.getExchangeLocationIndex();
			final double[] surplusAndDerivative = ex.exchangeSurplusAndDerivative();
			if (Double.isNaN(surplusAndDerivative[0])
					|| Double.isNaN(surplusAndDerivative[1])) {
				nanPresent = true;
				logger.warn("NaN present at " + ex);
			}
			result.surplusAndDerivative[0][eIndex] = surplusAndDerivative[0];
			result.surplusAndDerivative[1][eIndex] = surplusAndDerivative[1];
			result.buyingAndSelling[0][eIndex] = ex.boughtTotal();
			if (ex.boughtTotal() < 0) {
				// TODO removce this debug check
				throw new RuntimeException(ex + "bought total is negative "
						+ ex.boughtTotal());
			}
			result.buyingAndSelling[1][eIndex] = ex.soldTotal();
		}

		// Now return the surplus and derivatives for a particular commodity in
		// all exchange zones
		// to the SDWorkTask where the sAndD will be put into a message and sent
		// to the SDResultsQueue.
		// logger.info("Surplus and Derivative in each exchange zone has been
		// calculated for "+name+". Time in seconds:
		// "+(System.currentTimeMillis()-startTime)/1000.0);
		return result;
	}

	void putFlowResultsIntoMemory() {
		final Commodity c = Commodity.retrieveCommodity(commodityName);

		final FlowAllocator.SandDResults sAndD = (FlowAllocator.SandDResults) getResult();
		if (logger.isDebugEnabled()) {
			logger.debug("We have the results from flow allocation for " + c
					+ " now storing results back in the client machine");
		}
		final double[][] surplusAndDeriv = sAndD.surplusAndDerivative;
		final double[][] buyingAndSelling = sAndD.buyingAndSelling;

		final Iterator exchanges = c.getAllExchanges().iterator();
		while (exchanges.hasNext()) {
			final Exchange ex = (Exchange) exchanges.next();
			final int exIndex = ex.getExchangeLocationIndex();
			ex.setLastCalculatedSurplus(surplusAndDeriv[0][exIndex]);
			ex.setLastCalculatedDerivative(surplusAndDeriv[1][exIndex]);
			ex.setSurplusAndDerivativeValid(true);
			ex.setLastCalculatedBuyingTotal(buyingAndSelling[0][exIndex]);
			ex.setLastCalculatedSellingTotal(buyingAndSelling[1][exIndex]);
			ex.setBoughtAndSoldTotalsValid(true);
			if (calculatingSizeTerms) {
				ex.setBuyingSizeTerm(sizeTerms[0][ex.getExchangeLocationIndex()]);
				ex.setSellingSizeTerm(sizeTerms[1][ex.getExchangeLocationIndex()]);
			}
		}

		// NO the flows are not valid so don't do c.setFlowsValid(true);
		if (logger.isDebugEnabled()) {
			logger.debug("Finished allocating commodity " + c.name);
		}
	}

	/**
	 * Creates the flow object and populates its double array input objects from
	 * the in-memory objects. Runs on the Client machine Does the opposite of
	 * setInputValuesInCommodityAndExchangeObjects
	 * 
	 * @param c
	 * @return
	 */
	static FlowAllocator createFlowAllocator(Commodity c,
			boolean calculatingSizeTerms) {
		// get name
		final String cName = c.getName();

		// get TC and dTC
		// / total consumption and the derivative of total consumption with
		// respect to changes in zonal buyingZUtility
		final Iterator<CommodityZUtility> bUtils = c.getBuyingUtilitiesIterator();
		final int nBUtils = c.getNumBuyingUtilities();
		final double[] TC = new double[nBUtils];
		final double[] dTC = new double[nBUtils];

		while (bUtils.hasNext()) {
			final BuyingZUtility bUtil = (BuyingZUtility) bUtils.next();
			final int index = bUtil.getTaz().getZoneIndex();
			TC[index] = bUtil.getQuantity();
			dTC[index] = bUtil.getDerivative();
		}

		// get TP and dTP
		// total production and the derivative of total production with respect
		// to changes in zonal sellingZUtility
		final Iterator<CommodityZUtility> sUtils = c.getSellingUtilitiesIterator();
		final int nSUtils = c.getNumSellingUtilities();
		final double[] TP = new double[nSUtils];
		final double[] dTP = new double[nSUtils];
		while (sUtils.hasNext()) {
			final SellingZUtility sUtil = (SellingZUtility) sUtils.next();
			final int index = sUtil.getTaz().getZoneIndex();
			TP[index] = sUtil.getQuantity();
			dTP[index] = sUtil.getDerivative();
		}

		// get Price
		final Iterator exchanges = c.getAllExchanges().iterator();
		final double[] price = new double[nSUtils];
		while (exchanges.hasNext()) {
			final Exchange ex = (Exchange) exchanges.next();
			final int index = ex.getExchangeLocationIndex();
			price[index] = ex.getPrice();
		}
		return new FlowAllocator(cName, TP, TC, dTP, dTC, price,
				calculatingSizeTerms);
	}

}
