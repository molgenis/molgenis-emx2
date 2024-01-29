package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestSumQuery {
  private static final String TEST_SUM_QUERY = "TestSumQuery";
  static Database database;
  static Schema schema;
  private static final String SAMPLES = "Samples";
  private static final String TYPE = "Type";
  private static final String NAME = "Name";
  private static final String GENDER = "Gender";
  private static final String N = "N";

  @BeforeAll
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();

    // createColumn a schema to test with
    schema = database.dropCreateSchema(TEST_SUM_QUERY);

    // createColumn some tables with contents
    final Table typeTable = schema.create(table(TYPE).add(column(NAME).setPkey()));
    typeTable.insert(
        new Row().setString(NAME, "Type a"),
        new Row().setString(NAME, "Type b"),
        new Row().setString(NAME, "Type c"));
    final Table genderTable = schema.create(table(GENDER).add(column(NAME).setPkey()));
    genderTable.insert(new Row().setString(NAME, "M"), new Row().setString(NAME, "F"));

    final Table samplesTable =
        schema.create(
            table(SAMPLES)
                .add(column("N").setType(INT))
                .add(column(TYPE).setType(REF).setRefTable(TYPE).setPkey())
                .add(column(GENDER).setType(REF).setRefTable(GENDER).setPkey()));

    samplesTable.insert(
        new Row().setInt(N, 23).setString(TYPE, "Type a").setString(GENDER, "M"),
        new Row().setInt(N, 5).setString(TYPE, "Type a").setString(GENDER, "F"),
        new Row().setInt(N, 2).setString(TYPE, "Type b").setString(GENDER, "M"),
        new Row().setInt(N, 9).setString(TYPE, "Type b").setString(GENDER, "F"));
  }

  @Test
  public void testSumQuery() {
    //  // direct jooq works;
    //    final DSLContext jooq = ((SqlDatabase) database).getJooq();
    //    final SelectJoinStep from = jooq
    //            .select(schema.getTable(SAMPLES).getMetadata().getColumn(TYPE).getJooqField(),
    //                    sum(schema.getTable(SAMPLES).getMetadata().getColumn(N).getJooqField()))
    //            .from(schema.getTable(SAMPLES).getMetadata().getJooqTable());
    //    final SelectHavingStep groupBy =
    // from.groupBy(schema.getTable(SAMPLES).getMetadata().getColumn(TYPE).getJooqField());
    //    groupBy.fetch().forEach(System.out::println);

    Table table = schema.getTable(SAMPLES).getMetadata().getTable();
    Query query1 = table.groupBy();
    query1.select(s("sum").subselect(s(N)), s(TYPE).subselect(s(NAME)));
    final String json = query1.retrieveJSON();
    System.out.println(json);
  }

  @Test
  public void testOtherQuery() {
    Schema s = database.getSchema(TEST_SUM_QUERY);
    Table table = s.getTable(SAMPLES).getMetadata().getTable();
    Query query1 = table.select(s(TYPE).subselect(s(NAME)));
    //    query1.select(s(TYPE).subselect(s(NAME)));
    final String json = query1.retrieveJSON();
    System.out.println(json);
  }
}
