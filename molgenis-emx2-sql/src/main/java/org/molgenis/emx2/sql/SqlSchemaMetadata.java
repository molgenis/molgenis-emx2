package org.molgenis.emx2.sql;

import org.jooq.CreateSchemaFinalStep;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.DefaultRoles;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static org.jooq.impl.DSL.name;

public class SqlSchemaMetadata extends SchemaMetadata {
  private SqlDatabase db;
  private static Logger logger = LoggerFactory.getLogger(SqlSchemaMetadata.class);

  public SqlSchemaMetadata(SqlDatabase db, String name) {
    super(name);
    this.db = db;
  }

  public boolean exists() {
    return MetadataUtils.schemaExists(db.getJooq(), this.getName());
  }

  @Override
  public SqlTableMetadata getTableMetadata(String name) {
    TableMetadata metadata = super.getTableMetadata(name);
    if (metadata != null) return (SqlTableMetadata) metadata;
    else {
      // else retrieve from metadata
      SqlTableMetadata table = new SqlTableMetadata(db, this, name);
      table.load();
      if (table.exists()) {
        this.tables.put(name, table);
        return table;
      } else {
        return null;
      }
    }
  }

  void createSchema() {
    long start = System.currentTimeMillis();
    String schemaName = getName();

    try (CreateSchemaFinalStep step = db.getJooq().createSchema(schemaName)) {
      step.execute();

      String member = getRolePrefix() + DefaultRoles.VIEWER;
      String editor = getRolePrefix() + DefaultRoles.EDITOR;
      String manager = getRolePrefix() + DefaultRoles.MANAGER;
      String owner = getRolePrefix() + DefaultRoles.OWNER;

      db.createRole(member);
      db.createRole(editor);
      db.createRole(manager);
      db.createRole(owner);

      // make editor also member
      db.getJooq().execute("GRANT {0} TO {1}", name(member), name(editor));
      // make manager also editor and member
      db.getJooq().execute("GRANT {0},{1} TO {2}", name(member), name(editor), name(manager));
      // make owner also editor, manager, member
      db.getJooq()
          .execute(
              "GRANT {0},{1},{2} TO {3} WITH ADMIN OPTION",
              name(member), name(editor), name(manager), name(owner));

      // make current user the owner
      String currentUser = db.getJooq().fetchOne("SELECT current_user").get(0, String.class);
      db.getJooq().execute("GRANT {0} TO {1}", name(manager), name(currentUser));

      // grant the permissions
      db.getJooq().execute("GRANT USAGE ON SCHEMA {0} TO {1}", name(schemaName), name(member));
      db.getJooq().execute("GRANT ALL ON SCHEMA {0} TO {1}", name(schemaName), name(manager));

      log(start, "created");
    } catch (DataAccessException e) {
      throw new SqlMolgenisException("schema_create_failed", "Schema create failed", e);
    }
    MetadataUtils.saveSchemaMetadata(db.getJooq(), this);
  }

  String getRolePrefix() {
    return Constants.MG_ROLE_PREFIX + getName().toUpperCase() + "/";
  }

  @Override
  public TableMetadata createTable(String name) {
    TableMetadata metadata = getTableMetadata(name);
    if (metadata != null) return metadata;
    // else
    SqlTableMetadata table = new SqlTableMetadata(db, this, name);
    table.createTable();
    super.tables.put(name, table);
    return table;
  }

  @Override
  public Collection<String> getTableNames() {
    Collection<String> result = super.getTableNames();
    if (result.isEmpty()) {
      result = MetadataUtils.loadTableNames(this);
      for (String r : result) {
        this.tables.put(r, null);
      }
    }
    return result;
  }

  @Override
  public void dropTable(String tableName) {
    SqlTableMetadata tableMetadata = getTableMetadata(tableName);
    tableMetadata.dropTable();
    super.dropTable(tableName);
  }

  protected DSLContext getJooq() {
    return db.getJooq();
  }

  private void log(long start, String message) {
    String user = db.getActiveUser();
    if (user == null) user = "molgenis";
    logger.info("{} {} {} in {}ms", user, message, getName(), (System.currentTimeMillis() - start));
  }
}
