package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestExtends {

  static Database database;
  static Schema schema;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestExtends.class.getSimpleName());
  }

  @Test
  public void importColumnOrderSubclass() throws IOException {
    // we want to import a file that has inheritance and keep column order usefull
    // therefore mix the columns in one list, then order should stick (both import/export
    // consistent)

    String schema1 =
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
    SchemaMetadata sm = Emx2.fromRowList(CsvTableReader.read(new StringReader(schema1)));
    validate1(sm);

    // load into system
    schema.migrate(sm);
    validate1(schema.getMetadata());

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
    schema = database.dropCreateSchema(schema.getName() + "2");
    String schema2 =
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

    sm = Emx2.fromRowList(CsvTableReader.read(new StringReader(schema2)));
    schema.migrate(sm);
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
