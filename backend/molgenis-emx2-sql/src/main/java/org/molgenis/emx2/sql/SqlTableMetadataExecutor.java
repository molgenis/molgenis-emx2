package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.Name;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.DefaultRoles;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.TableMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.sql.Constants.MG_TEXT_SEARCH_COLUMN_NAME;
import static org.molgenis.emx2.sql.SqlColumnExecutor.asJooqTable;
import static org.molgenis.emx2.sql.SqlColumnExecutor.executeRemoveColumn;

class SqlTableMetadataExecutor {
  private SqlTableMetadataExecutor() {}

  static void executeCreateTable(DSLContext jooq, TableMetadata table) {

    // create the table
    Table tableName = asJooqTable(table);
    jooq.execute("CREATE TABLE {0}()", asJooqTable(table));
    // jooq.createTable(asJooqTable(table)).columns(new Name[0]).execute();
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

    // create columns from primary key of superclass
    if (table.getInherit() != null) {
      executeSetInherit(jooq, table, table.getInheritedTable());
    }

    // then create columns (use super to prevent side effects)
    for (Column column : table.getLocalColumns()) {
      if (table.getInherit() != null
          && table.getInheritedTable().getColumn(column.getName()) != null) {
        // don't create superclass keys, that is already done
      } else {
        SqlColumnExecutor.executeCreateColumn(jooq, new Column(table, column));
      }
    }

    // then create unique
    for (Map.Entry<Integer, List<String>> key : table.getKeys().entrySet()) {
      createOrReplaceUnique(jooq, table, key.getKey(), asJooqNames(key.getValue()));
    }

    // then create foreign keys etc
    for (Column column : table.getLocalColumns()) {
      if (table.getInherit() != null
          && table.getInheritedTable().getColumn(column.getName()) != null) {
        // don't create superclass keys, that is already done
      } else {
        SqlColumnExecutor.executeSetForeignkeys(jooq, new Column(table, column));
      }
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
    if (other.getPrimaryKeys() == null) {
      throw new MolgenisException(
          "Extend failed",
          "Cannot make table '"
              + table.getTableName()
              + "' extend table '"
              + table.getInherit()
              + "' because table primary key is null");
    }
    for (String pkey : other.getPrimaryKeys()) {
      table.add(column(pkey).type(REF).refTable(other.getTableName()).pkey());
    }
    MetadataUtils.saveTableMetadata(jooq, table);
  }

  static void executeSetPrimaryKey(DSLContext jooq, TableMetadata table, Name[] columnNames) {
    // drop previous primary key if exists
    jooq.execute(
        "ALTER TABLE {0} DROP CONSTRAINT IF EXISTS {1}",
        getJooqTable(table), getPrimaryKeyContraintName(table));

    // create the new one
    jooq.alterTable(getJooqTable(table)).add(constraint().primaryKey(columnNames)).execute();
  }

  static Name[] asJooqNames(List<String> strings) {
    List<Name> names = new ArrayList<>();
    for (String string : strings) {
      names.add(name(string));
    }
    return names.toArray(new Name[names.size()]);
  }

  private static Name getPrimaryKeyContraintName(TableMetadata table) {
    return name(table.getTableName() + "_pkey");
  }

  // helper methods
  static org.jooq.Table getJooqTable(TableMetadata table) {
    return DSL.table(name(table.getSchema().getName(), table.getTableName()));
  }

  static void createOrReplaceUnique(
      DSLContext jooq, TableMetadata table, Integer index, Name[] columnNames) {

    Name uniqueName = name(table.getTableName() + "_KEY" + index);

    // drop previous unique if exists
    if (index == 1) {
      executeSetPrimaryKey(jooq, table, columnNames);
    } else {
      jooq.execute(
          "ALTER TABLE {0} DROP CONSTRAINT IF EXISTS {1}", getJooqTable(table), uniqueName);
      jooq.alterTable(getJooqTable(table))
          .add(constraint(name(uniqueName)).unique(columnNames))
          .execute();
    }
  }

  static void executeDropTable(DSLContext jooq, TableMetadata table) {
    try {
      Table thisTable = getJooqTable(table);

      // remove pkey
      if (table.getPrimaryKeys() != null) {
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
    return SqlSchemaMetadataExecutor.getRolePrefix(table.getSchema());
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
  }

  private static void executeEnableSearch(DSLContext jooq, TableMetadata table) {

    Table jooqTable = getJooqTable(table);
    Name searchColumnName = name(MG_TEXT_SEARCH_COLUMN_NAME);
    Name searchIndexName = name(table.getTableName() + "_search_idx");

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
