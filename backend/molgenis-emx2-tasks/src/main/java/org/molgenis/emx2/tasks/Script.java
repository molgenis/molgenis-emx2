package org.molgenis.emx2.tasks;

import java.util.Map;
import java.util.Objects;

public class Script {
  public enum Language {
    PYTHON
  };

  private String script;
  private Language language;
  private Map<String, Object> parameters;

  public Script(Language language, String script, Map<String, Object> parameters) {
    Objects.requireNonNull(language);
    Objects.requireNonNull(script);
    this.language = language;
    this.script = script;
    this.parameters = parameters;
  }
}
