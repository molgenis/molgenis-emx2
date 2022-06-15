package org.molgenis.emx2.beaconv2.common;

import static org.molgenis.emx2.SelectColumn.s;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.SelectColumn;
import org.molgenis.emx2.Table;

public class QueryHelper {

  /** for every column that is an ontology, add the name, code and codesystem to select */
  public static void selectColumns(Table t, Query q) throws Exception {
    for (Column c : t.getMetadata().getColumns()) {
      if (c.isOntology()) {
        List<Column> ontoRefCols =
            c.getRefTable().getColumns().stream()
                .filter(
                    colDef ->
                        colDef.getName().equals("name")
                            || colDef.getName().equals("code")
                            || colDef.getName().equals("codesystem"))
                .collect(Collectors.toList());
        ArrayList<String> colNames = new ArrayList<>();
        for (Column cc : ontoRefCols) {
          colNames.add(cc.getName());
        }
        q.select(new SelectColumn(c.getName(), colNames));
      } else if (c.isReference()) {
        throw new Exception(
            "Reference datatypes (except ontology) not yet supported in Biosamples");
      } else {
        q.select(s(c.getName()));
      }
    }
  }
}
