package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.sql.Constants.MG_USER_PREFIX;
import static org.molgenis.emx2.sql.SqlDatabaseExecutor.*;
import static org.molgenis.emx2.sql.SqlSchemaMetadataExecutor.executeCreateSchema;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.emx2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlDatabase implements Database {
  public static final String ADMIN = "admin";
  public static final String ANONYMOUS = "anonymous";
  public static final String USER = "user";

  private DataSource source;
  private String databaseVersion;
  private DSLContext jooq;
  private SqlUserAwareConnectionProvider connectionProvider;
  private Map<String, SqlSchemaMetadata> schemaCache = new LinkedHashMap<>(); // cache
  private Collection<String> schemaNames = new ArrayList<>();
  private boolean inTx;
  private static Logger logger = LoggerFactory.getLogger(SqlDatabase.class);
  private DatabaseListener listener =
      new DatabaseListener() {
        private boolean reloadOnCommit = false;
        private Set<String> reloadSchemas = new HashSet<>();

        @Override
        public void schemaRemoved(String name) {
          clearCache();
          logger.info("clear cache schemaRemoved");
        }

        @Override
        public void userChanged() {
          // dummy
        }

        @Override
        public void schemaChanged(String schemaName) {
          // wait until end of transaction
          if (!inTx) {
            getSchema(schemaName).getMetadata().reload();
            clearCache();
            logger.info("reload schema " + schemaName + " on schemaChanged");
          } else {
            reloadOnCommit = true;
            reloadSchemas.add(schemaName);
          }
        }

        @Override
        public void afterCommit() {
          if (reloadOnCommit) {
            for (String schemaName : reloadSchemas) {
              if (getSchema(schemaName) != null) {
                getSchema(schemaName).getMetadata().reload();
              }
              logger.info("reload schema " + schemaName + " on afterCommit");
            }
            clearCache();
            reloadOnCommit = false;
            reloadSchemas.clear();
          }
        }
      };

  public SqlDatabase(DataSource source, boolean init) {
    this.source = source;
    this.connectionProvider = new SqlUserAwareConnectionProvider(source);
    this.jooq = DSL.using(connectionProvider, SQLDialect.POSTGRES);
    if (init) {
      this.init();
    }
    // get database version if exists
    databaseVersion = MetadataUtils.getVersion(jooq);
    logger.info("Database was created using version: " + this.databaseVersion);
  }

  @Override
  public void init() { // setup default stuff

    try {
      // short transaction
      jooq.transaction(
          config -> {
            DSLContext j = config.dsl();
            j.execute("LOCK TABLE pg_catalog.pg_namespace");
            j.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm"); // for fast fuzzy search
            j.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto"); // for password hashing
          });

      MetadataUtils.init(jooq);

      if (!hasUser(ANONYMOUS)) {
        addUser(ANONYMOUS); // used when not logged in
      }
      if (!hasUser(USER)) {
        addUser(USER); // used as role to identify all users except anonymous
      }
      if (!hasUser(ADMIN)) {
        addUser(ADMIN);
        setUserPassword(
            ADMIN, ADMIN); // TODO should be able to pass this as param so secure on deploy
        jooq.execute("ALTER USER {0} WITH SUPERUSER", name(MG_USER_PREFIX + ADMIN));
      }
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
    long start = System.currentTimeMillis();
    SqlSchemaMetadata metadata = new SqlSchemaMetadata(this, name);
    this.tx(
        db -> {
          executeCreateSchema((SqlDatabase) db, metadata);
          // make current user a manager
          if (db.getActiveUser() != null) {
            db.getSchema(metadata.getName())
                .addMember(db.getActiveUser(), Privileges.MANAGER.toString());
          }
        });
    getListener().schemaChanged(metadata.getName());
    this.log(start, "created schema " + name);
    return new SqlSchema(this, metadata);
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
  public void dropSchema(String name) {
    long start = System.currentTimeMillis();
    tx(d -> SqlSchemaMetadataExecutor.executeDropSchema((SqlDatabase) d, name));
    listener.schemaRemoved(name);
    log(start, "dropped schema " + name);
  }

  @Override
  public Schema dropCreateSchema(String name) {
    tx(
        db -> {
          if (getSchema(name) != null) {
            db.dropSchema(name);
          }
          db.createSchema(name);
        });
    return getSchema(name);
  }

  @Override
  public Collection<String> getSchemaNames() {
    if (this.schemaNames.isEmpty()) {
      this.schemaNames = MetadataUtils.loadSchemaNames(this);
    }
    return this.schemaNames;
  }

  @Override
  public void addUser(String user) {
    if (hasUser(user)) return; // idempotent
    long start = System.currentTimeMillis();
    // need elevated privileges, so not as active user
    String currentUser = getActiveUser();
    try {
      clearActiveUser();
      tx(d -> executeCreateUser(getJooq(), user));
    } finally {
      if (currentUser != null) {
        setActiveUser(currentUser);
      }
    }
    log(start, "created user " + user);
  }

  @Override
  public boolean checkUserPassword(String user, String password) {
    return MetadataUtils.checkUserPassword(getJooq(), user, password);
  }

  @Override
  public void setUserPassword(String user, String password) {
    // can only as admin or as own user
    if (getActiveUser() != null
        && !getActiveUser().equals(ADMIN)
        && !user.equals(getActiveUser())) {
      throw new MolgenisException("Set password failed for user '" + user + "': permission denied");
    }
    long start = System.currentTimeMillis();
    tx(d -> MetadataUtils.setUserPassword(getJooq(), user, password));
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
    if (!ADMIN.equals(getActiveUser()) && getActiveUser() != null) {
      throw new MolgenisException("getUsers denied");
    }
    return MetadataUtils.loadUsers(getJooq(), limit, offset);
  }

  @Override
  public void removeUser(String user) {
    long start = System.currentTimeMillis();
    if (!hasUser(user))
      throw new MolgenisException(
          "Remove user failed: User with name '" + user + "' doesn't exist");
    tx(d -> jooq.execute("DROP ROLE {0}", name(MG_USER_PREFIX + user)));
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
    tx(d -> executeGrantCreateSchema(getJooq(), user));
    log(start, "granted create schema to user " + user);
  }

  @Override
  public void setActiveUser(String username) {
    if (inTx) {
      try {
        jooq.execute("SET SESSION AUTHORIZATION {0}", name(MG_USER_PREFIX + username));
      } catch (DataAccessException dae) {
        throw new SqlMolgenisException("Set active user failed", dae);
      }
    } else {
      clearCache();
      this.connectionProvider.setActiveUser(username);
    }
    listener.userChanged();
  }

  @Override
  public String getActiveUser() {
    String user = jooq.fetchOne("SELECT SESSION_USER").get(0, String.class);
    if (user.contains(MG_USER_PREFIX)) return user.substring(MG_USER_PREFIX.length());
    return null;
  }

  @Override
  public void clearActiveUser() {
    if (inTx) {
      // then we don't use the connection provider
      try {
        jooq.execute("RESET SESSION AUTHORIZATION");
      } catch (DataAccessException dae) {
        throw new SqlMolgenisException("Clear active user failed", dae);
      }
    } else {
      this.connectionProvider.clearActiveUser();
    }
  }

  @Override
  public synchronized void tx(Transaction transaction) {
    if (inTx) {
      // we do not nest transactions
      transaction.run(this);
    } else {
      // createColumn independent merge of database with transaction connection
      DSLContext originalContext = jooq;
      try (Connection conn = source.getConnection()) {
        this.inTx = true;
        DSL.using(conn, SQLDialect.POSTGRES)
            .transaction(
                config -> {
                  DSLContext ctx = DSL.using(config);
                  ctx.execute("SET CONSTRAINTS ALL DEFERRED");
                  this.jooq = ctx;
                  if (connectionProvider.getActiveUser() != null) {
                    this.setActiveUser(connectionProvider.getActiveUser());
                  }
                  transaction.run(this);
                  this.clearActiveUser();
                });
      } catch (MolgenisException me) {
        throw me;
      } catch (DataAccessException dae) {
        throw new SqlMolgenisException(dae);
      } catch (SQLException e) {
        throw new MolgenisException("Transaction failed", e);
      } finally {
        this.inTx = false;
        jooq = originalContext;
        listener.afterCommit();
      }
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
  }

  protected DSLContext getJooq() {
    return jooq;
  }

  @Override
  public String getDatabaseVersion() {
    return databaseVersion;
  }

  @Override
  public int countUsers() {
    if (!ADMIN.equals(getActiveUser()) && getActiveUser() != null) {
      throw new MolgenisException("countUsers denied");
    }
    return MetadataUtils.countUsers(getJooq());
  }
}
