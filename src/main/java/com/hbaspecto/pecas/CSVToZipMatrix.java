package com.hbaspecto.pecas;

import java.io.File;
import com.pb.common.matrix.CSVMatrixReader;
import com.pb.common.matrix.ZipMatrixWriter;

public class CSVToZipMatrix
{
    public static void main(String[] args)
    {
        final File fromFile = new File(args[0]);
        final File toFile = new File(args[1]);

        final CSVMatrixReader reader = new CSVMatrixReader(fromFile);
        final ZipMatrixWriter writer = new ZipMatrixWriter(toFile);

        writer.writeMatrix(reader.readMatrix());
    }
}
