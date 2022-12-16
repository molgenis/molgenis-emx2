package org.molgenis.emx2.catalogue;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.BOOL;
import static org.molgenis.emx2.ColumnType.TEXT;
import static org.molgenis.emx2.TableMetadata.table;

import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.AvailableDataModels;
import org.molgenis.emx2.sql.SqlDatabase;

public class DashboardLoader implements AvailableDataModels.DataModelLoader {

  public static final String CATALOGUES = "Catalogues";
  public static final String NAME_COLUMN = "name";
  private static final String DESCRIPTION_COLUMN = "description";
  private static final String GQL_ENDPOINT_COLUMN = "gqlEndpoint";
  private static final String ACTIVE_COLUMN = "active";

  @Override
  public void load(Schema schema, boolean includeDemoData) {

    schema.create(
        table(CATALOGUES)
            .add(column(NAME_COLUMN).setDescription("unique name").setPkey())
            .add(column(DESCRIPTION_COLUMN, TEXT))
            .add(column(GQL_ENDPOINT_COLUMN))
            .add(column(ACTIVE_COLUMN, BOOL))
            .setDescription("Catalogues to include in dashboard"));

    if (includeDemoData) {
      loadExampleData(schema);
    }
  }

  private void loadExampleData(Schema schema) {
    schema
        .getTable(CATALOGUES)
        .insert(
            new Row()
                .set(NAME_COLUMN, "data catalogue")
                .set(DESCRIPTION_COLUMN, "main molgenis catalogue")
                .set(GQL_ENDPOINT_COLUMN, "https://data-catalogue.molgeniscloud.org")
                .set(ACTIVE_COLUMN, true));

    schema.addMember(SqlDatabase.ANONYMOUS, Privileges.VIEWER.toString());
  }
}
