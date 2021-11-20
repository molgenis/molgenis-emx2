// Acknowledgement to SakaiBorder for providing the template for this code

package org.molgenis.emx2.web;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import javax.script.*;
import org.molgenis.emx2.MolgenisException;

public class SsrService {

  public static String SSR_BASE_PATH = "public_html/ssr/";

  public static String renderRoute(String route, Object state) {
    try {
      // we use graalvm to render the js
      ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");
      ScriptContext context = new SimpleScriptContext();

      // create a variable to get the rendered result (is this necessary?)
      context.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
      Bindings engineScope = context.getBindings(ScriptContext.ENGINE_SCOPE);
      engine.setContext(context);

      // register variable for result
      engineScope.put("rendered", null);
      // pass the route
      engineScope.put("route", route);
      // pass the state
      engineScope.put("state", state);

      // tell vue it is in production, server side
      engine.eval(
          "var process = { env: { VUE_ENV: 'server', NODE_ENV: 'production' }}; this.global = { process: process };",
          context);

      // execute the app
      engine.eval(read("server.umd.js"), context);

      // get rendered variable and return as string
      return context.getAttribute("rendered").toString();
    } catch (Exception e) {
      throw new MolgenisException("internal error: ", e);
    }
  }
  // helper to read the js files
  // expects all file to live in public_html/ssr/*
  private static Reader read(String path) {
    InputStream in = SsrService.class.getClassLoader().getResourceAsStream(SSR_BASE_PATH + path);
    return new InputStreamReader(in);
  }
}
