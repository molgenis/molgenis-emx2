package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.sql.MetadataUtils.*;
import static org.molgenis.emx2.sql.SqlDatabaseExecutor.*;
import static org.molgenis.emx2.sql.SqlSchemaMetadataExecutor.executeCreateSchema;

import com.zaxxer.hikari.HikariDataSource;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.molgenis.emx2.utils.RandomString;
import org.molgenis.emx2.utils.generator.SnowflakeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlDatabase extends HasSettings<Database> implements Database {
  public static final String ADMIN_USER = "admin";
  public static final String ADMIN_PW_DEFAULT = "admin";

  public static final String ANONYMOUS = "anonymous";
  public static final String USER = "user";
  public static final String WITH = "with {} = {} ";
  public static final int MAX_EXECUTION_TIME_IN_SECONDS = 10;
  public static final int MAX_EXECUTION_TIME_IN_SECONDS_PROLONGED = 60;
  private static final Settings DEFAULT_JOOQ_SETTINGS =
      new Settings().withQueryTimeout(MAX_EXECUTION_TIME_IN_SECONDS);
  private static final Settings PROLONGED_TIMEOUT_JOOQ_SETTINGS =
      new Settings().withQueryTimeout(MAX_EXECUTION_TIME_IN_SECONDS_PROLONGED);
  private static final Random random = new SecureRandom();

  // shared between all instances
  private static DataSource source;

  private Integer databaseVersion;
  private DSLContext jooq;
  private final SqlUserAwareConnectionProvider connectionProvider;
  private final Map<String, SqlSchemaMetadata> schemaCache = new LinkedHashMap<>();
  private Map<String, Supplier<Object>> javaScriptBindings = new HashMap<>();
  private Collection<String> schemaNames = new ArrayList<>();
  private Collection<SchemaInfo> schemaInfos = new ArrayList<>();
  private boolean inTx;
  private static final Logger logger = LoggerFactory.getLogger(SqlDatabase.class);
  private String initialAdminPassword =
      (String)
          EnvironmentProperty.getParameter(Constants.MOLGENIS_ADMIN_PW, ADMIN_PW_DEFAULT, STRING);

  private static String postgresUser =
      (String)
          EnvironmentProperty.getParameter(
              org.molgenis.emx2.Constants.MOLGENIS_POSTGRES_USER, "molgenis", STRING);

  private DatabaseListener listener =
      new DatabaseListener() {
        @Override
        public void userChanged() {
          clearCache();
        }

        @Override
        public void afterCommit() {
          clearCache();
          super.afterCommit();
          logger.info("cleared caches after commit that includes changes on schema(s)");
        }
      };

  // for acting on save/deletes on tables
  private List<TableListener> tableListeners = new ArrayList<>();

  // copy constructor for transactions; only with its own jooq instance that contains tx
  private SqlDatabase(DSLContext jooq, SqlDatabase copy) {
    this.connectionProvider = new SqlUserAwareConnectionProvider(source);
    this.connectionProvider.setActiveUser(copy.connectionProvider.getActiveUser());
    this.jooq = jooq;
    databaseVersion = MetadataUtils.getVersion(jooq);

    this.listener = copy.listener;
    // copy all schemas
    this.schemaNames.addAll(copy.schemaNames);
    this.schemaInfos.addAll(copy.schemaInfos);
    this.setSettingsWithoutReload(copy.getSettings());
    for (Map.Entry<String, SqlSchemaMetadata> schema : copy.schemaCache.entrySet()) {
      this.schemaCache.put(schema.getKey(), new SqlSchemaMetadata(this, schema.getValue()));
    }

    this.javaScriptBindings.putAll(copy.javaScriptBindings);
  }

  private void setJooq(DSLContext ctx) {
    this.jooq = ctx;
  }

  public SqlDatabase(boolean init) {
    initDataSource();
    this.connectionProvider = new SqlUserAwareConnectionProvider(source);
    this.jooq = DSL.using(connectionProvider, SQLDialect.POSTGRES, DEFAULT_JOOQ_SETTINGS);
    if (init) {
      try {
        // elevate privileges for init (prevent reload)
        this.connectionProvider.setActiveUser(ADMIN_USER);
        this.init();
      } finally {
        // always sure to return to anonymous
        this.connectionProvider.clearActiveUser();
      }
    }
    // get database version if exists
    databaseVersion = MetadataUtils.getVersion(jooq);
    logger.info("Database was created using version: {} ", this.databaseVersion);
  }

  private static void initDataSource() {
    if (source == null) {
      String url =
          (String)
              EnvironmentProperty.getParameter(
                  org.molgenis.emx2.Constants.MOLGENIS_POSTGRES_URI,
                  "jdbc:postgresql:molgenis",
                  STRING);
      String pass =
          (String)
              EnvironmentProperty.getParameter(
                  org.molgenis.emx2.Constants.MOLGENIS_POSTGRES_PASS, "molgenis", STRING);
      logger.info(WITH, org.molgenis.emx2.Constants.MOLGENIS_POSTGRES_URI, url);
      logger.info(WITH, org.molgenis.emx2.Constants.MOLGENIS_POSTGRES_USER, postgresUser);
      logger.info(WITH, org.molgenis.emx2.Constants.MOLGENIS_POSTGRES_PASS, "<HIDDEN>");

      // create data source
      HikariDataSource dataSource = new HikariDataSource();
      dataSource.setJdbcUrl(url);
      dataSource.setUsername(postgresUser);
      dataSource.setPassword(pass);

      source = dataSource;
    }
  }

  @Override
  public void init() { // setup default stuff

    try {
      // short transaction
      jooq.transaction(
          config -> {
            DSLContext j = config.dsl();
            j.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm"); // for fast fuzzy search
            j.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto"); // for password hashing
          });

      Migrations.initOrMigrate(this);

      if (!hasUser(ANONYMOUS)) {
        addUser(ANONYMOUS); // used when not logged in
      }
      if (!hasUser(USER)) {
        addUser(USER); // used as role to identify all users except anonymous
      }
      if (!hasUser(ADMIN_USER)) {
        addUser(ADMIN_USER);
        setUserPassword(ADMIN_USER, initialAdminPassword);
      }

      initSystemSchema();

      // get the settings
      clearCache();

      if (!this.getSettings().containsKey(Constants.IS_OIDC_ENABLED)) {
        this.setSetting(
            Constants.IS_OIDC_ENABLED,
            (String) EnvironmentProperty.getParameter(MOLGENIS_OIDC_CLIENT_ID, "false", STRING));
      }

      String instanceId = getSetting(Constants.MOLGENIS_INSTANCE_ID);
      if (instanceId == null) {
        instanceId = String.valueOf(random.nextLong(SnowflakeIdGenerator.MAX_ID));
        this.setSetting(Constants.MOLGENIS_INSTANCE_ID, instanceId);
      }
      if (!SnowflakeIdGenerator.hasInstance()) SnowflakeIdGenerator.init(instanceId);

      if (getSetting(Constants.IS_PRIVACY_POLICY_ENABLED) == null) {
        this.setSetting(Constants.IS_PRIVACY_POLICY_ENABLED, String.valueOf(false));
      }

      if (getSetting(Constants.PRIVACY_POLICY_LEVEL) == null) {
        this.setSetting(Constants.PRIVACY_POLICY_LEVEL, Constants.PRIVACY_POLICY_LEVEL_DEFAULT);
      }

      if (getSetting(Constants.PRIVACY_POLICY_TEXT) == null) {
        this.setSetting(Constants.PRIVACY_POLICY_TEXT, Constants.PRIVACY_POLICY_TEXT_DEFAULT);
      }

      if (getSetting(Constants.LOCALES) == null) {
        this.setSetting(Constants.LOCALES, Constants.LOCALES_DEFAULT);
      }

      String key = getSetting(Constants.MOLGENIS_JWT_SHARED_SECRET);
      if (key == null) {
        key =
            (String)
                EnvironmentProperty.getParameter(
                    Constants.MOLGENIS_JWT_SHARED_SECRET, null, STRING);
      }
      // validate the key, or generate a good one
      if (key == null || key.getBytes().length < 32) {
        key = new RandomString(32).nextString();
      }
      // save the key again
      this.setSetting(Constants.MOLGENIS_JWT_SHARED_SECRET, key);
    } catch (Exception e) {
      // this happens if multiple inits run at same time, totally okay to ignore
      if (!e.getMessage()
          .contains(
              "duplicate key value violates unique constraint \"pg_type_typname_nsp_index\"")) {
        throw e;
      }
    }
  }

  private void initSystemSchema() {
    this.tx(
        tdb -> {
          if (!this.hasSchema(SYSTEM_SCHEMA)) {
            this.createSchema(SYSTEM_SCHEMA);
          }

          Schema schema;
          if (!this.hasSchema(SYSTEM_SCHEMA)) {
            schema = this.createSchema(SYSTEM_SCHEMA);
          } else {
            schema = this.getSchema(SYSTEM_SCHEMA);
          }

          if (!schema.getTableNames().contains("Templates")) {
            schema.create(
                table(
                    "Templates",
                    column("endpoint").setPkey(),
                    column("schema").setPkey(),
                    column("template").setType(ColumnType.TEXT)));
          }
        });
  }

  @Override
  public void setListener(DatabaseListener listener) {
    this.listener = listener;
  }

  @Override
  public DatabaseListener getListener() {
    return this.listener;
  }

  private void log(long start, String message) {
    if (logger.isInfoEnabled()) {
      logger.info("{} in {}ms", message, (System.currentTimeMillis() - start));
    }
  }

  @Override
  public SqlSchema createSchema(String name) {
    return this.createSchema(name, null);
  }

  @Override
  public SqlSchema createSchema(String name, String description) {
    long start = System.currentTimeMillis();
    this.tx(
        db -> {
          SqlSchemaMetadata metadata = new SqlSchemaMetadata(db, name, description);
          validateSchemaIdentifierIsUnique(metadata, db);
          executeCreateSchema((SqlDatabase) db, metadata);
          // copy
          SqlSchema schema = (SqlSchema) db.getSchema(metadata.getName());
          // make current user a manager
          if (db.getActiveUser() != null && !db.getActiveUser().equals(ADMIN_USER)) {
            schema.addMember(db.getActiveUser(), Privileges.MANAGER.toString());
          }
          // refresh
          db.clearCache();
        });
    getListener().schemaChanged(name);
    this.log(start, "created schema " + name);
    return getSchema(name);
  }

  private static void validateSchemaIdentifierIsUnique(SchemaMetadata metadata, Database db) {
    for (String name : db.getSchemaNames()) {
      if (!metadata.getName().equals(name)
          && metadata.getIdentifier().equals(new SchemaMetadata(name).getIdentifier())) {
        throw new MolgenisException(
            String.format(
                "Cannot create/alter schema because name resolves to same identifier: '%s' has same identifier as '%s' (both resolve to identifier '%s')",
                metadata.getName(), name, metadata.getIdentifier()));
      }
    }
  }

  @Override
  public Schema updateSchema(String name, String description) {
    long start = System.currentTimeMillis();
    this.tx(
        db -> {
          SqlSchemaMetadata metadata = new SqlSchemaMetadata(db, name, description);
          MetadataUtils.saveSchemaMetadata(((SqlDatabase) db).getJooq(), metadata);

          // refresh
          db.clearCache();
        });
    getListener().schemaChanged(name);
    this.log(start, "updated schema " + name);
    return getSchema(name);
  }

  @Override
  public SqlSchema getSchema(String name) {
    if (name == null) throw new MolgenisException("Schema name was null or empty");
    if (schemaCache.containsKey(name)) {
      return new SqlSchema(this, schemaCache.get(name));
    } else {
      SqlSchemaMetadata metadata = new SqlSchemaMetadata(this, name);
      if (metadata.exists()) {
        SqlSchema schema = new SqlSchema(this, metadata);
        schemaCache.put(name, metadata); // cache
        return schema;
      }
    }
    return null;
  }

  @Override
  public List<Table> getTablesFromAllSchemas(String tableId) {
    List<Table> tables = new ArrayList<>();
    for (String sn : this.getSchemaNames()) {
      Schema schema = this.getSchema(sn);
      Table t = schema.getTable(tableId);
      if (t != null) {
        tables.add(t);
      }
    }
    return tables;
  }

  @Override
  public void dropSchemaIfExists(String name) {
    if (getSchema(name) != null) {
      this.dropSchema(name);
    }
  }

  @Override
  public void dropSchema(String name) {
    long start = System.currentTimeMillis();
    tx(
        database -> {
          SqlDatabase sqlDatabase = (SqlDatabase) database;
          SqlSchemaMetadataExecutor.executeDropSchema(sqlDatabase, name);
          sqlDatabase.schemaNames.remove(name);
          sqlDatabase.schemaInfos.clear();
          sqlDatabase.schemaCache.remove(name);
        });

    listener.schemaRemoved(name);
    log(start, "dropped schema " + name);
  }

  @Override
  public Schema dropCreateSchema(String name) {
    return this.dropCreateSchema(name, null);
  }

  @Override
  public Schema dropCreateSchema(String name, String description) {
    tx(
        db -> {
          if (getSchema(name) != null) {
            SqlSchemaMetadataExecutor.executeDropSchema((SqlDatabase) db, name);
          }
          SqlSchemaMetadata metadata = new SqlSchemaMetadata(db, name, description);
          executeCreateSchema((SqlDatabase) db, metadata);
          ((SqlDatabase) db).schemaCache.put(name, new SqlSchemaMetadata(db, metadata));
        });
    return getSchema(name);
  }

  @Override
  public Collection<String> getSchemaNames() {
    if (this.schemaNames.isEmpty()) {
      this.schemaNames = MetadataUtils.loadSchemaNames(this);
    }
    return List.copyOf(this.schemaNames);
  }

  @Override
  public Collection<SchemaInfo> getSchemaInfos() {
    if (this.schemaInfos.isEmpty()) {
      this.schemaInfos = MetadataUtils.loadSchemaInfos(this);
    }
    return List.copyOf(this.schemaInfos);
  }

  @Override
  public SchemaInfo getSchemaInfo(String schemaName) {
    Optional<SchemaInfo> result =
        this.getSchemaInfos().stream()
            .filter(schemaInfo -> schemaInfo.tableSchema().equals(schemaName))
            .findFirst();
    if (result.isPresent()) {
      return result.get();
    }
    return null;
  }

  @Override
  public Database setSettings(Map<String, String> settings) {
    if (!isAdmin()) {
      throw new MolgenisException("Insufficient rights to create database level setting");
    }
    super.setSettings(settings);
    MetadataUtils.saveDatabaseSettings(jooq, getSettings());
    // force all sessions to reload
    this.listener.afterCommit();
    return this;
  }

  @Override
  public User addUser(String userName) {
    if (!hasUser(userName)) {
      long start = System.currentTimeMillis();
      // need elevated privileges, so clear user and run as root
      // this is not thread safe therefore must be in a transaction
      tx(
          db -> {
            String currentUser = db.getActiveUser();
            try {
              db.becomeAdmin();
              executeCreateUser((SqlDatabase) db, userName);
            } finally {
              db.setActiveUser(currentUser);
            }
          });
      log(start, "created user " + userName);
    }
    return getUser(userName);
  }

  @Override
  public boolean checkUserPassword(String user, String password) {
    return MetadataUtils.checkUserPassword(getJooq(), user, password);
  }

  @Override
  public void setUserPassword(String user, String password) {
    // can only as admin or as own user
    if (getActiveUser() != null
        && !getActiveUser().equals(ADMIN_USER)
        && !user.equals(getActiveUser())) {
      throw new MolgenisException("Set password failed for user '" + user + "': permission denied");
    }
    // password should have minimum length
    if (password == null || password.length() < 5) {
      throw new MolgenisException(
          "Set password failed for user '" + user + "': password too short");
    }
    long start = System.currentTimeMillis();
    tx(
        db -> {
          if (!db.hasUser(user)) {
            db.addUser(user);
          }
          MetadataUtils.setUserPassword(((SqlDatabase) db).getJooq(), user, password);
        });
    log(start, "set password for user '" + user + "'");
  }

  @Override
  public boolean hasUser(String user) {
    return !jooq.fetch(
            "SELECT rolname FROM pg_catalog.pg_roles WHERE rolname = {0}", MG_USER_PREFIX + user)
        .isEmpty();
  }

  @Override
  public List<User> getUsers(int limit, int offset) {
    if (!ADMIN_USER.equals(getActiveUser()) && getActiveUser() != null) {
      throw new MolgenisException("getUsers denied");
    }
    return MetadataUtils.loadUsers(this, limit, offset);
  }

  @Override
  public void removeUser(String user) {
    long start = System.currentTimeMillis();
    if (user.equals("admin")) throw new MolgenisException("You can't remove admin");
    if (user.equals("anonymous")) throw new MolgenisException("You can't remove anonymous");
    if (user.equals("user")) throw new MolgenisException("You can't remove user");

    if (!hasUser(user))
      throw new MolgenisException(
          "Remove user failed: User with name '" + user + "' doesn't exist");
    tx(db -> ((SqlDatabase) db).getJooq().execute("DROP ROLE {0}", name(MG_USER_PREFIX + user)));
    log(start, "removed user " + user);

    tx(
        db ->
            ((SqlDatabase) db)
                .getJooq()
                .deleteFrom(USERS_METADATA)
                .where(USER_NAME.eq(user))
                .execute());
    log(start, "removed metadata from user " + user);
  }

  public void setEnabledUser(String user, Boolean enabled) {
    long start = System.currentTimeMillis();
    if (user.equals("admin")) throw new MolgenisException("You cant enable or disable admin");
    if (user.equals("anonymous"))
      throw new MolgenisException("You cant enable or disable anonymous");
    if (!hasUser(user))
      throw new MolgenisException(
          (enabled ? "Enabling" : "Disabling")
              + " user failed: User with name '"
              + user
              + "' doesn't exist");
    tx(
        db ->
            ((SqlDatabase) db)
                .getJooq()
                .update(USERS_METADATA)
                .set(USER_ENABLED, enabled)
                .where(USER_NAME.eq(user))
                .execute());
    log(start, (enabled ? "Enabling" : "Disabling") + " user " + user);
  }

  public void addRole(String role) {
    long start = System.currentTimeMillis();
    executeCreateRole(getJooq(), role);
    log(start, "created role " + role);
  }

  @Override
  public void grantCreateSchema(String user) {
    long start = System.currentTimeMillis();
    tx(db -> executeGrantCreateSchema(((SqlDatabase) db).getJooq(), user));
    log(start, "granted create schema to user " + user);
  }

  @Override
  public void setActiveUser(String username) {
    if (username == null || username.isEmpty()) {
      throw new MolgenisException("setActiveUser failed: username cannot be null");
    }
    if (inTx) {
      try {
        if (username.equals(ADMIN_USER)) {
          // admin user is session user, so remove role
          jooq.execute("RESET ROLE;");
        } else {
          // any other user should be set
          jooq.execute("RESET ROLE; SET ROLE {0}", name(MG_USER_PREFIX + username));
        }
      } catch (DataAccessException dae) {
        throw new SqlMolgenisException("Set active user failed", dae);
      }
    } else {
      if (!Objects.equals(username, connectionProvider.getActiveUser())) {
        listener.userChanged();
      }
    }
    this.connectionProvider.setActiveUser(username);
    this.clearCache();
  }

  @Override
  public String getActiveUser() {
    return connectionProvider.getActiveUser();
  }

  @Override
  public void clearActiveUser() {
    if (inTx) {
      // then we don't use the connection provider
      try {
        setActiveUser(ANONYMOUS);
      } catch (DataAccessException dae) {
        throw new SqlMolgenisException("Clear active user failed", dae);
      }
    }
    this.connectionProvider.clearActiveUser();
  }

  @Override
  public void tx(Transaction transaction) {
    if (inTx) {
      // we do not nest transactions
      transaction.run(this);
    } else {
      // we create a new instance, isolated from 'this' until end of transaction
      SqlDatabase db = new SqlDatabase(jooq, this);
      this.tableListeners.forEach(listener -> db.addTableListener(listener));
      try {
        jooq.transaction(
            config -> {
              db.inTx = true;
              DSLContext ctx = DSL.using(config);
              ctx.execute("SET CONSTRAINTS ALL DEFERRED");
              db.setJooq(ctx);
              transaction.run(db);
              db.tableListenersExecutePostCommit();
            });
        // only when commit succeeds we copy state to 'this'
        this.sync(db);
        if (!Objects.equals(db.getActiveUser(), getActiveUser())) {
          this.getListener().userChanged();
        }
        if (db.getListener().isDirty()) {
          this.getListener().afterCommit();
        }
      } catch (Exception e) {
        throw new SqlMolgenisException("Transaction failed", e);
      }
    }
  }

  private void tableListenersExecutePostCommit() {
    for (TableListener tableListener : this.tableListeners) {
      tableListener.executePostCommit();
    }
  }

  private synchronized void sync(SqlDatabase from) {
    if (from != this) {
      this.connectionProvider.setActiveUser(from.connectionProvider.getActiveUser());
      this.databaseVersion = from.databaseVersion;

      this.schemaNames = from.schemaNames;
      this.schemaInfos = from.schemaInfos;
      this.setSettingsWithoutReload(from.getSettings());

      // remove schemas that were dropped
      Set<String> removeSet = new HashSet<>();
      for (String key : this.schemaCache.keySet()) {
        if (!from.schemaCache.keySet().contains(key)) {
          removeSet.add(key);
        }
      }
      for (String key : removeSet) {
        this.schemaCache.remove(key);
      }

      // sync the existing schema cache, add missing
      from.schemaCache
          .entrySet()
          .forEach(
              s -> {
                if (this.schemaCache.containsKey(s.getKey())) {
                  this.schemaCache.get(s.getKey()).sync(s.getValue());
                } else {
                  this.schemaCache.put(
                      s.getKey(), new SqlSchemaMetadata(this, from.schemaCache.get(s.getKey())));
                }
              });
    }
  }

  @Override
  public boolean inTx() {
    return inTx;
  }

  @Override
  public void clearCache() {
    this.schemaCache.clear();
    this.schemaNames.clear();
    this.schemaInfos.clear();

    getJooqAsAdmin(
        adminJooq -> this.setSettingsWithoutReload(MetadataUtils.loadDatabaseSettings(adminJooq)));
  }

  public DSLContext getJooq() {
    return jooq;
  }

  public DSLContext getJooqWithExtendedTimeout() {
    return jooq.configuration().derive(PROLONGED_TIMEOUT_JOOQ_SETTINGS).dsl();
  }

  void getJooqAsAdmin(JooqTransaction transaction) {
    if (ADMIN_USER.equals(getActiveUser())) {
      transaction.run(getJooq());
    } else if (inTx()) {
      // need to do this because updates before this call in current tx
      // might affect result
      // e.g. TestSettings will fail if we don't do this
      // because it does permission changes in same tx before calling the method
      // that uses getJooqAsAdmin.
      String user = connectionProvider.getActiveUser();
      try {
        connectionProvider.setActiveUser(ADMIN_USER);
        transaction.run(getJooq());
      } finally {
        connectionProvider.setActiveUser(user);
      }
    } else {
      final Settings settings = new Settings().withQueryTimeout(MAX_EXECUTION_TIME_IN_SECONDS);
      SqlUserAwareConnectionProvider adminProvider = new SqlUserAwareConnectionProvider(source);
      adminProvider.setActiveUser(ADMIN_USER);
      DSLContext adminJooq = DSL.using(adminProvider, SQLDialect.POSTGRES, settings);
      transaction.run(adminJooq);
    }
  }

  @Override
  public Integer getDatabaseVersion() {
    return databaseVersion;
  }

  @Override
  public int countUsers() {
    if (!ADMIN_USER.equals(getActiveUser()) && getActiveUser() != null) {
      throw new MolgenisException("countUsers denied");
    }
    return MetadataUtils.countUsers(getJooq());
  }

  @Override
  public String getAdminUserName() {
    return ADMIN_USER;
  }

  @Override
  public boolean isAdmin() {
    return ADMIN_USER.equals(getActiveUser());
  }

  @Override
  public boolean isAnonymous() {
    return ANONYMOUS.equals(getActiveUser());
  }

  @Override
  public void becomeAdmin() {
    this.setActiveUser(getAdminUserName());
  }

  @Override
  public boolean isOidcEnabled() {
    return this.getSettings().containsKey(Constants.IS_OIDC_ENABLED)
        && Boolean.parseBoolean(this.getSettings().get(Constants.IS_OIDC_ENABLED));
  }

  @Override
  public boolean hasSchema(String name) {
    return getSchema(name) != null;
  }

  @Override
  public void saveUser(User user) {
    if (!isAdmin() && !user.getUsername().equals(getActiveUser())) {
      throw new MolgenisException("Cannot save changes for user " + user + ": permission denied");
    }
    MetadataUtils.saveUserMetadata(getJooq(), user);
  }

  @Override
  public User getUser(String userName) {
    if (hasUser(userName)) {
      User user = MetadataUtils.loadUserMetadata(this, userName);
      // might not yet have any metadata saved
      return user != null ? user : new User(this, userName);
    }
    return null;
  }

  public Database setBindings(Map<String, Supplier<Object>> bindings) {
    this.javaScriptBindings = bindings;
    return this;
  }

  @Override
  public Map<String, Supplier<Object>> getJavaScriptBindings() {
    return javaScriptBindings;
  }

  public void addTableListener(TableListener tableListener) {
    this.tableListeners.add(tableListener);
  }

  public TableListener getTableListener(String schemaName, String tableName) {
    Optional<TableListener> result =
        tableListeners.stream()
            .filter(
                tableListener ->
                    tableListener.getTableName().equals(tableName)
                        && tableListener.getSchemaName().equals(schemaName))
            .findFirst();
    if (result.isPresent()) {
      return result.get();
    }
    return null;
  }

  @Override
  public List<LastUpdate> getLastUpdated() {
    return ChangeLogExecutor.executeLastUpdates(jooq);
  }

  public List<Member> loadUserRoles() {
    List<Member> members = new ArrayList<>();
    String roleFilter = Constants.MG_ROLE_PREFIX;
    String userFilter = Constants.MG_USER_PREFIX;
    List<Record> allRoles =
        jooq.fetch(
            "select distinct m.rolname as member, r.rolname as role"
                + " from pg_catalog.pg_auth_members am "
                + " join pg_catalog.pg_roles m on (m.oid = am.member)"
                + "join pg_catalog.pg_roles r on (r.oid = am.roleid)"
                + "where r.rolname LIKE {0} and m.rolname LIKE {1}",
            roleFilter + "%", userFilter + "%");

    for (Record userRecord : allRoles) {
      String memberName =
          userRecord.getValue("member", String.class).substring(userFilter.length());
      String roleName = userRecord.getValue("role", String.class).substring(roleFilter.length());
      members.add(new Member(memberName, roleName));
    }
    return members;
  }

  public void revokeRoles(String userName, List<Map<String, String>> members) {
    try {
      members.forEach(
          member -> {
            String prefixedRole =
                Constants.MG_ROLE_PREFIX + member.get("schemaId") + "/" + member.get(ROLE);
            jooq.execute(
                "REVOKE {0} FROM {1}",
                name(prefixedRole), name(Constants.MG_USER_PREFIX + userName));
          });
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("Removal of role failed", dae);
    }
  }

  public void updateRoles(String userName, List<Map<String, String>> members) {
    try {
      members.forEach(
          member -> {
            String schemaId = member.get("schemaId");
            String role = member.get(ROLE);
            String prefixedRole = MG_ROLE_PREFIX + schemaId + "/" + role;
            String prefixedName = MG_USER_PREFIX + userName;

            List<Member> existingUserRoles =
                this.getSchema(schemaId).getMembers().stream()
                    .filter(mem -> mem.getUser().equals(userName))
                    .toList();
            if (existingUserRoles.iterator().hasNext()) {
              existingUserRoles.forEach(
                  existingRole -> {
                    String oldRole = MG_ROLE_PREFIX + schemaId + "/" + existingRole.getRole();
                    jooq.execute("REVOKE {0} FROM {1}", name(oldRole), name(prefixedName));
                  });
            }
            jooq.execute("GRANT {0} TO {1}", name(prefixedRole), name(prefixedName));
          });
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("Updating of role failed", dae);
    }
  }
}
