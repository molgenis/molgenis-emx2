package org.molgenis.emx2.sql;

import static java.lang.Boolean.TRUE;
import static org.molgenis.emx2.Privileges.MANAGER;
import static org.molgenis.emx2.sql.ChangeLogExecutor.executeGetChanges;
import static org.molgenis.emx2.sql.ChangeLogExecutor.executeGetChangesCount;
import static org.molgenis.emx2.sql.SqlColumnExecutor.getOntologyTableDefinition;
import static org.molgenis.emx2.sql.SqlDatabase.*;
import static org.molgenis.emx2.sql.SqlSchemaMetadataExecutor.executeGetMembers;
import static org.molgenis.emx2.sql.SqlSchemaMetadataExecutor.executeGetRoles;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.executeCreateTable;

import java.util.*;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlSchemaMetadata extends SchemaMetadata {
  private static Logger logger = LoggerFactory.getLogger(SqlSchemaMetadata.class);
  // cache for retrieved roles
  private List<String> rolesCache = null;

  // copy constructor
  protected SqlSchemaMetadata(Database db, SqlSchemaMetadata copy) {
    this.name = copy.getName();
    this.description = copy.getDescription();
    this.database = db;
    this.sync(copy);
  }

  public synchronized void sync(SqlSchemaMetadata from) {
    if (from != this) {
      // database is excluded from sync

      // remove tables not available anymore
      Set<String> remove =
          this.tables.keySet().stream()
              .filter(t -> !from.tables.containsKey(t))
              .collect(Collectors.toSet());
      remove.forEach(t -> this.tables.remove(t));

      // sync tables metadata
      from.tables
          .entrySet()
          .forEach(
              e -> {
                if (this.tables.containsKey(e.getKey())) {
                  this.tables.get(e.getKey()).sync(e.getValue());
                } else {
                  this.tables.put(e.getKey(), new SqlTableMetadata(this, e.getValue()));
                }
              });

      // sync settings
      this.setSettingsWithoutReload(from.getSettings());
    }
  }

  public SqlSchemaMetadata(Database db, String name, String description) {
    super(
        db,
        MetadataUtils.loadSchemaMetadata(((SqlDatabase) db).getJooq(), new SchemaMetadata(name)));
    this.reload();
    this.setDescription(description);
  }

  public SqlSchemaMetadata(Database db, String name) {
    super(
        db,
        MetadataUtils.loadSchemaMetadata(((SqlDatabase) db).getJooq(), new SchemaMetadata(name)));
    this.reload();
  }

  public void reload() {
    if (logger.isInfoEnabled()) {
      logger.info("loading schema '{}' as user {}", getName(), getDatabase().getActiveUser());
    }
    long start = System.currentTimeMillis();
    MetadataUtils.loadSchemaMetadata(getDatabase().getJooq(), this);
    this.tables.clear();
    this.rolesCache = null;
    for (TableMetadata table : MetadataUtils.loadTables(getDatabase().getJooq(), this)) {
      super.create(new SqlTableMetadata(this, table));
    }
    if (logger.isInfoEnabled()) {
      logger.info(
          "loading schema '{}' complete in {}ms", getName(), System.currentTimeMillis() - start);
    }
  }

  public boolean exists() {
    return MetadataUtils.schemaExists(getDatabase().getJooq(), this.getName());
  }

  @Override
  public SqlTableMetadata create(TableMetadata table) {
    // delete to batch creator
    this.create(new TableMetadata[] {table});
    return getTableMetadata(table.getTableName());
  }

  @Override
  public SqlTableMetadata getTableMetadata(String name) {
    return (SqlTableMetadata) super.getTableMetadata(name);
  }

  @Override
  public SchemaMetadata create(TableMetadata... tables) {
    getDatabase()
        .tx(
            database -> {
              SqlSchema s = (SqlSchema) database.getSchema(getName());
              SqlSchemaMetadata sm = s.getMetadata();
              if (tables.length > 1) {
                // make use of dependency sorting etc
                s.migrate(new SchemaMetadata().create(tables));
              } else {
                TableMetadata table = tables[0];
                List<TableMetadata> tableList = new ArrayList<>();
                tableList.addAll(List.of(tables));
                validateTableIdentifierIsUnique(sm, table);
                SqlTableMetadata result = null;
                if (TableType.ONTOLOGIES.equals(table.getTableType())) {
                  result =
                      new SqlTableMetadata(
                          sm,
                          getOntologyTableDefinition(
                              table.getTableName(), table.getLabels(), table.getDescriptions()));
                } else {
                  result = new SqlTableMetadata(sm, table);
                }
                sm.tables.put(table.getTableName(), result);
                executeCreateTable(((SqlDatabase) database).getJooq(), result);
              }
              sync(sm);
            });
    getDatabase().getListener().schemaChanged(getName());
    return this;
  }

  static void validateTableIdentifierIsUnique(SqlSchemaMetadata sm, TableMetadata table) {
    for (TableMetadata existingTable : sm.getTables()) {
      if (!existingTable.getTableName().equals(table.getTableName())
          && existingTable.getIdentifier().equals(table.getIdentifier())) {
        throw new MolgenisException(
            String.format(
                "Cannot create/alter because name resolves to same identifier: '%s' has same identifier as '%s' (both resolve to identifier '%s')",
                table.getTableName(), existingTable.getTableName(), table.getIdentifier()));
      }
    }
  }

  @Override
  public void drop(String tableName) {
    getDatabase()
        .tx(
            database -> {
              sync(dropTransaction(tableName, database));
            });
    getDatabase().getListener().schemaChanged(getName());
  }

  private SqlSchemaMetadata dropTransaction(String tableName, Database database) {
    SqlSchemaMetadata sm = (SqlSchemaMetadata) database.getSchema(getName()).getMetadata();
    sm.getTableMetadata(tableName).drop();
    sm.tables.remove(tableName);
    return sm;
  }

  public boolean hasActiveUserRole(String role) {
    return this.getInheritedRolesForActiveUser().contains(role);
  }

  @Override
  public SchemaMetadata setSettings(Map<String, String> settings) {
    if (getDatabase().isAdmin() || hasActiveUserRole(MANAGER.toString())) {
      getDatabase()
          .tx(
              db -> {
                sync(setSettingsTransaction((SqlDatabase) db, getName(), settings));
              });
      getDatabase().getListener().schemaChanged(getName());
      return this;
    } else {
      throw new MolgenisException(
          "Permission denied for user "
              + getDatabase().getActiveUser()
              + " to change setting on schema "
              + getName()
              + ". You need at least MANAGER permission for schema settings");
    }
  }

  private static SqlSchemaMetadata setSettingsTransaction(
      SqlDatabase db, String schemaName, Map<String, String> settings) {
    SqlSchemaMetadata schema = db.getSchema(schemaName).getMetadata();
    settings
        .entrySet()
        .forEach(
            setting -> {
              if (Constants.IS_CHANGELOG_ENABLED.equals(setting.getKey())) {
                toggleChangeLogIfNeeded(db, schema, setting.getValue());
              }
            });
    schema.setSettingsWithoutReload(settings);
    MetadataUtils.saveSchemaMetadata(db.getJooq(), schema);
    return schema;
  }

  /**
   * Checks if the proposed changelog setting is different from the current setting and enables or
   * disables the changelog feature accordingly.
   */
  private static void toggleChangeLogIfNeeded(
      SqlDatabase db, SqlSchemaMetadata schema, String settingsValue) {
    boolean currentValue =
        TRUE.equals(
            TypeUtils.toBool(schema.getSettings().containsKey(Constants.IS_CHANGELOG_ENABLED)));
    boolean newValue = Boolean.parseBoolean(settingsValue);
    if (currentValue != newValue) {
      if (newValue) {
        ChangeLogExecutor.enableChangeLog(db, schema);
      } else {
        ChangeLogExecutor.disableChangeLog(db, schema);
      }
    }
  }

  @Override
  public Map<String, String> getSettings() {
    return super.getSettings();
  }

  protected DSLContext getJooq() {
    return getDatabase().getJooq();
  }

  @Override
  public SqlDatabase getDatabase() {
    return (SqlDatabase) super.getDatabase();
  }

  public List<String> getInheritedRolesForUser(String username) {
    if (username == null) return new ArrayList<>();
    User user = getDatabase().getUser(username);
    if (user.isAdmin()) {
      // admin has all roles
      return executeGetRoles(getJooq(), getName());
    }
    List<String> result = new ArrayList<>();
    // need elevated privileges, so clear user and run as root
    getDatabase()
        .getJooqAsAdmin(
            adminJooq ->
                result.addAll(
                    SqlSchemaMetadataExecutor.getInheritedRoleForUser(
                        adminJooq, getName(), username.trim())));
    return result;
  }

  public List<String> getInheritedRolesForActiveUser() {
    // add cache because this function is called often
    if (rolesCache == null) {
      rolesCache = getInheritedRolesForUser(getDatabase().getActiveUser());
    }
    return rolesCache;
  }

  public String getRoleForUser(String user) {
    if (user == null) user = ANONYMOUS;
    user = user.trim();
    for (Member m : executeGetMembers(getJooq(), this)) {
      if (m.getUser().equals(user)) return m.getRole();
    }
    return null;
  }

  public List<Change> getChanges(int limit) {
    return executeGetChanges(getJooq(), this, limit);
  }

  public Integer getChangesCount() {
    return executeGetChangesCount(getJooq(), this);
  }
}
