package com.hbaspecto.pecas.sd.orm;

import simpleorm.dataset.*;
import simpleorm.utils.*;
import simpleorm.sessionjdbc.SSessionJdbc;
import java.math.BigDecimal;
import java.util.Date;
import com.hbaspecto.pecas.sd.SpaceTypesI;

/**
 * Base class of table space_to_commodity.<br>
 * Do not edit as will be regenerated by running SimpleORMGenerator Generated on Fri Sep 25 16:13:29 MDT 2009
 ***/
abstract class SpaceToCommodity_gen
        extends SRecordInstance
        implements java.io.Serializable
{

    public static final SRecordMeta<SpaceToCommodity> meta        = new SRecordMeta<SpaceToCommodity>(
                                                                          SpaceToCommodity.class,
                                                                          "space_to_commodity");

    // Columns in table
    public static final SFieldInteger                 SpaceTypeId = new SFieldInteger(meta,
                                                                          "space_type_id",
                                                                          new SFieldFlags[] {
            SFieldFlags.SPRIMARY_KEY, SFieldFlags.SMANDATORY              });

    public static final SFieldString                  AaCommodity = new SFieldString(meta,
                                                                          "aa_commodity",
                                                                          2147483647,
                                                                          new SFieldFlags[] {
            SFieldFlags.SPRIMARY_KEY, SFieldFlags.SMANDATORY              });

    public static final SFieldDouble                  Weight      = new SFieldDouble(
                                                                          meta,
                                                                          "weight",
                                                                          new SFieldFlags[] {SFieldFlags.SMANDATORY});

    // Column getters and setters
    public int get_SpaceTypeId()
    {
        return getInt(SpaceTypeId);
    }

    public void set_SpaceTypeId(int value)
    {
        setInt(SpaceTypeId, value);
    }

    public String get_AaCommodity()
    {
        return getString(AaCommodity);
    }

    public void set_AaCommodity(String value)
    {
        setString(AaCommodity, value);
    }

    public double get_Weight()
    {
        return getDouble(Weight);
    }

    public void set_Weight(double value)
    {
        setDouble(Weight, value);
    }

    // Foreign key getters and setters
    public SpaceTypesI get_SPACE_TYPES_I(SSessionJdbc ses)
    {
        try
        {
            /**
             * Old code: return SpaceTypesI.findOrCreate(get_SpaceTypeId()); New code below :
             **/
            return ses.findOrCreate(SpaceTypesI.meta, new Object[] {get_SpaceTypeId(),});
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
        set_SpaceTypeId(value.get_SpaceTypeId());
    }

    // Find and create
    public static SpaceToCommodity findOrCreate(SSessionJdbc ses, int _SpaceTypeId,
            String _AaCommodity)
    {
        return ses.findOrCreate(meta, new Object[] {new Integer(_SpaceTypeId), _AaCommodity});
    }

    public static SpaceToCommodity findOrCreate(SSessionJdbc ses, SpaceTypesI _ref,
            String _AaCommodity)
    {
        return findOrCreate(ses, _ref.get_SpaceTypeId(), _AaCommodity);
    }

    public SRecordMeta<SpaceToCommodity> getMeta()
    {
        return meta;
    }
}
