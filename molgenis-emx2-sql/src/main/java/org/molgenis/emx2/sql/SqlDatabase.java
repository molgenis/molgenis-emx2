package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.sql.Constants.MG_USER_PREFIX;
import static org.molgenis.emx2.sql.CreateDatabase.*;
import static org.molgenis.emx2.sql.Roles.executeCreateRole;
import static org.molgenis.emx2.sql.SqlSchemaMetadataUtils.executeCreateSchema;

public class SqlDatabase implements Database {
  private static final String ADMIN = "admin";
  private DataSource source;
  private DSLContext jooq;
  private SqlUserAwareConnectionProvider connectionProvider;
  private Map<String, SchemaMetadata> schemas = new LinkedHashMap<>(); // cache
  private boolean inTx;
  private static Logger logger = LoggerFactory.getLogger(SqlDatabase.class);

  public SqlDatabase(DataSource source) {
    this.source = source;
    this.connectionProvider = new SqlUserAwareConnectionProvider(source);
    this.jooq = DSL.using(connectionProvider, SQLDialect.POSTGRES_10);
    MetadataUtils.createMetadataSchemaIfNotExists(jooq);

    // setup default stuff
    this.jooq.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
    if (!hasUser("anonymous")) {
      this.addUser("anonymous");
    }
    if (!hasUser(ADMIN)) {
      this.addUser(ADMIN);
      this.jooq.execute("ALTER USER {0} WITH SUPERUSER", name(MG_USER_PREFIX + ADMIN));
    }
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
        database -> {
          executeCreateSchema(this, metadata);
          schemas.put(name, metadata);
          // make current user a manager
          if (getActiveUser() != null) {
            getSchema(metadata.getName())
                .addMember(getActiveUser(), DefaultRoles.MANAGER.toString());
          }
        });
    this.log(start, "created schema " + name);
    return new SqlSchema(this, metadata);
  }

  @Override
  public SqlSchema getSchema(String name) {
    SqlSchemaMetadata metadata = new SqlSchemaMetadata(this, name);
    if (metadata.exists()) {
      SqlSchema schema = new SqlSchema(this, metadata);
      schemas.put(name, metadata); // cache
      return schema;
    }
    return null;
  }

  @Override
  public void dropSchema(String name) {
    long start = System.currentTimeMillis();
    tx(d -> executeDropSchema(getJooq(), getSchema(name).getMetadata()));
    schemas.remove(name);
    log(start, "dropped schema " + name);
  }

  @Override
  public Collection<String> getSchemaNames() {
    Collection<String> result = schemas.keySet();
    if (result.isEmpty()) {
      result = MetadataUtils.loadSchemaNames(this);
      for (String r : result) {
        this.schemas.put(r, null);
      }
    }
    return result;
  }

  @Override
  public void addUser(String user) {
    long start = System.currentTimeMillis();
    tx(d -> executeAddUser(getJooq(), user));
    log(start, "created user" + user);
  }

  @Override
  public boolean hasUser(String user) {
    return !jooq.fetch(
            "SELECT rolname FROM pg_catalog.pg_roles WHERE rolname = {0}", MG_USER_PREFIX + user)
        .isEmpty();
  }

  @Override
  public void removeUser(String user) {
    long start = System.currentTimeMillis();
    if (!hasUser(user))
      throw new MolgenisException(
          "Remove user failed", "User with name '" + user + "' doesn't exist");
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
      this.connectionProvider.setActiveUser(username);
    }
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
        DSL.using(conn, SQLDialect.POSTGRES_10)
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
        clearCache();
        throw me;
      } catch (DataAccessException dae) {
        clearCache();
        throw new SqlMolgenisException(dae);
      } catch (SQLException e) {
        throw new MolgenisException("Transaction failed", e.getMessage(), e);
      } finally {
        this.inTx = false;
        jooq = originalContext;
      }
    }
  }

  @Override
  public void clearCache() {
    this.schemas = new LinkedHashMap<>();
  }

  protected DSLContext getJooq() {
    return jooq;
  }
}
