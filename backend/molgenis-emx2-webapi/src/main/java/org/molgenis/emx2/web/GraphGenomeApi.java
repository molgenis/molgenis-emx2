package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.BeaconApi.getTableFromAllSchemas;
import static spark.Spark.get;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.graphgenome.GraphGenome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class GraphGenomeApi {
  private static Logger logger = LoggerFactory.getLogger(GraphqlApi.class);
  private static MolgenisSessionManager sessionManager;
  public static final String GRAPH_GENOME_API_LOCATION = "/api/graphgenome";

  public static void create(MolgenisSessionManager sm) {
    sessionManager = sm;
    get(GRAPH_GENOME_API_LOCATION, GraphGenomeApi::graphGenomeForDatabase);
  }

  private static int graphGenomeForDatabase(Request request, Response response) throws IOException {
    List<Table> tables = getTableFromAllSchemas("GenomicVariations", request);
    OutputStream outputStream = response.raw().getOutputStream();
    new GraphGenome()
        .graphGenomeAsRDF(outputStream, request, response, GRAPH_GENOME_API_LOCATION, tables);
    outputStream.flush();
    outputStream.close();
    return 200;
  }
}
