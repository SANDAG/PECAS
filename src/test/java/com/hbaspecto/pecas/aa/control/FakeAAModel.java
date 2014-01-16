package com.hbaspecto.pecas.aa.control;

import java.util.ResourceBundle;

import com.hbaspecto.pecas.IResource;

public class FakeAAModel extends AAModel {
	public int _calculateCompositeBuyAndSellUtilitiesCalled = 0;
	public int _calculateLocationConsumptionAndProductionCalled = 0;
	public int _allocateQuantitiesToFlowsAndExchangesTrueCalled = 0;
	public int _allocateQuantitiesToFlowsAndExchangesFalseCalled = 0;
	public int _calculateExchangeSizeTermsForSpecifiedNonFloorspaceCalled = 0;
	public int _calculateMeritMeasureWithLoggingCalled = 0;
	public int _calculateMeritMeasureWithoutLoggingCalled = 0;
	public int _snapShotCurrentPricesCalled = 0;
	public int _calculateNewPricesUsingBlockDerivativesCalled = 0;
	public int _decreaseStepSizeAndAdjustPricesCalled = 0;
	public int _decreaseStepSizeEvenIfBelowMinimumAndAdjustPricesCalled = 0;
	public int _recalculateLocationConsumptionAndProductionCalled = 0;
	public int _backUpToLastValidPricesCalled = 0;
	public boolean _usesInfinity = false;

	public boolean _isNAN = false;
	public boolean IsParallel = false;
	public boolean _usedDerivitives = false;

	public FakeAAModel(IResource resourceUtil, ResourceBundle aaRb,
			boolean isParallel) {
		super(resourceUtil, aaRb);
		IsParallel = isParallel;
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean calculateCompositeBuyAndSellUtilities() {
		_calculateCompositeBuyAndSellUtilitiesCalled++;
		return _isNAN;
	}

	@Override
	public boolean calculateLocationConsumptionAndProduction() {
		_calculateLocationConsumptionAndProductionCalled++;
		return _isNAN;
	}

	@Override
	public boolean allocateQuantitiesToFlowsAndExchanges(boolean param) {
		if (param)
			_allocateQuantitiesToFlowsAndExchangesTrueCalled++;
		else
			_allocateQuantitiesToFlowsAndExchangesFalseCalled++;

		return _isNAN;
	}

	@Override
	public boolean calculateExchangeSizeTermsForSpecifiedNonFloorspace() {
		_calculateExchangeSizeTermsForSpecifiedNonFloorspaceCalled++;
		return _isNAN;
	}

	@Override
	public double calculateMeritMeasureWithLogging() {
		_calculateMeritMeasureWithLoggingCalled++;
		if (_usesInfinity)
			return Double.POSITIVE_INFINITY;
		return _calculateMeritMeasureWithLoggingCalled;
	}

	@Override
	public double calculateMeritMeasureWithoutLogging() {
		_calculateMeritMeasureWithoutLoggingCalled++;
		return _calculateMeritMeasureWithoutLoggingCalled;
	}

	@Override
	public void snapShotCurrentPrices() {
		_snapShotCurrentPricesCalled++;
	}

	@Override
	public void calculateNewPricesUsingBlockDerivatives(
			boolean calcDeltaUsingDerivatives, IResource resourceUtil) {
		_usedDerivitives = calcDeltaUsingDerivatives;
		_calculateNewPricesUsingBlockDerivativesCalled++;
	}

	public void setNAN(boolean b) {
		_isNAN = b;

	}

	@Override
	public void decreaseStepSizeAndAdjustPrices() {
		_decreaseStepSizeAndAdjustPricesCalled++;
	}

	@Override
	public void decreaseStepSizeEvenIfBelowMinimumAndAdjustPrices() {
		_decreaseStepSizeEvenIfBelowMinimumAndAdjustPricesCalled++;
	}

	@Override
	public double getMinimumStepSize() {
		if (_usesInfinity)
			return getStepSize();
		else
			return super.getMinimumStepSize();
	}

	@Override
	public boolean recalculateLocationConsumptionAndProduction() {
		_recalculateLocationConsumptionAndProductionCalled++;
		return false;
	}

	@Override
	public void backUpToLastValidPrices() {
		_backUpToLastValidPricesCalled++;
	}
}
