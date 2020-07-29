package org.molgenis.emx2.sql;

import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;

import javax.script.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SqlJavascriptValidator {

  private SqlJavascriptValidator() {
    // hide constructor
  }

  private static ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
  private static Bindings rowBindings = engine.createBindings();
  private static Bindings valueBindings = engine.createBindings();

  private static Map<String, CompiledScript> cache = new LinkedHashMap<>();

  static String validateValue(String script, Object value) {
    try {
      valueBindings.put("value", value);
      return execute(script, valueBindings);
    } catch (ScriptException e) {
      throw new MolgenisException("Validation system failed", e);
    }
  }

  static String validate(String script, Row row) {
    try {
      rowBindings.put("row", row.getValueMap());
      return execute(script, rowBindings);
    } catch (ScriptException e) {
      throw new MolgenisException("Validation system failed", e);
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
