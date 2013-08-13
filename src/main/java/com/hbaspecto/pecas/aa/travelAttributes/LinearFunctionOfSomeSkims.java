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
 * Created on Mar 31, 2004
 *
 */
package com.hbaspecto.pecas.aa.travelAttributes;

import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * @author jabraham
 *
 */
public class LinearFunctionOfSomeSkims
    implements TravelUtilityCalculatorInterface {
    
    private static Logger logger = Logger.getLogger(LinearFunctionOfSomeSkims.class);
    
    SomeSkims lastSkims;
    
    ArrayList coefficientsList = new ArrayList();
    ArrayList namesList = new ArrayList();
    double[] coefficients = new double[0];
    int[] matrixIndices;

    /* (non-Javadoc)
     * @see com.pb.models.pecas.TravelUtilityCalculatorInterface#getUtility(com.pb.models.pecas.TravelAttributesInterface)
     */
    public double getUtility(int origin, int destination, TravelAttributesInterface travelConditions) {
        if (travelConditions == lastSkims) {
            // access by index number if the index numbers are already correct because we've been here before.
            double utility = 0;
            for (int i=0;i<coefficients.length;i++) {
                try {
                    utility += coefficients[i]*lastSkims.matrices[matrixIndices[i]].getValueAt(origin,destination);
                } catch (ArrayIndexOutOfBoundsException e) {
                    logger.fatal("Something wrong with "+namesList.get(matrixIndices[i]));
                    throw new RuntimeException("Something wrong with "+namesList.get(matrixIndices[i]),e);
                }
            }
            return utility;
        } 
        if (travelConditions instanceof SomeSkims) {
            // On the other hand, if the index numbers are not yet correct, then need to look up the matrices by name 
            lastSkims = (SomeSkims) travelConditions;
            matrixIndices = new int[namesList.size()];
            for (int i=0;i<namesList.size();i++) {
                // get matrix by name
                matrixIndices[i] = lastSkims.getMatrixId((String) namesList.get(i));
            }
            // now that we've set up all the index numbers to match the matrix names, we can call recursively to access by index number 
            return getUtility(origin, destination, lastSkims);
        }
        else throw new RuntimeException("Can't use LinearFunctionOfSomeSkims with travel attributes of type "+travelConditions.getClass());
    }
    
    public void addSkim(String name, double coefficient) {
        namesList.add(name);
        coefficientsList.add(new Double(coefficient));
        coefficients = new double[coefficientsList.size()];
        // store it in a double array for speed, and an ArrayList for resizing.
        for (int i=0;i<namesList.size();i++) {
            coefficients[i] = ((Double) (coefficientsList.get(i))).doubleValue();
        }
        
    }

    /* (non-Javadoc)
     * @see com.pb.models.pecas.TravelUtilityCalculatorInterface#getUtilityComponents(int, int, com.pb.models.pecas.TravelAttributesInterface)
     */
    public double[] getUtilityComponents(int origin, int destination, TravelAttributesInterface travelConditions) {
        if (travelConditions == lastSkims) {
            double[] components = new double[coefficients.length];
            for (int i=0;i<coefficients.length;i++) {
                components[i] = coefficients[i]*lastSkims.matrices[matrixIndices[i]].getValueAt(origin,destination);
            }
            return components;
        }
        if (travelConditions instanceof SomeSkims) {
            lastSkims = (SomeSkims) travelConditions;
            matrixIndices = new int[namesList.size()];
            for (int i=0;i<namesList.size();i++) {
                matrixIndices[i] = lastSkims.getMatrixId((String) namesList.get(i));
            }
            return getUtilityComponents(origin, destination, lastSkims);
        }
        else throw new RuntimeException("Can't use LinearFunctionOfSomeSkims with travel attributes of type "+travelConditions.getClass());
    }

}
