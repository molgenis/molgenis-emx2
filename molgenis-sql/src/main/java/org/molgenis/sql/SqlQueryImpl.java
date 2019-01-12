package org.molgenis.sql;

import org.jooq.*;

import java.util.*;

import static org.jooq.impl.DSL.*;
import static org.molgenis.sql.SqlRow.MOLGENISID;

class SqlQueryImpl implements SqlQuery {

  private SqlDatabase db;
  private DSLContext sql;
  private State state = null;
  private String lastFrom = null;
  private String lastSelect = null;
  private Map<String, From> select = new LinkedHashMap<>();
  private Condition conditions = null;

  enum State {
    FROM,
    SELECT,
    NONE
  }

  private class From {
    Map<String, SqlColumn> columns = new LinkedHashMap<>();
    SqlTable fromTable;
    String fromColumn;
    String joinTable;
    String joinColumn;
  }

  public SqlQueryImpl(SqlDatabase db, DSLContext sql) {
    this.db = db;
    this.sql = sql;
  }

  @Override
  public SqlQuery from(String table) throws SqlDatabaseException {
    if (lastFrom != null) throw new SqlDatabaseException("You can call from() only once");

    SqlTable t = db.getTable(table);
    if (t == null) throw new SqlDatabaseException("fromTable " + table + " does not exist");
    From f = new From();
    f.fromTable = t;

    select.put(table, f);
    state = State.FROM;
    lastFrom = table;
    return this;
  }

  @Override
  public SqlQuery join(String table, String toTable, String on) throws SqlDatabaseException {
    if (lastFrom == null)
      throw new SqlDatabaseException("You can call join() only after first calling a from()");

    From tableSelect = new From();

    // add fromTable
    SqlTable fromTable = db.getTable(table);
    if (fromTable == null) {
      throw new SqlDatabaseException("fromTable " + table + " does not exist");
    }
    tableSelect.fromTable = fromTable;

    String cannotJoin = "Cannot join ('";

    // add joinTable reference
    From temp = this.select.get(toTable);
    if (temp == null) {
      throw new SqlDatabaseException(
          cannotJoin
              + table
              + "','"
              + toTable
              + "','"
              + on
              + "'): to join fromTable '"
              + toTable
              + "' not in getQuery");
    }
    SqlTable joinTable = temp.fromTable;
    tableSelect.joinTable = toTable;

    // add fromColumn and joinColumn
    if (fromTable.getColumn(on) != null) {
      String refTable = fromTable.getColumn(on).getRefTable().getName();
      if (!refTable.equals(joinTable.getName())) {
        throw new SqlDatabaseException(
            cannotJoin
                + table
                + "','"
                + toTable
                + "','"
                + on
                + "'): select '"
                + on
                + "' references wrong from '"
                + refTable
                + "'");
      }
      tableSelect.fromColumn = on;
      tableSelect.joinColumn = MOLGENISID;
    } else if (joinTable.getColumn(on) != null) {
      String refTable = joinTable.getColumn(on).getRefTable().getName();
      if (!refTable.equals(fromTable.getName())) {
        throw new SqlDatabaseException(
            cannotJoin
                + table
                + "','"
                + toTable
                + "','"
                + on
                + "'): select '"
                + on
                + "' references wrong from '"
                + refTable
                + "'");
      }

      tableSelect.fromColumn = MOLGENISID;
      tableSelect.joinColumn = on;
    } else {
      throw new SqlDatabaseException(
          cannotJoin
              + table
              + "','"
              + toTable
              + "','"
              + on
              + "'): to join select '"
              + on
              + "' in neither tables");
    }

    this.select.put(table, tableSelect);
    state = State.FROM;
    lastFrom = table;
    return this;
  }

  @Override
  public SqlQuery select(String column) throws SqlDatabaseException {
    From f = select.get(lastFrom);
    SqlColumn c = f.fromTable.getColumn(column);
    if (c == null)
      throw new SqlDatabaseException(
          "select " + column + " does not exist in select fromTable " + lastFrom);
    f.columns.put(column, c);
    state = State.SELECT;
    lastSelect = column;
    return this;
  }

  @Override
  public SqlQuery as(String alias) throws SqlDatabaseException {
    switch (state) {
      case FROM:
        select.put(alias, select.remove(lastFrom));
        lastFrom = alias;
        break;
      case SELECT:
        select.get(lastFrom).columns.put(alias, select.get(lastFrom).columns.remove(lastSelect));
        lastSelect = alias;
        break;
      default:
        throw new SqlDatabaseException("cannot call as(" + alias + ") at this point");
    }

    state = State.NONE;
    return this;
  }

  @Override
  public List<SqlRow> retrieve() throws SqlDatabaseException {

    List<SqlRow> rows = new ArrayList<>();

    // get columns
    List<Field> columns = new ArrayList<>();
    Map<String, SqlType> colAliases = new LinkedHashMap<>();
    select.forEach(
        (tableName, table) ->
            table.columns.forEach(
                (colName, col) -> {
                  columns.add(field(name(tableName, col.getName())).as(colName));
                  colAliases.put(colName, col.getType());
                }));

    // create getQuery
    SelectSelectStep step = sql.select(columns);
    SelectJoinStep joinStep = null;
    for (Map.Entry<String, From> selectEntry : select.entrySet()) {
      String alias = selectEntry.getKey();
      From def = selectEntry.getValue();
      if (def.joinTable == null) {
        joinStep = step.from(table(name(def.fromTable.getName())).as(alias));
      } else if (joinStep != null) {
        joinStep =
            joinStep
                .leftOuterJoin(table(name(def.fromTable.getName())).as(alias))
                .on(
                    field(name(alias, def.fromColumn))
                        .eq(field(name(def.joinTable, def.joinColumn))));
      }
    }

    if (joinStep == null) throw new SqlDatabaseException("no tables defined as part of this query");
    joinStep.where(conditions);
    Result<Record> result = joinStep.fetch();
    for (Record r : result) {
      rows.add(new SqlRowImpl(r));
    }
    return rows;
  }

  private void validate(String table, String column, SqlType type) throws SqlDatabaseException {
    if (this.select.get(table) == null)
      throw new SqlDatabaseException(
          "table/alias '" + table + "' not known. Choose one of " + this.select.keySet());
    if (this.select.get(table).fromTable.getColumn(column) == null)
      throw new SqlDatabaseException(
          "select '" + column + "' not known in table/alias '" + table + "'");
    if (!type.equals(this.select.get(table).fromTable.getColumn(column).getType()))
      throw new SqlDatabaseException(
          "select '"
              + column
              + "' not of expected type '"
              + type
              + "', instead found "
              + this.select.get(table).fromTable.getColumn(column).getType());
  }

  private SqlQuery eqHelper(String table, String column, Object... value) {
    if (value.length > 1) {
      if (conditions == null) conditions = field(name(table, column)).in(value);
      else conditions = conditions.and(field(name(table, column)).in(value));
    }
    if (value.length == 1) {
      if (conditions == null) conditions = field(name(table, column)).eq(value[0]);
      else conditions = conditions.and(field(name(table, column)).eq(value[0]));
    }
    return this;
  }

  @Override
  public SqlQuery eq(String table, String column, UUID... value) throws SqlDatabaseException {
    validate(table, column, SqlType.UUID);
    return eqHelper(table, column, value);
  }

  @Override
  public SqlQuery eq(String table, String column, String... value) throws SqlDatabaseException {
    validate(table, column, SqlType.STRING);
    return eqHelper(table, column, value);
  }

  @Override
  public SqlQuery eq(String table, String column, Integer... value) throws SqlDatabaseException {
    validate(table, column, SqlType.INT);
    return eqHelper(table, column, value);
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("QUERY(");
    for (Map.Entry<String, From> selectEntry : select.entrySet()) {
      for (String c : selectEntry.getValue().columns.keySet()) {
        SqlColumn col = selectEntry.getValue().columns.get(c);
        builder
            .append("\n\tSELECT(")
            .append(selectEntry.getKey())
            .append(".")
            .append(col.getName())
            .append(")");
        if (!c.equals(col.getName())) {
          builder.append(" AS '").append(c).append("'");
        }
      }
    }
    for (Map.Entry<String, From> entry : select.entrySet()) {
      String f = entry.getKey();
      From table = entry.getValue();
      String name = table.fromTable.getName();
      builder.append("\n\tFROM(").append(name).append(")");
      if (select.get(f).joinTable != null) {
        builder
            .append(" JOIN(")
            .append(table.fromColumn)
            .append("=")
            .append(table.joinTable)
            .append(".")
            .append(table.joinColumn)
            .append(")");
      }
      if (!f.equals(name)) {
        builder.append(" AS '").append(f).append("'");
      }
    }
    builder.append("\n);");

    return builder.toString();
  }
}
