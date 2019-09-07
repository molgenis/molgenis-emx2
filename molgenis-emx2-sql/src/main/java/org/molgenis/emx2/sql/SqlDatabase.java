package org.molgenis.emx2.sql;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
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
    if (schemaName == null || schemaName.isEmpty())
      throw new MolgenisException(
          "schema_create_failed", "Schema create failed", "Schema name was null or empty");
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
    } else {
      throw new MolgenisException(
          "get_schema_failed",
          "Get schema failed",
          "Schema with name '" + name + "' could not be found");
    }
  }

  @Override
  public void dropSchema(String name) throws MolgenisException {
    try {
      SchemaMetadata schema = getSchema(name).getMetadata();
      getJooq().dropSchema(name);
      MetadataUtils.deleteSchema((SqlSchemaMetadata) schema);
    } catch (MolgenisException me) {
      throw new MolgenisException("drop_schema_failed", "Drop schema failed", me.getDetail());
    } catch (DataAccessException dae) {
      throw new MolgenisException(
          "drop_schema_failed", "Drop schema failed", dae.getCause().getMessage());
    }
  }

  @Override
  public Collection<String> getSchemaNames() throws MolgenisException {
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
  public void addUser(String user) throws MolgenisException {
    String userName = SqlTable.MG_USER_PREFIX + user;

    try {
      transaction(
          database -> {
            List<Record> result =
                jooq.fetch("SELECT FROM pg_catalog.pg_roles " + "WHERE rolname = {0}", userName);
            if (result.size() == 0) jooq.execute("CREATE ROLE {0} WITH NOLOGIN", name(userName));
          });
    } catch (DataAccessException dae) {
      throw new MolgenisException(dae);
    }
  }

  @Override
  public void removeUser(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void transaction(Transaction transaction) throws MolgenisException {
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
  public void transaction(String user, Transaction transaction) throws MolgenisException {
    // createColumn independent merge of database with transaction connection
    jooq.execute("SET SESSION AUTHORIZATION {0}", name(SqlTable.MG_USER_PREFIX + user));
    try {
      jooq.transaction(
          config -> {
            DSL.using(config).execute("SET CONSTRAINTS ALL DEFERRED");

            Database db = new SqlDatabase(config);
            transaction.run(db);
          });
    } catch (DataAccessException e) {
      throw new SqlMolgenisException(e);
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
