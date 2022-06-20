package org.molgenis.emx2.beaconv2.common;

import static org.molgenis.emx2.SelectColumn.s;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.SelectColumn;
import org.molgenis.emx2.Table;

public class QueryHelper {

  /**
   * Select query columns from table, including OntologyTerm sub columns. For ontologyTerm the name,
   * code and codesystem are added to the select. These are needed for the Beacon response.
   *
   * @param t
   * @param q
   * @throws Exception
   */
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

  /**
   * Convert list of maps to an array of ontology terms
   *
   * @param mapList
   * @return
   */
  public static OntologyTerm[] mapListToOntologyTerms(List<Map> mapList) {
    OntologyTerm[] result = new OntologyTerm[mapList.size()];
    for (int i = 0; i < mapList.size(); i++) {
      OntologyTerm ot = new OntologyTerm();
      ot.setId(mapList.get(i).get("codesystem") + ":" + (String) mapList.get(i).get("code"));
      ot.setLabel((String) mapList.get(i).get("name"));
      result[i] = ot;
    }
    return result;
  }

  /**
   * Convert a single map to an ontology term
   *
   * @param map
   * @return
   */
  public static OntologyTerm mapToOntologyTerm(Map map) {
    OntologyTerm ot = new OntologyTerm();
    ot.setId(map.get("codesystem") + ":" + (String) map.get("code"));
    ot.setLabel((String) map.get("name"));
    return ot;
  }
}
