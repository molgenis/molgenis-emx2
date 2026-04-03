package org.molgenis.emx2.sql;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF_ARRAY;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.utils.StopWatch;

public class Benchmark {

  public void testCopyInAndOut() {

    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(Benchmark.class.getSimpleName());

    int aSize = 50;
    int bSize = 100000;

    Table a = schema.create(table("TableA").add(column("ID").setPkey()));
    List<String> values = new ArrayList<>();

    Table b =
        schema.create(
            table("TableB")
                .add(column("ID").setPkey())
                .add(column("ref").setType(REF_ARRAY).setRefTable("TableA")));

    //    Table c =
    //        schema.create(
    //            table("TableC")
    //                .add(column("ID").setPkey())
    //                .add(column("ref").setType(MREF).setRefTable("TableA")));

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

    //    List<Row> cRows = new ArrayList<>();
    //    for (int i = 0; i < bSize; i++) {
    //      cRows.add(new Row().set("ID", "row" + i).set("ref", values));
    //    }
    //    StopWatch.start("benchmark3 started");
    //    c.insert(cRows);
    //    StopWatch.print("inserted mref", bSize);
    //    cRows.clear();

    // StopWatch.print("inserted mref", bSize);

    // ref_array

  }
}
