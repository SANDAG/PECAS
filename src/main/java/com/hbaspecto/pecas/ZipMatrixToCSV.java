package com.hbaspecto.pecas;

import java.io.File;
import com.pb.common.matrix.CSVMatrixWriter;
import com.pb.common.matrix.ZipMatrixReader;

public class ZipMatrixToCSV
{
    public static void main(String[] args)
    {
        final File fromFile = new File(args[0]);
        final File toFile = new File(args[1]);
        final ZipMatrixReader reader = new ZipMatrixReader(fromFile);
        final CSVMatrixWriter writer = new CSVMatrixWriter(toFile);

        writer.writeMatrix(reader.readMatrix());
    }
}
