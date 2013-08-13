package com.hbaspecto.pecas.landSynth;

import java.util.ArrayList;

import org.apache.log4j.Logger;

public class FieldNameReference {
	
	Logger logger = Logger.getLogger(FieldNameReference.class);
	
	ArrayList<String> integerFieldNames = new ArrayList<String>();
	ArrayList<String> doubleFieldNames= new ArrayList<String>();
	ArrayList <String> stringFieldNames= new ArrayList<String>();
	ArrayList <String> booleanFieldNames= new ArrayList<String>();
	ArrayList <String> longFieldNames = new ArrayList<String>();
	
//	class typeAndColumn {
//		static final int BOOLEAN = 1;
//		static final int STRING = 2;
//		static final int FLOAT = 3;
//		static final int INTEGER = 4;
//		int type;
//		int originalColumn;
//		int arrayIndex;
//	}

	public Object getField(String fieldName, int[] intValues, String[] stringValues, double[] doubleValues, boolean[] booleanValues, long[] longValues) {
		if (integerFieldNames.contains(fieldName)) {
			return Integer.valueOf(intValues[integerFieldNames.lastIndexOf(fieldName)]);
		} else if (doubleFieldNames.contains(fieldName)) {
			return Double.valueOf(doubleValues[doubleFieldNames.lastIndexOf(fieldName)]);
		} else if (booleanFieldNames.contains(fieldName)) {
			return Boolean.valueOf(booleanValues[booleanFieldNames.lastIndexOf(fieldName)]);
		} else if (stringFieldNames.contains(fieldName)) {
			return stringValues[stringFieldNames.lastIndexOf(fieldName)];
		} else if (longFieldNames.contains(fieldName)) {
			return Long.valueOf(longValues[longFieldNames.lastIndexOf(fieldName)]);
		} else {
			String msg = "Can't find field name "+fieldName;
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
	}
	
}
