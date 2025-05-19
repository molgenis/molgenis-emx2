package org.molgenis.emx2.web.controllers;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.List;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.SqlDatabase;

public class CatalogueSearchController {
  private CatalogueSearchController() {
    // hide constructor
  }

  public static void create(Javalin app) {
    app.get("/{schema}/_search", CatalogueSearchController::autoCompleteOptions);
  }

  public static void autoCompleteOptions(Context ctx) {
    Schema schema = getSchema(ctx);

    if (schema == null) {
      ctx.status(404);
      ctx.result("Schema not found");
      return;
    }

    DSLContext jooq = ((SqlDatabase) schema.getDatabase()).getJooq();

    String schemaName = schema.getName();
    String tableName = "Variables";

    Table<?> table = DSL.table(DSL.name(schemaName, tableName));

    Result<Record1<String>> result =
        jooq.select(DSL.field("name", String.class))
            .from(table)
            .where(DSL.field("name").like("age%"))
            .or(DSL.field("description").like("age%"))
            .limit(10)
            .fetch();

    for (Record record : result) {
      String name = record.get("name", String.class);
      System.out.println(name);
    }
    List<String> data = result.getValues(DSL.field("name", String.class));
    ctx.json(data);
  }
}
