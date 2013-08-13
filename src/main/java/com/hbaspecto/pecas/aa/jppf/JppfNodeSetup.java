/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.hbaspecto.pecas.aa.jppf;

import com.pb.common.util.ResourceUtil;
import com.hbaspecto.pecas.aa.control.AAPProcessor;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.ResourceBundle;

import org.jppf.task.storage.DataProvider;

/**
 * This class will setup the node
 * 
 */
public class JppfNodeSetup {
    static Logger logger = Logger.getLogger(JppfNodeSetup.class);
    public static Properties aaProps;
    public static Properties globalProps;
    String scenarioName;
    public static int timeInterval;
    
    static void setup(DataProvider provider) {
    	synchronized (AAPProcessor.isSetup) {
    		if (AAPProcessor.isSetup) {
    			logger.info("Thread " + Thread.currentThread().getId() + " has already been setup, no need to setup again");
    			return;
    		}
    		System.out.println("Initializing for PECAS Run");
    		logger.info("Thread "+Thread.currentThread().getId()+" is initializing");
	        logger.info("Loading aa.properties ResourceBundle");
	        try {
	        	aaProps = (Properties) provider.getValue("aaRb");
	
		        logger.info("Loading global.properties ResourceBundle");
		
		        logger.info("Reading data and setting up for AA run");
			} catch (Exception e1) {
				//test 
				System.out.println("Can't read aa.properties or global.properties ResourceBundle on the node");
				logger.fatal("Can't read aa.properties or global.properties ResourceBundle on the node");
				throw new RuntimeException("Can't read aa.properties or global.properties ResourceBundle on the node");
			}
	        long startTime = System.currentTimeMillis();
	        String pProcessorClass = aaProps.getProperty("pprocessor.class");
	        logger.info(JppfNodeSetup.class+" will be using the " + pProcessorClass + " for pre and post processing");
	        Class ppClass = null;
	        AAPProcessor aaReaderWriter = null;
	        try {
	            ppClass = Class.forName(pProcessorClass);
	            aaReaderWriter = (AAPProcessor) ppClass.newInstance();
	        } catch (ClassNotFoundException e) {
	            e.printStackTrace();
	        } catch (InstantiationException e) {
	            logger.fatal("Can't create new instance of  "+pProcessorClass);
	            e.printStackTrace();
	            throw new RuntimeException(e);
	        } catch (IllegalAccessException e) {
	            logger.fatal("Can't create new instance of "+ pProcessorClass);
	            e.printStackTrace();
	            throw new RuntimeException(e);
	        }

	        // this doesn't do anything yet, a placeholder
	        fixRelativePathsOnNode(aaProps);
	        
	        ResourceBundle aar = new PropertiesResourceBundle(aaProps);
	        
	        // instead try this, and don't send anything in the data provider
	        //ResourceBundle aar = ResourceUtil.getResourceBundle("aa");
	        //ResourceBundle gbr = ResourceUtil.getResourceBundle("global");

	
	        aaReaderWriter.setResourceBundles(aar);
	        aaReaderWriter.setTimePeriod(timeInterval);
	
	        aaReaderWriter.setUpAA();
	        logger.info("Setup is complete. Time in seconds: "+((System.currentTimeMillis()-startTime)/1000));
	        AAPProcessor.isSetup = true;
	        logger.info("Thread "+Thread.currentThread().getId()+" is initialized");
    	}
    }

    private static void fixRelativePathsOnNode(Properties aaProps2) {
		// TODO Change aa.base.data, aa.reference.data, aa.skim.data and all the other paths
    	// to reflect the setup of the node.  For instance if aa.reference.data is 
    	// OregonTestData/reference on the client, it might need to be
    	// Z:/modelshare/Oregon/OregonTestData/reference on a windows node or
    	// /Volumes/modelshare/Oregon/OregonTestData/reference on a mac/unix node
    	
    	// We haven't had to implement this because we've used the same drive mapping
    	// on all nodes, so all nodes can use the same path.  But this means we can't 
    	// use it on Unix machines yet, and each node needs to be configured
    	// with the same network share drive mapping
	}

	public static class PropertiesResourceBundle extends ResourceBundle {
    	
    	PropertiesResourceBundle(Properties p) {
    		myProperties = p;
    	}
    	
    	Properties myProperties;
    	
		@Override
		public Enumeration<String> getKeys() {
			throw new RuntimeException("Not implemented");
		}

		@Override
		protected Object handleGetObject(String arg0) {
			return myProperties.get(arg0);
		}
    	
    }


}