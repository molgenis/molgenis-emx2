package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Column;

import static org.jooq.impl.DSL.*;

public class SqlColumn extends Column {
  private DSLContext jooq;

  public SqlColumn(SqlTableMetadata table, String columnName, ColumnType columnColumnType) {
    super(table, columnName, columnColumnType);
    this.jooq = table.getJooq();
  }

  /** constructor for REF */
  public SqlColumn createColumn() {
    DataType thisType = SqlTypeUtils.jooqTypeOf(this);
    Field thisColumn = field(name(getColumnName()), thisType);
    jooq.alterTable(asJooqTable()).addColumn(thisColumn).execute();

    jooq.alterTable(asJooqTable())
        .alterColumn(thisColumn)
        .setNotNull()
        .execute(); // seperate to not interfere with type

    // save metadata
    MetadataUtils.saveColumnMetadata(this);
    return this;
  }

  @Override
  public SqlColumn setNullable(boolean nillable) {
    if (nillable)
      jooq.alterTable(asJooqTable()).alterColumn(getColumnName()).dropNotNull().execute();
    else jooq.alterTable(asJooqTable()).alterColumn(getColumnName()).setNotNull().execute();
    super.setNullable(getNullable());
    return this;
  }

  public SqlColumn setIndexed(boolean index) {
    String indexName = "INDEX_" + getTable().getTableName() + '_' + getColumnName();
    if (index) {
      jooq.createIndexIfNotExists(name(indexName))
          .on(asJooqTable(), field(name(getColumnName())))
          .execute();
    } else {
      jooq.dropIndexIfExists(name(getTable().getSchema().getName(), indexName));
    }
    return this;
  }

  // helper methods

  protected org.jooq.Table asJooqTable() {
    return table(name(getTable().getSchema().getName(), getTable().getTableName()));
  }

  protected DSLContext getJooq() {
    return jooq;
  }

  protected Column loadNullable(Boolean nullable) {
    super.setNullable(nullable);
    return this;
  }
}
