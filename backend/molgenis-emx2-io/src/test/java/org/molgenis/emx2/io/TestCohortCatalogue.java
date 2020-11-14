package org.molgenis.emx2.io;

import static org.junit.Assert.*;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.SelectColumn.s;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.rowstore.TableStoreForXlsxFile;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.StopWatch;

/** representative import file for testing */
public class TestCohortCatalogue {

  static Database database;
  static Schema schema;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(TestCohortCatalogue.class.getSimpleName());
  }

  @Test
  public void importTest() {
    StopWatch.print("begin");

    ClassLoader classLoader = getClass().getClassLoader();
    Path file = new File(classLoader.getResource("cohort_catalogue.xlsx").getFile()).toPath();

    TableStoreForXlsxFile store = new TableStoreForXlsxFile(file);

    SchemaMetadata cohortSchema = Emx2.fromRowList(store.readTable("molgenis"));

    System.out.println(cohortSchema);

    StopWatch.print("schema loaded, now creating tables");

    database.tx(
        db -> {
          runImportProcedure(store, cohortSchema);
          StopWatch.print("import of data complete");
        });

    // repeat for idempotency test (should not change anything)
    database.tx(
        db -> {
          runImportProcedure(store, cohortSchema);
          StopWatch.print("import of data complete");
        });

    assertEquals(23, schema.getTableNames().size());

    // test the queries still work

    // REF_ARRAY
    List<Row> rows = schema.getTable("VariableHarmonization").query().retrieveRows();
    assertEquals(
        "table1",
        rows.get(0).getString("targetTable")); // is 'hidden' column only visible on row select

    String result =
        schema
            .getTable("VariableHarmonization")
            .query()
            .select(s("sourceVariables", s("collection"), s("table"), s("name")))
            .retrieveJSON();
    System.out.println("result ref_array:\n" + result);
    assertTrue(result.contains("CHOP"));

    // with filter
    result =
        schema
            .getTable("VariableHarmonization")
            .query()
            .select(s("sourceVariables", s("collection"), s("table"), s("name")))
            .where(
                f(
                    "sourceVariables",
                    f("collection", Operator.EQUALS, "CHOP"),
                    f("name", Operator.EQUALS, "soc_born_father")))
            .retrieveJSON();
    System.out.println("result ref_array filtered:\n" + result);
    assertTrue(result.contains("CHOP"));

    // REF
    result =
        schema
            .getTable("VariableHarmonization")
            .query()
            .select(s("targetVariable", s("collection"), s("table"), s("name")))
            .retrieveJSON();
    System.out.println("result ref:\n" + result);
    assertTrue(result.contains("LifeCycle"));

    // REFBACK
    result =
        schema
            .getTable("AbstractVariable")
            .query()
            .select(
                s("collection"),
                s("table"),
                s("name"),
                s("harmonisations", s("sourceCollection"), s("sourceTable"), s("sourceVariables")))
            .retrieveJSON();
    System.out.println("result refback:\n" + result);
    assertTrue(result.contains("LifeCycle"));

    // REFBACK via subclass

    result =
        schema
            .getTable("Variable")
            .query()
            .select(
                s("collection"),
                s("table"),
                s("name"),
                s("harmonisations", s("sourceCollection"), s("sourceTable"), s("sourceVariables")))
            .retrieveJSON();
    System.out.println("result refback subclass:\n" + result);
    assertTrue(result.contains("LifeCycle"));

    // test the delete still works
    try {
      schema
          .getTable("Variable")
          .delete(new Row("collection", "CHOP", "table", "table1", "name", "soc_born_father"));
      fail("should have failed");
    } catch (Exception e) {
      // ok
      System.out.println("failed correctly: " + e.getMessage());
    }

    // so first remove harmonization
    schema
        .getTable("VariableHarmonization")
        .delete(
            new Row(
                "targetCollection",
                "LifeCycle",
                "targetTable",
                "table1",
                "targetVariable",
                "abroad_fa",
                "sourceCollection",
                "CHOP"));

    // then variable used
    schema
        .getTable("Variable")
        .delete(new Row("collection", "CHOP", "table", "table1", "name", "soc_born_father"));
  }

  private void runImportProcedure(TableStoreForXlsxFile store, SchemaMetadata cohortSchema) {
    schema.merge(cohortSchema);

    StopWatch.print("creation of tables complete, now starting import data");

    for (String tableName : schema.getTableNames()) {
      if (store.containsTable(tableName))
        schema.getTable(tableName).update(store.readTable(tableName)); // actually upsert
    }
  }
}
