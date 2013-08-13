package com.hbaspecto.pecas;

import java.io.File;

import com.pb.common.matrix.CSVMatrixWriter;
import com.pb.common.matrix.ZipMatrixReader;

public class ZipMatrixToCSV {

	public static void main(String[] args) {
		File fromFile = new File(args[0]);
		File toFile = new File(args[1]);
		
		ZipMatrixReader reader = new ZipMatrixReader(fromFile);
		CSVMatrixWriter writer = new CSVMatrixWriter(toFile);
		
		writer.writeMatrix(reader.readMatrix());
	}

}
