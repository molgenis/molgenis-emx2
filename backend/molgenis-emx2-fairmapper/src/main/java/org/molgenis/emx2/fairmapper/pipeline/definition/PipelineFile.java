package org.molgenis.emx2.fairmapper.pipeline.definition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
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
 * <pre>
 * schema: catalogue
 * tables: [Catalogues, Collections]
 * output: ./output-dir              # optional
 * emx2:
 *   endpoint: https://emx2.example.org
 *   token: my-token
 * steps:
 *   - extract: { url: https://fdp.example.org, crawl: fdp }
 *   - preprocessing: [temporal, typical-age]
 *   - transform
 *   - postprocessing:
 *       - coalesce-field: { table: Collections, field: id, derive-from: [acronym, name] }
 *       - resolve-ontologies
 *   - upload
 * </pre>
 *
 * <p>Each entry under {@code steps} names one stage. A stage may appear at most once, and the order
 * in which the stages are listed doesn't matter. The Extract and Transform stages are required.
 * Stages, pre-processors and post-processors without options can be written as a bare name.
 */
public final class PipelineFile {

  private static final String STEPS = "steps";
  private static final String EXTRACT = "extract";
  private static final String PRE_PROCESSING = "preprocessing";
  private static final String TRANSFORM = "transform";
  private static final String POST_PROCESSING = "postprocessing";
  private static final String UPLOAD = "upload";

  private static final JsonMapper MAPPER =
      JsonMapper.builder(new YAMLFactory())
          .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
          .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .build();

  /** Top-level fields of a pipeline file, everything except {@code steps}. */
  private record Settings(String schema, List<String> tables, String output, Emx2Settings emx2) {

    void validate() {
      if (schema == null) {
        throw new MolgenisException("Invalid pipeline file: schema is required");
      }
      if (tables == null || tables.isEmpty()) {
        throw new MolgenisException("Invalid pipeline file: tables must list at least one table");
      }
      if (emx2 == null) {
        throw new MolgenisException("Invalid pipeline file: emx2 is required");
      }
      emx2.validate();
    }
  }

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
    ObjectNode settingsNode = root.deepCopy();
    settingsNode.remove(STEPS);
    Settings settings = MAPPER.treeToValue(settingsNode, Settings.class);
    settings.validate();

    ExtractSpec extract = null;
    List<PreProcessorSpec> preProcessors = List.of();
    boolean transform = false;
    List<PostProcessorSpec> postProcessors = List.of();
    boolean upload = false;

    for (Map.Entry<String, JsonNode> stage : stages(root.path(STEPS))) {
      JsonNode options = stage.getValue();
      switch (stage.getKey()) {
        case EXTRACT -> {
          extract = MAPPER.treeToValue(objectOrEmpty(options), ExtractSpec.class);
          extract.validate();
        }
        case PRE_PROCESSING -> preProcessors = specs(options, new TypeReference<>() {});
        case TRANSFORM -> transform = withoutOptions(stage);
        case POST_PROCESSING -> {
          postProcessors = specs(options, new TypeReference<>() {});
          postProcessors.forEach(PostProcessorSpec::validate);
        }
        case UPLOAD -> upload = withoutOptions(stage);
        default ->
            throw new MolgenisException(
                "Invalid pipeline file: unknown stage '" + stage.getKey() + "'");
      }
    }

    if (extract == null) {
      throw new MolgenisException("Invalid pipeline file: the extract stage is required");
    }
    if (!transform) {
      throw new MolgenisException("Invalid pipeline file: the transform stage is required");
    }

    return new PipelineDefinition(
        settings.schema(),
        settings.tables(),
        settings.output(),
        settings.emx2(),
        extract,
        preProcessors,
        postProcessors,
        upload);
  }

  /** Lists the stages under {@code steps} as name and options, the options being null if none. */
  private static List<Map.Entry<String, JsonNode>> stages(JsonNode steps) {
    if (!steps.isArray()) {
      throw new MolgenisException("Invalid pipeline file: 'steps' must be a list");
    }
    Set<String> seen = new HashSet<>();
    return steps
        .valueStream()
        .map(
            step -> {
              Map.Entry<String, JsonNode> stage;
              if (step.isTextual()) {
                stage = Map.entry(step.asText(), NullNode.getInstance());
              } else if (step.isObject() && step.size() == 1) {
                stage = step.properties().iterator().next();
              } else {
                throw new MolgenisException(
                    "Invalid pipeline file: each step must name exactly one stage, got: " + step);
              }
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

  private static <T> List<T> specs(JsonNode options, TypeReference<List<T>> type)
      throws JsonProcessingException {
    if (options.isNull()) {
      return List.of();
    }
    return MAPPER.treeToValue(expandBareNames(options), type);
  }

  private static boolean withoutOptions(Map.Entry<String, JsonNode> stage) {
    JsonNode options = stage.getValue();
    if (!options.isNull() && !(options.isObject() && options.isEmpty())) {
      throw new MolgenisException(
          "Invalid pipeline file: stage '" + stage.getKey() + "' takes no options");
    }
    return true;
  }

  private static JsonNode objectOrEmpty(JsonNode options) {
    return options.isNull() ? MAPPER.createObjectNode() : options;
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
