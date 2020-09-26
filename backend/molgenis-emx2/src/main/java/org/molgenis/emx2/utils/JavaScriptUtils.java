package org.molgenis.emx2.utils;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;

import javax.script.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class JavaScriptUtils {

  private JavaScriptUtils() {
    // hide constructor
  }

  private static ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");
  private static Context context = Context.newBuilder().build();

  private static Bindings rowBindings = engine.createBindings();
  private static Bindings valueBindings = engine.createBindings();

  private static Map<String, CompiledScript> cache = new LinkedHashMap<>();
  private static Map<String, Source> sourceCache = new LinkedHashMap<>();

  public static String executeJavascriptOnValue(String script, Object value) {
    try {
      valueBindings.put("value", value);
      return execute(script, valueBindings);
    } catch (ScriptException e) {
      throw new MolgenisException("Validation system failed", e);
    }
  }

  public static String executeJavascriptOnMap(String script, Map<String, Object> map) {
    try {
      //      if (sourceCache.get(script) == null) {
      //        sourceCache.put(script, Source.create("js", script));
      //      }

      //      Context context = Context.newBuilder("js").allowHostAccess(true).build();
      //      for (Map.Entry<String, Object> col : row.getValueMap().entrySet()) {
      //        context.getBindings("js").putMember(col.getKey(), col.getValue());
      //      }
      //      return context.eval("js", script).toString();

      ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine("--language=es6");
      Bindings bindings = engine.createBindings();
      for (String col : map.keySet()) {
        bindings.put(col, map.get(col));
      }
      return engine.eval(script, bindings).toString();
    } catch (Exception e) {
      throw new MolgenisException("Compute value failed on script [" + script + "]", e);
    }
  }

  public static String executeJavascriptOnRow(String script, Row row) {
    try {
      //      if (sourceCache.get(script) == null) {
      //        sourceCache.put(script, Source.create("js", script));
      //      }

      //      Context context = Context.newBuilder("js").allowHostAccess(true).build();
      //      for (Map.Entry<String, Object> col : row.getValueMap().entrySet()) {
      //        context.getBindings("js").putMember(col.getKey(), col.getValue());
      //      }
      //      return context.eval("js", script).toString();

      ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine("--language=es6");
      Bindings bindings = engine.createBindings();
      for (Map.Entry<String, Object> col : row.getValueMap().entrySet()) {
        bindings.put(col.getKey().replace("-", "$"), col.getValue());
      }
      return engine.eval(script, bindings).toString();
    } catch (Exception e) {
      throw new MolgenisException("Compute value failed on script [" + script + "]", e);
    }
  }

  private static String execute(String script, Bindings bindings) throws ScriptException {
    if (cache.get(script) == null) {
      cache.put(script, ((Compilable) engine).compile(script));
    }
    Object result = cache.get(script).eval(bindings);
    if (result != null) return result.toString();
    else return null;
  }
}
