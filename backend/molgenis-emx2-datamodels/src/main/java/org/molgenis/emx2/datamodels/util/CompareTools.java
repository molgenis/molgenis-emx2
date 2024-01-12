package org.molgenis.emx2.datamodels.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.jooq.DSLContext;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;

public class CompareTools {

  private static Javers javers;

  public static Javers getJavers() {
    if (javers == null) {
      javers =
          JaversBuilder.javers()
              .registerIgnoredClass(DSLContext.class)
              .registerIgnoredClass(Database.class)
              .registerIgnoredClass(Schema.class)
              .registerIgnoredClass(SchemaMetadata.class)
              .build();
    }
    return javers;
  }

  private CompareTools() {
    // hide constructor
  }

  public static void assertEquals(List<Row> list1, List<Row> list2) {

    if (list1.size() != list2.size()) fail("List<Row> have different length ");

    for (int i = 0; i < list1.size(); i++) {

      Row r1 = list1.get(i);
      Collection<String> colNames1 = r1.getColumnNames();

      Row r2 = list2.get(i);
      Collection<String> colNames2 = r2.getColumnNames();

      if (!colNames1.equals(colNames2)) {
        fail("List<Row> has different column names on row " + i + ": " + r1 + "+\nversus\n" + r2);
      }

      Map<String, Object> values1 = r1.getValueMap();
      for (String colName : colNames1) {
        ColumnType columnType = TypeUtils.typeOf(values1.get(colName).getClass());

        if (!r1.get(colName, columnType).equals(r2.get(colName, columnType))
            && !Arrays.equals(
                (Object[]) r1.get(colName, columnType), (Object[]) r2.get(colName, columnType))) {
          fail(
              "List<Row> has different value for row "
                  + i
                  + ", column "
                  + colName
                  + ": "
                  + TypeUtils.toString(r1.get(colName, columnType))
                  + "\nversus\n"
                  + TypeUtils.toString(r2.get(colName, columnType)));
        }
      }
    }
  }

  public static void assertEquals(SchemaMetadata schema1, SchemaMetadata schema2) {
    Collection<String> tableNames1 = schema1.getTableNames();
    Collection<String> tableNames2 = schema2.getTableNames();

    for (Object tableName : tableNames1)
      if (!tableNames2.contains(tableName))
        fail("Schema's have different tables: schema2 doesn't contain '" + tableName + "'");
    for (Object tableName : tableNames2)
      if (!tableNames1.contains(tableName))
        fail("Schema's have different tables: schema1 doesn't contain '" + tableName + "'");

    for (String tableName : tableNames1) {
      Diff diff =
          getJavers()
              .compare(schema1.getTableMetadata(tableName), schema2.getTableMetadata(tableName));

      if (diff.hasChanges()) {
        fail("Roundtrip test failed: changes for table " + tableName + ": " + diff.toString());
      }
    }
  }

  public static void reloadAndCompare(Database database, Schema schema) {
    // remember
    String schemaName = schema.getMetadata().getName();
    Collection<String> tableNames = schema.getTableNames();

    // empty the cache
    database.clearCache();

    // check reload from drive
    Schema schemaLoadedFromDisk = database.getSchema(schemaName);

    for (String tableName : tableNames) {
      TableMetadata t1 = schema.getTable(tableName).getMetadata();
      TableMetadata t2 = schemaLoadedFromDisk.getTable(tableName).getMetadata();
      Diff diff = getJavers().compare(t1, t2);

      if (diff.hasChanges()) {
        fail("Roundtrip test failed: changes, " + diff.toString());
      }
    }
  }

  private static void fail(String message) {
    throw new MolgenisException(message);
  }
}
