package com.hbaspecto.pecas.land;

import java.util.concurrent.ArrayBlockingQueue;

public class LoadingQueue<E> extends ArrayBlockingQueue<E> {
	//private int numberOfTazs;
	public boolean finished;
	
	public LoadingQueue(int capacity) {
		super(capacity);
		finished = false;
		//this.numberOfTazs = numberOfTazs;		
	}
	
	public synchronized E getNext() throws InterruptedException{
		if (finished)
			return poll();
		else
			return take();		
	}
}
