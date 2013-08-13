/*
 *  Copyright 2006 HBA Specto Incorporated
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.UpperSymmDenseMatrix;
import no.uib.cipr.matrix.sparse.FlexCompColMatrix;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;

import org.apache.log4j.Logger;

import com.hbaspecto.discreteChoiceModelling.Alternative;
import com.hbaspecto.discreteChoiceModelling.LogitModel;
import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.NoAlternativeAvailable;
import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.pb.common.datafile.TableDataSet;

public class LogitTechnologyChoice  extends LogitModel {
    
    final String name;

    public LogitTechnologyChoice(String name, boolean needToGenerateOwnOptions) {
        super();
        this.name = name;
    }
    
    @Override
    public String toString() {
        return "Technology choice for "+name;
    }

    //private double lambda;
    /**
     * Will be null if doFinalSetupAndSetCommodityOrder has not been called, or setup routines have been called after.
     */
    private List<Commodity> myCommodityOrder;
    static class AmountOfCommodity {
    	final Commodity commodity;
    	final double amount;
    	final double scale;
    	final double offset;
    	
    	AmountOfCommodity(Commodity c, double amount, double scale, double offset) {
    		commodity = c;
    		this.amount = amount;
    		this.scale = scale;
    		this.offset =offset;
    	}
    }
    public double[] buyingUtilities;
    public double[] sellingUtilities;
    private int currentZoneIndex;
    static final Logger logger = Logger.getLogger("com.pb.models.pecas");
    private double[] choiceProbabilitiesTemporaryStorage;
    private double[][] utilityDerivativesWrtCommodityUtilities;
    private double[][] technicalCoefficientsMatrix;
    private boolean[] allSames;
	private double[][] floorspaceProportionsByLUZ;
    
    public void setUtilities(double[] buyingUtilitiesParam, double[] sellingUtilitiesParam, int zoneIndex) {
        buyingUtilities=buyingUtilitiesParam;
        sellingUtilities=sellingUtilitiesParam;
        currentZoneIndex = zoneIndex;
    }
    
/*    public void setBuyingUtilities(double[] buyingUtilitiesParam) {
        buyingUtilities = buyingUtilitiesParam;
    }
    
    public void setSellingUtilities(double[] sellingUtilitiesParam) {
        sellingUtilities = sellingUtilitiesParam;
    }
*/
    
    /**
     * This method calculates the composite utility of all of the technology options available
     * to an Activity
     * @param buyingUtilitiesParam the buying utilities (composite utility of exchange choice for buying for each commodity)
     * @param sellingUtilitiesParam the selling utilities (composite utility of exchange choice for selling for each commodity)
     * @return the composite utility over all of the options
     * @throws ChoiceModelOverflowException
     */
    public double overallUtility(double[] buyingUtilitiesParam, double[] sellingUtilitiesParam, int zoneParam)
            throws ChoiceModelOverflowException {
        setUtilities(buyingUtilitiesParam, sellingUtilitiesParam, zoneParam);
        return this.getUtility(1.0);
    }

    public void doFinalSetupAndSetCommodityOrder(List commodityList) {
        if (myCommodityOrder !=null) {
            // already setup
            return;
        }
        myCommodityOrder = commodityList;
        buyingUtilities = new double[commodityList.size()];
        sellingUtilities = new double[commodityList.size()];
        // now set them all with the right order.
        for (int a=0;a<getAlternatives().size();a++) {
            ((TechnologyOption) getAlternatives().get(a)).sortToMatch(commodityList);
        }
    }

    
    public double[] calcBuyingAmounts(double[] buyingUtilitiesParam, double[] sellingUtilitiesParam,int zoneIndex) throws ChoiceModelOverflowException, NoAlternativeAvailable {
        setUtilities(buyingUtilitiesParam, sellingUtilitiesParam,zoneIndex);
        double[] probabilities = null;
        double[] amounts = new double[buyingUtilitiesParam.length];
        try {
            probabilities=getChoiceProbabilities();
        } catch (NoAlternativeAvailable e) {
        	probabilities = new double[getAlternatives().size()];
        	for (int i =0;i<probabilities.length;i++) {
              probabilities[i]= 1.0/probabilities.length;
        	}
        }
        for (int option =0;option<getAlternatives().size();option++) {
            TechnologyOption techOption = (TechnologyOption) alternativeAt(option);
            double[] optionAmounts = techOption.getBuyingNegativeAmountsInOrder();
            for (int c=0;c<amounts.length;c++) {
                amounts[c]+=probabilities[option]*optionAmounts[c];
            }
        }
        return amounts;
    }


    public double[] calcSellingAmounts(double[] buyingUtilitiesParam, double[] sellingUtilitiesParam, int zoneIndex) throws ChoiceModelOverflowException, NoAlternativeAvailable {
        setUtilities(buyingUtilitiesParam, sellingUtilitiesParam,zoneIndex);
        double[] amounts = new double[sellingUtilitiesParam.length];
        double[] probabilities;
        try {
            probabilities = getChoiceProbabilities();
        } catch (NoAlternativeAvailable e) {
        	probabilities = new double[getAlternatives().size()];
        	for (int i =0;i<probabilities.length;i++) {
              probabilities[i]= 1.0/probabilities.length;
        	}
        }
        for (int option =0;option<getAlternatives().size();option++) {
            TechnologyOption techOption = (TechnologyOption) alternativeAt(option);
            double[] optionAmounts = techOption.getSellingPositiveAmountsInOrder();
            for (int c=0;c<amounts.length;c++) {
                amounts[c]+=probabilities[option]*optionAmounts[c];
            }
        }
        return amounts;
    }

    @Override
    public void addAlternative(Alternative a) {
        if (!(a instanceof TechnologyOption)) {
            String msg = "Tried to add an alternative of type "+a.getClass().getName()+" to a LogitTechnologyChoice -- can only add TechnologyOptions";
            logger.error(msg);
            throw new RuntimeException(msg);
        }
        super.addAlternative(a);
        // signal that things aren't set up right yet.
        myCommodityOrder=null;
    }

    public void addTechnologyOption(TechnologyOption newOption) {
        this.addAlternative(newOption);
    }
    
	public void checkAndLogRatios(String name, TableDataSet productionTable, TableDataSet consumptionTable, int row) {
        // check through all options.
        double highestOption = 0;
        for (int o = 0; o<getAlternatives().size();o++) {
            TechnologyOption option = (TechnologyOption) getAlternatives().get(o);
            highestOption = Math.max(option.calculateVarianceTimes6DividedByPiSquared(productionTable,consumptionTable, row),highestOption);
        }
        double max = 1/Math.sqrt(highestOption);
        if (getDispersionParameter() > max) {
            logger.warn("Technology dispersion parameter too high for "+name+", maximum is "+max);
        }
        logger.info("Dispersion parameter ratio for "+this+" is "+getDispersionParameter()/max);
		
	}

    /**
     * Calculates the partial derivatives of quantities bought wrt utility of buying,
     * and the partial derivatives of quantities sold wrt utility of selling
     * @param buyingCommodityUtilities utility of buying different commodities
     * @param sellingCommodityUtilities utility of selling different commodities
     * @return First elements in matrix are the partials of buying (buying amounts are negative), next are partials of selling
     */
    public double[] amountsDerivatives(double[] buyingCommodityUtilities, double[] sellingCommodityUtilities, int zoneIndex) {
        setUtilities(buyingCommodityUtilities, sellingCommodityUtilities,zoneIndex);
        int size = buyingCommodityUtilities.length+sellingCommodityUtilities.length;
        double[][] diffFromAmountBaseline = getDifferenceAmountsFromBaseline(buyingCommodityUtilities, sellingCommodityUtilities,zoneIndex);
        double[] diagonal = new double[size];
        for (int i=0;i<size;i++) {
            if (getAllSames()[i]) {
                diagonal[i] = 0;
            } else {
                double value=0;
                for (int p=0;p<choiceProbabilitiesTemporaryStorage.length;p++) {
                    value +=choiceProbabilitiesTemporaryStorage[p]*getTechnicalCoefficientsMatrix()[i][p]*
                    diffFromAmountBaseline[i][p];
                }
                diagonal[i] = value*getDispersionParameter();
            }
        }
        return diagonal;
        
    }
    
    
    
    /**
     * Calculates the partial derivatives of quantities bought wrt utility of buying,
     * and the partial derivatives of quantities sold wrt utility of selling
     * @param buyingCommodityUtilities utility of buying different commodities
     * @param sellingCommodityUtilities utility of selling different commodities
     * @return First elements in matrix are the partials of buying, next are partials of selling
     */
    public Matrix amountsDerivativesMatrix(double[] buyingCommodityUtilities, double[] sellingCommodityUtilities, int zoneIndex) {

        int size = buyingCommodityUtilities.length+sellingCommodityUtilities.length;
        double[][] diffFromAmountBaseline = getDifferenceAmountsFromBaseline(buyingCommodityUtilities, sellingCommodityUtilities, zoneIndex);
        
        FlexCompColMatrix matrix = new FlexCompColMatrix(size,size);
        for (int i=0;i<size;i++) {
            for (int j=0;j<size;j++) {
                if (!getAllSames()[i] && !getAllSames()[j]) {
                    double value = 0;
                    for (int p=0;p<choiceProbabilitiesTemporaryStorage.length;p++) {
                        value +=choiceProbabilitiesTemporaryStorage[p]*getTechnicalCoefficientsMatrix()[i][p]*
                        diffFromAmountBaseline[j][p];
                    }
                    value*=getDispersionParameter();
                    matrix.set(i,j,value);
                }
            }
        }
        return matrix;
    }

    private double[][] getDifferenceAmountsFromBaseline(double[] buyingCommodityUtilities, double[] sellingCommodityUtilities, int zoneIndex) {
        setUtilities(buyingCommodityUtilities, sellingCommodityUtilities, zoneIndex);
        int size = buyingCommodityUtilities.length+sellingCommodityUtilities.length;
        try {
            choiceProbabilitiesTemporaryStorage = getChoiceProbabilities();
        } catch (ChoiceModelOverflowException e) {
            String error = "Overflow in choice derivative calculation "+e;
            // TODO should it error gracefully in case the step size is too large?
            logger.error(error);
            throw new RuntimeException(error);
        } catch (NoAlternativeAvailable e) {
            // set all probabilities to zero
            choiceProbabilitiesTemporaryStorage = new double[getAlternatives().size()];
            for (int i=0;i<choiceProbabilitiesTemporaryStorage.length;i++) choiceProbabilitiesTemporaryStorage[i]=0;
        }
        double[] amountBaseline = new double[size];
        double[][] diffFromAmountBaseline = new double[size][choiceProbabilitiesTemporaryStorage.length];
        for (int i=0;i<size;i++) {
            for (int p=0;p<choiceProbabilitiesTemporaryStorage.length;p++) {
                double current = getUtilityDerivativesWrtCommodityUtilities()[p][i];
                amountBaseline[i]+=choiceProbabilitiesTemporaryStorage[p]*current;
            }
            for (int p=0;p<choiceProbabilitiesTemporaryStorage.length;p++) {
                if (getAllSames()[i]) {
                    diffFromAmountBaseline[i][p]=0;
                } else {
                    diffFromAmountBaseline[i][p]=getUtilityDerivativesWrtCommodityUtilities()[p][i]-amountBaseline[i];
                }
            }
        }
        return diffFromAmountBaseline;
    }

    private double[][] getAmountsDerivativesWrtProbabilities() {
        double[][] matrix = new double[myCommodityOrder.size()*2][getAlternatives().size()];
        for (int o=0;o<getAlternatives().size();o++) {
            TechnologyOption techOption = (TechnologyOption) getAlternatives().get(o);
            double[] buyingAmounts = techOption.getBuyingNegativeAmountsInOrder();
            double[] sellingAmounts = techOption.getSellingPositiveAmountsInOrder();
            for (int i=0;i<myCommodityOrder.size();i++) {
                if (buyingAmounts[i]!=0) matrix[i][o]= buyingAmounts[i];
                if (sellingAmounts[i]!=0) matrix[i+myCommodityOrder.size()][o] =sellingAmounts[i];
            }
        }
        return matrix;
    }


    public double[][] getTechnicalCoefficientsMatrix() {
        if (technicalCoefficientsMatrix == null) {
            technicalCoefficientsMatrix = getAmountsDerivativesWrtProbabilities();
        }
        return technicalCoefficientsMatrix;
    }
    
    private double[][] getUtilityDerivativesWrtCommodityUtilities() {
        if (buyingUtilities ==null || sellingUtilities ==null) {
            logger.fatal("Need to set buying utilities and selling utilities at least once before initializing matrix");
        }
        if (utilityDerivativesWrtCommodityUtilities == null) {
            double[][] derivatives = new double[getAlternatives().size()][buyingUtilities.length+sellingUtilities.length];
            for (int o=0;o<getAlternatives().size();o++) {
                TechnologyOption techOption = (TechnologyOption) getAlternatives().get(o);
                double[] scaledBuying = techOption.getBuyingNegativeScaledAmountsInOrder();
                double[] scaledSelling = techOption.getSellingPositiveScaledAmountsInOrder();
                // for buying need to reverse the sign as the utility derivative is positive
                for (int i=0;i<buyingUtilities.length;i++) {
                    if (scaledBuying[i]!=0) derivatives[o][i] = -scaledBuying[i];
                }
                for (int i=0;i<sellingUtilities.length;i++) {
                    if (scaledSelling[i]!=0) derivatives[o][buyingUtilities.length+i] = scaledSelling[i];
                }
            }
            utilityDerivativesWrtCommodityUtilities=derivatives;
        }
        return utilityDerivativesWrtCommodityUtilities;
    }

    /**
     * Calculate the derivative of the logsum term wrt changes in buying utilities or selling utilities
     * @param buyingCommodityUtilities utility of buying different commodities
     * @param sellingCommodityUtilities utility of selling different commodities
     * @return first c elements are partial derivative of composite utility wrt changes in buyingCommodityUtilities, next c elements are wrt changes in sellingCommodityUtilities
     * @throws NoAlternativeAvailable 
     */
    public double[] compositeUtilityDerivatives(double[] buyingCommodityUtilities, double[] sellingCommodityUtilities, int zoneIndex) throws NoAlternativeAvailable {
        // equation for composite utility is equation 14 in TRB 2007 paper 
        // Random utility location/production/exchange choice, the additive logit model, and spatial choice microsimulations
        // by Abraham and Hunt
        // derivative wrt V~cl
        this.setUtilities(buyingCommodityUtilities, sellingCommodityUtilities, zoneIndex);
        double[] choiceProbabilities = null;
        try {
            choiceProbabilities = getChoiceProbabilities();
        } catch (NoAlternativeAvailable e) {
            String error = "Overflow calculating compositeUtilityDerivatives "+e;
            if (logger.isDebugEnabled()) logger.debug(e);
            // TODO check to see whether returning zero is ok.
            double[] utilityDerivatives = new double[buyingCommodityUtilities.length+sellingCommodityUtilities.length];
            return utilityDerivatives;
        } catch (ChoiceModelOverflowException e) {
            String error = "Overflow calculating compositeUtilityDerivatives "+e;
            logger.error(e);
            throw new RuntimeException(error,e);
        }
        double[] utilityDerivatives = new double[buyingCommodityUtilities.length+sellingCommodityUtilities.length];
        for (int o=0;o<getAlternatives().size();o++) {
            TechnologyOption option = (TechnologyOption) getAlternatives().get(o);
            double[] scaledAmounts = option.getBuyingNegativeScaledAmountsInOrder();
            for (int i=0;i<buyingCommodityUtilities.length;i++) {
                utilityDerivatives[i]+=choiceProbabilities[o]*scaledAmounts[i];
            }
            scaledAmounts = option.getSellingPositiveScaledAmountsInOrder();
            for (int i =0;i<sellingCommodityUtilities.length;i++) {
                utilityDerivatives[i+buyingCommodityUtilities.length]+=choiceProbabilities[o]*scaledAmounts[i];
            }
        }
        return utilityDerivatives;
    }

    public void writeOutOptionUtilities(int activityId, int zoneNumber, PrintWriter out) throws IOException, ChoiceModelOverflowException {
    	double[] probabilities;
		try {
			probabilities = getChoiceProbabilities();
		} catch (NoAlternativeAvailable e) {
			probabilities = new double[getAlternatives().size()];
		}
    	int i = 0;
        for (Object a: getAlternatives()) {
            TechnologyOption t = (TechnologyOption) a;
            out.print(activityId+","+zoneNumber+","+t.myName+",");
            t.printUtilityAndSizes(getDispersionParameter(),out);
            out.println(","+probabilities[i]);
            i++;
        }
    }

    private boolean[] getAllSames() {
        if (allSames == null ) {
            allSames = new boolean[getUtilityDerivativesWrtCommodityUtilities()[0].length];
            for (int i= 0;i<allSames.length;i++) {
                allSames[i]=true;
                double last = 0;
                for (int p=0;p<getAlternatives().size();p++) {
                    double current = getUtilityDerivativesWrtCommodityUtilities()[p][i];
                    if (p!=0 && current!=last) allSames[i]= false;
                    last = current;
                }
            }
        }
        return allSames;
    }

	public double[] getFloorspaceTypeImportanceForCurrentZone() {
		return floorspaceProportionsByLUZ[currentZoneIndex];
	}

	public List<Commodity> getMyCommodityOrder() {
		return myCommodityOrder;
	}

	public void setFloorspaceProportionsByLUZ(
			double[][] floorspaceInventoryByLUZ) {
		this.floorspaceProportionsByLUZ = floorspaceInventoryByLUZ;
	}

	public double[][] getFloorspaceProportionsByLUZ() {
		return floorspaceProportionsByLUZ;
	}
    
}
