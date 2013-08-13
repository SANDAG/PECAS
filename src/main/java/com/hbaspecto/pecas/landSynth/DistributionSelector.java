/*
 * Copyright  2005 PB Consult Inc.
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
package com.hbaspecto.pecas.landSynth;

import org.apache.log4j.Logger;
import com.pb.common.util.SeededRandom;

/**
 * Created by IntelliJ IDEA.
 * @author Christi Willison
 * Date: Oct 3, 2005
 *
 * Tool for doing a Monte Carlo selection by passing in only
 * a probability matrix or for selecting from a uniform distribution
 * by passing in the lower and upper bound of the interval.
 */
public class DistributionSelector {


    /**
     * The method assumes that the probability array sums to 1
     *
     * @param probabilities
     * @return  returnValue - the index of the array where the cummulative
     *                        probability first exceeds the random number.
     * @throws RuntimeException if a return value cannot be found.
     */
    public static int getMonteCarloSelection( double[] probabilities) throws RuntimeException {
        double randomNumber = SeededRandom.getRandom();
        int returnValue = -1;

        double sum = 0.00;
        for (int i=0; i < probabilities.length; i++) {
            sum += probabilities[i];
            if (randomNumber <= sum)
                return i;
        }

        if (returnValue < 0) {
            throw new RuntimeException("getMonteCarloSelection has randomNumber=" + randomNumber +", sum=" + sum);
        }

        return returnValue;
    }

    /**
     * The method assumes that the probability array sums to 1.  It is
     * provided since TableDataSets deal in floats and sometimes
     * the probabilities are contained in a TableDataSet.
     *
     * @param probabilities
     * @return  returnValue - the index of the array where the cummulative
     *                        probability first exceeds the random number.
     * @throws RuntimeException if a return value cannot be found.
     */
    public static int getMonteCarloSelection( float[] probabilities) throws RuntimeException {
        float randomNumber = SeededRandom.getRandomFloat();
        int returnValue = -1;

        float sum = 0.0f;
        for (int i=0; i < probabilities.length; i++) {
            sum += probabilities[i];
            if (randomNumber <= sum)
                return i;
        }

        if (returnValue < 0) {
            throw new RuntimeException("getMonteCarloSelection has randomNumber=" + randomNumber +", sum=" + sum);
        }

        return returnValue;
    }

    /**
     * The method passes in the interval that defines the upper and
     * lower bounds of the uniform distribution and the method picks
     * a value in the interval.
     * @param a
     * @param b
     * @return a random number within the interval [a,b].
     */
    public static double selectFromUniformDistribution(double a, double b) {
        double randomNumber = SeededRandom.getRandom();
        return a + ((b-a)*randomNumber);
    }
}
