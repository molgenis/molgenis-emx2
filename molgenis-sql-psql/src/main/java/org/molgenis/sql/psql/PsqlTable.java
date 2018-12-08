package org.molgenis.sql.psql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.SQLDataType;
import org.molgenis.sql.*;

import java.util.*;

import static org.jooq.impl.DSL.*;
import static org.molgenis.sql.psql.PsqlDatabase.MOLGENISID;

public class PsqlTable implements SqlTable {

    private PsqlDatabase db;
    private String name;
    private DSLContext sql;
    private Map<String, PsqlColumn> columns = new LinkedHashMap<>();
    private List<SqlUnique> uniques = new ArrayList<>();

    PsqlTable(PsqlDatabase db, String name) {
        this.db = db;
        this.sql = db.getDslContext();
        this.name = name;
    }

    @Override
    public SqlColumn addColumn(String name, SqlTable otherTable) {
        sql.alterTable(name(this.name)).addColumn(name(name), SQLDataType.UUID.nullable(false)).execute();
        sql.alterTable(name(this.name)).add(
                constraint().foreignKey(name(name))
                        .references(name(otherTable.getName()), name(MOLGENISID))).execute();

        PsqlColumn c = new PsqlColumn(sql, this, name, otherTable);
        columns.put(name, c);
        return c;

    }

    @Override
    public SqlColumn addColumn(String name, SqlType sqlType) {
        DataType type = TypeUtils.typeOf(sqlType);
        sql.alterTable(this.name).addColumn(name, type.nullable(false)).execute();
        PsqlColumn c = new PsqlColumn(sql, this, name, sqlType);
        columns.put(name, c);
        return c;
    }

    public SqlUnique addUnique(String... keys) throws SqlDatabaseException {
        List<SqlColumn> uniqueColumns = new ArrayList<>();

        for (String key : keys) {
            SqlColumn col = getColumn(key);
            if (col == null) throw new SqlDatabaseException("addUnique failed: select '" + key + "' uknown");
            uniqueColumns.add(col);
        }
        sql.alterTable(name).add(constraint().unique(keys)).execute();
        SqlUnique unique = new PsqlUnique(this, uniqueColumns);
        uniques.add(unique);

        return unique;
    }

    public void reloadMetaData() {
        List<Table<?>> tables = sql.meta().getTables();
        columns = new LinkedHashMap<>();
        uniques = new ArrayList<>();
        for (Table t : tables) {
            if (t.getName().equals(name)) {
                reloadColumns(t);
                reloadReferences(t);
                reloadIndexes(t);
            }
        }
    }

    private void reloadColumns(Table t) {
        for (Field f : t.fields()) {
            columns.put(f.getName(), new PsqlColumn(sql, this, f));
        }
    }

    private void reloadReferences(Table t) {
        for (Object o3 : t.getReferences()) {
            ForeignKey fk = (ForeignKey) o3;
            for (Field f : (List<Field>) fk.getFields()) {
                PsqlColumn temp = columns.get(f.getName());
                PsqlColumn fkey = null;
                //check for cyclic dependency
                String refTableName = fk.getKey().getTable().getName();
                if (refTableName.equals(name)) {
                    fkey = new PsqlColumn(sql, this, f.getName(), this);
                } else {
                    fkey = new PsqlColumn(sql, this, f.getName(), db.getTable(refTableName));
                }
                fkey.setNullable(temp.isNullable());
                columns.put(f.getName(), fkey);
            }
        }
    }

    private void reloadIndexes(Table t) {
        for (Index i : (List<Index>) t.getIndexes()) {
            List<SqlColumn> cols = new ArrayList<>();
            for (SortField sf : i.getFields()) {
                cols.add(getColumn(sf.getName()));
            }
            uniques.add(new PsqlUnique(this, cols));
        }
    }

    @Override
    public Collection<SqlUnique> getUniques() {
        return Collections.unmodifiableCollection(uniques);
    }

    public String getName() {
        return name;
    }

    @Override
    public SqlColumn getColumn(String name) {
        return columns.get(name);
    }


    public Collection<SqlColumn> getColumns() {
        Collection<SqlColumn> cols = new ArrayList<>();
        cols.addAll(columns.values());
        return Collections.unmodifiableCollection(cols);
    }

    public SqlRow createRow() {
        return new PsqlRow(UUID.randomUUID());
    }

    @Override
    public void delete(Collection<SqlRow> rows) {
        Table t = sql.meta().getTables(name).get(0);
        BatchBindStep step = sql.batch(deleteFrom(t).where(field(MOLGENISID, SQLDataType.UUID).eq((UUID) null)));
        for (SqlRow row : rows) {
            step.bind(row.getRowID());
        }
        step.execute();
    }

    @Override
    public void insert(SqlRow row) throws SqlDatabaseException {
        this.insert(Arrays.asList(row));
    }

    @Override
    public void delete(SqlRow row) {
        this.delete(Arrays.asList(row));
    }

    @Override
    public void insert(Collection<SqlRow> rows) throws SqlDatabaseException {
        try {
            Table t = sql.meta().getTables(name).get(0);
            Field[] fields = t.fields();
            String[] fieldNames = new String[fields.length];
            for (int i = 0; i < fields.length; i++) fieldNames[i] = fields[i].getName();

            InsertValuesStepN step = sql.insertInto(t, fields);
            for (SqlRow row : rows) {
                validate(row);
                step.values(row.values(fieldNames));
            }
            step.execute();
        } catch(DataAccessException e) {
            throw new SqlDatabaseException(e.getCause().getMessage());
        }
    }

    @Override
    public void validate(SqlRow row) {
        //BIG TODO
//        for(SqlColumn c: getColumns()) {
//            Object val = row.get(c.getName());
//            if(val == null && !c.isNullable()) throw new RuntimeException("Column "+c.getName()+" is not nullable: "+row);
//            if(val != null) switch(c.getType()) {
//                case STRING:
//                    if(val instanceof String) break;
//                    else throw new RuntimeException(c.getName() + " must be of type String: "+row);
//                case INT:
//                    if(val instanceof Integer) break;
//                    else throw new RuntimeException(c.getName() + " must be of type Integer: "+row);
//            }
//        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TABLE(").append(name).append("){");
        for (SqlColumn c : getColumns()) {
            builder.append("\n\t").append(c.toString());
        }
        for (SqlUnique u : getUniques()) {
            builder.append("\n\t").append(u.toString());
        }
        builder.append("\n}");
        return builder.toString();
    }
}