package org.molgenis.emx2.examples;

import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.jooq.DSLContext;
import org.molgenis.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CompareTools {

  private static Javers javers;

  public static Javers getJavers() {
    if (javers == null) {
      javers =
          JaversBuilder.javers()
              .registerIgnoredClass(DSLContext.class)
              .registerIgnoredClass(Schema.class)
              .build();
    }
    return javers;
  }

  private CompareTools() {
    // hide constructor
  }

  public static void assertEquals(List<Row> list1, List<Row> list2) throws MolgenisException {

    if (list1.size() != list1.size())
      throw new MolgenisException("List<Row> have different length ");

    for (int i = 0; i < list1.size(); i++) {

      Row r1 = list1.get(i);
      Collection<String> colNames1 = r1.getColumnNames();

      Row r2 = list2.get(i);
      Collection<String> colNames2 = r2.getColumnNames();

      if (!colNames1.equals(colNames2)) {
        throw new MolgenisException(
            "List<Row> has different column names on row " + i + ": " + r1 + "+\nversus\n" + r2);
      }

      Map<String, Object> values1 = r1.getValueMap();
      for (String colName : colNames1) {
        Type type = TypeUtils.typeOf(values1.get(colName).getClass());

        if (!r1.get(type, colName).equals(r2.get(type, colName))
            && !Arrays.equals((Object[]) r1.get(type, colName), (Object[]) r2.get(type, colName))) {
          throw new MolgenisException(
              "List<Row> has different value for row "
                  + i
                  + ", column "
                  + colName
                  + ": "
                  + TypeUtils.toString(r1.get(type, colName))
                  + "\nversus\n"
                  + TypeUtils.toString(r2.get(type, colName)));
        }
      }
    }
  }

  public static void assertEquals(Schema schema1, Schema schema2) throws MolgenisException {
    Collection<String> tableNames1 = schema1.getTableNames();
    Collection<String> tableNames2 = schema2.getTableNames();

    for (Object tableName : tableNames1)
      if (!tableNames2.contains(tableName))
        throw new MolgenisException(
            "Schema's have different tables: schema2 doesn't contain '" + tableName + "'");
    for (Object tableName : tableNames2)
      if (!tableNames1.contains(tableName))
        throw new MolgenisException(
            "Schema's have different tables: schema1 doesn't contain '" + tableName + "'");

    for (String tableName : tableNames1) {
      Diff diff = getJavers().compare(schema1.getTable(tableName), schema2.getTable(tableName));

      if (diff.hasChanges()) {
        throw new MolgenisException("Roundtrip test failed: changes, " + diff.toString());
      }
    }
  }

  public static void reloadAndCompare(Database database, Schema schema) throws MolgenisException {
    // remember
    String schemaName = schema.getName();
    Collection<String> tableNames = schema.getTableNames();

    // empty the cache
    database.clearCache();

    // check reload from drive
    Schema schemaLoadedFromDisk = database.getSchema(schemaName);

    for (String tableName : tableNames) {
      Diff diff =
          getJavers().compare(schema.getTable(tableName), schemaLoadedFromDisk.getTable(tableName));

      if (diff.hasChanges()) {
        throw new MolgenisException("Roundtrip test failed: changes, " + diff.toString());
      }
    }
  }
}
