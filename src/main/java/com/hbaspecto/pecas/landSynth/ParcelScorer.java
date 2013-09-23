/*
 * Created on Dec 10, 2003
 * 
 * Copyright 2003 HBA Specto Incorporated
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.hbaspecto.pecas.landSynth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Properties;
import java.util.Random;
import java.util.ResourceBundle;
import com.pb.common.util.ResourceUtil;

/**
 * @author jabraham
 */
public class ParcelScorer
        implements Comparator
{

    final int             intCoverageType;
    final boolean         isIntCoverageType;
    final int             targetZone            = 0;
    final double          geogParam             = 0;
    // TODO add geographical restrictions/penalty
    private final double  farOverPenalty;
    // TODO add mechanism for user specified farOverPenalty (by use type?)
    private final double  farOverPenaltyKicksInAt;
    private final boolean isPowerFunction;
    private final double  farPenaltyPowerFactor;
    private final double  stdDeviation;
    private Long          randomPoolSeed;
    // TODO add mechanism for user specified point at which farOverPenalty
    // starts kicking in
    final double          coverageTypeMatches   = 0.5;
    final double          coverageTypeConflicts = -5.0;
    // TODO add mechanism for user specified coverageTypeMatches booster
    private final double  neighboringTaz;
    ArrayList             hintLists             = new ArrayList();

    // TODO here is the mechanism for user specied everything!
    ResourceBundle        props;

    // The TAZ that the parcels should be in.
    private int           currentTaz;

    public void clearScoreRecord()
    {
        // oldScores.clear();
    }

    static class HintList
    {
        final String   fieldName;
        final String[] fieldEntries;
        final double[] matchCoefficients;
        final double[] farCoefficients;  // floor area ratio coefficients

        HintList(String fieldName, int numberOfHints)
        {
            this.fieldName = fieldName;
            fieldEntries = new String[numberOfHints];
            matchCoefficients = new double[numberOfHints];
            farCoefficients = new double[numberOfHints];
        }

        HintList(String fieldName, String[] fieldEntries, double[] matchCoefficients,
                double[] farCoefficients)
        {
            this.fieldName = fieldName;
            this.fieldEntries = fieldEntries;
            this.matchCoefficients = matchCoefficients;
            this.farCoefficients = farCoefficients;
        }

    }

    // private HashMap<ParcelInterface, ScoreVersion> oldScores = new
    // HashMap<ParcelInterface, ScoreVersion>();

    static class ScoreVersion
    {
        double score;
        int    revision;
    }

    public double score(ParcelInterface c)
    {

        final double oldScore = c.getOldScore(intCoverageType);
        if (!Double.isNaN(oldScore))
        {
            return oldScore;
        }
        /*
         * ScoreVersion oldScore = oldScores.get(c); if(oldScore != null) { if(c.getRevision() == oldScore.revision) { return oldScore.score; } else {
         * oldScores.remove(c); } }
         */
        double score = 0;
        double farTarget = c.getInitialFAR();
        if (c.getTaz() == targetZone)
        {
            score += geogParam;
        }
        for (int hintListNumber = 0; hintListNumber < hintLists.size(); hintListNumber++)
        {
            final HintList hl = (HintList) hintLists.get(hintListNumber);
            String parcelValue = c.getValue(hl.fieldName);
            if (parcelValue == null)
            {
                parcelValue = "";
            }
            for (int i = 0; i < hl.fieldEntries.length; i++)
            {
                if (parcelValue.equals(hl.fieldEntries[i]))
                {
                    score += hl.matchCoefficients[i];
                    farTarget += hl.farCoefficients[i];
                }
            }
        }
        if (farTarget < 0)
        {
            farTarget = 0;
        }
        double far = 0;
        final double area = c.getSize();
        if (area == 0)
        {
            return Double.NEGATIVE_INFINITY;
        }
        far = c.getQuantity() / area;

        if (far > farTarget * farOverPenaltyKicksInAt)
        {
            if (isPowerFunction)
            {
                score -= Math.pow(far / farTarget - farOverPenaltyKicksInAt, farPenaltyPowerFactor)
                        * farOverPenalty;
            } else
            {
                // Use the linear old function.
                score -= (far - farTarget * farOverPenaltyKicksInAt) / farTarget * farOverPenalty;
            }
        }

        final RandomTerm randTerm = RandomTerm.getRandomTerm(randomPoolSeed);
        score += stdDeviation * randTerm.getNormalRandomNumber(c.getId(), c.getCoverage());

        final int currentCoverage = c.getCoverage();
        // TODO user specified coverage type match/conflict score adjustments

        // use isIntCoverageType variable to check the type of coverage
        if (!c.isVacantCoverege())
        {
            if (currentCoverage == intCoverageType)
            {
                score += coverageTypeMatches;
            } else
            {
                score += coverageTypeConflicts;
            }
        }

        // Penalty for parcel not in original TAZ.
        if (c.getTaz() != currentTaz)
        {
            score += neighboringTaz;
        }

        /*
         * ScoreVersion scoreRecord = new ScoreVersion(); scoreRecord.score = score; scoreRecord.revision = c.getRevision();
         * oldScores.put(c,scoreRecord);
         */
        c.setOldScore(intCoverageType, score);

        return score;
    }

    /**
	 * 
	 */
    public ParcelScorer(int intCoverage, boolean isIntSpaceType, ResourceBundle props2)
    {
        intCoverageType = intCoverage;
        isIntCoverageType = isIntSpaceType;
        props = props2;
        neighboringTaz = ResourceUtil.getDoubleProperty(props, "OutOfTazPenalty", 1);
        farOverPenalty = ResourceUtil.getDoubleProperty(props, "FarOverPenalty", 3);
        farOverPenaltyKicksInAt = ResourceUtil.getDoubleProperty(props, "FarRatioKicksInAt", 0.7);
        farPenaltyPowerFactor = ResourceUtil.getDoubleProperty(props, "FarPenaltyPowerFactor", 4);
        stdDeviation = ResourceUtil.getDoubleProperty(props, "StdDeviation", 0.1);
        isPowerFunction = ResourceUtil.getBooleanProperty(props, "IsPenaltyPowerFactor", false);

        final String strSeed = ResourceUtil.getProperty(props, "RandomPoolSeed");
        if (strSeed != null)
        {
            randomPoolSeed = Long.valueOf(strSeed);
        } else
        {
            randomPoolSeed = null;
        }
    }

    void addHint(HintList l)
    {
        hintLists.add(l);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public static boolean getBooleanWithDefaultValue(Properties props, String propName,
            boolean defaultValue)
    {
        final String prop = props.getProperty(propName);
        if (prop != null)
        {
            return Boolean.valueOf(prop);
        } else
        {
            return defaultValue;
        }
    }

    public static double getDoubleWithDefaultValue(Properties props, String propName,
            double defaultValue)
    {
        final String prop = props.getProperty(propName);
        if (prop != null)
        {
            return Double.valueOf(prop);
        } else
        {
            return defaultValue;
        }
    }

    @Override
    public int compare(Object o1, Object o2)
    {
        // FIXME Need a tie-breaking method for guaranteed correct operation of
        // sorting.
        if (o1 instanceof ParcelInterface && o2 instanceof ParcelInterface)
        {
            final double score1 = score((ParcelInterface) o1);
            final double score2 = score((ParcelInterface) o2);
            if (score1 < score2)
            {
                return -1;
            }
            if (score1 > score2)
            {
                return 1;
            }
            if (score2 == score1)
            {
                return 0;
            }
            throw new RuntimeException("Can't compare parcel scores " + score1 + " and " + score2);
        }
        throw new ClassCastException("Trying to compare non-parcels with ParcelSorter");
    }

    public void setCurrentTaz(int taz)
    {
        currentTaz = taz;
    }

    protected static class RandomTerm
    {
        private static RandomTerm randomTerm       = null;

        private Long              seed;
        private double[]          randomNormalPool = null;
        private final int         normalPoolSize   = 1000000;

        public static RandomTerm getRandomTerm(Long seed)
        {
            if (randomTerm != null)
            {
                return randomTerm;
            }
            randomTerm = new RandomTerm(seed);
            return randomTerm;
        }

        private RandomTerm(Long seed)
        {
            initializePool(seed);
        }

        private void initializePool(Long seed)
        {
            this.seed = seed;
            checkPool();
        }

        private void checkPool()
        {
            if (randomNormalPool == null)
            {
                randomNormalPool = new double[normalPoolSize];
                Random generator;
                if (seed != null)
                {
                    generator = new Random(seed);
                } else
                {
                    generator = new Random();
                }

                for (int i = 0; i < randomNormalPool.length; i++)
                {
                    randomNormalPool[i] = generator.nextGaussian();
                }
            }
        }

        public double getNormalRandomNumber(long pecasParcelID, int spaceTypeId)
        {
            checkPool();
            final int place = (int) ((pecasParcelID * 57 + spaceTypeId) % normalPoolSize);

            return randomNormalPool[place];
        }
    }

}
