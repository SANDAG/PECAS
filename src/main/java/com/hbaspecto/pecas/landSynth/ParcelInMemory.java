/*
 * Created on May 14, 2008
 *
 * Copyright  2008 HBA Specto Incorporated
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

package com.hbaspecto.pecas.landSynth;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

/**
 * @author John Abraham
 * 
 */
public class ParcelInMemory implements ParcelInterface {

	private static int[] coverageTypeLookupArray = null;
	static int firstSpaceTypeID = 0;
	static int numberOfSpaceTypeIDs = 0;
	final double[] oldScores;

	@Override
	public String toString() {
		return "Parcel: " + getId() + ",Coverage type: " + getCoverage()
				+ ", Qnty: " + getQuantity();
	}

	final FieldNameReference fieldNameReference;
	public final char EMPTY_COVERAGE_PLACEHOLDER = '_';

	static String database = null;
	static String landAreaColumnName = null;
	static Logger logger = Logger.getLogger(ParcelInMemory.class.getName());
	static String parcelIdField = null;
	static String parcelTableName = null;
	static String zoneColumnName = null;
	static String spaceAmountColumnName = null;

	static String spaceTypeIntColumnName = null;
	static String spaceTypeStringColumnName = null;
	static String initialFARColumnName;
	final int[] intValues;
	final String[] stringValues;
	final double[] doubleValues;
	final boolean[] booleanValues;
	final long[] longValues;

	private final long id;

	float amountOfLand;
	float amountOfSpace = 0;
	int coverage = 0;
	int taz;
	float initialFAR;

	int intCounter = 0;
	int booleanCounter = 0;
	int stringCounter = 0;
	int doubleCounter = 0;

	boolean gotRegularFieldsFromDatabase = false;

	private int revision = 0;

	// protected HashMap<String, String> otherValues = new
	// HashMap<String,String>();
	// protected Object[] otherValues = null;

	/**
	 * 
	 */
	public ParcelInMemory(Long long1, FieldNameReference fr) {
		assert numberOfSpaceTypeIDs > 0 : "Need to set numberOfSpaceTypeIDs before calling the constructor";
		oldScores = new double[numberOfSpaceTypeIDs];
		scoresAreInvalid();
		fieldNameReference = fr;
		id = long1;
		intValues = new int[fr.integerFieldNames.size()];
		booleanValues = new boolean[fr.booleanFieldNames.size()];
		stringValues = new String[fr.stringFieldNames.size()];
		longValues = new long[fr.longFieldNames.size()];
		doubleValues = new double[fr.doubleFieldNames.size()];

	}

	private void scoresAreInvalid() {
		revision++;
		for (int i = 0; i < oldScores.length; i++) {
			oldScores[i] = Double.NaN;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hbaspecto.pecas.landSynth.ParcelInterface#addSqFtAssigned(float)
	 */
	@Override
	public void addSqFtAssigned(float amount) {
		if (!gotRegularFieldsFromDatabase) {
			getRegularValuesFromDatabase();
		}
		amountOfSpace += amount;
		scoresAreInvalid();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hbaspecto.pecas.landSynth.ParcelInterface#getCoverage()
	 */
	@Override
	public int getCoverage() {
		if (!gotRegularFieldsFromDatabase) {
			getRegularValuesFromDatabase();
		}
		return coverage;
	}

	@Override
	public long getId() {
		return id;
	}

	private void getOtherValueFromDatabase(String string) {
		// not needed, all other values are retrieved when the parcel is first
		// retrieved
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hbaspecto.pecas.landSynth.ParcelInterface#getQuantity()
	 */
	@Override
	public float getQuantity() {
		if (!gotRegularFieldsFromDatabase) {
			getRegularValuesFromDatabase();
		}
		return amountOfSpace;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hbaspecto.pecas.landSynth.ParcelInterface#getRevision()
	 */
	@Override
	public int getRevision() {
		return revision;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hbaspecto.pecas.landSynth.ParcelInterface#getSize()
	 */
	@Override
	public float getSize() {
		if (!gotRegularFieldsFromDatabase) {
			getRegularValuesFromDatabase();
		}
		return amountOfLand;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hbaspecto.pecas.landSynth.ParcelInterface#getTaz()
	 */
	@Override
	public int getTaz() {
		if (!gotRegularFieldsFromDatabase) {
			getRegularValuesFromDatabase();
		}
		return taz;
	}

	/**
	 * @see com.hbaspecto.pecas.landSynth.ParcelInterface#getValue(java.lang.String)
	 */
	@Override
	public String getValue(String fieldName) {
		final Object fieldValue = fieldNameReference.getField(fieldName, intValues,
				stringValues, doubleValues, booleanValues, longValues);
		if (fieldValue == null) {
			return null;
		}
		return fieldValue.toString();
	}

	private void getRegularValuesFromDatabase() {

		taz = (Integer) fieldNameReference.getField(zoneColumnName, intValues,
				stringValues, doubleValues, booleanValues, longValues);
		amountOfLand = ((Double) fieldNameReference.getField(landAreaColumnName,
				intValues, stringValues, doubleValues, booleanValues, longValues))
				.floatValue();
		amountOfSpace = ((Double) fieldNameReference.getField(
				spaceAmountColumnName, intValues, stringValues, doubleValues,
				booleanValues, longValues)).floatValue();
		initialFAR = 0;
		if (initialFARColumnName != null) {
			initialFAR = ((Double) fieldNameReference.getField(initialFARColumnName,
					intValues, stringValues, doubleValues, booleanValues, longValues))
					.floatValue();
		}
		if (spaceTypeIntColumnName != null) {
			coverage = (Integer) fieldNameReference.getField(spaceTypeIntColumnName,
					intValues, stringValues, doubleValues, booleanValues, longValues);
		}
		else if (spaceTypeStringColumnName != null) {
			final String coverageString = (String) fieldNameReference.getField(
					spaceTypeStringColumnName, intValues, stringValues, doubleValues,
					booleanValues, longValues);
			if (coverageString == null || coverageString.trim().isEmpty()) {
				coverage = EMPTY_COVERAGE_PLACEHOLDER;
			}
			else {
				coverage = coverageString.trim().charAt(0); // OK!
			}
		}
		// TODO Put this check
		/*
		 * if (set.next()) { // more than 1 parcel with same id! String
		 * msg=parcelIdField+" is not unique, more than one entry for "+getId();
		 * logger.fatal(msg); throw new RuntimeException(msg); }
		 */

		gotRegularFieldsFromDatabase = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hbaspecto.pecas.landSynth.ParcelInterface#setCoverage(int) You
	 * should consider the data type of coverage code; int or char
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.hbaspecto.pecas.landSynth.ParcelInterface#setQuantity(float)
	 */
	@Override
	public void setQuantity(float f) {
		if (!gotRegularFieldsFromDatabase) {
			getRegularValuesFromDatabase();
		}
		amountOfSpace = f;
		scoresAreInvalid();
	}

	public void putPecasFieldsBackToDatabase(ParcelUpdater updater) {
		if (spaceTypeIntColumnName != null) {
			updater.requestUpdateIntCoverage(getId(), getCoverage(), getQuantity());
		}
		else {
			final char coverageChar = (char) getCoverage();
			// if (getCoverage()==0) coverageChar = ' '; //OK
			updater.requestUpdateStringCoverage(getId(),
					String.valueOf(coverageChar), getQuantity());
		}
	}

	@Override
	public double getInitialFAR() {
		if (!gotRegularFieldsFromDatabase) {
			getRegularValuesFromDatabase();
		}
		return initialFAR;
	}

	@Override
	public boolean isVacantCoverege() {
		// TODO Auto-generated method stub
		boolean isVacant = false;
		if (!gotRegularFieldsFromDatabase) {
			getRegularValuesFromDatabase();
		}

		if (spaceTypeIntColumnName != null) {
			if (coverage == 0 || coverage == 95) {
				isVacant = true;
			}
		}
		else {
			if (coverage == EMPTY_COVERAGE_PLACEHOLDER) {
				isVacant = true;
			}
		}
		return isVacant;
	}

	@Override
	public boolean isSameSpaceType(String type) {
		if (!gotRegularFieldsFromDatabase) {
			getRegularValuesFromDatabase();
		}
		if (spaceTypeIntColumnName != null) {
			return coverage == Integer.parseInt(type);
		}
		else {
			return coverage == type.trim().charAt(0); // OK
		}
	}

	@Override
	public void setCoverage(String myCode) {
		// TODO Check here if coverage is a single- or multiple- character
		// string.
		if (!gotRegularFieldsFromDatabase) {
			getRegularValuesFromDatabase();
		}
		if (spaceTypeIntColumnName != null) {
			coverage = Integer.parseInt(myCode);
		}
		else {
			coverage = myCode.trim().charAt(0); // OK
		}
		scoresAreInvalid();
	}

	public void addIntField(int field) {
		intValues[intCounter] = field;
		intCounter++;
	}

	public void addDoubleField(double field) {
		doubleValues[doubleCounter] = field;
		doubleCounter++;
	}

	public void addStringField(String field) {
		stringValues[stringCounter] = field;
		stringCounter++;
	}

	public void addBooleanField(boolean field) {
		booleanValues[booleanCounter] = field;
		booleanCounter++;
	}

	@Override
	public double getOldScore(int intCoverageType) {
		final int index = lookupCoverageTypeIndex(intCoverageType);
		return oldScores[index];
	}

	private int lookupCoverageTypeIndex(int intCoverageType) {
		final int index = coverageTypeLookupArray[intCoverageType
				- firstSpaceTypeID];
		return index;

	}

	@Override
	public void setOldScore(int intCoverageType, double score) {
		final int index = lookupCoverageTypeIndex(intCoverageType);
		oldScores[index] = score;
	}

	public static void initializeLookupCoverageType(Connection conn,
			String FloorspaceInventoryTable, String floorspaceSpaceTypeColName,
			String sdMatchCoeffSpaceTypeTableName, String sdMatchCoeffSpaceTypeColName) {
		try {
			// SQLite only supports TYPE_FORWARD_ONLY cursors
			final Statement getSpaceTypesStmt = conn.createStatement(
					ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

			int numberOfSpaceTypes = 0;
			int maxCoverageTypeID = 0, minCoverageTypeID = 0;

			ResultSet spaceTypesSet = getSpaceTypesStmt
					.executeQuery("SELECT min(space_type_id) as min, max(space_type_id) as max, count(*) as count "
							+ " FROM ( "
							+ "SELECT distinct "
							+ floorspaceSpaceTypeColName
							+ " as space_type_id FROM "
							+ FloorspaceInventoryTable
							+ " UNION "
							+ "SELECT distinct "
							+ sdMatchCoeffSpaceTypeColName
							+ " as space_type_id FROM "
							+ sdMatchCoeffSpaceTypeTableName
							+ " ) p");

			while (spaceTypesSet.next()) {
				// Columns order is min, max, count
				minCoverageTypeID = spaceTypesSet.getInt(1); // min
				maxCoverageTypeID = spaceTypesSet.getInt(2); // max
				numberOfSpaceTypes = spaceTypesSet.getInt(3); // count
			}

			ParcelInMemory.numberOfSpaceTypeIDs = numberOfSpaceTypes;
			ParcelInMemory.firstSpaceTypeID = minCoverageTypeID;

			assert numberOfSpaceTypes > 0 : "There are no spacetypes in "
					+ FloorspaceInventoryTable;
			spaceTypesSet.close();

			spaceTypesSet = getSpaceTypesStmt
					.executeQuery(" SELECT distinct space_type_id " + " FROM ( "
							+ "SELECT distinct " + floorspaceSpaceTypeColName
							+ " as space_type_id FROM " + FloorspaceInventoryTable
							+ " UNION " + "SELECT distinct " + sdMatchCoeffSpaceTypeColName
							+ " as space_type_id FROM " + sdMatchCoeffSpaceTypeTableName
							+ " ) p" + " ORDER BY space_type_id");

			// size of the array is last+first+1
			coverageTypeLookupArray = new int[maxCoverageTypeID - minCoverageTypeID
					+ 1];

			for (int i = 0; i < coverageTypeLookupArray.length; i++) {
				coverageTypeLookupArray[i] = Integer.MAX_VALUE;
			}
			int coverageType;
			int seqIndex = 0;
			while (spaceTypesSet.next()) {
				coverageType = spaceTypesSet.getInt(1);
				coverageTypeLookupArray[coverageType - minCoverageTypeID] = seqIndex;
				seqIndex++;
			}
			spaceTypesSet.close();
			getSpaceTypesStmt.close();

		}
		catch (final Exception e) {
			logger.fatal("Unexpected error occured.", e);
			throw new RuntimeException(e);
		}
	}

}
