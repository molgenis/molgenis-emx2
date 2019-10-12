package org.molgenis.emx2.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.Transaction;
import org.molgenis.emx2.utils.MolgenisException;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.sql.Constants.MG_USER_PREFIX;

public class SqlDatabase implements Database {
  private DSLContext jooq;
  private UserAwareConnectionProvider connectionProvider;
  private Map<String, SchemaMetadata> schemas = new LinkedHashMap<>();

  public SqlDatabase(DataSource source) {
    connectionProvider = new UserAwareConnectionProvider(source);
    this.jooq = DSL.using(connectionProvider, SQLDialect.POSTGRES_10);
    MetadataUtils.createMetadataSchemaIfNotExists(jooq);
  }

  /** private constructor for in transaction */
  private SqlDatabase(Configuration configuration) {
    this.jooq = DSL.using(configuration);
  }

  @Override
  public SqlSchema createSchema(String schemaName) {
    if (schemaName == null || schemaName.isEmpty())
      throw new MolgenisException(
          "schema_create_failed",
          "Schema createTableIfNotExists failed",
          "Schema name was null or empty");
    SqlSchemaMetadata schema = new SqlSchemaMetadata(this, schemaName);
    schema.createSchema();
    schemas.put(schemaName, schema);
    return new SqlSchema(this, schema);
  }

  @Override
  public SqlSchema getSchema(String name) {
    SqlSchemaMetadata metadata = new SqlSchemaMetadata(this, name);
    if (metadata.exists()) {
      SqlSchema schema = new SqlSchema(this, metadata);
      schemas.put(name, metadata);
      return schema;
    } else {
      throw new MolgenisException(
          "get_schema_failed",
          "Get schema failed",
          "Schema with name '" + name + "' could not be found");
    }
  }

  @Override
  public void dropSchema(String name) {
    try {
      SchemaMetadata schema = getSchema(name).getMetadata();
      getJooq().dropSchema(name(name)).cascade().execute();
      MetadataUtils.deleteSchema((SqlSchemaMetadata) schema);
      schemas.remove(name);
    } catch (MolgenisException me) {
      throw new MolgenisException("drop_schema_failed", "Drop schema failed", me.getDetail());
    } catch (DataAccessException dae) {
      throw new MolgenisException(
          "drop_schema_failed", "Drop schema failed", dae.getCause().getMessage());
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
      transaction(
          database -> {
            List<Record> result =
                jooq.fetch("SELECT rolname FROM pg_catalog.pg_roles WHERE rolname = {0}", userName);
            if (result.size() == 0) jooq.execute("CREATE ROLE {0} WITH NOLOGIN", name(userName));
          });
    } catch (DataAccessException dae) {
      throw new MolgenisException(dae);
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
    return jooq.fetch("SELECT rolname FROM pg_catalog.pg_roles WHERE rolname = {0}", userName)
            .size()
        > 0;
  }

  @Override
  public void removeUser(String user) {
    if (!hasUser(user))
      throw new MolgenisException(
          "remove_user_failed",
          "remove user failed",
          "User with name '" + user + "' doesn't exist");
    String userName = MG_USER_PREFIX + user;
    jooq.execute("DROP ROLE {0}", name(userName));
  }

  @Override
  public void transaction(Transaction transaction) {
    // createColumn independent merge of database with transaction connection
    try {
      jooq.transaction(
          config -> {
            DSL.using(config).execute("SET CONSTRAINTS ALL DEFERRED");

            Database db = new SqlDatabase(config);
            transaction.run(db);
          });
    } catch (DataAccessException e) {
      throw new SqlMolgenisException(e);
    }
  }

  @Override
  public void setActiveUser(String username) {
    this.connectionProvider.setActiveUser(username);
  }

  @Override
  public String getActiveUser() {
    String user = jooq.fetchOne("SELECT SESSION_USER").get(0, String.class);
    if (user.contains(MG_USER_PREFIX)) return user.substring(MG_USER_PREFIX.length());
    return null;
    // return this.connectionProvider.getActiveUser();
  }

  @Override
  public void clearActiveUser() {
    this.connectionProvider.clearActiveUser();
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
  }
}
