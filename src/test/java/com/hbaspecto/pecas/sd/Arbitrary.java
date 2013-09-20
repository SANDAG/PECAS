package com.hbaspecto.pecas.sd;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.TableDataFileWriter;
import com.pb.common.datafile.TableDataSet;

public class Arbitrary
{
    private Arbitrary()
    {
        //not called
    }
    
    // A temporary class for testing simple code snippets.
    public static void main(String[] args) throws IOException
    {
        final TableDataSet blah = new TableDataSet();
        blah.appendColumn(new float[0], "Meamer");
        blah.appendColumn(new int[0], "Sheamer");
        blah.appendColumn(new String[0], "Lemur");
        final HashMap<Object, Object> data = new HashMap<Object, Object>();
        data.put("Meamer", 6.1f);
        data.put("Sheamer", 19f);
        data.put("Lemur", "Strepsirrhini");
        blah.appendRow(data);
        final TableDataFileWriter writer = new CSVFileWriter();
        writer.writeFile(blah, new File("E:\\Models\\blah.csv"));
        writer.close();
    }
}
