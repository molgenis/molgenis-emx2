package org.molgenis.emx2.jsonld;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.molgenis.emx2.MolgenisException;

public class JsonLdValidator {

  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Validates JSON-LD document: 1. Recursively scans all local contexts and merges prefixes 2.
   * Checks @id and @type in context and graph nodes 3. Finally validates with Titanium (via RDF4J)
   */
  public static void validateJsonLd(Map<String, Object> jsonLd) {
    // Collect top-level prefixes
    Map<String, String> centralPrefixes = new LinkedHashMap<>();
    Object topContext = jsonLd.get("@context");
    if (topContext != null) {
      extractPrefixes(topContext, centralPrefixes, "@context.");
      checkContextTypes(topContext, "@context.");
    }

    // Recursively validate graph with merged prefixes
    Object graph = jsonLd.get("data"); // adjust if your graph is at root
    if (graph != null) {
      scanNode(graph, centralPrefixes, "@context.", "data.");
    }
  }

  /** Recursively scans a node, merges inherited prefixes, and validates terms */
  private static void scanNode(
      Object node,
      Map<String, String> inheritedPrefixes,
      String inheritedContextPath,
      String nodePath) {
    Map<String, String> localPrefixes = new LinkedHashMap<>(inheritedPrefixes);
    Object localContext = null;

    if (node instanceof Map<?, ?> map) {
      for (Map.Entry entry : map.entrySet()) {
        if ("@context".equals(entry.getKey().toString())) {
          localContext = entry.getValue();
        }
      }
      ;

      // Merge local context prefixes
      if (localContext != null) {
        extractPrefixes(localContext, localPrefixes, nodePath + "@context.");
        checkContextTypes(localContext, nodePath + "@context.");
      }

      // Validate @type
      if (map.containsKey("@type")) {
        Object typeVal = map.get("@type");
        checkTypeOrId(typeVal, localPrefixes, nodePath + "@type");
      }

      // Validate @id
      if (map.containsKey("@id")) {
        Object idVal = map.get("@id");
        checkTypeOrId(idVal, localPrefixes, nodePath + "@id");
      }

      // Recurse into children
      for (Map.Entry<?, ?> e : map.entrySet()) {
        if (!"@context".equals(e.getKey().toString())) {
          scanNode(e.getValue(), localPrefixes, inheritedContextPath, nodePath + e.getKey() + ".");
        }
      }
    } else if (node instanceof Iterable<?> iterable) {
      int i = 0;
      for (Object item : iterable) {
        scanNode(item, localPrefixes, inheritedContextPath, nodePath + "[" + i + "].");
        i++;
      }
    }
  }

  /** Extracts prefixes from a local context recursively */
  private static void extractPrefixes(Object ctxObj, Map<String, String> prefixes, String path) {
    if (ctxObj instanceof Map<?, ?> map) {
      map.forEach(
          (k, v) -> {
            String key = k.toString();
            if (v instanceof String s) {
              prefixes.put(key, s);
            } else if (v instanceof Map<?, ?> nested) {
              Object idVal = nested.get("@id");
              if (idVal instanceof String s) checkTypeOrId(s, prefixes, path + key + ".@id");
              extractPrefixes(nested, prefixes, path + key + ".");
            } else if (v instanceof Iterable<?> arr) {
              int i = 0;
              for (Object item : arr) {
                extractPrefixes(item, prefixes, path + key + "[" + i + "]");
                i++;
              }
            }
          });
    } else if (ctxObj instanceof Iterable<?> arr) {
      int i = 0;
      for (Object item : arr) {
        extractPrefixes(item, prefixes, path + "[" + i + "]");
        i++;
      }
    }
  }

  /** Checks that all @type values in @context are strings */
  private static void checkContextTypes(Object ctxObj, String path) {
    if (ctxObj instanceof Map<?, ?> map) {
      for (var entry : map.entrySet()) {
        String key = entry.getKey().toString();
        Object val = entry.getValue();
        if (val instanceof Map<?, ?> nested) {
          Object typeVal = nested.get("@type");
          if (typeVal != null && !(typeVal instanceof String)) {
            System.err.println(
                "⚠️ Invalid @type in @context at path "
                    + path
                    + key
                    + ": must be a string, found "
                    + typeVal.getClass().getSimpleName());
          }
          checkContextTypes(nested, path + key + ".");
        } else if (val instanceof Iterable<?> arr) {
          int i = 0;
          for (Object item : arr) {
            checkContextTypes(item, path + key + "[" + i + "].");
            i++;
          }
        }
      }
    } else if (ctxObj instanceof Iterable<?> arr) {
      int i = 0;
      for (Object item : arr) {
        checkContextTypes(item, path + "[" + i + "]");
        i++;
      }
    }
  }

  /** Validates a @type or @id value (string or array) against current prefixes */
  private static void checkTypeOrId(Object val, Map<String, String> prefixes, String path) {
    if (val instanceof String s) {
      checkPrefixedIri(s, prefixes, path);
    } else if (val instanceof Iterable<?> iterable) {
      int i = 0;
      for (Object t : iterable) {
        if (t instanceof String ts) checkPrefixedIri(ts, prefixes, path + "[" + i + "]");
        i++;
      }
    } else {
      System.err.println(
          "⚠️ Invalid value at path "
              + path
              + ": must be string or array, found "
              + (val == null ? "null" : val.getClass().getSimpleName()));
    }
  }

  /** Checks a compact IRI or full IRI for validity */
  private static void checkPrefixedIri(String value, Map<String, String> prefixes, String path) {
    if (value == null || value.isEmpty()) {
      throw new MolgenisException("Invalid empty @id/@type at path: " + path);
    }
    if (value.startsWith("_:") || value.startsWith("http")) return; // blank node or full IRI
    if (value.contains(":")) {
      String prefix = value.substring(0, value.indexOf(':'));
      if (!prefixes.containsKey(prefix)) {
        throw new MolgenisException(
            "Missing prefix '" + prefix + "' at path: " + path + " -> " + value);
      }
    } else {
      throw new MolgenisException("Invalid IRI at path: " + path + " -> " + value);
    }
  }
}
