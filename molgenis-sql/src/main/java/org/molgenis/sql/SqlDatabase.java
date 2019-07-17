package org.molgenis.sql;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.molgenis.*;
import org.molgenis.Schema;
import org.molgenis.Transaction;
import org.molgenis.beans.DatabaseBean;

import javax.sql.DataSource;
import java.util.List;

public class SqlDatabase extends DatabaseBean {
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
  public Schema createSchema(String name) throws MolgenisException {
    try (CreateSchemaFinalStep step = sql.createSchema(name)) {
      step.execute();
    } catch (Exception e) {
      throw new MolgenisException(e);
    }
    super.addSchema(new SqlSchema(this, sql, name));
    return getSchema(name);
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
}
