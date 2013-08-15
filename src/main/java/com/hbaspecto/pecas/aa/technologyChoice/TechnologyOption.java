/*
 * Copyright  2006 HBA Specto Incorporated
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
package com.hbaspecto.pecas.aa.technologyChoice;

import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.hbaspecto.discreteChoiceModelling.Alternative;
import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.pb.common.datafile.TableDataSet;

public class TechnologyOption implements Alternative {
	
    static final Logger logger = Logger.getLogger("com.pb.models.pecas");
    int[] commodityIndexNumbers = null;
    final public String myName;

    final LogitTechnologyChoice myChoiceModel;
    class CommodityAmount {
        final Commodity commodity;
        final double amount;
        final double scale;
        
        CommodityAmount(Commodity c, double a, double scale) {
            commodity =c;
            amount =a;
            this.scale = scale;
            if (scale<0) {
                logger.fatal("Scale is negative in technology option");
                throw new RuntimeException("Scale is negative in technology option");
            }
        }

        @Override
        public String toString() {
            return commodity.name+"("+amount+")";
        }
    }
    
    double weight;
    double constant;
    
    ArrayList<CommodityAmount> commodityAmounts = new ArrayList<CommodityAmount>();
    
    public TechnologyOption(LogitTechnologyChoice myChoiceModel, double size, double constant, String name)  {
        myName = name;
        this.myChoiceModel=myChoiceModel;
        this.weight=size;
        this.constant=constant;
    }
    
    public void addCommodity(Commodity commodity, double amount, double scale) {
        if (amount !=0) {
            commodityAmounts.add(new CommodityAmount(commodity,amount,scale));
        }
    }
    
    /** sorts internally so that when calcAmounts() or overallUtility() are called with commodity utilities passed in as an array of doubles,
     * the CommodityQuantities object knows which utility vlaue applies to which commodity.  This also affects subsequent calls to commodityAt()
     * and must be called before commodityAt() is called.
     * @param commodityList an ordered list of commodities, commodity buying and selling utilities will be in this order
     */
    public void sortToMatch(java.util.List commodityList) {
        commodityIndexNumbers = new int[commodityAmounts.size()];
        for (int c=0;c<commodityIndexNumbers.length;c++) {
            boolean found=false;
            for (int c1=0;c1<commodityList.size();c1++) {
                if (((Commodity) commodityList.get(c1))==commodityAmounts.get(c).commodity) {
                    found = true;
                    commodityIndexNumbers[c]=c1;
                }
            }
            if (found==false) {
                String msg ="Can't find "+commodityAmounts.get(c).commodity+" in commodity list when aligning sort order for technology option";
                logger.fatal(msg);
                throw new RuntimeException(msg);
            }
        }
    }

    

    @Override
    public double getUtility(double dispersionParameterForSizeTermCalculation) throws ChoiceModelOverflowException {
        if (commodityIndexNumbers == null) {
            String msg = "For TechnologyOptions after creating options you need to call sortToMatch first before trying to calculate their utility";
            logger.fatal(msg);
            throw new RuntimeException(msg);
        }
        double[] buyingUtilities = myChoiceModel.buyingUtilities;
        double[] sellingUtilities = myChoiceModel.sellingUtilities;
        double utility =constant;
        utility += 1/dispersionParameterForSizeTermCalculation*Math.log(weight);
        double size = 1;
        for (int c=0;c<commodityAmounts.size();c++) {
            CommodityAmount ca = commodityAmounts.get(c);
            Commodity com = ca.commodity;
            if (ca.amount <0) {
                utility += -ca.amount*ca.scale*buyingUtilities[commodityIndexNumbers[c]];
                if (com.isFloorspaceCommodity()) {
                	// TODO should it be size *= or size +=?  If += should be scaled by -ca.amount*ca.scale
                	size *= myChoiceModel.getFloorspaceTypeImportanceForCurrentZone()[com.commodityNumber];
                }
            }
            if (ca.amount >0) {
                utility += ca.amount*ca.scale*sellingUtilities[commodityIndexNumbers[c]];
            }
        }
        utility += 1/dispersionParameterForSizeTermCalculation*Math.log(size);
        return utility;
    }
    
	public void printUtilityAndSizes(double dispersionParameterForSizeTermCalculation,PrintWriter out) throws ChoiceModelOverflowException {
        if (commodityIndexNumbers == null) {
            String msg = "For TechnologyOptions after creating options you need to call sortToMatch first before trying to calculate their utility";
            logger.fatal(msg);
            throw new RuntimeException(msg);
        }
        double[] buyingUtilities = myChoiceModel.buyingUtilities;
        double[] sellingUtilities = myChoiceModel.sellingUtilities;
        double utility =constant;
        double size = 1;
        for (int c=0;c<commodityAmounts.size();c++) {
            CommodityAmount ca = commodityAmounts.get(c);
            Commodity com = ca.commodity;
            if (ca.amount <0) {
                utility += -ca.amount*ca.scale*buyingUtilities[commodityIndexNumbers[c]];
                if (com.isFloorspaceCommodity()) {
                	// TODO should it be size *= or size +=?  If += should be scaled by -ca.amount*ca.scale
                	size *= myChoiceModel.getFloorspaceTypeImportanceForCurrentZone()[com.commodityNumber];
                }
            }
            if (ca.amount >0) {
                utility += ca.amount*ca.scale*sellingUtilities[commodityIndexNumbers[c]];
            }
        }
        double baseUtility = utility;
        utility += 1/dispersionParameterForSizeTermCalculation*Math.log(size);
        utility += 1/dispersionParameterForSizeTermCalculation*Math.log(weight);
        out.print(utility+","+baseUtility+","+weight+","+size);
	}


    public double[] getAmountsInOrder() {
        double[] amounts = new double[myChoiceModel.buyingUtilities.length];
        for (int c=0;c<commodityAmounts.size();c++) {
            amounts[commodityIndexNumbers[c]]+=commodityAmounts.get(c).amount;
        }
        return amounts;
    }
    
    public double[] getSellingPositiveAmountsInOrder() {
        // ENHANCEMENT save these matrices, they are constant once the model is running.
        // Save on garbage colleciton and processing that way.
        double[] amounts = new double[myChoiceModel.buyingUtilities.length];
        for (int c=0;c<commodityAmounts.size();c++) {
            double am= commodityAmounts.get(c).amount;
            if (am>0) amounts[commodityIndexNumbers[c]]+=am;
        }
        return amounts;
    }
    
    public double[] getBuyingNegativeAmountsInOrder() {
        // ENHANCEMENT save these matrices, they are constant once the model is running.
        // Save on garbage colleciton and processing that way.
        double[] amounts = new double[myChoiceModel.buyingUtilities.length];
        for (int c=0;c<commodityAmounts.size();c++) {
            double am= commodityAmounts.get(c).amount;
            if (am<0) amounts[commodityIndexNumbers[c]]+=am;
        }
        return amounts;
    }
    
    public double calculateVarianceTimes6DividedByPiSquared(TableDataSet productionTable, TableDataSet consumptionTable, int tableRow) {
    	double variance6pi = 0;
    	for (int a = 0;a<commodityAmounts.size(); a++) {
    		CommodityAmount ca = commodityAmounts.get(a);
    		double stdev =0;
    		if (ca.amount >0) {
    			stdev = ca.amount*ca.scale/ca.commodity.getDefaultSellingDispersionParameter();
                int column = productionTable.getColumnPosition(ca.commodity.name);
                if (column == -1) {
                    productionTable.appendColumn(new float[productionTable.getRowCount()],ca.commodity.name);
                    column = productionTable.checkColumnPosition(ca.commodity.name);
                }
                productionTable.setValueAt(tableRow,column,Math.max((float) stdev, productionTable.getValueAt(tableRow,column)));
    		} else if (ca.amount <0) {
                stdev = -ca.amount*ca.scale/ca.commodity.getDefaultBuyingDispersionParameter();
                int column = consumptionTable.getColumnPosition(ca.commodity.name);
                if (column == -1) {
                    consumptionTable.appendColumn(new float[consumptionTable.getRowCount()],ca.commodity.name);
                    column = consumptionTable.checkColumnPosition(ca.commodity.name);
                }
                consumptionTable.setValueAt(tableRow,column,Math.max((float) stdev, consumptionTable.getValueAt(tableRow,column)));
    		}
            variance6pi += stdev * stdev;
    	}
        return variance6pi;
    }

    public double[] getBuyingNegativeScaledAmountsInOrder() {
        double[] amounts = new double[myChoiceModel.buyingUtilities.length];
        for (int c=0;c<commodityAmounts.size();c++) {
            double am= commodityAmounts.get(c).amount;
            if (am<0) amounts[commodityIndexNumbers[c]]+=am*commodityAmounts.get(c).scale;
        }
        return amounts;
    }

    public double[] getSellingPositiveScaledAmountsInOrder() {
        double[] amounts = new double[myChoiceModel.buyingUtilities.length];
        for (int c=0;c<commodityAmounts.size();c++) {
            double am= commodityAmounts.get(c).amount;
            if (am>0) amounts[commodityIndexNumbers[c]]+=am*commodityAmounts.get(c).scale;
        }
        return amounts;
    }

    @Override
    public String toString() {
        return "TechnologyOption "+myName;
    }

}
