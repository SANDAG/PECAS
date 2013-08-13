/*
 * Copyright  2005 HBA Specto Incorporated
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
package com.hbaspecto.pecas.aa.travelAttributes;

import com.hbaspecto.pecas.zones.AbstractZone;
import com.hbaspecto.pecas.zones.PECASZone;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.CSVSquareTableMatrixReader;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.ZipMatrixReader;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;

/**
 * A class that reads in peak auto skims and facilitates zone pair disutility calculations
 * @author John Abraham, Joel Freedman
 * 
 */
public class SomeSkims extends TransportKnowledge implements TravelAttributesInterface {
    protected static Logger logger = Logger.getLogger("com.pb.models.pecas");

    private ArrayList matrixList = new ArrayList();
    public Matrix[] matrices = new Matrix[0];
    private ArrayList matrixNameList = new ArrayList();

    String my1stPath;
    String my2ndPath;


    public SomeSkims() {
        my1stPath = System.getProperty("user.dir");
    }

    public SomeSkims(String firstPath, String secondPath) {
        my1stPath = firstPath;
        my2ndPath = secondPath;
    };
    
    public Matrix getMatrix(String name) {
        int place = matrixNameList.indexOf(name);
        if (place >=0) return matrices[place];
        return null;
    }
    
    public void addCSVSquareMatrix(String matrixName) {
        if (matrixNameList.contains(matrixName)) {
            logger.info("SomeSkims already contains matrix named "+matrixName+", not reading it in again");
        } else {
        	CSVSquareTableMatrixReader csvReader1 = new CSVSquareTableMatrixReader(my1stPath);
        	CSVSquareTableMatrixReader csvReader2 = new CSVSquareTableMatrixReader(my2ndPath);
        	Matrix m = null;
        	try {
        		m = csvReader1.readMatrix(matrixName);
        	} catch (Exception e) {
        		try {
        			m = csvReader2.readMatrix(matrixName);
        		} catch (Exception e1) {
        			logger.fatal("Could not find "+ matrixName+".csv in " + my1stPath+" or in "+my2ndPath,e1);
        			throw new RuntimeException("Could not find "+ matrixName+".csv in " + my1stPath+" or in "+my2ndPath,e1);
        		}
        	}
            matrixList.add(m);
            matrixNameList.add(matrixName);
            matrices = (Matrix[]) matrixList.toArray(matrices);
        }
        
        // TODO remove print statement
        System.out.println("finished reading zipmatrix of skims "+matrixName+" into memory");
        if(logger.isDebugEnabled()) logger.debug("finished reading zipmatrix of skims "+matrixName+" into memory");
    	
    }

    public void addZipMatrix(String matrixName) {
        if (matrixNameList.contains(matrixName)) {
            logger.info("SomeSkims already contains matrix named "+matrixName+", not reading it in again");
        } else {
            File skim = new File(my1stPath+matrixName+".zip");
            if (!skim.exists()) skim = new File(my1stPath+matrixName+".zipMatrix");
            if(!skim.exists()) skim = new File(my1stPath+matrixName+".zmx");
            if(!skim.exists()){
                skim = new File(my2ndPath+matrixName+".zip");
                if(!skim.exists()) skim = new File(my2ndPath+matrixName+".zipMatrix");
                if(!skim.exists()) skim = new File(my2ndPath+matrixName+".zmx");
                if (!skim.exists()) {
                    logger.fatal("Could not find "+ matrixName+".zip, .zipMatrix or .zmx in either the pt or the ts directory");
                    throw new RuntimeException("Could not find "+ matrixName+".zip, .zipMatrix or .zmx in either the pt or the ts directory");
                }
            }
            matrixList.add(new ZipMatrixReader(skim).readMatrix());
            matrixNameList.add(matrixName);
            matrices = (Matrix[]) matrixList.toArray(matrices);
        }
        
        if(logger.isDebugEnabled()) logger.debug("finished reading zipmatrix of skims "+matrixName+" into memory");
    }
    
   
    public void addMatrixCSVSkims(TableDataSet s, String name) {
        int rows = s.getRowCount();
        int columns = s.getColumnCount()-1;
        if (rows!=columns) {
            logger.fatal("Trying to add CSV Matrix Skims and number of columns does not equal number of rows");
            throw new RuntimeException("Trying to add CSV Matrix Skims and number of columns does not equal number of rows");
        }
        float[][] tempArray = new float[rows][columns];
        int[] userToSequentialLookup = new int[rows+1];
        // check order of rows and columns 
        for (int check = 1;check < s.getRowCount();check++) {
            if (!(s.getColumnLabel(check+1).trim().equals(String.valueOf((int) (s.getValueAt(check,1)))))) {
                logger.fatal("CSVMatrixSkims have columns out of order (needs to be the same as rows)");
                throw new RuntimeException("CSVMatrixSkims have columns out of order (needs to be the same as rows)");
            }
        }
        // TODO check for missing skims when using CSV format
        for (int tdsRow = 1;tdsRow <= s.getRowCount();tdsRow++) {
            userToSequentialLookup[tdsRow]=(int) s.getValueAt(tdsRow,1);
            for (int tdsCol=2;tdsCol<=s.getColumnCount();tdsCol++) {
                tempArray[tdsRow-1][tdsCol-2]=s.getValueAt(tdsRow,tdsCol);
            }
        }
        Matrix m = new Matrix(name,"",tempArray);
        matrixNameList.add(name);
        m.setExternalNumbers(userToSequentialLookup);
        this.matrixList.add(m);
        matrices = (Matrix[]) matrixList.toArray(matrices);
    }

    /** Adds a table data set of skims into the set of skims that are available 
     * 
     * @param s the table dataset of skims.  There must be a column called "origin"
     * and another column called "destination"
     * @param fieldsToAdd the names of the fields from which to create matrices from, all other fields
     * will be ignored.
     */
    public void addTableDataSetSkims(TableDataSet s, String[] fieldsToAdd, int maxZoneNumber) {
        int originField = s.getColumnPosition("origin");
        if (originField == -1) originField=s.checkColumnPosition("i");
        int destinationField = s.getColumnPosition("destination");
        if (destinationField == -1) destinationField = s.checkColumnPosition("j");
        int[] userToSequentialLookup = new int[maxZoneNumber];
        int[] sequentialToUserLookup = new int[maxZoneNumber];
        for (int i =0; i<userToSequentialLookup.length;i++) {
                    userToSequentialLookup[i] = -1;
        }
        int[] origins = s.getColumnAsInt(originField);
        int zonesFound = 0;
        for (int o = 0;o<origins.length;o++) {
            int sequentialOrigin = userToSequentialLookup[origins[o]];
            if (sequentialOrigin == -1) {
                sequentialOrigin = zonesFound;
                zonesFound++;
                userToSequentialLookup[origins[o]]=sequentialOrigin;
                sequentialToUserLookup[sequentialOrigin] = origins[o];
            }
        }
        int[] externalZoneNumbers = new int[zonesFound+1];
        for(int i=1;i<externalZoneNumbers.length;i++){
            externalZoneNumbers[i]=sequentialToUserLookup[i-1];
        }
        //enable garbage collection
        origins = null;
        
        int[] fieldIds = new int[fieldsToAdd.length];
        for (int mNum=0; mNum < fieldsToAdd.length; mNum++) {
            if (matrixNameList.contains(fieldsToAdd[mNum])) {
                fieldIds[mNum] = -1;
                logger.warn("SomeSkims already contains matrix named "+fieldsToAdd[mNum]+", not reading it in again");
            } else {
                fieldIds[mNum] = s.getColumnPosition(fieldsToAdd[mNum]);
                if (fieldIds[mNum]<=0) {
                    logger.fatal("No field named "+fieldsToAdd[mNum]+ " in skim TableDataSet "+s);
                    throw new RuntimeException("No field named "+fieldsToAdd[mNum]+ " in skim TableDataSet "+s);
                }
            }
        }
        float [][][] tempArrays = new float[fieldsToAdd.length][zonesFound][zonesFound];
        
        // initialize to NaN so that missing values throw errors later
        for (float[][] array : tempArrays) {
        	for (float[] row : array) {
        		for (int i = 0; i< row.length; i++) {
        			row[i] = Float.NaN;
        		}
        	}
        }
        
        
        for (int row = 1;row <= s.getRowCount();row++) {
            int origin = (int) s.getValueAt(row,originField);
            int destination = (int) s.getValueAt(row,destinationField);
            for (int entry = 0;entry<fieldIds.length;entry++) {
                if (fieldIds[entry]>0) {
                    tempArrays[entry][userToSequentialLookup[origin]][userToSequentialLookup[destination]] = s.getValueAt(row,fieldIds[entry]);
                }
            }
        }
        
        for (int matrixToBeAdded =0; matrixToBeAdded<fieldsToAdd.length; matrixToBeAdded++) {
            if (fieldIds[matrixToBeAdded]>0) {
                matrixNameList.add(fieldsToAdd[matrixToBeAdded]);
                Matrix m = new Matrix(fieldsToAdd[matrixToBeAdded],"",tempArrays[matrixToBeAdded]);
                m.setExternalNumbers(externalZoneNumbers);
                this.matrixList.add(m);
            }
        }
        
        matrices = (Matrix[]) matrixList.toArray(matrices);
        
        logger.info("Finished reading TableDataSet skims "+s+" into memory");
    }




    public double getUtility(int fromZoneUserNumber, int toZoneUserNumber, TravelUtilityCalculatorInterface tp, boolean useRouteChoice) {
        return tp.getUtility(fromZoneUserNumber, toZoneUserNumber, this);
    }

    public double[] getUtilityComponents(int fromZoneUserNumber, int toZoneUserNumber, TravelUtilityCalculatorInterface tp, boolean useRouteChoice) {
        return tp.getUtilityComponents(fromZoneUserNumber, toZoneUserNumber, this);
    }

    
    /* (non-Javadoc)
     * @see com.pb.models.pecas.TransportKnowledge#getUtility(com.pb.models.pecas.AbstractZone, com.pb.models.pecas.AbstractZone, com.pb.models.pecas.TravelUtilityCalculatorInterface, boolean)
     */
    public double getUtility(AbstractZone from, AbstractZone to, TravelUtilityCalculatorInterface tp, boolean useRouteChoice) {
        return getUtility(from.getZoneUserNumber(), to.getZoneUserNumber(), tp, useRouteChoice);
    }

    public int getMatrixId(String string) {
        
        return matrixNameList.indexOf(string);
    }

    /* (non-Javadoc)
     * @see com.pb.models.pecas.TransportKnowledge#getUtilityComponents(com.pb.models.pecas.AbstractZone, com.pb.models.pecas.AbstractZone, com.pb.models.pecas.TravelUtilityCalculatorInterface, boolean)
     */
    public double[] getUtilityComponents(AbstractZone from, AbstractZone to, TravelUtilityCalculatorInterface tp, boolean useRouteChoice) {
        return getUtilityComponents(from.getZoneUserNumber(), to.getZoneUserNumber(), tp, useRouteChoice);
    }



    /**
     * @param my1stPath The my1stPath to set.
     */
    public void setMy1stPath(String my1stPath) {
        this.my1stPath = my1stPath;
    }

    /**
     * @param my2ndPath The my2ndPath to set.
     */
    public void setMy2ndPath(String my2ndPath) {
        this.my2ndPath = my2ndPath;
    }

	public void checkCompleteness(ArrayList<Integer> internalZoneNumbers, ArrayList<Integer> externalZoneNumbers) {
		boolean ok=true;
		for (Matrix m : matrices) {
			for (int origin : internalZoneNumbers) {
				for (int destination : internalZoneNumbers) {
					if (Float.isNaN(m.getValueAt(origin, destination))) {
						logger.fatal("Skim "+m+" from "+origin+" to "+destination+" has not been set");
						ok=false;
					}
				}
				for (int destination : externalZoneNumbers) {
					if (Float.isNaN(m.getValueAt(origin, destination))) {
						logger.fatal("I-E Skim "+m+" from "+origin+" to "+destination+" has not been set");
						ok=false;
					}
					 // also check destination to origin for the external zones, here in the inner loop
					if (Float.isNaN(m.getValueAt(destination, origin))) {
						logger.fatal("E-I Skim "+m+" from "+origin+" to "+destination+" has not been set");
						ok=false;
					}

				}
			}
		}
		if (!ok) {
			throw new RuntimeException("Skims are missing, see error messages");
		}
	}

};
