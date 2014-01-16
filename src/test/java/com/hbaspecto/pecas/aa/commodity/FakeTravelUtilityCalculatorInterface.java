package com.hbaspecto.pecas.aa.commodity;

import com.hbaspecto.pecas.aa.travelAttributes.TravelAttributesInterface;
import com.hbaspecto.pecas.aa.travelAttributes.TravelUtilityCalculatorInterface;

public class FakeTravelUtilityCalculatorInterface implements
		TravelUtilityCalculatorInterface {

	@Override
	public double getUtility(int origin, int destination,
			TravelAttributesInterface travelConditions) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double[] getUtilityComponents(int fromZoneUserNumber,
			int toZoneUserNumber, TravelAttributesInterface travelConditions) {
		// TODO Auto-generated method stub
		return null;
	}

}
