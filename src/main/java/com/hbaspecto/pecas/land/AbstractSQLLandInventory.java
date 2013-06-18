/*
 * Copyright 2007 HBA Specto Incorporated
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
package com.hbaspecto.pecas.land;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.pb.common.datafile.JDBCTableReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.sql.JDBCConnection;

public abstract class AbstractSQLLandInventory implements LandInventory {

	static final int maxCharCodes = 256;
	protected static transient Logger logger = Logger
			.getLogger(AbstractSQLLandInventory.class);
	protected double maxParcelSize = Double.POSITIVE_INFINITY;
	protected final Connection conn;
	protected final String tableName;
	protected int zoneColumnNumber = -1;
	protected int coverageColumnNumber = -1;
	protected int quantityColumnNumber = -1;
	protected int zoningColumnNumber = -1;
	protected int idColumnNumber = -1;
	protected int yearBuiltColumnNumber = -1;
	protected int sizeColumnNumber = -1;
	protected int serviceColumnNumber = -1;
	protected boolean integerCodeForCoverage = false;
	/**
	 * Current value of currentZone.
	 */
	protected int currentZone;
	/**
	 * Current value of id2.
	 */
	protected long id2;
	protected String idColumnName;
	protected String zoneColumnName;
	protected String coverageColumnName;
	protected String quantityColumnName;
	protected String zoningColumnName;
	protected String yearBuiltColumnName;
	protected String sizeColumnName;
	protected String landDatabaseDriver;
	protected String landDatabaseSpecifier;
	Double[][] prices = new Double[maxCharCodes][];
	protected String landDatabaseUser;
	protected String landDatabasePassword;

	public AbstractSQLLandInventory(String landDatabaseDriver,
			String landDatabaseSpecifier, String tableName, String user,
			String password) throws SQLException {
		landDatabaseUser = user;
		landDatabasePassword = password;
		this.landDatabaseDriver = landDatabaseDriver;
		this.landDatabaseSpecifier = landDatabaseSpecifier;
		this.tableName = tableName;
		try {
			Class.forName(landDatabaseDriver).newInstance();

			if (user != null) {
				conn = DriverManager.getConnection(landDatabaseSpecifier, user,
						password);
			}
			else {
				conn = DriverManager.getConnection(landDatabaseSpecifier);
			}
		}
		catch (final Exception e) {
			System.out.println("Error opening land database");
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void setColumnNames(String zoneColumnName, String coverageColumnName,
			String quantityColumnName, String zoningColumnName,
			String yearBuiltColumnName, String sizeColumnName) {
		this.zoneColumnName = zoneColumnName;
		this.coverageColumnName = coverageColumnName;
		this.quantityColumnName = quantityColumnName;
		this.zoningColumnName = zoningColumnName;
		this.yearBuiltColumnName = yearBuiltColumnName;
		this.sizeColumnName = sizeColumnName;
		zoneColumnNumber = -1;
		coverageColumnNumber = -1;
		quantityColumnNumber = -1;
		zoningColumnNumber = -1;
		yearBuiltColumnNumber = -1;
		sizeColumnNumber = -1;
		serviceColumnNumber = -1;

	}

	public void putPrice(int zoneNumber, int coverageType, double price) {
		if (zoneNumber >= currentSizeOfPriceStorageArrays) {
			for (int i = 0; i < maxCharCodes; i++) {
				if (prices[i] != null) {
					final Double[] oldPrices = prices[i];
					prices[i] = new Double[zoneNumber + 1];
					System.arraycopy(oldPrices, 0, prices[i], 0, oldPrices.length);
				}
			}
			currentSizeOfPriceStorageArrays = zoneNumber;
		}
		if (prices[coverageType] == null) {
			prices[coverageType] = new Double[currentSizeOfPriceStorageArrays + 1];
		}
		prices[coverageType][zoneNumber] = price;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hbaspecto.pecas.land.LandInventory#summarizeInventory(java.lang.String
	 * , java.lang.String)
	 */
	public TableDataSet summarizeInventory(String commodityNameTable,
			String commodityNameColumn) {
		final String createTableString = "SELECT " + tableName + "."
				+ zoneColumnName + " AS FloorspaceZone, " + commodityNameTable + "."
				+ commodityNameColumn + ", " + tableName + "." + coverageColumnName
				+ " AS pecastype, Sum(" + tableName + "." + quantityColumnName
				+ ") AS Quantity INTO Floorspace " + "FROM " + commodityNameTable
				+ " INNER JOIN " + tableName + " ON " + commodityNameTable + "."
				+ coverageColumnName + " = " + tableName + "." + coverageColumnName
				+ " GROUP BY " + tableName + "." + zoneColumnName + ", "
				+ commodityNameTable + "." + commodityNameColumn + ", " + tableName
				+ "." + coverageColumnName + ";";

		return createFloorspaceTableFromQuery(createTableString);
	}

	/**
	 * @deprecated use database views instead, define floorspacei in the database
	 *             as part of the install
	 * @param createTableString
	 * @return
	 */
	@Deprecated
	public TableDataSet createFloorspaceTableFromQuery(String createTableString) {
		Statement aNewStatement = null;
		try {
			aNewStatement = conn.createStatement();
		}
		catch (final SQLException e) {
			throw new RuntimeException(
					"Can't recreate statement to summarize floorspace inventory", e);
		}
		try {
			aNewStatement.execute("DROP TABLE FLOORSPACE;");
		}
		catch (final SQLException e) {
			System.out.println("can't delete existing floorspace table " + e);
		}
		try {
			aNewStatement.execute(createTableString);
		}
		catch (final SQLException e) {
			throw new RuntimeException("Can't create floorspace inventory table", e);
		}
		final JDBCConnection jdbcConn = new JDBCConnection(landDatabaseSpecifier,
				landDatabaseDriver, landDatabaseUser, landDatabasePassword);
		final JDBCTableReader reader = new JDBCTableReader(jdbcConn);
		try {
			return reader.readTable("FLOORSPACE");
		}
		catch (final IOException e) {
			throw new RuntimeException("Can't read in floorspace inventory", e);
		}
	}

	@Override
	public double getPrice(int coverageCode, int currentYear, int baseYear) {
		// TODO needs to apply local level effect modifiers
		if (prices[coverageCode] == null) {
			throw new RuntimeException("No price set for coverage " + coverageCode
					+ "(integer code " + coverageCode + ") zone number " + currentZone);
		}
		final Double price = prices[coverageCode][currentZone];
		if (price == null) {
			final String msg = "No price set for coverage " + coverageCode + " zone "
					+ currentZone;
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
		return price.doubleValue();
	}

	public double getLocalVacancyRate(int coverageCode, double radius) {
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.pb.models.pecas.land.LandInventory#summarizeInventory(com.pb.common
	 * .datafile.TableDataSet, java.lang.String)
	 */
	@Override
	public abstract TableDataSet summarizeInventory();

	public void setIntegerCodeForCoverage(boolean integerCodeForCoverage) {
		this.integerCodeForCoverage = integerCodeForCoverage;
	}

	public boolean isIntegerCodeForCoverage() {
		return integerCodeForCoverage;
	}

	public void setIdColumnName(String idColumnName) {
		this.idColumnName = idColumnName;
		idColumnNumber = -1;
	}

	public String getIdColumnName() {
		return idColumnName;
	}

	@Override
	public abstract String getParcelId();

	public TableDataSet readInventoryTable(String tableName) {
		final JDBCConnection jdbcConn = new JDBCConnection(landDatabaseSpecifier,
				landDatabaseDriver, landDatabaseUser, landDatabasePassword);
		final JDBCTableReader reader = new JDBCTableReader(jdbcConn);
		try {
			final TableDataSet s = reader.readTable(tableName);
			if (s == null) {
				logger.fatal("Query " + tableName
						+ " to summarize inventory has problems");
				throw new RuntimeException("Query " + tableName
						+ " to summarize inventory has problems");
			}
			return s;
		}
		catch (final IOException e) {
			logger.fatal("Can't run query " + tableName
					+ " to summarize floorspace inventory");
			throw new RuntimeException("Can't run query " + tableName
					+ " to summarize floorspace inventory", e);
		}
	}

	@Override
	public double getMaxParcelSize() {
		return maxParcelSize;
	}

	@Override
	public void setMaxParcelSize(double maxParcelSize) {
		this.maxParcelSize = maxParcelSize;
	}

	protected ArrayList<Parcels> newBitsToBeAdded = new ArrayList<Parcels>();
	protected ArrayList<Integer> zoneNumbers;
	int currentSizeOfPriceStorageArrays = 4000;

	protected int[] getZoneNumbers() throws SQLException {
		zoneNumbers = new ArrayList<Integer>();
		final String queryString = "SELECT " + zoneColumnName + " FROM \""
				+ tableName + "\" GROUP BY " + zoneColumnName + ";";
		final Statement statement = conn.createStatement(
				ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
		if (statement.getMaxRows() != 0) {
			System.out
					.println("Max rows is set by default in statement .. attempting to remove maxrows limitation");
			statement.setMaxRows(0);
		}

		final ResultSet r = statement.executeQuery(queryString);
		while (r.next()) {
			zoneNumbers.add(new Integer(r.getInt(zoneColumnName)));
		}
		final int[] zoneNumbersInt = new int[zoneNumbers.size()];
		for (int i = 0; i < zoneNumbers.size(); i++) {
			zoneNumbersInt[i] = zoneNumbers.get(i).intValue();
		}
		return zoneNumbersInt;
	}

	protected boolean findNextZoneNumber() {
		if (zoneNumbers == null) {
			try {
				getZoneNumbers();
			}
			catch (final SQLException e) {
				logger.fatal("Error setting up iteration through parcels", e);
				throw new RuntimeException(
						"Error setting up iteration through parcels", e);
			}
		}
		int currentIndex = zoneNumbers.lastIndexOf(new Integer(currentZone));
		currentIndex++;
		if (currentIndex >= zoneNumbers.size()) {
			return false;
		}
		else {
			currentZone = zoneNumbers.get(currentIndex);
			logger.info("now trying parcels with currentZone =" + currentZone);
			id2 = 0;
		}
		return true;
	}

	@Override
	public String parcelToString() {
		return getParcelId() + "," + currentZone + "," + id2;
	}

	public int getServiceCode() {
		throw new RuntimeException(
				"SQLLandInventory.getServiceCode() is not implemented");
	}

	public void putServiceCode(int serviceCode) {
		throw new RuntimeException(
				"SQLLandInventory.putServiceCode() is not implemented");
	}

	@Override
	public long getPECASParcelNumber() {
		return 0;
	}

	public void init() {
	}

}
