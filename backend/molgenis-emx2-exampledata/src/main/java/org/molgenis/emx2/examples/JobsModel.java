package org.molgenis.emx2.examples;

import org.molgenis.emx2.SchemaMetadata;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

public class JobsModel {
  public static void create(SchemaMetadata schema) {

    schema.create(table("Jobs", column("id").pkey(), column("owner"), column("group")));

    schema.create(
        table(
            "Steps",
            column("id").pkey(),
            column("job").type(REF).refTable("Jobs").key(2),
            column("step").type(INT).key(2),
            column("label"),
            column("scheduled").type(DATETIME),
            column("started").type(DATETIME).nullable(true),
            column("completed").type(DATETIME).nullable(true),
            column("error"),
            column("success"),
            column("count").type(INT)));

    // refback
    schema
        .getTableMetadata("Jobs")
        .add(column("steps").type(REFBACK).refTable("Steps").mappedBy("job"));
  }
}
