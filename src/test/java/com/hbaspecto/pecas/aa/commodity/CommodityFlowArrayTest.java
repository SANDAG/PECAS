package com.hbaspecto.pecas.aa.commodity;

import org.junit.Assert;
import org.junit.Test;

import com.hbaspecto.pecas.ChoiceModelOverflowException;
import com.hbaspecto.pecas.aa.control.FakeAbstractCommodity;
import com.hbaspecto.pecas.aa.control.FakeExchange;
import com.hbaspecto.pecas.aa.travelAttributes.TravelUtilityCalculatorInterface;
import com.hbaspecto.pecas.zones.PECASZone;

public class CommodityFlowArrayTest {
	
	public double delta = .000000000001;

	@Test
	public void testCommodityFlowArray() {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "c1";
		char exchangeTypePar = 'p'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		CommodityZUtility where = new FakeCommodityZUtility(c, z, to);		
		CommodityFlowArray control = new CommodityFlowArray(where, to);
	}
	
	@Test
	public void testCommodityFlowArrayGetUtility() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "c1";
		char exchangeTypePar = 'p'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		CommodityZUtility where = new FakeCommodityZUtility(c, z, to);		
		CommodityFlowArray control = new CommodityFlowArray(where, to);
		double higherLevelDispersionParameter = .5;
		double result = control.getUtility(higherLevelDispersionParameter);
	}
	
	@Test
	public void testCommodityFlowArrayGetUtilityPSell() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "sell";
		char exchangeTypePar = 'p'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		PECASZone z2 = PECASZone.createTaz(1);
		Exchange ex = new Exchange(c, z, 5);
		Exchange ex2 = new Exchange(c, z2, 5);
		CommodityZUtility where = new FakeSellingCommodityZUtility(c, z, to);		
		CommodityFlowArray control = new TestCommodityFlowArray(where, to);
		double higherLevelDispersionParameter = .5;
		double result = control.getUtility(higherLevelDispersionParameter);
		Assert.assertEquals(1.5, result, delta);
	}
	
	@Test
	public void testCommodityFlowArrayGetUtilityCBuy() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "buy";
		char exchangeTypePar = 'c'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		PECASZone z2 = PECASZone.createTaz(1);
		Exchange ex = new Exchange(c, z, 5);
		Exchange ex2 = new Exchange(c, z2, 5);
		CommodityZUtility where = new FakeBuyingCommodityZUtility(c, z, to);		
		CommodityFlowArray control = new TestCommodityFlowArray(where, to);
		double higherLevelDispersionParameter = .5;
		double result = control.getUtility(higherLevelDispersionParameter);
		Assert.assertEquals(1.5, result, delta);
	}
	
	@Test
	public void testCommodityFlowArrayGetUtilityN() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "buy";
		char exchangeTypePar = 'n'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		PECASZone z2 = PECASZone.createTaz(1);
		Exchange ex = new Exchange(c, z, 5);
		Exchange ex2 = new Exchange(c, z2, 5);
		CommodityZUtility where = new FakeBuyingCommodityZUtility(c, z, to);		
		CommodityFlowArray control = new TestCommodityFlowArray(where, to);
		double higherLevelDispersionParameter = .5;
		double result = control.getUtility(higherLevelDispersionParameter);
		Assert.assertEquals(1.5, result, delta);
	}
	
	@Test
	public void testCommodityFlowArrayGetUtilitySize() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "buy";
		char exchangeTypePar = 's'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		Exchange ex = new Exchange(c, z, 5);
		CommodityZUtility where = new FakeBuyingCommodityZUtility(c, z, to);		
		CommodityFlowArray control = new TestCommodityFlowArray(where, to);
		double higherLevelDispersionParameter = .5;
		double result = control.getUtility(higherLevelDispersionParameter);
		Assert.assertEquals(1.5, result, delta);
	}
	
	@Test
	public void testCommodityFlowArrayGetUtilityNan() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "buy";
		char exchangeTypePar = 's'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		PECASZone z2 = PECASZone.createTaz(1);
		Exchange ex = new Exchange(c, z, 5);
		Exchange ex2 = new Exchange(c, z2, 5);
		CommodityZUtility where = new FakeBuyingCommodityZUtility(c, z, to);		
		TestCommodityFlowArray control = new TestCommodityFlowArray(where, to);
		control.setDispersionParameter(Math.sqrt(-1));
		double higherLevelDispersionParameter = .5;
		double result = control.getUtility(higherLevelDispersionParameter);
		Assert.assertTrue(Double.isNaN(result));
	}
	
	@Test(expected = ChoiceModelOverflowException.class)
	public void testCommodityFlowArrayGetUtilityPInf() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "buy";
		char exchangeTypePar = 's'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		PECASZone z2 = PECASZone.createTaz(1);
		Exchange ex = new Exchange(c, z, 5);
		Exchange ex2 = new Exchange(c, z2, 5);
		CommodityZUtility where = new FakeBuyingCommodityZUtility(c, z, to);		
		TestCommodityFlowArray control = new TestCommodityFlowArray(where, to);
		control.setDispersionParameter(0);
		double higherLevelDispersionParameter = .5;
		double result = control.getUtility(higherLevelDispersionParameter);
		Assert.assertEquals(1.5, result, delta);
	}
	
	@Test
	public void testCommodityFlowArrayGetUtilityNInf() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "buy";
		char exchangeTypePar = 's'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		PECASZone z2 = PECASZone.createTaz(1);
		Exchange ex = new Exchange(c, z, 5);
		Exchange ex2 = new Exchange(c, z2, 5);
		CommodityZUtility where = new FakeBuyingCommodityZUtility(c, z, to);		
		TestCommodityFlowArray control = new TestCommodityFlowArray(where, to);
		control.setDispersionParameter(1);
		control.UseNegative = true;
		double higherLevelDispersionParameter = .5;
		double result = control.getUtility(higherLevelDispersionParameter);
		Assert.assertEquals(Double.NEGATIVE_INFINITY, result, delta);
	}
	
	@Test
	public void testCommodityFlowArrayGetUtilityNormal() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "buy";
		char exchangeTypePar = 's'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		PECASZone z2 = PECASZone.createTaz(1);
		Exchange ex = new Exchange(c, z, 5);
		Exchange ex2 = new Exchange(c, z2, 5);
		CommodityZUtility where = new FakeBuyingCommodityZUtility(c, z, to);		
		TestCommodityFlowArray control = new TestCommodityFlowArray(where, to);
		control.setDispersionParameter(.75);
		double higherLevelDispersionParameter = .5;
		double sum = 2 * Math.exp(.75 * 1.5);
		double expected = 1 / .75 * Math.log(sum);
		double result = control.getUtility(higherLevelDispersionParameter);
		Assert.assertEquals(expected, result, delta);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	@Test
	public void testCommodityFlowArrayGetUtilityNoSizeEffect() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "c1";
		char exchangeTypePar = 'p'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		CommodityZUtility where = new FakeCommodityZUtility(c, z, to);		
		CommodityFlowArray control = new CommodityFlowArray(where, to);
		double result = control.getUtilityNoSizeEffect();
	}
	
	@Test
	public void testCommodityFlowArrayGetUtilityNoSizeEffectPSell() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "sell";
		char exchangeTypePar = 'p'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		PECASZone z2 = PECASZone.createTaz(1);
		Exchange ex = new Exchange(c, z, 5);
		Exchange ex2 = new Exchange(c, z2, 5);
		CommodityZUtility where = new FakeSellingCommodityZUtility(c, z, to);		
		CommodityFlowArray control = new TestCommodityFlowArray(where, to);
		double result = control.getUtilityNoSizeEffect();
		Assert.assertEquals(1.5, result, delta);
	}
	
	@Test
	public void testCommodityFlowArrayGetUtilityNoSizeEffectCBuy() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "buy";
		char exchangeTypePar = 'c'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		PECASZone z2 = PECASZone.createTaz(1);
		Exchange ex = new Exchange(c, z, 5);
		Exchange ex2 = new Exchange(c, z2, 5);
		CommodityZUtility where = new FakeBuyingCommodityZUtility(c, z, to);		
		CommodityFlowArray control = new TestCommodityFlowArray(where, to);
		double result = control.getUtilityNoSizeEffect();
		Assert.assertEquals(1.5, result, delta);
	}
	
	@Test
	public void testCommodityFlowArrayGetUtilityNoSizeEffectN() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "buy";
		char exchangeTypePar = 'n'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		PECASZone z2 = PECASZone.createTaz(1);
		Exchange ex = new Exchange(c, z, 5);
		Exchange ex2 = new Exchange(c, z2, 5);
		CommodityZUtility where = new FakeBuyingCommodityZUtility(c, z, to);		
		CommodityFlowArray control = new TestCommodityFlowArray(where, to);
		double result = control.getUtilityNoSizeEffect();
		Assert.assertEquals(1.5, result, delta);
	}
	
	@Test
	public void testCommodityFlowArrayGetUtilityNoSizeEffectSize() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "buy";
		char exchangeTypePar = 's'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		Exchange ex = new Exchange(c, z, 5);
		CommodityZUtility where = new FakeBuyingCommodityZUtility(c, z, to);		
		CommodityFlowArray control = new TestCommodityFlowArray(where, to);
		double result = control.getUtilityNoSizeEffect();
		Assert.assertEquals(1.5, result, delta);
	}
	
	@Test
	public void testCommodityFlowArrayGetUtilityNoSizeEffectNan() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "buy";
		char exchangeTypePar = 's'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		PECASZone z2 = PECASZone.createTaz(1);
		Exchange ex = new Exchange(c, z, 5);
		Exchange ex2 = new Exchange(c, z2, 5);
		CommodityZUtility where = new FakeBuyingCommodityZUtility(c, z, to);		
		TestCommodityFlowArray control = new TestCommodityFlowArray(where, to);
		control.setDispersionParameter(Math.sqrt(-1));
		double result = control.getUtilityNoSizeEffect();
		Assert.assertTrue(Double.isNaN(result));
	}
	
	@Test(expected = ChoiceModelOverflowException.class)
	public void testCommodityFlowArrayGetUtilityNoSizeEffectPInf() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "buy";
		char exchangeTypePar = 's'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		PECASZone z2 = PECASZone.createTaz(1);
		Exchange ex = new Exchange(c, z, 5);
		Exchange ex2 = new Exchange(c, z2, 5);
		CommodityZUtility where = new FakeBuyingCommodityZUtility(c, z, to);		
		TestCommodityFlowArray control = new TestCommodityFlowArray(where, to);
		control.setDispersionParameter(0);
		double result = control.getUtilityNoSizeEffect();
		Assert.assertEquals(1.5, result, delta);
	}
	
	@Test
	public void testCommodityFlowArrayGetUtilityNoSizeEffectNInf() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "buy";
		char exchangeTypePar = 's'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		PECASZone z2 = PECASZone.createTaz(1);
		Exchange ex = new Exchange(c, z, 5);
		Exchange ex2 = new Exchange(c, z2, 5);
		CommodityZUtility where = new FakeBuyingCommodityZUtility(c, z, to);		
		TestCommodityFlowArray control = new TestCommodityFlowArray(where, to);
		control.setDispersionParameter(1);
		control.UseNegative = true;
		double result = control.getUtilityNoSizeEffect();
		Assert.assertEquals(Double.NEGATIVE_INFINITY, result, delta);
	}
	
	@Test
	public void testCommodityFlowArrayGetUtilityNoSizeEffectNormal() throws ChoiceModelOverflowException {
		FakeAbstractCommodity.clearCommodities();
		TravelUtilityCalculatorInterface to = new FakeTravelUtilityCalculatorInterface();
		String name = "buy";
		char exchangeTypePar = 's'; 
		Commodity c = Commodity.createOrRetrieveCommodity(name, exchangeTypePar);
		PECASZone.createTazArray(10);
		PECASZone z = PECASZone.createTaz(0);
		PECASZone z2 = PECASZone.createTaz(1);
		Exchange ex = new Exchange(c, z, 5);
		Exchange ex2 = new Exchange(c, z2, 5);
		CommodityZUtility where = new FakeBuyingCommodityZUtility(c, z, to);		
		TestCommodityFlowArray control = new TestCommodityFlowArray(where, to);
		control.setDispersionParameter(.75);
		double sum = 2 * Math.exp(.75 * 1.5);
		double expected = 1 / .75 * Math.log(sum);
		double result = control.getUtilityNoSizeEffect();
		Assert.assertEquals(expected, result, delta);
	}
}
