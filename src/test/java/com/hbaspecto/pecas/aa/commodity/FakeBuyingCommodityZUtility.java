package com.hbaspecto.pecas.aa.commodity;

import com.hbaspecto.pecas.OverflowException;
import com.hbaspecto.pecas.aa.travelAttributes.TravelUtilityCalculatorInterface;
import com.hbaspecto.pecas.zones.PECASZone;

public class FakeBuyingCommodityZUtility extends BuyingZUtility {

	public FakeBuyingCommodityZUtility(Commodity c, PECASZone t,
			TravelUtilityCalculatorInterface tp) {
		super(c, t, tp);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void addExchange(Exchange x) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addAllExchanges() {
		// TODO Auto-generated method stub

	}

	@Override
	public void allocateQuantityToFlowsAndExchanges() throws OverflowException {
		// TODO Auto-generated method stub

	}

}
