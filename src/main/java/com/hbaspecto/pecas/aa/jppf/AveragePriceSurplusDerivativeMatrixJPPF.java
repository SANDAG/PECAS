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

public class AveragePriceSurplusDerivativeMatrixJPPF
        extends AveragePriceSurplusDerivativeMatrix
{

    public AveragePriceSurplusDerivativeMatrixJPPF(JPPFClient client,
            DataProvider propertiesDataProvider)
    {
        super();
        this.client = client;
        this.propertiesDataProvider = propertiesDataProvider;
    }

    JPPFClient   client;
    DataProvider propertiesDataProvider;

    @Override
    protected void aggregateValuesFromEachActivity(double[][] myValues)
    {
        List<JPPFTask> activityInitializers = new ArrayList<JPPFTask>();

        final AbstractZone[] zones = AbstractZone.getAllZones();
        final double[][] commodityBuyingUtilities = new double[AbstractCommodity
                .getAllCommodities().size()][zones.length];
        final double[][] commoditySellingUtilities = new double[AbstractCommodity
                .getAllCommodities().size()][zones.length];
        final Iterator commodityIt = AbstractCommodity.getAllCommodities().iterator();
        // set up commodity z utilities;
        while (commodityIt.hasNext())
        {
            final Commodity c = (Commodity) commodityIt.next();
            final int comNum = c.commodityNumber;
            for (int z = 0; z < zones.length; z++)
            {
                try
                {
                    final CommodityZUtility bzu = c.retrieveCommodityZUtility(zones[z], false);
                    commodityBuyingUtilities[comNum][zones[z].zoneIndex] = bzu.getUtility(1.0);
                    final CommodityZUtility szu = c.retrieveCommodityZUtility(zones[z], true);
                    commoditySellingUtilities[comNum][zones[z].zoneIndex] = szu.getUtility(1.0);
                } catch (final ChoiceModelOverflowException e)
                {
                    final String msg = "Problem calculating commodity zutilities, these should have been "
                            + "precalculated so this error shouldn't appear here";
                    logger.fatal(msg, e);
                    System.out.println(msg);
                    e.printStackTrace();
                    throw new RuntimeException(msg, e);
                }
            }
        }

        final Iterator actIt = ProductionActivity.getAllProductionActivities().iterator();
        while (actIt.hasNext())
        {
            final ProductionActivity prodActivity = (ProductionActivity) actIt.next();
            if (prodActivity instanceof AggregateActivity)
            {
                final AggregateActivity activity = (AggregateActivity) prodActivity;
                final ActivityMatrixJPPFInitializer actInit = new ActivityMatrixJPPFInitializer(
                        activity, matrixSize, matrixSize,
                        AveragePriceSurplusDerivativeMatrix.numCommodities,
                        commodityBuyingUtilities, commoditySellingUtilities);
                activityInitializers.add(actInit);
            }
        }

        try
        {
            activityInitializers = client.submit(activityInitializers, propertiesDataProvider);
        } catch (final Exception e)
        {
            logger.fatal("Can't submit job to JPPF");
            throw new RuntimeException("Can't submit job to JPPF", e);
        }

        for (final JPPFTask task : activityInitializers)
        {
            final ActivityMatrixJPPFInitializer ainiter = (ActivityMatrixJPPFInitializer) task;
            Object done;
            done = ainiter.getResult(); // this will wait until its done
            if (done instanceof Boolean)
            {
                if ((Boolean) done == false)
                {
                    throw new RuntimeException(
                            "Problem in initializing one component of average derivative matrix");
                }
                // add this one in
                for (int r = 0; r < myValues.length; r++)
                {
                    for (int c = 0; c < myValues[r].length; c++)
                    {
                        myValues[r][c] += ainiter.dStorage[r][c];
                    }
                }
            } else
            {
                if (done instanceof Throwable)
                {
                    throw new RuntimeException((Throwable) done);
                } else
                {
                    throw new RuntimeException(
                            "Problem in initializing one component of average derivative matrix,"
                                    + done);
                }
            }
        }
    }

}
