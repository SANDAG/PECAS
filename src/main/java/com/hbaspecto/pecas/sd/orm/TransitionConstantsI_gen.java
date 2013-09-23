package com.hbaspecto.pecas.sd.orm;

import simpleorm.dataset.*;
import simpleorm.utils.*;
import simpleorm.sessionjdbc.SSessionJdbc;
import com.hbaspecto.pecas.sd.SpaceTypesI;

/**
 * Base class of table transition_constants_i.<br>
 * Do not edit as will be regenerated by running SimpleORMGenerator Generated on
 * Fri Sep 25 16:13:29 MDT 2009
 ***/
abstract class TransitionConstantsI_gen
        extends SRecordInstance
        implements java.io.Serializable
{

    public static final SRecordMeta<TransitionConstantsI> meta               = new SRecordMeta<TransitionConstantsI>(
                                                                                     TransitionConstantsI.class,
                                                                                     "transition_constants_i");

    // Columns in table
    public static final SFieldInteger                     FromSpaceTypeId    = new SFieldInteger(
                                                                                     meta,
                                                                                     "from_space_type_id",
                                                                                     new SFieldFlags[] {
            SFieldFlags.SPRIMARY_KEY, SFieldFlags.SMANDATORY                         });

    public static final SFieldInteger                     ToSpaceTypeId      = new SFieldInteger(
                                                                                     meta,
                                                                                     "to_space_type_id",
                                                                                     new SFieldFlags[] {
            SFieldFlags.SPRIMARY_KEY, SFieldFlags.SMANDATORY                         });

    public static final SFieldDouble                      TransitionConstant = new SFieldDouble(
                                                                                     meta,
                                                                                     "transition_constant");

    // Column getters and setters
    public int get_FromSpaceTypeId()
    {
        return getInt(FromSpaceTypeId);
    }

    public void set_FromSpaceTypeId(int value)
    {
        setInt(FromSpaceTypeId, value);
    }

    public int get_ToSpaceTypeId()
    {
        return getInt(ToSpaceTypeId);
    }

    public void set_ToSpaceTypeId(int value)
    {
        setInt(ToSpaceTypeId, value);
    }

    public double get_TransitionConstant()
    {
        return getDouble(TransitionConstant);
    }

    public void set_TransitionConstant(double value)
    {
        setDouble(TransitionConstant, value);
    }

    // Foreign key getters and setters
    public SpaceTypesI get_SPACE_TYPES_I(SSessionJdbc ses)
    {
        try
        {
            /**
             * Old code: return SpaceTypesI.findOrCreate(get_FromSpaceTypeId(),
             * get_ToSpaceTypeId()); New code below :
             **/
            return ses.findOrCreate(SpaceTypesI.meta, new Object[] {get_FromSpaceTypeId(),
                    get_ToSpaceTypeId(),});
        } catch (SException e)
        {
            if (e.getMessage().indexOf("Null Primary key") > 0)
            {
                return null;
            }
            throw e;
        }
    }

    public void set_SPACE_TYPES_I(SpaceTypesI value)
    {
        set_FromSpaceTypeId(value.get_SpaceTypeId());
        set_ToSpaceTypeId(value.get_SpaceTypeId());
    }

    // Find and create
    public static TransitionConstantsI findOrCreate(SSessionJdbc ses, int _FromSpaceTypeId,
            int _ToSpaceTypeId)
    {
        return ses.findOrCreate(meta, new Object[] {new Integer(_FromSpaceTypeId),
                new Integer(_ToSpaceTypeId)});
    }

    public static TransitionConstantsI findOrCreate(SSessionJdbc ses, SpaceTypesI _ref)
    {
        return findOrCreate(ses, _ref.get_SpaceTypeId(), _ref.get_SpaceTypeId());
    }

    public SRecordMeta<TransitionConstantsI> getMeta()
    {
        return meta;
    }
}
