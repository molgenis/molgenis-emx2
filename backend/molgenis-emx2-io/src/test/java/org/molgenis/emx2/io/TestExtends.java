package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestExtends {

  private static final String PARENT_DEF =
      """
      tableName,tableExtends,refSchema,columnName,key
      shape,,,,
      shape,,,name,1""";

  static Database database;
  static Schema schema;
  static Schema schema2;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(TestExtends.class.getSimpleName() + "2");
    database.dropSchemaIfExists(TestExtends.class.getSimpleName());
    database.dropSchemaIfExists("TestExtendsExpB");
    database.dropSchemaIfExists("TestExtendsExpA");
    database.dropSchemaIfExists("TestExtendsRtFresh");
    database.dropSchemaIfExists("TestExtendsRtB");
    database.dropSchemaIfExists("TestExtendsRtA");
    database.dropSchemaIfExists("TestExtendsReB");
    database.dropSchemaIfExists("TestExtendsReA");
    database.dropSchemaIfExists("TestExtendsSelf");
    database.dropSchemaIfExists("TestExtendsMsgB");
    database.dropSchemaIfExists("TestExtendsMsgA");
    database.dropSchemaIfExists("TestExtendsFreshMsg");
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

  @Test
  public void exportWritesRefSchema() {
    Schema parent = createParentSchema("TestExtendsExpA");
    Schema child = createChildSchema("TestExtendsExpB", parent.getName());

    Row myshapeRow = tableRow(child.getMetadata(), "myshape");

    assertEquals("shape", myshapeRow.getString(Emx2.TABLE_EXTENDS));
    assertEquals(parent.getName(), myshapeRow.getString(Emx2.REF_SCHEMA));
  }

  @Test
  public void exportOmitsSelfRefSchema() {
    String schemaName = "TestExtendsSelf";
    Schema selfSchema = createParentSchema(schemaName);
    selfSchema.migrate(
        Emx2.fromRowList(
            CsvTableReader.read(
                new StringReader(
                    """
                    tableName,tableExtends,refSchema,columnName,key
                    square,shape,%s,,
                    square,,,sidelength,"""
                        .formatted(schemaName)))));

    assertEquals(schemaName, selfSchema.getMetadata().getTableMetadata("square").getImportSchema());
    assertNull(tableRow(selfSchema.getMetadata(), "square").getString(Emx2.REF_SCHEMA));
  }

  @Test
  public void crossSchemaRoundTrip() {
    Schema parent = createParentSchema("TestExtendsRtA");
    Schema child = createChildSchema("TestExtendsRtB", parent.getName());

    List<Row> exported = Emx2.toRowList(child.getMetadata());

    database.dropSchemaIfExists("TestExtendsRtFresh");
    Schema fresh = database.createSchema("TestExtendsRtFresh");
    fresh.migrate(Emx2.fromRowList(exported));

    assertEquals(
        parent.getName(), fresh.getMetadata().getTableMetadata("myshape").getImportSchema());
    assertTrue(
        fresh.getMetadata().getTableMetadata("myshape").getColumnNames().contains("name"),
        "inherited column 'name' should be present");
  }

  @Test
  public void exportThenReimportOverExisting() {
    Schema parent = createParentSchema("TestExtendsReA");
    Schema child = createChildSchema("TestExtendsReB", parent.getName());

    List<Row> exported = Emx2.toRowList(child.getMetadata());
    child.migrate(Emx2.fromRowList(exported));

    assertEquals(
        parent.getName(), child.getMetadata().getTableMetadata("myshape").getImportSchema());
  }

  @Test
  public void reimportWithoutRefSchemaGivesMessageNamingTheTable() {
    Schema parent = createParentSchema("TestExtendsMsgA");
    Schema child = createChildSchema("TestExtendsMsgB", parent.getName());

    SchemaMetadata withoutRefSchema =
        Emx2.fromRowList(
            CsvTableReader.read(
                new StringReader(
                    """
                    tableName,tableExtends,columnName,key
                    myshape,shape,,
                    myshape,,color,""")));

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> child.migrate(withoutRefSchema));

    assertFalse(exception.getMessage().contains("null"), exception.getMessage());
    assertTrue(
        exception
            .getMessage()
            .contains(
                "Table 'myshape' cannot inherit table 'shape': not found or permission denied. If the table lives in another schema, provide refSchema."),
        exception.getMessage());
  }

  @Test
  public void importIntoFreshSchemaWithDanglingParentGivesMessageWithoutNull() {
    Schema fresh = database.createSchema("TestExtendsFreshMsg");

    SchemaMetadata danglingParent =
        Emx2.fromRowList(
            CsvTableReader.read(
                new StringReader(
                    """
                    tableName,tableExtends,columnName,key
                    Employee,Contact,,
                    Employee,,salary,""")));

    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> fresh.migrate(danglingParent));

    assertFalse(exception.getMessage().contains("null"), exception.getMessage());
    assertTrue(
        exception
            .getMessage()
            .contains(
                "Table 'TestExtendsFreshMsg.Employee' cannot inherit table 'Contact': not found or permission denied. If the table lives in another schema, provide refSchema."),
        exception.getMessage());
  }

  private Schema createParentSchema(String parentName) {
    Schema parent = database.createSchema(parentName);
    parent.migrate(Emx2.fromRowList(CsvTableReader.read(new StringReader(PARENT_DEF))));
    return parent;
  }

  private Schema createChildSchema(String childName, String parentName) {
    Schema child = database.createSchema(childName);
    child.migrate(
        Emx2.fromRowList(
            CsvTableReader.read(
                new StringReader(
                    """
                    tableName,tableExtends,refSchema,columnName,key
                    myshape,shape,%s,,
                    myshape,,,color,"""
                        .formatted(parentName)))));
    return child;
  }

  private Row tableRow(SchemaMetadata metadata, String tableName) {
    return Emx2.toRowList(metadata).stream()
        .filter(
            row ->
                tableName.equals(row.getString(Emx2.TABLE_NAME))
                    && row.getString(Emx2.COLUMN_NAME) == null)
        .findFirst()
        .orElseThrow();
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
