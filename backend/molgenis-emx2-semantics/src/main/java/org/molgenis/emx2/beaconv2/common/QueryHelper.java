package org.molgenis.emx2.beaconv2.common;

import static org.molgenis.emx2.SelectColumn.s;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jooq.tools.StringUtils;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Query;
import org.molgenis.emx2.SelectColumn;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.utils.TypeUtils;

public class QueryHelper {

  /**
   * Select query columns from table, including the columns of any reference.
   *
   * @param table
   * @throws Exception
   */
  public static Query selectColumns(Table table) {
    Query query = table.query();
    for (Column column : table.getMetadata().getColumns()) {
      if (column.isOntology() || column.isReference()) {
        List<Column> ontoRefCols =
            column.getRefTable().getColumns().stream().collect(Collectors.toList());
        ArrayList<String> colNames = new ArrayList<>();
        for (Column ontoRefCol : ontoRefCols) {
          colNames.add(ontoRefCol.getName());
        }
        query.select(new SelectColumn(column.getName(), colNames));
      } else if (column.isFile()) {
        ArrayList<String> colNames = new ArrayList<>();
        colNames.add("id");
        colNames.add("mimetype");
        colNames.add("extension");
        // skip contents, which is served by file api
        query.select(new SelectColumn(column.getName(), colNames));
      } else if (column.isHeading()) {
        // ignore headings, not part of rows
      } else {
        query.select(s(column.getName()));
      }
    }
    return query;
  }

  /**
   * Convert list of maps to an array of ontology terms
   *
   * @param mapList
   * @return
   */
  public static OntologyTerm[] mapListToOntologyTerms(List<Map> mapList) {
    if (mapList == null) {
      return null;
    }

    OntologyTerm[] result = new OntologyTerm[mapList.size()];
    for (int i = 0; i < mapList.size(); i++) {
      OntologyTerm ontologyTerm = new OntologyTerm();
      ontologyTerm.setId(
          mapList.get(i).get("codesystem") + ":" + TypeUtils.toString(mapList.get(i).get("code")));
      ontologyTerm.setLabel(TypeUtils.toString(mapList.get(i).get("name")));
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
    if (map == null) {
      return null;
    }
    ontologyTerm.setId(map.get("codesystem") + ":" + TypeUtils.toString(map.get("code")));
    ontologyTerm.setLabel(TypeUtils.toString(map.get("name")));
    return ontologyTerm;
  }

  /**
   * Find column and GraphQL path to that column in a table based on a semantic tag.
   *
   * @param buildGraphQLFilter Supply empty string, will be filled by function recursively
   * @param columnSemanticTagOrIRI Find column that matches this semantic tag
   * @param table Table structure to search in
   * @return
   */
  public static ColumnPath findColumnPath(
      String buildGraphQLFilter, String columnSemanticTagOrIRI, Table table) {
    for (Column column : table.getMetadata().getColumns()) {
      if (column.getName().startsWith("mg_")) {
        continue;
      }

      // check semantics, return if found
      for (String semantics : column.getSemantics()) {
        if (semantics.endsWith(columnSemanticTagOrIRI)) {
          return new ColumnPath(column, buildGraphQLFilter + " {" + column.getName() + ": {");
        }
      }

      // if reference, also step in recursively
      if (column.isReference()) {
        Table refTable = table.getSchema().getTable(column.getRefTableName());
        // cannot refer to itself, circular
        if (!refTable.getName().equals(table.getName())) {
          ColumnPath columnPath =
              findColumnPath(
                  buildGraphQLFilter + " {" + column.getName() + ": ",
                  columnSemanticTagOrIRI,
                  refTable);
          if (columnPath != null) {
            return columnPath;
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
