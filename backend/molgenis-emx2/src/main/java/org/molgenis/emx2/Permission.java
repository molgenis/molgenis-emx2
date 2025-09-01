package org.molgenis.emx2;

public class Permission {

  private boolean isRowLevel;
  private String group;

  private boolean hasSelect;
  private boolean hasInsert;
  private boolean hasUpdate;
  private boolean hasDelete;
  private boolean hasAdmin;

  public Permission(
      boolean isRowLevel,
      String group,
      boolean hasSelect,
      boolean hasInsert,
      boolean hasUpdate,
      boolean hasDelete,
      boolean hasAdmin) {
    this.isRowLevel = isRowLevel;
    this.group = group;
    this.hasSelect = hasSelect;
    this.hasInsert = hasInsert;
    this.hasUpdate = hasUpdate;
    this.hasDelete = hasDelete;
    this.hasAdmin = hasAdmin;
  }

  public Permission() {}

  public boolean isRowLevel() {
    return isRowLevel;
  }

  public void setRowLevel(boolean rowLevel) {
    isRowLevel = rowLevel;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public boolean hasSelect() {
    return hasSelect;
  }

  public void setHasSelect(boolean hasSelect) {
    this.hasSelect = hasSelect;
  }

  public boolean hasInsert() {
    return hasInsert;
  }

  public void setHasInsert(boolean hasInsert) {
    this.hasInsert = hasInsert;
  }

  public boolean hasUpdate() {
    return hasUpdate;
  }

  public void setHasUpdate(boolean hasUpdate) {
    this.hasUpdate = hasUpdate;
  }

  public boolean hasDelete() {
    return hasDelete;
  }

  public void setHasDelete(boolean hasDelete) {
    this.hasDelete = hasDelete;
  }

  public boolean hasAdmin() {
    return hasAdmin;
  }

  public void setHasAdmin(boolean hasAdmin) {
    this.hasAdmin = hasAdmin;
  }

  @Override
  public String toString() {
    return "Permission{"
        + "isRowLevel="
        + isRowLevel
        + ", group='"
        + group
        + '\''
        + ", hasSelect="
        + hasSelect
        + ", hasInsert="
        + hasInsert
        + ", hasUpdate="
        + hasUpdate
        + ", hasDelete="
        + hasDelete
        + ", hasAdmin="
        + hasAdmin
        + '}';
  }
}
