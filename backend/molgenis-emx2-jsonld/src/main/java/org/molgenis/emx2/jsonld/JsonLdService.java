package org.molgenis.emx2.jsonld;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.molgenis.emx2.*;

public class JsonLdService {
  private static ObjectMapper json =
      new ObjectMapper()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .setDateFormat(new StdDateFormat().withColonInTimeZone(true));

  private JsonLdService() {
    // hidden
  }

  // naive implementation, need some kind of paging in the future, and column selection
  public static void jsonld(Schema schema, PrintWriter writer) {
    // iterate through meta model
    writer.append("[");
    boolean comma = false;
    for (Table t : schema.getTablesSorted()) {
      if (comma) {
        writer.append(",");
      } else {
        comma = true;
      }
      jsonld(t, writer);
    }
    writer.append("]");
  }

  public static void jsonld(Table table, PrintWriter writer) {
    try {

      String path = "http://localhost/" + table.getSchema().getName() + "/";

      // create the context
      Map<String, Object> context = new LinkedHashMap<>();
      context.put(table.getName(), path + table.getName());
      // map id because @id is not graphql compatible
      context.put("_molgenisid", "@id");

      // is composition of type specific context elements
      for (Column c : table.getMetadata().getColumns()) {
        if (c.getJsonldType() != null) {
          Object type = read(c.getJsonldType());
          if (type instanceof Map) {
            // in case of map it overrides the other values
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) type).entrySet()) {
              context.put(entry.getKey(), entry.getValue());
            }
          } else {
            context.put(c.getName(), type);
          }
        }
      }

      // get the data (we try to keep json as clean as we can, so use @context above for all stuff)
      List<Map<String, Object>> data = new ArrayList<>();
      for (Row row : table.retrieveRows()) {
        if (table.getMetadata().getJsonldType() != null) {
          Object type = read(table.getMetadata().getJsonldType());
          if (type instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) type).entrySet()) {
              row.set(entry.getKey(), entry.getValue());
            }
          } else {
            row.set("@type", type);
          }
        }
        for (Column c : table.getMetadata().getColumns()) {
          // in case of reference we must change to use fully qualified name
          if (c.isReference()) {
            row.set(c.getName(), path + c.getRefTableName() + "/" + row.getString(c.getName()));
          }
          if (c.getKey() == 1) {
            row.set("_molgenisid", path + table.getName() + "/" + row.getString(c.getName()));
          }
        }
        data.add(row.getValueMap());
      }

      // assemble the json-ld for this table
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("@context", context);
      result.put("@id", path + table.getName());
      result.put(table.getName(), data);

      writer.append(json.writeValueAsString(result));
    } catch (Exception e) {
      throw new MolgenisException("jsonld export failed", e);
    }
  }

  public static void ttl(Schema schema, PrintWriter printWriter) {
    StringWriter sw = new StringWriter();
    jsonld(schema, new PrintWriter(sw));
    convertToTtl(printWriter, sw);
  }

  public static void ttl(Table table, PrintWriter printWriter) {
    StringWriter sw = new StringWriter();
    jsonld(table, new PrintWriter(sw));
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

  private static Object read(String value) throws JsonProcessingException {
    // this is to accomodate that json might be a string or an object or an array
    Object result;
    try {
      // map
      result = json.readValue(value, Map.class);
    } catch (Exception e) {
      try {
        // array
        result = json.readValue(value, List.class);
      } catch (Exception e2) {
        try {
          result = json.readValue(value, String.class);
        } catch (Exception e3) {
          throw new MolgenisException("Cannot parse '" + value + "'", e3);
        }
      }
    }
    return result;
  }
}
