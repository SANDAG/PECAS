package com.hbaspecto.pecas.aa.control;

import java.util.ResourceBundle;

import org.junit.Assert;
import org.junit.Test;

import com.hbaspecto.pecas.aa.commodity.Commodity;

public class AAControlTests {

	@Test
	public void testAAControl() {
		ResourceBundle bundle = new FakeResourceBundle();
		AAControl control = new AAControl(TestAAPProcessor.class, bundle);
	}
	
	@Test
	public void testAAControlRunAAToFindPricesBasic() {
		FakeResourceBundle bundle = new FakeResourceBundle();
		FakeResourceUtil util = new FakeResourceUtil();
		util._ints.put("aa.maxIterations", 0);
		util._doubles.put("aa.ConFac", .1);
		TestAAControl control = new TestAAControl(TestAAPProcessor.class, bundle);
		Commodity.setCalculateSizeTerms(false);
		int result = control.runAAToFindPrices(util);
		FakeAAModel model = control.GetModel();
		Assert.assertEquals(0, model._calculateExchangeSizeTermsForSpecifiedNonFloorspaceCalled);
		Assert.assertEquals(1, model._calculateCompositeBuyAndSellUtilitiesCalled);
		Assert.assertEquals(1, model._calculateLocationConsumptionAndProductionCalled);
		Assert.assertEquals(1, model._allocateQuantitiesToFlowsAndExchangesFalseCalled);
		
		Assert.assertEquals(0, model._calculateMeritMeasureWithoutLoggingCalled);
		Assert.assertEquals(1, model._calculateMeritMeasureWithLoggingCalled);
		Assert.assertEquals(0, model._snapShotCurrentPricesCalled);
		Assert.assertEquals(0, model._calculateNewPricesUsingBlockDerivativesCalled);
		
		Assert.assertEquals(2, result);
		
	}
	
	@Test
	public void testAAControlRunAAToFindPricesCalculateSizeTerms() {
		FakeResourceBundle bundle = new FakeResourceBundle();
		FakeResourceUtil util = new FakeResourceUtil();
		util._ints.put("aa.maxIterations", 0);
		util._doubles.put("aa.ConFac", .1);
		TestAAControl control = new TestAAControl(TestAAPProcessor.class, bundle);
		Commodity.setCalculateSizeTerms(true);
		int result = control.runAAToFindPrices(util);
		FakeAAModel model = control.GetModel();
		Assert.assertEquals(1, model._calculateExchangeSizeTermsForSpecifiedNonFloorspaceCalled);
		Assert.assertEquals(1, model._calculateCompositeBuyAndSellUtilitiesCalled);
		Assert.assertEquals(1, model._calculateLocationConsumptionAndProductionCalled);
		Assert.assertEquals(1, model._allocateQuantitiesToFlowsAndExchangesFalseCalled);
		
		Assert.assertEquals(0, model._calculateMeritMeasureWithoutLoggingCalled);
		Assert.assertEquals(1, model._calculateMeritMeasureWithLoggingCalled);
		Assert.assertEquals(0, model._snapShotCurrentPricesCalled);
		Assert.assertEquals(0, model._calculateNewPricesUsingBlockDerivativesCalled);
		
		Assert.assertEquals(2, result);
		
	}
	
	@Test (expected = RuntimeException.class)
	public void testAAControlRunAAToFindPricesNanPresent() {		
		FakeResourceBundle bundle = new FakeResourceBundle();
		FakeResourceUtil util = new FakeResourceUtil();
		util._ints.put("aa.maxIterations", 0);
		util._doubles.put("aa.ConFac", .1);
		TestAAControl control = new TestAAControl(TestAAPProcessor.class, bundle);
		Commodity.setCalculateSizeTerms(false);
		FakeAAModel model = new FakeAAModel(util, bundle); 
		control.SetModel(model);		
		model.setNAN(true);		
		int result = control.runAAToFindPrices(util);
	}
	
	@Test
	public void testAAControlRunAAToFindPricesEquilibrium() {
		FakeResourceBundle bundle = new FakeResourceBundle();
		FakeResourceUtil util = new FakeResourceUtil();
		util._ints.put("aa.maxIterations", 100);
		util._doubles.put("aa.ConFac", .1);
		TestAAControl control = new TestAAControl(TestAAPProcessor.class, bundle);
		control.convergeAfter(10);
		Commodity.setCalculateSizeTerms(false);
		int result = control.runAAToFindPrices(util);
		FakeAAModel model = control.GetModel();
		Assert.assertEquals(0, model._calculateExchangeSizeTermsForSpecifiedNonFloorspaceCalled);
		Assert.assertEquals(11, model._calculateCompositeBuyAndSellUtilitiesCalled);
		Assert.assertEquals(1, model._calculateLocationConsumptionAndProductionCalled);
		Assert.assertEquals(11, model._allocateQuantitiesToFlowsAndExchangesFalseCalled);
		
		Assert.assertEquals(0, model._calculateMeritMeasureWithoutLoggingCalled);
		Assert.assertEquals(11, model._calculateMeritMeasureWithLoggingCalled);
		Assert.assertEquals(1, model._snapShotCurrentPricesCalled);
		Assert.assertEquals(1, model._calculateNewPricesUsingBlockDerivativesCalled);
		Assert.assertEquals(9, model._decreaseStepSizeAndAdjustPricesCalled);
		
		Assert.assertEquals(0, result);
	}
	
	@Test
	public void testAAControlRunAAToFindPricesInfinity() {
		FakeResourceBundle bundle = new FakeResourceBundle();
		FakeResourceUtil util = new FakeResourceUtil();
		util._ints.put("aa.maxIterations", 10);
		util._doubles.put("aa.ConFac", .1);
		TestAAControl control = new TestAAControl(TestAAPProcessor.class, bundle);
		//control.convergeAfter(10);
		FakeAAModel model = new FakeAAModel(util, bundle);
		model._usesInfinity = true;
		control.SetModel(model);		
		Commodity.setCalculateSizeTerms(false);
		int result = control.runAAToFindPrices(util);
		Assert.assertEquals(0, model._calculateExchangeSizeTermsForSpecifiedNonFloorspaceCalled);
		Assert.assertEquals(11, model._calculateCompositeBuyAndSellUtilitiesCalled);
		Assert.assertEquals(1, model._calculateLocationConsumptionAndProductionCalled);
		Assert.assertEquals(11, model._allocateQuantitiesToFlowsAndExchangesFalseCalled);
		
		Assert.assertEquals(0, model._calculateMeritMeasureWithoutLoggingCalled);
		Assert.assertEquals(11, model._calculateMeritMeasureWithLoggingCalled);
		Assert.assertEquals(0, model._snapShotCurrentPricesCalled);
		Assert.assertEquals(0, model._calculateNewPricesUsingBlockDerivativesCalled);
		Assert.assertEquals(0, model._decreaseStepSizeAndAdjustPricesCalled);
		
		Assert.assertEquals(9, model._decreaseStepSizeEvenIfBelowMinimumAndAdjustPricesCalled);
		
		
		Assert.assertEquals(2, result);
	}
	
	@Test
	public void testAAControlRunAAToFindPricesMeasureLogging() {
		FakeResourceBundle bundle = new FakeResourceBundle();
		FakeResourceUtil util = new FakeResourceUtil();
		util._ints.put("aa.maxIterations", 100);
		util._doubles.put("aa.ConFac", .1);
		util._ints.put("aa.logFrequency", 10);
		TestAAControl control = new TestAAControl(TestAAPProcessor.class, bundle);
		control.convergeAfter(10);
		Commodity.setCalculateSizeTerms(false);
		int result = control.runAAToFindPrices(util);
		FakeAAModel model = control.GetModel();
		Assert.assertEquals(0, model._calculateExchangeSizeTermsForSpecifiedNonFloorspaceCalled);
		Assert.assertEquals(11, model._calculateCompositeBuyAndSellUtilitiesCalled);
		Assert.assertEquals(1, model._calculateLocationConsumptionAndProductionCalled);
		Assert.assertEquals(11, model._allocateQuantitiesToFlowsAndExchangesFalseCalled);
		
		Assert.assertEquals(9, model._calculateMeritMeasureWithoutLoggingCalled);
		Assert.assertEquals(2, model._calculateMeritMeasureWithLoggingCalled);
		Assert.assertEquals(10, model._snapShotCurrentPricesCalled);
		Assert.assertEquals(10, model._calculateNewPricesUsingBlockDerivativesCalled);
		Assert.assertEquals(0, model._decreaseStepSizeAndAdjustPricesCalled);
		
		Assert.assertEquals(0, result);
	}
	
	@Test
	public void testAAControlRunAAToFindPricesParallel() {
	}
	
	@Test
	public void testAAControlRunAAToFindPricesCalcAvgPriceFalse() {
	}
	
	@Test
	public void testAAControlRunAAToFindPricesCalcDeltaUsingDerivatives() {
	}
	
	@Test
	public void testAAControlRunAAToFindPricesSecondLast() {
	}
	

}
