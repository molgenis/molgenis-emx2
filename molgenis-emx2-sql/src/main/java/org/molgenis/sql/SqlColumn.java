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
  public SqlColumn nullable(boolean nillable) throws MolgenisException {
    if (nillable) jooq.alterTable(asJooqTable()).alterColumn(getName()).dropNotNull().execute();
    else jooq.alterTable(asJooqTable()).alterColumn(getName()).setNotNull().execute();
    super.nullable(isNullable());
    return this;
  }

  protected org.jooq.Table asJooqTable() {
    return table(name(getTable().getSchema().getName(), getTable().getName()));
  }

  public SqlColumn setIndexed(boolean indexed) {
    String indexName = "INDEX_" + getTable().getName() + '_' + getName();
    if (indexed) {
      jooq.createIndexIfNotExists(name(indexName))
          .on(asJooqTable(), field(name(getName())))
          .execute();
    } else {
      jooq.dropIndexIfExists(name(getTable().getSchemaName(), indexName));
    }
    return this;
  }

  protected DSLContext getJooq() {
    return jooq;
  }

  protected Column loadNullable(Boolean nullable) throws MolgenisException {
    super.nullable(nullable);
    return this;
  }
}
