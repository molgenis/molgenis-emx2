package org.molgenis.emx2.web;

import static org.molgenis.emx2.Constants.API_RDF;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.graphgenome.GraphGenome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphGenomeApi {
  private static Logger logger = LoggerFactory.getLogger(GraphqlApi.class);
  private static MolgenisSessionManager sessionManager;
  public static final String GRAPH_GENOME_API_LOCATION = "/api/graphgenome";

  public static void create(Javalin app, MolgenisSessionManager sm) {
    sessionManager = sm;
    app.get(GRAPH_GENOME_API_LOCATION, GraphGenomeApi::graphGenomeForDatabase);
  }

  private static int graphGenomeForDatabase(Context ctx) throws IOException {

    Database database = sessionManager.getSession(ctx.req()).getDatabase();
    List<Table> tables = database.getTablesFromAllSchemas("GenomicVariations");
    OutputStream outputStream = ctx.outputStream();

    String gene = ctx.queryParam("gene");
    String assembly = ctx.queryParam("assembly");
    String ucscgenome = ctx.queryParam("ucscgenome");
    String baseURL =
        ctx.scheme()
            + "://"
            + ctx.host()
            + (ctx.port() > 0 ? ":" + ctx.port() : "")
            + (!ctx.path().isEmpty() ? "/" + ctx.path() + "/" : "/");
    var format = RDFApi.selectFormat(ctx);
    new GraphGenome(baseURL, API_RDF, format)
        .graphGenomeAsRDF(
            outputStream, gene, assembly, ucscgenome, GRAPH_GENOME_API_LOCATION, tables);
    outputStream.flush();
    outputStream.close();
    return 200;
  }
}
