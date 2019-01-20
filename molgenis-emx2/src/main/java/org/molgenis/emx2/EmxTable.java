package org.molgenis.emx2;

import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.TypeName;

import java.util.*;

import static org.molgenis.emx2.EmxConstants.MOLGENISID;
import static org.molgenis.emx2.EmxType.MREF;
import static org.molgenis.emx2.EmxType.REF;

@TypeName("table")
public class EmxTable {
  @Id private String name;
  private EmxModel model;
  private EmxTable extend;
  private Map<String, EmxColumn> columns = new LinkedHashMap<>();
  private List<EmxUnique> uniques = new ArrayList<>();

  public EmxTable(EmxModel model, String name) {
    this.model = model;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Collection<EmxColumn> getColumns() {
    List<EmxColumn> result = new ArrayList<>();
    for (Map.Entry<String, EmxColumn> entry : columns.entrySet()) {
      result.add(entry.getValue());
    }
    return Collections.unmodifiableList(result);
  }

  public EmxColumn getColumn(String name) {
    return columns.get(name);
  }

  public List<EmxUnique> getUniques() {
    return Collections.unmodifiableList(uniques);
  }

  public void setUniques(List<EmxUnique> uniques) throws EmxException {
    for (EmxUnique u : uniques) {
      u.setTable(this);
      uniques.add(u);
    }
    model.onTableChange(this);
  }

  public EmxTable getExtend() {
    return extend;
  }

  public EmxColumn getIdColumn() {
    return getColumn(MOLGENISID);
  }

  public void addUnique(Collection<String> columnNames) throws EmxException {
    EmxUnique unique = new EmxUnique(this);
    for (String colName : columnNames) {
      if (this.getColumn(colName) == null)
        throw new EmxException("column '" + colName + "' is unknown in table '" + getName() + "'");
      unique.addColumn(getColumn(colName));
    }
    uniques.add(unique);
    model.onTableChange(this);
  }

  public EmxTable setExtend(EmxTable extend) throws EmxException {
    this.extend = extend;
    model.onTableChange(this);
    return this;
  }

  public EmxColumn addColumn(String name, EmxType type) throws EmxException {
    if (MREF.equals(type))
      throw new EmxException("addColumn(MREF) not allowed, use addMref() instead");
    EmxColumn c = new EmxColumn(model, this, name, type);
    columns.put(name, c);
    model.onColumnChange(c);
    return c;
  }

  public EmxColumn addRef(String name, EmxTable otherTable) throws EmxException {
    EmxColumn c = new EmxColumn(model, this, name, REF);
    c.setRef(otherTable);
    columns.put(name, c);
    model.onColumnChange(c);
    return c;
  }

  public EmxColumn addMref(String columnName, EmxTable otherTable, String joinTableName)
      throws EmxException {
    // check if joinTable exists and is of right type
    EmxTable joinTable = model.createTable(joinTableName);
    joinTable.addRef(this.getName(), this);
    joinTable.addRef(otherTable.getName(), otherTable);
    joinTable.addUnique(Arrays.asList(this.getName(), otherTable.getName()));
    // create column with the joinTable behind it
    EmxColumn c = new EmxColumn(model, this, columnName, MREF);
    c.setRef(otherTable);
    c.setJoinTable(joinTable.getName());
    columns.put(columnName, c);
    model.onColumnChange(c);
    return c;
  }

  public void removeColumn(String name) throws EmxException {
    model.onColumnRemove(getColumn(name));
    columns.remove(name);
  }

  public String toString() {
    return getName();
  }

  public String print() {
    StringBuilder builder = new StringBuilder();
    builder.append("EmxTable(name=").append(name);
    if (extend != null) builder.append(", extend=").append(extend.getName());
    if (!columns.isEmpty()) {
      for (EmxColumn c : columns.values()) {
        builder.append("\n\t").append(c.print());
      }
    }
    if (!uniques.isEmpty()) {
      for (EmxUnique u : uniques) {
        builder.append("\n\t").append(u.print());
      }
    }
    builder.append(")");
    return builder.toString();
  }

  public void addUnique(String... keys) throws EmxException {
    if (keys.length > 0) this.addUnique(Arrays.asList(keys));
  }
}
