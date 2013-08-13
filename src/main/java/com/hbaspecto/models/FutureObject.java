package com.hbaspecto.models;

public class FutureObject {
	protected Object value;
	
	public FutureObject(Object val) {
		value = val;
	}

	public FutureObject() {value = this;}
	
	public synchronized Object getValue() throws InterruptedException {
		while (value == this) 
			wait();
		return value;
	}
	
	public synchronized boolean isSet() {
		return (value !=this);
	}
	
	public synchronized void setValue(Object val) {
		if (value == this) {
			value = val;
			notifyAll();
		}
	}
}
