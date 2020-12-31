package org.molgenis.emx2.sql;

import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.executeCreateTable;
import static org.molgenis.emx2.sql.SqlTableMetadataExecutor.executeDropTable;
import static org.molgenis.emx2.utils.TableSort.sortTableByDependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jooq.DSLContext;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.Setting;
import org.molgenis.emx2.TableMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlSchemaMetadata extends SchemaMetadata {
  private static Logger logger = LoggerFactory.getLogger(SqlSchemaMetadata.class);

  public SqlSchemaMetadata(SqlDatabase db, String name) {
    super(db, MetadataUtils.loadSchemaMetadata(db.getJooq(), new SchemaMetadata(name)));
    if (logger.isInfoEnabled()) {
      logger.info("loading schema '{}' as user {}", name, db.getActiveUser());
    }
    long start = System.currentTimeMillis();
    for (TableMetadata table : MetadataUtils.loadTables(db.getJooq(), this)) {
      super.create(new SqlTableMetadata(this, table));
    }
    if (logger.isInfoEnabled()) {
      logger.info("loading schema '{}' complete in {}ms", name, System.currentTimeMillis() - start);
    }
  }

  public boolean exists() {
    return MetadataUtils.schemaExists(getDatabase().getJooq(), this.getName());
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
    getDatabase()
        .tx(
            database -> {
              List<TableMetadata> tableList = new ArrayList<>();
              tableList.addAll(List.of(tables));
              if (tableList.size() > 1) sortTableByDependency(tableList);
              for (TableMetadata table : tableList) {
                SqlTableMetadata result = new SqlTableMetadata(this, table);
                executeCreateTable(getJooq(), result);
                super.create(result);
              }
            });
    getDatabase().getListener().schemaChanged(getName());
  }

  @Override
  public void drop(String tableName) {
    long start = System.currentTimeMillis();
    getDatabase()
        .tx(
            dsl -> {
              executeDropTable(getJooq(), getTableMetadata(tableName));
              super.drop(tableName);
            });
    getDatabase().getListener().schemaChanged(getName());
    log(start, "dropped");
  }

  @Override
  public SqlSchemaMetadata setSettings(Collection<Setting> settings) {
    super.setSettings(settings);
    for (Setting setting : settings) {
      MetadataUtils.saveSetting(getDatabase().getJooq(), this, null, setting);
    }
    return this;
  }

  @Override
  public SqlSchemaMetadata setSetting(String key, String value) {
    MetadataUtils.saveSetting(getDatabase().getJooq(), this, null, new Setting(key, value));
    return this;
  }

  @Override
  public List<Setting> getSettings() {
    if (super.getSettings().size() == 0) {
      super.setSettings(MetadataUtils.loadSettings(getJooq(), this));
    }
    return super.getSettings();
  }

  @Override
  public void removeSetting(String key) {
    MetadataUtils.deleteSetting(getDatabase().getJooq(), this, null, new Setting(key, null));
    super.removeSetting(key);
  }

  protected DSLContext getJooq() {
    return getDatabase().getJooq();
  }

  private void log(long start, String message) {
    String user = getDatabase().getActiveUser();
    if (user == null) user = "molgenis";
    logger.info("{} {} {} in {}ms", user, message, getName(), (System.currentTimeMillis() - start));
  }

  @Override
  public SqlDatabase getDatabase() {
    return (SqlDatabase) super.getDatabase();
  }
}
