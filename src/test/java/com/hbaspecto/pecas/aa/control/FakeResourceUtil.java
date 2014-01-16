package com.hbaspecto.pecas.aa.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

import com.hbaspecto.pecas.IResource;
import com.pb.common.util.ResourceUtil;

public class FakeResourceUtil implements IResource {
	HashMap<String, String> _strings = new HashMap<String, String>();
	HashMap<String, Boolean> _bools = new HashMap<String, Boolean>();
	HashMap<String, Double> _doubles = new HashMap<String, Double>();
	HashMap<String, Integer> _ints = new HashMap<String, Integer>();
	HashMap<String, ResourceBundle> _bundles = new HashMap<String, ResourceBundle>();
	
	ArrayList _arrayList = new ArrayList<>();

	@Override
	public String getProperty(ResourceBundle aaRb, String key) {
		return _strings.get(key);
	}

	@Override
	public boolean getBooleanProperty(ResourceBundle aaRb, String key, boolean b) {
		if (_bools.containsKey(key))
			return _bools.get(key);
		return b;
	}

	@Override
	public String checkAndGetProperty(ResourceBundle aaRb, String key) {
		return _strings.get(key);
	}

	@Override
	public ArrayList getListWithUserDefinedSeparator(ResourceBundle aaRb,
			String key1, String key2) {
		return _arrayList;
	}

	@Override
	public double getDoubleProperty(ResourceBundle aaRb, String key,
			double positiveInfinity) {
		if (_doubles.containsKey(key))
			return _doubles.get(key);
		return positiveInfinity;
	}

	@Override
	public boolean getBooleanProperty(ResourceBundle aaRb, String key) {
		return _bools.get(key);
	}

	@Override
	public String getProperty(ResourceBundle aaRb, String key1, String key2) {
		return _strings.get(key1 + key2);
	}

	public void setBoolean(String key, boolean value) {
		_bools.put(key, value);
	}

	@Override
	public int getIntegerProperty(ResourceBundle aaRb, String key, int def) {
		if (_ints.containsKey(key))
			return _ints.get(key);
		return def;
	}

	@Override
	public double getDoubleProperty(ResourceBundle aaRb, String key) {
		return _doubles.get(key);		
	}

	@Override
	public ResourceBundle getResourceBundle(String key) {
		return _bundles.get(key);
	}
	

}
