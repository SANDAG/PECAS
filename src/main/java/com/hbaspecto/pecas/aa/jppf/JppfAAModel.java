package com.hbaspecto.pecas.aa.jppf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

import org.jppf.client.JPPFClient;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.DataProvider;
import org.jppf.task.storage.MemoryMapDataProvider;

import com.hbaspecto.pecas.IResource;
import com.hbaspecto.pecas.aa.activities.AggregateActivity;
import com.hbaspecto.pecas.aa.activities.AmountInZone;
import com.hbaspecto.pecas.aa.commodity.AbstractCommodity;
import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.hbaspecto.pecas.aa.commodity.Exchange;
import com.hbaspecto.pecas.aa.control.AAModel;
import com.hbaspecto.pecas.aa.control.AveragePriceSurplusDerivativeMatrix;

public class JppfAAModel
        extends AAModel
{

    private JPPFClient   client;
    private DataProvider myDataProvider = null;

    public JppfAAModel(IResource resourceUtil, ResourceBundle aaRb)
    {
        super(resourceUtil, aaRb);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.hbaspecto.pecas.aa.control.AAModel#allocateQuantitiesToFlowsAndExchanges
     * (boolean)
     */
    @Override
    public boolean allocateQuantitiesToFlowsAndExchanges(boolean setSizeTerms)
    {
        // size terms need to be calculated all at once.
        if (setSizeTerms)
        {
            return super.allocateQuantitiesToFlowsAndExchanges(setSizeTerms);
        }
        // use JPPF version
        // return super.allocateQuantitiesToFlowsAndExchanges(setSizeTerms);
        if (logger.isDebugEnabled())
        {
            logger.debug("Beginning 'allocateQuantitiesToFlowsAndExchanges'");
        }
        final long startTime = System.currentTimeMillis();
        boolean nanPresent = false;
        // exchange objects
        // inside the commodity
        // objects and sets the sell, buy qtys and the derivatives to 0, in
        // anticipation of
        // receiving new values as a result of the JPPF task
        Commodity.clearAllCommodityExchangeQuantities(); // iterates through the

        final Iterator allComms = AbstractCommodity.getAllCommodities().iterator();
        List<JPPFTask> flowAllocators = new ArrayList<JPPFTask>();
        // creating a job with one task for each commodity.
        // ARM get all this working with JPPF
        while (allComms.hasNext())
        {
            final Commodity c = (Commodity) allComms.next();
            final FlowAllocator flower = FlowAllocator.createFlowAllocator(c, setSizeTerms);
            flowAllocators.add(flower);
        }

        try
        {
            flowAllocators = getClient().submit(flowAllocators, getPropertiesDataProvider());
        } catch (final Exception e)
        {
            logger.fatal("Can't submit job to JPPF");
            throw new RuntimeException("Can't submit job to JPPF", e);
        }

        // getting the results back.
        for (final JPPFTask task : flowAllocators)
        {
            if (task.getException() != null)
            {
                logger.fatal("Exception in JPPF task");
                throw new RuntimeException("Exception in JPPF task", task.getException());
            }
            final FlowAllocator flower = (FlowAllocator) task;
            Object worked;
            worked = flower.getResult();
            if (worked instanceof Exception)
            {
                logger.warn("Overflow error in Bc,z,k and Sc,z,k calculations for "
                        + flower.commodityName, (Exception) worked);
                nanPresent = true;
            } else
            {
                flower.putFlowResultsIntoMemory();
            }
        }
        logger.info("All commodities have been allocated.  Time in seconds: "
                + (System.currentTimeMillis() - startTime) / 1000.0);
        return nanPresent;
    }

    @Override
    protected AveragePriceSurplusDerivativeMatrix makeAveragePriceSurplusDerivativeMatrix()
    {
        // TOD use JPPF version below, don't delegate to superclass
        return super.makeAveragePriceSurplusDerivativeMatrix();
        // try {
        // AveragePriceSurplusDerivativeMatrixJPPF matrix = new
        // AveragePriceSurplusDerivativeMatrixJPPF(getClient(),
        // getPropertiesDataProvider());
        // matrix.init();
        // return matrix;
        // } catch (Exception e) {
        // logger.fatal("Can't create AveragePriceSurplusDerivativeMatrix",e);
        // throw new
        // RuntimeException("Can't create AveragePriceSurplusDerivativeMatrix",
        // e);
        // }
    }

    @Override
    protected boolean useJPPF()
    {
        return true;
    }

    private DataProvider getPropertiesDataProvider() throws Exception
    {
        if (myDataProvider != null)
        {
            return myDataProvider;
        }
        myDataProvider = new MemoryMapDataProvider();

        final Properties aaProps = propertiesFromResourceBundle(aaRb);

        myDataProvider.setValue("aaRb", aaProps);
        return myDataProvider;
    }

    private Properties propertiesFromResourceBundle(ResourceBundle rb)
    {
        final Properties props = new Properties();
        final Iterator it = rb.keySet().iterator();
        while (it.hasNext())
        {
            final String key = (String) it.next();
            final Object value = rb.getObject(key);
            if (!(value instanceof String))
            {
                throw new RuntimeException(
                        "Non string value in resource bundle, can't send via JPPF");
            } else
            {
                props.setProperty(key, (String) value);
            }
        }
        return props;
    }

    @Override
    public boolean calculateCompositeBuyAndSellUtilities()
    {
        // use JPPF version
        // return super.calculateCompositeBuyAndSellUtilities();
        if (logger.isDebugEnabled())
        {
            logger.debug("Entering 'calculateCompositeBuyAndSellUtilities'");
        }
        final long startTime = System.currentTimeMillis();
        Commodity.unfixPricesAndConditionsForAllCommodities();
        boolean nanPresent = false;
        final Iterator allOfUs = AbstractCommodity.getAllCommodities().iterator();

        List<JPPFTask> conditionCalculators = new ArrayList<JPPFTask>();

        while (allOfUs.hasNext())
        {
            final Commodity c = (Commodity) allOfUs.next();

            final ConditionCalculator calc = new ConditionCalculator(c);
            conditionCalculators.add(calc);
        }
        try
        {
            conditionCalculators = getClient().submit(conditionCalculators,
                    getPropertiesDataProvider());
        } catch (final Exception e)
        {
            logger.fatal("Can't submit job to JPPF");
            throw new RuntimeException("Can't submit job to JPPF", e);
        }
        // getting the results back.
        for (final JPPFTask task : conditionCalculators)
        {
            if (task.getException() != null)
            {
                logger.fatal("Exception in JPPF task");
                throw new RuntimeException("Exception in JPPF task", task.getException());
            }
            final ConditionCalculator condCalc = (ConditionCalculator) task;
            Object worked;
            worked = condCalc.getResult();
            if (worked instanceof Exception)
            {
                logger.warn("Overflow error in CUBuy, CUSell calcs " + condCalc.commodityName,
                        (Exception) worked);
                nanPresent = true;
            } else
            {
                condCalc.getMyCommodity().setCommodityZUtilities(condCalc.zutilities);
            }
        }
        logger.info("Composite buy and sell utilities have been calculated for all commodities. Time in seconds: "
                + (System.currentTimeMillis() - startTime) / 1000.0);
        return nanPresent;
    }

    @Override
    public boolean recalculateLocationConsumptionAndProduction()
    {
        // FIXME use JPPF version
        return super.recalculateLocationConsumptionAndProduction();
        // boolean nanPresent = false;
        // CommodityZUtility.resetCommodityBoughtAndSoldQuantities();
        // if (logger.isDebugEnabled()) {
        // logger.debug("Beginning CalculatingConsumptionAndProduction");
        // }
        // long startTime = System.currentTimeMillis();
        //
        // // receiving new values as a result of the JPPF task
        //
        // Iterator it =
        // ProductionActivity.getAllProductionActivities().iterator();
        // List<JPPFTask> allocators = new ArrayList<JPPFTask>();
        // while (it.hasNext()) {
        // AggregateActivity aa = (AggregateActivity) it.next();
        // LocationProductionConsumptionJPPFAllocator allocator;
        // try {
        // allocator = new LocationProductionConsumptionJPPFAllocator(aa);
        // allocators.add(allocator);
        // } catch (ChoiceModelOverflowException e) {
        // logger.error("Can't get utilities to send to JPPF location allocation task");
        // nanPresent = true;
        // }
        // }
        //
        // try {
        // allocators= getClient().submit(allocators,
        // getPropertiesDataProvider());
        // } catch (Exception e) {
        // logger.fatal("Can't submit job to JPPF");
        // throw new RuntimeException("Can't submit job to JPPF", e);
        // }
        //
        // // getting the results back.
        // for (JPPFTask task : allocators){
        // if (task.getException() != null) {
        // logger.fatal("Exception in JPPF task");
        // throw new RuntimeException("Exception in JPPF task",
        // task.getException());
        // }
        // LocationProductionConsumptionJPPFAllocator lpcAllocator =
        // (LocationProductionConsumptionJPPFAllocator) task;
        // Object worked;
        // worked = lpcAllocator.getResult();
        // if (worked instanceof Exception) {
        // logger.warn("Overflow error in Bc,z,k and Sc,z,k calculations for "+lpcAllocator.activityName,
        // (Exception) worked);
        // nanPresent= true;
        // } else {
        // try {
        // lpcAllocator.putActivityAllocationAmountsIntoMemory();
        // } catch (OverflowException e) {
        // nanPresent = true;
        // logger.fatal("Exception storing JPPF allocation results into client");
        // throw new
        // RuntimeException("Exception storing JPPF allocation results into client");
        // }
        // }
        // }
        // logger.info("All Activities have been allocated.  Time in seconds: "
        // + (System.currentTimeMillis() - startTime) / 1000.0);
        // return nanPresent;
    }

    private JPPFClient getClient()
    {
        if (client == null)
        {
            client = new JPPFClient();
        }
        return client;
    }

    static void setSizesAndConstants(AggregateActivity a, double[][] sizesAndConstants)
    {
        for (int i = 0; i < a.myDistribution.length; i++)
        {
            final AmountInZone amz = a.myDistribution[i];
            amz.setLocationSpecificUtilityInclTaxes(sizesAndConstants[amz.myTaz.zoneIndex][0]);
            amz.setAllocationSizeTerm(sizesAndConstants[amz.myTaz.zoneIndex][1]);
        }
    }

    static double[][] getSizesAndConstants(AggregateActivity a)
    {
        final double[][] sAndC = new double[a.myDistribution.length][2];
        for (int i = 0; i < a.myDistribution.length; i++)
        {
            final AmountInZone amz = a.myDistribution[i];
            sAndC[amz.myTaz.zoneIndex][0] = amz.getLocationSpecificUtilityInclTaxes();
            sAndC[amz.myTaz.zoneIndex][1] = amz.getAllocationSizeTerm();
        }
        return sAndC;
    }

    static double[][] getSizeTerms(Commodity commodity)
    {
        final double[][] sizeTerms = new double[2][commodity.getAllExchanges().size()];
        for (final Exchange x : commodity.getAllExchanges())
        {
            sizeTerms[0][x.getExchangeLocationIndex()] = x.getBuyingSizeTerm();
            sizeTerms[1][x.getExchangeLocationIndex()] = x.getSellingSizeTerm();
        }
        return sizeTerms;
    }

    static void setSizeTerms(Commodity commodity, double[][] sizeTerms)
    {
        for (final Exchange x : commodity.getAllExchanges())
        {
            x.setBuyingSizeTerm(sizeTerms[0][x.getExchangeLocationIndex()]);
            x.setSellingSizeTerm(sizeTerms[1][x.getExchangeLocationIndex()]);
        }
    }

}
