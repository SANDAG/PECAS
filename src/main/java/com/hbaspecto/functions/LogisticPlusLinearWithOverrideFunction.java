package com.hbaspecto.functions;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.log4j.Logger;

import com.hbaspecto.pecas.aa.commodity.Commodity;

public class LogisticPlusLinearWithOverrideFunction implements	SingleParameterFunction {
	
	static Logger logger = Logger.getLogger(LogisticPlusLinearWithOverrideFunction.class);
	
	private LogisticPlusLinearFunction baseFunction;

	private double overridePoint;
	private double overrideQuantity;
	private double exponent;

	public final LogisticPlusLinearFunction getBaseFunction() {
		return baseFunction;
	}

	public final void setBaseFunction(LogisticPlusLinearFunction baseFunction) {
		this.baseFunction = baseFunction;
	}

	public double getOverridePoint() {
		return overridePoint;
	}

	public void setOverridePoint(double overridePoint) {
		this.overridePoint = overridePoint;
	}

	public double getOverrideQuantity() {
		return overrideQuantity;
	}

	public void setOverrideQuantity(double overrideQuantity) {
		this.overrideQuantity = overrideQuantity;
	}

	public double getExponent() {
		return exponent;
	}

	public void setExponent(double exponent) {
		this.exponent = exponent;
		if (exponent <1) {
			String msg = "Exponent on override function should be >= 1 to ensure continuous derivatives";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}

	}

	public LogisticPlusLinearWithOverrideFunction(LogisticPlusLinearFunction myFunction, double overridePoint, double overrideQuantity, double exponent) {
		this.baseFunction = myFunction;
		this.overridePoint=overridePoint;
		this.overrideQuantity=overrideQuantity;
		this.exponent=exponent;
		if (exponent <1) {
			String msg = "Exponent on override function should be >= 1 to ensure continuous derivatives";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
	}

	@Override
	public double evaluate(double point) {
		double result = baseFunction.evaluate(point);
		if (point > overridePoint) {
			result += overrideQuantity*Math.pow(point-overridePoint,exponent);
		}
		return result;
	}

	@Override
	public double derivative(double point) {
		double result = baseFunction.derivative(point);
		if (point > overridePoint) {
			if (exponent ==1 ){
				result += overrideQuantity;
			} else {
				result += exponent*overrideQuantity*Math.pow(point-overridePoint, exponent-1);
			}
		}
		return result;
	}

	public void logOverrides(double point, String context) {
		if (point>overridePoint) {
			logger.warn("Override of "+overrideQuantity*Math.pow(point-overridePoint, exponent)+" in "+context);
		}
	}

	public static String getHeader() {
		return "Zone,Commodity,Price,TotalQuantity,OverrideQuantity";
	}

	public void writeOverride(Writer stream, int zone, String commodityName, double price) throws IOException {
		double overrideAmount = 0;
		if (price > overridePoint) overrideAmount = overrideQuantity*Math.pow(price-overridePoint,exponent);
		stream.write(zone+","+commodityName+","+price+","+evaluate(price)+","+overrideAmount+"\n");
		
	}

}
