package org.molgenis.emx2.sql;

import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.utils.StopWatch;

import java.util.ArrayList;
import java.util.List;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.MREF;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;
import static org.molgenis.emx2.TableMetadata.table;

public class Benchmark {

  public static void main(String[] args) {

    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(Benchmark.class.getSimpleName());

    int aSize = 50;
    int bSize = 100000;

    Table a = schema.create(table("TableA").add(column("ID").pkey()));
    List<String> values = new ArrayList<>();

    Table b =
        schema.create(
            table("TableB")
                .add(column("ID").pkey())
                .add(column("ref").type(REF_ARRAY).refTable("TableA")));

    Table c =
        schema.create(
            table("TableC")
                .add(column("ID").pkey())
                .add(column("ref").type(MREF).refTable("TableA")));

    StopWatch.start("benchmark started");

    List<Row> aRows = new ArrayList<>();
    for (int i = 0; i < aSize; i++) {
      aRows.add(new Row().set("ID", "row" + i));
      values.add("row" + i);
    }

    StopWatch.start("benchmark1");
    a.insert(aRows);
    StopWatch.print("inserted primary", aSize);
    aRows.clear();

    List<Row> bRows = new ArrayList<>();
    for (int i = 0; i < bSize; i++) {
      bRows.add(new Row().set("ID", "row" + i).set("ref", values));
    }
    StopWatch.start("benchmark2 started");
    b.insert(bRows);
    StopWatch.print("inserted ref_array", bSize);
    bRows.clear();

    List<Row> cRows = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      cRows.add(new Row().set("ID", "row" + i).set("ref", values));
    }
    StopWatch.start("benchmark3 started");
    c.insert(cRows);
    StopWatch.print("inserted mref", 1000);
    cRows.clear();

    // StopWatch.print("inserted mref", bSize);

    // ref_array

  }
}
