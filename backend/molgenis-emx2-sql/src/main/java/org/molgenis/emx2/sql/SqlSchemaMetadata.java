package org.molgenis.emx2.sql;

import static org.molgenis.emx2.Privileges.MANAGER;
import static org.molgenis.emx2.sql.ChangeLogExecutor.executeGetChanges;
import static org.molgenis.emx2.sql.ChangeLogExecutor.executeGetChangesCount;
import static org.molgenis.emx2.sql.SqlColumnExecutor.getOntologyTableDefinition;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_USER;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;
import static org.molgenis.emx2.sql.SqlSchemaMetadataExecutor.executeGetMembers;
import static org.molgenis.emx2.sql.SqlSchemaMetadataExecutor.executeGetRoles;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.executeCreateTable;
import static org.molgenis.emx2.utils.TableSort.sortTableByDependency;

import java.util.*;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.molgenis.emx2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlSchemaMetadata extends SchemaMetadata {
  private static Logger logger = LoggerFactory.getLogger(SqlSchemaMetadata.class);
  // cache for retrieved roles
  private List<String> rolesCache = null;

  // copy constructor
  protected SqlSchemaMetadata(Database db, SqlSchemaMetadata copy) {
    this.name = copy.getName();
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

      // remove settings not available anymore
      remove =
          this.settings.keySet().stream()
              .filter(t -> !from.settings.containsKey(t))
              .collect(Collectors.toSet());
      remove.forEach(s -> this.settings.remove(s));

      from.settings.putAll(from.getSettingsAsMap());
    }
  }

  public SqlSchemaMetadata(Database db, String name, String description) {
    super(
        db,
        MetadataUtils.loadSchemaMetadata(
            ((SqlDatabase) db).getJooq(), new SchemaMetadata(name, description)));
    this.reload();
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
    this.tables.clear();
    this.settings.clear();
    this.rolesCache = null;
    for (TableMetadata table : MetadataUtils.loadTables(getDatabase().getJooq(), this)) {
      super.create(new SqlTableMetadata(this, table));
    }
    for (Map.Entry<String, String> entry :
        MetadataUtils.loadSettings(getDatabase().getJooq(), this).entrySet()) {
      super.setSetting(entry.getKey(), entry.getValue());
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
              List<TableMetadata> tableList = new ArrayList<>();
              tableList.addAll(List.of(tables));
              if (tableList.size() > 1) sortTableByDependency(tableList);
              for (TableMetadata table : tableList) {
                SqlTableMetadata result = null;
                if (TableType.ONTOLOGIES.equals(table.getTableType())) {
                  result =
                      new SqlTableMetadata(
                          sm,
                          getOntologyTableDefinition(table.getTableName(), table.getDescription()));
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
  public SqlSchemaMetadata setSettings(Collection<Setting> settings) {
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

  @Override
  public SqlSchemaMetadata setSetting(String key, String value) {
    this.setSettings(List.of(new Setting(key, value)));
    return this;
  }

  private static SqlSchemaMetadata setSettingsTransaction(
      SqlDatabase db, String schemaName, Collection<Setting> settings) {
    SqlSchemaMetadata schema = db.getSchema(schemaName).getMetadata();
    settings.forEach(
        setting -> {
          if (Constants.IS_CHANGELOG_ENABLED.equals(setting.key())) {
            toggleChangeLogIfNeeded(db, schema, setting.value());
          }
          MetadataUtils.saveSetting(db.getJooq(), schema, null, setting);
          schema.settings.put(setting.key(), setting.value());
        });
    return schema;
  }

  /**
   * Checks if the proposed changelog setting is different from the current setting and enables or
   * disables the changelog feature accordingly.
   */
  private static void toggleChangeLogIfNeeded(
      SqlDatabase db, SqlSchemaMetadata schema, String settingsValue) {
    boolean currentValue =
        schema
            .findSettingValue(Constants.IS_CHANGELOG_ENABLED)
            .filter(Boolean::parseBoolean)
            .isPresent();
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
  public List<Setting> getSettings() {
    if (super.getSettings().size() == 0) {
      super.setSettings(MetadataUtils.loadSettings(getJooq(), this));
    }
    return super.getSettings();
  }

  @Override
  public Map<String, String> getSettingsAsMap() {
    if (super.getSettingsAsMap().size() == 0) {
      super.setSettings(MetadataUtils.loadSettings(getJooq(), this));
    }
    return super.getSettingsAsMap();
  }

  @Override
  public SqlSchemaMetadata removeSetting(String key) {
    getDatabase()
        .tx(
            db -> {
              sync(removeSettingTransaction(key, (SqlDatabase) db, getName()));
            });
    getDatabase().getListener().schemaChanged(getName());
    return this;
  }

  private static SqlSchemaMetadata removeSettingTransaction(
      String key, SqlDatabase db, String schemaName) {
    SqlSchemaMetadata schema = db.getSchema(schemaName).getMetadata();
    if (Constants.IS_CHANGELOG_ENABLED.equals(key)) {
      toggleChangeLogIfNeeded(db, schema, Boolean.FALSE.toString());
    }
    MetadataUtils.deleteSetting(db.getJooq(), schema, null, new Setting(key, null));
    schema.settings.remove(key);
    return schema;
  }

  protected DSLContext getJooq() {
    return getDatabase().getJooq();
  }

  @Override
  public SqlDatabase getDatabase() {
    return (SqlDatabase) super.getDatabase();
  }

  public void renameTable(TableMetadata table, String newName) {
    getDatabase()
        .tx(
            db -> {
              sync(renameTableTransaction(db, getName(), table.getTableName(), newName));
            });
  }

  private static SqlSchemaMetadata renameTableTransaction(
      Database db, String schemaName, String tableName, String newName) {
    SqlSchemaMetadata sm = (SqlSchemaMetadata) db.getSchema(schemaName).getMetadata();
    SqlTableMetadata tm = sm.getTableMetadata(tableName);
    tm.alterName(newName);
    sm.tables.remove(tableName);
    sm.tables.put(newName, tm);
    return sm;
  }

  public List<String> getIneritedRolesForUser(String user) {
    if (user == null) return new ArrayList<>();
    if (ADMIN_USER.equals(user)) {
      // admin has all roles
      return executeGetRoles(getJooq(), getName());
    }
    final String username = user.trim();
    List<String> result = new ArrayList<>();
    // need elevated privileges, so clear user and run as root
    // this is not thread safe therefore must be in a transaction
    getDatabase()
        .tx(
            tdb -> {
              String current = tdb.getActiveUser();
              try {
                tdb.becomeAdmin(); // elevate privileges
                result.addAll(
                    SqlSchemaMetadataExecutor.getInheritedRoleForUser(
                        ((SqlDatabase) tdb).getJooq(), getName(), username));
              } finally {
                tdb.setActiveUser(current); // reset privileges
              }
            });
    return result;
  }

  public List<String> getInheritedRolesForActiveUser() {
    // add cache because this function is called often
    if (rolesCache == null) {
      rolesCache = getIneritedRolesForUser(getDatabase().getActiveUser());
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
