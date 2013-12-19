package com.hbaspecto.pecas.aa.control.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.hbaspecto.pecas.IResource;
import com.hbaspecto.pecas.aa.activities.ProductionActivity;

public class ProductionActivityExporter 
{
    public void writeActivityFiles(IResource resourceUtil, BufferedWriter activitiesFile, Collection<ProductionActivity> productionActivities, Logger logger)
    {
        try
        {
            activitiesFile.write("ActivityNumber,Activity\n");
            Iterator it = productionActivities.iterator();
            while (it.hasNext())
            {
                final ProductionActivity p = (ProductionActivity) it.next();
                activitiesFile.write(p.getNumber() + "," + p.name + "\n");
            }
            activitiesFile.close();
        } catch (final IOException e)
        {
            logger.warn("Can't write ActivityNumbers.csv and CommodityNumbers.csv, " + e);
        }
    }
}
