package com.hbaspecto.pecas.sd.orm;

import simpleorm.dataset.SFieldDouble;
import simpleorm.dataset.SFieldFlags;
import simpleorm.dataset.SFieldInteger;
import simpleorm.dataset.SFieldString;
import simpleorm.dataset.SRecordInstance;
import simpleorm.dataset.SRecordMeta;
import simpleorm.sessionjdbc.SSessionJdbc;
import simpleorm.utils.SException;

/**
 * Base class of table construction_commodities.<br>
 * Do not edit as will be regenerated by running SimpleORMGenerator Generated on
 * Wed Dec 23 15:34:48 MST 2009
 ***/

abstract class ConstructionCommodities_gen
        extends SRecordInstance
        implements java.io.Serializable
{

    public static final SRecordMeta<ConstructionCommodities> meta              = new SRecordMeta<ConstructionCommodities>(
                                                                                       ConstructionCommodities.class,
                                                                                       "construction_commodities");

    // Columns in table
    public static final SFieldInteger                        CcCommodityId     = new SFieldInteger(
                                                                                       meta,
                                                                                       "cc_commodity_id",
                                                                                       new SFieldFlags[] {
            SFieldFlags.SPRIMARY_KEY, SFieldFlags.SMANDATORY                           });

    public static final SFieldString                         CcCommodityName   = new SFieldString(
                                                                                       meta,
                                                                                       "cc_commodity_name",
                                                                                       100);

    public static final SFieldInteger                        SpaceTypesGroupId = new SFieldInteger(
                                                                                       meta,
                                                                                       "space_types_group_id");

    public static final SFieldDouble                         ConvertingFactor  = new SFieldDouble(
                                                                                       meta,
                                                                                       "converting_factor");

    // Column getters and setters
    public int get_CcCommodityId()
    {
        return getInt(CcCommodityId);
    }

    public void set_CcCommodityId(int value)
    {
        setInt(CcCommodityId, value);
    }

    public String get_CcCommodityName()
    {
        return getString(CcCommodityName);
    }

    public void set_CcCommodityName(String value)
    {
        setString(CcCommodityName, value);
    }

    public int get_SpaceTypesGroupId()
    {
        return getInt(SpaceTypesGroupId);
    }

    public void set_SpaceTypesGroupId(int value)
    {
        setInt(SpaceTypesGroupId, value);
    }

    public double get_ConvertingFactor()
    {
        return getDouble(ConvertingFactor);
    }

    public void set_ConvertingFactor(double value)
    {
        setDouble(ConvertingFactor, value);
    }

    // Foreign key getters and setters
    public SpaceTypesGroup get_SPACE_TYPES_GROUP(SSessionJdbc ses)
    {
        try
        {
            /**
             * Old code: return
             * SpaceTypesGroup.findOrCreate(get_SpaceTypesGroupId()); New code
             * below :
             **/
            return ses.findOrCreate(SpaceTypesGroup.meta, new Object[] {get_SpaceTypesGroupId(),});
        } catch (SException e)
        {
            if (e.getMessage().indexOf("Null Primary key") > 0)
            {
                return null;
            }
            throw e;
        }
    }

    public void set_SPACE_TYPES_GROUP(SpaceTypesGroup value)
    {
        set_SpaceTypesGroupId(value.get_SpaceTypesGroupId());
    }

    // Find and create
    public static ConstructionCommodities findOrCreate(SSessionJdbc ses, int _CcCommodityId)
    {
        return ses.findOrCreate(meta, new Object[] {new Integer(_CcCommodityId)});
    }

    public SRecordMeta<ConstructionCommodities> getMeta()
    {
        return meta;
    }
}
