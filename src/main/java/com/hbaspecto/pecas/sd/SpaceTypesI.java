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
package com.hbaspecto.pecas.sd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import simpleorm.dataset.SQuery;
import simpleorm.sessionjdbc.SSessionJdbc;
import com.hbaspecto.pecas.land.LandInventory;
import com.hbaspecto.pecas.sd.orm.DevelopmentFees;
import com.hbaspecto.pecas.sd.orm.SpaceTypesGroup;
import com.hbaspecto.pecas.sd.orm.SpaceTypesI_gen;
import com.hbaspecto.pecas.sd.orm.TransitionConstantsI;
import com.hbaspecto.pecas.sd.orm.TransitionCosts;

/**
 * A class that represents a type of land development
 * 
 * @author John Abraham
 */
public class SpaceTypesI
        extends SpaceTypesI_gen
        implements SpaceTypeInterface, java.io.Serializable
{

    protected static transient Logger            logger                        = Logger.getLogger(SDModel.class);
    private static HashMap<Integer, SpaceTypesI> spaceTypesHash                = new HashMap<Integer, SpaceTypesI>();
    private HashMap<Integer, Double>             fromTrConstCache              = new HashMap<Integer, Double>();
    private HashMap<Integer, Double>             toTrConstCache                = new HashMap<Integer, Double>();
    private static boolean                       isCached                      = false;
    private double                               utilityConstructionAdjustment = 0;

    public double getUtilityConstructionAdjustment()
    {
        return utilityConstructionAdjustment;
    }

    public void setUtilityConstructionAdjustment(double utilityConstructionAdjustment)
    {
        this.utilityConstructionAdjustment = utilityConstructionAdjustment;
    }

    public double cumulativeCostForAdd = 0, cumulativeCostForDevelopNew = 0;
    public double numberOfParcelsConsideredForAdd = 0, numberOfParcelsConsideredForDevelopNew = 0;

    public double cumulativeAmountOfDevelopment   = 0;

    public SpaceTypesI()
    {
        if (logger.isDebugEnabled())
            logger.debug("Setting up development type with no-argument constructor, lookup arrays will not be initialized");
    }

    public int getSpaceTypeID()
    {
        return this.get_SpaceTypeId();
    }

    public String toString()
    {
        try
        {
            return "DevelopmentType " + this.get_SpaceTypeName() + " (SpaceTypeCode: "
                    + this.get_SpaceTypeCode() + ", SpaceTypeID: " + this.get_SpaceTypeId() + ")";
        } catch (Exception e)
        {
        }
        return String.valueOf(this.get_SpaceTypeId());
    }

    public double getMaintenanceCost()
    {
        return get_MaintenanceCost();
    }

    public double getAgeMaintenanceCost()
    {
        return this.get_AgeMaintenanceCost();
    }

    public double getAdjustedMaintenanceCost(double age)
    {
        return getMaintenanceCost() * Math.pow(1 + getAgeMaintenanceCost(), age);
    }

    public double getRentDiscountFactor(double age)
    {
        return Math.pow(1 - this.get_AgeRentDiscount(), age);
    }

    @Override
    public String getName()
    {
        return this.get_SpaceTypeName();
    }

    public static SpaceTypesI getAlreadyCreatedSpaceTypeBySpaceTypeID(int coverage)
    {
        SpaceTypesI devType = spaceTypesHash.get(coverage);
        if (devType != null)
        {
            return devType;
        } else
        {
            SSessionJdbc session = SSessionJdbc.getThreadLocalSession();
            boolean wasBegun = true;
            if (!session.hasBegun())
            {
                session.begin();
                wasBegun = false;
            }
            devType = session.mustFind(SpaceTypesI.meta, coverage);
            spaceTypesHash.put(coverage, devType);
            if (!wasBegun) session.commit();
            return devType;
        }
    }

    public static Collection<SpaceTypesI> getAllSpaceTypes()
    {
        if (!isCached)
        {
            SSessionJdbc session = SSessionJdbc.getThreadLocalSession();
            boolean wasBegun = true;
            if (!session.hasBegun())
            {
                session.begin();
                wasBegun = false;
            }
            SQuery<SpaceTypesI> qry = new SQuery<SpaceTypesI>(SpaceTypesI.meta)
                    .ascending(SpaceTypeId);
            ArrayList<SpaceTypesI> list = session.query(qry);
            Iterator<SpaceTypesI> itr = list.iterator();
            while (itr.hasNext())
            {
                SpaceTypesI spaceType = itr.next();
                spaceTypesHash.put(spaceType.get_SpaceTypeId(), spaceType);
            }
            isCached = true;
            if (!wasBegun) session.commit();
        }

        return spaceTypesHash.values();
    }

    public static List<Integer> getAllSpaceTypesIDs()
    {
        if (!isCached)
        {
            SSessionJdbc session = SSessionJdbc.getThreadLocalSession();
            if (!session.hasBegun()) session.begin();
            SQuery<SpaceTypesI> qry = new SQuery<SpaceTypesI>(SpaceTypesI.meta);
            ArrayList<SpaceTypesI> list = session.query(qry);
            Iterator<SpaceTypesI> itr = list.iterator();
            while (itr.hasNext())
            {
                SpaceTypesI spaceType = itr.next();
                spaceTypesHash.put(spaceType.get_SpaceTypeId(), spaceType);
            }
            isCached = true;
        }
        List<Integer> list = new ArrayList<Integer>(spaceTypesHash.keySet());
        return list;
    }

    private TransitionCosts getTransitionCostsRecord(int spaceTypeID, int costScheduleID)
    {

        SSessionJdbc session = SSessionJdbc.getThreadLocalSession(); // this way
                                                                     // we are
                                                                     // using
                                                                     // the same
                                                                     // session
        boolean wasBegun = true;
        if (!session.hasBegun())
        {
            session.begin();
            wasBegun = false;
        }

        TransitionCosts transitionCost = session.mustFind(TransitionCosts.meta, costScheduleID,
                spaceTypeID);
        if (!wasBegun) session.commit();
        return transitionCost;
    }

    public double getConstructionCost(int costScheduleID)
    {
        TransitionCosts transCost = getTransitionCostsRecord(this.get_SpaceTypeId(), costScheduleID);
        return transCost.get_ConstructionCost();
    }

    public double getAdditionCost(int costScheduleID)
    {
        TransitionCosts transCost = getTransitionCostsRecord(this.get_SpaceTypeId(), costScheduleID);
        return transCost.get_AdditionCost();
    }

    public double getDemolitionCost(int costScheduleID)
    {
        TransitionCosts transCost = getTransitionCostsRecord(this.get_SpaceTypeId(), costScheduleID);
        return transCost.get_DemolitionCost();
    }

    public double getRenovationCost(int costScheduleID)
    {
        TransitionCosts transCost = getTransitionCostsRecord(this.get_SpaceTypeId(), costScheduleID);
        return transCost.get_RenovationCost();
    }

    public double getRenovationDerelictCost(int costScheduleID)
    {
        TransitionCosts transCost = getTransitionCostsRecord(this.get_SpaceTypeId(), costScheduleID);
        return transCost.get_RenovationDerelictCost();
    }

    private DevelopmentFees getDevelopmentFeesRecord(int spaceTypeID, int feeScheduleID)
    {

        SSessionJdbc session = SSessionJdbc.getThreadLocalSession(); // this way
                                                                     // we are
                                                                     // using
                                                                     // the same
                                                                     // session
        boolean wasBegun = true;
        if (!session.hasBegun())
        {
            session.begin();
            wasBegun = false;
        }

        DevelopmentFees developmentFee = session.mustFind(DevelopmentFees.meta, feeScheduleID,
                spaceTypeID);
        if (!wasBegun) session.commit();
        return developmentFee;
    }

    public double getDevlopmentFeePerUnitSpaceInitial(int feeScheduleID)
    {
        DevelopmentFees developmentFee = getDevelopmentFeesRecord(this.get_SpaceTypeId(),
                feeScheduleID);
        return developmentFee.get_DevelopmentFeePerUnitSpaceInitial();
    }

    public double getDevlopmentFeePerUnitLandInitial(int feeScheduleID)
    {
        DevelopmentFees developmentFee = getDevelopmentFeesRecord(this.get_SpaceTypeId(),
                feeScheduleID);
        return developmentFee.get_DevelopmentFeePerUnitLandInitial();
    }

    public double getDevlopmentFeePerUnitSpaceOngoing(int feeScheduleID)
    {
        DevelopmentFees developmentFee = getDevelopmentFeesRecord(this.get_SpaceTypeId(),
                feeScheduleID);
        return developmentFee.get_DevelopmentFeePerUnitSpaceOngoing();
    }

    public double getDevlopmentFeePerUnitLandOngoing(int feeScheduleID)
    {
        DevelopmentFees developmentFee = getDevelopmentFeesRecord(this.get_SpaceTypeId(),
                feeScheduleID);
        return developmentFee.get_DevelopmentFeePerUnitLandOngoing();
    }

    // Transition Constants methods
    private TransitionConstantsI cacheTransitionConstantsRecord(SSessionJdbc session, int id,
            boolean to)
    {
        int fromId;
        int toId;
        HashMap<Integer, Double> myCache;
        HashMap<Integer, Double> otherCache;
        if (to)
        {
            fromId = this.get_SpaceTypeId();
            toId = id;
            myCache = toTrConstCache;
            otherCache = getAlreadyCreatedSpaceTypeBySpaceTypeID(id).fromTrConstCache;
        } else
        {
            fromId = id;
            toId = this.get_SpaceTypeId();
            myCache = fromTrConstCache;
            otherCache = getAlreadyCreatedSpaceTypeBySpaceTypeID(id).toTrConstCache;
        }
        TransitionConstantsI transitionConstants = session.mustFind(TransitionConstantsI.meta,
                fromId, toId);
        myCache.put(id, transitionConstants.get_TransitionConstant());
        otherCache.put(this.get_SpaceTypeId(), transitionConstants.get_TransitionConstant());
        return transitionConstants;
    }

    public double getTransitionConstantTo(SSessionJdbc session, int to_SpaceTypeID)
    {
        Double value = toTrConstCache.get(to_SpaceTypeID);
        if (value != null) return value.doubleValue();
        else
        {
            TransitionConstantsI transitionConstants = cacheTransitionConstantsRecord(session,
                    to_SpaceTypeID, true);
            return transitionConstants.get_TransitionConstant();
        }
    }

    public double getTransitionConstantFrom(SSessionJdbc session, int from_existingSpaceTypeID)
    {
        Double value = fromTrConstCache.get(from_existingSpaceTypeID);
        if (value != null) return value.doubleValue();
        else
        {
            TransitionConstantsI transitionConstants = cacheTransitionConstantsRecord(session,
                    from_existingSpaceTypeID, false);
            return transitionConstants.get_TransitionConstant();
        }
    }

    /**
     * @deprecated
     */
    private void setTransitionConstant(SSessionJdbc session, int from, int to, double value)
    {
        TransitionConstantsI transitionConstants = session.mustFind(TransitionConstantsI.meta,
                from, to);
        transitionConstants.set_TransitionConstant(value);
    }

    /**
     * Not sure if this is the right way to do this...
     * 
     * @deprecated
     */
    public void setTransitionConstantTo(SSessionJdbc session, int to, double value)
    {
        setTransitionConstant(session, this.get_SpaceTypeId(), to, value);
        // Since the value has changed, re-cache it.
        cacheTransitionConstantsRecord(session, to, true);
    }

    /**
     * @deprecated
     */
    public void setTransitionConstantFrom(SSessionJdbc session, int from, double value)
    {
        setTransitionConstant(session, from, this.get_SpaceTypeId(), value);
        // Since the value has changed, re-cache it.
        cacheTransitionConstantsRecord(session, from, false);
    }

    public boolean isVacant()
    {
        return (get_SpaceTypeId() == LandInventory.VACANT_ID);
    }

    /**
     * @param spaceType
     * @return
     */
    public double getConstructionUtilityAdjustment(int batchCount, int numberOfBatches)
    {
        if (batchCount == 1) return 0;
        double trgt = SpaceTypesGroup.getTargetConstructionQuantity(get_SpaceTypeGroupId());
        double rateTarget = trgt / numberOfBatches;
        double rateObtained = SpaceTypesGroup
                .getObtainedConstructionQuantity(get_SpaceTypeGroupId()) / batchCount;

        double probChange = (numberOfBatches * rateTarget - batchCount * rateObtained)
                / ((numberOfBatches - batchCount) * rateObtained);
        probChange = Math.min(2, Math.max(probChange, 0.5));

        return 1 / (get_ConstructionCapacityTuningParameter()) * Math.log(probChange);
    }

    public static List<SpaceTypesI> getSpaceTypesBySpaceTypeGroup(int spaceTypeGroupId)
    {
        Collection<SpaceTypesI> spaceTypes = getAllSpaceTypes();
        ArrayList<SpaceTypesI> theOnes = new ArrayList<SpaceTypesI>();

        Iterator<SpaceTypesI> itr = spaceTypes.iterator();
        SpaceTypesI sp;

        while (itr.hasNext())
        {
            sp = itr.next();
            if (sp.get_SpaceTypeGroupId() == spaceTypeGroupId)
            {
                theOnes.add(sp);
            }
        }
        return theOnes;
    }
}
