/*
 *  Copyright 2005 HBA Specto Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
/*
 * Created on Jan 3, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.hbaspecto.pecas.aa.control;

import drasys.or.matrix.DenseMatrix;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import com.hbaspecto.pecas.aa.activities.AggregateActivity;
import com.hbaspecto.pecas.aa.activities.ProductionActivity;
import com.hbaspecto.pecas.aa.commodity.AbstractCommodity;
import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.hbaspecto.pecas.aa.commodity.Exchange;

/**
 * @author jabraham
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class AveragePriceSurplusDerivativeMatrix extends DenseMatrix {

    
    protected static transient Logger logger =
        Logger.getLogger("com.pb.models.pecas");
    protected static int numCommodities;
    protected static int matrixSize=0;

    public static void calculateMatrixSize() {
        numCommodities = AbstractCommodity.getAllCommodities().size();
        matrixSize = numCommodities ;
    }
    
    private static Executor activityThreadPool; 
    
    public AveragePriceSurplusDerivativeMatrix() {
        super(matrixSize, matrixSize);
        
        if (matrixSize ==0) {
            throw new RuntimeException("Call BlockPriceSurplusMatrix.calculateMatrixSize() before instantiating BlockPriceSurplusMAtrix");
        }
    }


	public void init() {
		double[][] myValues = new double[matrixSize][matrixSize]; // it sucks to have to use a double[][] for temporary storage, but setElementAt and elementAt seem very slow
        
      	aggregateValuesFromEachActivity(myValues);

        
        
        // add import/export effect to diagonal
        Iterator comIt = AbstractCommodity.getAllCommodities().iterator();
        int comNum = 0;
        while (comIt.hasNext()) {
            Commodity c = (Commodity) comIt.next();
            double derivative = 0;
            Iterator exIt = c.getAllExchanges().iterator();
            while (exIt.hasNext()) {
                Exchange x = (Exchange) exIt.next();
                double[] importsAndExports = x.importsAndExports(x.getPrice());
                derivative += importsAndExports[2] - importsAndExports[3];
            }
            myValues[comNum][comNum] += derivative;
            comNum++;
        }
        this.setElements(new DenseMatrix(myValues));
	}


	protected void aggregateValuesFromEachActivity(double[][] myValues) {
		ArrayList<ActivityMatrixInitializer> activityInitializers = new ArrayList<ActivityMatrixInitializer>();
        
        Iterator actIt = ProductionActivity.getAllProductionActivities().iterator();
        while (actIt.hasNext()) {
            ProductionActivity prodActivity = (ProductionActivity) actIt.next();
            if (prodActivity instanceof AggregateActivity) {
                AggregateActivity activity = (AggregateActivity) prodActivity;
                ActivityMatrixInitializer actInit = new ActivityMatrixInitializer(activity, new double[matrixSize][matrixSize]);
                activityInitializers.add(actInit);
                getActivityThreadPool().execute(actInit);
            }
        }
        for (int anum = 0; anum < activityInitializers.size();anum++) {
            ActivityMatrixInitializer ainiter = activityInitializers.get(anum);
            Object done;
            try {
                done= ainiter.done.getValue();  // this will wait until its done
            } catch (InterruptedException e) {
                logger.fatal("Thread interrupted unexpectedly");
                e.printStackTrace();
                throw new RuntimeException(e);
            }
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


    private static Executor getActivityThreadPool() {
        if (activityThreadPool==null) activityThreadPool = Executors.newFixedThreadPool(ProductionActivity.getAllProductionActivities().size()); 
        return activityThreadPool;
    }
}
