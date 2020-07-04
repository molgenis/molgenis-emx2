package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.sql.Constants.MG_EDIT_ROLE;
import static org.molgenis.emx2.sql.SqlColumnExecutor.executeCreateColumn;
import static org.molgenis.emx2.sql.SqlColumnExecutor.reapplyRefbackContraints;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.*;

class SqlTableMetadata extends TableMetadata {
  private static final String SET_INHERITANCE_FAILED = "Set inheritance failed";
  private Database db;
  private static Logger logger = LoggerFactory.getLogger(SqlTableMetadata.class);

  SqlTableMetadata(Database db, SqlSchemaMetadata schema, TableMetadata metadata) {
    super(schema, metadata);
    this.db = db;
  }

  void load() {
    long start = System.currentTimeMillis();
    clearCache();
    TableMetadata temp = new TableMetadata(getSchema(), getTableName());
    MetadataUtils.loadTableMetadata(getJooq(), temp);
    this.copy(temp);
    log(start, "RELOAD");
  }

  @Override
  public TableMetadata add(Column column) {
    long start = System.currentTimeMillis();
    db.tx(
        dsl -> {
          if (getLocalColumn(column.getName()) != null)
            throw new MolgenisException(
                "Add column failed",
                "Duplicate name; column with name "
                    + getTableName()
                    + "."
                    + column.getName()
                    + " already exists");

          Column result = new Column(this, column);
          executeCreateColumn(getJooq(), result);
          super.add(result);
          if (column.getKey() > 0) {
            SqlTableMetadataExecutor.createOrReplaceUnique(
                getJooq(), this, column.getKey(), asJooqNames(getKeyNames(column.getKey())));
          }
          SqlColumnExecutor.executeSetForeignkeys(getJooq(), new Column(this, column));
          log(start, "added column '" + column.getName() + "' to ");
        });
    return this;
  }

  @Override
  public TableMetadata alterColumn(String name, Column column) {
    Column oldColumn = getColumn(name);
    if (oldColumn == null) {
      throw new MolgenisException(
          "Alter column failed",
          "Column  '" + getTableName() + "." + column.getName() + "' does not exist");
    }
    db.tx(
        dsl -> {
          Column newColumn = new Column(this, column);
          SqlColumnExecutor.executeAlterColumn(getJooq(), oldColumn, newColumn);
          super.alterColumn(name, newColumn);
          reapplyRefbackContraints(oldColumn, newColumn);
        });

    return this;
  }

  @Override
  public void dropColumn(String name) {
    long start = System.currentTimeMillis();
    if (getColumn(name) == null) return; // return silently, idempotent
    db.tx(
        dsl -> {
          SqlColumnExecutor.executeRemoveColumn(getJooq(), getColumn(name));
          super.columns.remove(name);
        });
    log(start, "removed column '" + name + "' from ");
  }

  @Override
  public TableMetadata setInherit(String otherTable) {
    // todo split first inherit from change
    long start = System.currentTimeMillis();
    if (getInherit() != null)
      throw new MolgenisException(
          SET_INHERITANCE_FAILED,
          "Table '"
              + getTableName()
              + "'can only extend one table. Therefore it cannot extend '"
              + otherTable
              + "' because it already extends other table '"
              + getInherit()
              + "'");
    TableMetadata other = getSchema().getTableMetadata(otherTable);
    if (other == null)
      throw new MolgenisException(
          SET_INHERITANCE_FAILED, "Other table '" + otherTable + "' does not exist in this schema");

    if (other.getPrimaryKeys() == null)
      throw new MolgenisException(
          SET_INHERITANCE_FAILED,
          "To extend table '" + otherTable + "' it must have primary key set");
    db.tx(
        tdb -> {
          // extends means we copy primary key column from parent to child, make it foreign key to
          // parent, and make it primary key of this table also.
          executeSetInherit(getJooq(), this, other);
          super.inherit = otherTable;
        });
    log(start, "set inherit on ");
    return this;
  }

  @Override
  public TableMetadata removeInherit() {
    throw new RuntimeException("removeInherit not yet implemented");
  }

  @Override
  public void enableRowLevelSecurity() {
    // todo, study if we need different row level security in inherited tables
    this.add(column(MG_EDIT_ROLE).index(true));

    getJooq().execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", getJooqTable(this));
    getJooq()
        .execute(
            "CREATE POLICY {0} ON {1} USING (pg_has_role(session_user, {2}, 'member')) WITH CHECK (pg_has_role(session_user, {2}, 'member'))",
            name("RLS/" + getSchema().getName() + "/" + getTableName()),
            getJooqTable(this),
            name(MG_EDIT_ROLE));
    // set RLS on the table
    // add policy for 'viewer' and 'editor'.
  }

  @Override
  public boolean exists() {
    // first look at already loaded metadata, in case of no columns, check the underlying table
    if (!getColumns().isEmpty()) {
      return true;
    }
    // jooq doesn't have operator for this, so by hand. Might be slow
    return 0
        < getJooq()
            .select(count())
            .from(name("information_schema", "tables"))
            .where(
                field("table_schema")
                    .eq(getSchema().getName())
                    .and(field("table_name").eq(getTableName())))
            .fetchOne(0, Integer.class);
  }

  public DSLContext getJooq() {
    return ((SqlDatabase) db).getJooq();
  }

  private void log(long start, String message) {
    String user = db.getActiveUser();
    if (user == null) user = "molgenis";
    if (logger.isInfoEnabled()) {
      logger.info(
          "{} {} {} in {}ms",
          user,
          message,
          getJooqTable(this),
          (System.currentTimeMillis() - start));
    }
  }
}
