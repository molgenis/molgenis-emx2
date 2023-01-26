package org.molgenis.emx2.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.script.*;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;

public class JavaScriptUtils {

  private static ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");
  private static Bindings valueBindings = engine.createBindings();
  private static Map<String, CompiledScript> cache = new LinkedHashMap<>();

  private JavaScriptUtils() {
    // hide constructor
  }

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
      // ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine("--language=es6");
      Bindings bindings = engine.createBindings();
      for (Map.Entry<String, Object> col : map.entrySet()) {
        bindings.put(col.getKey(), col.getValue());
      }
      return engine.eval(script, bindings).toString();
    } catch (Exception e) {
      throw new MolgenisException("Compute value failed on script [" + script + "]", e);
    }
  }

  public static String executeJavascriptOnRow(String script, Row row) {
    try {
      // ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine("--language=es6");
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
