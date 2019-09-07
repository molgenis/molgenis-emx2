package org.molgenis.emx2.web.json;

import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.Type;
import org.molgenis.emx2.utils.MolgenisException;

public class Column {

  private String name;
  private Boolean unique = false;
  private Boolean pkey = false;
  private Boolean nullable = false;
  private String refTableName;
  private String refColumnName;
  private Type type;

  public Column() {}

  public Column(org.molgenis.emx2.Column column) {
    this.name = column.getColumnName();
    this.pkey = column.isPrimaryKey();
    this.unique = column.isUnique();
    this.type = column.getType();
    this.refTableName = column.getRefTableName();
    this.refColumnName = column.getRefColumnName();
    this.nullable = column.getNullable();
  }

  public org.molgenis.emx2.Column getColumnMetadata(TableMetadata tm) throws MolgenisException {
    org.molgenis.emx2.Column c = new org.molgenis.emx2.Column(tm, name, type);
    c.setPrimaryKey(pkey);
    c.setUnique(unique);
    c.setNullable(nullable);
    c.setReference(refTableName, refColumnName);
    return c;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean getUnique() {
    return unique;
  }

  public void setUnique(Boolean unique) {
    this.unique = unique;
  }

  public Boolean getPkey() {
    return pkey;
  }

  public void setPkey(Boolean pkey) {
    this.pkey = pkey;
  }

  public Boolean getNullable() {
    return nullable;
  }

  public void setNullable(Boolean nullable) {
    this.nullable = nullable;
  }

  public String getRefTableName() {
    return refTableName;
  }

  public void setRefTableName(String refTableName) {
    this.refTableName = refTableName;
  }

  public String getRefColumnName() {
    return refColumnName;
  }

  public void setRefColumnName(String refColumnName) {
    this.refColumnName = refColumnName;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }
}
