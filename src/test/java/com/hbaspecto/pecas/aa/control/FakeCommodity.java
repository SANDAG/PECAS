package com.hbaspecto.pecas.aa.control;

import com.hbaspecto.pecas.OverflowException;
import com.hbaspecto.pecas.aa.commodity.AbstractCommodity;
import com.hbaspecto.pecas.zones.AbstractZone;

public class FakeCommodity extends AbstractCommodity {

	protected FakeCommodity(String name) {
		super(name);
	}

	@Override
	public double calcZUtility(AbstractZone t, boolean selling)
			throws OverflowException {
		return 0;
	}

}
