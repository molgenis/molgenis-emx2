package org.molgenis.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.*;
import org.molgenis.Schema;
import org.molgenis.Transaction;
import org.molgenis.beans.DatabaseBean;

import javax.sql.DataSource;

import java.util.Collection;

import static org.jooq.impl.DSL.*;
import static org.molgenis.sql.MetadataUtils.loadSchemaNames;

public class SqlDatabase extends DatabaseBean implements Database {

  private DSLContext jooq;

  public SqlDatabase(DataSource source) throws MolgenisException {
    this.jooq = DSL.using(source, SQLDialect.POSTGRES_10);
    MetadataUtils.createMetadataSchemaIfNotExists(jooq);
  }

  /** private constructor for in transaction */
  private SqlDatabase(Configuration configuration) throws MolgenisException {
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
      } else throw new MolgenisException("Schema '" + name + " doesn't exist");
    }
  }

  @Override
  public Collection<String> getSchemaNames() throws MolgenisException {
    Collection<String> result = super.getSchemaNames();
    if (result.size() == 0) {
      result = loadSchemaNames(this);
      for (String r : result) {
        this.schemas.put(r, null);
      }
    }
    return result;
  }

  @Override
  public void createUser(String name) throws MolgenisException {
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
  public void grantRoleToUser(String role, String user) throws MolgenisException {
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
            Database db = new SqlDatabase(config);
            transaction.run(db);
          });
    } catch (org.jooq.exception.DataAccessException e) {
      throw new MolgenisException(e);
    } catch (Exception e3) {
      throw new MolgenisException(e3);
    }
  }

  @Override
  public void transaction(String user, Transaction transaction) throws MolgenisException {
    // createColumn independent copy of database with transaction connection
    jooq.execute("SET SESSION AUTHORIZATION {0}", name(user));
    try {
      jooq.transaction(
          config -> {
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
  public void setDeferChecks(boolean shouldDefer) {
    if (shouldDefer) {
      jooq.execute("SET CONSTRAINTS ALL DEFERRED");
    } else {
      jooq.execute("SET CONSTRAINTS ALL IMMEDIATE");
    }
  }

  DSLContext getJooq() {
    return jooq;
  }
}
