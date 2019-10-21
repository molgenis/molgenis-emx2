package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.utils.MolgenisException;

import static org.jooq.impl.DSL.*;

public class SqlColumn extends Column {
  public SqlColumn(SqlTableMetadata table, String columnName, ColumnType columnColumnType) {
    super(table, columnName, columnColumnType);
  }

  /** constructor for REF */
  public SqlColumn createColumn() {
    DataType thisType = SqlTypeUtils.jooqTypeOf(this);
    Field thisColumn = field(name(getColumnName()), thisType);
    getJooq().alterTable(asJooqTable()).addColumn(thisColumn).execute();

    getJooq()
        .alterTable(asJooqTable())
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
      getJooq().alterTable(asJooqTable()).alterColumn(getColumnName()).dropNotNull().execute();
    else getJooq().alterTable(asJooqTable()).alterColumn(getColumnName()).setNotNull().execute();
    super.setNullable(getNullable());
    return this;
  }

  @Override
  public Column setIndexed(boolean index) {
    getJooq()
        .transaction(
            dsl -> {
              String indexName = "INDEX_" + getTable().getTableName() + '_' + getColumnName();
              if (index) {
                getJooq()
                    .createIndexIfNotExists(name(indexName))
                    .on(asJooqTable(), field(name(getColumnName())))
                    .execute();
              } else {
                getJooq().dropIndexIfExists(name(getTable().getSchema().getName(), indexName));
              }
              super.setIndexed(index);
              MetadataUtils.saveColumnMetadata(this);
            });
    return this;
  }

  // helper methods

  protected org.jooq.Table asJooqTable() {
    return table(name(getTable().getSchema().getName(), getTable().getTableName()));
  }

  protected DSLContext getJooq() {
    return ((SqlTableMetadata) getTable()).getJooq();
  }

  protected Column loadNullable(Boolean nullable) {
    super.setNullable(nullable);
    return this;
  }
}
