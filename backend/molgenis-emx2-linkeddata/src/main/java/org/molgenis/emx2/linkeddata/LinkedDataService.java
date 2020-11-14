package org.molgenis.emx2.linkeddata;

import static org.molgenis.emx2.SelectColumn.s;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.JavaScriptUtils;

public class LinkedDataService {

  private LinkedDataService() {
    // hidden
  }

  // naive implementation, need some kind of paging in the future, and column selection
  public static void dump(Schema schema, PrintWriter writer) {

    // iterate through meta model
    for (Table table : schema.getTablesSorted()) {
      applySemanticDump(writer, table);
    }
    // query each table

    // write each column value that has the rdfTemplate property

  }

  private static void applySemanticDump(PrintWriter writer, Table table) {
    // check if has any semantic stuff
    if (table.getMetadata().getColumns().stream().anyMatch(c -> c.getRdfTemplate() != null)) {
      // need two layer select
      Query q = table.query();
      for (Column c : table.getMetadata().getColumns()) {
        SelectColumn s = s(c.getName());
        if (c.isReference()) {
          for (Column sub : c.getRefTable().getColumns()) {
            s.select(sub.getName());
          }
        }
        q.select(s);
      }

      try {
        for (Map<String, Object> row :
            (List<Map<String, Object>>)
                new ObjectMapper().readValue(q.retrieveJSON(), Map.class).get(table.getName())) {
          applyTemplateOnRow(writer, table, row);
        }
      } catch (Exception e) {
        throw new MolgenisException("rdf generation failed", e);
      }
    }
  }

  private static void applyTemplateOnRow(PrintWriter writer, Table table, Map<String, Object> row) {
    StringBuilder builder = new StringBuilder();
    for (Column c : table.getMetadata().getColumns()) {
      applyTemplateOnColumn(row, builder, c);
    }
    String subject = builder.toString().trim();
    if (subject.endsWith(";")) subject = subject.substring(0, subject.length() - 1) + ".";
    writer.println(subject);
  }

  private static void applyTemplateOnColumn(
      Map<String, Object> row, StringBuilder builder, Column c) {
    if (c.getRdfTemplate() != null && row.get(c.getName()) != null) {
      if (c.isArray()) {
        for (Object sub : (List<Object>) row.get(c.getName())) {
          Map<String, Object> nest = new LinkedHashMap<>();
          nest.put(c.getName(), sub);
          builder.append(
              JavaScriptUtils.executeJavascriptOnMap("`" + c.getRdfTemplate() + "`", nest) + "\n");
        }
      } else {
        builder.append(
            JavaScriptUtils.executeJavascriptOnMap("`" + c.getRdfTemplate() + "`", row) + "\n");
      }
    }
  }
}
