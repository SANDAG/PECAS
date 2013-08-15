package com.hbaspecto.pecas.landSynth;

import java.sql.*;
import java.util.*;

import com.hbaspecto.pecas.land.LoadingQueue;

// import org.sqlite.SQLiteJDBCLoader;

public class ParcelCoverageSynthesizer extends AssignSquareFeetToParcel implements Runnable {

	ParcelCoverageSynthesizer(LoadingQueue<Integer> queue) {
		super(queue);
	}

	ArrayList<ParcelInMemory> currentParcels = new ArrayList<ParcelInMemory>();
	static int threadCount = 3;

	private static final int THREAD_DELAY = 1000;
	//Connection generalConnection; 
	ParcelUpdater updater;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		props = loadProperties(args);
		String threadCountString = props.getProperty("ThreadCount");
		if(threadCountString!=null) {
			threadCount = Integer.valueOf(threadCountString).intValue();
		}


		ParcelCoverageSynthesizer mySquareFeetToParcel[] = new ParcelCoverageSynthesizer[threadCount];

		// TODO figure out something other than a LoadingQueue (don't want a fixed size); we don't
		// want it to block on inserts.
		LoadingQueue<Integer> zoneNumberQueue = new LoadingQueue<Integer>(10000);

		for(int i = 0; i < mySquareFeetToParcel.length; i++){
			mySquareFeetToParcel[i] = new ParcelCoverageSynthesizer(zoneNumberQueue);
		}

		for(int i=0;i<mySquareFeetToParcel.length;i++) {
			mySquareFeetToParcel[i].setupDatabaseAccess(props);        	
			mySquareFeetToParcel[i].initialize();
			mySquareFeetToParcel[i].setUpInventory();
			mySquareFeetToParcel[i].setUpSorters();
		}

		// put zone numbers in queue
		for(Integer zone : getZoneNumbers()) {
			zoneNumberQueue.add(zone);
		}
		zoneNumberQueue.finished = true;


		Thread[] threads = new Thread[mySquareFeetToParcel.length];
		for(int i=0;i<mySquareFeetToParcel.length;i++) {
			Thread thread = new Thread(mySquareFeetToParcel[i]);
			thread.start();
			threads[i] = thread;
			try
			{
				Thread.sleep(THREAD_DELAY);
			}
			catch(InterruptedException e)
			{
			}
		}

		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				logger.fatal("Thread was interrupted");
				throw new RuntimeException("Thread was interrupted");
			}
		}
		logger.info("All threads have finished");


	}

	@Override
    public void run() {
		assignSquareFeet();
	}

	@Override
	public void assignSquareFeet()
	{
		super.assignSquareFeet();
		updater.cap();
		logger.info("Waiting for updater to finish");
		try {
			updater.join();
		} catch (InterruptedException e) {
			logger.error("Updater thread was interrupted");
		}
		logger.info("Thread is finished...");
	}

	public static Collection<Integer> getZoneNumbers() {

		Connection tempConn;
		Statement getSqFtStatement ;
		ArrayList<Integer> zoneNumbers = new ArrayList<Integer>();

		try {
			tempConn = AssignSquareFeetToParcel.getNewConnection(props, true, false, "getting the Zone numbers");

			// Statement getSqFtStatement =
			// conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_READ_ONLY);
			getSqFtStatement = tempConn.createStatement(
					ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY); // SQLite only
			// supports
			// TYPE_FORWARD_ONLY
			// cursors

			//Added by Eric
			getSqFtStatement.setQueryTimeout(0);

			// get a list of the types and a list of the zones, could use the
			// database to group by gridcode and zoneColumnName
			ResultSet zoneSet = getSqFtStatement.executeQuery("SELECT distinct "
					+ floorspaceZoneColumnName + " FROM " + sqftInventoryTableName);
			while (zoneSet.next()) {
				// TODO for testing: only get the the first TAZ.
				//for(int i = 0; i < 500; i++)
				//{
				//    zoneSet.next();
				int zoneNumber = zoneSet.getInt(1);
				zoneNumbers.add(zoneNumber);				
			}
			getSqFtStatement.close();
			String msg ="Closing connection for Zone numbers.";
			closeConnection(tempConn, msg);
		}
		catch(Exception e)
		{
			throw new RuntimeException("Can't get zone numbers", e);
		}
		return zoneNumbers;
	}

	@Override
	protected boolean putParcelsInArray(Integer taz) 
	throws SQLException {
		Connection localConn= getConnection("loading parcels in "+ taz);
		Statement stmt = localConn.createStatement(ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY); // SQLite supports only TYPE_FORWARD_ONLY, CONCUR_READ_ONLY    
		//Added by Eric
		stmt.setQueryTimeout(0);
		try{	
			if (stmt.getMaxRows() != 0) {
				System.out.println("Max rows is set by default in statement .. attempting to remove maxrows limitation");
				stmt.setMaxRows(0);
			}
		} catch (SQLException e){
			logger.warn("Couldn't set the maxRows property in the database, manually check to see if all parcels are being processed.");
		}

		ResultSet result;
		if(bufferSize == 0)
			// Read only by the parcels within the TAZ.
			// TODO select column names that user has specified, instead of all column names
			result = stmt.executeQuery("SELECT * FROM " + parcelTableName + " where \""
					+ parcelZoneColumnName + "\"=" + taz);
		else
		{
			// Read parcels within a buffer zone.
			String query = "SELECT * FROM " + parcelTableName
			+ " p WHERE st_intersects(st_centroid(p." + parcelGeomName
			+ "), (SELECT st_buffer(t." + tazGeomName + ", " + bufferSize + ") FROM "
			+ tazTableName + " t WHERE t.\"" + tazNumberName + "\"=" + taz + "))";
			System.out.println(query);
			result = stmt.executeQuery(query);
		}
		stmt.setMaxRows(0);
		// System.out.println( result.isClosed());
		boolean foundNone = true;

		int count = 0;

		try {
			Map<String, Object> properties;

			int idColumnNumber = result.findColumn(parcelIdField);
			ResultSetMetaData meta = result.getMetaData();

			FieldNameReference fr = new FieldNameReference();
			for(int i = 1; i <= meta.getColumnCount(); i++)
			{
				int datatype = meta.getColumnType(i);

				if(datatype == Types.INTEGER)
				{
					fr.integerFieldNames.add(meta.getColumnName(i));
				}
				else if(datatype == Types.DOUBLE)
				{
					fr.doubleFieldNames.add(meta.getColumnName(i));
				}
				else if(datatype == Types.CHAR || datatype == Types.VARCHAR
						|| datatype == Types.LONGVARCHAR || datatype == Types.NVARCHAR ||
						datatype == Types.LONGNVARCHAR)
				{
					fr.stringFieldNames.add(meta.getColumnName(i));
				}
				else if(datatype == Types.BOOLEAN || datatype == Types.BIT){
					fr.booleanFieldNames.add(meta.getColumnName(i));
				} else if (datatype == Types.BIGINT) {
					fr.longFieldNames.add(meta.getColumnName(i));
				}
				else {
					logger.warn("Unrecognized data type "+datatype+" for field "+meta.getColumnName(i));
				}
			}
			while(result.next()) {
				foundNone = false;
				count++;
				if((count)%5000 == 0) {
					logger.info("Processing parcel " + count + " in TAZ " + taz);
				}
				// get one parcel
				String id = result.getString(idColumnNumber);

				// put the parcel properties in the map.

				ParcelInMemory p = new ParcelInMemory(Long.valueOf(id), fr);

				for(int i = 1; i <= meta.getColumnCount(); i++)
				{
					int datatype = meta.getColumnType(i);

					if(datatype == Types.INTEGER)
					{
						int field = result.getInt(i);
						p.addIntField(field);
					}
					else if(datatype == Types.DOUBLE)
					{
						double field = result.getDouble(i);
						p.addDoubleField(field);
					}
					else if(datatype == Types.CHAR || datatype == Types.VARCHAR
							|| datatype == Types.LONGVARCHAR)
					{
						String field = result.getString(i);
						p.addStringField(field);
					}
					else if(datatype == Types.BOOLEAN || datatype == Types.BIT){
						boolean  field = result.getBoolean(i);
						p.addBooleanField(field);
					}
				}


				currentParcels.add(p);

				for (List<ParcelInterface> aParcelArray : sortedParcelLists.values()) {
					// add it to each list
					aParcelArray.add(p);
				}
			}
			stmt.close();						
			String message = "Closing connection for "+ taz;			
			closeConnection(localConn, message);
		} catch(Exception e) {
			logger.error("Couldn't iterate through the parcels.", e);
		} finally {
			if (foundNone) {
				// we have no parcels for this zone
				logger.error("No parcels found for zone " + taz
						+ ", not assigning floorspace inventory to parcels/gridcells");
				return false;
			}
		}
		return true;
	}

	@Override
	protected void finishedProcessingTAZ() throws SQLException {
		//PreparedStatement updateStatement = createUpdateStatement();

		for(int i = 0; i < currentParcels.size();i++) {
			currentParcels.get(i).putPecasFieldsBackToDatabase(updater);
		}
		currentParcels.clear();

		//updateStatement.close();
	}

	@Override
	public void initialize() {
		super.initialize();
		ParcelInMemory.zoneColumnName = parcelZoneColumnName;
		ParcelInMemory.initialFARColumnName = initialFARColumnName;
		ParcelInMemory.landAreaColumnName = areaColumnName;
		ParcelInMemory.parcelIdField = parcelIdField;
		ParcelInMemory.parcelTableName = parcelTableName;
		user = props.getProperty("DatabaseUser");
		password = props.getProperty("DatabasePassword");
		ParcelInMemory.spaceAmountColumnName = props.getProperty("SpaceAmountField");
		boolean intSpaceTypeCode = props.getProperty("IntegerSpaceTypeCode").equalsIgnoreCase(
		"true");
		if(intSpaceTypeCode) {
			ParcelInMemory.spaceTypeIntColumnName = props.getProperty("SpaceTypeField");
		} else {
			ParcelInMemory.spaceTypeStringColumnName = props.getProperty("SpaceTypeField");
		}
		ParcelInMemory.database = database;

		// Start the updater.
		try {
			updater = new ParcelUpdater(props ,parcelTableName, parcelIdField);            
			updater.start();
		}
		catch(SQLException e) {
			logger.fatal("Cannot create updater", e);
			throw new RuntimeException(e);
		}
	}

}
