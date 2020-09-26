package org.molgenis.emx2.linkeddata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.JSON;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.JavaScriptUtils;

import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.molgenis.emx2.SelectColumn.s;

public class LinkedDataService {

  // naive implementation, need some kind of paging in the future, and column selection
  public static void dump(Schema schema, PrintWriter writer) {

    // iterate through meta model
    for (Table table : schema.getTablesSorted()) {

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
            StringBuffer buffer = new StringBuffer();
            for (Column c : table.getMetadata().getColumns()) {
              if (c.getRdfTemplate() != null && row.get(c.getName()) != null) {
                if (c.isArray()) {
                  for (Object sub : (List<Object>) row.get(c.getName())) {
                    Map nest = new LinkedHashMap();
                    nest.put(c.getName(), sub);
                    buffer.append(
                        JavaScriptUtils.executeJavascriptOnMap("`" + c.getRdfTemplate() + "`", nest)
                            + "\n");
                  }
                } else {
                  buffer.append(
                      JavaScriptUtils.executeJavascriptOnMap("`" + c.getRdfTemplate() + "`", row)
                          + "\n");
                  // }
                }
              }
            }
            String subject = buffer.toString().trim();
            if (subject.endsWith(";")) subject = subject.substring(0, subject.length() - 1) + ".";
            writer.println(subject);
          }
        } catch (Exception e) {
          throw new MolgenisException("rdf generation failed", e);
        }
      }
    }
    // query each table

    // write each column value that has the rdfTemplate property

  }
}
