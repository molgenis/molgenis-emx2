package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.sql.ChangeLogExecutor.disableChangeLog;
import static org.molgenis.emx2.sql.MetadataUtils.saveColumnMetadata;
import static org.molgenis.emx2.sql.SqlColumnExecutor.*;
import static org.molgenis.emx2.utils.ColumnSort.sortColumnsByDependency;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.emx2.*;

class SqlTableMetadataExecutor {

  public static final String MG_TABLECLASS_UPDATE = "_MG_TABLECLASS_UPDATE";

  private SqlTableMetadataExecutor() {}

  static void executeCreateTable(DSLContext jooq, SqlTableMetadata table) {

    // create the table
    Table jooqTable = table.getJooqTable();
    jooq.execute("CREATE TABLE {0}()", jooqTable);
    MetadataUtils.saveTableMetadata(jooq, table);

    // grant rights to schema manager, editor and viewer role
    jooq.execute(
        "GRANT SELECT ON {0} TO {1}",
        jooqTable, name(getRolePrefix(table) + Privileges.AGGREGATOR.toString()));
    jooq.execute(
        "GRANT SELECT ON {0} TO {1}",
        jooqTable, name(getRolePrefix(table) + Privileges.VIEWER.toString()));
    jooq.execute(
        "GRANT INSERT, UPDATE, DELETE, REFERENCES, TRUNCATE ON {0} TO {1}",
        jooqTable, name(getRolePrefix(table) + Privileges.EDITOR.toString()));
    jooq.execute(
        "ALTER TABLE {0} OWNER TO {1}",
        jooqTable, name(getRolePrefix(table) + Privileges.MANAGER.toString()));

    // create columns from primary key of superclass
    if (table.getInheritName() != null) {
      if (table.getInheritedTable() == null) {
        throw new MolgenisException(
            "Cannot inherit "
                + table.getImportSchema()
                + "."
                + table.getInheritName()
                + ": not found");
      }
      executeSetInherit(jooq, table, table.getInheritedTable());
    }

    // then create columns
    for (Column column : table.getNonInheritedColumns()) {
      if (!column.isHeading()) {
        validateColumn(column);
        if (table.getInheritName() == null
            || table.getInheritedTable().getColumn(column.getName()) == null) {
          executeCreateColumn(jooq, column);
        }
      } else {
        saveColumnMetadata(jooq, column);
      }
    }

    // then create unique
    createOrReplaceKeys(jooq, table);

    // then create (composite) foreign keys
    for (Column column : table.getStoredColumns()) {
      if ((table.getInheritName() == null
              || table.getInheritedTable().getColumn(column.getName()) == null)
          && column.isReference()) {
        SqlColumnExecutor.executeCreateRefConstraints(jooq, column);
      }
    }

    // add search column
    executeEnableSearch(jooq, table);

    // add meta columns (only superclass table)
    if (table.getInheritName() == null) {
      executeAddMetaColumns(table);
    }

    if (ChangeLogUtils.isChangeSchema(table.getSchema().getDatabase(), table.getSchemaName())) {
      // setup trigger processing function
      jooq.execute(
          ChangeLogUtils.buildProcessAuditFunction(table.getSchemaName(), table.getTableName()));

      // set audit trigger, logs insert, update and delete actions on table
      jooq.execute(ChangeLogUtils.buildAuditTrigger(table.getSchemaName(), table.getTableName()));
    }
  }

  static void executeAlterName(DSLContext jooq, TableMetadata table, String newName) {
    // drop search trigger
    dropSearchTrigger(jooq, table);

    // rename search column
    jooq.alterTable(table.getJooqTable()).renameTo(newName + "search_vector_trigger");

    // rename table
    jooq.alterTable(table.getJooqTable()).renameTo(name(table.getSchemaName(), newName)).execute();

    // recreate search trigger
    createSearchTrigger(jooq, table, newName);
  }

  static void createOrReplaceKeys(DSLContext jooq, SqlTableMetadata table) {
    for (Integer key : table.getKeys().keySet()) {
      createOrReplaceKey(jooq, table, key, table.getKeyFields(key));
    }
  }

  static void executeDropKey(DSLContext jooq, TableMetadata table, Integer key) {
    jooq.alterTable(table.getJooqTable())
        .dropConstraintIfExists(name(table.getTableName() + "_KEY" + key))
        .execute();
  }

  static void executeSetInherit(DSLContext jooq, TableMetadata table, TableMetadata other) {
    if (other.getPrimaryKeys().isEmpty()) {
      throw new MolgenisException(
          "Extend failed: Cannot make table '"
              + table.getTableName()
              + "' extend table '"
              + table.getInheritName()
              + "' because table primary key is null");
    }

    // remove meta, we use super class meta
    executeRemoveMetaColumns(jooq, table);

    TableMetadata copyTm = new TableMetadata(table.getSchema(), table);
    copyTm.setInheritName(other.getTableName());
    // create primary key fields based on parent
    for (Field pkey : other.getPrimaryKeyFields()) {
      jooq.alterTable(table.getJooqTable()).addColumn(pkey).execute();
    }
    createOrReplaceKey(jooq, copyTm, 1, other.getPrimaryKeyFields());
    // create foreign key to parent
    jooq.alterTable(table.getJooqTable())
        .add(
            constraint("fkey_" + table.getTableName() + "_extends_" + other.getTableName())
                .foreignKey(other.getPrimaryKeyFields())
                .references(other.getJooqTable(), other.getPrimaryKeyFields())
                .onUpdateCascade()
                .onDeleteCascade())
        .execute();
    // add column to superclass table
    if (other.getLocalColumn(MG_TABLECLASS) == null) {
      other.add(column(MG_TABLECLASS).setReadonly(true).setPosition(10005));

      // should not be user editable, we add trigger
      createMgTableClassCannotUpdateCheck((SqlTableMetadata) other, jooq);
    }
    createOrReplaceKey(jooq, table, 1, other.getKeyFields(1));
  }

  static void createMgTableClassCannotUpdateCheck(SqlTableMetadata table, DSLContext jooq) {
    String functionName = table.getTableName() + MG_TABLECLASS_UPDATE;

    String keyColumns =
        table.getPrimaryKeyColumns().stream()
            .map(keyColumn -> name(keyColumn.getName()).toString())
            .collect(Collectors.joining(","));

    String keyValues =
        table.getPrimaryKeyColumns().stream()
            .map(
                keyColumn -> {
                  if (keyColumn.isReference()) {
                    return keyColumn.getReferences().stream()
                        .map(ref -> "OLD." + name(ref.getName()))
                        .collect(Collectors.joining("||','||"));
                  } else {
                    return "OLD." + name(keyColumn.getName());
                  }
                })
            .collect(Collectors.joining("||','||"));

    dropMgTableClassCannotUpdateCheck(table, jooq);

    jooq.execute(
        "CREATE OR REPLACE FUNCTION {0}() RETURNS trigger AS $BODY$ "
            + "\nBEGIN"
            + "\n\tIF OLD.{1} <> NEW.{1} THEN"
            + "\n\t\tRAISE EXCEPTION USING ERRCODE='23505'"
            + ", MESSAGE = 'insert or update on table ' || NEW.{1} || ' violates primary key constraint'"
            + ", DETAIL = 'Duplicate key: ('||{2}||')=('|| {3} ||') already exists in inherited table ' || OLD.{1};"
            + "\n\tEND IF;"
            + "\n\tRETURN NEW;"
            + "\nEND; $BODY$ LANGUAGE plpgsql;",
        name(table.getSchemaName(), functionName),
        name(MG_TABLECLASS),
        inline(keyColumns),
        keyword(keyValues));

    jooq.execute(
        "CREATE CONSTRAINT TRIGGER {0} "
            + "\n\tAFTER UPDATE OF {1} ON {2}"
            + "\n\tDEFERRABLE INITIALLY IMMEDIATE "
            + "\n\tFOR EACH ROW EXECUTE PROCEDURE {3}()",
        name(functionName),
        name(MG_TABLECLASS),
        table.getJooqTable(),
        name(table.getSchemaName(), functionName));
  }

  private static void dropMgTableClassCannotUpdateCheck(SqlTableMetadata table, DSLContext jooq) {
    String functionName = table.getTableName() + MG_TABLECLASS_UPDATE;
    jooq.execute("DROP TRIGGER IF EXISTS {0} ON {1};", name(functionName), table.getJooqTable());
    jooq.execute("DROP FUNCTION IF EXISTS {0}", name(table.getSchemaName(), functionName));
  }

  static Name[] asJooqNames(List<String> strings) {
    List<Name> names = new ArrayList<>();
    for (String string : strings) {
      names.add(name(string));
    }
    return names.toArray(new Name[names.size()]);
  }

  // helper methods
  static org.jooq.Table getJooqTable(TableMetadata table) {
    return DSL.table(name(table.getSchema().getName(), table.getTableName()));
  }

  static void createOrReplaceKey(
      DSLContext jooq, TableMetadata table, Integer index, List<Field<?>> keyFields) {
    Name uniqueName = name(table.getTableName() + "_KEY" + index);
    jooq.execute("ALTER TABLE {0} DROP CONSTRAINT IF EXISTS {1}", getJooqTable(table), uniqueName);
    // when we upgrade to psql 15 we can enable this
    //    if (keyFields.size() > 1) {
    //      // in composite keys allow nulls
    //      jooq.execute(
    //          "ALTER TABLE {0} ADD CONSTRAINT {1} UNIQUE NULLS NOT DISTINCT  ({2})",
    //          table.getJooqTable(),
    //          name(uniqueName),
    //          keyword(
    //              keyFields.stream()
    //                  .map(field -> name(field.getName()).toString())
    //                  .collect(Collectors.joining(","))));
    //    } else {
    jooq.alterTable(getJooqTable(table))
        .add(constraint(name(uniqueName)).unique(keyFields.toArray(new Field[keyFields.size()])))
        .execute();
    //    }
  }

  static void executeDropTable(DSLContext jooq, TableMetadata table) {
    try {
      // disableChangeLog
      disableChangeLog((SqlDatabase) table.getSchema().getDatabase(), table);

      // drop search trigger
      jooq.execute(
          "DROP FUNCTION IF EXISTS {0} CASCADE",
          name(table.getSchema().getName(), getSearchTriggerName(table.getTableName())));

      // drop trigger function if extended
      dropMgTableClassCannotUpdateCheck((SqlTableMetadata) table, jooq);

      // drop audit trigger
      jooq.execute(
          ChangeLogUtils.buildAuditTriggerRemove(
              table.getSchema().getName(), table.getTableName()));
      jooq.execute(
          ChangeLogUtils.buildProcessAuditFunctionRemove(
              table.getSchema().getName(), table.getTableName()));

      // drop all triggers from all columns
      List<Column> columns = table.getStoredColumns();
      sortColumnsByDependency(columns);
      for (Column c : columns) {
        executeRemoveColumn(jooq, c);
      }

      // drop the table
      jooq.dropTable(name(table.getSchema().getName(), table.getTableName())).execute();
      MetadataUtils.deleteTable(jooq, table);
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("Drop table failed", dae);
    }
  }

  private static String getRolePrefix(TableMetadata table) {
    return SqlSchemaMetadataExecutor.getRolePrefix(table.getSchema().getName());
  }

  static String updateSearchIndexTriggerFunction(
      DSLContext jooq, TableMetadata table, String tableName) {
    // TODO should also join in REFBACK column to make them searchable as part of 'mew'
    //  TODO and then also should trigger indexing on update for tables with a REF to me so trigger
    // on ref
    // change
    // then?

    String triggerName = getSearchTriggerName(tableName);
    String triggerfunction =
        String.format("\"%s\".\"%s\"()", table.getSchema().getName(), triggerName);

    StringBuilder mgSearchVector = new StringBuilder("' '");
    for (Column c : table.getStoredColumns()) {
      if (!c.isSystemColumn()) {
        if (c.isFile()) {
          // do nothing for now
        } else if (c.isReference()) {
          for (Reference r : c.getReferences()) {
            mgSearchVector.append(
                String.format(" || coalesce(new.\"%s\"::text,'') || ' '", r.getName()));
          }
        } else {
          mgSearchVector.append(
              String.format(" || coalesce(new.\"%s\"::text,'') || ' '", c.getName()));
        }
      }
    }

    String functionBody =
        String.format(
            "CREATE OR REPLACE FUNCTION %s RETURNS trigger AS $$\n"
                + "begin\n"
                + "\tnew.%s:= %s  ;\n"
                + "\treturn new;\n"
                + "end\n"
                + "$$ LANGUAGE plpgsql;",
            triggerfunction, name(searchColumnName(tableName)), mgSearchVector);

    jooq.execute(functionBody);
    jooq.execute(
        "ALTER FUNCTION " + triggerfunction + " OWNER TO {0}",
        name(getRolePrefix(table) + Privileges.MANAGER.toString()));
    return triggerfunction;
  }

  static String searchColumnName(String tableName) {
    return tableName + TEXT_SEARCH_COLUMN_NAME;
  }

  private static String getSearchTriggerName(String tableName) {
    return tableName + "search_vector_trigger";
  }

  private static void dropSearchTrigger(DSLContext jooq, TableMetadata table) {
    String triggerfunction = getSearchTriggerName(table.getTableName());
    jooq.execute("DROP FUNCTION {0} CASCADE", name(table.getSchema().getName(), triggerfunction));
  }

  private static void createSearchTrigger(DSLContext jooq, TableMetadata table, String tableName) {
    // 3. create the trigger function to automatically update the MG_SEARCH_INDEX_COLUMN_NAME
    String triggerfunction = updateSearchIndexTriggerFunction(jooq, table, tableName);
    Name searchColumnName = name(searchColumnName(tableName));

    // 4. add trigger to update the tsvector on each insert or update
    jooq.execute(
        "CREATE TRIGGER {0} BEFORE INSERT OR UPDATE ON {1} FOR EACH ROW EXECUTE FUNCTION "
            + triggerfunction,
        searchColumnName,
        name(table.getSchemaName(), tableName));
  }

  private static void executeAddMetaColumns(TableMetadata table) {
    // negative positions so they don't interfere with the positions of user provided columns
    table.add(column(MG_DRAFT).setType(BOOL).setPosition(-5));
    table.add(column(MG_INSERTEDBY).setPosition(-4));
    table.add(column(MG_INSERTEDON).setType(DATETIME).setPosition(-3));
    table.add(column(MG_UPDATEDBY).setPosition(-2));
    table.add(column(MG_UPDATEDON).setType(DATETIME).setPosition(-1));
  }

  private static void executeRemoveMetaColumns(DSLContext jooq, TableMetadata table) {
    // don't delete superclass columns so check local colum
    if (table.getLocalColumn(MG_DRAFT) != null) table.dropColumn(MG_DRAFT);
    if (table.getLocalColumn(MG_INSERTEDBY) != null) table.dropColumn(MG_INSERTEDBY);
    if (table.getLocalColumn(MG_INSERTEDON) != null) table.dropColumn(MG_INSERTEDON);
    if (table.getLocalColumn(MG_UPDATEDBY) != null) table.dropColumn(MG_UPDATEDBY);
    if (table.getLocalColumn(MG_UPDATEDON) != null) table.dropColumn(MG_UPDATEDON);
  }

  private static void executeEnableSearch(DSLContext jooq, TableMetadata table) {

    Table jooqTable = getJooqTable(table);
    Name searchColumnName = name(searchColumnName(table.getTableName()));
    Name searchIndexName = name(table.getTableName() + "_search_idx");

    // also add text search  column
    // 1. create column
    jooq.execute("ALTER TABLE {0} ADD COLUMN {1} TEXT", jooqTable, searchColumnName);

    // 2. create trigram index
    jooq.execute(
        "CREATE INDEX {0} ON {1} USING GIN( {2} gin_trgm_ops)",
        searchIndexName, jooqTable, searchColumnName);

    createSearchTrigger(jooq, table, table.getTableName());
  }

  static void checkNoColumnWithSameNameExistsInSubclass(
      String columnName, TableMetadata tm, DSLContext jooq) {
    String recursiveQuerySql =
        """
WITH RECURSIVE inherited_columns AS (
SELECT\s
  a.table_schema,
  a.table_name,
  a.column_name,
  a.position,
  a.key,
  b.import_schema,
  b.table_inherits
  FROM "MOLGENIS".column_metadata a, "MOLGENIS".table_metadata b
  WHERE a.table_schema = b.table_schema
  AND a.table_name=b.table_name
  AND b.table_name={0}
  AND b.table_schema={1}
UNION
SELECT
  a.table_schema,
  a.table_name,
  a.column_name,
  a.key,
  a.position,
  b.import_schema,
  b.table_inherits
  FROM "MOLGENIS".column_metadata a, "MOLGENIS".table_metadata b,  inherited_columns c
  WHERE b.table_inherits=c.table_name
  AND (b.import_schema IS NULL AND b.table_schema = c.table_schema OR b.import_schema = c.table_schema)
  AND a.table_schema = b.table_schema
  AND a.table_name=b.table_name
)
SELECT table_schema, table_name, column_name, key FROM inherited_columns WHERE key <> 1 AND column_name={2} AND (table_name <> {0} OR table_schema <> {1});
"""; // nb does not apply to key=1 columns, these are copied between subclasses

    Result<Record> result =
        jooq.fetch(recursiveQuerySql, tm.getTableName(), tm.getSchemaName(), columnName);
    if (!result.isEmpty()) {
      String schemaName = result.get(0).get(0, String.class);
      String tableName = result.get(0).get(1, String.class);
      throw new MolgenisException(
          String.format(
              "Cannot create column '%s.%s' because this column name already exists in subclass table '%s'.'%s' ",
              tm.getTableName(), columnName, schemaName, tableName));
    }
  }
}
