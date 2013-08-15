package com.hbaspecto.pecas.aa.jppf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jppf.client.JPPFClient;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.DataProvider;
import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.aa.activities.AggregateActivity;
import com.hbaspecto.pecas.aa.activities.ProductionActivity;
import com.hbaspecto.pecas.aa.commodity.AbstractCommodity;
import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.hbaspecto.pecas.aa.commodity.CommodityZUtility;
import com.hbaspecto.pecas.aa.control.AveragePriceSurplusDerivativeMatrix;
import com.hbaspecto.pecas.zones.AbstractZone;
import com.hbaspecto.pecas.zones.PECASZone;

public class AveragePriceSurplusDerivativeMatrixJPPF extends
		AveragePriceSurplusDerivativeMatrix {
	
	public AveragePriceSurplusDerivativeMatrixJPPF(JPPFClient client, DataProvider propertiesDataProvider) {
		super();
		this.client = client;
		this.propertiesDataProvider = propertiesDataProvider;
	}

	JPPFClient client;
	DataProvider propertiesDataProvider;

	@Override
	protected void aggregateValuesFromEachActivity(double[][] myValues) {
		List<JPPFTask> activityInitializers = new ArrayList<JPPFTask>();

		AbstractZone[] zones = AbstractZone.getAllZones();
		double[][] commodityBuyingUtilities = new double[AbstractCommodity.getAllCommodities().size()][zones.length];
		double[][] commoditySellingUtilities = new double[AbstractCommodity.getAllCommodities().size()][zones.length];
		Iterator commodityIt = AbstractCommodity.getAllCommodities().iterator();
		//set up commodity z utilities;
		while (commodityIt.hasNext()) {
			Commodity c = (Commodity) commodityIt.next();
			int comNum = c.commodityNumber;
			for (int z=0;z<zones.length;z++) {
				try {
					CommodityZUtility bzu = c.retrieveCommodityZUtility(zones[z], false);
					commodityBuyingUtilities[comNum][zones[z].zoneIndex] = bzu.getUtility(1.0);
					CommodityZUtility szu = c.retrieveCommodityZUtility(zones[z], true);
					commoditySellingUtilities[comNum][zones[z].zoneIndex] = szu.getUtility(1.0);
				} catch (ChoiceModelOverflowException e) {
					String msg = "Problem calculating commodity zutilities, these should have been precalculated so this error shouldn't appear here";
					logger.fatal(msg,e);
					System.out.println(msg);
					e.printStackTrace();
					throw new RuntimeException(msg,e);
				}
			}
		}

        
        Iterator actIt = ProductionActivity.getAllProductionActivities().iterator();
        while (actIt.hasNext()) {
            ProductionActivity prodActivity = (ProductionActivity) actIt.next();
            if (prodActivity instanceof AggregateActivity) {
                AggregateActivity activity = (AggregateActivity) prodActivity;
                ActivityMatrixJPPFInitializer actInit = new ActivityMatrixJPPFInitializer(activity, matrixSize, matrixSize, AveragePriceSurplusDerivativeMatrix.numCommodities, commodityBuyingUtilities, commoditySellingUtilities);
                activityInitializers.add(actInit);
            }
        }

        try {
			activityInitializers = client.submit(activityInitializers, propertiesDataProvider);	
		} catch (Exception e) {
			logger.fatal("Can't submit job to JPPF");
			throw new RuntimeException("Can't submit job to JPPF", e);
		}
		

        for (JPPFTask task : activityInitializers) {
        	ActivityMatrixJPPFInitializer ainiter = (ActivityMatrixJPPFInitializer) task;
            Object done;
            done= ainiter.getResult();  // this will wait until its done
            if (done instanceof Boolean) {
                if ((Boolean) done == false) {
                    throw new RuntimeException("Problem in initializing one component of average derivative matrix");
                }
                // add this one in
                for (int r = 0; r<myValues.length; r++) {
                    for (int c = 0; c<myValues[r].length;c++) {
                        myValues[r][c]+=ainiter.dStorage[r][c];
                    }
                }
            } else {
                if (done instanceof Throwable) {
                    throw new RuntimeException((Throwable) done);
                } else {
                    throw new RuntimeException("Problem in initializing one component of average derivative matrix,"+ done);
                }
            }
        }
	}

}
