import { gql } from "graphql-tag";

/**
 * buildQuery
 * Compile a query into graphql format
 *
 * @param table name of the table where the data is stored
 * @param x name of the column to plot along the x-axis
 * @param y name of the column to plot along the y-axis
 *
 * @returns string
 */
export function buildQuery(table: string, x: string, y: string): string {
  const query = gql`query {
    ${table} {
      ${x}
      ${y}
    }
  }`;
  return query;
}

/**
 * gqlExtractSelectionName
 * The purpose of this function is to provide graphql support for variable selections.
 * If a query is supplied, it is important to extract the column name. This function
 * takes the variable query and extracts the selection set name. For example, if you
 * define the xvar as `column { name }`, this function extracts the string `column`.
 *
 * @param variable string containing a column name as a string or a query
 *
 * @returns string
 */
export function gqlExtractSelectionName(variable: string) {
  if (variable.match(/[\{\}]/)) {
    const query = gql`query { ${variable} }`;
    const definitions = query.definitions[0];
    const selectionName = definitions.selectionSet?.selections[0].name?.value;
    if (selectionName) {
      return selectionName;
    }
  }
  return variable;
}

/**
 * gqlExtractSubSelectionNames
 * Like the function `gqlExtractSelectionname`, this function provides graphql support
 * for variable selection. If a query is defined, we need to identify the primary
 * subselection. For now, this function selects the first item in the subselection.
 *
 * @param variable string or graphql string
 *
 * @returns name of the first subselection
 */
export function gqlExtractSubSelectionNames(variable: string) {
  if (variable.match(/[\{\}]/)) {
    const query = gql`query { ${variable} }`;
    const selectionSet =
      query.definitions[0].selectionSet?.selections[0].selectionSet?.selections;
    if (selectionSet.length > 0) {
      const subSelections = selectionSet[0].name?.value;
      return subSelections;
    }
  }
}

/**
 * extractNestedRowValue
 * This function can be used to flatten data of ref types (ontology, ref, etc.) at the
 * row level. The value is extracted if a nested key is defined.
 *
 * @param row a row in a dataset (object)
 * @param key the name of the column in a row
 * @param nestedKey the name of the nested column which contains the value to extract
 *
 * @returns string or number
 */
export function extractNestedRowValue(
  row: object,
  key: string,
  nestedKey: string
): string | number {
  if (typeof row[key] === "object" && nestedKey) {
    return row[key][nestedKey];
  }
  return row[key];
}

/**
 * prepareChartData
 * This function should be used to prepare a chart
 * @param data dataset return from a graphql quert (array of objects)
 * @param x the name of the column to plot along the x-axis
 * @param y the name of the column to plot along the y-axis
 * @param nestedXKey for ref data types, the name of the nested column to target
 * @param nestedYKey for ref data types, the name of the nested column to target
 *
 * @returns array of objects reduced and flattened to x and y variables
 */
export function prepareChartData(
  data: Array[],
  x: string,
  y: string,
  nestedXKey: string,
  nestedYKey: string
): array {
  return data.map((row: object) => {
    const newRow: object = {};
    newRow[x] = extractNestedRowValue(row, x, nestedXKey);
    newRow[y] = extractNestedRowValue(row, y, nestedYKey);
    return newRow;
  });
}
