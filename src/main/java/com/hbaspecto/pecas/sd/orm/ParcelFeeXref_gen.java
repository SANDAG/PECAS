package com.hbaspecto.pecas.sd.orm;

import simpleorm.dataset.*;
import simpleorm.utils.*;
import simpleorm.sessionjdbc.SSessionJdbc;
import com.hbaspecto.pecas.land.Parcels;

/**
 * Base class of table parcel_fee_xref.<br>
 * Do not edit as will be regenerated by running SimpleORMGenerator Generated on
 * Fri Sep 25 16:13:29 MDT 2009
 ***/
abstract class ParcelFeeXref_gen
        extends SRecordInstance
        implements java.io.Serializable
{

    public static final SRecordMeta<ParcelFeeXref> meta           = new SRecordMeta<ParcelFeeXref>(
                                                                          ParcelFeeXref.class,
                                                                          "parcel_fee_xref");

    // Columns in table
    public static final SFieldLong                 PecasParcelNum = new SFieldLong(meta,
                                                                          "pecas_parcel_num",
                                                                          new SFieldFlags[] {
            SFieldFlags.SPRIMARY_KEY, SFieldFlags.SMANDATORY              });

    public static final SFieldInteger              FeeScheduleId  = new SFieldInteger(meta,
                                                                          "fee_schedule_id");

    public static final SFieldInteger              YearEffective  = new SFieldInteger(meta,
                                                                          "year_effective",
                                                                          new SFieldFlags[] {
            SFieldFlags.SPRIMARY_KEY, SFieldFlags.SMANDATORY              });

    // Column getters and setters
    public long get_PecasParcelNum()
    {
        return getLong(PecasParcelNum);
    }

    public void set_PecasParcelNum(long value)
    {
        setLong(PecasParcelNum, value);
    }

    public int get_FeeScheduleId()
    {
        return getInt(FeeScheduleId);
    }

    public void set_FeeScheduleId(int value)
    {
        setInt(FeeScheduleId, value);
    }

    public int get_YearEffective()
    {
        return getInt(YearEffective);
    }

    public void set_YearEffective(int value)
    {
        setInt(YearEffective, value);
    }

    // Foreign key getters and setters
    public DevelopmentFeeSchedules get_DEVELOPMENT_FEE_SCHEDULES(SSessionJdbc ses)
    {
        try
        {
            /**
             * Old code: return
             * DevelopmentFeeSchedules.findOrCreate(get_FeeScheduleId()); New
             * code below :
             **/
            return ses.findOrCreate(DevelopmentFeeSchedules.meta,
                    new Object[] {get_FeeScheduleId(),});
        } catch (SException e)
        {
            if (e.getMessage().indexOf("Null Primary key") > 0)
            {
                return null;
            }
            throw e;
        }
    }

    public void set_DEVELOPMENT_FEE_SCHEDULES(DevelopmentFeeSchedules value)
    {
        set_FeeScheduleId(value.get_FeeScheduleId());
    }

    public Parcels get_PARCELS(SSessionJdbc ses)
    {
        try
        {
            /**
             * Old code: return Parcels.findOrCreate(get_PecasParcelNum()); New
             * code below :
             **/
            return ses.findOrCreate(Parcels.meta, new Object[] {get_PecasParcelNum(),});
        } catch (SException e)
        {
            if (e.getMessage().indexOf("Null Primary key") > 0)
            {
                return null;
            }
            throw e;
        }
    }

    public void set_PARCELS(Parcels value)
    {
        set_PecasParcelNum(value.get_PecasParcelNum());
    }

    // Find and create
    public static ParcelFeeXref findOrCreate(SSessionJdbc ses, long _PecasParcelNum,
            int _YearEffective)
    {
        return ses.findOrCreate(meta, new Object[] {new Long(_PecasParcelNum),
                new Integer(_YearEffective)});
    }

    public static ParcelFeeXref findOrCreate(SSessionJdbc ses, Parcels _ref, int _YearEffective)
    {
        return findOrCreate(ses, _ref.get_PecasParcelNum(), _YearEffective);
    }

    public SRecordMeta<ParcelFeeXref> getMeta()
    {
        return meta;
    }
}
