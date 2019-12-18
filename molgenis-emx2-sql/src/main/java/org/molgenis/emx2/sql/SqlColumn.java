package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.TableMetadata;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.REF;

public class SqlColumn extends Column {
  public SqlColumn(SqlTableMetadata table, String columnName, ColumnType columnColumnType) {
    super(table, columnName, columnColumnType);
  }

  SqlColumn createColumn() {
    DataType thisType = SqlTypeUtils.jooqTypeOf(this);
    Field thisColumn = field(name(getName()), thisType);
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
      getJooq().alterTable(asJooqTable()).alterColumn(getName()).dropNotNull().execute();
    else getJooq().alterTable(asJooqTable()).alterColumn(getName()).setNotNull().execute();
    super.setNullable(getNullable());
    return this;
  }

  @Override
  public Column setIndexed(boolean index) {
    getJooq()
        .transaction(
            dsl -> {
              String indexName = "INDEX_" + getTable().getTableName() + '_' + getName();
              if (index) {
                getJooq()
                    .createIndexIfNotExists(name(indexName))
                    .on(asJooqTable(), field(name(getName())))
                    .execute();
              } else {
                getJooq().dropIndexIfExists(name(getTable().getSchema().getName(), indexName));
              }
              super.setIndexed(index);
              MetadataUtils.saveColumnMetadata(this);
            });
    return this;
  }

  //  @Override
  //  public Column setReverseReference(String reverseColumnName, String reverseRefColumn) {
  //    TableMetadata otherTable = getTable().getSchema().getTableMetadata(getRefViaName());
  //    if (otherTable == null) {
  //      throw new MolgenisException(
  //          "Set reverse reference failed",
  //          "Reference back from column '" + getName() + "' failed because RefTableName was not
  // set");
  //    }
  //
  //    // in case of REF we must create the refBack columnn
  //    if (REF.equals(getColumnType())) {
  //      otherTable.addRefBack(
  //          reverseColumnName, getTable().getTableName(), reverseRefColumn, getName());
  //    }
  //
  //    // update state
  //    super.setReverseReference(reverseColumnName, reverseRefColumn);
  //    return this;
  //  }

  // helper methods
  private org.jooq.Table asJooqTable() {
    return table(name(getTable().getSchema().getName(), getTable().getTableName()));
  }

  protected DSLContext getJooq() {
    return ((SqlTableMetadata) getTable()).getJooq();
  }

  protected SqlColumn loadNullable(Boolean nullable) {
    super.setNullable(nullable);
    return this;
  }

  //  protected SqlColumn loadReverseReference(String reverseRefTable, String reverseToColumn) {
  //    super.setReverseReference(reverseRefTable, reverseToColumn);
  //    return this;
  //  }

  protected SqlColumn loadVia(String via) {
    super.setJoinVia(via);
    return this;
  }

  public TableMetadata getRefTable() {
    return getTable().getSchema().getTableMetadata(getRefTableName());
  }

  public TableMetadata getJoinTable() {
    return getTable().getSchema().getTableMetadata(getJoinViaName());
  }
}
