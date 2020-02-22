package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.molgenis.emx2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.executeCreateTable;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.executeDropTable;

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
  public TableMetadata create(TableMetadata metadata) {
    long start = System.currentTimeMillis();
    db.tx(
        database -> {
          TableMetadata result = new SqlTableMetadata(database, this, metadata);
          executeCreateTable(getJooq(), result);
          super.create(result);
          db.getListener().schemaChanged(getName());
        });
    log(start, "created");
    return getTableMetadata(metadata.getTableName());
  }

  @Override
  public TableMetadata getTableMetadata(String name) {
    SqlTableMetadata metadata = (SqlTableMetadata) super.getTableMetadata(name);
    if (metadata != null) {
      return super.getTableMetadata(name);
    } else {
      // else retrieve from database
      SqlTableMetadata table = new SqlTableMetadata(db, this, table(name));
      table.load();
      if (table.exists()) {
        super.tableCache.put(name, table);
      }
    }
    return super.getTableMetadata(name);
  }

  @Override
  public Collection<String> getTableNames() {
    Collection<String> result = super.getTableNames();
    if (result.isEmpty()) {
      result = MetadataUtils.loadTableNames(getJooq(), this); // try to load
      super.tableNames = result;
    }
    return result;
  }

  @Override
  public void drop(String tableName) {
    long start = System.currentTimeMillis();
    db.tx(
        dsl -> {
          executeDropTable(getJooq(), getTableMetadata(tableName));
          super.drop(tableName);
          db.getListener().schemaChanged(getName());
        });
    log(start, "dropped");
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
