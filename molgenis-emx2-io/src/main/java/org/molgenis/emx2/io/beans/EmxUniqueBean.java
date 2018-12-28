package org.molgenis.emx2.io.beans;

import org.molgenis.emx2.EmxColumn;
import org.molgenis.emx2.EmxTable;
import org.molgenis.emx2.EmxUnique;

import java.util.ArrayList;
import java.util.List;

public class EmxUniqueBean implements EmxUnique {
    EmxTable table;
    List<EmxColumn> columns = new ArrayList<>();

    public EmxUniqueBean(EmxTable forTable) {
        this.table = forTable;
    }

    public EmxTable getTable() {
        return table;
    }

    public void setTable(EmxTable table) {
        this.table = table;
    }

    public EmxUniqueBean addColumn(EmxColumn c) {
        this.columns.add(c);
        return this;
    }

    @Override
    public List<EmxColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<EmxColumn> columns) {
        this.columns = columns;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EmxUnique(");
        for(EmxColumn c: columns) {
            builder.append("\n\t\t").append(c.toString());
        }
        builder.append(")");
        return builder.toString();
    }
}
