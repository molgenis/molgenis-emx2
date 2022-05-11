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
  private static ObjectMapper jsonMapper =
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

      String path = "http://localhost/" + table.getSchema().getName() + "/";

      // define the selection
      // in particular, for references we check if there are columns tagged with 'id'
      // because then 'id' will be used as the @id of the reference, otherwise we will use primary
      // key
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
                  .collect(Collectors.toList());
          // if no with id, we use primary key
          if (refId.size() == 0) {
            refId =
                c.getRefTable().getColumns().stream()
                    .filter(r -> r.getKey() == 1)
                    .collect(Collectors.toList());
          }
          // check if only one
          if (refId.size() > 1) {
            throw new MolgenisException(
                "Generation of jsonLd failed: more than one column marked with 'id' or primary key in table "
                    + table.getName());
          }
          // add to select
          q.select(s(c.getName(), s(refId.get(0).getName())));
        } else if (c.isReference() && c.isOntology()) {
          Column iri =
              c.getRefTable().getColumns().stream()
                  .filter(r -> r.getName().equals("ontologyTermIRI"))
                  .collect(Collectors.toList())
                  .get(0);
          q.select(s(c.getName(), s(iri.getName())));
        } else {
          q.select(s(c.getName()));
        }
      }

      // create the context
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

      // we use the query here
      String json = q.retrieveJSON();
      Map<String, List<Map<String, Object>>> jsonMap = jsonMapper.readValue(json, Map.class);
      List<Map<String, Object>> data = jsonMap.get(table.getName());

      // enhance json
      for (Map row : data) {
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
              row.put(c.getName(), ((Map<String, Object>) row.get(c.getName())).get(ref.getName()));
            } else if (c.isRef() && c.isOntology()) {
              row.put(
                  c.getName(), ((Map<String, Object>) row.get(c.getName())).get("ontologyTermIRI"));
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

      // assemble the json-ld for this table
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("@context", context);
      result.put("@id", path + table.getName());
      result.put(table.getName(), data);

      writer.append(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
    } catch (Exception e) {
      throw new MolgenisException("jsonld export failed", e);
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
