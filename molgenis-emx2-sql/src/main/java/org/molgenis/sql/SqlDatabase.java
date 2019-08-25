package org.molgenis.sql;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.data.Database;
import org.molgenis.MolgenisException;
import org.molgenis.Transaction;
import org.molgenis.metadata.SchemaMetadata;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;
import static org.molgenis.sql.MetadataUtils.loadSchemaNames;

public class SqlDatabase implements Database {
  private DSLContext jooq;
  private Map<String, SchemaMetadata> schemas = new LinkedHashMap<>();

  public SqlDatabase(DataSource source) throws MolgenisException {
    this.jooq = DSL.using(source, SQLDialect.POSTGRES_10);
    MetadataUtils.createMetadataSchemaIfNotExists(jooq);
  }

  /** private constructor for in transaction */
  private SqlDatabase(Configuration configuration) {
    this.jooq = DSL.using(configuration);
  }

  @Override
  public SqlSchema createSchema(String schemaName) throws MolgenisException {
    SqlSchemaMetadata schema = new SqlSchemaMetadata(this, schemaName);
    schema.createSchema();
    schemas.put(schemaName, schema);
    return new SqlSchema(this, schema);
  }

  @Override
  public SqlSchema getSchema(String name) throws MolgenisException {
    SqlSchemaMetadata metadata = new SqlSchemaMetadata(this, name);
    if (metadata.exists()) {
      SqlSchema schema = new SqlSchema(this, metadata);
      schemas.put(name, metadata);
      return schema;
    } else
      throw new MolgenisException(
          "invalid_schema", "Schema doesn't exist", "Schema '" + name + " could not be found");
  }

  @Override
  public Collection<String> getSchemaNames() throws MolgenisException {
    Collection<String> result = schemas.keySet();
    if (result.isEmpty()) {
      result = loadSchemaNames(this);
      for (String r : result) {
        this.schemas.put(r, null);
      }
    }
    return result;
  }

  @Override
  public void addUser(String name) throws MolgenisException {
    try {
      jooq.execute("CREATE ROLE {0} WITH NOLOGIN", name(name));
    } catch (DataAccessException dae) {
      if (dae.getMessage().contains("already exists")) {
        // do nothing, idempotent
      } else {
        throw new MolgenisException(dae);
      }
    }
  }

  @Override
  public void removeUser(String name) {}

  @Override
  public void grantRole(String role, String user) throws MolgenisException {
    try {
      jooq.execute("GRANT {0} TO {1}", name(role), name(user));
    } catch (DataAccessException dae) {
      throw new MolgenisException(dae);
    }
  }

  @Override
  public void revokeRole(String role, String user) throws MolgenisException {}

  @Override
  public void transaction(Transaction transaction) throws MolgenisException {
    // createColumn independent copy of database with transaction connection
    try {
      jooq.transaction(
          config -> {
            DSL.using(config).execute("SET CONSTRAINTS ALL DEFERRED");

            Database db = new SqlDatabase(config);
            transaction.run(db);
          });
    } catch (DataAccessException e) {
      throw new MolgenisException(e);
    }
  }

  @Override
  public void transaction(String user, Transaction transaction) throws MolgenisException {
    // createColumn independent copy of database with transaction connection
    jooq.execute("SET SESSION AUTHORIZATION {0}", name(user));
    try {
      jooq.transaction(
          config -> {
            DSL.using(config).execute("SET CONSTRAINTS ALL DEFERRED");

            Database db = new SqlDatabase(config);
            transaction.run(db);
          });
    } catch (Exception e) {
      throw new MolgenisException(e);
    } finally {
      jooq.execute("RESET SESSION AUTHORIZATION");
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
  }
}
