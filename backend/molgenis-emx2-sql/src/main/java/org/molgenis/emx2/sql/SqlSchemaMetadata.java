package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.utils.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.executeCreateTable;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.executeDropTable;
import static org.molgenis.emx2.utils.TableSort.sortTableByDependency;

public class SqlSchemaMetadata extends SchemaMetadata {
  private SqlDatabase db;
  private static Logger logger = LoggerFactory.getLogger(SqlSchemaMetadata.class);

  public SqlSchemaMetadata(SqlDatabase db, String name) {
    super(name);
    StopWatch.start("loading schema '" + name + "'");
    for (TableMetadata t : MetadataUtils.loadTables(db.getJooq(), this)) {
      super.create(new SqlTableMetadata(db, this, t));
    }
    StopWatch.print("loading schema '" + name + "'complete");
    this.db = db;
  }

  public boolean exists() {
    return MetadataUtils.schemaExists(db.getJooq(), this.getName());
  }

  @Override
  public TableMetadata create(TableMetadata table) {
    long start = System.currentTimeMillis();
    db.tx(
        database -> {
          TableMetadata result = new SqlTableMetadata(database, this, table);
          executeCreateTable(getJooq(), result);
          super.create(result);
          db.getListener().schemaChanged(getName());
        });
    log(start, "created");
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
          sortTableByDependency(tableList);
          for (TableMetadata tm : tableList) {
            this.create(tm);
          }
        });
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
