package com.hbaspecto.pecas.land;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;


public class ParcelErrorLog {

	static Logger logger = Logger.getLogger(ParcelErrorLog.class);

	BufferedWriter errorLogBuffer;
	int counter =0;

	public void open(String fileNameAndPath) {
		counter =0;
		try {
			errorLogBuffer = new BufferedWriter(new FileWriter(fileNameAndPath));
		} catch (IOException e) {
			logger.fatal("Can't open error log",e);
			throw new RuntimeException("Can't open error log", e);
		}
	}
	public void close() {
		try {
			errorLogBuffer.close();
		} catch (IOException e) {
			logger.error("Can't close stream");
			e.printStackTrace();
		}
	}
	public void flush() {
		try {
			errorLogBuffer.flush();
		} catch (IOException e) {
			logger.error("Can't close stream");
			e.printStackTrace();
		}
	}

	public void logParcelError(LandInventory land, Exception ex) {
		try {
			counter++;
			StringBuffer buffer = new StringBuffer("");
			buffer.append(counter+", ");
			buffer.append("Pecas Parcel number: " + land.getPECASParcelNumber()+ ", ");
			buffer.append(ex.getMessage() +"\n");
			errorLogBuffer.write(buffer.toString());
		} catch (IOException e) {
			logger.fatal("Can't write out to development log",e);
			throw new RuntimeException("Can't write out to development log", e);
		}
	}

}
