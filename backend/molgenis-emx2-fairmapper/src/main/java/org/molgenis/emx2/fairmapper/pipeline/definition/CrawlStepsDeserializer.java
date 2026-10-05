package org.molgenis.emx2.fairmapper.pipeline.definition;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.util.Values;
import org.molgenis.emx2.DefaultNamespace;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.fairmapper.extractors.CrawlStep;
import org.molgenis.emx2.fairmapper.extractors.CrawlSteps;

/**
 * Reads the {@code crawl} option of the Extract stage. It is either the name of a crawl preset or a
 * list of crawl steps, each with a {@code name} and a {@code predicate}:
 *
 * <pre>
 * crawl: fdp
 *
 * crawl:
 *   - { name: catalog, predicate: fdp-o:metadataCatalog }
 *   - { name: dataset, predicate: dcat:dataset }
 * </pre>
 *
 * <p>A predicate is a full IRI ({@code http(s):...} or any IRI between {@code <} and {@code >}) or
 * a prefixed name using one of the {@link DefaultNamespace} prefixes.
 */
class CrawlStepsDeserializer extends JsonDeserializer<List<CrawlStep>> {

  private static final Set<String> STEP_FIELDS = Set.of("name", "predicate");

  private static final Map<String, Namespace> NAMESPACES =
      DefaultNamespace.streamAll()
          .collect(Collectors.toMap(Namespace::getPrefix, namespace -> namespace));

  @Override
  public List<CrawlStep> deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    JsonNode node = parser.readValueAsTree();
    try {
      if (node.isTextual()) {
        return preset(node.asText());
      }
      if (node.isArray()) {
        List<CrawlStep> steps = new ArrayList<>();
        node.forEach(step -> steps.add(step(step)));
        return steps;
      }
      throw new MolgenisException("crawl must be a crawl preset name or a list of crawl steps");
    } catch (MolgenisException e) {
      throw InvalidFormatException.from(parser, e.getMessage(), node, List.class);
    }
  }

  private static List<CrawlStep> preset(String name) {
    return Arrays.stream(CrawlSteps.values())
        .filter(preset -> preset.name().toLowerCase(Locale.ROOT).equals(name))
        .findFirst()
        .map(CrawlSteps::steps)
        .orElseThrow(
            () ->
                new MolgenisException(
                    "unknown crawl preset '"
                        + name
                        + "'; known presets: "
                        + String.join(
                            ", ",
                            Arrays.stream(CrawlSteps.values())
                                .map(preset -> preset.name().toLowerCase(Locale.ROOT))
                                .toList())));
  }

  private static CrawlStep step(JsonNode step) {
    if (!step.isObject() || !step.path("name").isTextual() || !step.path("predicate").isTextual()) {
      throw new MolgenisException("each crawl step requires a name and a predicate, got: " + step);
    }
    step.fieldNames()
        .forEachRemaining(
            field -> {
              if (!STEP_FIELDS.contains(field)) {
                throw new MolgenisException("unknown crawl step option '" + field + "'");
              }
            });
    return new CrawlStep(step.get("name").asText(), predicate(step.get("predicate").asText()));
  }

  private static IRI predicate(String value) {
    try {
      if (value.startsWith("<") && value.endsWith(">")) {
        return Values.iri(value.substring(1, value.length() - 1));
      }
      if (value.startsWith("http:") || value.startsWith("https:")) {
        return Values.iri(value);
      }
    } catch (IllegalArgumentException e) {
      throw new MolgenisException("'" + value + "' is not a valid IRI");
    }
    int colon = value.indexOf(':');
    Namespace namespace = colon > 0 ? NAMESPACES.get(value.substring(0, colon)) : null;
    if (namespace == null || colon == value.length() - 1) {
      throw new MolgenisException(
          "'" + value + "' is not a full IRI nor a prefixed name with a known prefix");
    }
    return Values.iri(namespace, value.substring(colon + 1));
  }
}
