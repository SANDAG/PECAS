package com.hbaspecto.pecas.land;

import java.util.List;

import simpleorm.dataset.SDataSet;

public class QueueItem<E> {
private SDataSet dataSet;
private List<E> list;
	public SDataSet getDataSet() {
	return dataSet;
}
public void setDataSet(SDataSet dataSet) {
	this.dataSet = dataSet;
}
public List<E> getList() {
	return list;
}
public void setList(List<E> list) {
	this.list = list;
}

public QueueItem(SDataSet ds,List<E> l){
		dataSet =ds ;
		list =l;
	}
	
}
