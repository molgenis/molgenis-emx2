package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.MG_USER_PREFIX;
import static org.molgenis.emx2.sql.SqlDatabaseExecutor.*;
import static org.molgenis.emx2.sql.SqlSchemaMetadataExecutor.executeCreateSchema;

import com.zaxxer.hikari.HikariDataSource;
import java.util.*;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.EnvironmentProperty;
import org.molgenis.emx2.utils.RandomString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlDatabase extends HasSettings<Database> implements Database {
  public static final String ADMIN_USER = "admin";
  public static final String ADMIN_PW_DEFAULT = "admin";

  public static final String ANONYMOUS = "anonymous";
  public static final String USER = "user";
  public static final String WITH = "with {} = {} ";
  public static final int TEN_SECONDS = 10;

  // shared between all instances
  private static DataSource source;

  private Integer databaseVersion;
  private DSLContext jooq;
  private final SqlUserAwareConnectionProvider connectionProvider;
  private final Map<String, SqlSchemaMetadata> schemaCache = new LinkedHashMap<>(); // cache
  private Collection<String> schemaNames = new ArrayList<>();
  private Collection<SchemaInfo> schemaInfos = new ArrayList<>();
  private boolean inTx;
  private static Logger logger = LoggerFactory.getLogger(SqlDatabase.class);
  private String initialAdminPassword =
      (String)
          EnvironmentProperty.getParameter(Constants.MOLGENIS_ADMIN_PW, ADMIN_PW_DEFAULT, STRING);
  private final Boolean isOidcEnabled =
      EnvironmentProperty.getParameter(Constants.MOLGENIS_OIDC_CLIENT_ID, null, STRING) != null;
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

    // copy all schemas
    this.schemaNames.addAll(copy.schemaNames);
    this.schemaInfos.addAll(copy.schemaInfos);
    this.setSettingsWithoutReload(copy.getSettings());
    for (Map.Entry<String, SqlSchemaMetadata> schema : copy.schemaCache.entrySet()) {
      this.schemaCache.put(schema.getKey(), new SqlSchemaMetadata(this, schema.getValue()));
    }
  }

  private void setJooq(DSLContext ctx) {
    this.jooq = ctx;
  }

  public SqlDatabase(boolean init) {
    initDataSource();
    this.connectionProvider = new SqlUserAwareConnectionProvider(source);
    final Settings settings = new Settings().withQueryTimeout(TEN_SECONDS);
    this.jooq = DSL.using(connectionProvider, SQLDialect.POSTGRES, settings);
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

      // get the settings
      clearCache();

      if (getSetting(Constants.IS_OIDC_ENABLED) == null) {
        // use environment property unless overridden in settings
        this.setSetting(Constants.IS_OIDC_ENABLED, String.valueOf(isOidcEnabled));
      }

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
    if (isAdmin()) {
      super.setSettings(settings);
      MetadataUtils.saveDatabaseSettings(jooq, getSettings());
      // force all sessions to reload
      this.listener.afterCommit();
    } else {
      throw new MolgenisException("Insufficient rights to create database level setting");
    }
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
    if (!hasUser(user))
      throw new MolgenisException(
          "Remove user failed: User with name '" + user + "' doesn't exist");
    tx(db -> ((SqlDatabase) db).getJooq().execute("DROP ROLE {0}", name(MG_USER_PREFIX + user)));
    log(start, "removed user " + user);
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
    String user = jooq.fetchOne("SELECT CURRENT_USER").get(0, String.class);
    if (user.equals(postgresUser)) {
      return ADMIN_USER;
    } else if (user.contains(MG_USER_PREFIX)) {
      String userName = user.substring(MG_USER_PREFIX.length());
      if (!userName.isEmpty()) {
        return userName;
      }
    }
    // user is either valid MG_USER_* or postgresUser, otherwise error state
    throw new MolgenisException("Unexpected user found as activeUser " + user);
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
    // elevate privileges for loading settings
    String user = this.connectionProvider.getActiveUser();
    try {
      this.connectionProvider.setActiveUser(ADMIN_USER);
      this.setSettingsWithoutReload(MetadataUtils.loadDatabaseSettings(getJooq()));
    } finally {
      this.connectionProvider.setActiveUser(user);
    }
  }

  public DSLContext getJooq() {
    return jooq;
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
    return this.isOidcEnabled;
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
}
