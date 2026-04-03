import { gql } from "graphql-tag";
import type {
  gqlVariableSubSelectionIF,
  PrepareChartDataIF,
} from "../interfaces/viz";

/**
 * buildQuery
 * Compile a query into graphql format
 *
 * @param table name of the table where the data is stored
 * @param selections an array containing one or more columns to selection
 *
 * @returns string
 */

export interface BuildQueryIF {
  table: string;
  selections?: string[];
}

export function buildQuery({ table, selections }: BuildQueryIF): string {
  const subSelection: Array<string> = selections.filter(
    (entry: string) => typeof entry !== undefined
  );
  const query = gql`query {
    ${table} {
      ${subSelection}
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
export function gqlExtractSelectionName(variable: string): string {
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
export function gqlExtractSubSelectionNames(variable: string): string | null {
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
 *
 * @param data dataset return from a graphql quert (array of objects)
 * @param chartVariables an array of objects containing variables to select for the chart dataset
 *
 * @returns array of objects reduced and flattened to x and y variables
 */

export function prepareChartData({
  data,
  chartVariables,
}: PrepareChartDataIF): object[] {
  return data.map((row: object) => {
    const newRow: object = {};
    chartVariables.forEach((variable: string) => {
      newRow[variable.key] = extractNestedRowValue(
        row,
        variable.key,
        variable.nestedKey
      );
    });

    return newRow;
  });
}
