package com.hbaspecto.pecas.aa.jppf;

import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.jppf.server.protocol.JPPFTask;
import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.OverflowException;
import com.hbaspecto.pecas.aa.activities.AggregateActivity;
import com.hbaspecto.pecas.aa.activities.AmountInZone;
import com.hbaspecto.pecas.aa.activities.ProductionActivity;
import com.hbaspecto.pecas.aa.commodity.AbstractCommodity;
import com.hbaspecto.pecas.aa.commodity.BuyingZUtility;
import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.hbaspecto.pecas.aa.commodity.CommodityZUtility;
import com.hbaspecto.pecas.aa.commodity.SellingZUtility;
import com.hbaspecto.pecas.zones.AbstractZone;
import com.hbaspecto.pecas.zones.PECASZone;

/**
 * @author jabraham
 *
 */
class LocationProductionConsumptionJPPFAllocator extends JPPFTask{

	static final Logger logger = Logger.getLogger(LocationProductionConsumptionJPPFAllocator.class);

	final String activityName;
	/* inputs */
	/**
	 * Total quantity of activity to be allocated
	 */
	double totalQuantity;
	/**
	 * CommodityZUtilities of buying (price-weighted accessibility measures) by commodity and zone for buying stuff
	 */
	double[][] commodityBuyingUtilities;
	/**
	 * CommodityZUtilities of selling (price-weighted accessibility measures) by commodity and zone for selling stuff
	 */
	double[][] commoditySellingUtilities;
	
	double[][] sizesAndConstants;

	/* transient */
	
	/**
	 * Location to hold the activity we are dealing with, transient so that it is not sent as part of the message, rather
	 * it is retrieved from the name of the Activty instead.
	 */
	transient AggregateActivity activity = null;
	
	/* outputs */
	
	/**
	 * Allocation of activity into different zones
	 */
	double[] locationAllocation;
	double[][] buyingAmounts;
	double[][] buyingDerivatives;
	double[][] sellingAmounts;
	double[][] sellingDerivatives;
	

	@Override
    public void run() {
		try {
			JppfNodeSetup.setup(getDataProvider());

			activity = (AggregateActivity) ProductionActivity.retrieveProductionActivity(activityName);

			activity.setTotalAmount(totalQuantity);
			

			// TODO this is not thread safe, since the output values are stored in the CommodityZUtility objects, and
			// aggregated back on the client.  If two threads are running at once on the node, they will
			// partially aggregate the results on the node, and then they will be aggregate again
			// on the client, so double counted.
			// So we need to block and only use one local thread on the node, until this is enhanced
			synchronized (this.getClass()) {

				// this could probably be out of the synchronized loop, all zUtilities should be the same,
				// unless it's still stuck doing an old (dead) allocation while it receives the request from a new one
				setZUtilities();
				JppfAAModel.setSizesAndConstants(activity, sizesAndConstants);
				
				// erase utilities, don't need to send them back to JPPF client
				commodityBuyingUtilities=null;
				commoditySellingUtilities=null;
	
				CommodityZUtility.resetCommodityBoughtAndSoldQuantities();
				
				activity.reMigrationAndReAllocationWithOverflowTracking();
	
				
				getLocationAllocation();
				
				getSellingAndBuyingAmountsAndDerivatives();
			}
			logger.info("Finished calculating location for "+activityName);
			
			setResult(true);
			
		} catch (RuntimeException e1) {
			logger.fatal("JPPF Task didn't work",e1);
			System.out.println("JPPF Task didn't work "+e1);
			throw e1;
		} catch (OverflowException e) {
			logger.warn("Can't allocate "+activityName+" in JPPF node", e);
			setResult(e);
		}
	}

	private void getSellingAndBuyingAmountsAndDerivatives() {
		AbstractZone[] zones = AbstractZone.getAllZones();
		List commodityList = AbstractCommodity.getAllCommodities();
		
		buyingAmounts = new double[commodityList.size()][zones.length];
		buyingDerivatives = new double[commodityList.size()][zones.length];
		sellingAmounts = new double[commodityList.size()][zones.length];
		sellingDerivatives = new double[commodityList.size()][zones.length];
		
		Iterator comit = AbstractCommodity.getAllCommodities().iterator();
		while (comit.hasNext()) {
			Commodity c= (Commodity) comit.next();
			int comNum = c.commodityNumber;
			for (int z = 0;z<zones.length;z++) {
				buyingAmounts[comNum][zones[z].zoneIndex] = c.retrieveCommodityZUtility(zones[z], false).getQuantity();
				sellingAmounts[comNum][zones[z].zoneIndex] = c.retrieveCommodityZUtility(zones[z], true).getQuantity();
				buyingDerivatives[comNum][zones[z].zoneIndex] = c.retrieveCommodityZUtility(zones[z], false).getDerivative();
				sellingDerivatives[comNum][zones[z].zoneIndex] = c.retrieveCommodityZUtility(zones[z], true).getDerivative();			}
			
		}
	}

	private void getLocationAllocation() {
		AbstractZone[] zones = AbstractZone.getAllZones();
		locationAllocation = new double[zones.length];
		for (int z=0;z<zones.length;z++) {
			locationAllocation[z] = activity.myDistribution[zones[z].zoneIndex].getQuantity();
		}
	}

	/**
	 * Runs on the client to store the resulting allocation in the right place
	 * @throws OverflowException 
	 */
	void putActivityAllocationAmountsIntoMemory() throws OverflowException {
		activity = (AggregateActivity) ProductionActivity.retrieveProductionActivity(activityName);	

		if (logger.isDebugEnabled()) logger.debug("We have the results from activity allocation for "+activityName+" now storing results back in the client machine");
		
		AmountInZone[] locations = activity.myDistribution;
		
		for (int z=0;z<locations.length;z++) {
			locations[z].setQuantity(locationAllocation[locations[z].getMyTaz().zoneIndex]);
		}
		
		Iterator<AbstractCommodity> it = AbstractCommodity.getAllCommodities().iterator();
		while (it.hasNext()) {
			Commodity c = (Commodity) it.next();
			int commodityNum = c.commodityNumber;
			Iterator<CommodityZUtility> buying = c.getBuyingUtilitiesIterator();
			while (buying.hasNext()) {
				BuyingZUtility bzu = (BuyingZUtility) buying.next();
				bzu.changeQuantityBy(buyingAmounts[commodityNum][bzu.myTaz.zoneIndex]);
				bzu.changeDerivativeBy(buyingDerivatives[commodityNum][bzu.myTaz.zoneIndex]);
			}
			Iterator<CommodityZUtility> selling = c.getSellingUtilitiesIterator();
			while (selling.hasNext()) {
				SellingZUtility szu= (SellingZUtility) selling.next();
				szu.changeQuantityBy(sellingAmounts[commodityNum][szu.myTaz.zoneIndex]);
				szu.changeDerivativeBy(sellingDerivatives[commodityNum][szu.myTaz.zoneIndex]);
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Finished allocating activity " + activityName );
		}
	}


	
	/**
	 * Constructor to create the LocationProductionConsumption allocator on the client machine,
	 * will be serialized and sent over to the node.
	 * @param a the activity we are dealing with
	 * @throws ChoiceModelOverflowException
	 */
	LocationProductionConsumptionJPPFAllocator (AggregateActivity a) throws ChoiceModelOverflowException {
		activity = a;
		activityName = a.name;
		totalQuantity = a.getTotalAmount();
		AbstractZone[] zones = AbstractZone.getAllZones();
		commodityBuyingUtilities = new double[AbstractCommodity.getAllCommodities().size()][zones.length];
		commoditySellingUtilities = new double[AbstractCommodity.getAllCommodities().size()][zones.length];
		Iterator commodityIt = AbstractCommodity.getAllCommodities().iterator();
		//set up commodity z utilities;
		while (commodityIt.hasNext()) {
			Commodity c = (Commodity) commodityIt.next();
			int comNum = c.commodityNumber;
			for (int z=0;z<zones.length;z++) {
				CommodityZUtility bzu = c.retrieveCommodityZUtility(zones[z], false);
				commodityBuyingUtilities[comNum][zones[z].zoneIndex] = bzu.getUtility(1.0);
				CommodityZUtility szu = c.retrieveCommodityZUtility(zones[z], true);
				commoditySellingUtilities[comNum][zones[z].zoneIndex] = szu.getUtility(1.0);
			}
		}
		sizesAndConstants = JppfAAModel.getSizesAndConstants(a);
	}

	private void setZUtilities() {
		AbstractZone[] zones = AbstractZone.getAllZones();
		Iterator commodityIt = AbstractCommodity.getAllCommodities().iterator();
		//store up commodity z utilities;
		while (commodityIt.hasNext()) {
			Commodity c = (Commodity) commodityIt.next();
			int comNum = c.commodityNumber;
			for (int z=0;z<zones.length;z++) {
				CommodityZUtility bzu = c.retrieveCommodityZUtility(zones[z], false);
				bzu.setLastCalculatedUtility(commodityBuyingUtilities[comNum][zones[z].zoneIndex]);
				bzu.setLastUtilityValid(true);
				bzu.setPricesFixed(true);
				CommodityZUtility szu = c.retrieveCommodityZUtility(zones[z], true);
				szu.setLastCalculatedUtility(commoditySellingUtilities[comNum][zones[z].zoneIndex]);
				szu.setLastUtilityValid(true);
				szu.setPricesFixed(true);
			}
		}
	}

}
