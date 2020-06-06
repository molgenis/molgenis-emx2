package org.molgenis.emx2.examples;

import org.molgenis.emx2.SchemaMetadata;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.TableMetadata.table;

public class JobsModel {
  public static void create(SchemaMetadata schema) {

    schema.create(table("Jobs", column("id"), column("owner"), column("group")).pkey("id"));

    schema.create(
        table(
                "Steps",
                column("id"),
                column("job").type(REF).refTable("Jobs"),
                column("step").type(INT),
                column("label"),
                column("scheduled").type(DATETIME),
                column("started").type(DATETIME).nullable(true),
                column("completed").type(DATETIME).nullable(true),
                column("error"),
                column("success"),
                column("count").type(INT))
            .pkey("id")
            .addUnique("job", "step")
            .addUnique("job", "label"));

    // refback
    schema
        .getTableMetadata("Jobs")
        .add(column("steps").type(REFBACK).refTable("Steps").mappedBy("job"));
  }
}
