package org.molgenis.emx2;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public class Permission {

  private boolean isRowLevel;
  private String groupName;
  private String tableId;
  private String tableSchema;
  private List<String> users;

  private boolean hasSelect;
  private boolean hasInsert;
  private boolean hasUpdate;
  private boolean hasDelete;
  private boolean hasAdmin;

  public Permission() {}

  public boolean isRowLevel() {
    return isRowLevel;
  }

  @JsonAlias("isRowLevel")
  public void setRowLevel(boolean rowLevel) {
    isRowLevel = rowLevel;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
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
        + groupName
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

  public List<String> getUsers() {
    return users;
  }

  public void setUsers(List<String> users) {
    this.users = users;
  }

  public String getTableSchema() {
    return tableSchema;
  }

  public void setTableSchema(String tableSchema) {
    this.tableSchema = tableSchema;
  }

  public String getTableId() {
    return tableId;
  }

  public void setTableId(String tableId) {
    this.tableId = tableId;
  }
}
