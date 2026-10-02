package org.molgenis.emx2.fairmapper.pipeline.definition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.molgenis.emx2.MolgenisException;

/**
 * Reads a pipeline file into a {@link PipelineDefinition}. Only reads and validates the file; no
 * network calls are made and no components are created.
 *
 * <p>The stages are listed under {@code steps}, each entry naming one stage. Each stage may appear
 * at most once. For now only the {@code postprocessing} stage is supported. A post-processor
 * without arguments can be written as a bare name:
 *
 * <pre>
 * steps:
 *   - postprocessing:
 *       - coalesce-field: { table: Collections, field: id, derive-from: [acronym, name] }
 *       - resolve-ontologies
 * </pre>
 */
public final class PipelineFile {

  private static final String STEPS = "steps";
  private static final String POST_PROCESSING = "postprocessing";

  private static final JsonMapper MAPPER =
      JsonMapper.builder(new YAMLFactory())
          .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
          .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .build();

  private PipelineFile() {}

  public static PipelineDefinition read(Path path) {
    try {
      return parse(Files.readString(path));
    } catch (IOException e) {
      throw new MolgenisException("Unable to read pipeline file: " + path, e);
    }
  }

  public static PipelineDefinition parse(String yaml) {
    try {
      return toDefinition(MAPPER.readTree(yaml));
    } catch (JsonProcessingException e) {
      throw invalid(e);
    }
  }

  private static PipelineDefinition toDefinition(JsonNode root) throws JsonProcessingException {
    if (root == null || !root.isObject()) {
      throw new MolgenisException("Invalid pipeline file: expected a map at the top level");
    }
    root.fieldNames()
        .forEachRemaining(
            name -> {
              if (!STEPS.equals(name)) {
                throw new MolgenisException("Invalid pipeline file: unknown field '" + name + "'");
              }
            });

    List<PostProcessorSpec> postProcessors = List.of();
    for (Map.Entry<String, JsonNode> stage : stages(root.path(STEPS))) {
      if (POST_PROCESSING.equals(stage.getKey())) {
        postProcessors =
            MAPPER.treeToValue(expandBareNames(stage.getValue()), new TypeReference<>() {});
        postProcessors.forEach(PostProcessorSpec::validate);
      } else {
        throw new MolgenisException(
            "Invalid pipeline file: unknown stage '" + stage.getKey() + "'");
      }
    }
    return new PipelineDefinition(postProcessors);
  }

  private static List<Map.Entry<String, JsonNode>> stages(JsonNode steps) {
    if (!steps.isArray()) {
      throw new MolgenisException("Invalid pipeline file: 'steps' must be a list");
    }
    Set<String> seen = new HashSet<>();
    return steps
        .valueStream()
        .map(
            step -> {
              if (!step.isObject() || step.size() != 1) {
                throw new MolgenisException(
                    "Invalid pipeline file: each step must name exactly one stage, got: " + step);
              }
              Map.Entry<String, JsonNode> stage = step.properties().iterator().next();
              if (!seen.add(stage.getKey())) {
                throw new MolgenisException(
                    "Invalid pipeline file: stage '"
                        + stage.getKey()
                        + "' is listed more than once");
              }
              return stage;
            })
        .toList();
  }

  /**
   * Rewrites each bare name in {@code list} (e.g. {@code resolve-ontologies}) to a name without
   * arguments ({@code resolve-ontologies: {}}), the form Jackson expects for a named type.
   */
  private static JsonNode expandBareNames(JsonNode list) {
    if (!list.isArray()) {
      return list;
    }
    ArrayNode expanded = MAPPER.createArrayNode();
    for (JsonNode entry : list) {
      if (entry.isTextual()) {
        ObjectNode named = MAPPER.createObjectNode();
        named.putObject(entry.asText());
        expanded.add(named);
      } else {
        expanded.add(entry);
      }
    }
    return expanded;
  }

  private static MolgenisException invalid(JsonProcessingException e) {
    return new MolgenisException("Invalid pipeline file: " + e.getOriginalMessage(), e);
  }
}
