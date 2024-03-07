import { gql, request } from "graphql-request";
import { emxTypes } from "../utils/defaults";

/**
 * Query schema metadata including tables and columns
 */
export async function query_schema_meta() {
  const query = gql`
    query {
      _schema {
        name
        tables {
          name
          tableType
          columns {
            name
            label
            columnType
          }
        }
      }
    }
  `;
  const response = await request("../api/graphql", query);
  const data = response._schema;
  data.tables = data.tables
    .filter((table) => table.tableType === "DATA")
    .map((table) => {
      const columns = table.columns.filter(
        (column) => column.name.indexOf("mg_") === -1
      );
      table.columns = columns;
      return table;
    });
  return data;
}

/**
 * Map color values to one or more groups
 *
 * @param labels - An array containing one or more categorical values
 * @param palette - An array containing a color scheme
 *
 * @returns Returns an object where the labels are mapped to colors
 */

export function createPalette(labels: Array, palette: Array): Object {
  const colors = labels.map((label, index) => [label, palette[index]]);
  return Object.fromEntries(colors);
}

/**
 * Generate a sequence between two numbers by a specific interval
 * @param start the number to start the sequence
 * @param stop the number to stop the sequence
 * @param by the increment between each item in the sequence
 * @returns an array of numbers
 */
export function seqAlongBy(start: Number, stop: Number, by: Number): Array {
  return Array.from(
    { length: (stop - start) / by + 1 },
    (_, i) => start + i * by
  );
}

/**
 * Check to see if number is between 0 and 1
 * @param value a value that is between 0 and 1
 * @returns boolean
 */
export function validateNumRange(value: number) {
  return value >= 0 && value <= 1;
}

/**
 * Deterimine acceptable chart type based on selected attributes
 *
 * @param xType the data class of the selected x variable
 * @param yType the data class of the selected y variable
 * @returns Array of one or more acceptable chart types
 */
export function setChartType(xType: String, yType: String) {
  if (xType === "continuous" && yType === "continuous") {
    return ["ScatterPlot"];
  }

  if (xType === "continuous" && yType === "categorical") {
    return ["BarChart", "PieChart"];
  }

  if (xType === "categorical" && yType === "continuous") {
    return ["ColumnChart", "PieChart"];
  }

  return null;
}

/**
 * Generate a psuedo-random string of numbers and letters
 * @param length number of characters to return in the string
 * @returns String of letters and numbers
 */
export function generateIdString(length: Number = 6) {
  return Math.random()
    .toString(36)
    .substring(2, 2 + length);
}
