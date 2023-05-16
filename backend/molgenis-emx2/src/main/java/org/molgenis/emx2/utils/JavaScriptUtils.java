package org.molgenis.emx2.utils;

import java.util.Map;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.utils.generator.IdGenerator;
import org.molgenis.emx2.utils.generator.IdGeneratorImpl;

public class JavaScriptUtils {
  public static IdGenerator idGenerator = new IdGeneratorImpl();

  private static final Engine engine =
      Engine.newBuilder()
          .allowExperimentalOptions(true)
          .option("engine.WarnInterpreterOnly", "false")
          .build();

  private JavaScriptUtils() {
    // hide constructor
  }

  public static String executeJavascriptOnMap(String script, Map<String, Object> values) {
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
        return script.replace("${mg_autoid}", idGenerator.generateId());
      } else {
        return context.eval("js", script).toString();
      }

    } catch (Exception e) {
      throw new MolgenisException("script failed: " + e.getMessage());
    }
  }
}
