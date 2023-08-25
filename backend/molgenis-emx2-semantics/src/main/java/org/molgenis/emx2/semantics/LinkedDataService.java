package org.molgenis.emx2.semantics;

import static org.molgenis.emx2.SelectColumn.s;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.molgenis.emx2.*;

public class LinkedDataService {
  private static final ObjectMapper jsonMapper =
      new ObjectMapper()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .setDateFormat(new StdDateFormat().withColonInTimeZone(true));

  private LinkedDataService() {
    // hidden
  }

  // naive implementation, need some kind of paging in the future, and column selection
  public static void getJsonLdForSchema(Schema schema, PrintWriter writer) {
    // iterate through meta model
    writer.append("[");
    boolean comma = false;
    for (Table t : schema.getTablesSorted()) {
      if (comma) {
        writer.append(",");
      } else {
        comma = true;
      }
      getJsonLdForTable(t, writer);
    }
    writer.append("]");
  }

  public static void getJsonLdForTable(Table table, PrintWriter writer) {
    try {
      // fixme: construct URL based on the actual server URL
      String path = "http://localhost/" + table.getSchema().getName() + "/";

      // define the selection
      // in particular, for references we check if there are columns tagged with 'id'
      // because then 'id' will be used as the @id of the reference, otherwise we will use primary
      // key
      // fixme: what happens when non-unique values are tagged as 'id' ?
      Query q = table.query();
      for (Column c : table.getMetadata().getColumns()) {
        if (c.isReference() && !c.isOntology()) {
          // check ref columns for id
          List<Column> refId =
              c.getRefTable().getColumns().stream()
                  .filter(
                      r ->
                          r.getSemantics() != null
                              && Arrays.asList(r.getSemantics()).contains("id"))
                  .toList();
          // if no with id, we use primary key
          if (refId.isEmpty()) {
            refId = c.getRefTable().getColumns().stream().filter(r -> r.getKey() == 1).toList();
          }
          // add to select
          for (var id : refId) {
            q.select(s(c.getName(), s(id.getName())));
          }
        } else if (c.isOntology()) {
          Column ontoRefCol =
              c.getRefTable().getColumns().stream()
                  .filter(r -> r.getName().equals("ontologyTermURI"))
                  .toList()
                  .get(0);
          q.select(s(c.getName(), s(ontoRefCol.getName())));
        } else {
          q.select(s(c.getName()));
        }
      }

      // we use the query here
      String json = q.retrieveJSON();
      Map<String, List<Map<String, Object>>> jsonMap = jsonMapper.readValue(json, Map.class);
      List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
      var jsonForTable = jsonMap.get(table.getIdentifier());
      if (jsonForTable != null) {
        data.addAll(jsonForTable);
        annotate(table, data, path);
      }

      // assemble the json-ld for this table
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("@context", createContext(table, path));
      result.put("@id", path + table.getName());
      result.put(table.getName(), data);

      writer.append(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
    } catch (Exception e) {
      throw new MolgenisException("jsonld export failed", e);
    }
  }

  private static Map<String, Object> createContext(Table table, String path) {
    Map<String, Object> context = new LinkedHashMap<>();
    context.put(table.getName(), path + table.getName());

    // is composition of type specific context elements
    for (Column c : table.getMetadata().getColumns()) {
      if (c.getSemantics() != null) {
        List<String> type =
            Arrays.stream(c.getSemantics())
                // we omit the 'id' keyword, we use this to pass URI for references
                // or use as object id
                .filter(s -> !"id".equals(s))
                .collect(Collectors.toList());

        if (type.size() > 1) {
          context.put(c.getName(), type);
        } else if (type.size() == 1) {
          context.put(c.getName(), type.get(0));
        }
      }
    }
    return context;
  }

  /**
   * Enrich the given map with JSON-LD semantic annotations.
   *
   * @param table the table that is represented in the data
   * @param data the data to annotate
   * @param path the path component of the default @id URL
   * @throws Exception when an ontology type does not match the expected type (should not happen)
   */
  private static void annotate(
      final Table table, final List<Map<String, Object>> data, final String path) throws Exception {
    for (var row : data) {
      if (table.getMetadata().getSemantics() != null) {
        List<String> type =
            Arrays.stream(table.getMetadata().getSemantics()).collect(Collectors.toList());
        if (type.size() > 1) {
          row.put("@type", type);
        } else if (type.size() == 1) {
          row.put("@type", type.get(0));
        }
      }
      for (Column c : table.getMetadata().getColumns()) {
        // check id
        if (c.getSemantics() != null && Arrays.asList(c.getSemantics()).contains("id")) {
          row.put("@id", row.get(c.getName()));
        }
        // flatten references
        if (c.isReference()) {
          Column temp = null;
          String prefixTemp = "";
          for (Column r : c.getRefTable().getColumns()) {
            if (r.getSemantics() != null && Arrays.asList(r.getSemantics()).contains("id")) {
              temp = r;
            }
          }
          if (temp == null) {
            temp = c.getRefTable().getPrimaryKeyColumns().get(0);
            prefixTemp = path + c.getRefTableName() + "/";
          }
          final Column ref = temp;
          final String prefix = prefixTemp;
          if (c.isRef() && !c.isOntology()) {
            var map = ((Map<String, Object>) row.get(c.getName()));
            if (map != null) {
              row.put(c.getName(), map.get(ref.getName()));
            }
          } else if (c.isOntology()) {
            Object rowObj = row.get(c.getName());
            if (rowObj == null) {
              row.put(c.getName(), null);
            } else if (rowObj instanceof List) {
              var listOfObjects = (List<Map<String, Object>>) row.get(c.getName());
              if (listOfObjects != null) {
                row.put(
                    c.getName(),
                    listOfObjects.stream()
                        .map(o -> prefix + o.get(ref.getName()))
                        .collect(Collectors.toList()));
              }
            } else if (rowObj instanceof Map) {
              Map ontologyTerm = (Map) row.get(c.getName());
              row.put(c.getName(), ontologyTerm.get("ontologyTermURI"));
            } else {
              throw new Exception(
                  "Expected ontology row to be instance of map or list but was "
                      + rowObj.getClass());
            }
          } else {
            // list of maps
            List<Map<String, Object>> listOfObjects =
                (List<Map<String, Object>>) row.get(c.getName());
            if (listOfObjects != null) {
              row.put(
                  c.getName(),
                  listOfObjects.stream()
                      .map(o -> prefix + o.get(ref.getName()))
                      .collect(Collectors.toList()));
            } else {
              row.put(c.getName(), null);
            }
          }
        }
      }
      // check if _molgenisid has been set via @id
      if (row.get("@id") == null) {
        row.put(
            "@id",
            path + table.getName() + "/" + row.get(table.getMetadata().getPrimaryKeys().get(0)));
      }
    }
  }

  public static void getTtlForSchema(Schema schema, PrintWriter printWriter) {
    StringWriter sw = new StringWriter();
    getJsonLdForSchema(schema, new PrintWriter(sw));
    convertToTtl(printWriter, sw);
  }

  public static void getTtlForTable(Table table, PrintWriter printWriter) {
    StringWriter sw = new StringWriter();
    getJsonLdForTable(table, new PrintWriter(sw));
    convertToTtl(printWriter, sw);
  }

  private static void convertToTtl(PrintWriter printWriter, StringWriter sw) {
    try {
      RDFParser rdfParser = Rio.createParser(RDFFormat.JSONLD);
      Model model = new LinkedHashModel();
      rdfParser.setRDFHandler(new StatementCollector(model));
      rdfParser.parse(new StringReader(sw.toString()), "http://localhost/");
      Rio.write(model, printWriter, RDFFormat.TURTLE);
    } catch (Exception e) {
      throw new MolgenisException("generation of ttl failed", e);
    }
  }
}
