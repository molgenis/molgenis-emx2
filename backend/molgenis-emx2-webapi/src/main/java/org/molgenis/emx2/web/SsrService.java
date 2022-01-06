// Acknowledgement to SakaiBorder for providing the template for this code
// see https://github.com/SakaiBorder/java-vue-ssr/tree/master/src/main/js/src

package org.molgenis.emx2.web;

import static org.molgenis.emx2.graphql.GraphqlTableFieldFactory.createKeyFilter;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;
import static spark.Spark.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import java.io.*;
import java.net.MalformedURLException;
import java.util.Map;
import javax.script.*;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.molgenis.emx2.Filter;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.resource.AbstractFileResolvingResource;
import spark.resource.ClassPathResourceHandler;

public class SsrService {

  static Logger logger = LoggerFactory.getLogger(SsrService.class);

  public static String SSR_BASE_PATH = "/public_html/ssr/";

  public static void create() {
    get("/:schema/view/:table/:filter", SsrService::getRecordForDefaultRoute);
    get("/:schema/view/:table/:filter/:route", SsrService::getRecordForRoute);
  }

  static String getRecordForDefaultRoute(Request request, Response response) {
    try {
      return renderRoute(
          "/" + request.params("table") + "/" + request.params("filter"), retrieveRecord(request));
    } catch (Exception e) {
      throw new MolgenisException("Rendering failed: ", e);
    }
  }

  static String getRecordForRoute(Request request, Response response) {
    try {
      return renderRoute(
          "/"
              + request.params("table")
              + "/"
              + request.params("filter")
              + "/"
              + request.params("route"),
          retrieveRecord(request));
    } catch (Exception e) {
      throw new MolgenisException("Rendering failed: ", e);
    }
  }

  public static String retrieveRecord(Request request) throws IOException {
    Schema schema = getSchema(request);
    Table table = schema.getTable(request.params("table"));

    // create primary key filter
    Filter filter =
        createKeyFilter(new ObjectMapper().readValue(request.params("filter"), Map.class));

    return "{\"row\":"
        + table.query().where(filter).retrieveJSON()
        + ", \"table\":\""
        + table.getName()
        + "\", \"schema\": "
        + JsonUtil.schemaToJson(schema.getMetadata())
        + "}";
  }

  public static String renderRoute(String route, String jsonState) {
    StringWriter stringWriter = new StringWriter();
    try {
      // we use graalvm to render the js
      ScriptEngine engine =
          GraalJSScriptEngine.create(
              null,
              Context.newBuilder("js")
                  .allowHostAccess(HostAccess.ALL)
                  .allowHostClassLookup(s -> true)
                  .option("js.ecmascript-version", "2021"));

      // ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");
      ScriptContext context = new SimpleScriptContext();

      // catch the output
      engine.getContext().setWriter(stringWriter);

      // create a variable to get the rendered result (is this necessary?)
      context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
      Bindings engineScope = context.getBindings(ScriptContext.ENGINE_SCOPE);
      engine.setContext(context);

      // register variable for result
      engineScope.put("rendered", null);
      // pass the route
      engineScope.put("route", route);
      // pass the state
      engineScope.put("state", jsonState);

      // set env
      engine.eval(
          "'var process = { env: { VUE_ENV: \"server\", NODE_ENV: \"production\" }}; this.global = { process: process };'",
          context);

      // load the app
      engine.eval(read("server.umd.js"), context);

      // execute
      logger.debug(stringWriter.toString());

      // get rendered variable and return as string
      return context.getAttribute("rendered").toString();
    } catch (Exception e) {
      e.printStackTrace();
      logger.debug(stringWriter.toString());
      throw new MolgenisException("internal error: ", e);
    }
  }

  // helper to read the js files
  // expects all file to live in public_html/ssr/*
  private static Reader read(String path) throws IOException {
    MyClassPathReader handler = new MyClassPathReader("public_html");
    AbstractFileResolvingResource resource = handler.getResource("/apps/ssr/" + path);
    return new InputStreamReader(resource.getInputStream());
  }

  // reusing SparkJava classpath reading framework: unfortunately they made the method we need
  // protected)
  // so we read from all dependencies instead only current jar
  // and thus we can access the 'apps' contents
  private static class MyClassPathReader extends ClassPathResourceHandler {
    public MyClassPathReader(String baseResource) {
      super(baseResource);
    }

    public AbstractFileResolvingResource getResource(String path) throws MalformedURLException {
      return super.getResource(path);
    }
  }
}
