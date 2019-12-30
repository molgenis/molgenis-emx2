package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.emx2.*;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.sql.Constants.MG_TEXT_SEARCH_COLUMN_NAME;
import static org.molgenis.emx2.sql.SqlColumnUtils.executeRemoveColumn;

public class SqlTableMetadataUtils {

  static void executeCreateTable(DSLContext jooq, TableMetadata table) {

    // create the table
    Name tableName = name(table.getSchema().getName(), table.getTableName());
    jooq.createTable(tableName).columns().execute();
    MetadataUtils.saveTableMetadata(jooq, table);

    // grant rights to schema manager, editor and viewer roles
    jooq.execute(
        "GRANT SELECT ON {0} TO {1}",
        tableName, name(getRolePrefix(table) + DefaultRoles.VIEWER.toString()));
    jooq.execute(
        "GRANT INSERT, UPDATE, DELETE, REFERENCES, TRUNCATE ON {0} TO {1}",
        tableName, name(getRolePrefix(table) + DefaultRoles.EDITOR.toString()));
    jooq.execute(
        "ALTER TABLE {0} OWNER TO {1}",
        tableName, name(getRolePrefix(table) + DefaultRoles.MANAGER.toString()));

    if (table.getInherit() != null) {
      executeSetInherit(jooq, table, table.getInheritedTable());
    }

    // then create other columns (use super to prevent side effects)
    for (Column column : table.getLocalColumns()) {
      // if inherited, pkey is aready there
      if (table.getInherit() == null || !column.getName().equals(table.getPrimaryKey())) {
        SqlColumnUtils.executeCreateColumn(jooq, new Column(table, column));
      }
    }

    // finally unique constraints
    for (String[] unique : table.getUniques()) {
      executeCreateUnique(jooq, table, unique);
    }

    executeEnableSearch(jooq, table);
  }

  static void executeSetInherit(DSLContext jooq, TableMetadata table, TableMetadata other) {
    // todo remove old inherit
    if (other == null) {
      throw new MolgenisException(
          "Extend failed",
          "Cannot make table '"
              + table.getTableName()
              + "' extend table '"
              + table.getInherit()
              + "' because table '"
              + table.getInherit()
              + "' does not exist");
    }
    if (other.getPrimaryKey() == null) {
      throw new MolgenisException(
          "Extend failed",
          "Cannot make table '"
              + table.getTableName()
              + "' extend table '"
              + table.getInherit()
              + "' because table primary key is null");
    }
    table.addColumn(column(other.getPrimaryKey()).type(REF).refTable(other.getTableName()));
    table.setPrimaryKey(other.getPrimaryKey());
    MetadataUtils.saveTableMetadata(jooq, table);
  }

  static void executeSetPrimaryKey(DSLContext jooq, TableMetadata table, String columName) {
    // drop previous primary key if exists
    jooq.execute(
        "ALTER TABLE {0} DROP CONSTRAINT IF EXISTS {1}",
        getJooqTable(table), getPrimaryKeyContraintName(table));

    // createTableIfNotExists the new one
    jooq.alterTable(getJooqTable(table)).add(constraint().primaryKey(name(columName))).execute();
  }

  private static Name getPrimaryKeyContraintName(TableMetadata table) {
    return name(table.getTableName() + "_pkey");
  }

  // helper methods
  static org.jooq.Table getJooqTable(TableMetadata table) {
    return DSL.table(name(table.getSchema().getName(), table.getTableName()));
  }

  static void executeCreateUnique(DSLContext jooq, TableMetadata table, String[] columnNames) {
    String uniqueName = table.getTableName() + "_" + String.join("_", columnNames) + "_UNIQUE";
    jooq.alterTable(getJooqTable(table))
        .add(constraint(name(uniqueName)).unique(columnNames))
        .execute();
    MetadataUtils.saveUnique(jooq, table, columnNames);
  }

  static void executeDropTable(DSLContext jooq, TableMetadata table) {
    try {
      Table thisTable = getJooqTable(table);

      // remove pkey
      if (table.getPrimaryKey() != null) {
        jooq.alterTable(thisTable).dropConstraint(getPrimaryKeyContraintName(table)).execute();
      }

      // drop search trigger
      jooq.execute(
          "DROP FUNCTION {0} CASCADE",
          name(table.getSchema().getName(), getSearchTriggerName(table)));

      // drop all triggers from all columns
      for (Column c : table.getLocalColumns()) {
        executeRemoveColumn(jooq, c);
      }

      // drop the table
      jooq.dropTable(name(table.getSchema().getName(), table.getTableName())).cascade().execute();
      MetadataUtils.deleteTable(jooq, table);
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("Drop table failed", dae);
    }
  }

  private static String getRolePrefix(TableMetadata table) {
    return SqlSchemaMetadataUtils.getRolePrefix(table.getSchema());
  }

  static String updateSearchIndexTriggerFunction(DSLContext jooq, TableMetadata table) {
    // TODO should also join in REFBACK column to make them searchable as part of 'mew'
    //  TODO and then also should trigger indexing on update for tables with a REF to me so trigger
    // on ref
    // change
    // then?

    String triggerName = getSearchTriggerName(table);
    String triggerfunction =
        String.format("\"%s\".\"%s\"()", table.getSchema().getName(), triggerName);

    StringBuilder mgSearchVector = new StringBuilder("' '");
    for (Column c : table.getLocalColumns()) {
      if (!c.getName().startsWith("MG_"))
        mgSearchVector.append(
            String.format(" || coalesce(new.\"%s\"::text,'') || ' '", c.getName()));
    }

    String functionBody =
        String.format(
            "CREATE OR REPLACE FUNCTION %s RETURNS trigger AS $$\n"
                + "begin\n"
                + "\tnew.%s:= %s  ;\n"
                + "\treturn new;\n"
                + "end\n"
                + "$$ LANGUAGE plpgsql;",
            triggerfunction, name(MG_TEXT_SEARCH_COLUMN_NAME), mgSearchVector);

    jooq.execute(functionBody);
    jooq.execute(
        "ALTER FUNCTION " + triggerfunction + " OWNER TO {0}",
        name(getRolePrefix(table) + DefaultRoles.MANAGER.toString()));
    return triggerfunction;
  }

  private static String getSearchTriggerName(TableMetadata table) {
    return table.getTableName() + "search_vector_trigger";
  }

  static void executeRemoveUnique(DSLContext jooq, TableMetadata table, String[] unique) {
    String uniqueName = table.getTableName() + "_" + String.join("_", unique) + "_UNIQUE";
    jooq.alterTable(getJooqTable(table)).dropConstraint(name(uniqueName)).execute();
    MetadataUtils.deleteUnique(jooq, table, unique);
  }

  private static void executeEnableSearch(DSLContext jooq, TableMetadata table) {

    Table jooqTable = getJooqTable(table);
    Name searchColumnName = name(MG_TEXT_SEARCH_COLUMN_NAME);
    Name searchIndexName = name(table.getTableName() + "_search_idx");

    // 1. add tsvector column with index
    // jooq.execute("ALTER TABLE {0} ADD COLUMN {1} tsvector", jooqTable, searchColumnName);

    // for future performance enhancement consider studying 'gin (t gin_trgm_ops)

    // 2. createColumn index on that column to speed up search
    // jooq.execute(
    //   "CREATE INDEX {0} ON {1} USING GIN( {2} )", searchIndexName, jooqTable, searchColumnName);

    // also add text search  column
    // 1. create column
    jooq.execute("ALTER TABLE {0} ADD COLUMN {1} TEXT", jooqTable, searchColumnName);

    // 2. create trigram index
    jooq.execute(
        "CREATE INDEX {0} ON {1} USING GIN( {2} gin_trgm_ops)",
        searchIndexName, jooqTable, searchColumnName);

    // 3. createColumn the trigger function to automatically update the MG_SEARCH_INDEX_COLUMN_NAME
    String triggerfunction = updateSearchIndexTriggerFunction(jooq, table);

    // 4. add trigger to update the tsvector on each insert or update
    jooq.execute(
        "CREATE TRIGGER {0} BEFORE INSERT OR UPDATE ON {1} FOR EACH ROW EXECUTE FUNCTION "
            + triggerfunction,
        searchColumnName,
        jooqTable);
  }
}
