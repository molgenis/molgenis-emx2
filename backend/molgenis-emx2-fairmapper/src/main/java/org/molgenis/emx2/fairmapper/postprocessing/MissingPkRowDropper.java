package org.molgenis.emx2.fairmapper.postprocessing;

import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drops rows that are missing a value for one or more of their primary key columns, for each of the
 * configured tables.
 */
public class MissingPkRowDropper implements PostProcessor {

  private static final Logger logger = LoggerFactory.getLogger(MissingPkRowDropper.class);

  private final SchemaMetadata schema;
  private final List<String> tableNames;

  public MissingPkRowDropper(SchemaMetadata schema, List<String> tableNames) {
    this.schema = schema;
    this.tableNames = tableNames;
  }

  @Override
  public void process(TableStore tableStore) {
    for (String tableName : tableNames) {
      List<Column> pkColumns = schema.getTableMetadata(tableName).getPrimaryKeyColumns();

      List<Row> rows = new ArrayList<>();
      for (Row row : tableStore.readTable(tableName)) {
        if (hasAllPkValues(row, pkColumns)) {
          rows.add(row);
        } else {
          logger.warn("Row is missing a primary key value, dropping: {}", row);
        }
      }

      if (rows.isEmpty()) {
        tableStore.writeTable(tableName, List.of(), List.of());
      } else {
        tableStore.writeTable(tableName, rows.getFirst().getColumnNames().stream().toList(), rows);
      }
    }
  }

  private static boolean hasAllPkValues(Row row, List<Column> pkColumns) {
    return pkColumns.stream().allMatch(pk -> row.notNull(pk.getName()));
  }
}
