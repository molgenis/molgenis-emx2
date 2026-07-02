package org.molgenis.emx2.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.EnvironmentAccess;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;
import org.molgenis.emx2.MolgenisException;

public class JavaScriptUtils {

  private static final Engine engine =
      Engine.newBuilder()
          .allowExperimentalOptions(true)
          .option("engine.WarnInterpreterOnly", "false")
          .build();

  private static final HostAccess SAFE_HOST_ACCESS =
      HostAccess.newBuilder()
          .allowMapAccess(true)
          .allowListAccess(true)
          .allowArrayAccess(true)
          .allowIterableAccess(true)
          .allowIteratorAccess(true)
          .allowBigIntegerNumberAccess(true)
          .allowAccessAnnotatedBy(HostAccess.Export.class)
          .build();

  private JavaScriptUtils() {
    // hide constructor
  }

  public static Object executeJavascript(String script) {
    return executeJavascript(script, Object.class);
  }

  public static Object executeJavascript(String script, Class clazz) {
    return executeJavascriptOnMap(script, null, clazz);
  }

  public static Object executeJavascriptOnMap(String script, Map<String, Object> values) {
    return executeJavascriptOnMap(script, values, Object.class);
  }

  public static Object executeJavascriptOnMap(
      String script, Map<String, Object> values, Class clazz) {
    try {
      final Context context =
          Context.newBuilder("js")
              .allowHostAccess(SAFE_HOST_ACCESS)
              .allowHostClassLookup(className -> false)
              .allowHostClassLoading(false)
              .allowCreateProcess(false)
              .allowCreateThread(false)
              .allowNativeAccess(false)
              .allowIO(IOAccess.NONE)
              .allowEnvironmentAccess(EnvironmentAccess.NONE)
              .engine(engine)
              .build();
      Value bindings = context.getBindings("js");
      if (values != null) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
          Object value = entry.getValue();
          if (value != null
              && (value.getClass() == LocalDateTime.class || value.getClass() == LocalDate.class)) {
            bindings.putMember(entry.getKey(), value.toString());
          } else {
            bindings.putMember(entry.getKey(), value);
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
