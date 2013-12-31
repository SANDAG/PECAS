package com.hbaspecto.pecas.aa.control;

import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.hbaspecto.pecas.aa.commodity.CommodityZUtility;
import com.hbaspecto.pecas.aa.commodity.NonTransportableExchange;
import com.hbaspecto.pecas.zones.PECASZone;

public class FakeNonTransportableExchange extends NonTransportableExchange {

	public FakeNonTransportableExchange(Commodity com, PECASZone zone) {
		super(com, zone);
	}

	public CommodityZUtility[] GetBuyingFlow() {
		return buyingFromExchangeFlows;
	}

	public CommodityZUtility[] GetSellingFlow() {
		return sellingToExchangeFlows;
	}
}
