package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;

class Emx2CsvMigrationTest {

  @Test
  void testEnumColumnCsvRoundtrip() throws Exception {
    SchemaMetadata original = new SchemaMetadata();
    TableMetadata table = original.create(new TableMetadata("Patients"));
    table.add(new Column("id").setKey(1));
    table.add(
        new Column("smoking_status")
            .setType(ColumnType.ENUM)
            .setValues("never", "former", "current"));

    TableStoreForCsvInMemory store = new TableStoreForCsvInMemory();
    Emx2.outputMetadata(store, original);

    SchemaMetadata reimported = Emx2.fromRowList(store.readTable("molgenis"));

    Column col = reimported.getTableMetadata("Patients").getColumn("smoking_status");
    assertNotNull(col);
    assertEquals(ColumnType.ENUM, col.getColumnType());
    assertArrayEquals(new String[] {"never", "former", "current"}, col.getValues());
  }

  @Test
  void testEnumArrayColumnCsvRoundtrip() throws Exception {
    SchemaMetadata original = new SchemaMetadata();
    TableMetadata table = original.create(new TableMetadata("Survey"));
    table.add(new Column("id").setKey(1));
    table.add(
        new Column("diet")
            .setType(ColumnType.ENUM_ARRAY)
            .setValues("vegan", "vegetarian", "omnivore"));

    TableStoreForCsvInMemory store = new TableStoreForCsvInMemory();
    Emx2.outputMetadata(store, original);

    SchemaMetadata reimported = Emx2.fromRowList(store.readTable("molgenis"));

    Column col = reimported.getTableMetadata("Survey").getColumn("diet");
    assertNotNull(col);
    assertEquals(ColumnType.ENUM_ARRAY, col.getColumnType());
    assertArrayEquals(new String[] {"vegan", "vegetarian", "omnivore"}, col.getValues());
  }

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
