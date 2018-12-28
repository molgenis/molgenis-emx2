package org.molgenis.sql.psql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.SQLDataType;
import org.molgenis.sql.*;

import java.util.*;

import static org.jooq.impl.DSL.*;
import static org.molgenis.sql.SqlType.REF;
import static org.molgenis.sql.psql.PsqlDatabase.MOLGENISID;

public class PsqlTable implements SqlTable {

    private DSLContext sql;
    private PsqlDatabase db;
    private String name;
    private Map<String, PsqlColumn> columnMap = new LinkedHashMap<>();
    private Map<String, SqlUnique> uniquesMap = new LinkedHashMap<>();

    PsqlTable(PsqlDatabase db, DSLContext sql, String name) {
        this.db = db;
        this.sql = sql;
        this.name = name;
        reloadMetaData();
    }

    private void reloadMetaData() {
        reloadColumns();
        reloadUniques();
    }

    private void reloadColumns() {
        columnMap = new LinkedHashMap<>();
        Table t = getTable();
        for (Field f : t.fields()) {
            columnMap.put(f.getName(), new PsqlColumn(sql, this, f));
        }
        reloadReferences();
    }

    private void reloadReferences() {
        Table t = getTable();
        for (Object o3 : t.getReferences()) {
            ForeignKey fk = (ForeignKey) o3;
            for (Field f : (List<Field>) fk.getFields()) {
                PsqlColumn temp = columnMap.get(f.getName());
                PsqlColumn fkey = null;
                String refTableName = fk.getKey().getTable().getName();
                if (refTableName.equals(name)) {
                    fkey = new PsqlColumn(sql, this, f.getName(), REF, this);
                } else {
                    fkey = new PsqlColumn(sql, this, f.getName(), REF, db.getTable(refTableName));
                }
                fkey.setNullable(temp.isNullable());
                columnMap.put(f.getName(), fkey);
            }
        }
    }

    @Override
    public Collection<SqlColumn> getColumns() {
        Collection<SqlColumn> cols = new ArrayList<>();
        cols.addAll(columnMap.values());
        return Collections.unmodifiableCollection(cols);
    }

    @Override
    public SqlColumn getColumn(String name) {
        return columnMap.get(name);
    }

    @Override
    public SqlColumn addColumn(String name, SqlType sqlType) {
        DataType type = PsqlTypeUtils.typeOf(sqlType);
        sql.alterTable(name(this.name)).addColumn(name, type.nullable(false)).execute();
        reloadColumns();
        return getColumn(name);
    }

    @Override
    public SqlColumn addColumn(String name, SqlTable otherTable) {
        sql.alterTable(name(this.name)).addColumn(name(name), SQLDataType.UUID.nullable(false)).execute();
        sql.alterTable(name(this.name)).add(
                constraint().foreignKey(name(name))
                        .references(name(otherTable.getName()), name(MOLGENISID))).execute();
        reloadColumns();
        return getColumn(name);
    }

    @Override
    public void removeColumn(String name) throws SqlDatabaseException {
        sql.alterTable(this.name).renameColumn(name);
        reloadColumns();
    }


    private void reloadUniques() {
        uniquesMap = new LinkedHashMap<>();
        Table t = getTable();
        for (Index i : (List<Index>) t.getIndexes()) {
            List<SqlColumn> cols = new ArrayList<>();
            for (SortField sf : i.getFields()) {
                cols.add(getColumn(sf.getName()));
            }
            uniquesMap.put(i.getName(), new PsqlUnique(this, cols));
        }
    }

    @Override
    public Collection<SqlUnique> getUniques() {
        return Collections.unmodifiableCollection(uniquesMap.values());
    }

    private String getUniqueName(String ... keys) throws SqlDatabaseException {
        List<String> keyList = Arrays.asList(keys);
        for(Map.Entry<String,SqlUnique> el: this.uniquesMap.entrySet()) {
            if(el.getValue().getColumns().size() == keyList.size() && el.getValue().getColumnNames().containsAll(keyList)) {
                return el.getKey();
            }
        }
        throw new SqlDatabaseException("getUniqueName("+keyList+") failed: constraint unknown in table "+this.name);
    }

    public SqlUnique addUnique(String... keys) throws SqlDatabaseException {
        List<SqlColumn> uniqueColumns = new ArrayList<>();
        for (String key : keys) {
            SqlColumn col = getColumn(key);
            if (col == null) throw new SqlDatabaseException("addUnique("+keys+") failed: column '" + key + "' unknown in table "+this.name);
            uniqueColumns.add(col);
        }
        sql.alterTable(name).add(constraint().unique(keys)).execute();
        reloadUniques();
        return this.uniquesMap.get(getUniqueName(keys));
    }

    @Override
    public void removeUnique(String ... keys) throws SqlDatabaseException {
        sql.alterTable(this.name).dropConstraint(getUniqueName(keys));
        reloadUniques();
    }

    private Table<?> getTable() {
        return sql.meta().getTables(this.name).get(0);
    }

    public String getName() {
        return name;
    }

    @Override
    public void insert(Collection<SqlRow> rows) throws SqlDatabaseException {
        try {
            Table t = getTable();
            Field[] fields = t.fields();
            String[] fieldNames = new String[fields.length];
            for (int i = 0; i < fields.length; i++) fieldNames[i] = fields[i].getName();
            InsertValuesStepN step = sql.insertInto(t, fields);
            for (SqlRow row : rows) {
                step.values(row.values(fieldNames));
            }
            step.execute();
        } catch(DataAccessException e) {
            throw new SqlDatabaseException(e.getCause().getMessage());
        }
    }

    @Override
    public void insert(SqlRow row) throws SqlDatabaseException {
        this.insert(Arrays.asList(row));
    }


    @Override
    public void update(Collection<SqlRow> rows) throws SqlDatabaseException {
        try {
            Table t = getTable();
            Field[] fields = t.fields();
            String[] fieldNames = new String[fields.length];
            for (int i = 0; i < fields.length; i++) fieldNames[i] = fields[i].getName();
            //create multi-value insert
            InsertValuesStepN step = sql.insertInto(t, fields);
            for (SqlRow row : rows) {
                step.values(row.values(fieldNames));
            }
            //on duplicate key update using same record via "excluded" keyword in postgres
            InsertOnDuplicateSetStep step2 = step.onConflict(t.field(MOLGENISID)).doUpdate();
            for(int i = 0; i < fieldNames.length; i++) {
                if(!MOLGENISID.equals(fieldNames[i])) {
                    step2.set(field(fieldNames[i]), (Object) field(unquotedName("\"excluded\"."+fieldNames[i])));
                }
            }
            step.execute();
        } catch(DataAccessException e) {
            throw new SqlDatabaseException(e.getCause().getMessage());
        }

    }

    @Override
    public void update(SqlRow row) throws SqlDatabaseException {
        this.update(Arrays.asList(row));
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
    public void delete(SqlRow row) {
        this.delete(Arrays.asList(row));
    }


    @Override
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