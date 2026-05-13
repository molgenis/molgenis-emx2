package org.molgenis.emx2;

public class TablePermission {
  private final String table;
  private Boolean select;
  private Boolean insert;
  private Boolean update;
  private Boolean delete;
  private Boolean isRowLevel;

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

  public Boolean isRowLevel() {
    return isRowLevel;
  }

  public boolean hasSelect() {
    return Boolean.TRUE.equals(select);
  }

  public boolean hasInsert() {
    return Boolean.TRUE.equals(insert);
  }

  public boolean hasUpdate() {
    return Boolean.TRUE.equals(update);
  }

  public boolean hasDelete() {
    return Boolean.TRUE.equals(delete);
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

  public TablePermission rowLevel(Boolean isRowLevel) {
    this.isRowLevel = isRowLevel;
    return this;
  }
}
