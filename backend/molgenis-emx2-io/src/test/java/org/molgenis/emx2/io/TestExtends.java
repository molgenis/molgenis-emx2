package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestExtends {

  static Database database;
  static Schema schema;
  static Schema schema2;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(TestExtends.class.getSimpleName() + "2");
    database.dropSchemaIfExists(TestExtends.class.getSimpleName());
  }

  @Test
  public void importColumnOrderSubclass() throws IOException {
    // we want to import a file that has inheritance and keep column order usefull
    // therefore mix the columns in one list, then order should stick (both import/export
    // consistent)

    schema = database.createSchema(TestExtends.class.getSimpleName());
    String schemaDef1 =
        """
                        tableName,tableExtends,columnName,key,description
                        shape,,,,root class is shape that has general properties
                        square,shape,,,special shape that has sidelength
                        circle,shape,,,special shape that has radius
                        shape,,name,1,should be first column in all shapes
                        square,,sidelength,,should be second column in square
                        circle,,radius,,should be second column in circle
                        shape,,description,,all shapes have description, should be last column in all shapes in this schema""";

    // load in memory
    SchemaMetadata sm = Emx2.fromRowList(CsvTableReader.read(new StringReader(schemaDef1)));
    validate1(sm);

    // load into system
    schema.migrate(sm);
    validate1(schema.getMetadata());

    // second load should make no errors, see #3655
    schema.migrate(sm);

    // export again
    List<Row> emx = Emx2.toRowList(schema.getMetadata());

    // load into memory again
    sm = Emx2.fromRowList(emx);
    validate1(sm);

    // load into system again (to complete roundtrip) and validate again
    schema = database.dropCreateSchema(schema.getName());
    schema.migrate(sm);
    validate1(schema.getMetadata());

    // now, to make it really difficult, we add a second schema expanding on the first
    schema2 = database.dropCreateSchema(schema.getName() + "2");
    String schemaDef2 =
        """
                        tableName,tableExtends,refSchema,columnName,key,description
                        myshape,shape,TestExtends,,,root class is shape in the other schema
                        rectangle,myshape,,,,
                        triangle,myshape,,,,
                        myshape,,,color,,,all my shapes also have color
                        triangle,,,adjacent,,,
                        triangle,,,opposite,,,
                        triangle,,,hypotenuse,,,
                        rectangle,,,width,,,
                        rectangle,,,height,,,
                        myshape,,,author,,,all my shapes also have author as last column""";

    sm = Emx2.fromRowList(CsvTableReader.read(new StringReader(schemaDef2)));
    schema2.migrate(sm);
    validate2(sm);
  }

  private void validate2(SchemaMetadata sm) {
    assertEquals(4, sm.getTableMetadata("myshape").getColumnsWithoutMetadata().size());
    assertEquals("name", sm.getTableMetadata("myshape").getColumns().get(0).getName());
    assertEquals("description", sm.getTableMetadata("myshape").getColumns().get(1).getName());
    assertEquals("color", sm.getTableMetadata("myshape").getColumns().get(2).getName());
    assertEquals("author", sm.getTableMetadata("myshape").getColumns().get(3).getName());

    assertEquals(7, sm.getTableMetadata("triangle").getColumnsWithoutMetadata().size());
    assertEquals("name", sm.getTableMetadata("triangle").getColumns().get(0).getName());
    assertEquals("description", sm.getTableMetadata("triangle").getColumns().get(1).getName());
    assertEquals("color", sm.getTableMetadata("triangle").getColumns().get(2).getName());
    assertEquals("adjacent", sm.getTableMetadata("triangle").getColumns().get(3).getName());
    assertEquals("opposite", sm.getTableMetadata("triangle").getColumns().get(4).getName());
    assertEquals("hypotenuse", sm.getTableMetadata("triangle").getColumns().get(5).getName());
    assertEquals("author", sm.getTableMetadata("triangle").getColumns().get(6).getName());

    assertEquals(6, sm.getTableMetadata("rectangle").getColumnsWithoutMetadata().size());
    assertEquals("name", sm.getTableMetadata("rectangle").getColumns().get(0).getName());
    assertEquals("description", sm.getTableMetadata("rectangle").getColumns().get(1).getName());
    assertEquals("color", sm.getTableMetadata("rectangle").getColumns().get(2).getName());
    assertEquals("width", sm.getTableMetadata("rectangle").getColumns().get(3).getName());
    assertEquals("height", sm.getTableMetadata("rectangle").getColumns().get(4).getName());
    assertEquals("author", sm.getTableMetadata("rectangle").getColumns().get(5).getName());
  }

  @Test
  public void multiParentInheritanceCsvRoundTrip() throws IOException {
    String csv =
        """
        tableName,tableExtends,columnName,key
        Experiments,,,
        sampling,Experiments,,
        sequencing,Experiments,,
        WGS,"sampling,sequencing",,
        Experiments,,id,1
        Experiments,,name,
        sampling,,sample_type,
        sequencing,,library_strategy,
        WGS,,coverage,
        """;

    SchemaMetadata sm = Emx2.fromRowList(CsvTableReader.read(new StringReader(csv)));

    assertArrayEquals(
        new String[] {"sampling", "sequencing"},
        sm.getTableMetadata("WGS").getExtendNames(),
        "WGS should inherit from both sampling and sequencing after import");

    List<Row> exported = Emx2.toRowList(sm);

    Row wgsTableRow =
        exported.stream()
            .filter(
                r -> "WGS".equals(r.getString("tableName")) && r.getString("columnName") == null)
            .findFirst()
            .orElseThrow(() -> new AssertionError("WGS table row not found in export"));

    String tableExtendsValue = wgsTableRow.getString("tableExtends");
    assertEquals(
        "sampling,sequencing",
        tableExtendsValue,
        "tableExtends should be comma-separated for multi-parent inheritance");

    SchemaMetadata reimported = Emx2.fromRowList(exported);

    assertArrayEquals(
        new String[] {"sampling", "sequencing"},
        reimported.getTableMetadata("WGS").getExtendNames(),
        "WGS should still inherit from both sampling and sequencing after re-import");
  }

  @Test
  public void internalTableTypeRoundTrip() throws IOException {
    String csv =
        """
        tableName,tableExtends,tableType,columnName,key
        Experiments,,,id,1
        Experiments,,,name,
        Experiments,,,experiment_type,
        Sampling,Experiments,INTERNAL,sample_type,
        Sequencing,Experiments,INTERNAL,library_strategy,
        WGS,"Sampling,Sequencing",,coverage,
        """;

    SchemaMetadata sm = Emx2.fromRowList(CsvTableReader.read(new StringReader(csv)));

    assertEquals(
        TableType.INTERNAL,
        sm.getTableMetadata("Sampling").getTableType(),
        "Sampling should have INTERNAL table type after import");
    assertEquals(
        TableType.INTERNAL,
        sm.getTableMetadata("Sequencing").getTableType(),
        "Sequencing should have INTERNAL table type after import");
    assertEquals(
        TableType.DATA,
        sm.getTableMetadata("WGS").getTableType(),
        "WGS should have DATA table type");

    List<Row> exported = Emx2.toRowList(sm);

    Row samplingRow =
        exported.stream()
            .filter(
                r ->
                    "Sampling".equals(r.getString("tableName"))
                        && r.getString("columnName") == null)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Sampling table row not found in export"));

    assertEquals(
        "INTERNAL",
        samplingRow.getString("tableType"),
        "Sampling tableType should be INTERNAL in export");

    SchemaMetadata reimported = Emx2.fromRowList(exported);

    assertEquals(
        TableType.INTERNAL,
        reimported.getTableMetadata("Sampling").getTableType(),
        "Sampling should still have INTERNAL table type after re-import");
    assertEquals(
        TableType.INTERNAL,
        reimported.getTableMetadata("Sequencing").getTableType(),
        "Sequencing should still have INTERNAL table type after re-import");
  }

  @Test
  public void multiParentWithInternalTablesAndProfileColumnRoundTrip() throws IOException {
    String csv =
        """
        tableName,tableExtends,tableType,columnName,key,columnType
        Experiments,,,id,1,
        Experiments,,,name,,
        Experiments,,,experiment_type,,profile
        Sampling,Experiments,INTERNAL,sample_type,,
        Sequencing,Experiments,INTERNAL,library_strategy,,
        WGS,"Sampling,Sequencing",,coverage,,int
        """;

    SchemaMetadata sm = Emx2.fromRowList(CsvTableReader.read(new StringReader(csv)));

    assertEquals(TableType.INTERNAL, sm.getTableMetadata("Sampling").getTableType());
    assertEquals(TableType.INTERNAL, sm.getTableMetadata("Sequencing").getTableType());
    assertArrayEquals(
        new String[] {"Sampling", "Sequencing"}, sm.getTableMetadata("WGS").getExtendNames());

    assertNotNull(
        sm.getTableMetadata("Experiments").getColumn("experiment_type"),
        "experiment_type column should exist");
    assertEquals(
        ColumnType.VARIANT,
        sm.getTableMetadata("Experiments").getColumn("experiment_type").getColumnType(),
        "experiment_type should have VARIANT column type");

    List<Row> exported = Emx2.toRowList(sm);
    SchemaMetadata reimported = Emx2.fromRowList(exported);

    assertEquals(TableType.INTERNAL, reimported.getTableMetadata("Sampling").getTableType());
    assertEquals(TableType.INTERNAL, reimported.getTableMetadata("Sequencing").getTableType());
    assertArrayEquals(
        new String[] {"Sampling", "Sequencing"},
        reimported.getTableMetadata("WGS").getExtendNames());
    assertEquals(
        ColumnType.VARIANT,
        reimported.getTableMetadata("Experiments").getColumn("experiment_type").getColumnType());
  }

  @Test
  public void oneStepCsvImportMultiParentWithProfileColumn() throws IOException {
    String csv =
        """
        tableName,tableType,tableExtends,columnName,columnType,key,required
        Experiments,,,id,STRING,1,TRUE
        Experiments,,,experiment_type,VARIANT,,
        sampling,INTERNAL,Experiments,tissue_type,STRING,,
        sequencing,INTERNAL,Experiments,read_length,INT,,
        WGS,,"sampling,sequencing",coverage,DECIMAL,,
        Imaging,,Experiments,modality,STRING,,
        """;

    SchemaMetadata sm = Emx2.fromRowList(CsvTableReader.read(new StringReader(csv)));

    assertArrayEquals(
        new String[] {"sampling", "sequencing"},
        sm.getTableMetadata("WGS").getExtendNames(),
        "WGS should inherit from both sampling and sequencing in memory");
    assertEquals(
        ColumnType.VARIANT,
        sm.getTableMetadata("Experiments").getColumn("experiment_type").getColumnType(),
        "Experiments should have a VARIANT column in memory");
    assertEquals(
        TableType.INTERNAL,
        sm.getTableMetadata("sampling").getTableType(),
        "sampling should have INTERNAL table type in memory");

    String schemaName = "TestExtendsOneStep";
    database.dropSchemaIfExists(schemaName);
    Schema oneStepSchema = database.createSchema(schemaName);
    try {
      oneStepSchema.migrate(sm);

      assertArrayEquals(
          new String[] {"sampling", "sequencing"},
          oneStepSchema.getMetadata().getTableMetadata("WGS").getExtendNames(),
          "WGS should inherit from both sampling and sequencing after DB migrate");
      assertEquals(
          ColumnType.VARIANT,
          oneStepSchema
              .getMetadata()
              .getTableMetadata("Experiments")
              .getColumn("experiment_type")
              .getColumnType(),
          "Experiments should have a VARIANT column after DB migrate");
      assertEquals(
          TableType.INTERNAL,
          oneStepSchema.getMetadata().getTableMetadata("sampling").getTableType(),
          "sampling should have INTERNAL table type after DB migrate");
    } finally {
      database.dropSchemaIfExists(schemaName);
    }
  }

  private void validate1(SchemaMetadata sm) {
    // shape should have columns name',description
    assertEquals(2, sm.getTableMetadata("shape").getColumnsWithoutMetadata().size());
    assertEquals("name", sm.getTableMetadata("shape").getColumns().get(0).getName());
    assertEquals("description", sm.getTableMetadata("shape").getColumns().get(1).getName());

    // triangle should have columns: name, height, description
    assertEquals(3, sm.getTableMetadata("square").getColumnsWithoutMetadata().size());
    assertEquals("name", sm.getTableMetadata("square").getColumns().get(0).getName());
    assertEquals("sidelength", sm.getTableMetadata("square").getColumns().get(1).getName());
    assertEquals("description", sm.getTableMetadata("square").getColumns().get(2).getName());

    // cirle should have columns: name, radius, description
    assertEquals(3, sm.getTableMetadata("circle").getColumnsWithoutMetadata().size());
    assertEquals("name", sm.getTableMetadata("circle").getColumns().get(0).getName());
    assertEquals("radius", sm.getTableMetadata("circle").getColumns().get(1).getName());
    assertEquals("description", sm.getTableMetadata("circle").getColumns().get(2).getName());
  }
}
