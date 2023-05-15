package org.molgenis.emx2.utils;

import java.util.Map;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.utils.generator.IdGenerator;

public class JavaScriptUtils {

  private static final Engine engine =
      Engine.newBuilder()
          .allowExperimentalOptions(true)
          .option("engine.WarnInterpreterOnly", "false")
          .build();

  private final IdGenerator idGenerator;

  public JavaScriptUtils(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
  }

  public String executeJavascriptOnMap(final String script, Map<String, Object> values) {
    try {
      final Context context =
          Context.newBuilder("js")
              .allowHostAccess(
                  HostAccess.newBuilder()
                      .allowArrayAccess(true)
                      .allowListAccess(true)
                      .allowMapAccess(true)
                      .build())
              .engine(engine)
              .build();
      Value bindings = context.getBindings("js");
      for (Map.Entry<String, Object> entry : values.entrySet()) {
        bindings.putMember(entry.getKey(), entry.getValue());
      }

      if (script.contains("${mg_autoid}")) {
        String idScript = script.replace("${mg_autoid}", "\"" + idGenerator.generateId() + "\"");
        return context.eval("js", idScript).toString();
      } else {
        return context.eval("js", script).toString();
      }

    } catch (Exception e) {
      throw new MolgenisException("script failed: " + e.getMessage());
    }
  }
}
