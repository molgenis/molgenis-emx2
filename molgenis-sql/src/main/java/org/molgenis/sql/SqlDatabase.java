package org.molgenis.sql;

import org.jooq.*;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.*;
import org.molgenis.Schema;
import org.molgenis.Transaction;
import org.molgenis.beans.DatabaseBean;

import javax.sql.DataSource;
import java.util.List;

import static org.jooq.impl.DSL.name;
import static org.molgenis.Database.Prefix.MGROLE_;
import static org.molgenis.Database.Roles.*;

public class SqlDatabase extends DatabaseBean implements Database {
  private DSLContext sql;

  public SqlDatabase(DataSource source) throws MolgenisException {
    DSLContext context = DSL.using(source, SQLDialect.POSTGRES_10);
    this.sql = context;
  }

  /** private constructor for in transaction */
  private SqlDatabase(Configuration configuration) throws MolgenisException {
    this.sql = DSL.using(configuration);
  }

  @Override
  public Schema createSchema(String schemaName) throws MolgenisException {
    try (CreateSchemaFinalStep step = sql.createSchema(schemaName)) {
      step.execute();
      Name viewer = name(MGROLE_ + schemaName.toUpperCase() + _VIEWER);
      Name editor = name(MGROLE_ + schemaName.toUpperCase() + _EDITOR);
      Name manager = name(MGROLE_ + schemaName.toUpperCase() + _MANAGER);
      Name admin = name(MGROLE_ + schemaName.toUpperCase() + _ADMIN);

      sql.execute("CREATE ROLE {0}", viewer);
      sql.execute("CREATE ROLE {0}", editor);
      sql.execute("CREATE ROLE {0}", manager);
      sql.execute("CREATE ROLE {0}", admin);

      sql.execute("GRANT {0} TO {1}", viewer, editor);
      sql.execute("GRANT {0},{1} TO {2}", viewer, editor, manager);
      sql.execute("GRANT {0},{1},{2} TO {3} WITH ADMIN OPTION", viewer, editor, manager, admin);

      sql.execute("GRANT USAGE ON SCHEMA {0} TO {1}", name(schemaName), viewer);
      sql.execute("GRANT ALL ON SCHEMA {0} TO {1}", name(schemaName), manager);
    } catch (Exception e) {
      throw new MolgenisException(e);
    }
    super.addSchema(new SqlSchema(this, sql, schemaName));
    return getSchema(schemaName);
  }

  @Override
  public Schema getSchema(String name) throws MolgenisException {
    // get cached if available
    Schema s = super.getSchema(name);
    if (s != null) return s;

    // else try to load from metadata
    List<org.jooq.Schema> schemas = sql.meta().getSchemas(name);
    if (schemas.size() == 0) throw new MolgenisException("Schema '" + name + "' unknown");
    return new SqlSchema(this, sql, name);
  }

  @Override
  public void createUser(String name) throws MolgenisException {
    try {
      sql.execute("CREATE ROLE {0} WITH NOLOGIN", name(name));
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
      sql.execute("GRANT {0} TO {1}", name(role), name(user));
    } catch (DataAccessException dae) {
      throw new MolgenisException(dae);
    }
  }

  @Override
  public void transaction(Transaction transaction) throws MolgenisException {
    // create independent copy of database with transaction connection
    try {
      sql.transaction(
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
    // create independent copy of database with transaction connection
    sql.execute("SET SESSION AUTHORIZATION {0}", name(user));
    try {
      sql.transaction(
          config -> {
            Database db = new SqlDatabase(config);
            transaction.run(db);
          });
    } catch (Exception e) {
      throw new MolgenisException(e);
    } finally {
      sql.execute("RESET SESSION AUTHORIZATION");
    }
  }
}
