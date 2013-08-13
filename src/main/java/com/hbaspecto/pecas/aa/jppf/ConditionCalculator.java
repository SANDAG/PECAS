package com.hbaspecto.pecas.aa.jppf;

import org.apache.log4j.Logger;
import org.jppf.server.protocol.JPPFTask;

import com.hbaspecto.models.FutureObject;
import com.hbaspecto.pecas.OverflowException;
import com.hbaspecto.pecas.aa.commodity.Commodity;

public class ConditionCalculator extends JPPFTask {

	double[] prices;
	double[][] zutilities=null;
	double[][] sizes;

	private static Logger logger = Logger.getLogger(ConditionCalculator.class);

	private transient Commodity myCommodity;
	String commodityName;
	//FutureObject worked = new FutureObject();

	ConditionCalculator(Commodity cParam) {
		myCommodity = cParam;
		commodityName = myCommodity.getName();
		this.prices = myCommodity.getPriceInAllExchanges();
		sizes = JppfAAModel.getSizeTerms(myCommodity);
	}

	public void run() {
		try{
			System.out.println("Starting condition calculator --- "+commodityName);
			//logger.info("Starting condition calculator "+commodityName);
			JppfNodeSetup.setup(getDataProvider());
			JppfAAModel.setSizeTerms(getMyCommodity(), sizes);

			// could be running on the node, better get the commodity from the name
			getMyCommodity().setPriceInAllExchanges(prices);

			try {
				zutilities =  getMyCommodity().fixPricesAndConditionsAtNewValues();
				//logger.info("Calculated zutilities, setting result");
			} catch (OverflowException e) {
				logger.error("Overflow error in condition calculator "+commodityName);
				setResult(e);
			}
			// set prices to null so that we don't bother sending them back
			prices = null;
			setResult(new Boolean(true));
		} catch (RuntimeException e1) {
			System.out.println("Error in JPPF task "+e1);
			logger.fatal("JPPF Task didn't work",e1);
			System.out.println("JPPF Task didn't work "+e1);
			throw e1;
		}
	}

	Commodity getMyCommodity() {
		if (myCommodity==null) myCommodity = Commodity.retrieveCommodity(commodityName);
		return myCommodity;
	}

}
