package com.hbaspecto.pecas.sd.orm;

import simpleorm.dataset.*;
import simpleorm.utils.*;
import simpleorm.sessionjdbc.SSessionJdbc;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Base class of table transition_cost_codes.<br>
 * Do not edit as will be regenerated by running SimpleORMGenerator Generated on
 * Fri Sep 25 16:13:29 MDT 2009
 ***/
abstract class TransitionCostCodes_gen
        extends SRecordInstance
        implements java.io.Serializable
{

    public static final SRecordMeta<TransitionCostCodes> meta                                 = new SRecordMeta<TransitionCostCodes>(
                                                                                                      TransitionCostCodes.class,
                                                                                                      "transition_cost_codes");

    // Columns in table
    public static final SFieldInteger                    CostScheduleId                       = new SFieldInteger(
                                                                                                      meta,
                                                                                                      "cost_schedule_id",
                                                                                                      new SFieldFlags[] {
            SFieldFlags.SPRIMARY_KEY, SFieldFlags.SMANDATORY                                          });

    public static final SFieldDouble                     HighCapacityServicesInstallationCost = new SFieldDouble(
                                                                                                      meta,
                                                                                                      "high_capacity_services_installation_cost");

    public static final SFieldDouble                     LowCapacityServicesInstallationCost  = new SFieldDouble(
                                                                                                      meta,
                                                                                                      "low_capacity_services_installation_cost");

    public static final SFieldDouble                     BrownFieldCleanupCost                = new SFieldDouble(
                                                                                                      meta,
                                                                                                      "brownfield_cleanup_cost");

    public static final SFieldDouble                     GreenFieldPreparationCost            = new SFieldDouble(
                                                                                                      meta,
                                                                                                      "greenfield_preparation_cost");

    // Column getters and setters
    public int get_CostScheduleId()
    {
        return getInt(CostScheduleId);
    }

    public void set_CostScheduleId(int value)
    {
        setInt(CostScheduleId, value);
    }

    public double get_HighCapacityServicesInstallationCost()
    {
        return getDouble(HighCapacityServicesInstallationCost);
    }

    public void set_HighCapacityServicesInstallationCost(double value)
    {
        setDouble(HighCapacityServicesInstallationCost, value);
    }

    public double get_LowCapacityServicesInstallationCost()
    {
        return getDouble(LowCapacityServicesInstallationCost);
    }

    public void set_LowCapacityServicesInstallationCost(double value)
    {
        setDouble(LowCapacityServicesInstallationCost, value);
    }

    public double get_BrownFieldCleanupCost()
    {
        return getDouble(BrownFieldCleanupCost);
    }

    public void set_BrownFieldCleanupCost(double value)
    {
        setDouble(BrownFieldCleanupCost, value);
    }

    public double get_GreenFieldPreparationCost()
    {
        return getDouble(GreenFieldPreparationCost);
    }

    public void set_GreenFieldPreparationCost(double value)
    {
        setDouble(GreenFieldPreparationCost, value);
    }

    // Find and create
    public static TransitionCostCodes findOrCreate(SSessionJdbc ses, int _CostScheduleId)
    {
        return ses.findOrCreate(meta, new Object[] {new Integer(_CostScheduleId)});
    }

    public SRecordMeta<TransitionCostCodes> getMeta()
    {
        return meta;
    }
}
