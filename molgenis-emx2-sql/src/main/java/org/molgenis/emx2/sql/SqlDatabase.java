package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.DefaultRoles;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.Transaction;
import org.molgenis.emx2.MolgenisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.sql.Constants.MG_USER_PREFIX;

public class SqlDatabase implements Database {
  private static final String ADMIN = "admin";
  private DataSource source;
  private DSLContext jooq;
  private SqlUserAwareConnectionProvider connectionProvider;
  private Map<String, SchemaMetadata> schemas = new LinkedHashMap<>();
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

  @Override
  public SqlSchema createSchema(String schemaName) {
    if (schemaName == null || schemaName.isEmpty())
      throw new MolgenisException("Create schema failed", "Schema name was null or empty");
    SqlSchemaMetadata schema = new SqlSchemaMetadata(this, schemaName);
    schema.createSchema();
    schemas.put(schemaName, schema);
    if (getActiveUser() != null) {
      getSchema(schema.getName()).addMember(getActiveUser(), DefaultRoles.MANAGER.toString());
    }
    return new SqlSchema(this, schema);
  }

  @Override
  public SqlSchema getSchema(String name) {
    // todo, re-enable caching
    SqlSchemaMetadata metadata = new SqlSchemaMetadata(this, name);
    if (metadata.exists()) {
      SqlSchema schema = new SqlSchema(this, metadata);
      schemas.put(name, metadata);
      return schema;
    }
    return null;
  }

  @Override
  public void dropSchema(String name) {
    try {
      SchemaMetadata schema = getSchema(name).getMetadata();
      getJooq().dropSchema(name(name)).cascade().execute();
      MetadataUtils.deleteSchema((SqlSchemaMetadata) schema);
      schemas.remove(name);
      if (logger.isInfoEnabled()) {
        logger.info("dropped schema {}", name);
      }
    } catch (MolgenisException me) {
      throw new MolgenisException("Drop schema failed", me.getMessage());
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("Drop schema failed", dae);
    }
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
    String userName = MG_USER_PREFIX + user;
    try {
      tx(
          database -> {
            List<Record> result =
                jooq.fetch("SELECT rolname FROM pg_catalog.pg_roles WHERE rolname = {0}", userName);
            if (result.isEmpty()) jooq.execute("CREATE ROLE {0} WITH NOLOGIN", name(userName));
          });
      if (logger.isInfoEnabled()) {
        logger.info("created user {}", user);
      }
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("Add user failed", dae);
    }
  }

  @Override
  public void grantCreateSchema(String user) {
    try {
      String databaseName = jooq.fetchOne("SELECT current_database()").get(0, String.class);
      jooq.execute(
          "GRANT CREATE ON DATABASE {0} TO {1}", name(databaseName), name(MG_USER_PREFIX + user));
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException(dae);
    }
  }

  @Override
  public boolean hasUser(String user) {
    String userName = MG_USER_PREFIX + user;
    return !jooq.fetch("SELECT rolname FROM pg_catalog.pg_roles WHERE rolname = {0}", userName)
        .isEmpty();
  }

  @Override
  public void removeUser(String user) {
    if (!hasUser(user))
      throw new MolgenisException(
          "Remove user failed", "User with name '" + user + "' doesn't exist");
    String userName = MG_USER_PREFIX + user;
    jooq.execute("DROP ROLE {0}", name(userName));
    if (logger.isInfoEnabled()) {
      logger.info("removed user {}", user);
    }
  }

  @Override
  public synchronized void tx(Transaction transaction) {

    if (inTx) {
      // we dont nest transactions
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
        e.printStackTrace();
      } finally {
        this.inTx = false;
        jooq = originalContext;
      }
    }
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
  public void clearCache() {
    this.schemas = new LinkedHashMap<>();
  }

  protected DSLContext getJooq() {
    return jooq;
  }

  public void createRole(String role) {
    jooq.execute(
        "DO $$\n"
            + "BEGIN\n"
            + "    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = {0}) THEN\n"
            + "        CREATE ROLE {1};\n"
            + "    END IF;\n"
            + "END\n"
            + "$$;\n",
        inline(role), name(role));
    logger.info("created role {}", role);
  }
}
