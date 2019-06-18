package org.molgenis.sql;

import org.jooq.*;
import org.jooq.impl.SQLDataType;
import org.molgenis.Column;
import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.Unique;
import org.molgenis.beans.TableBean;

import java.util.*;

import static org.jooq.impl.DSL.*;

import static org.molgenis.sql.RowImpl.MOLGENISID;
import static org.molgenis.Column.Type.MREF;

class SqlTable extends TableBean {
  private DSLContext sql;
  private Schema schema;
  private boolean isLoading = false;

  SqlTable(Schema schema, DSLContext sql, String name) throws MolgenisException {
    super(name);
    this.schema = schema;
    this.sql = sql;
    loadMetadata();
  }

  private void loadMetadata() throws MolgenisException {
    isLoading = true;
    org.jooq.Table t = sql.meta().getTables(getName()).get(0);
    loadColumns(t);
    loadUniques(t);
    isLoading = false;
  }

  private void loadColumns(org.jooq.Table t) throws MolgenisException {
    // get all foreign keys
    Map<String, org.molgenis.Table> refs = new LinkedHashMap<>();
    for (Object o3 : t.getReferences()) {
      ForeignKey fk = (ForeignKey) o3;
      for (Field field : (List<Field>) fk.getFields()) {
        Column temp = getColumn(field.getName());
        String refTableName = fk.getKey().getTable().getName();
        if (refTableName.equals(getName())) {
          refs.put(field.getName(), this);
        } else {
          refs.put(field.getName(), schema.getTable(refTableName));
        }
      }
    }

    // get
    for (Field field : t.fields()) {
      String name = field.getName();
      org.molgenis.Table ref = refs.get(name);
      if (ref != null) addRef(name, refs.get(field.getName()));
      else addColumn(name, SqlTypeUtils.getSqlType(field));
    }
    // TODO: null constraints
    // TODO: settings that are not in schema
  }

  private void loadUniques(org.jooq.Table t) throws MolgenisException {
    for (Index i : (List<Index>) t.getIndexes()) {
      if (i.getUnique()) {
        List<String> cols = new ArrayList<>();
        for (SortField sf : i.getFields()) {
          cols.add(sf.getName());
        }
        addUnique(cols.toArray(new String[cols.size()]));
      }
    }
  }

  private void reloadMrefs(org.jooq.Table t) {
    // TODO mrefs
  }

  @Override
  public SqlColumn addColumn(String name, SqlColumn.Type type) throws MolgenisException {
    if (!isLoading) {
      DataType jooqType = SqlTypeUtils.typeOf(type);
      Field field = field(name(name), jooqType.nullable(false));
      sql.alterTable(name(getName())).addColumn(field).execute();
    }
    SqlColumn c = new SqlColumn(sql, this, name, type);
    columns.put(name, c);
    return c;
  }

  @Override
  public SqlColumn addRef(String name, org.molgenis.Table otherTable) throws MolgenisException {
    if (!isLoading) {
      Field field = field(name(name), SQLDataType.UUID.nullable(false));
      sql.alterTable(name(getName())).addColumn(field).execute();
      sql.alterTable(name(getName()))
          .add(
              constraint(name(getName()) + "_" + name(name) + "_FK")
                  .foreignKey(name(name))
                  .references(name(otherTable.getName()), name(MOLGENISID)))
          .execute();
      sql.createIndex(name(getName()) + "_" + name(name) + "_FKINDEX")
          .on(table(name(getName())), field)
          .execute();
    }
    SqlColumn c = new SqlColumn(sql, this, name, otherTable);
    columns.put(name, c);
    return c;
  }

  @Override
  public SqlColumn addMref(String name, org.molgenis.Table otherTable, String otherColumn)
      throws MolgenisException {
    if (!isLoading) {
      Column check = getColumn(name);
      if (check != null && MREF.equals(check.getType()) && otherTable.equals(check.getRefTable())) {
        // todo check
      } else {
        String joinTable = this.getName() + name + "MREF" + otherTable.getName() + otherColumn;
        org.molgenis.Table jTable = schema.createTable(joinTable);
        jTable.addRef(otherColumn, this);
        jTable.addRef(name, otherTable);
        // add reverse link
        // otherTable.addMref(otherColumn, this, name);
      }
    }
    SqlColumn c = new SqlColumn(sql, this, name, otherTable, otherColumn);
    columns.put(name, c);
    return c;
  }

  @Override
  public void removeColumn(String name) throws MolgenisException {
    if (MOLGENISID.equals(name))
      throw new MolgenisException("You are not allowed to remove primary key column " + MOLGENISID);
    sql.alterTable(name(getName())).dropColumn(field(name(name))).execute();
    super.removeColumn(name);
  }

  public Unique addUnique(String... keys) throws MolgenisException {
    if (!isLoading) {
      String uniqueName = getName() + "_" + String.join("_", keys) + "_UNIQUE";
      sql.alterTable(name(getName())).add(constraint(name(uniqueName)).unique(keys)).execute();
    }
    return super.addUnique(keys);
  }

  @Override
  public boolean isUnique(String... keys) {
    try {
      getUniqueName(keys);
      return true;
    } catch (MolgenisException e) {
      return false;
    }
  }

  @Override
  public void removeUnique(String... keys) throws MolgenisException {
    if (keys.length == 1 && MOLGENISID.equals(keys[0]))
      throw new MolgenisException(
          "You are not allowed to remove unique constraint on primary key column " + MOLGENISID);
    String uniqueName = getUniqueName(keys);
    sql.alterTable(name(getName())).dropConstraint(name(uniqueName)).execute();
    super.removeUnique(keys);
  }
}
