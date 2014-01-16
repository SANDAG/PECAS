package com.hbaspecto.pecas.aa.control;

import com.hbaspecto.pecas.OverflowException;
import com.hbaspecto.pecas.aa.commodity.AbstractCommodity;
import com.hbaspecto.pecas.zones.AbstractZone;

public class FakeAbstractCommodity extends AbstractCommodity {

	protected FakeAbstractCommodity(String name) {
		super(name);
	}

	@Override
	public double calcZUtility(AbstractZone t, boolean selling)
			throws OverflowException {
		return 0;
	}
	
	public static void clearCommodities()
	{
		allCommoditiesArrayList.clear();
		allCommoditiesHashmap.clear();
		nextCommodityNumber = 0;
	}

}
