package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.io.readers.CsvTableReader;

class TestEmx2EnumValues {

  private static final String MODEL =
      """
      tableName,columnName,columnType,key,values
      Sample,,,,
      Sample,id,string,1,
      Sample,answer,enum,,"yes,no,unknown"
      Sample,colors,enum_array,,"red,green,blue"
      """;

  @Test
  void valuesColumnIsReadInOrderWrittenAndWrittenBack() {
    SchemaMetadata schema = Emx2.fromRowList(CsvTableReader.read(new StringReader(MODEL)));
    Column answer = schema.getTableMetadata("Sample").getColumn("answer");
    Column colors = schema.getTableMetadata("Sample").getColumn("colors");

    assertEquals(ColumnType.ENUM, answer.getColumnType());
    assertEquals(List.of("yes", "no", "unknown"), answer.getAllowedValues());
    assertEquals(ColumnType.ENUM_ARRAY, colors.getColumnType());
    assertEquals(List.of("red", "green", "blue"), colors.getAllowedValues());
    assertNull(schema.getTableMetadata("Sample").getColumn("id").getAllowedValues());

    assertTrue(Emx2.getHeaders(schema).contains("values"));
    SchemaMetadata roundTripped = Emx2.fromRowList(Emx2.toRowList(schema));
    assertEquals(
        List.of("red", "green", "blue"),
        roundTripped.getTableMetadata("Sample").getColumn("colors").getAllowedValues());
    assertEquals(
        List.of("yes", "no", "unknown"),
        roundTripped.getTableMetadata("Sample").getColumn("answer").getAllowedValues());
  }

  @Test
  void valueContainingCommaSurvivesRoundTripAndQuotedCsvCell() {
    SchemaMetadata schema = new SchemaMetadata();
    schema.create(
        TableMetadata.table("T")
            .add(Column.column("k").setPkey())
            .add(Column.column("c").setType(ColumnType.ENUM).setAllowedValues("a,b", "c")));

    SchemaMetadata roundTripped = Emx2.fromRowList(Emx2.toRowList(schema));
    assertEquals(
        List.of("a,b", "c"), roundTripped.getTableMetadata("T").getColumn("c").getAllowedValues());

    String csv =
        "tableName,columnName,columnType,key,values\nT,,,,\nT,k,string,1,\nT,c,enum,,\"\"\"a,b\"\",c\"\n";
    SchemaMetadata parsed = Emx2.fromRowList(CsvTableReader.read(new StringReader(csv)));
    assertEquals(
        List.of("a,b", "c"), parsed.getTableMetadata("T").getColumn("c").getAllowedValues());
  }
}
