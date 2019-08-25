package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.molgenis.MolgenisException;
import org.molgenis.metadata.Type;
import org.molgenis.metadata.ColumnMetadata;

import static org.jooq.impl.DSL.*;

public class SqlColumnMetadata extends ColumnMetadata {
  private DSLContext jooq;

  public SqlColumnMetadata(SqlTableMetadata table, String columnName, Type columnType) {
    super(table, columnName, columnType);
    this.jooq = table.getJooq();
  }

  /** constructor for REF */
  public SqlColumnMetadata createColumn() throws MolgenisException {
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
  public SqlColumnMetadata setNullable(boolean nillable) {
    if (nillable)
      jooq.alterTable(asJooqTable()).alterColumn(getColumnName()).dropNotNull().execute();
    else jooq.alterTable(asJooqTable()).alterColumn(getColumnName()).setNotNull().execute();
    super.setNullable(getNullable());
    return this;
  }

  public SqlColumnMetadata setIndexed(boolean index) {
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

  protected ColumnMetadata loadNullable(Boolean nullable) throws MolgenisException {
    super.setNullable(nullable);
    return this;
  }
}
