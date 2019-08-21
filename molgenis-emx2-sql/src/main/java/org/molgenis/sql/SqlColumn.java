package org.molgenis.sql;

import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.molgenis.Column;
import org.molgenis.MolgenisException;
import org.molgenis.Type;
import org.molgenis.beans.ColumnMetadata;

import static org.jooq.impl.DSL.*;

public class SqlColumn extends ColumnMetadata {
  private DSLContext jooq;

  public SqlColumn(SqlTable table, String columnName, Type columnType) {
    super(table, columnName, columnType);
    this.jooq = table.getJooq();
  }

  /** constructor for REF */
  public SqlColumn createColumn() throws MolgenisException {
    DataType thisType = SqlTypeUtils.jooqTypeOf(this);
    Field thisColumn = field(name(getName()), thisType);
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
  public SqlColumn setNullable(boolean nillable) throws MolgenisException {
    if (nillable) jooq.alterTable(asJooqTable()).alterColumn(getName()).dropNotNull().execute();
    else jooq.alterTable(asJooqTable()).alterColumn(getName()).setNotNull().execute();
    super.setNullable(getNullable());
    return this;
  }

  public SqlColumn setIndexed(boolean index) {
    String indexName = "INDEX_" + getTable().getName() + '_' + getName();
    if (index) {
      jooq.createIndexIfNotExists(name(indexName))
          .on(asJooqTable(), field(name(getName())))
          .execute();
    } else {
      jooq.dropIndexIfExists(name(getTable().getSchemaName(), indexName));
    }
    return this;
  }

  // helper methods

  protected org.jooq.Table asJooqTable() {
    return table(name(getTable().getSchema().getName(), getTable().getName()));
  }

  protected DSLContext getJooq() {
    return jooq;
  }

  protected Column loadNullable(Boolean nullable) throws MolgenisException {
    super.setNullable(nullable);
    return this;
  }
}
