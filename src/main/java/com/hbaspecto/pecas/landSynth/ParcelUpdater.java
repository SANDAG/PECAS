package com.hbaspecto.pecas.landSynth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.pb.common.util.ResourceUtil;

public class ParcelUpdater extends Thread {
	private final ConcurrentLinkedQueue<UpdateData> queue;
	private PreparedStatement statement;
	private Connection conn;
	// private Properties connectionProps;
	private final Logger logger = Logger.getLogger(ParcelUpdater.class.getName());
	private boolean capped = false;
	private final ResourceBundle localProps;
	final String parcelTableName;
	final String parcelIdField;
	private boolean isAutoCommit = true;

	private static AtomicLong totalBlockTime = new AtomicLong(0);
	private static AtomicLong totalWaitTime = new AtomicLong(0);

	public ParcelUpdater(ResourceBundle props, String parcelTableName,
			String parcelIDField) throws SQLException {
		this.parcelTableName = parcelTableName;
		parcelIdField = parcelIDField;
		localProps = props;
		isAutoCommit = ResourceUtil.getBooleanProperty(localProps, "isAutoCommit",
				true);
		queue = new ConcurrentLinkedQueue<UpdateData>();

	}

	public void requestUpdateIntCoverage(long id, int intCoverage, float quantity) {
		final UpdateData update = new UpdateData();
		update.id = id;
		update.intCoverage = intCoverage;
		update.quantity = quantity;
		update.coverageIsInt = true;
		queue.add(update);
		interrupt();
	}

	public void requestUpdateStringCoverage(long id, String stringCoverage,
			float quantity) {
		final UpdateData update = new UpdateData();
		update.id = id;
		update.stringCoverage = stringCoverage;
		update.quantity = quantity;
		update.coverageIsInt = false;
		queue.add(update);
		interrupt();
	}

	// Indicate that no more updates will be requested.
	public void cap() {
		capped = true;
	}

	@Override
	public void run() {
		try {
			getUpdateStatement();

			int count = 0;
			while (true) {
				final UpdateData next = queue.poll();
				if (next == null) {
					if (capped) {
						break;
					}
					final long start = System.currentTimeMillis();
					try {

						statement.close();
						AssignSquareFeetToParcel.closeConnection(conn,
								"Closing connection for Updater");
						Thread.sleep(1000);
					}
					catch (final InterruptedException e) {
					}

					final long waitTime = System.currentTimeMillis() - start;
					totalWaitTime.getAndAdd(waitTime);
				}
				else {
					getUpdateStatement();
					statement.setFloat(1, next.quantity);
					if (next.coverageIsInt) {
						statement.setInt(2, next.intCoverage);
					}
					else {
						statement.setString(2, next.stringCoverage);
					}
					statement.setLong(3, next.id);
					final long start = System.currentTimeMillis();
					statement.executeUpdate();
					final long blockTime = System.currentTimeMillis() - start;
					totalBlockTime.getAndAdd(blockTime);
					if (count % 5000 == 0) {
						logger.info("Updated parcel " + next.id);
						conn.commit();
					}
					// No committing for individual records anymore
					// conn.commit();
				}
				count++;
			}// end of while
			statement.close();
			AssignSquareFeetToParcel.closeConnection(conn,
					"Closing connection for Updater");

			logger.info("Finished updating");
			logger.info("Spent " + totalBlockTime
					+ " milliseconds blocked on database updates.");
			logger.info("Spent " + totalWaitTime
					+ "milliseconds waiting for update requests.");
		}
		catch (final SQLException e) {
			final String msg = "Can't update parcels";
			logger.fatal(msg, e);
			throw new RuntimeException(msg, e);
		}
	}

	private void getUpdateStatement() throws SQLException {
		if (conn != null) {
			if (!conn.isClosed()) {
				setAvalidUpdateStatment(conn);
			}
			else { // conn is closed
				conn = AssignSquareFeetToParcel.getNewConnection(localProps, false,
						isAutoCommit, "The updater");
				setAvalidUpdateStatment(conn);
			}

		}
		else { // conn is null
			conn = AssignSquareFeetToParcel.getNewConnection(localProps, false,
					isAutoCommit, "The updater");
			setAvalidUpdateStatment(conn);
		}

	}

	private void setAvalidUpdateStatment(Connection conn) throws SQLException {

		if (statement == null) {
			if (ParcelInMemory.spaceTypeIntColumnName != null) {
				statement = conn.prepareStatement("UPDATE " + parcelTableName
						+ " SET \"" + ParcelInMemory.spaceAmountColumnName + "\" = ?, \""
						+ ParcelInMemory.spaceTypeIntColumnName + "\" = ? " + "WHERE \""
						+ parcelIdField + "\" = ?");
			}
			else {
				statement = conn.prepareStatement("UPDATE " + parcelTableName
						+ " SET \"" + ParcelInMemory.spaceAmountColumnName + "\" = ?, \""
						+ ParcelInMemory.spaceTypeStringColumnName + "\" = ? " + "WHERE \""
						+ parcelIdField + "\" = ?");
			}

		}
		else { // statement is not null
			if (statement.isClosed()) {
				if (ParcelInMemory.spaceTypeIntColumnName != null) {
					statement = conn.prepareStatement("UPDATE " + parcelTableName
							+ " SET \"" + ParcelInMemory.spaceAmountColumnName + "\" = ?, \""
							+ ParcelInMemory.spaceTypeIntColumnName + "\" = ? " + "WHERE \""
							+ parcelIdField + "\" = ?");
				}
				else {
					statement = conn.prepareStatement("UPDATE " + parcelTableName
							+ " SET \"" + ParcelInMemory.spaceAmountColumnName + "\" = ?, \""
							+ ParcelInMemory.spaceTypeStringColumnName + "\" = ? "
							+ "WHERE \"" + parcelIdField + "\" = ?");
				}
			}
		}
	}

	private class UpdateData {
		private long id;
		private int intCoverage;
		private String stringCoverage;
		private float quantity;
		private boolean coverageIsInt;
	}
}
