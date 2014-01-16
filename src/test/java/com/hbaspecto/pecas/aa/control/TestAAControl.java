package com.hbaspecto.pecas.aa.control;

import java.util.ResourceBundle;

import com.hbaspecto.pecas.IResource;

public class TestAAControl extends AAControl {

	AAModel newModel = null;
	int convergeAfter = -1;

	public TestAAControl(Class pProcessorClass, ResourceBundle aaRb) {
		super(pProcessorClass, aaRb);
	}

	@Override
	protected AAModel getNormalAAModel(IResource resourceUtil,
			ResourceBundle aaRb) {
		if (newModel == null)
			newModel = new FakeAAModel(resourceUtil, aaRb, false);
		return newModel;
	}

	@Override
	protected AAModel getParallelAAModel(IResource resourceUtil,
			ResourceBundle aaRb) {
		if (newModel == null)
			newModel = new FakeAAModel(resourceUtil, aaRb, true);
		return newModel;
	}

	public FakeAAModel GetModel() {
		return (FakeAAModel) aa;
	}

	public void SetModel(FakeAAModel model) {
		newModel = model;

	}

	public void convergeAfter(int i) {
		convergeAfter = i;
	}

	@Override
	protected boolean isConverged(int iteration, IResource resource) {
		if (iteration == convergeAfter)
			return true;
		return false;
	}

}
