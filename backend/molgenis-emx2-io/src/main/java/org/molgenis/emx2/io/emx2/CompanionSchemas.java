package org.molgenis.emx2.io.emx2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Extracts the {@code schemas:} companion declarations from a single-file model upload. On the wire
 * a model is a single YAML document, so companion content must be inlined; a {@code bundle:} path
 * reference cannot be resolved server-side and is carried as {@link CompanionDeclaration#bundleRef}
 * for the apply layer to reject. Cycle and member checks stay in {@link BundleValidator}; this
 * helper only turns each inline companion body into a parsed {@link Emx2YamlBundle}.
 */
public final class CompanionSchemas {

  private static final String MOLGENIS_YAML = "molgenis.yaml";
  private static final String KEY_SCHEMAS = "additionalSchemas";
  private static final String KEY_BUNDLE = "bundle";
  private static final String KEY_PERMISSIONS = "permissions";

  public record CompanionDeclaration(
      String name, String bundleRef, Emx2YamlBundle model, Map<String, String> permissions) {

    public boolean hasInlineModel() {
      return model != null;
    }
  }

  private CompanionSchemas() {}

  @SuppressWarnings("unchecked")
  public static List<CompanionDeclaration> fromSingleFile(String rootContent) {
    List<CompanionDeclaration> declarations = new ArrayList<>();
    Object loaded = new Yaml().load(rootContent);
    if (!(loaded instanceof Map)) {
      return declarations;
    }
    Object schemasNode = ((Map<String, Object>) loaded).get(KEY_SCHEMAS);
    if (!(schemasNode instanceof Map)) {
      return declarations;
    }
    for (Map.Entry<String, Object> entry : ((Map<String, Object>) schemasNode).entrySet()) {
      declarations.add(toDeclaration(entry.getKey(), entry.getValue()));
    }
    return declarations;
  }

  @SuppressWarnings("unchecked")
  private static CompanionDeclaration toDeclaration(String name, Object rawBody) {
    if (!(rawBody instanceof Map)) {
      return new CompanionDeclaration(name, null, null, Map.of());
    }
    Map<String, Object> body = new LinkedHashMap<>((Map<String, Object>) rawBody);
    Map<String, String> permissions = readPermissions(body.remove(KEY_PERMISSIONS));
    Object bundleRef = body.remove(KEY_BUNDLE);
    Emx2YamlBundle model =
        body.isEmpty() ? null : Emx2Yaml.fromBundleFiles(Map.of(MOLGENIS_YAML, dump(body)));
    return new CompanionDeclaration(
        name, bundleRef == null ? null : bundleRef.toString(), model, permissions);
  }

  private static Map<String, String> readPermissions(Object node) {
    Map<String, String> permissions = new LinkedHashMap<>();
    if (node instanceof Map<?, ?> map) {
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        permissions.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
      }
    }
    return permissions;
  }

  private static String dump(Object data) {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    return new Yaml(options).dump(data);
  }
}
