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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import no.uib.cipr.matrix.DenseMatrix;
import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.Matrix;
import no.uib.cipr.matrix.Vector;
import org.apache.log4j.Logger;
import com.pb.common.util.SeededRandom;
import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.NoAlternativeAvailable;
import com.hbaspecto.pecas.sd.estimation.ExpectedValue;

public class LogitModel
        extends DiscreteChoiceModel
        implements ParameterSearchAlternative
{
    private static Logger          logger          = Logger.getLogger("com.pb.models.pecas");

    private double                 dispersionParameter;
    private Coefficient            wrappedDispersionParameter;
    private double                 constantUtility = 0;
    private Coefficient            wrappedConstantUtility;
    private ArrayList<Alternative> alternatives;

    private boolean                caching         = false;

    @Override
    public void allocateQuantity(double amount) throws ChoiceModelOverflowException
    {
        double[] probs;
        try
        {
            probs = getChoiceProbabilities();
        } catch (final NoAlternativeAvailable e)
        {
            throw new ChoiceModelOverflowException(
                    "Can't allocate quantity because all alternatives are unavailable", e);
        }
        for (int i = 0; i < probs.length; i++)
        {
            final Alternative a = alternativeAt(i);
            ((AggregateAlternative) a).setAggregateQuantity(amount * probs[i], amount * probs[i]
                    * (1 - probs[i]) * dispersionParameter);
        }
    }

    /**
     * @param a
     *            the alternative to add into the choice set
     */
    @Override
    public void addAlternative(Alternative a)
    {
        alternatives.add(a);
    }

    public boolean hasAlternatives()
    {
        return !alternatives.isEmpty();
    }

    public LogitModel()
    {
        alternatives = new ArrayList();
        dispersionParameter = 1.0;
    }

    // use this constructor if you know how many alternatives
    public LogitModel(int numberOfAlternatives)
    {
        alternatives = new ArrayList(numberOfAlternatives);
        dispersionParameter = 1.0;
    }

    private double  lastUtility;
    private boolean utilityCached = false;

    @Override
    public double getUtilityNoSizeEffect() throws ChoiceModelOverflowException
    {
        double sum = 0;
        int i = 0;
        while (i < alternatives.size())
        {
            sum += Math
                    .exp(getDispersionParameter() * alternatives.get(i).getUtilityNoSizeEffect());
            i++;
        }
        if (sum == 0)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("No alternative available for " + this
                        + ", returning -Infinity for logsum calculation");
            }
            return Double.NEGATIVE_INFINITY;
        }
        return 1 / getDispersionParameter() * Math.log(sum) + getConstantUtility();
    }

    /** @return the composite utility (log sum value) of all the alternatives */
    @Override
    public double getUtility(double higherLevelDispersionParameter)
            throws ChoiceModelOverflowException
    {
        if (utilityCached)
        {
            return lastUtility;
        }
        double sum = 0;
        int i = 0;
        while (i < alternatives.size())
        {
            sum += Math.exp(getDispersionParameter()
                    * alternatives.get(i).getUtility(getDispersionParameter()));
            i++;
        }
        if (sum == 0)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("No alternative available for " + this
                        + ", returning -Infinity for logsum calculation");
            }
            return Double.NEGATIVE_INFINITY;
        }
        final double bob = 1 / getDispersionParameter() * Math.log(sum);
        if (Double.isNaN(bob) || Double.isInfinite(bob))
        {
            logger.error("Choice model overflow error in composite utility calculation, denominator term is "
                    + sum);
            logger.error("Dispersion parameter is " + getDispersionParameter());
            int j = 0;
            while (j < alternatives.size())
            {
                final Alternative a = alternatives.get(j);
                logger.error("Alt " + a + " has utility " + a.getUtility(getDispersionParameter()));
                j++;
            }
            throw new ChoiceModelOverflowException("Overflow getting composite utility");
        }
        if (caching)
        {
            utilityCached = true;
        }
        lastUtility = bob + getConstantUtility();
        return lastUtility;
    }

    public double getDispersionParameter()
    {
        if (wrappedDispersionParameter == null)
        {
            return dispersionParameter;
        } else
        {
            return wrappedDispersionParameter.getValue();
        }
    }

    /**
     * Returns the coefficient object representing the dispersion parameter, if
     * this parameter was set using a coefficient object. Otherwise returns
     * null.
     * 
     * @return The coefficient object, or null if there isn't one.
     */
    public Coefficient getDispersionParameterAsCoeff()
    {
        return wrappedDispersionParameter;
    }

    public void setDispersionParameter(double dispersionParameter)
    {
        if (wrappedDispersionParameter == null)
        {
            this.dispersionParameter = dispersionParameter;
        } else
        {
            wrappedDispersionParameter.setValue(dispersionParameter);
        }
    }

    public void setDispersionParameterAsCoeff(Coefficient dispersionParameter)
    {
        wrappedDispersionParameter = dispersionParameter;
    }

    /**
     * Wraps the dispersion parameter in the given coefficient object.
     * 
     * @param wrapper
     *            A coefficient object that will hold the dispersion parameter.
     */
    public void wrapDispersionParameter(Coefficient wrapper)
    {
        wrapper.setValue(getDispersionParameter());
        wrappedDispersionParameter = wrapper;
    }

    private double[] lastProb;
    private boolean  probCached = false;

    @Override
    public double[] getChoiceProbabilities() throws ChoiceModelOverflowException,
            NoAlternativeAvailable
    {
        if (probCached)
        {
            return Arrays.copyOf(lastProb, lastProb.length);
        }
        synchronized (alternatives)
        {
            final double[] weights = new double[alternatives.size()];
            double sum = 0;
            final Iterator it = alternatives.listIterator();
            int i = 0;
            while (it.hasNext())
            {
                final Alternative a = (Alternative) it.next();
                final double utility = a.getUtility(getDispersionParameter());
                weights[i] = Math.exp(getDispersionParameter() * utility);
                if (Double.isNaN(weights[i]))
                {
                    throw new ChoiceModelOverflowException("NAN in weight for alternative " + a);
                }
                if (Double.isInfinite(weights[i]))
                {
                    throw new ChoiceModelOverflowException(weights[i] + " weight for alternative "
                            + a + ", Dispersion: " + getDispersionParameter() + ", Utility: "
                            + utility);
                }
                sum += weights[i];
                i++;
            }
            if (sum == 0)
            {
                throw new NoAlternativeAvailable("Denominator in " + this + " model is 0");
            }
            if (Double.isInfinite(sum))
            {
                throw new ChoiceModelOverflowException("Denominator in logit model is infinite");
            }
            for (i = 0; i < weights.length; i++)
            {
                weights[i] /= sum;
            }
            if (caching)
            {
                probCached = true;
            }
            lastProb = weights;
            return Arrays.copyOf(lastProb, lastProb.length);
        }
    }

    /**
     * Returns an array of the derivatives of the probabilities of each
     * alternative with respect to the utility of each alternative. (rows are
     * probabilities of alternatives, columns are utilities of alternatives)
     * 
     * @return
     * @throws ChoiceModelOverflowException
     */
    public double[][] choiceProbabilityDerivatives() throws ChoiceModelOverflowException
    {
        final double[] weights = new double[alternatives.size()];
        final double[][] derivatives = new double[weights.length][weights.length];
        synchronized (alternatives)
        {
            double sum = 0;
            final Iterator it = alternatives.listIterator();
            int i = 0;
            while (it.hasNext())
            {
                final Alternative a = (Alternative) it.next();
                final double utility = a.getUtility(getDispersionParameter());
                weights[i] = Math.exp(getDispersionParameter() * utility);
                if (Double.isNaN(weights[i]))
                {
                    throw new ChoiceModelOverflowException("NAN in weight for alternative " + a);
                }
                sum += weights[i];
                i++;
            }
            if (sum == 0)
            {
                // no alternative available
                // pretend derivatives are all zero
                return derivatives;
            }
            if (Double.isInfinite(sum))
            {
                throw new ChoiceModelOverflowException("Denominator in logit model is infinite");
            }
            for (i = 0; i < weights.length; i++)
            {
                weights[i] /= sum;
            }
            for (int a = 0; a < derivatives.length; a++)
            {
                derivatives[a][a] += getDispersionParameter() * weights[a];
                for (int p = 0; p < derivatives.length; p++)
                {
                    derivatives[a][p] -= getDispersionParameter() * weights[a] * weights[p];
                }
            }
            return derivatives;
        }
    }

    @Override
    public Alternative alternativeAt(int i)
    {
        return alternatives.get(i);
    } // should throw an error if out of range

    /** Picks one of the alternatives based on the logit model probabilities */
    @Override
    public Alternative monteCarloChoice() throws NoAlternativeAvailable,
            ChoiceModelOverflowException
    {
        synchronized (alternatives)
        {
            final double[] weights = new double[alternatives.size()];
            double sum = 0;
            final Iterator it = alternatives.listIterator();
            int i = 0;
            while (it.hasNext())
            {
                final double utility = ((Alternative) it.next())
                        .getUtility(getDispersionParameter());
                weights[i] = Math.exp(getDispersionParameter() * utility);
                if (Double.isNaN(weights[i]))
                {
                    throw new ChoiceModelOverflowException(
                            "in monteCarloChoice alternative was such that LogitModel weight was NaN");
                }
                sum += weights[i];
                i++;
            }
            if (sum == 0)
            {
                throw new NoAlternativeAvailable();
            }
            double randomNum = SeededRandom.getRandom();
            logger.debug("-----RANDOM NUMBER: " + randomNum + "----------"); 
            final double selector = randomNum * sum;
            sum = 0;
            for (i = 0; i < weights.length; i++)
            {
                sum += weights[i];
                if (selector <= sum)
                {
                    return alternatives.get(i);
                }
            }
            // yikes!
            throw new Error(
                    "Random Number Generator in Logit Model didn't return value between 0 and 1");
        }
    }

    /**
     * Picks one of the alternatives based on the logit model probabilities; use
     * this if you want to give method random number
     */
    @Override
    public Alternative monteCarloChoice(double randomNumber) throws NoAlternativeAvailable,
            ChoiceModelOverflowException
    {
        synchronized (alternatives)
        {
            final double[] weights = new double[alternatives.size()];
            double sum = 0;
            final Iterator it = alternatives.listIterator();
            int i = 0;
            while (it.hasNext())
            {
                final double utility = ((Alternative) it.next())
                        .getUtility(getDispersionParameter());
                weights[i] = Math.exp(getDispersionParameter() * utility);
                if (Double.isNaN(weights[i]))
                {
                    throw new ChoiceModelOverflowException(
                            "in monteCarloChoice alternative was such that LogitModel weight was NaN");
                }
                sum += weights[i];
                i++;
            }
            if (sum == 0)
            {
                throw new NoAlternativeAvailable();
            }
            double randomNum = SeededRandom.getRandom();
            logger.debug("-----RANDOM NUMBER: " + randomNum + "----------"); 
            final double selector = randomNum * sum;
            sum = 0;
            for (i = 0; i < weights.length; i++)
            {
                sum += weights[i];
                if (selector <= sum)
                {
                    return alternatives.get(i);
                }
            }
            // yikes!
            throw new Error(
                    "Random Number Generator in Logit Model didn't return value between 0 and 1");
        }
    }

    @Override
    public String toString()
    {
        final StringBuffer altsString = new StringBuffer();
        int alternativeCounter = 0;
        if (alternatives.size() > 5)
        {
            altsString.append("LogitModel with " + alternatives.size() + "alternatives {");
        } else
        {
            altsString.append("LogitModel, choice between ");
        }
        final Iterator it = alternatives.iterator();
        while (it.hasNext() && alternativeCounter < 5)
        {
            altsString.append(it.next());
            altsString.append(",");
            alternativeCounter++;
        }
        if (it.hasNext())
        {
            altsString.append("...}");
        } else
        {
            altsString.append("}");
        }
        return new String(altsString);
    }

    public double getConstantUtility()
    {
        if (wrappedConstantUtility == null)
        {
            return constantUtility;
        } else
        {
            return wrappedConstantUtility.getValue();
        }
    }

    /**
     * Returns the coefficient object representing the constant utility, if this
     * parameter was set using a coefficient object. Otherwise returns null.
     * 
     * @return The coefficient object, or null if there isn't one.
     */
    public Coefficient getConstantUtilityAsCoeff()
    {
        return wrappedConstantUtility;
    }

    public void setConstantUtility(double constantUtility)
    {
        if (wrappedConstantUtility == null)
        {
            this.constantUtility = constantUtility;
        } else
        {
            wrappedConstantUtility.setValue(constantUtility);
        }
    }

    public void setConstantUtilityAsCoeff(Coefficient constantUtility)
    {
        wrappedConstantUtility = constantUtility;
    }

    /**
     * Wraps the dispersion parameter in the given coefficient object.
     * 
     * @param wrapper
     *            A coefficient object that will hold the dispersion parameter.
     */
    public void wrapConstantUtility(Coefficient wrapper)
    {
        wrapper.setValue(getConstantUtility());
        wrappedConstantUtility = wrapper;
    }

    /**
     * Method arrayCoefficientSimplifiedChoice.
     * 
     * @param theCoefficients
     * @param theAttributes
     * @return int
     */
    public static int arrayCoefficientSimplifiedChoice(double[][] theCoefficients,
            double[] theAttributes)
    {

        final double[] utilities = new double[theCoefficients.length];
        int alt;
        for (alt = 0; alt < theCoefficients.length; alt++)
        {
            utilities[alt] = 0;
            for (int c = 0; c < theAttributes.length; c++)
            {
                utilities[alt] += theCoefficients[alt][c] * theAttributes[c];
            }
        }
        int denominator = 0;
        for (alt = 0; alt < utilities.length; alt++)
        {
            utilities[alt] = Math.exp(utilities[alt]);
            denominator += utilities[alt];
        }
        double randomNum = SeededRandom.getRandom();
        logger.debug("-----RANDOM NUMBER: " + randomNum + "----------"); 
        final double selector = randomNum * denominator;
        double cumulator = 0;
        for (alt = 0; alt < utilities.length; alt++)
        {
            cumulator += utilities[alt];
            if (selector <= cumulator)
            {
                return alt;
            }
        }
        // shouldn't happen
        return utilities.length - 1;
    }

    public double[] choiceProbabilityDerivativesWRTDispersion() throws ChoiceModelOverflowException
    {
        final double[] weights = new double[alternatives.size()];
        final double[] derivatives = new double[weights.length];
        final double[] utilities = new double[weights.length];
        synchronized (alternatives)
        {
            double sum = 0;
            final Iterator it = alternatives.listIterator();
            int i = 0;
            while (it.hasNext())
            {
                final Alternative a = (Alternative) it.next();
                utilities[i] = a.getUtility(getDispersionParameter());
                weights[i] = Math.exp(getDispersionParameter() * utilities[i]);
                if (Double.isNaN(weights[i]))
                {
                    throw new ChoiceModelOverflowException("NAN in weight for alternative " + a);
                }
                sum += weights[i];
                i++;
            }
            if (sum == 0)
            {
                // no alternative available
                // pretend derivatives are all zero
                return derivatives;
            }
            if (Double.isInfinite(sum))
            {
                throw new ChoiceModelOverflowException("Denominator in logit model is infinite");
            }
            for (i = 0; i < weights.length; i++)
            {
                weights[i] /= sum;
            }
            for (int a = 0; a < derivatives.length; a++)
            {
                derivatives[a] += weights[a];
                double thing1 = 0;
                double thing2 = 0;
                for (int p = 0; p < derivatives.length; p++)
                {
                    if (p != a && weights[p] > 0)
                    {
                        thing1 += utilities[a] * weights[p];
                        thing2 -= utilities[p] * weights[p];
                    }
                }
                derivatives[a] *= thing1 + thing2;
            }
            return derivatives;
        }
    }

    protected void setAlternatives(ArrayList<Alternative> alternatives)
    {
        this.alternatives = alternatives;
    }

    public ArrayList<Alternative> getAlternatives()
    {
        return alternatives;
    }

    private Vector  lastTarget;
    private boolean targetCached = false;

    @Override
    public Vector getExpectedTargetValues(List<ExpectedValue> ts) throws NoAlternativeAvailable,
            ChoiceModelOverflowException
    {
        if (targetCached)
        {
            return lastTarget.copy();
        }
        // Sum expected values for each alternative weighted by their
        // probabilities.
        final Vector values = new DenseVector(ts.size());

        final double[] probs = getChoiceProbabilities();
        int i = 0;
        for (final Alternative a : getAlternatives())
        {
            if (probs[i] > 0.0)
            {
                final ParameterSearchAlternative da = (ParameterSearchAlternative) a;
                final Vector altexpected = da.getExpectedTargetValues(ts);
                altexpected.scale(probs[i]);
                values.add(altexpected);
            }
            i++;
        }
        if (caching)
        {
            targetCached = true;
        }
        lastTarget = values;
        return lastTarget.copy();
    }

    private Vector  lastUtilDeriv;
    private boolean utilDerivCached = false;

    @Override
    public Vector getUtilityDerivativesWRTParameters(List<Coefficient> cs)
            throws NoAlternativeAvailable, ChoiceModelOverflowException
    {
        if (utilDerivCached)
        {
            return lastUtilDeriv.copy();
        }
        final int dispIndex = cs.indexOf(getDispersionParameterAsCoeff());
        final int constIndex = cs.indexOf(getConstantUtilityAsCoeff());

        double sumWeights = 0;
        double sumWeightsTimesUtilities = 0;
        final Vector sumWeightsTimesDerivatives = new DenseVector(cs.size());
        // Iterate through alternatives.
        final Iterator<Alternative> it = getAlternatives().iterator();
        while (it.hasNext())
        {
            final ParameterSearchAlternative da = (ParameterSearchAlternative) it.next();
            final double utility = da.getUtility(getDispersionParameter());
            final double weight = Math.exp(getDispersionParameter() * utility);
            final Vector derivatives = da.getUtilityDerivativesWRTParameters(cs);
            final Vector weightTimesDerivatives = derivatives.scale(weight);
            sumWeights += weight;
            if (!Double.isInfinite(utility))
            {
                sumWeightsTimesUtilities += weight * utility;
            }
            sumWeightsTimesDerivatives.add(weightTimesDerivatives);
        }

        // If there are no valid alternatives, the utility is always -infinity.
        // Return 0 for the derivatives.
        if (sumWeights == 0)
        {
            return new DenseVector(cs.size());
        }

        // Derivatives wrt normal parameters.
        sumWeightsTimesDerivatives.scale(1 / sumWeights);

        // Derivative wrt dispersion parameter.
        if (dispIndex >= 0)
        {
            final double term1 = sumWeightsTimesUtilities / (getDispersionParameter() * sumWeights);
            final double term2 = Math.log(sumWeights)
                    / (getDispersionParameter() * getDispersionParameter());
            final double dispderivative = term1 - term2;
            sumWeightsTimesDerivatives.set(dispIndex, dispderivative);
        }

        // Derivative wrt constant utility, which is always 1.
        if (constIndex >= 0)
        {
            sumWeightsTimesDerivatives.set(constIndex, 1);
        }

        if (caching)
        {
            utilDerivCached = true;
        }
        lastUtilDeriv = sumWeightsTimesDerivatives;
        return lastUtilDeriv.copy();
    }

    @Override
    public Matrix getExpectedTargetDerivativesWRTParameters(List<ExpectedValue> ts,
            List<Coefficient> cs) throws NoAlternativeAvailable, ChoiceModelOverflowException
    {
        final int numTargets = ts.size();
        final int numCoeffs = cs.size();
        final int numAlts = getAlternatives().size();

        final Matrix derivatives = new DenseMatrix(numTargets, numCoeffs);

        // find which coefficient is the dispersion parameter.
        final int dispIndex = cs.indexOf(getDispersionParameterAsCoeff());

        // derivative of probability wrt utility (dp by du)
        final double[][] probDerivativesWRTUtility = choiceProbabilityDerivatives();
        // derivative of probability wrt top-level dispersion parameter (dp by
        // dlambda)
        final double[] probDerivativesWRTDispersion = choiceProbabilityDerivativesWRTDispersion();
        final double[] probabilities = getChoiceProbabilities();
        // du by dc
        final Vector[] dUtilityBydParameters = new Vector[numAlts];
        // Set up du by dc.
        int i = 0;
        for (final Alternative a : getAlternatives())
        {
            final ParameterSearchAlternative da = (ParameterSearchAlternative) a;
            dUtilityBydParameters[i] = da.getUtilityDerivativesWRTParameters(cs);
            i++;
        }
        // Turn du by dc into a matrix.
        final Matrix dudct = new DenseMatrix(dUtilityBydParameters);
        Matrix dudc = new DenseMatrix(numAlts, numCoeffs);
        dudc = dudct.transpose(dudc);
        i = 0;
        for (final Alternative a : getAlternatives())
        {
            final double prob = probabilities[i];
            if (prob > 0.0)
            {
                final ParameterSearchAlternative da = (ParameterSearchAlternative) a;
                // derivative of target value wrt parameters.
                final Matrix dmdc = da.getExpectedTargetDerivativesWRTParameters(ts, cs);
                // expected target values.
                final Vector altexpected = da.getExpectedTargetValues(ts);
                // contribution to matrix is
                // (dmdc)p + m(dpdu . dudc)
                // These vectors have to be multiplied as matrices.
                final DenseMatrix m = new DenseMatrix(altexpected);
                // And this vector needs to be a row vector.
                final DenseMatrix dpdut = new DenseMatrix(new DenseVector(
                        probDerivativesWRTUtility[i]));
                DenseMatrix dpdu = new DenseMatrix(1, numAlts);
                dpdu = (DenseMatrix) dpdut.transpose(dpdu);

                final DenseMatrix term1 = (DenseMatrix) dmdc.scale(prob);
                DenseMatrix dpdc = new DenseMatrix(1, numCoeffs);
                DenseMatrix term2 = new DenseMatrix(numTargets, numCoeffs);
                dpdc = (DenseMatrix) dpdu.mult(dudc, dpdc);
                // Copy dispersion parameter derivative into this matrix.
                if (dispIndex >= 0)
                {
                    dpdc.set(0, dispIndex, probDerivativesWRTDispersion[i]);
                }

                term2 = (DenseMatrix) m.mult(dpdc, term2);
                final DenseMatrix altderivative = (DenseMatrix) term1.add(term2);

                // Add the results to the running total.
                derivatives.add(altderivative);
            }

            i++;
        }
        return derivatives;
    }

    @Override
    public void startCaching()
    {
        caching = true;
        for (final Alternative a : alternatives)
        {
            final ParameterSearchAlternative pa = (ParameterSearchAlternative) a;
            pa.startCaching();
        }
    }

    @Override
    public void endCaching()
    {
        caching = false;
        utilityCached = false;
        targetCached = false;
        probCached = false;
        utilDerivCached = false;
        for (final Alternative a : alternatives)
        {
            final ParameterSearchAlternative pa = (ParameterSearchAlternative) a;
            pa.endCaching();
        }
    }

}
