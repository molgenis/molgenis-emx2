package org.molgenis.emx2;

import org.javers.core.metamodel.annotation.Id;
import org.javers.core.metamodel.annotation.TypeName;

import java.util.*;

import static org.molgenis.emx2.EmxConstants.MOLGENISID;

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
    this.addColumn(MOLGENISID, EmxType.UUID);
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

  public void setUniques(List<EmxUnique> uniques) {
    for (EmxUnique u : uniques) {
      u.setTable(this);
      uniques.add(u);
    }
  }

  public EmxTable getExtend() {
    return extend;
  }

  public EmxColumn getIdColumn() {
    return getColumn(MOLGENISID);
  }

  public void addUnique(List<String> columnNames) throws EmxException {
    EmxUnique unique = new EmxUnique(this);
    for (String colName : columnNames) {
      if (this.getColumn(colName) == null)
        throw new EmxException("column '" + colName + "' is unknown in table '" + getName() + "'");
      unique.addColumn(getColumn(colName));
    }
    uniques.add(unique);
  }

  public EmxTable setExtend(EmxTable extend) {
    this.extend = extend;
    return this;
  }

  public EmxColumn addColumn(String name, EmxType type) {
    EmxColumn c = new EmxColumn(this, name, type);
    columns.put(name, c);
    return c;
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
}
