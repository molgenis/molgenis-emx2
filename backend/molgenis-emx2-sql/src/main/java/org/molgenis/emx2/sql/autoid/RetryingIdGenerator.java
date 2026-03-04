package org.molgenis.emx2.sql.autoid;

import org.jooq.Field;
import org.jooq.impl.DSL;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.utils.generator.AutoIdFormat;
import org.molgenis.emx2.utils.generator.DSLIdGenerator;

public class RetryingIdGenerator implements DSLIdGenerator {

  private final Column column;
  private final AutoIdFormat autoIdFormat;

  public RetryingIdGenerator(Column column) {
    this.column = column;
    if (column.getComputed() == null) {
      throw new MolgenisException("Column with no computed value provided");
    }

    this.autoIdFormat =
        AutoIdFormat.fromComputedString(column.getComputed())
            .orElseThrow(
                () ->
                    new MolgenisException(
                        "Invalid computed value provided to column: " + column.getComputed()));
  }

  public Field<String> generateId() {
    return generateAutoIdSubquery(
        column.getSchemaName(), column.getTableName(), column.getName(), autoIdFormat);
  }

  public Field<String> generateAutoIdSubquery(
      String schemaName, String tableName, String columnName, AutoIdFormat format) {
    Field<Object> generated =
        DSL.select(
                DSL.field(
                    "\"MOLGENIS\".mg_generate_autoid({0}, {1}, {2}, {3}, {4})",
                    String.class,
                    DSL.val(schemaName),
                    DSL.val(tableName),
                    DSL.val(columnName),
                    DSL.val(format.format().getCharacters()),
                    DSL.val(format.length())))
            .asField();

    return DSL.concat(DSL.val(format.prefix()), generated, DSL.val(format.suffix()));
  }
}
