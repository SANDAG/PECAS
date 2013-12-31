package com.hbaspecto.pecas.aa.control;

import com.hbaspecto.pecas.aa.commodity.Commodity;
import com.hbaspecto.pecas.aa.commodity.CommodityZUtility;
import com.hbaspecto.pecas.aa.commodity.Exchange;
import com.hbaspecto.pecas.zones.PECASZone;

public class FakeExchange extends Exchange{

	public FakeExchange(Commodity com, PECASZone zone, int arraySize) {
		super(com, zone, arraySize);		
	}
	
	public CommodityZUtility[] GetBuyingFlow() {
		return buyingFromExchangeFlows;
	}

	public CommodityZUtility[] GetSellingFlow() {
		return sellingToExchangeFlows;
	}

}
