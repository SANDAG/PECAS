package com.hbaspecto.pecas.sd.orm;

import simpleorm.dataset.*;
import simpleorm.utils.*;
import simpleorm.sessionjdbc.SSessionJdbc;
import java.math.BigDecimal;
import java.util.Date;
import com.hbaspecto.pecas.land.Parcels;
import com.hbaspecto.pecas.sd.SpaceTypesI;
import com.hbaspecto.pecas.land.Tazs;

/**
 * Base class of table parcels.<br>
 * Do not edit as will be regenerated by running SimpleORMGenerator Generated on
 * Fri Sep 25 16:13:29 MDT 2009
 ***/
public abstract class ObservedDevelopmentEvents_gen
        extends SRecordInstance
        implements java.io.Serializable
{

    public static final SRecordMeta<ObservedDevelopmentEvents> meta                   = new SRecordMeta<ObservedDevelopmentEvents>(
                                                                                              ObservedDevelopmentEvents.class,
                                                                                              "observed_development_events");

    // Columns in table
    public static final SFieldString                           EventType              = new SFieldString(
                                                                                              meta,
                                                                                              "event_type",
                                                                                              2147483647);

    public static final SFieldString                           ParcelId               = new SFieldString(
                                                                                              meta,
                                                                                              "parcel_id",
                                                                                              2147483647);

    public static final SFieldLong                             OriginalPecasParcelNum = new SFieldLong(
                                                                                              meta,
                                                                                              "original_pecas_parcel_num",
                                                                                              new SFieldFlags[] {
            SFieldFlags.SPRIMARY_KEY, SFieldFlags.SMANDATORY                                  });

    public static final SFieldLong                             NewPecasParcelNum      = new SFieldLong(
                                                                                              meta,
                                                                                              "New_pecas_parcel_num");
    // ,new SFieldFlags[] { SFieldFlags.SPRIMARY_KEY, SFieldFlags.SMANDATORY });

    public static final SFieldInteger                          AvailableServices      = new SFieldInteger(
                                                                                              meta,
                                                                                              "available_services");

    public static final SFieldInteger                          OldSpaceTypeId         = new SFieldInteger(
                                                                                              meta,
                                                                                              "old_space_type_id");

    public static final SFieldInteger                          NewSpaceTypeId         = new SFieldInteger(
                                                                                              meta,
                                                                                              "new_space_type_id");

    public static final SFieldDouble                           OldSpaceQuantity       = new SFieldDouble(
                                                                                              meta,
                                                                                              "old_space_quantity");

    public static final SFieldDouble                           NewSpaceQuantity       = new SFieldDouble(
                                                                                              meta,
                                                                                              "new_space_quantity");

    public static final SFieldInteger                          OldYearBuilt           = new SFieldInteger(
                                                                                              meta,
                                                                                              "old_year_built");

    public static final SFieldInteger                          NewYearBuilt           = new SFieldInteger(
                                                                                              meta,
                                                                                              "new_year_built");

    public static final SFieldDouble                           LandArea               = new SFieldDouble(
                                                                                              meta,
                                                                                              "land_area");

    public static final SFieldBooleanBit                       OldIsDerelict          = new SFieldBooleanBit(
                                                                                              meta,
                                                                                              "old_is_derelict");

    public static final SFieldBooleanBit                       NewIsDerelict          = new SFieldBooleanBit(
                                                                                              meta,
                                                                                              "new_is_derelict");

    public static final SFieldBooleanBit                       OldIsBrownfield        = new SFieldBooleanBit(
                                                                                              meta,
                                                                                              "old_is_brownfield");

    public static final SFieldBooleanBit                       NewIsBrownfield        = new SFieldBooleanBit(
                                                                                              meta,
                                                                                              "new_is_brownfield");

    public static final SFieldInteger                          ZoningRulesCode        = new SFieldInteger(
                                                                                              meta,
                                                                                              "zoning_rules_code");

    public static final SFieldInteger                          Taz                    = new SFieldInteger(
                                                                                              meta,
                                                                                              "taz");

    // Column getters;
    // FIXME: fix thge letter case in the method names

    public String get_Eventtype()
    {
        return getString(EventType);
    }

    public String get_ParcelId()
    {
        return getString(ParcelId);
    }

    public Long get_Originalpecasparcelnum()
    {
        return getLong(OriginalPecasParcelNum);
    }

    public Long get_Newpecasparcelnum()
    {
        return getLong(NewPecasParcelNum);
    }

    public int get_AvailableServicesCode()
    {
        return getInt(AvailableServices);
    }

    public int get_Oldspacetypeid()
    {
        return getInt(OldSpaceTypeId);
    }

    public int get_NewSpaceIypeId()
    {
        return getInt(NewSpaceTypeId);
    }

    public double get_Oldspacequantity()
    {
        return getDouble(OldSpaceQuantity);
    }

    public double get_NewSpaceQuantity()
    {
        return getDouble(NewSpaceQuantity);
    }

    public int get_Oldyearbuilt()
    {
        return getInt(OldYearBuilt);
    }

    public int get_Newyearbuilt()
    {
        return getInt(NewYearBuilt);
    }

    public double get_LandArea()
    {
        return getDouble(LandArea);
    }

    public boolean get_Oldisderelict()
    {
        return getBoolean(OldIsDerelict);
    }

    public boolean get_Newisderelict()
    {
        return getBoolean(NewIsDerelict);
    }

    public boolean get_Oldisbrownfield()
    {
        return getBoolean(OldIsBrownfield);
    }

    public boolean get_Newisbrownfield()
    {
        return getBoolean(NewIsBrownfield);
    }

    public int get_Zoningrulescode()
    {
        return getInt(ZoningRulesCode);
    }

    public int get_Taz()
    {
        return getInt(Taz);
    }

    // Find and create
    public static ObservedDevelopmentEvents findOrCreate(SSessionJdbc ses, long _PecasParcelNum)
    {
        return ses.findOrCreate(meta, new Object[] {new Long(_PecasParcelNum)});
    }

    public SRecordMeta<ObservedDevelopmentEvents> getMeta()
    {
        return meta;
    }

}
