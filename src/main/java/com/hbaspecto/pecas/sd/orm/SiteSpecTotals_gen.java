package com.hbaspecto.pecas.sd.orm;

import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import simpleorm.dataset.SFieldDouble;
import simpleorm.dataset.SFieldFlags;
import simpleorm.dataset.SFieldInteger;
import simpleorm.dataset.SRecordInstance;
import simpleorm.dataset.SRecordMeta;
import com.pb.common.util.ResourceUtil;

public abstract class SiteSpecTotals_gen
        extends SRecordInstance
        implements java.io.Serializable
{

    private static final long                 serialVersionUID = -2567189654989010074L;

    static Logger                             logger           = Logger.getLogger(SiteSpecTotals_gen.class);

    public static SRecordMeta<SiteSpecTotals> meta;

    // Columns in table

    public static SFieldInteger               SpaceTypeId;

    public static SFieldInteger               YearEffective;

    public static SFieldDouble                SpaceQuantity;

    public SiteSpecTotals_gen()
    {
        if (meta == null)
        {
            final String msg = SiteSpecTotals_gen.class.getName() + " was not initialized for ORM";
            logger.fatal(msg);
            throw new RuntimeException(msg);
        }
    }

    public static void init(ResourceBundle rb)
    {

        meta = new SRecordMeta<SiteSpecTotals>(SiteSpecTotals.class, ResourceUtil.getProperty(rb,
                "sdorm.sitespec_totals", "sitespec_totals"));

        // Columns in table

        SpaceTypeId = new SFieldInteger(meta, ResourceUtil.getProperty(rb,
                "sdorm.sitespec_totals.space_type_id", "space_type_id"), new SFieldFlags[] {
                SFieldFlags.SPRIMARY_KEY, SFieldFlags.SMANDATORY});

        YearEffective = new SFieldInteger(meta, ResourceUtil.getProperty(rb,
                "sdorm.sitespec_totals.year_effective", "year_effective"), new SFieldFlags[] {
                SFieldFlags.SPRIMARY_KEY, SFieldFlags.SMANDATORY});

        SpaceQuantity = new SFieldDouble(meta, ResourceUtil.getProperty(rb,
                "sdorm.sitesec_totals.space_quantity", "space_quantity"));
    }

    public int get_YearEffective()
    {
        return getInt(YearEffective);
    }

    public int get_SpaceTypeId()
    {
        return getInt(SpaceTypeId);
    }

    public double get_SpaceQuantity()
    {
        return getDouble(SpaceQuantity);
    }

    public void set_SpaceQuantity(double value)
    {
        setDouble(SpaceQuantity, value);
    }

    @Override
    public SRecordMeta<SiteSpecTotals> getMeta()
    {
        return meta;
    }
}
