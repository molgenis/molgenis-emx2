package org.molgenis.sql.psql;

import org.jooq.*;
import org.molgenis.sql.*;

import java.util.*;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.molgenis.sql.psql.PsqlDatabase.MOLGENISID;


public class PsqlQuery implements SqlQuery {

    enum State {
            FROM, SELECT, NONE
    }

    private class From {
        Map<String, SqlColumn> columns = new LinkedHashMap<>();
        SqlTable fromTable;
        String fromColumn;
        String joinTable;
        String joinColumn;
    }

    SqlDatabase db;
    DSLContext sql;
    State state = null;
    String lastFrom = null;
    String lastSelect = null;

    Map<String, From> select = new LinkedHashMap<>();
    Condition conditions = null;

    public PsqlQuery(SqlDatabase db, DSLContext sql) {
        this.db = db;
        this.sql = sql;
    }

    @Override
    public SqlQuery from(String table) {
        if(lastFrom != null) throw new RuntimeException("You can call from() only once");

        SqlTable t = db.getTable(table);
        if(t == null) throw new RuntimeException("fromTable "+table+" does not exist");
        From f = new From();
        f.fromTable = t;

        select.put(table, f);
        state = State.FROM;
        lastFrom = table;
        return this;
    }

    @Override
    public SqlQuery join(String table, String toTable, String on) {
        if(lastFrom == null) throw new RuntimeException("You can call join() only after first calling a from()");

        From tableSelect = new From();

        //add fromTable
        SqlTable fromTable = db.getTable(table);
        if (fromTable == null) {
            throw new RuntimeException("fromTable " + table + " does not exist");
        }
        tableSelect.fromTable = fromTable;

        //add joinTable reference
            From temp = this.select.get(toTable);
            if (temp == null) {
                throw new RuntimeException("Cannot join ('" + table + "','" + toTable + "','" + on + "'): to join fromTable '" + toTable + "' not in getQuery");
            }
        SqlTable joinTable = temp.fromTable;
        tableSelect.joinTable = toTable;

        //add fromColumn and joinColumn
        if(fromTable.getColumn(on) != null) {
            String refTable = fromTable.getColumn(on).getRefTable().getName();
            if(!refTable.equals(joinTable.getName())) {
                throw new RuntimeException("Cannot join ('"+table+"','"+ toTable +"','"+on+"'): select '"+on+"' references wrong from '"+refTable+"'");
            }
            tableSelect.fromColumn = on;
            tableSelect.joinColumn = MOLGENISID;
        } else if(joinTable.getColumn(on) != null) {
            String refTable = joinTable.getColumn(on).getRefTable().getName();
            if(!refTable.equals(fromTable.getName())) {
                throw new RuntimeException("Cannot join ('"+table+"','"+ toTable +"','"+on+"'): select '"+on+"' references wrong from '"+refTable+"'");
            }

            tableSelect.fromColumn = MOLGENISID;
            tableSelect.joinColumn = on;
        } else {
            throw new RuntimeException("Cannot join ('" + table + "','" + toTable + "','" + on + "'): to join select '" + on + "' in neither tables");
        }

        this.select.put(table, tableSelect);
        state = State.FROM;
        lastFrom = table;
        return this;
    }

    @Override
    public SqlQuery select(String column) {
        From f = select.get(lastFrom);
        SqlColumn c = f.fromTable.getColumn(column);
        if(c == null) throw new RuntimeException("select "+column+" does not exist in select fromTable "+lastFrom);
        f.columns.put(column, c);
        state = State.SELECT;
        lastSelect = column;
        return this;
    }

    @Override
    public SqlQuery as(String alias) {
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
                throw new RuntimeException("cannot call as(" + alias + ") at this point");
        }

        state = State.NONE;
        return this;
    }

    //TODO, make streaming?
    @Override
    public List<SqlRow> retrieve() {

        List<SqlRow> rows = new ArrayList<>();

        //get columns
        List<Field> columns = new ArrayList<>();
        Map<String, SqlType> colAliases = new LinkedHashMap<>();
        for(String tableAlias: select.keySet()) {
            for(String columnAlias: select.get(tableAlias).columns.keySet()) {
                SqlColumn col = select.get(tableAlias).columns.get(columnAlias);
                //todo, do we need to put column/field type here?
                //DataType<LocalDate> dateType = SQLDataType.DATE.asConvertedDataType(new LocalDateConverter());
                columns.add(field(name(tableAlias, col.getName())).as(columnAlias));
                colAliases.put(columnAlias, col.getType());
            }
        }

        //create getQuery
        SelectSelectStep step = sql.select(columns);
        SelectJoinStep joinStep = null;
        for(String alias: select.keySet()) {
            From def = select.get(alias);

            if(def.joinTable == null) {
                joinStep = step.from(table(name(def.fromTable.getName())).as(alias));
            }
            else {
                joinStep = joinStep.leftOuterJoin(table(name(def.fromTable.getName())).as(alias)).on(field(name(alias, def.fromColumn)).eq(field(name(def.joinTable, def.joinColumn))));
            }
        }
        joinStep.where(conditions);
        System.out.println("retrieve: "+joinStep.getSQL());

        Result<Record> result = joinStep.fetch();
        for(Record r: result) {
            rows.add(new PsqlRow(r));
        }
        return rows;
    }

    private void validate(String table, String column, SqlType type) {
        if(this.select.get(table) == null) throw new RuntimeException("table/alias '"+table+"' not known. Choose one of "+this.select.keySet());
        if(this.select.get(table).fromTable.getColumn(column) == null) throw new RuntimeException("select '"+column+"' not known in table/alias '"+table+"'");
        if(!type.equals(this.select.get(table).fromTable.getColumn(column).getType())) throw new RuntimeException("select '"+column+"' not of expected type '"+type+"'");
    }

    private SqlQuery eqHelper(String table, String column, Object ... value) {
        if(value.length > 1) {
            if(conditions == null) conditions = field(name(table, column)).in(value);
            else conditions = conditions.and(field(name(table, column)).in(value));
        }
        if(value.length == 1) {
            if(conditions == null) conditions = field(name(table, column)).eq(value[0]);
            else conditions = conditions.and(field(name(table, column)).eq(value[0]));
        }
        return this;
    }

    @Override
    public SqlQuery eq(String table, String column, UUID ... value) {
        validate(table,column, SqlType.UUID);
        return eqHelper(table, column, value);
    }

    @Override
    public SqlQuery eq(String table, String column, String ... value) {
        validate(table,column, SqlType.STRING);
        return eqHelper(table, column, value);
    }

    @Override
    public SqlQuery eq(String table, String column, Integer... value) {
        validate(table,column, SqlType.INT);
        return eqHelper(table, column, value);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("QUERY(");
        for(String s: select.keySet()) {
            for(String c: select.get(s).columns.keySet()) {
                SqlColumn col = select.get(s).columns.get(c);
                builder.append("\n\tSELECT(").append(s).append(".").append(col.getName()).append(")");
                if (!c.equals(col.getName())) {
                    builder.append(" AS '").append(c).append("'");
                }
            }
        }
        for(String f: select.keySet()) {
            From table = select.get(f);
            String name = table.fromTable.getName();
            builder.append("\n\tFROM(").append(name).append(")");
            if(select.get(f).joinTable != null) {
                builder.append(" JOIN(").append(table.fromColumn).append("=").append(table.joinTable).append(".").append(table.joinColumn).append(")");
            }
            if(!f.equals(name)) {
                builder.append(" AS '").append(f).append("'");
            }
        }
        builder.append("\n);");

        return builder.toString();
    }
}