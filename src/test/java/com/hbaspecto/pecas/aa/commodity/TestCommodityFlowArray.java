package com.hbaspecto.pecas.aa.commodity;

import com.hbaspecto.pecas.aa.travelAttributes.TravelUtilityCalculatorInterface;

public class TestCommodityFlowArray extends CommodityFlowArray {

	boolean UseNegative = false;
	TestCommodityFlowArray(CommodityZUtility where,
			TravelUtilityCalculatorInterface tci) {
		super(where, tci);		
	}
	
	@Override
	public double calcUtilityForExchange(Exchange theExchange)
	{
		if (UseNegative)
			return Double.NEGATIVE_INFINITY;
		return 1.5;
	}

	public void setDispersionParameter(double i) {
		dispersionParameter = i;
		
	}
	

}
