package org.molgenis.emx2.examples;

import org.molgenis.emx2.SchemaMetadata;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

public class JobsModel {

  private JobsModel() {
    // hide constructor
  }

  public static void create(SchemaMetadata schema) {

    schema.create(table("Jobs", column("id").setPkey(), column("owner"), column("group")));

    schema.create(
        table(
            "Steps",
            column("id").setPkey(),
            column("job").setType(REF).setRefTable("Jobs").setKey(2),
            column("step").setType(INT).setKey(2),
            column("label"),
            column("scheduled").setType(DATETIME),
            column("started").setType(DATETIME).setNullable(true),
            column("completed").setType(DATETIME).setNullable(true),
            column("error"),
            column("success"),
            column("count").setType(INT)));

    // refback
    schema
        .getTableMetadata("Jobs")
        .add(column("steps").setType(REFBACK).setRefTable("Steps").setMappedBy("job"));
  }
}
