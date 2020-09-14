package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.executeCreateTable;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.executeDropTable;
import static org.molgenis.emx2.utils.TableSort.sortTableByDependency;

public class SqlSchemaMetadata extends SchemaMetadata {
  private SqlDatabase db;
  private static Logger logger = LoggerFactory.getLogger(SqlSchemaMetadata.class);

  public SqlSchemaMetadata(SqlDatabase db, String name) {
    super(MetadataUtils.loadSchemaMetadata(db.getJooq(), new SchemaMetadata(name)));
    logger.info("loading schema '" + name + "' as user " + db.getActiveUser());
    long start = System.currentTimeMillis();
    for (TableMetadata table : MetadataUtils.loadTables(db.getJooq(), this)) {
      super.create(new SqlTableMetadata(db, this, table));
    }
    logger.info(
        "loading schema '" + name + "'complete in " + (System.currentTimeMillis() - start) + "ms");
    this.db = db;
  }

  public boolean exists() {
    return MetadataUtils.schemaExists(db.getJooq(), this.getName());
  }

  @Override
  public SqlTableMetadata create(TableMetadata table) {
    // delete to batch creator
    this.create(new TableMetadata[] {table});
    return getTableMetadata(table.getTableName());
  }

  @Override
  public SqlTableMetadata getTableMetadata(String name) {
    return (SqlTableMetadata) super.getTableMetadata(name);
  }

  @Override
  public void create(TableMetadata... tables) {
    db.tx(
        database -> {
          List<TableMetadata> tableList = new ArrayList<>();
          tableList.addAll(List.of(tables));
          if (tableList.size() > 1) sortTableByDependency(tableList);
          for (TableMetadata table : tableList) {
            SqlTableMetadata result = new SqlTableMetadata(database, this, table);
            executeCreateTable(getJooq(), result);
            super.create(result);
          }
        });
    db.getListener().schemaChanged(getName());
  }

  @Override
  public void drop(String tableName) {
    long start = System.currentTimeMillis();
    db.tx(
        dsl -> {
          executeDropTable(getJooq(), getTableMetadata(tableName));
          super.drop(tableName);
        });
    db.getListener().schemaChanged(getName());
    log(start, "dropped");
  }

  @Override
  public SqlSchemaMetadata setSettings(Map<String, String> settings) {
    super.setSettings(settings);
    MetadataUtils.saveSchemaMetadata(db.getJooq(), this);
    db.getListener().schemaChanged(getName());
    return this;
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
