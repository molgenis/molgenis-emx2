package org.molgenis.sql;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.Database;
import org.molgenis.MolgenisException;
import org.molgenis.Transaction;
import org.molgenis.beans.DatabaseBean;

import javax.sql.DataSource;
import java.util.Collection;

import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.name;
import static org.molgenis.sql.MetadataUtils.loadSchemaNames;

public class SqlDatabase extends DatabaseBean implements Database {
  private DSLContext jooq;

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
    SqlSchema schema = new SqlSchema(this, schemaName);
    schema.createSchema();
    super.schemas.put(schemaName, schema);
    return schema;
  }

  @Override
  public SqlSchema getSchema(String name) throws MolgenisException {
    try {
      return (SqlSchema) super.getSchema(name);
    } catch (Exception e) {
      SqlSchema schema = new SqlSchema(this, name);
      if (schema.exists()) {
        schemas.put(name, schema);
        return schema;
      } else
        throw new MolgenisException(
            "invalid_schema", "Schema doesn't exist", "Schema '" + name + " could not be found");
    }
  }

  @Override
  public Collection<String> getSchemaNames() throws MolgenisException {
    Collection<String> result = super.getSchemaNames();
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
  public void grantRole(String role, String user) throws MolgenisException {
    try {
      jooq.execute("GRANT {0} TO {1}", name(role), name(user));
    } catch (DataAccessException dae) {
      throw new MolgenisException(dae);
    }
  }

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
