/*
 *  Copyright 2007 HBA Specto Incorporated
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
package com.hbaspecto.pecas.aa.travelAttributes;

public class LinearSkimFunctionEEOverride extends LinearFunctionOfSomeSkims
        implements TravelUtilityCalculatorInterface {

    boolean[] isOverRidden;
    double overRideValue;
    
    public LinearSkimFunctionEEOverride(int[] overRides, double overRideValue) {
        super();
        this.overRideValue = overRideValue;
        isOverRidden = new boolean[maxValue(overRides)+1];
        for (int i=0;i<isOverRidden.length;i++) {
            isOverRidden[i]=false;
        }
        for (int j=0;j<overRides.length;j++) {
            isOverRidden[overRides[j]]=true;            
        }
    }

    private static int maxValue(int[] anArray) {
        int maxValue =0;
        for (int i=0;i<anArray.length;i++) {
            maxValue = Math.max(maxValue,anArray[i]);            
        }
        return maxValue;
    }

    @Override
    public double getUtility(int origin, int destination, TravelAttributesInterface travelConditions) {
        if (origin<isOverRidden.length&&destination<isOverRidden.length) {
            if (isOverRidden[origin]&& isOverRidden[destination]) {
                return overRideValue;
            }
        }
        return super.getUtility(origin, destination, travelConditions);
    }

    @Override
    public double[] getUtilityComponents(int origin, int destination, TravelAttributesInterface travelConditions) {
        double[] wouldBe = super.getUtilityComponents(origin, destination, travelConditions);
        if (origin<isOverRidden.length&&destination<isOverRidden.length) {
            if (isOverRidden[origin]&& isOverRidden[destination]) {
                wouldBe[0] = overRideValue;
                for (int i=1;i<wouldBe.length;i++) {
                    wouldBe[i] = 0;
                }
            }
        }
        return wouldBe;
    }

    
}
