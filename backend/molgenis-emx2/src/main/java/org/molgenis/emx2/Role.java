package org.molgenis.emx2;

import java.time.OffsetDateTime;
import java.util.List;

public class Role {
  private String roleName;
  private String schemaName = "*";
  private String description;
  private boolean immutable = false;
  private String status = "active";
  private String createdBy;
  private OffsetDateTime createdOn;
  private OffsetDateTime deletedOn;
  private List<TablePermission> permissions = List.of();

  public Role() {}

  public Role(String roleName) {
    this.roleName = roleName;
  }

  public String getRoleName() {
    return roleName;
  }

  public Role setRoleName(String roleName) {
    this.roleName = roleName;
    return this;
  }

  public String getSchemaName() {
    return schemaName;
  }

  public Role setSchemaName(String schemaName) {
    this.schemaName = schemaName;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Role setDescription(String description) {
    this.description = description;
    return this;
  }

  public boolean isImmutable() {
    return immutable;
  }

  public Role setImmutable(boolean immutable) {
    this.immutable = immutable;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public Role setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public Role setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  public OffsetDateTime getCreatedOn() {
    return createdOn;
  }

  public Role setCreatedOn(OffsetDateTime createdOn) {
    this.createdOn = createdOn;
    return this;
  }

  public OffsetDateTime getDeletedOn() {
    return deletedOn;
  }

  public Role setDeletedOn(OffsetDateTime deletedOn) {
    this.deletedOn = deletedOn;
    return this;
  }

  public List<TablePermission> getPermissions() {
    return permissions;
  }

  public Role withPermissions(List<TablePermission> permissions) {
    this.permissions = permissions != null ? permissions : List.of();
    return this;
  }

  public String name() {
    return roleName;
  }

  public boolean isSystemRole() {
    return immutable;
  }

  public List<TablePermission> permissions() {
    return permissions;
  }
}
