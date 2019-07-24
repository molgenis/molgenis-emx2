package org.molgenis.sql;

import org.jooq.*;
import org.jooq.impl.SQLDataType;
import org.molgenis.*;
import org.molgenis.Table;
import org.molgenis.beans.SchemaBean;

import java.util.*;

import static org.jooq.impl.DSL.constraint;
import static org.jooq.impl.DSL.name;
import static org.molgenis.Column.Type.*;
import static org.molgenis.Database.Prefix.MGROLE_;
import static org.molgenis.Database.Roles.*;
import static org.molgenis.Row.MOLGENISID;

public class SqlSchema extends SchemaBean {
  private Database db;
  private DSLContext sql;

  public SqlSchema(Database db, DSLContext sql, String name) throws MolgenisException {
    super(name);
    this.sql = sql;
    this.db = db;

    // alas, muuuuch faster than using jooq metadata features
    Map<String, Map<String, Set<String>>> uniques = new LinkedHashMap<>();
    List<Record> records =
        sql.fetch(
            "SELECT t.table_name, c.column_name, c.data_type, c.is_nullable, kcu.constraint_name, ccu.table_name as ref_table, ccu.column_name as ref_column "
                + "FROM information_schema.tables t "
                + "NATURAL JOIN information_schema.columns c "
                + "LEFT JOIN information_schema.key_column_usage kcu ON c.table_name = kcu.table_name AND c.column_name = kcu.column_name "
                + "LEFT JOIN information_schema.table_constraints fkey ON kcu.constraint_name = fkey.constraint_name AND fkey.constraint_type = 'FOREIGN KEY' "
                + "LEFT JOIN information_schema.constraint_column_usage ccu ON fkey.constraint_name = ccu.constraint_name "
                + "WHERE t.table_schema = {0} ORDER BY c.ordinal_position, t.table_name",
            getName());
    // sorting by ordinal position ensures all tables are created before xrefs are added
    for (Record record : records) {

      // System.out.println(record);

      String tableName = record.get("table_name", String.class);
      String columnName = record.get("column_name", String.class);
      String dataType = record.get("data_type", String.class);
      String refTableName = record.get("ref_table", String.class);
      String constraintName = record.get("constraint_name", String.class);
      String refColumnName = record.get("ref_column", String.class);
      boolean isNullable = record.get("is_nullable", String.class).equals("YES") ? true : false;

      // get unique metadata
      if (constraintName != null
          && (constraintName.endsWith("UNIQUE") || constraintName.startsWith("PK"))) {
        uniques.putIfAbsent(tableName, new LinkedHashMap<>());
        uniques.get(tableName).putIfAbsent(constraintName, new HashSet<>());
        uniques.get(tableName).get(constraintName).add(columnName);
      }

      // get table and column
      SqlTable t;
      try {
        t = (SqlTable) getTable(tableName);
      } catch (Exception e) {
        t = new SqlTable(this, sql, tableName);
        this.loadTable(t);
      }
      if (refTableName != null) {
        Table refTable = getTable(refTableName);
        t.loadColumn(new SqlColumn(sql, t, columnName, refTableName, refColumnName, isNullable));
      } else {
        t.loadColumn(
            new SqlColumn(sql, t, columnName, getTypeFormPsqlString(dataType), isNullable));
      }
    }
    for (String tableName : getTableNames()) {
      Table t = getTable(tableName);
      ((SqlTable) t).loadMrefs();
      if (uniques.containsKey(t.getName())) {
        for (Set<String> keys : uniques.get(t.getName()).values()) {
          ((SqlTable) t).loadUnique(new ArrayList<>(keys));
        }
      }
    }
  }

  private Column.Type getTypeFormPsqlString(String dataType) {
    switch (dataType) {
      case "character varying":
        return STRING;
      case "uuid":
        return UUID;
      case "bool":
        return BOOL;
      case "integer":
        return INT;
      case "decimal":
        return DECIMAL;
      case "text":
        return TEXT;
      case "date":
        return DATE;
      case "datatime":
        return DATETIME;
      case "enum":
        return ENUM;
      default:
        throw new RuntimeException("data type unknown " + dataType);
    }
  }

  @Override
  public SqlTable createTable(String name) throws MolgenisException {
    Name tableName = name(getName(), name);
    sql.createTableIfNotExists(tableName)
        .column(MOLGENISID, SQLDataType.UUID)
        .constraints(constraint("PK_" + name).primaryKey(MOLGENISID))
        .execute();
    // immediately make the 'admin' owner
    sql.execute(
        "ALTER TABLE {0} OWNER TO {1}",
        tableName, name(MGROLE_ + getName().toUpperCase() + _MANAGER));
    sql.execute(
        "GRANT SELECT ON {0} TO {1}", tableName, name(MGROLE_ + getName().toUpperCase() + _VIEWER));
    sql.execute(
        "GRANT INSERT, UPDATE, DELETE, REFERENCES, TRUNCATE ON {0} TO {1}",
        tableName, name(MGROLE_ + getName().toUpperCase() + _EDITOR));
    SqlTable t = new SqlTable(this, sql, name);
    super.loadTable(t);
    return t;
  }

  @Override
  public void grantAdmin(String user) {
    sql.execute(
        "GRANT {0} TO {1} WITH ADMIN OPTION",
        name(MGROLE_ + getName().toUpperCase() + _ADMIN), name(user));
  }

  @Override
  public void grantManage(String user) {
    sql.execute("GRANT {0} TO {1}", name(MGROLE_ + getName().toUpperCase() + _MANAGER), name(user));
  }

  @Override
  public void grantEdit(String user) {
    sql.execute("GRANT {0} TO {1}", name(MGROLE_ + getName().toUpperCase() + _EDITOR), name(user));
  }

  @Override
  public void grantView(String user) {
    sql.execute("GRANT {0} TO {1}", name(MGROLE_ + getName().toUpperCase() + _VIEWER), name(user));
  }

  @Override
  public void dropTable(String tableId) {
    sql.dropTable(name(getName(), tableId)).execute();
    super.dropTable(tableId);
  }
}
