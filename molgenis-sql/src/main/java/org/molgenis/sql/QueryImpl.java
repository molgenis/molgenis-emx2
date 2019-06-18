package org.molgenis.sql;

import org.jooq.*;
import org.jooq.impl.SQLDataType;
import org.molgenis.*;
import org.molgenis.Row;

import java.util.*;

import static org.jooq.impl.DSL.*;
import static org.molgenis.sql.RowImpl.MOLGENISID;
import static org.molgenis.Column.Type.MREF;
import static org.molgenis.Column.Type.REF;

class QueryImpl implements QueryOld {

  private Database db;
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
    Map<String, Column> columns = new LinkedHashMap<>();
    org.molgenis.Table fromTable;
    String fromColumn;
    String joinTable;
    String toTable;
    String toColumn;

    public From(org.molgenis.Table fromTable, String fromColumn, String toTable, String toColumn) {
      this.fromTable = fromTable;
      this.fromColumn = fromColumn;
      this.toTable = toTable;
      this.toColumn = toColumn;
    }
  }

  public QueryImpl(Database db, DSLContext sql, String tableName) throws MolgenisException {
    this.db = db;
    this.sql = sql;
    this.from(tableName);
  }

  private QueryOld from(String table) throws MolgenisException {
    org.molgenis.Table t = db.getSchema().getTable(table);
    if (t == null) throw new MolgenisException("table  " + table + " does not exist");
    select.put(table, new From(t, null, null, null));
    state = State.FROM;
    lastFrom = table;
    return this;
  }

  @Override
  public QueryOld join(String table, String toTable, String on) throws MolgenisException {

    // check fromTable
    org.molgenis.Table fromTable = db.getSchema().getTable(table);
    if (fromTable == null) {
      throw new MolgenisException(
          String.format("Cannot join table '%s': table unknown in database.", table));
    }
    // check toTable
    From temp = this.select.get(toTable);
    if (temp == null) {
      throw new MolgenisException(
          String.format(
              "Cannot join to previous selected table '%s': table not selected", toTable));
    }
    org.molgenis.Table joinTable = temp.fromTable;

    // check 'on' column
    if (fromTable.getColumn(on) != null) {
      String otherTable = fromTable.getColumn(on).getRefTable().getName();
      if (!otherTable.equals(joinTable.getName())) {
        throw new MolgenisException(
            String.format("Column '%s' does not link '%s' to '%s'", on, table, toTable));
      }
      if (MREF.equals(fromTable.getColumn(on).getType())) {
        Column col = fromTable.getColumn(on);
        org.molgenis.Table mrefTable = col.getMrefTable();
        this.select.put(
            mrefTable.getName(),
            new From(fromTable.getColumn(on).getMrefTable(), on, toTable, MOLGENISID));
        this.select.put(
            table, new From(fromTable, MOLGENISID, mrefTable.getName(), col.getMrefBack()));
      } else {
        this.select.put(table, new From(fromTable, on, MOLGENISID, toTable));
      }
    } else if (joinTable.getColumn(on) != null) {
      String refTable = joinTable.getColumn(on).getRefTable().getName();
      if (!refTable.equals(fromTable.getName())) {
        throw new MolgenisException(
            String.format("Column '%s' does not link '%s' to '%s'", on, toTable, table));
      }
      if (MREF.equals(joinTable.getColumn(on).getType())) {
        Column col = joinTable.getColumn(on);
        org.molgenis.Table mrefTable = col.getMrefTable();
        this.select.put(
            mrefTable.getName(), new From(mrefTable, col.getMrefBack(), toTable, MOLGENISID));
        this.select.put(table, new From(fromTable, MOLGENISID, mrefTable.getName(), on));
      } else {
        this.select.put(table, new From(fromTable, MOLGENISID, toTable, on));
      }
    } else {
      throw new MolgenisException(
          String.format(
              "Cannot join on column '%s': column not known in table '%s' and '%s'",
              on, toTable, table));
    }
    state = State.FROM;
    lastFrom = table;
    return this;
  }

  @Override
  public QueryOld select(String column) throws MolgenisException {
    From f = select.get(lastFrom);
    Column c = f.fromTable.getColumn(column);
    if (c == null)
      throw new MolgenisException(
          String.format(
              "select '%s' does not exist in table '%s' as '%s'",
              column, f.fromTable.getName(), lastFrom));
    f.columns.put(column, c);
    state = State.SELECT;
    lastSelect = column;
    return this;
  }

  @Override
  public QueryOld as(String alias) throws MolgenisException {
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
        throw new MolgenisException("cannot call as(" + alias + ") at this point");
    }

    state = State.NONE;
    return this;
  }

  @Override
  public List<org.molgenis.Row> retrieve() throws MolgenisException {

    // define the 'select' clause in terms of fields to be queried
    List<Field> columns = new ArrayList<>();
    Map<String, Column.Type> colAliases = new LinkedHashMap<>();
    select.forEach(
        (tableName, table) ->
            table.columns.forEach(
                (colName, col) -> {
                  columns.add(field(name(tableName, col.getName())).as(colName));
                  colAliases.put(colName, col.getType());
                }));

    // define the 'from' clause in terms of 'from/joins'
    SelectSelectStep step = sql.select(columns);
    SelectJoinStep joinStep = null;
    for (Map.Entry<String, From> selectEntry : select.entrySet()) {
      String alias = selectEntry.getKey();
      From def = selectEntry.getValue();
      // not a join
      if (def.toTable == null) {
        joinStep = step.from(table(name(def.fromTable.getName())).as(alias));
        // is a join
      } else if (joinStep != null) {
        joinStep =
            joinStep
                .leftOuterJoin(table(name(def.fromTable.getName())).as(alias))
                .on(
                    field(name(alias, def.fromColumn), SQLDataType.UUID)
                        .eq(field(name(def.toTable, def.toColumn), SQLDataType.UUID)));
      }
    }

    // define the 'where' clause
    if (joinStep == null) throw new MolgenisException("This should never happen");
    joinStep.where(conditions);

    List<Row> rows = new ArrayList<>();

    // return rows
    System.out.println(joinStep.getSQL());
    Result<Record> result = joinStep.fetch();
    for (Record r : result) {
      rows.add(new RowImpl(r));
    }
    return rows;
  }

  private void validate(String table, String column, Column.Type type) throws MolgenisException {
    if (this.select.get(table) == null)
      throw new MolgenisException(
          "table/alias '" + table + "' not known. Choose one of " + this.select.keySet());
    if (this.select.get(table).fromTable.getColumn(column) == null)
      throw new MolgenisException(
          "select '" + column + "' not known in table/alias '" + table + "'");
    Column.Type foundType = this.select.get(table).fromTable.getColumn(column).getType();
    if (!type.equals(foundType) && !(REF.equals(foundType) && Column.Type.UUID.equals(type))) {
      throw new MolgenisException(
          "select '"
              + column
              + "' not of expected type '"
              + type
              + "', instead found "
              + this.select.get(table).fromTable.getColumn(column).getType());
    }
  }

  private QueryOld eqHelper(String table, String column, Object... value) {
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
  public QueryOld eq(String table, String column, UUID... value) throws MolgenisException {
    validate(table, column, Column.Type.UUID);
    return eqHelper(table, column, value);
  }

  @Override
  public QueryOld eq(String table, String column, String... value) throws MolgenisException {
    validate(table, column, Column.Type.STRING);
    return eqHelper(table, column, value);
  }

  @Override
  public QueryOld eq(String table, String column, Integer... value) throws MolgenisException {
    validate(table, column, Column.Type.INT);
    return eqHelper(table, column, value);
  }

  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("QUERY(");
    for (Map.Entry<String, From> selectEntry : select.entrySet()) {
      for (String c : selectEntry.getValue().columns.keySet()) {
        Column col = selectEntry.getValue().columns.get(c);
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
      if (select.get(f).toTable != null) {
        builder
            .append(" JOIN(")
            .append(table.fromColumn)
            .append("=")
            .append(table.toTable)
            .append(".")
            .append(table.toColumn)
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
