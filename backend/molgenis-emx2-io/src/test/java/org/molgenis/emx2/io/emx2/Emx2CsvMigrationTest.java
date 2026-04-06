package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.readers.CsvTableReader;

class Emx2CsvMigrationTest {

  @Test
  void testLegacyProfilesColumnMapsToSubsetsOnRead() throws Exception {
    String csv = "tableName,columnName,profiles\n" + "MyTable,,wgs\n" + "MyTable,myCol,rna\n";
    Iterable<Row> rows = CsvTableReader.read(new StringReader(csv));
    SchemaMetadata schema = Emx2.fromRowList(rows);

    assertArrayEquals(
        new String[] {"wgs"},
        schema.getTableMetadata("MyTable").getProfiles(),
        "legacy CSV 'profiles' column on table row should map to subsets");
    assertArrayEquals(
        new String[] {"rna"},
        schema.getTableMetadata("MyTable").getColumn("myCol").getProfiles(),
        "legacy CSV 'profiles' column on column row should map to subsets");
  }
}
