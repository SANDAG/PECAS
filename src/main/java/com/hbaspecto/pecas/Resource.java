package com.hbaspecto.pecas;

import java.util.ArrayList;
import java.util.ResourceBundle;

import com.pb.common.util.ResourceUtil;

public class Resource implements IResource{

    @Override
    public String getProperty(ResourceBundle rb, String keyName) {
        return ResourceUtil.getProperty(rb, keyName);
    }

    @Override
    public boolean getBooleanProperty(ResourceBundle rb, String keyName,
            boolean b) {
        return ResourceUtil.getBooleanProperty(rb, keyName, b);
    }

    @Override
    public String checkAndGetProperty(ResourceBundle rb, String keyName) {
        return ResourceUtil.checkAndGetProperty(rb, keyName);
    }

    @Override
    public ArrayList getListWithUserDefinedSeparator(
            ResourceBundle rb, String string, String string2) {
        return ResourceUtil.getListWithUserDefinedSeparator(rb, string, string2);
    }

    @Override
    public double getDoubleProperty(ResourceBundle rb, String string,
            double positiveInfinity) {
        return ResourceUtil.getDoubleProperty(rb, string, positiveInfinity);
    }

    @Override
    public boolean getBooleanProperty(ResourceBundle rb, String string) {
        return ResourceUtil.getBooleanProperty(rb, string);
    }

    @Override
    public String getProperty(ResourceBundle rb, String string, String string2) {
        return ResourceUtil.getProperty(rb, string, string2);
    }

	@Override
	public int getIntegerProperty(ResourceBundle rb, String string, int i) {
		return ResourceUtil.getIntegerProperty(rb, string, i);
	}

	@Override
	public double getDoubleProperty(ResourceBundle rb, String string) {
		return ResourceUtil.getDoubleProperty(rb, string);
	}

	@Override
	public ResourceBundle getResourceBundle(String string) {
		return ResourceUtil.getResourceBundle(string);
	}

}
