package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.fairmapper.dcat.DcatHarvestTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DcatHarvestApi {
  private static final Logger log = LoggerFactory.getLogger(DcatHarvestApi.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private DcatHarvestApi() {}

  public static void create(Javalin app) {
    app.post("/{schema}/api/harvest/dcat", DcatHarvestApi::handleHarvest);
  }

  private static void handleHarvest(Context ctx) {
    Schema schema = getSchema(ctx);
    if (schema == null) {
      throw new MolgenisException("Schema not found");
    }

    String body = ctx.body();
    JsonNode json;
    try {
      json = OBJECT_MAPPER.readTree(body);
    } catch (Exception e) {
      throw new MolgenisException("Invalid request body: " + e.getMessage());
    }

    JsonNode urlNode = json.get("url");
    JsonNode rdfNode = json.get("rdf");
    boolean hasUrl = urlNode != null && !urlNode.asText().isBlank();
    boolean hasRdf = rdfNode != null && !rdfNode.asText().isBlank();

    if (!hasUrl && !hasRdf) {
      throw new MolgenisException("Request body must contain 'url' or 'rdf' field");
    }

    DcatHarvestTask task;
    if (hasUrl) {
      String url = urlNode.asText();
      log.info("DCAT harvest requested for schema '{}' from URL: {}", schema.getName(), url);
      task = new DcatHarvestTask(schema, url);
    } else {
      String rdfContent = rdfNode.asText();
      log.info(
          "DCAT harvest requested for schema '{}' from pasted RDF ({} chars)",
          schema.getName(),
          rdfContent.length());
      task = new DcatHarvestTask(schema, "pasted RDF", rdfContent);
    }

    String taskId = TaskApi.submit(task);
    ctx.status(202);
    ctx.json(new TaskReference(taskId, schema));
  }
}
