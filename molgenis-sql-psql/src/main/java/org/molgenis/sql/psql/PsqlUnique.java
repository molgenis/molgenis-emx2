package org.molgenis.sql.psql;

import org.molgenis.sql.SqlColumn;
import org.molgenis.sql.SqlTable;
import org.molgenis.sql.SqlUnique;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PsqlUnique implements SqlUnique {
    private SqlTable table;
    private List<SqlColumn> columns;

    PsqlUnique(SqlTable table, List<SqlColumn> columns) {
        this.table = table;
        this.columns = columns;
    }

    @Override
    public SqlTable getTable() {
        return table;
    }

    @Override
    public Collection<SqlColumn> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    @Override
    public Collection<String> getColumnNames() {
        List<String> names = new ArrayList<>();
        for(SqlColumn col: columns) {
            names.add(col.getName());
        }
        return Collections.unmodifiableList(names);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UNIQUE(");
        for(int i = 0; i < columns.size(); i++) {
            if(i>0) builder.append(", ");
            builder.append(columns.get(i).getName());
        }
        builder.append(")");
        return builder.toString();
    }
}
