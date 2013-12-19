package com.hbaspecto.pecas.aa.control.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.hbaspecto.pecas.IResource;
import com.hbaspecto.pecas.aa.commodity.AbstractCommodity;
import com.hbaspecto.pecas.aa.commodity.Commodity;

public class CommodityExporter 
{
    public void writeCommodityFiles(IResource resourceUtil, Collection<AbstractCommodity> commodities, BufferedWriter commoditiesFile, Logger logger)
    {
        try
        {
            commoditiesFile.write("CommodityNumber,Commodity\n");
            Iterator it = commodities.iterator();
            while (it.hasNext())
            {
                final Commodity c = (Commodity) it.next();
                commoditiesFile.write(c.commodityNumber + "," + c.name + "\n");
            }
            commoditiesFile.close();
        } catch (final IOException e)
        {
            logger.warn("Can't write CommodityNumbers.csv, " + e);
        }
    }
}
