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
   * @param table
   * @param query
   * @throws Exception
   */
  public static void selectColumns(Table table, Query query) throws Exception {
    for (Column column : table.getMetadata().getColumns()) {
      if (column.isOntology() || column.isReference()) {
        List<Column> ontoRefCols =
            column.getRefTable().getColumns().stream().collect(Collectors.toList());
        ArrayList<String> colNames = new ArrayList<>();
        for (Column ontoRefCol : ontoRefCols) {
          colNames.add(ontoRefCol.getName());
        }
        query.select(new SelectColumn(column.getName(), colNames));
      } else {
        query.select(s(column.getName()));
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
      OntologyTerm ontologyTerm = new OntologyTerm();
      ontologyTerm.setId(
          mapList.get(i).get("codesystem") + ":" + (String) mapList.get(i).get("code"));
      ontologyTerm.setLabel((String) mapList.get(i).get("name"));
      result[i] = ontologyTerm;
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
    OntologyTerm ontologyTerm = new OntologyTerm();
    ontologyTerm.setId(map.get("codesystem") + ":" + (String) map.get("code"));
    ontologyTerm.setLabel((String) map.get("name"));
    return ontologyTerm;
  }
}
