package org.molgenis.emx2.beaconv2.endpoints;

import java.util.*;
import java.util.Map;
import org.jooq.tools.StringUtils;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.TypeUtils;

// todo: do we still need this if we use retrieveRows?
public class QueryHelper {
  private QueryHelper() {
    // static only
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
    Map<String, Object> map = (Map<String, Object>) mapObj;
    ontologyTerm.setId(map.get("codesystem") + ":" + TypeUtils.toString(map.get("code")));
    ontologyTerm.setLabel(TypeUtils.toString(map.get("name")));
    ontologyTerm.setUri(TypeUtils.toString(map.get("ontologyTermURI")));
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
      List<Column> pathToColumn, String columnSemanticTagOrIRI, Table table) {

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
