package com.hbaspecto.pecas;

import java.io.File;

import com.pb.common.matrix.CSVMatrixReader;
import com.pb.common.matrix.ZipMatrixWriter;

public class CSVToZipMatrix {

	public static void main(String[] args) {
		File fromFile = new File(args[0]);
		File toFile = new File(args[1]);
		
		CSVMatrixReader reader = new CSVMatrixReader(fromFile);
		ZipMatrixWriter writer = new ZipMatrixWriter(toFile);
		
		writer.writeMatrix(reader.readMatrix());
	}

}
