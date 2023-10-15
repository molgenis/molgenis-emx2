package org.molgenis.emx2.semantics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jooq.tools.StringUtils;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.utils.TypeUtils;

public class QueryHelper {

  /**
   * Convert list of maps to an array of ontology terms
   *
   * @param mapListObj
   * @return
   */
  public static OntologyTerm[] mapListToOntologyTerms(Object mapListObj) {
    if (mapListObj == null) {
      return null;
    }
    List<Map> mapList = (List<Map>) mapListObj;
    OntologyTerm[] result = new OntologyTerm[mapList.size()];
    for (int i = 0; i < mapList.size(); i++) {
      OntologyTerm ontologyTerm = new OntologyTerm();
      ontologyTerm.setId(
          mapList.get(i).get("codesystem") + ":" + TypeUtils.toString(mapList.get(i).get("code")));
      ontologyTerm.setLabel(TypeUtils.toString(mapList.get(i).get("name")));
      ontologyTerm.setURI(TypeUtils.toString(mapList.get(i).get("ontologyTermURI")));
      result[i] = ontologyTerm;
    }
    return result;
  }

  /**
   * Convert a single map to an ontology term
   *
   * @param mapObj
   * @return
   */
  public static OntologyTerm mapToOntologyTerm(Object mapObj) {
    OntologyTerm ontologyTerm = new OntologyTerm();
    if (mapObj == null) {
      return null;
    }
    Map map = (Map) mapObj;
    ontologyTerm.setId(map.get("codesystem") + ":" + TypeUtils.toString(map.get("code")));
    ontologyTerm.setLabel(TypeUtils.toString(map.get("name")));
    ontologyTerm.setURI(TypeUtils.toString(map.get("ontologyTermURI")));
    return ontologyTerm;
  }

  /**
   * Find column and GraphQL path to that column in a table based on a semantic tag.
   *
   * @param pathToColumn Supply empty ArrayList, will be filled by function recursively
   * @param columnSemanticTagOrIRI Find the column that matches this semantic tag
   * @param table Table structure to start search in, pathToColumn will be relative to this
   * @return
   */
  public static ColumnPath findColumnPath(
      ArrayList<Column> pathToColumn, String columnSemanticTagOrIRI, Table table) {

    for (Column column : table.getMetadata().getColumns()) {
      if (column.isSystemColumn()) {
        continue;
      }

      // check semantics, return if found
      for (String semantics : column.getSemantics()) {
        if (semantics.endsWith(columnSemanticTagOrIRI)) {
          return new ColumnPath(column, pathToColumn);
        }
      }

      // if reference, also step in recursively
      if (column.isReference()) {
        Table refTable = table.getSchema().getTable(column.getRefTableName());
        boolean isNotACircularReference =
            !refTable.getName().equals(table.getName()) && !pathToColumn.contains(column);
        if (isNotACircularReference) {
          pathToColumn.add(column);
          ColumnPath columnPath = findColumnPath(pathToColumn, columnSemanticTagOrIRI, refTable);
          if (columnPath != null) {
            return columnPath;
          } else {
            // if not found, remove last path entry
            pathToColumn.remove(pathToColumn.size() - 1);
          }
        }
      }
    }
    return null;
  }

  /**
   * Finalize GraphQL filter by adding missing end braces
   *
   * @param filter
   * @return
   */
  public static String finalizeFilter(String filter) {
    int nrOfStartingBraces = StringUtils.countMatches(filter, "{");
    return filter + "}".repeat(nrOfStartingBraces);
  }
}
