package com.hbaspecto.pecas;

import java.util.ArrayList;
import java.util.ResourceBundle;

public interface IResource 
{

    String getProperty(ResourceBundle aaRb, String string);

    boolean getBooleanProperty(ResourceBundle aaRb, String string, boolean b);

    String checkAndGetProperty(ResourceBundle aaRb, String outputSource);

    ArrayList<String> getListWithUserDefinedSeparator(ResourceBundle aaRb,
            String string, String string2);

    double getDoubleProperty(ResourceBundle aaRb, String string,
            double positiveInfinity);

    boolean getBooleanProperty(ResourceBundle aaRb, String string);

    String getProperty(ResourceBundle aaRb, String string, String string2);

	int getIntegerProperty(ResourceBundle aaRb, String string, int i);

	double getDoubleProperty(ResourceBundle aaRb, String string);

	ResourceBundle getResourceBundle(String string);

}
