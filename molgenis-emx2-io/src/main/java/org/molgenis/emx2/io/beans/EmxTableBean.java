package org.molgenis.emx2.io.beans;

import org.molgenis.emx2.*;

import java.util.*;

import static org.molgenis.emx2.io.EmxConstants.MOLGENISID;

public class EmxTableBean implements EmxTable {
    String name;
    EmxTableBean extend;
    Map<String, EmxColumnBean> columns = new LinkedHashMap<>();
    List<EmxUniqueBean> uniques = new ArrayList<>();

    public EmxTableBean(String name) {
        this.name = name;
        this.addColumn(MOLGENISID, EmxType.UUID);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<EmxColumn> getColumns() {
        List<EmxColumn> result = new ArrayList<>();
        for(Map.Entry<String,EmxColumnBean> entry: columns.entrySet()) {
            result.add(entry.getValue());
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public EmxColumn getColumn(String name) {
        return columns.get(name);
    }

    @Override
    public List<EmxUnique> getUniques() {
        return Collections.unmodifiableList(uniques);
    }

    public void setUniques(List<EmxUniqueBean> uniques) {
        for (EmxUniqueBean u : uniques) {
            u.setTable(this);
            uniques.add(u);
        }
    }

    @Override
    public EmxTable getExtend() {
        return extend;
    }

    @Override
    public EmxColumn getIdColumn() {
        return getColumn(MOLGENISID);
    }

    @Override
    public void addUnique(List<String> columnNames) throws EmxModelException {
        EmxUniqueBean unique = new EmxUniqueBean(this);
        for(String colName: columnNames)
        {
            //TODO: solve this elsewhere colName = colName.trim();
            if(this.getColumn(colName) == null) throw new EmxModelException("column '"+colName+"' is unknown in table '"+getName()+"'");
            unique.addColumn(getColumn(colName));
        }
        uniques.add(unique);
    }

    public EmxTable setExtend(EmxTableBean extend) {
        this.extend = extend;
        return this;
    }

    public EmxColumnBean addColumn(String name, EmxType type) {
        EmxColumnBean c = new EmxColumnBean(this, name, type);
        columns.put(name, c);
        return c;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EmxTable(name=").append(name);
        if (extend != null) builder.append(", extend=").append(extend.getName());
        if (columns.size() > 0) {
            builder.append(")");
            for (EmxColumnBean c : columns.values()) {
                builder.append("\n\t").append(c.toString());
            }
        }
        if(uniques.size() > 0) {
            for (EmxUniqueBean u : uniques) {
                builder.append("\n\t").append(u.toString());
            }
        }
        builder.append(")");
        return builder.toString();
    }
}