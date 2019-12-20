package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.DefaultRoles;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.ColumnType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.sql.Constants.*;

class SqlTableMetadata extends TableMetadata {
  public static final String DROP_TABLE_FAILED_MESSAGE = "Drop table failed";
  public static final String INHERITANCE_FAILED_MESSAGE = "Inheritance failed";
  private static final String SET_INHERITANCE_FAILED = "Set inheritance failed";
  private SqlDatabase db;
  private static Logger logger = LoggerFactory.getLogger(SqlTableMetadata.class);

  SqlTableMetadata(SqlDatabase db, SqlSchemaMetadata schema, String name) {
    super(schema, name);
    this.db = db;
  }

  void load() {
    clearCache();
    for (Column c : MetadataUtils.loadColumnMetadata(this)) {
      super.addColumn(c);
    }
    MetadataUtils.loadTableMetadata(this);
    MetadataUtils.loadUniqueMetadata(this);
  }

  @Override
  public Column getColumn(String name) {
    // see if it is already loaded
    Column c = super.getColumn(name);
    if (c != null) return c;
    // try to load
    this.load();
    return super.getColumn(name);
  }

  public void createTable() {
    long start = System.currentTimeMillis();

    db.tx(
        dsl -> {
          Name tableName = name(getSchema().getName(), getTableName());
          DSLContext jooq = db.getJooq();
          jooq.createTable(tableName).columns().execute();
          MetadataUtils.saveTableMetadata(this);

          // grant rights to schema manager, editor and viewer roles
          jooq.execute(
              "GRANT SELECT ON {0} TO {1}",
              tableName, name(getRolePrefix() + DefaultRoles.VIEWER.toString()));
          jooq.execute(
              "GRANT INSERT, UPDATE, DELETE, REFERENCES, TRUNCATE ON {0} TO {1}",
              tableName, name(getRolePrefix() + DefaultRoles.EDITOR.toString()));
          jooq.execute(
              "ALTER TABLE {0} OWNER TO {1}",
              tableName, name(getRolePrefix() + DefaultRoles.MANAGER.toString()));

          enableSearch();
        });
    log(start, "created");
  }

  private String getRolePrefix() {
    return ((SqlSchemaMetadata) getSchema()).getRolePrefix();
  }

  public void dropTable() {
    long start = System.currentTimeMillis();
    try {
      db.tx(
          dsl -> {
            db.getJooq().dropTable(name(getSchema().getName(), getTableName())).execute();
            MetadataUtils.deleteTable(this);
          });
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException(DROP_TABLE_FAILED_MESSAGE, dae);
    }
    log(start, "dropped");
  }

  @Override
  public TableMetadata setInherit(String otherTable) {
    long start = System.currentTimeMillis();
    db.tx(
        tdb -> {
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
                SET_INHERITANCE_FAILED,
                "Other table '" + otherTable + "' does not exist in this schema");

          if (other.getPrimaryKey() == null)
            throw new MolgenisException(
                SET_INHERITANCE_FAILED,
                "To extend table '" + otherTable + "' it must have primary key set");

          // extends means we copy primary key column from parent to child, make it foreign key to
          // parent, and make it primary key of this table also.
          this.addRef(other.getPrimaryKey(), other.getTableName(), other.getPrimaryKey());
          this.setPrimaryKey(other.getPrimaryKey());

          // update super and then save the metadata
          super.setInherit(otherTable);
          MetadataUtils.saveTableMetadata(this);
        });
    log(start, "set inherit on ");
    return this;
  }

  @Override
  public TableMetadata setPrimaryKey(String columnName) {
    long start = System.currentTimeMillis();
    if (columnName == null)
      throw new MolgenisException(
          "Set primary key failed", "Primary key requires 1 however null was provided");
    if (columnName.equals(getPrimaryKey())) return this;
    if (getInherit() != null)
      throw new MolgenisException(
          "Set primary key failed",
          "Primary key cannot be set on table '"
              + getTableName()
              + "' because inherits its primary key from other table '"
              + getInherit()
              + "'");

    db.tx(
        dsl -> {
          Name[] keyNames = Stream.of(columnName).map(DSL::name).toArray(Name[]::new);

          // drop previous primary key if exists
          db.getJooq()
              .execute(
                  "ALTER TABLE {0} DROP CONSTRAINT IF EXISTS {1}",
                  getJooqTable(), name(getTableName() + "_pkey"));

          // createTableIfNotExists the new one
          db.getJooq().alterTable(getJooqTable()).add(constraint().primaryKey(keyNames)).execute();

          // update the decorated super
          super.setPrimaryKey(columnName);
          MetadataUtils.saveTableMetadata(this);
        });
    log(start, "set primary key " + List.of(columnName) + " on ");
    return this;
  }

  @Override
  public Column addColumn(String name, ColumnType columnType) {
    db.tx(
        dsl -> {
          SqlColumn c = new SqlColumn(this, name, columnType);
          super.addColumn(c);
          c.createColumn();
          this.updateSearchIndexTriggerFunction();
        });
    return getColumn(name);
  }

  @Override
  public Column addColumn(Column metadata) {
    long start = System.currentTimeMillis();

    if (getColumn(metadata.getName()) != null) {
      throw new MolgenisException(
          "Add column failed",
          "Column with columnName='"
              + metadata.getName()
              + "' already exist in table '"
              + getTableName()
              + "'");
    }
    if (getInherit() != null && getInheritedTable().getColumn(metadata.getName()) != null) {
      throw new MolgenisException(
          "Add column failed",
          "Column with columnName='"
              + metadata.getName()
              + "' already exist in table '"
              + getTableName()
              + "' because it got inherited from table '"
              + getInherit()
              + "'");
    }
    // if ref column is empty, guess it
    db.tx(
        dsl -> {
          Column result = null;

          switch (metadata.getColumnType()) {
            case REF:
              result =
                  addRef(
                      metadata.getName(), metadata.getRefTableName(), metadata.getRefColumnName());
              break;
            case REF_ARRAY:
              result =
                  addRefArray(
                      metadata.getName(), metadata.getRefTableName(), metadata.getRefColumnName());
              break;
            case REFBACK:
              result =
                  addRefBack(
                      metadata.getName(),
                      metadata.getRefTableName(),
                      metadata.getRefColumnName(),
                      metadata.getMappedBy());
              break;
            case MREF:
              result =
                  addMref(
                      metadata.getName(), metadata.getRefTableName(), metadata.getRefColumnName());
              break;
            default:
              result = addColumn(metadata.getName(), metadata.getColumnType());
          }
          result.setDescription(metadata.getDescription());
          result.setNullable(metadata.isNullable());
          result.setDefaultValue(metadata.getDefaultValue());
        });
    log(start, "added column '" + metadata.getName() + "' to ");
    return getColumn(metadata.getName());
  }

  @Override
  public Column addRef(String name, String toTable, String toColumn) {
    long start = System.currentTimeMillis();
    db.tx(
        dsl -> {
          SqlRefColumn c =
              new SqlRefColumn(
                  this,
                  name,
                  toTable,
                  toColumn == null ? getRefTablePrimaryKey(toTable) : toColumn);
          c.createColumn();
          super.addColumn(c);
          this.updateSearchIndexTriggerFunction();
        });
    log(start, "added ref '" + name + "' to ");
    return getColumn(name);
  }

  @Override
  public Column addRefBack(String name, String toTable, String toColumn, String viaColumn) {
    long start = System.currentTimeMillis();
    db.tx(
        dsl -> {
          SqlRefBackColumn c =
              new SqlRefBackColumn(
                  this,
                  name,
                  toTable,
                  toColumn == null ? getRefTablePrimaryKey(toTable) : toColumn,
                  viaColumn);
          c.createColumn();
          super.addColumn(c);
          this.updateSearchIndexTriggerFunction();
        });

    log(start, "added ref '" + name + "' to ");
    return getColumn(name);
  }

  private String getRefTablePrimaryKey(String toTable) {
    if (getSchema().getTableMetadata(toTable) == null) {
      return null;
    } else {
      return getSchema().getTableMetadata(toTable).getPrimaryKey();
    }
  }

  @Override
  public Column addRefArray(String name, String toTable, String toColumn) {
    long start = System.currentTimeMillis();
    db.tx(
        dsl -> {
          SqlRefArrayColumn c =
              new SqlRefArrayColumn(
                  this,
                  name,
                  toTable,
                  toColumn == null ? getRefTablePrimaryKey(toTable) : toColumn);
          c.createColumn();
          super.addColumn(c);
          this.updateSearchIndexTriggerFunction();
        });
    log(start, "added refarray '" + name + "' to ");
    return getColumn(name);
  }

  @Override
  public Column addMref(String name, String refTable, String refColumn) {
    long start = System.currentTimeMillis();
    db.tx(
        dsl -> {
          SqlMrefColumn c =
              new SqlMrefColumn(
                  this,
                  name,
                  refTable,
                  refColumn == null ? getRefTablePrimaryKey(refTable) : refColumn);
          c.createColumn();
          super.addColumn(c);
          this.updateSearchIndexTriggerFunction();
        });
    log(start, "added mref '" + name + "' to ");
    return getColumn(name);
  }

  protected void addMrefReverse(SqlMrefColumn reverse) {
    super.addColumn(reverse);
  }

  @Override
  public void removeColumn(String name) {
    long start = System.currentTimeMillis();
    if (getColumn(name) == null) return; // return silently, idempotent
    db.getJooq().alterTable(getJooqTable()).dropColumn(field(name(name))).execute();
    super.removeColumn(name);
    log(start, "removed column '" + name + "' from ");
  }

  org.jooq.Table getJooqTable() {
    return table(name(getSchema().getName(), getTableName()));
  }

  public boolean exists() {
    // first look at already loaded metadata, in case of no columns, check the underlying table
    if (!getColumns().isEmpty()) {
      return true;
    }
    // jooq doesn't have operator for this, so by hand. Might be slow
    return 0
        < db.getJooq()
            .select(count())
            .from(name("information_schema", "tables"))
            .where(
                field("table_schema")
                    .eq(getSchema().getName())
                    .and(field("table_name").eq(getTableName())))
            .fetchOne(0, Integer.class);
  }

  protected void loadPrimaryKey(String pkey) {
    super.setPrimaryKey(pkey);
  }

  protected void loadUnique(String[] columns) {
    super.addUnique(columns);
  }

  @Override
  public TableMetadata addUnique(String... columnNames) {
    long start = System.currentTimeMillis();

    // check if the columns exists
    for (String columnName : columnNames) {
      Column c = getColumn(columnName);
      if (c == null)
        throw new MolgenisException(
            "Add unique failed",
            "Column '" + columnName + "' is not known in table " + getTableName());
    }

    // check if already exists
    if (isUnique(columnNames)) return this; // idempotent, we silently ignore

    db.tx(
        db2 -> {

          // create the unique
          String uniqueName = getTableName() + "_" + String.join("_", columnNames) + "_UNIQUE";
          db.getJooq()
              .alterTable(getJooqTable())
              .add(constraint(name(uniqueName)).unique(columnNames))
              .execute();
          MetadataUtils.saveUnique(this, columnNames);
          super.addUnique(columnNames);
        });
    log(start, "added unique '" + List.of(columnNames) + "' to ");

    return this;
  }

  @Override
  public void removeUnique(String... columnNames) {
    long start = System.currentTimeMillis();
    // try to find the right unique
    String[] correctOrderedNames = null;
    List list1 = Arrays.asList(columnNames);
    for (String[] unique : getUniques()) {
      List list2 = Arrays.asList(unique);
      if (list1.containsAll(list2) && list2.containsAll(list1)) {
        correctOrderedNames = unique;
      }
    }
    if (correctOrderedNames == null) {
      throw new MolgenisException(
          "Remove unique failed",
          "Unique constraint consisting of columns " + list1 + "could not be found. ");
    }

    String uniqueName = getTableName() + "_" + String.join("_", correctOrderedNames) + "_UNIQUE";
    db.getJooq().alterTable(getJooqTable()).dropConstraint(name(uniqueName)).execute();
    MetadataUtils.deleteUnique(this, correctOrderedNames);
    super.removeUnique(correctOrderedNames);
    log(start, "removed unique '" + columnNames + "' to ");
  }

  private void enableSearch() {

    // 1. add tsvector column with index
    db.getJooq()
        .execute(
            "ALTER TABLE {0} ADD COLUMN {1} tsvector",
            getJooqTable(), name(MG_SEARCH_INDEX_COLUMN_NAME));

    // for future performance enhancement consider studying 'gin (t gin_trgm_ops)

    // 2. createColumn index on that column to speed up search
    db.getJooq()
        .execute(
            "CREATE INDEX {0} ON {1} USING GIN( {2} )",
            name(getTableName() + "_search_idx"),
            getJooqTable(),
            name(MG_SEARCH_INDEX_COLUMN_NAME));

    // 3. createColumn the trigger function to automatically update the
    // MG_SEARCH_INDEX_COLUMN_NAME
    String triggerfunction = updateSearchIndexTriggerFunction();

    // 4. add trigger to update the tsvector on each insert or update
    db.getJooq()
        .execute(
            "CREATE TRIGGER {0} BEFORE INSERT OR UPDATE ON {1} FOR EACH ROW EXECUTE FUNCTION "
                + triggerfunction,
            name(MG_SEARCH_INDEX_COLUMN_NAME),
            getJooqTable());

    // also add text search  column
    // 1. create column
    db.getJooq()
        .execute(
            "ALTER TABLE {0} ADD COLUMN {1} TEXT",
            getJooqTable(), name(MG_TEXT_SEARCH_COLUMN_NAME));
    // 2. create trigram index
    db.getJooq()
        .execute(
            "CREATE INDEX {0} ON {1} USING GIN( {2} gin_trgm_ops)",
            name(getTableName() + "_text_search_idx"),
            getJooqTable(),
            name(MG_TEXT_SEARCH_COLUMN_NAME));
  }

  private String updateSearchIndexTriggerFunction() {
    // TODO should also join in REFBACK column to make them searchable as part of 'mew'
    //  TODO and then also should trigger indexing on update for tables with a REF to me so trigger
    // on ref
    // change
    // then?

    String triggerName = getTableName() + "search_vector_trigger";
    String triggerfunction = String.format("\"%s\".\"%s\"()", getSchema().getName(), triggerName);

    StringBuilder mgSearchVector = new StringBuilder("' '");
    for (Column c : getLocalColumns()) {
      if (!c.getName().startsWith("MG_"))
        mgSearchVector.append(
            String.format(" || coalesce(new.\"%s\"::text,'') || ' '", c.getName()));
    }

    String functionBody =
        String.format(
            "CREATE OR REPLACE FUNCTION %s RETURNS trigger AS $$\n"
                + "begin\n"
                + "\tnew.%s:=to_tsvector('english', %s ) ;\n"
                + "\tnew.%s:= %s  ;\n"
                + "\treturn new;\n"
                + "end\n"
                + "$$ LANGUAGE plpgsql;",
            triggerfunction,
            name(MG_SEARCH_INDEX_COLUMN_NAME),
            mgSearchVector,
            name(MG_TEXT_SEARCH_COLUMN_NAME),
            mgSearchVector);

    db.getJooq().execute(functionBody);
    db.getJooq()
        .execute(
            "ALTER FUNCTION " + triggerfunction + " OWNER TO {0}",
            name(getRolePrefix() + DefaultRoles.MANAGER.toString()));
    return triggerfunction;
  }

  @Override
  public void enableRowLevelSecurity() {
    // todo, study if we need different row level security in inherited tables
    Column c = this.addColumn(MG_EDIT_ROLE, STRING);
    c.setIndexed(true);

    db.getJooq().execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", getJooqTable());
    db.getJooq()
        .execute(
            "CREATE POLICY {0} ON {1} USING (pg_has_role(session_user, {2}, 'member')) WITH CHECK (pg_has_role(session_user, {2}, 'member'))",
            name("RLS/" + getSchema().getName() + "/" + getTableName()),
            getJooqTable(),
            name(MG_EDIT_ROLE));
    // set RLS on the table
    // add policy for 'viewer' and 'editor'.
  }

  public DSLContext getJooq() {
    return db.getJooq();
  }

  private void log(long start, String message) {
    String user = db.getActiveUser();
    if (user == null) user = "molgenis";
    if (logger.isInfoEnabled()) {
      logger.info(
          "{} {} {} in {}ms", user, message, getJooqTable(), (System.currentTimeMillis() - start));
    }
  }

  public void loadInherits(String tableName) {
    super.setInherit(tableName);
  }
}
