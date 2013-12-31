package com.hbaspecto.pecas.aa.control;

import com.hbaspecto.functions.LogisticPlusLinearFunction;

public class FakeLogisticPlusLinearFunction extends LogisticPlusLinearFunction {

	public FakeLogisticPlusLinearFunction(double y0, double x0, double lambda,
			double delta, double slope) {
		super(y0, x0, lambda, delta, slope);
	}

	public double getY0() {
		return y0;
	}
	
	public double getX0() {
		return x0;
	}
	
	public double getSlope() {
		return slope;
	}
	
	public double getLambda() {
		return lambda;
	}
	
	public double getDelta() {
		return delta;
	}

}
