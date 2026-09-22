package org.molgenis.emx2.sql.autoid;

import org.jooq.Field;
import org.jooq.impl.DSL;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.utils.generator.AutoIdFormat;

public class RetryingIdGenerator {

  private final AutoIdFormat autoIdFormat;
  private final String schemaName;
  private final String tableName;
  private final String name;

  public RetryingIdGenerator(Column column) {
    if (column.getComputed() == null) {
      throw new MolgenisException("Column with no computed value provided");
    }
    this.autoIdFormat =
        AutoIdFormat.fromComputedString(column.getComputed())
            .orElseThrow(
                () ->
                    new MolgenisException(
                        "Invalid computed value provided to column: " + column.getComputed()));

    this.schemaName = column.getSchemaName();
    this.tableName = column.getTableName();
    this.name = column.getName();
  }

  public Field<String> generateId() {
    return generateAutoIdSubquery(schemaName, tableName, name, autoIdFormat);
  }

  private Field<String> generateAutoIdSubquery(
      String schemaName, String tableName, String columnName, AutoIdFormat format) {
    return DSL.select(
            DSL.field(
                "\"MOLGENIS\".mg_generate_autoid({0}, {1}, {2}, {3}, {4}, {5}, {6})",
                String.class,
                DSL.val(schemaName),
                DSL.val(tableName),
                DSL.val(columnName),
                DSL.val(format.format().getCharacters()),
                DSL.val(format.length()),
                DSL.val(format.prefix()),
                DSL.val(format.suffix())))
        .asField();
  }
}
