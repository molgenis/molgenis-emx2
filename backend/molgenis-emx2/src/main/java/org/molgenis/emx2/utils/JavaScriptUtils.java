package org.molgenis.emx2.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Supplier;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.molgenis.emx2.MolgenisException;

public class JavaScriptUtils {

  private static final Engine engine =
      Engine.newBuilder()
          .allowExperimentalOptions(true)
          .option("engine.WarnInterpreterOnly", "false")
          .build();

  private JavaScriptUtils() {
    // hide constructor
  }

  public static Object executeJavascript(String script) {
    return executeJavascript(script, Object.class);
  }

  public static Object executeJavascript(String script, Class clazz) {
    return executeJavascriptOnMap(script, null, null, clazz);
  }

  public static Object executeJavascriptOnMap(
      String script, Map<String, Object> values, Class clazz) {
    return executeJavascriptOnMap(script, values, null, clazz);
  }

  public static Object executeJavascriptOnMap(String script, Map<String, Object> values) {
    return executeJavascriptOnMap(script, values, null, Object.class);
  }

  public static Object executeJavascriptOnMap(
      String script, Map<String, Object> values, Map<String, Supplier<Object>> bindings) {
    return executeJavascriptOnMap(script, values, bindings, Object.class);
  }

  public static Object executeJavascriptOnMap(
      String script,
      Map<String, Object> values,
      Map<String, Supplier<Object>> bindings,
      Class clazz) {
    try {
      final Context context =
          Context.newBuilder("js")
              .allowHostAccess(HostAccess.newBuilder(HostAccess.ALL).build())
              .engine(engine)
              .build();

      // todo: do this dynamically, only when simple simplePostClient is in script
      values.put("simplePostClient", bindings.get("simplePostClient").get());

      Value jsBindings = context.getBindings("js");
      if (values != null) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
          Object value = entry.getValue();
          if (value != null
              && (value.getClass() == LocalDateTime.class || value.getClass() == LocalDate.class)) {
            jsBindings.putMember(entry.getKey(), value.toString());
          } else {
            jsBindings.putMember(entry.getKey(), value);
          }
        }
      }

      String scriptWithFixedRegex = script.replace("\\\\", "\\");
      return context.eval("js", scriptWithFixedRegex).as(clazz);
    } catch (Exception e) {
      throw new MolgenisException("script failed: " + e.getMessage());
    }
  }
}
