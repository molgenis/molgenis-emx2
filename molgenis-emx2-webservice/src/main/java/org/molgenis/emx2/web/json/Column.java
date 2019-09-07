package org.molgenis.emx2.web.json;

import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.Type;
import org.molgenis.emx2.utils.MolgenisException;

public class Column {

  public String name;
  public Boolean unique = false;
  public Boolean pkey = false;
  public Boolean nullable = false;
  public String refTableName;
  public String refColumnName;
  public Type type;

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
}
