package org.molgenis.emx2.catalogue;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.BOOL;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.ColumnType.TEXT;
import static org.molgenis.emx2.TableMetadata.table;

import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.AvailableDataModels;
import org.molgenis.emx2.sql.SqlDatabase;

public class DashboardLoader implements AvailableDataModels.DataModelLoader {

  static final String CATALOGUES = "Catalogues";
  static final String NAME_COLUMN = "name";
  static final String DESCRIPTION_COLUMN = "description";
  static final String GQL_ENDPOINT_COLUMN = "gqlEndpoint";
  static final String ACTIVE_COLUMN = "active";

  static final String COUNTS = "Counts";
  static final String DATA = "Data";
  static final String MOMENT = "moment";

  @Override
  public void load(Schema schema, boolean includeDemoData) {

    schema.create(
        table(CATALOGUES)
            .add(column(NAME_COLUMN).setDescription("unique name").setPkey())
            .add(column(DESCRIPTION_COLUMN, TEXT))
            .add(column(GQL_ENDPOINT_COLUMN))
            .add(column(ACTIVE_COLUMN, BOOL))
            .setDescription("Catalogues to include in dashboard"));

    schema.create(
        table(COUNTS)
            .add(
                column(NAME_COLUMN)
                    .setType(REF)
                    .setRefTable(CATALOGUES)
                    .setRequired(true)
                    .setDescription("catalog name")
                    .setPkey())
            .add(column(MOMENT, ColumnType.DATETIME).setPkey())
            .add(column(DATA, ColumnType.INT))
            .setDescription("Dashboard data"));

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
