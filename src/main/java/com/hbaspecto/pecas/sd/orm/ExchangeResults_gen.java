package com.hbaspecto.pecas.sd.orm;

import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import simpleorm.dataset.SFieldDouble;
import simpleorm.dataset.SFieldFlags;
import simpleorm.dataset.SFieldInteger;
import simpleorm.dataset.SFieldString;
import simpleorm.dataset.SRecordInstance;
import simpleorm.dataset.SRecordMeta;
import simpleorm.sessionjdbc.SSessionJdbc;

import com.hbaspecto.pecas.land.ExchangeResults;
import com.pb.common.util.ResourceUtil;

/**
 * Base class of table exchange_results.<br>
 * Do not rerun the SimpleORMGenerator, as this class has been modified to not
 * use final static members for the meta and the field definition. Generated on
 * Fri Sep 25 16:13:29 MDT 2009
 ***/
public abstract class ExchangeResults_gen extends SRecordInstance implements
		java.io.Serializable {

	static Logger logger = Logger.getLogger(ExchangeResults_gen.class);

	public static SRecordMeta<ExchangeResults> meta;

	// Columns in table
	public static SFieldString Commodity;

	public static SFieldInteger Luz;

	public static SFieldDouble Price;

	public static void init(ResourceBundle rb) {

		meta = new SRecordMeta<ExchangeResults>(ExchangeResults.class,
				ResourceUtil.getProperty(rb, "sdorm.sdprices", "exchange_results"));

		// Columns in table
		Commodity = new SFieldString(meta, ResourceUtil.getProperty(rb,
				"sdorm.sdprices.commodity", "commodity"), 2147483647,
				new SFieldFlags[] { SFieldFlags.SPRIMARY_KEY, SFieldFlags.SMANDATORY });

		Luz = new SFieldInteger(meta, ResourceUtil.getProperty(rb,
				"sdorm.sdprices.luz", "luz"), new SFieldFlags[] {
				SFieldFlags.SPRIMARY_KEY, SFieldFlags.SMANDATORY });

		Price = new SFieldDouble(meta, ResourceUtil.getProperty(rb,
				"sdorm.sdprices.price", "price"));

	}

	// Column getters and setters
	public String get_Commodity() {
		return getString(Commodity);
	}

	public void set_Commodity(String value) {
		setString(Commodity, value);
	}

	public int get_Luz() {
		return getInt(Luz);
	}

	public void set_Luz(int value) {
		setInt(Luz, value);
	}

	public double get_Price() {
		return getDouble(Price);
	}

	public void set_Price(double value) {
		setDouble(Price, value);
	}

	// Find and create
	public static ExchangeResults findOrCreate(SSessionJdbc ses,
			String _Commodity, int _Luz) {
		return ses.findOrCreate(meta,
				new Object[] { _Commodity, new Integer(_Luz) });
	}

	@Override
	public SRecordMeta<ExchangeResults> getMeta() {
		return meta;
	}

	public ExchangeResults_gen() {
		super();
		if (meta == null) {
			final String msg = ExchangeResults_gen.class.getName()
					+ " was not initialized for ORM";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}

	}

}
