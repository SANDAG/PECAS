package com.hbaspecto.pecas.aa.commodity;

import com.hbaspecto.pecas.OverflowException;
import com.hbaspecto.pecas.aa.travelAttributes.TravelUtilityCalculatorInterface;
import com.hbaspecto.pecas.zones.PECASZone;

public class FakeCommodityZUtility extends CommodityZUtility {

	protected FakeCommodityZUtility(Commodity c, PECASZone z,
			TravelUtilityCalculatorInterface tp) {
		super(c, z, tp);
		
		
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
