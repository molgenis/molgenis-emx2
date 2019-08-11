package org.molgenis.emx2.io;

import org.molgenis.MolgenisException;
import org.molgenis.Table;
import org.molgenis.emx2.io.csv.CsvRowWriter;

import java.io.IOException;
import java.io.Writer;

public class MolgenisExporter {

  private MolgenisExporter() {
    // hide constructor
  }

  public static void exportCsv(Table table, Writer writer) throws MolgenisException, IOException {
    CsvRowWriter.writeCsv(table.retrieve(), writer);
  }
}
