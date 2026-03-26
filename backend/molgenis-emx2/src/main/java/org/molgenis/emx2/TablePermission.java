package org.molgenis.emx2;

public class TablePermission {
  private final String table;
  private Boolean select;
  private Boolean insert;
  private Boolean update;
  private Boolean delete;

  public TablePermission(String table) {
    this.table = table;
  }

  public String table() {
    return table;
  }

  public Boolean select() {
    return select;
  }

  public Boolean insert() {
    return insert;
  }

  public Boolean update() {
    return update;
  }

  public Boolean delete() {
    return delete;
  }

  public TablePermission select(Boolean select) {
    this.select = select;
    return this;
  }

  public TablePermission insert(Boolean insert) {
    this.insert = insert;
    return this;
  }

  public TablePermission update(Boolean update) {
    this.update = update;
    return this;
  }

  public TablePermission delete(Boolean delete) {
    this.delete = delete;
    return this;
  }
}
