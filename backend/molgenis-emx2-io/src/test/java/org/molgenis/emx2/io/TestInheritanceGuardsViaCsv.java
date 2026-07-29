package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestInheritanceGuardsViaCsv {

  private static final String CANNOT_CHANGE =
      "': inheritance cannot be changed after the table is created.";

  private static Database database;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  private static void migrateCsv(Schema schema, String csv) {
    schema.migrate(Emx2.fromRowList(CsvTableReader.read(new StringReader(csv))));
  }

  private static void assertRefused(Schema schema, String csv, String expectedMessage) {
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> migrateCsv(schema, csv));
    assertTrue(exception.getMessage().contains(expectedMessage), exception.getMessage());
  }

  @Test
  public void everyInheritanceChangeIsRefusedViaCsvImport() {
    Schema change = database.dropCreateSchema("CsvGuardChange");
    migrateCsv(
        change,
        """
        tableName,tableExtends,columnName,key
        Shape,,,
        Shape,,name,1
        Other,,,
        Other,,oname,1
        MyShape,Shape,,
        MyShape,,size,""");

    Schema add = database.dropCreateSchema("CsvGuardAdd");
    migrateCsv(
        add,
        """
        tableName,tableExtends,columnName,key
        Shape,,,
        Shape,,name,1
        MyShape,,,
        MyShape,,myid,1""");

    database.dropSchemaIfExists("CsvGuardChild");
    database.dropSchemaIfExists("CsvGuardParentA");
    database.dropSchemaIfExists("CsvGuardParentB");
    migrateCsv(
        database.createSchema("CsvGuardParentA"),
        """
        tableName,columnName,key
        Shape,,
        Shape,name,1""");
    migrateCsv(
        database.createSchema("CsvGuardParentB"),
        """
        tableName,columnName,key
        Shape,,
        Shape,name,1""");
    Schema child = database.createSchema("CsvGuardChild");
    migrateCsv(
        child,
        """
        tableName,tableExtends,refSchema,columnName,key
        MyShape,Shape,CsvGuardParentA,,
        MyShape,,,size,""");

    Schema remove = database.dropCreateSchema("CsvGuardRemove");
    migrateCsv(
        remove,
        """
        tableName,tableExtends,columnName,key
        Shape,,,
        Shape,,name,1
        MyShape,Shape,,
        MyShape,,size,""");

    Schema local = database.dropCreateSchema("CsvGuardLocal");
    migrateCsv(
        local,
        """
        tableName,tableExtends,columnName,key
        Shape,,,
        Shape,,name,1
        MyShape,Shape,,
        MyShape,,size,""");

    Schema rename = database.dropCreateSchema("CsvGuardRename");
    migrateCsv(
        rename,
        """
        tableName,tableExtends,columnName,key
        Shape,,,
        Shape,,name,1
        MyShape,Shape,,
        MyShape,,size,""");

    assertAll(
        () ->
            assertRefused(
                change,
                """
                tableName,tableExtends,columnName,key
                Shape,,,
                Shape,,name,1
                Other,,,
                Other,,oname,1
                MyShape,Other,,
                MyShape,,size,""",
                "Cannot change tableExtends of table 'CsvGuardChange.MyShape" + CANNOT_CHANGE),
        () ->
            assertRefused(
                add,
                """
                tableName,tableExtends,columnName,key
                Shape,,,
                Shape,,name,1
                MyShape,Shape,,
                MyShape,,size,""",
                "Cannot set tableExtends of table 'CsvGuardAdd.MyShape" + CANNOT_CHANGE),
        () ->
            assertRefused(
                child,
                """
                tableName,tableExtends,refSchema,columnName,key
                MyShape,Shape,CsvGuardParentB,,
                MyShape,,,size,""",
                "Cannot change refSchema of table 'CsvGuardChild.MyShape" + CANNOT_CHANGE),
        () ->
            assertRefused(
                remove,
                """
                tableName,tableExtends,columnName,key
                Shape,,,
                Shape,,name,1
                MyShape,,,
                MyShape,,size,""",
                "Cannot remove tableExtends of table 'CsvGuardRemove.MyShape" + CANNOT_CHANGE),
        () ->
            assertRefused(
                local,
                """
                tableName,tableExtends,refSchema,columnName,key
                Shape,,,,
                Shape,,,name,1
                MyShape,Shape,CsvGuardParentA,,
                MyShape,,,size,""",
                "Cannot change refSchema of table 'CsvGuardLocal.MyShape" + CANNOT_CHANGE),
        () ->
            assertRefused(
                rename,
                """
                tableName,oldName,tableExtends,columnName,key
                Renamed,Shape,,,
                Renamed,,,name,1
                MyShape,,Shape,,
                MyShape,,,size,""",
                "Cannot rename table 'CsvGuardRename.Shape': table 'CsvGuardRename.MyShape' inherits from it. Renaming a table with inheriting children is not supported yet."));
  }
}
