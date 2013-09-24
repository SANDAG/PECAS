/*
 * Copyright 2005 HBA Specto Incorporated
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.hbaspecto.discreteChoiceModelling;

import java.util.Random;
import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.NoAlternativeAvailable;

/**
 * 
 */
public abstract class DiscreteChoiceModel
{
    /** Picks one of the alternatives based on the logit model probabilities */
    public abstract Alternative monteCarloChoice() throws NoAlternativeAvailable,
            ChoiceModelOverflowException;

    /**
     * Picks one of the alternatives based on the logit model probabilities and
     * random number given
     */
    public abstract Alternative monteCarloChoice(double r) throws NoAlternativeAvailable,
            ChoiceModelOverflowException;

    public Alternative monteCarloElementalChoice() throws NoAlternativeAvailable,
            ChoiceModelOverflowException
    {
        Alternative a = monteCarloChoice();
        while (a instanceof DiscreteChoiceModel)
        {
            a = ((DiscreteChoiceModel) a).monteCarloChoice();
        }
        return a;
    }

    /** Use this method if you want to give a random number */
    public Alternative monteCarloElementalChoice(double r) throws NoAlternativeAvailable,
            ChoiceModelOverflowException
    {
        Alternative a = monteCarloChoice(r);
        final Random newRandom = new Random(new Double(r * 1000).longValue());
        while (a instanceof DiscreteChoiceModel)
        {
            a = ((DiscreteChoiceModel) a).monteCarloChoice(newRandom.nextDouble());
        }
        return a;
    }

    /**
     * @param a
     *            the alternative to add into the choice set
     */
    public abstract void addAlternative(Alternative a);

    public abstract Alternative alternativeAt(int i);

    public abstract double[] getChoiceProbabilities() throws ChoiceModelOverflowException,
            NoAlternativeAvailable;

    public abstract void allocateQuantity(double amount) throws ChoiceModelOverflowException;

}
