package org.molgenis.emx2.datamodels.test;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

import org.molgenis.emx2.SchemaMetadata;

public class JobsModel {

  private JobsModel() {
    // hide constructor
  }

  public void create(SchemaMetadata schema) {

    schema.create(table("Jobs", column("id").setPkey(), column("owner"), column("group")));

    schema.create(
        table(
            "Steps",
            column("id").setPkey(),
            column("job").setType(REF).setRefTable("Jobs").setKey(2).setRequired(true),
            column("step").setType(INT).setKey(2).setRequired(true),
            column("label").setRequired(true),
            column("scheduled").setType(DATETIME).setRequired(true),
            column("started").setType(DATETIME).setRequired(true),
            column("completed").setType(DATETIME),
            column("error").setRequired(true),
            column("success").setRequired(true),
            column("count").setType(INT).setRequired(true)));

    // refBack
    schema
        .getTableMetadata("Jobs")
        .add(column("steps").setType(REFBACK).setRefTable("Steps").setRefBack("job"));
  }
}
