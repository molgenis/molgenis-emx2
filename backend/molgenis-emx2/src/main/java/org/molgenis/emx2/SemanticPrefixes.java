package org.molgenis.emx2;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.util.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.molgenis.emx2.Constants.SETTING_SEMANTIC_PREFIXES;

/**
 * Class that contains the needed code to process the semantic field into a usable object.
 * It keeps account for both any schema-specific custom prefixes if a sequence path is used.
 * <br><br>Examples of allowed values for the semantic field:
 * <ul>
 *   <li><pre><code><http://example.com/first>/<http://example.com/second></code></pre></li>
 *   <li><pre><code>myPrefix:first/myPrefix:second</code></pre></li>
 *   <li><pre><code><http://example.com/first>/myPrefix:second</code></pre></li>
 * </ul>
 */
public class SemanticPrefixes {
  private static final Logger logger = LoggerFactory.getLogger(SemanticPrefixes.class);

  private static final String SEMANTIC_PREFIXES_NAME_PREFIX = "prefix";
  private static final String SEMANTIC_PREFIXES_NAME_IRI = "iri";

  private static final Map<String, Namespace> DEFAULT_NAMESPACES_MAP =
    DefaultNamespace.streamAll().collect(Collectors.toMap(Namespace::getPrefix, i -> i));

  private static final Set<Namespace> DEFAULT_NAMESPACES_SET =
    DefaultNamespace.streamAll().collect(Collectors.toUnmodifiableSet());

  private static final CsvSchema SEMANTIC_PREFIXES_CSV_SCHEMA =
    CsvSchema.builder()
      .addColumn(SEMANTIC_PREFIXES_NAME_PREFIX)
      .addColumn(SEMANTIC_PREFIXES_NAME_IRI)
      .build();

  // Matches per IRI/prefixed name in semantic field, NOT full field.
  public static final Pattern SEMANTIC_PATTERN = Pattern.compile("(<.*?>|.*?:.*?)(/|$)");

  // TreeMap for consistency in case it's used for generating output
  private final Map<String, Namespace> namespaces;

  public SemanticPrefixes(Schema schema) {
    this(schema.getMetadata());
  }

  public SemanticPrefixes(SchemaMetadata schemaMetadata) {
    Map<String, Namespace> namespaces;
    if (!schemaMetadata.getSettings().containsKey(SETTING_SEMANTIC_PREFIXES)) {
      namespaces = DEFAULT_NAMESPACES_MAP;
    } else {
      try {
        Set<Namespace> namespaceSet = getCustomPrefixes(schemaMetadata);
        namespaces = namespaceSet.stream().collect(Collectors.toMap(Namespace::getPrefix, i -> i));
      } catch (IOException e) {
        logger.error("Failed to retrieve custom prefixes, falling back to default", e);
        namespaces = DEFAULT_NAMESPACES_MAP;
      }
    }
    this.namespaces = namespaces;
  }

  private static Set<Namespace> getCustomPrefixes(SchemaMetadata schema) throws IOException {
    Set<Namespace> namespaces = new HashSet<>();
    try (MappingIterator<Map<String, String>> iterator =
           new CsvMapper()
             .readerForMapOf(String.class)
             .with(SEMANTIC_PREFIXES_CSV_SCHEMA)
             .readValues(schema.getSetting(SETTING_SEMANTIC_PREFIXES))) {

      iterator.forEachRemaining(
        i -> {
          Namespace namespace = Values.namespace(
            i.get(SEMANTIC_PREFIXES_NAME_PREFIX), i.get(SEMANTIC_PREFIXES_NAME_IRI));
          try {
            Values.iri(namespace, "test");
          } catch (Exception e) {
            throw new MolgenisException("Unusable IRI found in custom_prefixes of %s: %s,%s".formatted(schema.getName(), namespace.getPrefix(), namespace.getName()));
          }
          namespaces.add(namespace);
        });
    }
    return namespaces;
  }

  public Set<Namespace> getAllNamespaces() {
    return Set.copyOf(namespaces.values());
  }

  private <R> List<R> map(final String semantic, Function<String,R> iriOperator, Function<String,R> prefixedNameOperator) {
    List<R> sequencePath = new ArrayList<>();

    Matcher matcher = SEMANTIC_PATTERN.matcher(semantic);
    while (matcher.find()) {
      String semanticPart  = matcher.group(1);
      if(semanticPart.startsWith("<")){
        sequencePath.add(iriOperator.apply(semanticPart.substring(1,semanticPart.length()-2)));
      } else {
        sequencePath.add(prefixedNameOperator.apply(semanticPart));
      }
    }
    return sequencePath;
  }

  public List<IRI> map(final String semantic) {
    return map(semantic, Values::iri, prefixedName -> {
      String[] prefixedNameSplit = prefixedName.split(":");
      return Values.iri(namespaces.get(prefixedNameSplit[0]), prefixedNameSplit[1]);
    });
  }

  public List<String> mapAsPrefixedName(final String semantic) {
    return map(semantic, iri -> {
      Namespace foundNamespace = namespaces.values().stream().filter(namespace -> iri.startsWith(namespace.getName())).findFirst().orElse(null);
      return (foundNamespace == null ? "<%s>".formatted(iri) : "%s:%s".formatted(foundNamespace.getPrefix(), iri.substring(foundNamespace.getName().length())));
    }, String::toString);
  }
}
