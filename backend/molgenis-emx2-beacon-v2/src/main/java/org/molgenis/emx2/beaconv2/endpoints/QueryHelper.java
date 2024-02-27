package org.molgenis.emx2.beaconv2.endpoints;

import static org.molgenis.emx2.SelectColumn.s;

import graphql.ExecutionResult;
import graphql.GraphQL;
import java.util.*;
import java.util.Map;
import org.jooq.tools.StringUtils;
import org.molgenis.emx2.*;
import org.molgenis.emx2.graphql.GraphqlApiFactory;
import org.molgenis.emx2.utils.TypeUtils;

// todo: do we still need this if we use retrieveRows?
public class QueryHelper {
  private QueryHelper() {
    // static only
  }

  /**
   * Select query columns from table, including the columns of any reference.
   *
   * @param table
   * @throws Exception
   */
  public static Query selectColumns(Table table, String... filters) {
    Query query = table.query();
    for (Column column : table.getMetadata().getColumns()) {
      if (column.isOntology() || column.isReference()) {
        List<Column> ontoRefCols = column.getRefTable().getColumns().stream().toList();
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
    if (filters != null && filters.length != 0 && filters[0] != null) {
      query.search(filters);
    }

    return query;
  }

  public static ExecutionResult queryTable(Table table) {
    GraphQL graphQL = new GraphqlApiFactory().createGraphqlForSchema(table.getSchema());
    StringBuilder sb = new StringBuilder("{");
    sb.append(table.getName()).append("{");

    queryColumns(table.getMetadata().getColumnsWithoutHeadings(), sb);

    sb.append("}}");
    return graphQL.execute(sb.toString());
  }

  private static void queryColumns(List<Column> columns, StringBuilder sb) {
    Set<String> seenTables = new HashSet<>();
    seenTables.add(columns.get(0).getTable().getIdentifier());
    int currentDepth = 0;
    int maxDepth = 4;
    queryColumnsRecursively(columns, seenTables, sb, maxDepth, currentDepth);
  }

  private static int queryColumnsRecursively(
      List<Column> columns,
      Set<String> seenTables,
      StringBuilder sb,
      int maxDepth,
      int currentDepth) {
    for (Column column : columns) {
      if (column.isOntology() || column.isReference()) {
        if (currentDepth < maxDepth) {
          TableMetadata refTable = column.getRefTable();
          // Don't select the same table twice
          //          if (seenTables.contains(refTable.getIdentifier())) continue;
          seenTables.add(refTable.getIdentifier());

          currentDepth++;
          sb.append(column.getIdentifier()).append("{");
          currentDepth =
              queryColumnsRecursively(
                  refTable.getColumnsWithoutHeadings(), seenTables, sb, maxDepth, currentDepth);
          sb.append("}");
        }
      } else if (!column.isSystemColumn()) {
        sb.append(column.getIdentifier()).append(",");
      }
    }
    currentDepth--;
    return currentDepth;
  }

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
    List<Map<String, Object>> mapList = (List<Map<String, Object>>) mapListObj;
    OntologyTerm[] result = new OntologyTerm[mapList.size()];
    for (int i = 0; i < mapList.size(); i++) {
      OntologyTerm ontologyTerm = new OntologyTerm();
      ontologyTerm.setId(
          mapList.get(i).get("codesystem") + ":" + TypeUtils.toString(mapList.get(i).get("code")));
      ontologyTerm.setLabel(TypeUtils.toString(mapList.get(i).get("name")));
      ontologyTerm.setUri(TypeUtils.toString(mapList.get(i).get("ontologyTermURI")));
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
