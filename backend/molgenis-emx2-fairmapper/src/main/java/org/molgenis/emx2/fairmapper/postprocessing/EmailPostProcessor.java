package org.molgenis.emx2.fairmapper.postprocessing;

import java.util.Arrays;
import java.util.List;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;

/**
 * Strips the {@code mailto:} scheme from email values, e.g. {@code mailto:info@example.org} becomes
 * {@code info@example.org}. RDF sources express emails as {@code mailto:} IRIs, whereas EMX2 stores
 * the bare address. Only EMAIL and EMAIL_ARRAY columns are touched.
 */
public class EmailPostProcessor implements PostProcessor {

  private static final String MAILTO_PREFIX = "mailto:";

  private final SchemaMetadata schema;

  public EmailPostProcessor(SchemaMetadata schema) {
    this.schema = schema;
  }

  @Override
  public void process(InMemoryTableStore tableStore) {
    for (String tableName : tableStore.getTableNames()) {
      List<Column> emailColumns = emailColumns(tableName);
      if (emailColumns.isEmpty()) {
        continue;
      }

      for (Row row : tableStore.readTable(tableName)) {
        emailColumns.forEach(column -> stripPrefix(row, column));
      }
    }
  }

  private List<Column> emailColumns(String tableName) {
    return schema.getTableMetadata(tableName).getDownloadColumnNames().stream()
        .filter(EmailPostProcessor::isEmailColumn)
        .toList();
  }

  private static boolean isEmailColumn(Column column) {
    ColumnType columnType = column.getColumnType();
    return ColumnType.EMAIL == columnType || ColumnType.EMAIL_ARRAY == columnType;
  }

  private static void stripPrefix(Row row, Column column) {
    String columnName = column.getName();
    if (!row.notNull(columnName)) {
      return;
    }

    if (column.isArray()) {
      String[] stripped =
          Arrays.stream(row.getStringArray(columnName))
              .map(EmailPostProcessor::stripPrefix)
              .toArray(String[]::new);
      row.set(columnName, stripped);
    } else {
      row.set(columnName, stripPrefix(row.getString(columnName)));
    }
  }

  private static String stripPrefix(String value) {
    return value.startsWith(MAILTO_PREFIX) ? value.substring(MAILTO_PREFIX.length()) : value;
  }
}
